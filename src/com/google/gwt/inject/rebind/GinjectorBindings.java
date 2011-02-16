/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.inject.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.util.Preconditions;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.assistedinject.FactoryModule;
import com.google.gwt.inject.rebind.BindingResolver.BindingResolverFactory;
import com.google.gwt.inject.rebind.binding.BindingContext;
import com.google.gwt.inject.rebind.binding.BindingIndex;
import com.google.gwt.inject.rebind.binding.ExposedChildBinding;
import com.google.gwt.inject.rebind.binding.ParentBinding;
import com.google.gwt.inject.rebind.binding.RemoteServiceProxyBinding;
import com.google.gwt.inject.rebind.binding.RequiredKeys;
import com.google.gwt.inject.rebind.reflect.FieldLiteral;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.inject.rebind.util.GuiceUtil;
import com.google.gwt.inject.rebind.util.MemberCollector;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionPoint;
import com.google.inject.spi.Message;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

/**
 * Stores information that describes the bindings present in a given injector,
 * and the relationship to the other injectors in the hierarchy. This class is
 * used in two stages:
 * 
 * <ul>
 * <li>During bindings processing {@link BindingsProcessor} this
 * gathers up explicit bindings and unresolved dependencies. After all bindings
 * have been gathered from the modules, {@link #resolveBindings} is called in
 * the last stage of processing and it is finalized. After that point, no new
 * unresolved bindings should be added.
 * </li>
 * <li>At this point, it is ready to be used by {@link GinjectorOutputter} for
 * generating the Ginjector implementation. In this stage several additional
 * methods are available for getting information about the code that has been
 * generated to represent this ginjector.
 * </li>
 * </ul>
 *
 * <p>Each {@link GinjectorBindings} can have a parent ginjector, and any number of
 * child ginjectors.
 */
public class GinjectorBindings implements BindingIndex {

  private final TreeLogger logger;

  /**
   * Generates names for code we produce to resolve injection requests.
   */
  private final NameGenerator nameGenerator;

  /**
   * Map from key to binding for all types we already have a binding for.
   */
  private final Map<Key<?>, BindingEntry> bindings = new HashMap<Key<?>, BindingEntry>();

  /**
   * Map from key to scope for all types we have a binding for.
   */
  private final Map<Key<?>, GinScope> scopes = new HashMap<Key<?>, GinScope>();

  /**
   * Set of keys for classes that we still need to resolve. Every time a binding
   * is added to {@code bindings}, the key is removed from this set. When this
   * set and {@code unresolvedOptional} becomes empty, we know we've satisfied
   * all dependencies.
   */
  private final Set<Key<?>> unresolved = new HashSet<Key<?>>();

  /**
   * Set of keys for classes that we still need to resolve but that are
   * optionally bound. Every time a binding is added to {@code bindings}, the
   * key is removed from this set. When this set and {@code unresolved} becomes
   * empty, we know we've satisfied all dependencies.
   */
  private final Set<Key<?>> unresolvedOptional = new HashSet<Key<?>>();

  /**
   * Collection of keys for which the ginjector interface provides member inject
   * methods. If a regular binding is defined for the same key, no special
   * member inject handling is required - a member inject method will be created
   * as part of a regular binding.
   */
  private final Set<Key<?>> memberInjectRequests = new HashSet<Key<?>>();

  /**
   * Collection of all factory modules configured for this ginjector.
   */
  private final Set<FactoryModule<?>> factoryModules = new HashSet<FactoryModule<?>>();

  /**
   * All types for which static injection has been requested.
   */
  private final Set<Class<?>> staticInjectionRequests = new HashSet<Class<?>>();
  
  /**
   * The set of all keys that are bound in children of this ginjector.  This is used when creating
   * implicit bindings.  Specifically, we can't create an implicit binding here if any of the 
   * children already bind it (even if its not exposed) because it would lead to a double binding
   * error.
   */
  private final Set<Key<?>> boundInChildren = new HashSet<Key<?>>();

  /**
   * Collector that gathers all methods from an injector.
   */
  private final MemberCollector completeCollector;

  private final GuiceUtil guiceUtil;

  /**
   * Interface of the injector that this class is implementing.
   */
  private final TypeLiteral<? extends Ginjector> ginjectorInterface;

  private final ErrorManager errorManager;

  /**
   * The class (either Ginjector or Module) that this {@link GinjectorBindings}
   * is created for.
   */
  private Class<?> module;

  // Parent/Child information used for creating hierarchical injectors
  private GinjectorBindings parent = null;
  private final List<GinjectorBindings> children = new ArrayList<GinjectorBindings>();

  private final Provider<GinjectorBindings> ginjectorBindingsProvider;

  private final BindingResolver bindingResolver;

  /**
   * The {@link GinjectorBindings} are used in two "stages" -- during binding processing 
   * (in {@link BindingsProcessor}) it is used to gather information, and during ginjector
   * generation where (in {@link GinjectorOutputter}) that information is read. To help
   * catch accidental mistakes this tracks which stage it is currently being used in.
   * TODO(bchambers): Split this class into two parts, and refactor the stages so that
   * they use the appropriate part.
   */
  private boolean finalized = false;

  @Inject
  public GinjectorBindings(NameGenerator nameGenerator,
      TreeLogger logger,
      GuiceUtil guiceUtil,
      @GinjectorInterfaceType Class<? extends Ginjector> ginjectorInterface,
      Provider<GinjectorBindings> ginjectorBindingsProvider,
      MemberCollector collector,
      ErrorManager errorManager,
      BindingResolverFactory bindingResolverFactory) {
    this.nameGenerator = nameGenerator;
    this.logger = logger;
    this.guiceUtil = guiceUtil;
    this.bindingResolver = bindingResolverFactory.create(this);
    this.ginjectorInterface = TypeLiteral.get(ginjectorInterface);
    this.ginjectorBindingsProvider = ginjectorBindingsProvider;
    this.errorManager = errorManager;

    completeCollector = collector;
    completeCollector.setMethodFilter(MemberCollector.ALL_METHOD_FILTER);
  }

  void assertFinalized() {
    assert finalized;
  }

  void assertNotFinalized() {
    assert !finalized;
  }

  /**
   * Create a new {@link GinjectorBindings} that collects bindings for an injector that is the
   * child of this {@link GinjectorBindings}.
   *
   * @param module the module the ginjector is being created for
   * @return the child {@link GinjectorBindings}
   */
  public GinjectorBindings createChildGinjectorBindings(Class<?> module) {
    assertNotFinalized();
    GinjectorBindings child = ginjectorBindingsProvider.get();
    child.setParent(this);
    child.setModule(module);
    children.add(child);
    return child;
  }

  public void resolveBindings() {
    assertNotFinalized();
    if (!unresolved.isEmpty() || !unresolvedOptional.isEmpty()) {
      // Sanity check to make sure we never let bound things into the unresolved
      // sets
      Preconditions.checkState(!unresolved.removeAll(bindings.keySet()),
          "There shouldn't be any unresolved bindings in the bindings set");
      Preconditions.checkState(!unresolvedOptional.removeAll(bindings.keySet()),
          "There shouldn't be any unresolved bindings in the bindings set");

      // Iterate through copies because we will modify sets during iteration
      for (Key<?> key : new ArrayList<Key<?>>(unresolved)) {
        // TODO(bchambers,dburrows): Figure out a better initial context for
        // these.
        BindingContext context = BindingContext.forText("Implicit binding for unresolved " + key);
        bindingResolver.resolveAndInherit(key, false, context);
      }

      for (Key<?> key : new ArrayList<Key<?>>(unresolvedOptional)) {
        BindingContext context =
            BindingContext.forText("Implicit binding for unresolved optional " + key);
        if (bindingResolver.resolveAndInherit(key, true, context) == null) {
          unresolvedOptional.remove(key);
        }
      }
    }

    // After one pass, all bindings should be resolved.
    assert unresolved.isEmpty();
    assert unresolvedOptional.isEmpty();

    // Mark this collection as finalized, so that no new bindings or unresolved
    // dependencies
    // can be added.
    finalized = true;
  }

  public Map<Key<?>, BindingEntry> getBindings() {
    return bindings;
  }

  public BindingEntry getBinding(Key<?> key) {
    return bindings.get(key);
  }

  public Map<Key<?>, GinScope> getScopes() {
    return scopes;
  }

  public Set<Class<?>> getStaticInjectionRequests() {
    return staticInjectionRequests;
  }

  public void addMemberInjectRequests(Set<Key<?>> implementations) {
    memberInjectRequests.addAll(implementations);
  }

  public Set<Key<?>> getMemberInjectRequests() {
    return memberInjectRequests;
  }

  void putScope(Key<?> key, GinScope scope) {
    scopes.put(key, scope);
  }

  public GinjectorBindings getParent() {
    return parent;
  }

  public void setParent(GinjectorBindings parent) {
    assertNotFinalized();
    this.parent = parent;
  }

  public Class<?> getModule() {
    return module;
  }

  public void setModule(Class<?> module) {
    this.module = module;
  }

  public Iterable<GinjectorBindings> getChildren() {
    return children;
  }

  public NameGenerator getNameGenerator() {
    assertFinalized();
    return nameGenerator;
  }

  public Set<FactoryModule<?>> getFactoryModules() {
    return factoryModules;
  }

  public GinScope determineScope(Key<?> key) {
    assertFinalized();
    GinScope scope = getScopes().get(key);
    if (scope == null) {
      Class<?> raw = key.getTypeLiteral().getRawType();
      BindingEntry binding = bindings.get(key);
      if (binding != null 
          && (binding.getBinding() instanceof ExposedChildBinding
              || binding.getBinding() instanceof ParentBinding)) {
        // If this is just a "copy" of a binding higher/lower in the injector
        // tree, we prefer to treat the binding like it's unscoped, and refer to
        // the "real" binding every time we need the value.
        scope = GinScope.NO_SCOPE; 
      } else if (raw.getAnnotation(Singleton.class) != null
          || raw.getAnnotation(javax.inject.Singleton.class) != null) {
        // Look for scope annotation as a fallback
        scope = GinScope.SINGLETON;
      } else if (RemoteServiceProxyBinding.isRemoteServiceProxy(key.getTypeLiteral())) {
        // Special case for remote services
        scope = GinScope.SINGLETON;
      } else {
        scope = GinScope.NO_SCOPE;
      }
    }

    logger.log(TreeLogger.TRACE, "scope for " + key + ": " + scope);
    return scope;
  }

  public boolean isBound(Key<?> key) {
    return bindings.containsKey(key);
  }

  public void addUnresolved(Key<?> key) {
    assertNotFinalized();
    if (!bindings.containsKey(key)) {
      logger.log(TreeLogger.TRACE, "Add unresolved key: " + key);
      unresolved.add(key);
    }
  }

  void addUnresolvedEntriesForInjectorInterface() {
    assertNotFinalized();
    for (MethodLiteral<?, Method> method : completeCollector.getMethods(ginjectorInterface)) {
      nameGenerator.markAsUsed(method.getName());
      Key<?> key = guiceUtil.getKey(method);
      logger.log(TreeLogger.TRACE, "Add unresolved key from injector interface: " + key);

      // Member inject types do not need to be gin-creatable themselves but we
      // need to provide all dependencies.
      if (guiceUtil.isMemberInject(method)) {
        if (!unresolved.contains(key)) {
          memberInjectRequests.add(key);
          RequiredKeys requiredKeys =
              guiceUtil.getMemberInjectionRequiredKeys(key.getTypeLiteral());
          addRequiredKeys(key, requiredKeys);
        }
      } else if (!bindings.containsKey(key)) {
        unresolved.add(key);
      }
    }
  }

  void addBinding(Key<?> key, BindingEntry bindingEntry) {
    assertNotFinalized();
    if (bindings.containsKey(key)) {
      BindingEntry keyEntry = bindings.get(key);
      errorManager.logError("Double-bound: " + key + ". " + keyEntry.getBindingContext() + ", "
          + bindingEntry.getBindingContext());
      return;
    }

    if (!isClassAccessibleFromGinjector(key.getTypeLiteral())) {
      errorManager.logError("Can not inject an instance of an inaccessible class. Key=" + key);
      return;
    }

    bindings.put(key, bindingEntry);
    unresolved.remove(key);
    unresolvedOptional.remove(key);
    memberInjectRequests.remove(key);
    if (parent != null) {      
      parent.registerChildBinding(key);
    }

    addRequiredKeys(key, bindingEntry.getBinding().getRequiredKeys());

    logger.log(TreeLogger.TRACE, "bound " + key + " to " + bindingEntry);
  }
  
  /**
   * Register the key in the "boundInChildren" set for this injector, and recursively
   * register it with all of the ancestors.
   */
  private void registerChildBinding(Key<?> key) {
    boundInChildren.add(key);
    if (parent != null) {
      parent.registerChildBinding(key);
    }
  }
 
  public boolean isBoundInChild(Key<?> key) {
    return boundInChildren.contains(key);
  }
  
  private void addRequiredKeys(Key<?> key, RequiredKeys requiredKeys) {
    // Resolve optional keys.
    // Clone the returned set so we can safely mutate it
    Set<Key<?>> optionalKeys = new HashSet<Key<?>>(requiredKeys.getOptionalKeys());
    optionalKeys.removeAll(bindings.keySet());
    if (!optionalKeys.isEmpty()) {
      logger.log(TreeLogger.TRACE,
          "Add optional unresolved as dep from binding to " + key + ": " + optionalKeys);
      unresolvedOptional.addAll(optionalKeys);
    }

    // Resolve required keys.
    // Clone the returned set so we can safely mutate it
    Set<Key<?>> nowUnresolved = new HashSet<Key<?>>(requiredKeys.getRequiredKeys());
    nowUnresolved.removeAll(bindings.keySet());
    if (!nowUnresolved.isEmpty()) {
      logger.log(
          TreeLogger.TRACE, "Add unresolved as dep from binding to " + key + ": " + nowUnresolved);
      unresolved.addAll(nowUnresolved);
    }
  }

  private boolean isClassAccessibleFromGinjector(TypeLiteral<?> type) {
    if (ReflectUtil.isPublic(type)) {
      return true;
    }

    Package classPackage = type.getRawType().getPackage();
    Package ginjectorPackage = ginjectorInterface.getRawType().getPackage();
    return classPackage == ginjectorPackage;
  }

  void addStaticInjectionRequest(Class<?> type) {
    assertNotFinalized();
    staticInjectionRequests.add(type);

    // Calculate required bindings and add to unresolved.
    Set<Key<?>> unresolved = new HashSet<Key<?>>();
    Set<Key<?>> unresolvedOptional = new HashSet<Key<?>>();
    for (InjectionPoint injectionPoint : InjectionPoint.forStaticMethodsAndFields(type)) {
      Member member = injectionPoint.getMember();
      if (member instanceof Method) {
        RequiredKeys keys = guiceUtil.getRequiredKeys(
            MethodLiteral.get((Method) member, TypeLiteral.get(member.getDeclaringClass())));
        unresolved.addAll(keys.getRequiredKeys());
        unresolvedOptional.addAll(keys.getOptionalKeys());
      } else if (member instanceof Field) {
        FieldLiteral<?> field =
            FieldLiteral.get((Field) member, TypeLiteral.get(member.getDeclaringClass()));
        Key<?> key = guiceUtil.getKey(field);
        if (guiceUtil.isOptional(field)) {
          unresolvedOptional.add(key);
        } else {
          unresolved.add(key);
        }
      }
    }
    addRequiredKeys(Key.get(type), new RequiredKeys(unresolved, unresolvedOptional));
  }

  public void addFactoryModule(FactoryModule<?> install) {
    factoryModules.add(install);
  }
}
