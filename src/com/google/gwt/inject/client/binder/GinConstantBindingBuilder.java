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

public interface GinConstantBindingBuilder {

  void to(java.lang.String s);

  void to(int i);

  void to(long l);

  void to(boolean b);

  void to(double v);

  void to(float v);

  void to(short i);

  void to(char c);

  void to(java.lang.Class<?> aClass);

  <E extends java.lang.Enum<E>> void to(E e);
}
