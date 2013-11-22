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

import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

import java.util.Map;
import java.util.Set;

/**
 * Fake equivalent for TypeLiterals.
 * <p>
 * This is not exactly a Guice API but a helper around Guice APIs and requires
 * to be super-source for the same reasons.
 */
public class TypeLiterals {

  public static <K, V> TypeLiteral<Map<K, V>> mapOf(TypeLiteral<K> key, TypeLiteral<V> value) {
    throw new UnsupportedOperationException("Should never be called in client code.");
  }

  public static <V> TypeLiteral<Set<V>> setOf(TypeLiteral<V> type) {
    throw new UnsupportedOperationException("Should never be called in client code.");
  }

  public static <V> TypeLiteral<Provider<V>> providerOf(TypeLiteral<V> type) {
    throw new UnsupportedOperationException("Should never be called in client code.");
  }

  public static TypeLiteral newParameterizedType(Class<?> baseClass, TypeLiteral<?>... literals) {
    throw new UnsupportedOperationException("Should never be called in client code.");
  }
}
