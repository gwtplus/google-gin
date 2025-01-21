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
package com.google.gwt.inject.client.binder;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import jakarta.inject.Provider;

/**
 * See the EDSL examples at {@link GinBinder}.
 * 
 * @param <T> type
 */
public interface GinLinkedBindingBuilder<T> extends GinScopedBindingBuilder {

  /**
   * See the EDSL examples at {@link GinBinder}.
   */
  <I extends T> GinScopedBindingBuilder to(Class<I> implementation);

  /**
   * See the EDSL examples at {@link GinBinder}.
   */
  <I extends T> GinScopedBindingBuilder to(TypeLiteral<I> implementation);

  /**
   * See the EDSL examples at {@link GinBinder}.
   */
  <I extends T> GinScopedBindingBuilder to(Key<I> targetKey);

  /**
   * See the EDSL examples at {@link GinBinder}.
   */
  <I extends Provider<? extends T>> GinScopedBindingBuilder toProvider(Class<I> provider);

  /**
   * See the EDSL examples at {@link GinBinder}.
   */
  <I extends Provider<? extends T>> GinScopedBindingBuilder toProvider(Key<I> providerKey);
}
