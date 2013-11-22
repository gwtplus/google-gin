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

import com.google.inject.PrivateModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

/**
 * Bindings for components of the resolver.
 */
public class ResolutionModule extends PrivateModule {

  @Override
  protected void configure() {
    install(new FactoryModuleBuilder()
        .build(BindingInstaller.Factory.class));
    install(new FactoryModuleBuilder()
        .build(BindingPositioner.Factory.class));
    install(new FactoryModuleBuilder()
        .build(DependencyExplorer.Factory.class));
    install(new FactoryModuleBuilder()
        .build(ImplicitBindingCreator.Factory.class));
    install(new FactoryModuleBuilder()
        .build(UnresolvedBindingValidator.Factory.class));

    bind(BindingResolver.class);
    expose(BindingResolver.class);
  }
}
