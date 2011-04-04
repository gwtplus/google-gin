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
import com.google.gwt.inject.rebind.binding.ParentBinding;
import com.google.gwt.inject.rebind.resolution.DependencyExplorer.DependencyExplorerOutput;
import com.google.gwt.inject.rebind.resolution.UnresolvedBindingValidator.InvalidKeys;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Drives the top level Binding Resolution algorithm.  This performs the following steps:
 * <ol>
 * <li>Create a dependency graph representing all of the unresolved keys (required and optional)
 * for a Ginjector in the hierarchy.  This graph will have internal nodes for all of the implicit
 * bindings that need to be created, and leaf nodes (no outgoing edges) for all bindings that are
 * already available to the Ginjector.  See {@link DependencyExplorer}.
 * </li>
 * <li>Verify that there are no errors in the dependency graph.  Errors are detected and reported as
 * described in {@link UnresolvedBindingValidator} which also makes use of {@link EagerCycleFinder}.
 * </li>
 * <li>Determine which injector each of the implicit bindings should be placed in, according to the
 * constraints described in {@link BindingPositioner}.
 * </li>
 * <li>Install each implicit binding, and any {@link ParentBinding}s necessary to inherit 
 * dependencies from higher in the hierarchy, to the Ginjectors.  See {@link BindingInstaller}.
 * </ol>
 */
public class BindingResolver {
 
  private final Provider<DependencyExplorer> explorerProvider;
  private final Provider<UnresolvedBindingValidator> validatorProvider;
  private final Provider<BindingInstaller> installerProvider;
  
  @Inject
  public BindingResolver(Provider<DependencyExplorer> explorerProvider,
      Provider<UnresolvedBindingValidator> validatorProvider,
      Provider<BindingInstaller> installerProvider) {
    this.explorerProvider = explorerProvider;
    this.validatorProvider = validatorProvider;
    this.installerProvider = installerProvider;
  }
  
  public void resolveBindings(GinjectorBindings origin) {
    // Use providers so that the instances are cleaned up after this method.  This ensures that even
    // though BindingResolver may be held on to (eg, {@link GinjectorBindings}, we won't leak
    // memory used for temporary storage during resolution.
    DependencyExplorerOutput output = explorerProvider.get().explore(origin);
    
    UnresolvedBindingValidator validator = validatorProvider.get();
    InvalidKeys invalidKeys = validator.getInvalidKeys(output);
    if (validator.validate(output, invalidKeys)) {
      validator.pruneInvalidOptional(output, invalidKeys);
      installerProvider.get().installBindings(output);
    }
  }
}
