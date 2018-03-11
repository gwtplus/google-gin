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
package com.google.gwt.inject.client;

import com.google.gwt.inject.client.binder.GinAnnotatedBindingBuilder;
import com.google.gwt.inject.client.binder.GinAnnotatedConstantBindingBuilder;
import com.google.gwt.inject.client.binder.GinAnnotatedElementBuilder;
import com.google.gwt.inject.client.binder.GinBinder;
import com.google.gwt.inject.client.binder.GinLinkedBindingBuilder;
import com.google.gwt.inject.client.binder.PrivateGinBinder;
import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.TypeLiteral;

/**
 * Gin counterpart of Guice's {@link PrivateModule}.
 */
public abstract class PrivateGinModule implements GinModule {

  private PrivateGinBinder binder = null;

  public void configure(GinBinder binder) {
    if (this.binder != null) {
      throw new IllegalStateException("Re-entry is not allowed");
    }

    this.binder = (PrivateGinBinder) binder;
    try {
      configure();
    } finally {
      this.binder = null;
    }
  }

  /**
   * Creates bindings and other configurations private to this module.  Use {@link #expose(Class)
   * expose()} to make the bindings in this module available externally.
   */
  protected abstract void configure();

  /** 
   * Makes the binding for {@code key} available to other modules and the injector.
   */
  protected final <T> void expose(Key<T> key) {
    binder.expose(key);
  }

  /** 
   * Makes a binding for {@code type} available to other modules and the injector.  Use {@link
   * GinAnnotatedElementBuilder#annotatedWith(Class) annotatedWith()} to expose {@code type}
   * with a binding annotation.
   */
  protected final GinAnnotatedElementBuilder expose(Class<?> type) {
    return binder.expose(type);
  }

  /** 
   * Makes a binding for {@code type} available to other modules and the injector.  Use {@link
   * GinAnnotatedElementBuilder#annotatedWith(Class) annotatedWith()} to expose {@code type}
   * with a binding annotation.
   */
  protected final GinAnnotatedElementBuilder expose(TypeLiteral<?> type) {
    return binder.expose(type);
  }

  /** 
   * Returns the current binder.
   */
  protected final PrivateGinBinder binder() {
    return binder;
  }

  // Everything below is copied from AbstractGinModule
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
}
