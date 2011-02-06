/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.inject.rebind.binding;

import com.google.inject.Key;
import com.google.inject.spi.Element;

/**
 * Describes the context in which a binding was created.
 */
public class BindingContext {

  private final String context;

  protected BindingContext(String context) {
    this.context = context;
  }

  @Override
  public String toString() {
    return context;
  }

  /**
   * Create a {@code BindingContext} storing the context of the given Guice
   * {@link Element}.
   */
  public static BindingContext forElement(Element element) {
    return new BindingContext(element.getSource().toString());
  }

  /**
   * Create a {@code BindingContext} storing the given text as its context.
   */
  public static BindingContext forText(String context) {
    return new BindingContext(context);
  }
  
  /**
   * Create a {@code BindingContext} storing the given key as the "key" that
   * required it.
   */
  public static BindingContext forDependency(Key<?> dependency, Key<?> key) {
    return new BindingContext(dependency + " is dependency of binding for " + key);
  }
}
