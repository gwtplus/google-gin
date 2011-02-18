/**
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.gwt.inject.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.util.Preconditions;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.BindingContext;
import com.google.gwt.inject.rebind.binding.ParentBinding;
import com.google.gwt.inject.rebind.binding.RequiredKeys;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.assistedinject.Assisted;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

/**
 * Instantiates implicit bindings for unresolved keys, after determining in which Ginjector they
 * should be created.
 * 
 * <p>It is important to place bindings in the correct Ginjector, because it can affect the
 * semantics of the generated code.  For instance, if an implicitly bound object is annotated
 * with {@code @Singleton}, each implicit binding that is instantiated will correspond to a
 * <i>separate</i> instance of the object.
 * 
 * <p>A new implicit binding is placed in the highest Ginjector satisfying the following criteria:
 * 
 * <ul>
 * <li>For each dependency of the binding's target key, a binding of that dependency exists at or
 * above the Ginjector in which the new binding is created.  This may require recursively
 * instantiating an implicit binding for one or more of the dependencies.
 * </li>
 * <li>The new binding is created at or above the Ginjector in which it was required.
 * </li>
 * <li>The new binding does not cause a key in another injector to become double-bound 
 * (i.e, no children of the injector in which the new binding is created have a binding to
 * the same key).
 * </li>
 * </ul>
 * 
 * <p>Specifically, we do the following to resolve a binding for a particular Ginjector
 * (known as the "origin" injector):
 * 
 * <ol>
 * <li>If the binding is already available to the origin from one of it's ancestors, we use
 * that binding ({@link #findBindingInParent(Key)}).
 * </li>
 * <li>For each dependency of the new binding's target key, find a binding at or above the
 * given Ginjector.  For each dependency which currently has no binding, instantiate an
 * implicit binding at or above the given Ginjector (by recursively calling {@link #resolve}).
 * </li>
 * <li>Determine the lowest "target" Ginjector at or above the origin Ginjector which (a) is not
 * higher than any of the dependencies of the target key, and (b) does not have a descendent which
 * binds the target key itself ({@link #lowest(Key, Set)}).
 * </li>
 * <li>Create the implicit binding in the target Ginjector.
 * </li>
 * </ol>
 * 
 * <p>Note that the same origin injector is used to instantiate the dependencies of a binding as for
 * the binding itself, even though the binding might end up being placed above the origin.  This
 * does not change where bindings are placed: each new binding is placed as high in the Ginjector
 * tree as possible, so if the binding of a dependency causes the output binding to move lower in
 * the tree, the output binding would always have to be that low anyway (since otherwise its
 * dependencies could not be bound).
 */
public class BindingResolver {

  private final ImplicitBindingCreator implicitBindingCreator;
  private final Provider<ParentBinding> parentBindingProvider;
  private final TreeLogger logger;
  private final GinjectorBindings origin;
  private final ErrorManager errorManager;
  
  /**
   * The dependency chain we're currently investigating.  We use a linked hash set because
   * we care about ordering when reconstructing the cycle for use in error messages.
   */
  private final Set<Key<?>> resolutionChain = new LinkedHashSet<Key<?>>();

  public interface BindingResolverFactory {
    BindingResolver create(GinjectorBindings origin);
  }

  @Inject
  public BindingResolver(TreeLogger logger, ImplicitBindingCreator implicitBindingCreator,
      Provider<ParentBinding> parentBindingProvider, 
      @Assisted GinjectorBindings origin, ErrorManager errorManager) {
    this.logger = logger;
    this.implicitBindingCreator = implicitBindingCreator;
    this.parentBindingProvider = parentBindingProvider;
    this.errorManager = errorManager;
    this.origin = origin;
  }

  /**
   * Resolves a key within this resolvers origin, using the algorithm described in the class
   * comments.  Once the binding has been resolved, returns the {@link GinjectorBindings}
   * corresponding to the highest Ginjector that the binding is available on.  Makes sure 
   * that the key is available to the origin by inheriting it from higher in the tree if
   * necessary.
   *  
   * @param key the key to resolve
   * @param optional should we log an error if we fail to resolve the binding
   * @param context a description of why the key was necessary -- used for logging
   *     and error messages
   * @return the {@link GinjectorBindings} with the actual binding for the key or null if we
   *     couldn't locate or create a binding.
   */
  public GinjectorBindings resolveAndInherit(Key<?> key, boolean optional, BindingContext context) {
    GinjectorBindings source = resolve(key, optional, context);
    Preconditions.checkState(resolutionChain.isEmpty(),
        "Expected all elements to be popped from resolution chain");
    return ensureParentBinding(origin, source, key);
  }
  
  /**
   * Same as {@link #resolveAndInherit(Key, boolean, BindingContext)} but without
   * creating the ParentBinding in the origin for accessing the key.  Used for
   * resolving dependencies.
   */
  private GinjectorBindings resolve(Key<?> key, boolean optional, BindingContext context) {
    // Check to see if we're trying to resolve something that's already on our resolve stack
    if (!resolutionChain.add(key)) {
      errorManager.logError("Circular dependency detected: " + describeCycle(key));
      // TODO(bchambers): If we detect a circular dependency, it may be worth ignoring other
      // errors (eg, Unable to create binding X) that appear as a result.
      return null;
    }
    
    // (Steps 1 & 2) If there is an explicit binding here or in the parent, use it
    GinjectorBindings target = findBindingInParent(key);
    
    // (Step 3) If the binding is available from the child, it would already be present in
    // the parent as either an ExposedChildBinding (in the bindings set) or in the
    // boundInChildren set (which would block resolution here)
    if (origin.isBoundInChild(key)) {
      if (!optional) {
        errorManager.logError("Unable to create binding for " + key 
            + ".  It is already bound in a descendent.  Consider exposing it?");
      }
      
      resolutionChain.remove(key);
      return null;
    }

    // (Steps 2 & 4-11) Create an implicit binding, in the appropriate parent injector, or
    // here.  Location is determined by examining the dependencies of the implicit binding.
    if (target == null) {
      target = createAndPositionImplicitBinding(key, optional, context);
    }

    if (!optional && target == null) {
      errorManager.logError("Unable to create binding for required key " + key + " in context: "
          + context);
    }

    // Pop this key from the resolution stack.
    resolutionChain.remove(key);

    return target;
  }

  /**
   * Creates a {@link ParentBinding} in the {@code child} {@link GinjectorBindings} that makes
   * the {@code source}'s binding for {@code key} available.
   */
  private GinjectorBindings ensureParentBinding(
      GinjectorBindings child, GinjectorBindings source, Key<?> key) {
    if (source == null || child == source) {
      return source;
    }
    
    Preconditions.checkState(!origin.isBound(key), "Shouldn't be trying to resolve bound keys");
    
    // TODO(bchambers): Modify addBinding to actually indicate which injector
    // it's modifying?  Also, should context include the BindingContext that caused us to
    // create this?
    ParentBinding parentBinding = parentBindingProvider.get();
    parentBinding.setKey(key);
    parentBinding.setParent(source);
    BindingContext context = BindingContext.forText("Inheriting " + key + " from parent");
    child.addBinding(key, new BindingEntry(parentBinding, context));
    return source;
  }

  /**
   * Find the highest binding in the Ginjector tree that could be used to supply the given key.
   * 
   * <p>Only Ginjectors at or above the origin injector need to be considered.  If the binding
   * is exposed from a sibling injector, it will appear in the parent injector that it is exposed
   * to.
   * 
   * <p>If this ever becomes a bottleneck, we could save a little time by jumping to the
   * parent whenever we encounter a ParentBinding.
   * 
   * @param key The binding to search for
   * @return The parent that contains the binding to use if it is found, otherwise {@code null}.
   */
  private GinjectorBindings findBindingInParent(Key<?> key) {
    // Find the closest injector with a binding
    GinjectorBindings source = findClosestBinding(key);
    if (source == null) {
      return source;
    }
   
    // Search up for the highest ginjector that contains a binding that is identical.
    while (source.getParent() != null) {
      BindingEntry entry = source.getBinding(key);
      if (entry != null) {
        // If a key is bound in a Ginjector, then the only ancestor Ginjectors that can contain
        // a binding to the same key are those which got the binding from the same source; 
        // otherwise, we would have a double binding.
        //
        // This means that we can stop searching up unless one of these cases pertains:
        // 1. The binding was inherited from an ancestor module, and is thus a ParentBinding
        // 2. The binding was exposed to a parent module, and thus the immediate parent binds
        //    the same key.
        if (!(entry.getBinding() instanceof ParentBinding || source.getParent().isBound(key))) {
          break;
        }
        source = source.getParent();
      }
    }
    return source;
  }
  
  private GinjectorBindings findClosestBinding(Key<?> key) {
    GinjectorBindings source = origin;
    while (source != null && !source.isBound(key)) {
      source = source.getParent();
    }
    return source;
  }

  private GinjectorBindings createAndPositionImplicitBinding(Key<?> key, boolean optional,
      BindingContext context) {
    // Create the binding
    Binding binding = implicitBindingCreator.create(key, optional);
    if (binding == null) {
      return null;
    }

    logger.log(TreeLogger.TRACE, "Implicit binding for " + key + ": " + binding);


    GinjectorBindings target = resolveDependenciesAndPosition(key, binding, optional);
    if (target != null) {
      target.addBinding(key, new BindingEntry(binding, context));
    }
    return target;
  }

  private GinjectorBindings resolveDependenciesAndPosition(Key<?> key, Binding binding,
      boolean optional) {

    Map<Key<?>, GinjectorBindings> depSources = new HashMap<Key<?>, GinjectorBindings>();

    // Resolve the required dependencies
    RequiredKeys requiredKeys = binding.getRequiredKeys();
    for (Key<?> dependency : requiredKeys.getRequiredKeys()) {
      // If this binding is optional than its dependencies are also optional
      GinjectorBindings source =
          resolve(dependency, optional, BindingContext.forDependency(dependency, key));
      if (source == null) {
        // If this isn't optional, then the binding wasn't optional, and an
        // error was already logged. Return "null" to indicate that this binding
        // was unavailable.
        return null;
      } else {
        depSources.put(dependency, source);
      }
    }

    // Resolve the optional dependencies
    for (Key<?> dependency : requiredKeys.getOptionalKeys()) {
      GinjectorBindings source =
          resolve(dependency, true, BindingContext.forDependency(dependency, key));
      if (source != null) {
        depSources.put(dependency, source);
      }
    }
    
    // Find the target by examining all the sources, and determining the "lowest" dependency
    Set<GinjectorBindings> injectorDeps = new HashSet<GinjectorBindings>(depSources.values());
    GinjectorBindings target = lowest(key, injectorDeps);
    Preconditions.checkNotNull(target);
    
    // Add bindings to the target to make any binding that come from "above" available
    for (Map.Entry<Key<?>, GinjectorBindings> dependencySource : depSources.entrySet()) {
      ensureParentBinding(target, dependencySource.getValue(), dependencySource.getKey());
    }
    
    return target;
  }

  /**
   * @return the lowest Ginjector at or above the origin Ginjector which (a) is not
   * higher than any of the dependencies of the target key, and (b) does not have a
   * descendent which binds the target key itself.  Returns null if no valid injector
   * can be found.
   */
  private GinjectorBindings lowest(Key<?> key, Set<GinjectorBindings> sources) {
    GinjectorBindings lowest = origin;
    while (lowest.getParent() != null) {
      if (sources.contains(lowest) || lowest.getParent().isBoundInChild(key)) {
        return lowest;
      }
      lowest = lowest.getParent();
    }
    
    if (lowest.isBoundInChild(key)) {
      return null;
    }
    
    return lowest;
  }
  
  /**
   * Describe the cycle that we detected in the resolution tree, starting with the given key.
   */
  private String describeCycle(Key<?> key) {
    StringBuilder depChain = new StringBuilder();
    Iterator<Key<?>> iter = resolutionChain.iterator();
    
    // Discard all elements leading up to the cycle (which started at key)
    while (iter.next() != key) {}
        
    // Now add the key to the builder
    depChain.append(key);
    
    // Now add all the elements that participated in the cycle:
    while (iter.hasNext()) {
      depChain.append(" -> ").append(iter.next());
    }
    
    // Tie the knot by adding the link back to the key
    depChain.append(" -> " ).append(key);
    return depChain.toString();
  }
}
