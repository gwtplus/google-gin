package com.google.gwt.inject.rebind.resolution;

import com.google.gwt.inject.rebind.binding.Dependency;
import com.google.inject.Key;

import java.util.HashSet;
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
    requiredKeys = new HashSet<Key<?>>(graph.size());
    
    Set<Key<?>> newKeys = new HashSet<Key<?>>();
    addRequiredKeysFor(graph.getOrigin().getDependencies(), newKeys);
    
    while (!newKeys.isEmpty()) {
      requiredKeys.addAll(newKeys);
      Set<Key<?>> discoveredKeys = new HashSet<Key<?>>();
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
