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
package com.google.gwt.inject.client.multibindings;

import com.google.gwt.inject.client.binder.GinBinder;
import com.google.gwt.inject.client.binder.GinConstantBindingBuilder;
import com.google.gwt.inject.client.binder.GinLinkedBindingBuilder;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * A helper to register a binding of T to {@link RuntimeBindingsRegistry}.
 * <p>
 * {@code BindingRecorder} converts compile time multibinding information into runtime binding
 * information with the help of eager singletons and generics.
 */
class BindingRecorder {

  private final GinBinder binder;

  public <T> BindingRecorder(GinBinder binder, Key<T> multibindingKey) {
    BindingRegistererModule<T> module = new BindingRegistererModule<T>(multibindingKey);
    binder.install(module);
    this.binder = module.getBinder();
  }

  public <T> GinLinkedBindingBuilder<T> bind(TypeLiteral<T> type) {
    return binder.bind(type).annotatedWith(Internal.class);
  }

  public <T> GinConstantBindingBuilder bindConstant() {
    return binder.bindConstant().annotatedWith(Internal.class);
  }

  /**
   * A module that configures BindingRegisterer in a way that type bound to 'T' in {@link #binder}
   * will be available at runtime via BindingRegistry<T>.
   */
  // TODO(user): not private due to http://code.google.com/p/google-gin/issues/detail?id=184
  static class BindingRegistererModule<T> extends InternalModule<T> {

    private GinBinder binder;

    public BindingRegistererModule(Key<T> multibindingKey) {
      super(multibindingKey);
    }

    @Override
    protected void configure() {
      this.binder = binder();
      bindInternalBindingsRegistry();
      bind(registererOf(multibindingType())).asEagerSingleton();
    }

    public GinBinder getBinder() {
      return binder;
    }
  }

  static class BindingRegisterer<T> {
    @Inject
    public BindingRegisterer(@Internal RuntimeBindingsRegistry<T> registry, @Internal T binding) {
      registry.register(binding);
    }
  }

  private static TypeLiteral<?> registererOf(TypeLiteral<?> type) {
    return TypeLiterals.newParameterizedType(BindingRegisterer.class, type);
  }
}
