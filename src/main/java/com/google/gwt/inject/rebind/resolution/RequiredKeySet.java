/*
 * Copyright 2013 Google Inc.
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

import com.google.gwt.inject.rebind.binding.Dependency;
import com.google.inject.Key;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Computes the set of required keys in a given {@link DependencyGraph}.  A key is considered 
 * required if there exists a path from the dependencies of the origin Ginjector to the key that
 *  only passes through required edges.
 */
public class RequiredKeySet {
  
  private Set<Key<?>> requiredKeys;
  private final DependencyGraph graph;
  
  public RequiredKeySet(DependencyGraph graph) {
    this.graph = graph;
  }
  
  public boolean isRequired(Key<?> key) {
    if (requiredKeys == null) {
      computeRequiredKeys();
    }
    return requiredKeys.contains(key);
  }
  
  private void computeRequiredKeys() {
    requiredKeys = new LinkedHashSet<Key<?>>(graph.size());
    
    Set<Key<?>> newKeys = new LinkedHashSet<Key<?>>();
    addRequiredKeysFor(graph.getOrigin().getDependencies(), newKeys);
    
    while (!newKeys.isEmpty()) {
      requiredKeys.addAll(newKeys);
      Set<Key<?>> discoveredKeys = new LinkedHashSet<Key<?>>();
      for (Key<?> key : newKeys) {
        addRequiredKeysFor(graph.getDependenciesOf(key), discoveredKeys);
      }
      newKeys = discoveredKeys;
      newKeys.removeAll(requiredKeys);
    }
  }
  
  private void addRequiredKeysFor(Iterable<Dependency> edges, Set<Key<?>> discoveredKeys) {
    for (Dependency edge : edges) {
      if (!edge.isOptional()) {
        discoveredKeys.add(edge.getTarget());
      }
    }
  }
}
