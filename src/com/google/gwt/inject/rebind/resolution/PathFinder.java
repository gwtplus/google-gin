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
import com.google.gwt.inject.rebind.binding.Dependency;
import com.google.inject.Key;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Finds the shortest path from the edges in the root set to any of one or more destination keys.
 * 
 * <p>This is used in {@link EagerCycleFinder} and {@link UnresolvedBindingValidator} for explaining
 * why a given error/cycle was reachable from the Ginjector.
 */
public class PathFinder {
  
  private DependencyGraph graph;
  private Collection<Key<?>> destinations = new LinkedHashSet<Key<?>>();
  private Collection<Key<?>> roots = new LinkedHashSet<Key<?>>();
  private boolean onlyRequiredEdges;

  /**
   * For every key that is discovered during the Breadth-first search, this points to the edge that
   * discovered it.  We don't need to revisit(or requeue) any keys that are already defined here,
   * because any new edge leading to back to the given key will at best be longer than the current
   * path.
   */
  private Map<Key<?>, Dependency> visited;
  
  /**
   * The nodes remaining to visit.
   */
  private Queue<Key<?>> workQueue;

  public PathFinder() {}
  
  public PathFinder onGraph(DependencyGraph graph) {
    this.graph = graph;
    return this;
  }
  
  public PathFinder addRoots(Key<?>... roots) {
    Collections.addAll(this.roots, roots);
    return this;
  }
  
  /**
   * Add destinations to be used for the next search.  The shortest path from the unresolved set
   * to any member of the destination set will be returned from {@link #findShortestPath()}.
   */
  public PathFinder addDestinations(Key<?>... destinations) {
    Collections.addAll(this.destinations, destinations);
    return this;
  }
  
  /**
   * @param onlyRequiredEdges if true, only required edges will be considered when searching for the
   *     path
   */
  public PathFinder withOnlyRequiredEdges(boolean onlyRequiredEdges) {
    this.onlyRequiredEdges = onlyRequiredEdges;
    return this;
  }
  
  /**
   * Find the shortest path from an unresolved edge in the roots to a key in the destinations.
   * 
   * <p>Implemented as a Breadth-first search from the destination set back to the origin. 
   * 
   * @return the shortest path from the roots to any of the destinations specified that passes
   *     through edges meeting the criteria; can be empty if destination is already in the root
   *     set, or null if no path exists
   */
  public List<Dependency> findShortestPath() {
    Preconditions.checkNotNull(graph, "Must call onGraph(DependencyGraph) before findShortestPath");
    Preconditions.checkState(!roots.isEmpty(), 
        "Must call addRoots(Key<?>...) before findShortestPath");
    Preconditions.checkState(!destinations.isEmpty(),
        "Must call addDestinations(Key<?>...) before findShortestPath");
    
    visited = new LinkedHashMap<Key<?>, Dependency>();
    workQueue = new LinkedList<Key<?>>();
    
    // Populate the workqueue with our initial destination keys.  If any of them are in the root
    // set, we can return early.
    for (Key<?> key : destinations) {
      visited.put(key, null);
      if (roots.contains(key)) {
        return getPathFor(key);
      }
      workQueue.add(key);
    }
    
    // Perform a BFS looking for a path back to a root edge
    while (!workQueue.isEmpty()) {
      Key<?> key = workQueue.remove();
      
      for (Dependency edge : graph.getDependenciesTargeting(key)) {
        if (isEdgeUsable(edge)) {
          Key<?> sourceKey = edge.getSource();
          if (!visited.containsKey(sourceKey)) {
            workQueue.add(sourceKey);
            visited.put(sourceKey, edge);
          
            // Check for early termination
            if (roots.contains(sourceKey)) {
              return getPathFor(sourceKey);
            }
          }
        }
      }
    }
    
    // Shouldn't be possible, unless the only paths reaching the destinations take optional edges
    // and requiredOnly is true.
    return null;
  }
  
  private List<Dependency> getPathFor(Key<?> rootKey) {
    List<Dependency> result = new ArrayList<Dependency>();
    
    // Now, add the edges from the BFS path
    Dependency edge = visited.get(rootKey);
    while (edge != null) {
      result.add(edge);
      edge = visited.get(edge.getTarget());
    }
    return result;
  }
  
  /**
   * Returns true if the given edge meets our criteria for use, false otherwise.
   */
  private boolean isEdgeUsable(Dependency edge) {
    return !edge.isOptional() || !onlyRequiredEdges;
  }
}
