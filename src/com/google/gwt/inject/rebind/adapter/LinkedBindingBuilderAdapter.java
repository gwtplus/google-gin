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
package com.google.gwt.inject.rebind.adapter;

import com.google.gwt.inject.client.binder.GinLinkedBindingBuilder;
import com.google.gwt.inject.client.binder.GinScopedBindingBuilder;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;

import java.lang.annotation.Annotation;

class LinkedBindingBuilderAdapter<T> implements GinLinkedBindingBuilder<T> {
  private final LinkedBindingBuilder<T> linkedBindingBuilder;

  public LinkedBindingBuilderAdapter(LinkedBindingBuilder<T> linkedBindingBuilder) {
    this.linkedBindingBuilder = linkedBindingBuilder;
  }

  public <I extends T> GinScopedBindingBuilder to(TypeLiteral<I> implementation) {
    return new ScopedBindingBuilderAdapter(linkedBindingBuilder.to(implementation));
  }

  public <I extends T> GinScopedBindingBuilder to(Key<I> targetKey) {
    return new ScopedBindingBuilderAdapter(linkedBindingBuilder.to(targetKey));
  }

  public <I extends Provider<? extends T>> GinScopedBindingBuilder toProvider(Key<I> providerKey) {
    return new ScopedBindingBuilderAdapter(linkedBindingBuilder.toProvider(providerKey));
  }

  public <I extends T> GinScopedBindingBuilder to(Class<I> implementation) {
    return new ScopedBindingBuilderAdapter(linkedBindingBuilder.to(implementation));
  }

  public <I extends Provider<? extends T>> GinScopedBindingBuilder toProvider(
      Class<I> provider) {
    return new ScopedBindingBuilderAdapter(linkedBindingBuilder.toProvider(provider));
  }

  public void asEagerSingleton() {
    GwtDotCreateProvider.bind(linkedBindingBuilder).asEagerSingleton();
  }

  public void in(Class<? extends Annotation> scopeAnnotation) {
    GwtDotCreateProvider.bind(linkedBindingBuilder)
        .in(scopeAnnotation);
  }
}
