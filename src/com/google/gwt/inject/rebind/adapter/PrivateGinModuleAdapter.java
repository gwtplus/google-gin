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
package com.google.gwt.inject.rebind.adapter;

import com.google.gwt.inject.client.PrivateGinModule;
import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.PrivateBinder;
import com.google.inject.PrivateModule;
import com.google.inject.internal.ProviderMethodsModule;

/**
 * Makes a {@link PrivateGinModule} available as a {@link Module}.
 */
public class PrivateGinModuleAdapter extends PrivateModule {
  private final PrivateGinModule ginModule;
  private final GinjectorBindings bindings;
  private final boolean hidePrivateBindings;

  public PrivateGinModuleAdapter(PrivateGinModule ginModule, GinjectorBindings bindings, 
      boolean hidePrivateBindings) {
    this.ginModule = ginModule;
    this.bindings = bindings;
    this.hidePrivateBindings = hidePrivateBindings;
  }

  public void configure() {
    Binder binder = binder().skipSources(PrivateGinModuleAdapter.class,
        BinderAdapter.class, PrivateGinModule.class);

    ginModule.configure(new PrivateBinderAdapter((PrivateBinder) binder, 
        bindings == null ? null : bindings.createChildGinjectorBindings(ginModule.getClass()), 
        hidePrivateBindings));

    // Install provider methods from the GinModule
    binder.install(ProviderMethodsModule.forObject(ginModule));
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof PrivateGinModuleAdapter) {
      PrivateGinModuleAdapter other = (PrivateGinModuleAdapter) obj;
      return ginModule.equals(other.ginModule);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return ginModule.hashCode();
  }
}
