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

import com.google.gwt.inject.client.binder.GinConstantBindingBuilder;
import com.google.inject.binder.ConstantBindingBuilder;

class ConstantBindingBuilderAdapter implements GinConstantBindingBuilder {
  private final ConstantBindingBuilder guiceBuilder;

  public ConstantBindingBuilderAdapter(ConstantBindingBuilder guiceBuilder) {
    this.guiceBuilder = guiceBuilder;
  }

  public void to(String s) {
    guiceBuilder.to(s);
  }

  public void to(int i) {
    guiceBuilder.to(i);
  }

  public void to(long l) {
    guiceBuilder.to(l);
  }

  public void to(boolean b) {
    guiceBuilder.to(b);
  }

  public void to(double v) {
    guiceBuilder.to(v);
  }

  public void to(float v) {
    guiceBuilder.to(v);
  }

  public void to(short i) {
    guiceBuilder.to(i);
  }

  public void to(char c) {
    guiceBuilder.to(c);
  }

  public void to(Class<?> aClass) {
    guiceBuilder.to(aClass);
  }

  public <E extends Enum<E>> void to(E e) {
    guiceBuilder.to(e);
  }
}
