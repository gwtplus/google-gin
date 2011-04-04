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

import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.BindingContext;
import com.google.gwt.inject.rebind.binding.Dependency;
import com.google.gwt.inject.rebind.binding.ParentBinding;
import com.google.gwt.inject.rebind.resolution.DependencyExplorer.DependencyExplorerOutput;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;

import java.util.Map;

/**
 * Adds all of the positioned implicit bindings necessary to satisfy the unresolved bindings in the
 * origin injector to the {@link GinjectorBindings} where we've placed each key.  It installs the
 * following bindings using {@link GinjectorBindings#addBinding(Key, Binding, BindingContext)}:
 * <ul>
 * <li>Each implicit binding created for a key is installed in the Ginjector that 
 * {@link BindingPositioner} decided it should be placed in.
 * </li>
 * <li>For each implicit binding added to a Ginjector G, we add {@link ParentBinding}s to G
 * to make sure it can access any dependencies for the implicit binding.
 * </li>
 * <li>For all the dependencies in the origin, we add  {@link ParentBinding}s for the targets that
 * are not available at the origin (eg, installed higher in the Ginjector hierarchy).
 * </li>
 * </ul>
 * 
 * <p>See {@link BindingResolver} for how this fits into the overall algorithm for resolution.
 */
class BindingInstaller {
  
  private final Provider<ParentBinding> parentBindingProvider;
  private final BindingPositioner positions;

  @Inject
  public BindingInstaller(Provider<ParentBinding> parentBindingProvider,
      BindingPositioner positions) {
    this.parentBindingProvider = parentBindingProvider;
    this.positions = positions;
  }
  
  /**
   * Installs all of the implicit bindings as described {@link BindingInstaller above}.
   * 
   * @param output {@link DependencyExplorerOutput} with information about the unresolved bindings
   *     for the current ginjector.
   */
  public void installBindings(DependencyExplorerOutput output) {
    positions.position(output);
    
    // Install each implicit binding in the correct position
    for (Map.Entry<Key<?>, Binding> entry : output.getImplicitBindings()) {
      installBinding(output.getGraph(), entry.getKey(), entry.getValue());
    }
    
    // Make sure that each of the dependencies needed directly from the origin are available
    GinjectorBindings origin = output.getGraph().getOrigin();
    inheritBindingsForDeps(origin, origin.getDependencies());
  }
  
  /**
   * Adds the given implicit binding in the graph to the injector hierarchy in the position
   * specified by the {@link BindingPositioner}. Also ensures that the dependencies of the implicit
   * binding are available at the chosen position.
   */
  private void installBinding(DependencyGraph graph, Key<?> key, Binding binding) {
    // Figure out where we're putting the implicit entry
    GinjectorBindings implicitEntryPosition = positions.getPosition(key);
    
    // Ensure that the dependencies are available to the ginjector
    inheritBindingsForDeps(implicitEntryPosition, graph.getDependenciesOf(key));
    
    // Now add the implicit binding to the ginjector
    implicitEntryPosition.addBinding(key, binding,
        BindingContext.forText("Implicit binding for key " + key));
  }
  
  /**
   * @param ginjector Ginjector that needs the dependencies
   * @param deps dependencies that are needed
   */
  private void inheritBindingsForDeps(GinjectorBindings ginjector, Iterable<Dependency> deps) {
    for (Dependency dep : deps) {
      ensureAccessible(dep.getTarget(), positions.getPosition(dep.getTarget()), ginjector);
    }
  }
  
  /**
   * Ensure that the binding for key which exists in the parent Ginjector is also available to the
   * child Ginjector.
   */
  private void ensureAccessible(Key<?> key, GinjectorBindings parent, GinjectorBindings child) {
    // Parent will be null if it is was an optional dependency and it couldn't be created.
    if (parent != null && !child.equals(parent) && !child.isBound(key)) {
      ParentBinding parentBinding = parentBindingProvider.get();
      parentBinding.setKey(key);
      parentBinding.setParent(parent);
      BindingContext context = BindingContext.forText("Inheriting " + key + " from parent");
      
      // We don't strictly need all the extra checks in addBinding, but it can't hurt.  We know, for
      // example, that there will not be any unresolved bindings for this key.
      child.addBinding(key, parentBinding, context);
    }
  }
}
