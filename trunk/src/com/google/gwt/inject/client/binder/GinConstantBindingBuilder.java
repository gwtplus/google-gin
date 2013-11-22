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

/**
 * Binds to a constant value. See the EDSL examples at {@link GinBinder}.
 */
public interface GinConstantBindingBuilder {

  /**
   * Binds constant to the given value
   */
  void to(java.lang.String s);

  /**
   * Binds constant to the given value
   */
  void to(int i);

  /**
   * Binds constant to the given value
   */
  void to(long l);

  /**
   * Binds constant to the given value
   */
  void to(boolean b);

  /**
   * Binds constant to the given value
   */
  void to(double v);

  /**
   * Binds constant to the given value
   */
  void to(float v);

  /**
   * Binds constant to the given value
   */
  void to(short i);

  /**
   * Binds constant to the given value
   */
  void to(char c);

  /**
   * Binds constant to the given value
   */
  void to(java.lang.Class<?> aClass);

  /**
   * Binds constant to the given value
   */
  <E extends java.lang.Enum<E>> void to(E e);
}
