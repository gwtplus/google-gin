/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.inject.client.GinModule;
import com.google.gwt.inject.client.PrivateGinModule;
import com.google.gwt.inject.client.assistedinject.FactoryModule;
import com.google.gwt.inject.client.binder.GinAnnotatedBindingBuilder;
import com.google.gwt.inject.client.binder.GinAnnotatedConstantBindingBuilder;
import com.google.gwt.inject.client.binder.GinBinder;
import com.google.gwt.inject.client.binder.GinLinkedBindingBuilder;
import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;

class BinderAdapter implements GinBinder {
  private final Binder binder;
  private GinjectorBindings bindings;

  BinderAdapter(Binder binder, GinjectorBindings bindings) {
    this.binder = binder;
    this.bindings = bindings;
  }

  public <T> GinAnnotatedBindingBuilder<T> bind(Class<T> clazz) {
    return new AnnotatedBindingBuilderAdapter<T>(binder.bind(clazz));
  }

  public <T> GinAnnotatedBindingBuilder<T> bind(TypeLiteral<T> clazz) {
    return new AnnotatedBindingBuilderAdapter<T>(binder.bind(clazz));
  }

  public <T> GinLinkedBindingBuilder<T> bind(Key<T> key) {
    return new LinkedBindingBuilderAdapter<T>(binder.bind(key));
  }

  public GinAnnotatedConstantBindingBuilder bindConstant() {
    return new AnnotatedConstantBindingBuilderAdapter(binder.bindConstant());
  }

  public void install(GinModule install) {

    // Filtering out fake factory modules.
    if (install instanceof FactoryModule) {
      bindings.addFactoryModule((FactoryModule<?>) install);
    } else {
      // Here we need to take care to ensure that PrivateGinModule uses the appropriate
      // type of adapter, and also get the corresponding Guice private binder.
      final Module moduleAdapter;
      if (install == null) {
        moduleAdapter = null;
      } else if (install instanceof PrivateGinModule) {
        moduleAdapter = new PrivateGinModuleAdapter((PrivateGinModule) install, bindings);
      } else {
        moduleAdapter = new GinModuleAdapter(install, bindings);
      }
      binder.install(moduleAdapter);
    }
  }

  public void requestStaticInjection(Class<?>... types) {
    binder.requestStaticInjection(types);
  }
}
