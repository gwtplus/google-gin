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
package com.google.inject;

import java.lang.annotation.Annotation;

/**
 * Fake equivalent to Guice's Key (without the methods referencing Type).
 * This hackery is required due to the dual nature of {@code GinModule}
 * implementations.
 *
 * @see com.google.gwt.inject.client.GinModule
 */
@SuppressWarnings({"UnusedDeclaration"})
public class Key<T> {
  protected Key(Class<? extends Annotation> annotationType) {
    throw new UnsupportedOperationException();
  }

  protected Key(Annotation annotation) {
    throw new UnsupportedOperationException();
  }

  protected Key() {
    throw new UnsupportedOperationException();
  }

  public static <T> Key<T> get(Class<T> type) {
    throw new UnsupportedOperationException();
  }

  public static <T> Key<T> get(Class<T> type,
      Class<? extends Annotation> annotationType) {
    throw new UnsupportedOperationException();
  }

  public static <T> Key<T> get(Class<T> type, Annotation annotation) {
    throw new UnsupportedOperationException();
  }

  public static <T> Key<T> get(TypeLiteral<T> typeLiteral) {
    throw new UnsupportedOperationException();
  }

  public static <T> Key<T> get(TypeLiteral<T> typeLiteral,
      Class<? extends Annotation> annotationType) {
    throw new UnsupportedOperationException();
  }

  public static <T> Key<T> get(TypeLiteral<T> typeLiteral,
      Annotation annotation) {
    throw new UnsupportedOperationException();
  }
}