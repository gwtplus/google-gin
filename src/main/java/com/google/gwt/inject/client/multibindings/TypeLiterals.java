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
import com.google.inject.util.Types;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for creating TypeLiteral instances.
 */
public class TypeLiterals {

  private TypeLiterals() {}

  @SuppressWarnings("unchecked")
  public static <K, V> TypeLiteral<Map<K, V>> mapOf(TypeLiteral<K> key, TypeLiteral<V> value) {
    return newParameterizedType(Map.class, key, value);
  }

  @SuppressWarnings("unchecked")
  public static <V> TypeLiteral<Set<V>> setOf(TypeLiteral<V> type) {
    return newParameterizedType(Set.class, type);
  }

  @SuppressWarnings("unchecked")
  public static <V> TypeLiteral<Provider<V>> providerOf(TypeLiteral<V> type) {
    return newParameterizedType(Provider.class, type);
  }

  public static TypeLiteral newParameterizedType(Class<?> baseClass, TypeLiteral<?>... literals) {
    Type[] types = new Type[literals.length];
    for (int i = 0; i < types.length; i++) {
      types[i] = literals[i].getType();
    }
    return TypeLiteral.get(
        Types.newParameterizedTypeWithOwner(baseClass.getEnclosingClass(), baseClass, types));
  }
}
