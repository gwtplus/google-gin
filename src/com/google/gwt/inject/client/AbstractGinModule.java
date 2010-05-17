/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.inject.client;

import com.google.gwt.inject.client.binder.GinAnnotatedBindingBuilder;
import com.google.gwt.inject.client.binder.GinAnnotatedConstantBindingBuilder;
import com.google.gwt.inject.client.binder.GinLinkedBindingBuilder;
import com.google.gwt.inject.client.binder.GinBinder;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * GIN counterpart of Guice's {@code AbstractModule}.
 */
public abstract class AbstractGinModule implements GinModule {
  private GinBinder binder;

  public final void configure(GinBinder binder) {
    this.binder = binder;
    configure();
  }

  protected abstract void configure();

  protected final <T> GinAnnotatedBindingBuilder<T> bind(Class<T> clazz) {
    return binder.bind(clazz);
  }

  protected final <T> GinAnnotatedBindingBuilder<T> bind(TypeLiteral<T> type) {
    return binder.bind(type);
  }

  protected final <T> GinLinkedBindingBuilder<T> bind(Key<T> key) {
    return binder.bind(key);
  }

  protected final GinAnnotatedConstantBindingBuilder bindConstant() {
    return binder.bindConstant();
  }

  protected final void install(GinModule install) {
    binder.install(install);
  }

  protected void requestStaticInjection(Class<?>... types) {
    binder.requestStaticInjection(types);
  }

  protected GinBinder binder() {
    return binder;
  }
}
