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
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.util.Preconditions;
import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.Dependency;
import com.google.gwt.inject.rebind.binding.ExposedChildBinding;
import com.google.gwt.inject.rebind.resolution.ImplicitBindingCreator.BindingCreationException;
import com.google.gwt.inject.rebind.util.PrettyPrinter;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.assistedinject.Assisted;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Explores the unresolved dependencies for a given Ginjector and builds the {@link DependencyGraph}
 * representing all of the implicit bindings that need to be created to satisfy the dependencies. 
 * 
 * <p>See {@link BindingResolver} for how this fits into the overall algorithm for resolution.
 */
public class DependencyExplorer {
  
  private final TreeLogger logger;
  
  private final Set<Key<?>> visited = new HashSet<Key<?>>();
  
  private final ImplicitBindingCreator bindingCreator;
  
  @Inject
  public DependencyExplorer(ImplicitBindingCreator.Factory bindingCreatorFactory,
      @Assisted TreeLogger logger) {
    this.bindingCreator = bindingCreatorFactory.create(logger);
    this.logger = logger;
  }
  
  /**
   * Explore the unresolved dependencies in the origin Ginjector, and create the corresponding
   * dependency graph.  Also gathers information about key in the dependency graph, such as which
   * Ginjector it is already available on, or what implicit binding was created for it.
   * 
   * @param origin the ginjector to build a dependency graph for
   */
  public DependencyExplorerOutput explore(GinjectorBindings origin) {
    DependencyExplorerOutput output = new DependencyExplorerOutput();
    DependencyGraph.Builder builder = new DependencyGraph.Builder(origin);
    
    for (Dependency edge : origin.getDependencies()) {
      Preconditions.checkState(
          Dependency.GINJECTOR.equals(edge.getSource()) || origin.isBound(edge.getSource()),
          "Expected non-null source %s to be bound in origin!", edge.getSource());
      builder.addEdge(edge);
      if (!edge.getSource().equals(Dependency.GINJECTOR) && visited.add(edge.getSource())) {
        // Need to register where we can find the "source".  Note that this will be always be
        // available somewhere, because it's already available at the origin (that's how we found
        // this dependency).
        PrettyPrinter.log(logger, TreeLogger.DEBUG,
              "Registering %s as available at %s because of the dependency %s", edge.getSource(),
              origin, edge);
        output.preExistingBindings.put(edge.getSource(), 
            locateHighestAccessibleSource(edge.getSource(), origin));
      }

      PrettyPrinter.log(logger, TreeLogger.DEBUG,
          "Exploring from %s in %s because of the dependency %s", edge.getTarget(), origin, edge);
      // Visit the target of the dependency to find additional bindings
      visit(edge.getTarget(), builder, output, origin);
    }
    
    output.setGraph(builder.build());
    return output;
  }
 
  private void visit(Key<?> key, DependencyGraph.Builder builder, 
      DependencyExplorerOutput output, GinjectorBindings origin) {
    if (visited.add(key)) {
      GinjectorBindings accessibleSource = locateHighestAccessibleSource(key, origin);
      if (accessibleSource != null) {
        PrettyPrinter.log(logger, TreeLogger.DEBUG, "Using binding of %s in %s.", key,
            accessibleSource);
        output.preExistingBindings.put(key, accessibleSource);
      } else {
        try {
          Binding binding = bindingCreator.create(key);
          PrettyPrinter.log(logger, TreeLogger.DEBUG, "Implicitly bound %s in %s using %s.", key,
              origin, binding);
          
          for (Dependency edge : binding.getDependencies()) {
            PrettyPrinter.log(logger, TreeLogger.DEBUG, "Following %s", edge);
            builder.addEdge(edge);
            visit(edge.getTarget(), builder, output, origin);
          }
          
          // Do this *after* visiting all dependencies so that that the ordering is post-order
          output.implicitBindings.put(key, binding);
        } catch (BindingCreationException e) {
          PrettyPrinter.log(logger, TreeLogger.DEBUG, "Implicit binding failed for %s: %s", key,
              e.getMessage());
          output.bindingErrors.put(key, e.getMessage());
        } catch (RuntimeException e) {
          logger.log(Type.ERROR, "Exception while visiting " + key);
          throw e;
        }
      }
    }
  }
  
  /**
   * Find the highest binding in the Ginjector tree that could be used to supply the given key.
   * 
   * <p>This takes care not to use a higher parent binding if the parent only has the binding
   * because its exposed from this Ginjector.  This leads to problems because the resolution
   * algorithm won't actually create the binding here if it can just use a parents binding.
   * 
   * @param key The binding to search for
   * @return the highest ginjector that contains the key or {@code null} if none contain it
   */
  private GinjectorBindings locateHighestAccessibleSource(Key<?> key, GinjectorBindings origin) {
    // If we don't already have a binding, and the key is "pinned", it means that this injector
    // is supposed to contain a binding, but it needs to have one created implicitly.  We return
    // null so that we attempt to create the implicit binding.
    if (!origin.isBound(key) && origin.isPinned(key)) {
      return null;
    }

    GinjectorBindings source = null;
    for (GinjectorBindings iter = origin; iter != null; iter = iter.getParent()) {
      if (iter.isBound(key)) {
        source = iter;
      }
    }
    return source;
  }
  
  /**
   * Class that packages up all the output of exploring the unresolved dependencies for a Ginjector.
   * This contains the {@link DependencyGraph} itself, as well as additional information about the
   * nodes.
   */
  public static class DependencyExplorerOutput {
    
    private final Map<Key<?>, GinjectorBindings> preExistingBindings =
        new LinkedHashMap<Key<?>, GinjectorBindings>(); 
    private final LinkedHashMap<Key<?>, Binding> implicitBindings =
        new LinkedHashMap<Key<?>, Binding>();
    private final LinkedHashMap<Key<?>, String> bindingErrors = new LinkedHashMap<Key<?>, String>();
    private DependencyGraph graph;
    
    private DependencyExplorerOutput() {}
    
    void setGraph(DependencyGraph graph) {
      this.graph = graph;
    }
    
    public int size() {
      return graph.size();
    }
   
    /**
     * Returns a map from each {@code Key<?>} that was already available in the injector hierarchy
     * to the Ginjector on which it was found.
     */
    public Map<Key<?>, GinjectorBindings> getPreExistingLocations() {
      return Collections.unmodifiableMap(preExistingBindings);
    }
    
    /**
     * Return the {@code Key<?>}s that weren't already available and for which we successfully
     * created implicit bindings. 
     */
    public Collection<Key<?>> getImplicitlyBoundKeys() {
      return Collections.unmodifiableCollection(implicitBindings.keySet());
    }
    
    /**
     * Returns pairs containing the {@code Key<?>}s that were unavailable from the injector
     * hierarchy but that we were unable to create implicit bindings for and an error message
     * describing the problem we encountered while creating the implicit binding.
     */
    public Collection<Map.Entry<Key<?>, String>> getBindingErrors() {
      return Collections.unmodifiableCollection(bindingErrors.entrySet());
    }
    
    /**
     * Returns map entries containing the {@code Key<?>}s that weren't already available and the
     * {@link Binding} we created (implicitly) for it.  If there was an error creating the implicit
     * binding, the key will not be found here.  Look in {@link #getBindingErrors()} instead.
     */
    public Collection<Map.Entry<Key<?>, Binding>> getImplicitBindings() {
      return Collections.unmodifiableCollection(implicitBindings.entrySet());
    }
    
    /**
     * Removes an implicit binding from the information being tracked.
     */
    public void removeBinding(Key<?> key) {
      implicitBindings.remove(key);
    }
    
    /**
     * Returns the {@link DependencyGraph} containing information about nodes found from the origin.
     */
    public DependencyGraph getGraph() {
      return graph;
    }
  }

  public interface Factory {
    DependencyExplorer create(TreeLogger logger);
  }
}
