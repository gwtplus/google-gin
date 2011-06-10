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
package com.google.gwt.inject.rebind.resolution;

import com.google.gwt.dev.util.Preconditions;
import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.binding.Dependency;
import com.google.gwt.inject.rebind.resolution.DependencyExplorer.DependencyExplorerOutput;
import com.google.inject.Inject;
import com.google.inject.Key;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Given the dependency information about all unresolved (and required and optional) keys needed by
 * a given Ginjector (the origin), this determines the position for each key.  This class computes
 * Level(k), the Ginjector in which a binding of the key k is available or will be created.  It is
 * subject to the following constraints: 
 * <ul>
 * <li>For each already available key k, Level(k) is the highest (closest to the root) 
 * Ginjector that the the key is available from.
 * </li>
 * <li>For each key k that is not already bound, Level(k) is the highest Ginjector that satisfies
 * the following constraints:
 * <ol>
 *   <li>For all dependencies d of the implicit binding for k, Level(k) is no higher than
 *   Level(d).  This is necessary to ensure that all of values that the binding depends on are
 *   available to the ginjector at Level(k).
 *   </li>
 *   <li>There can be no descendant Ginjector below Level(k) that also has a binding for k.  This 
 *   prevents us from creating a binding that causes double-binding errors.
 *   
 *   <p>Consider a simple injector hierarchy with two child-injectors in which we're trying to
 *   materialize key K for one of the children.  If the sibling already has a binding for K, even if
 *   it's not exposed, we cannot materialize K at the parent; we must instead place it in the child
 *   that needs it.
 *   </li>
 * </ol>
 * </li>
 * </ul>
 * 
 * <p>The positions of all the implicit bindings are solved simultaneously, by starting with an
 * initial guess for each key that starts with them placed as high as possible without causing any
 * double-binding problems, and then iterating over all the keys and moving them down according to
 * the following equation:
 * {@code
 *    Level(k) = lowest(Level(k) U {Level(d) | d \in deps(k)})
 * }
 * 
 * <p>One exception to the rules above is bindings that are needed in the origin and exposed to the
 * parent.  Instead of installing and using them from "as high as possible", we need to install
 * them in the origin, but still use them from "as high as possible."  These bindings are treated
 * specially, using {@link installOverrides} to separate the use-location from the install-location.
 *
 * <p>See {@link BindingResolver} for how this fits into the overall algorithm for resolution.
 */
class BindingPositioner {
  
  /**
   * The keys that still need to be positioned.  We use a LinkedHashSet so that we visit keys in the
   * order they were added, but disallow a key be queued multiple times.
   */
  private final LinkedHashSet<Key<?>> workqueue = new LinkedHashSet<Key<?>>();
  
  /**
   * Map containing the current (and eventually correct) positions for each key.
   */
  private Map<Key<?>, GinjectorBindings> positions = new HashMap<Key<?>, GinjectorBindings>();
  
  /**
   * Stores positions for keys that need to be placed below where they are actually installed.  This
   * is the case for keys that are exposed to parents.  Specifically, if a child module binds and
   * exposes Foo, then we should install Foo in the child injector, while any uses of Foo (such as
   * by Bar) can still be created in the parent).
   */
  private Map<Key<?>, GinjectorBindings> installOverrides =
      new HashMap<Key<?>, GinjectorBindings>();

  /**
   * The output from {@link DependencyExplorer} which includes the dependency graph, and also the
   * positions for all already available keys.
   */
  private DependencyExplorerOutput output;

  @Inject
  public BindingPositioner() {}
  
  public void position(DependencyExplorerOutput output) {
    Preconditions.checkState(this.output == null, "Should not call position more than once");
    this.output = output;

    computeInitialPositions();
    workqueue.addAll(output.getImplicitlyBoundKeys());
    calculateExactPositions();
  }
 
  /**
   * Returns the Ginjector where the binding for key should be placed, or null if the key was
   * removed from the dependency graph earlier.
   */
  public GinjectorBindings getInstallPosition(Key<?> key) {
    Preconditions.checkNotNull(positions,
        "Must call position before calling getInstallPosition(Key<?>)");
    GinjectorBindings position = installOverrides.get(key);
    if (position == null) {
      position = positions.get(key);
    }
    return position;
  }

  public GinjectorBindings getAccessPosition(Key<?> key) {
    Preconditions.checkNotNull(positions,
        "Must call position before calling getAccessPosition(Key<?>)");
    return positions.get(key);
  }

  /**
   * Place an initial guess in the position map that places each implicit binding as high as
   * possible in the injector tree without causing double binding.
   */
  private void computeInitialPositions() {
    positions.putAll(output.getPreExistingLocations());
    for (Key<?> key : output.getImplicitlyBoundKeys()) {
      positions.put(key, computeInitialPosition(key));
    }
  }
  
  /**
   * Returns the highest injector that we could possibly position the key at without causing a 
   * double binding. 
   */
  private GinjectorBindings computeInitialPosition(Key<?> key) {
    GinjectorBindings initialPosition = output.getGraph().getOrigin();

    // If the key is pinned (explicitly bound) at the origin, we may be in a situation where we need
    // to install a binding at the origin, even though we should *use* the binding form a higher
    // location.
    // If key is already bound in parent, there is a reason that {@link DependencyExplorer}
    // chose not to use that binding.  Specifically, it implies that the key is exposed to the
    // parent from the origin.  While we are fine using the higher binding, it is still necessary
    // to install the binding in the origin.
    if (initialPosition.isPinned(key)) {
      installOverrides.put(key, initialPosition);
    }

    while (initialPosition.getParent() != null 
        && !initialPosition.getParent().isBoundInChild(key)) {
      initialPosition = initialPosition.getParent();
    }
    return initialPosition;
  }
    
  /**
   * Iterates on the position equation, updating each binding in the queue and re-queueing nodes
   * that depend on any node we move.  This will always terminate, since we only re-queue when we
   * make a change, and there are a finite number of entries in the injector hierarchy.
   */
  private void calculateExactPositions() {
    while (!workqueue.isEmpty()) {
      Key<?> key = workqueue.iterator().next();
      workqueue.remove(key);
      
      Set<GinjectorBindings> injectors = getSourceGinjectors(key);
      injectors.add(positions.get(key));
      GinjectorBindings newPosition = lowest(injectors);
      
      if (positions.put(key, newPosition) != newPosition) {
        // We don't care if GINJECTOR is present, as its Ginjector will resolve to "null", which
        // will never be reached on the path from the origin up to the root, therefore it won't
        // actually constrain anything.
        for (Dependency dependency : output.getGraph().getDependenciesTargeting(key)) {
          workqueue.add(dependency.getSource());
        }
      }
    }
  }
  
  /**
   * Returns the injectors where the dependencies for node are currently placed.
   */
  private Set<GinjectorBindings> getSourceGinjectors(Key<?> key) {
    Set<GinjectorBindings> sourceInjectors = new HashSet<GinjectorBindings>();
    for (Dependency dep : output.getGraph().getDependenciesOf(key)) {
      sourceInjectors.add(positions.get(dep.getTarget()));
    }
    return sourceInjectors;
  }
  
  /**
   * Returns the member of {@code sources} closest to the origin.
   */
  private GinjectorBindings lowest(Set<GinjectorBindings> sources) {
    GinjectorBindings lowest = output.getGraph().getOrigin();
    while (!sources.contains(lowest)) {
      lowest = lowest.getParent();
    }
    return Preconditions.checkNotNull(lowest, "Should never make it to null");
  }
}
