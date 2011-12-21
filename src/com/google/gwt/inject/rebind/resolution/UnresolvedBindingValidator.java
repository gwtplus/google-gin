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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.util.Preconditions;
import com.google.gwt.inject.rebind.ErrorManager;
import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.Dependency;
import com.google.gwt.inject.rebind.binding.ParentBinding;
import com.google.gwt.inject.rebind.resolution.DependencyExplorer.DependencyExplorerOutput;
import com.google.gwt.inject.rebind.util.PrettyPrinter;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.assistedinject.Assisted;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Finds and reports errors in the dependency information.  Removes all optional bindings that can't
 * be constructed.
 * 
 * <p>A key is required if there exists a path from a key in the unresolved set of the origin 
 * Ginjector to the key in question that passes only through required dependencies.  A key is
 * optional if every path that leads to the key contains an optional dependency.
 * 
 * This detects the following:
 * <ul>
 * <li>Circular dependencies that do not pass through a Provider or an AsyncProvider.  This is a
 * fatal error, even for optional keys.
 * </li>
 * <li>Bindings that cannot be created without causing a double-binding.  Specifically, bindings for
 * which the key is already bound below, but not exposed to, the Ginjector that needs the key.  This
 * is only an error for required keys; optional keys are removed.
 * </li>
 * <li>Bindings that cannot have implicit dependencies created for them.  This is only an error if
 * the key is required; optional keys are removed.
 * </li>
 * </ul> 
 * 
 * <p>Optional bindings with errors must be removed from the dependency graph before proceeding.
 * This is to prevent them from incorrectly constraining the positions for keys that depend on them.
 * 
 * <p>See {@link BindingResolver} for how this fits into the overall algorithm for resolution.
 */
public class UnresolvedBindingValidator {
  
  private final ErrorManager errorManager;
  private final EagerCycleFinder cycleFinder;
  private final TreeLogger logger;
  
  @Inject
  public UnresolvedBindingValidator(EagerCycleFinder cycleFinder, ErrorManager errorManager,
      @Assisted TreeLogger logger) {
    this.cycleFinder = cycleFinder;
    this.errorManager = errorManager;
    this.logger = logger;
  }
  
  /**
   * Returns an {@link InvalidKeys} object containing information about all the errors that we
   * discovered in required keys, and the set of all optional bindings that should be removed from
   * the graph in order to make it valid.
   */
  public InvalidKeys getInvalidKeys(DependencyExplorerOutput output) {
    Map<Key<?>, String> allInvalidKeys = getAllInvalidKeys(output);
    Set<Key<?>> optionalInvalidKeys = removeOptionalKeys(output, allInvalidKeys);
    optionalInvalidKeys = getKeysToRemove(output.getGraph(), optionalInvalidKeys);
    return new InvalidKeys(allInvalidKeys, optionalInvalidKeys);
  }
  
  /**
   * Returns true if the graph is valid (does not have any cycles or problems creating required
   * keys).  If there are any errors, they will be reported to the global {@link ErrorManager}. 
   */
  public boolean validate(DependencyExplorerOutput output, InvalidKeys invalidKeys) {
    Collection<Map.Entry<Key<?>, String>> invalidRequiredKeys = 
        invalidKeys.getInvalidRequiredKeys();
    for (Map.Entry<Key<?>, String> error : invalidRequiredKeys) {
      reportError(output, error.getKey(), error.getValue());
    }
    
    return !cycleFinder.findAndReportCycles(output.getGraph()) && invalidRequiredKeys.isEmpty();
  }
  
  /**
   * Prune all of the invalid optional keys from the graph.  After this method, all of the keys
   * remaining in the graph are resolvable.
   */
  public void pruneInvalidOptional(DependencyExplorerOutput output, InvalidKeys invalidKeys) {   
    DependencyGraph.GraphPruner prunedGraph = new DependencyGraph.GraphPruner(output.getGraph());
    for (Key<?> key : invalidKeys.getInvalidOptionalKeys()) {
      prunedGraph.remove(key);
      output.removeBinding(key);
    }
    output.setGraph(prunedGraph.update());
  }
  
  /**
   * Returns a map from keys that are invalid to errors explaining why each key is invalid.
   */
  private Map<Key<?>, String> getAllInvalidKeys(DependencyExplorerOutput output) {
    Map<Key<?>, String> invalidKeys = new LinkedHashMap<Key<?>, String>();
    
    // Look for errors in the nodes, and either report the error (if its required) or remove the
    // node (if its optional).
    for (Entry<Key<?>, String> error : output.getBindingErrors()) {
      invalidKeys.put(error.getKey(), "Unable to create or inherit binding: " + error.getValue());
    }
    
    GinjectorBindings origin = output.getGraph().getOrigin();
    for (Key<?> key : output.getImplicitlyBoundKeys()) {
      if (origin.isBoundLocallyInChild(key)) {

        GinjectorBindings child = origin.getChildWhichBindsLocally(key);
        Binding childBinding = child.getBinding(key);

        PrettyPrinter.log(logger, TreeLogger.DEBUG,
            "Marking the key %s as bound in the ginjector %s (implicitly), and in the child"
                + " %s (%s)", key, origin, child, childBinding.getContext());

        // TODO(schmitt): Determine path to binding in child ginjector (requires
        // different DependencyExplorerOutput).
        invalidKeys.put(key,
            PrettyPrinter.format("Already bound in child Ginjector %s. Consider exposing it?",
                child));
      }
    }
    
    return invalidKeys;
  }

  /**
   * Remove optional keys from the {@code invalidKeys} map, and return the the set of optional keys
   * that had problems.
   */
  private Set<Key<?>> removeOptionalKeys(
      DependencyExplorerOutput output, Map<Key<?>, String> invalidKeys) {
    RequiredKeySet requiredKeys = new RequiredKeySet(output.getGraph());
    Set<Key<?>> optionalKeys = new HashSet<Key<?>>();
    // We need to use a for loop instead of a foreach loop because we remove
    // entries as we go (see the call to iterator.remove() below).
    for (Iterator<Map.Entry<Key<?>, String>> iterator = invalidKeys.entrySet().iterator();
        iterator.hasNext();) {
      Map.Entry<Key<?>, String> entry = iterator.next();
      Key<?> key = entry.getKey();
      if (!requiredKeys.isRequired(key)) {
        PrettyPrinter.log(logger, TreeLogger.DEBUG,
            "Removing the optional key %s because it had errors: %s", key, entry.getValue());
        optionalKeys.add(key);
        iterator.remove();
      }
    }
    return optionalKeys;
  }
  
  /**
   * Given the set of optional keys that had problems, compute the set of all optional keys that
   * should be removed.  This may include additional keys, for instance if an optional key requires
   * an invalid key, than the optional key should also be removed.
   * 
   * <p>This will only add optional keys to {@code toRemove}.  Recall that a key is required iff 
   * there exists a path to it that passes through only required edges.  If key Y is optional, and
   * there is a required edge from X -> Y, then X must also be optional.  If X were required, by
   * our definition Y would also be required.
   */
  private Set<Key<?>> getKeysToRemove(DependencyGraph graph, Collection<Key<?>> discovered) {
    Set<Key<?>> toRemove = new HashSet<Key<?>>();
    while (!discovered.isEmpty()) {
      toRemove.addAll(discovered);
      discovered = getRequiredSourcesTargeting(graph, discovered);
      discovered.removeAll(toRemove);
    }
    return toRemove;
  }
  
  /**
   * Returns all of the source keys that have a required dependency on any key in the target set.
   */
  private Collection<Key<?>> getRequiredSourcesTargeting(
      DependencyGraph graph, Iterable<Key<?>> targets) {
    Collection<Key<?>> requiredSources = new HashSet<Key<?>>();
    for (Key<?> target : targets) {
      for (Dependency edge : graph.getDependenciesTargeting(target)) {
        if (!edge.isOptional()) {
          PrettyPrinter.log(logger, TreeLogger.DEBUG, "Removing the key %s because of %s",
              edge.getSource(), edge);
          requiredSources.add(edge.getSource());
        }
      }
    }
    return requiredSources;
  }
  
  private void reportError(DependencyExplorerOutput output, Key<?> key, String error) {
    // TODO(dburrows, bchambers): consider better approaches to pretty-printing keys.
    List<Dependency> path = new PathFinder()
        .onGraph(output.getGraph())
        .withOnlyRequiredEdges(true)
        .addRoots(Dependency.GINJECTOR)
        .addDestinations(key)
        .findShortestPath();

    errorManager.logError("Error injecting %s: %s%n  Path to required node:%n%s",
        key, error, path);
  }
  
  /**
   * Container for information about invalid keys.
   */
  public static class InvalidKeys {
    private final Map<Key<?>, String> invalidRequiredKeys;
    private final Collection<Key<?>> invalidOptionalKeys;
    
    private InvalidKeys(Map<Key<?>, String> invalidRequiredKeys, Set<Key<?>> invalidOptionalKeys) {
      this.invalidRequiredKeys = Collections.unmodifiableMap(invalidRequiredKeys);
      this.invalidOptionalKeys = Collections.unmodifiableCollection(invalidOptionalKeys);
    }
        
    public Set<Entry<Key<?>, String>> getInvalidRequiredKeys() {
      return invalidRequiredKeys.entrySet();
    }
    
    public Collection<Key<?>> getInvalidOptionalKeys() {
      return invalidOptionalKeys;
    }
  }

  public interface Factory {
    UnresolvedBindingValidator create(TreeLogger logger);
  }
}
