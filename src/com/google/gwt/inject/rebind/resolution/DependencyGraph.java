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
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.Dependency;
import com.google.inject.Key;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A graph of the dependency information between types that need to be injected
 * at a given injector, called the origin.
 *
 * <p>
 * A {@link DependencyGraph} consists of a set of dependencies linking
 * dependency nodes (keys). Each edge explains how a particular node came to be
 * required: for instance, a key may be required in the {@code @Inject}
 * constructor of a class, or it might be required by the Ginjector. There are
 * two kinds of dependency nodes:
 * <ul>
 * <li>Pre-existing dependency nodes contain a {@link Key} and a
 * {@link GinjectorBindings}. They represent keys that are already available on
 * some Ginjector in the hierarchy and visible to the origin.</li>
 * <li>Internal dependency nodes represent bindings that are not already
 * created/available, but that can have implicit bindings created for them. They
 * contain the {@link Key} that the binding is for and the {@link Binding} that
 * can be created.</li>
 * <li>In addition, some nodes may represent errors. These contain a {@link Key}
 * and a {@link String} describing the error. These nodes are created for keys
 * that do not already exist and for which we cannot create an implicit binding.
 * </li>
 * </ul>
 */
public class DependencyGraph {

  /**
   * For each key, this stores the set of {@link Dependency}s that have the key as their source.
   */
  private final Map<Key<?>, Set<Dependency>> dependenciesOf;

  /**
   * For each key, this stores the set of {@link Dependency}s that have the key as their target.
   */
  private final Map<Key<?>, Set<Dependency>> dependenciesTargeting;

  private final GinjectorBindings origin;

  private DependencyGraph(GinjectorBindings origin, Map<Key<?>, Set<Dependency>> dependenciesOf,
      Map<Key<?>, Set<Dependency>> dependenciesTargeting) {
    this.origin = origin;
    this.dependenciesOf = dependenciesOf;
    this.dependenciesTargeting = dependenciesTargeting;
  }

  public int size() {
    return dependenciesTargeting.size();
  }

  public GinjectorBindings getOrigin() {
    return origin;
  }
  
  public Collection<Dependency> getDependenciesOf(Key<?> key) {
    Set<Dependency> dependencySet = dependenciesOf.get(key);
    return dependencySet == null ? Collections.<Dependency>emptyList() 
        : Collections.unmodifiableCollection(dependencySet);
  }

  public Collection<Dependency> getDependenciesTargeting(Key<?> key) {
    Collection<Dependency> dependencySet = dependenciesTargeting.get(key);
    return dependencySet == null ? Collections.<Dependency>emptyList() 
        : Collections.unmodifiableCollection(dependencySet);
  }
  
  /**
   * Returns all the keys that appear in the Dependency Graph, other than the "common root", 
   * {@link Dependency#GINJECTOR}.
   */
  public Iterable<Key<?>> getAllKeys() {
    // All keys in the graph should be reachable from the Ginjector, which means they must appear as
    // the target of some dependency.  Thus, the keyset covers all nodes.
    return dependenciesTargeting.keySet();
  }

  public static class Builder {
    private final Map<Key<?>, Set<Dependency>> dependenciesOf;
    private final Map<Key<?>, Set<Dependency>> dependenciesTargeting;
    private final GinjectorBindings origin;

    /**
     * Creates a Builder that constructs a new DependencyGraph for the given origin Ginjector.
     *
     * @param origin the origin Ginjector
     */
    public Builder(GinjectorBindings origin) {
      this.origin = origin;
      // Use linked hash maps so that error messages (and tests) are stable
      this.dependenciesOf = new LinkedHashMap<Key<?>, Set<Dependency>>();
      this.dependenciesTargeting = new LinkedHashMap<Key<?>, Set<Dependency>>();
    }
   
    public Builder addEdge(Dependency dependency) {
      addTo(dependency.getSource(), dependency, dependenciesOf);
      addTo(dependency.getTarget(), dependency, dependenciesTargeting);
      return this;
    }

    private void addTo(
        Key<?> key, Dependency dependency, Map<Key<?>, Set<Dependency>> dependencies) {
      Set<Dependency> dependencySet = dependencies.get(key);
      if (dependencySet == null) {
        // Use LinkedHashSet so that error messages (and tests) are stable
        dependencySet = new LinkedHashSet<Dependency>();
        dependencies.put(key, dependencySet);
      }
      dependencySet.add(dependency);
    }

    public DependencyGraph build() {
      return new DependencyGraph(origin, dependenciesOf, dependenciesTargeting);
    }
  }

  public static class GraphPruner {

    private final DependencyGraph source;

    /**
     * Create a {@link GraphPruner} for building a new DependencyGraph by (destructively!) removing
     * edges from an existing DependencyGraph.
     *
     * @param source the DependencyGraph to use as the base (will be mutated)
     */
    public GraphPruner(DependencyGraph source) {
      this.source = source;
    }

    /**
     * Removes the given key, all its incoming edges, and all its outgoing edges, from the graph.
     */
    public GraphPruner remove(Key<?> key) {
      for (Dependency dependency : source.getDependenciesOf(key)) {
        removeFrom(dependency.getTarget(), dependency, source.dependenciesTargeting);
      }
      
      for (Dependency dependency : source.getDependenciesTargeting(key)) {
        removeFrom(dependency.getSource(), dependency, source.dependenciesOf);
      }
      
      return this;
    }
    
    private void removeFrom(Key<?> key, Dependency dependency, 
        Map<Key<?>, Set<Dependency>> dependencies) {
      Set<Dependency> dependencySet = dependencies.get(key);
      Preconditions.checkNotNull(dependencySet,
          "Expected dependency set to be present for dependency %s", key);
      if (!dependencySet.remove(dependency)) {
        throw new IllegalStateException(String.format(
            "Expected %s to be present in the dependency set", dependency));
      }
      
      if (dependencySet.isEmpty()) {
        dependencies.remove(key);
      }
    }

    public DependencyGraph update() {
      return source;
    }
  }
}
