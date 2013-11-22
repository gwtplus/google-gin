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

import com.google.gwt.inject.client.binder.GinAnnotatedBindingBuilder;
import com.google.gwt.inject.client.binder.GinLinkedBindingBuilder;
import com.google.gwt.inject.client.binder.GinScopedBindingBuilder;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;

import java.lang.annotation.Annotation;

import javax.inject.Provider;

class AnnotatedBindingBuilderAdapter<T> implements GinAnnotatedBindingBuilder<T> {
  private final AnnotatedBindingBuilder<T> annotatedBindingBuilder;

  public AnnotatedBindingBuilderAdapter(AnnotatedBindingBuilder<T> annotatedBindingBuilder) {
    this.annotatedBindingBuilder = annotatedBindingBuilder;
  }

  public <I extends T> GinScopedBindingBuilder to(Class<I> implementation) {
    return new ScopedBindingBuilderAdapter(annotatedBindingBuilder.to(implementation));
  }

  public <I extends T> GinScopedBindingBuilder to(TypeLiteral<I> implementation) {
    return new ScopedBindingBuilderAdapter(annotatedBindingBuilder.to(implementation));
  }

  public <I extends T> GinScopedBindingBuilder to(Key<I> targetKey) {
    return new ScopedBindingBuilderAdapter(annotatedBindingBuilder.to(targetKey));
  }

  public <I extends Provider<? extends T>> GinScopedBindingBuilder toProvider(Key<I> providerKey) {
    return new ScopedBindingBuilderAdapter(annotatedBindingBuilder.toProvider(providerKey));
  }

  public <I extends Provider<? extends T>> GinScopedBindingBuilder toProvider(
      Class<I> provider) {
    return new ScopedBindingBuilderAdapter(annotatedBindingBuilder.toProvider(provider));
  }

  public GinLinkedBindingBuilder<T> annotatedWith(Class<? extends Annotation> annotation) {
    return new LinkedBindingBuilderAdapter<T>(
        annotatedBindingBuilder.annotatedWith(annotation));
  }

  public GinLinkedBindingBuilder<T> annotatedWith(Annotation annotation) {
    return new LinkedBindingBuilderAdapter<T>(
        annotatedBindingBuilder.annotatedWith(annotation));
  }

  public void asEagerSingleton() {
    GwtDotCreateProvider.bind(annotatedBindingBuilder).asEagerSingleton();
  }

  public void in(Class<? extends Annotation> scopeAnnotation) {
    GwtDotCreateProvider.bind(annotatedBindingBuilder).in(scopeAnnotation);
  }

}
