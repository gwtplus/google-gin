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

import com.google.inject.Provider;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;

/**
 * A dummy provider to register for cases like
 * {@code bind().in(scope)} (where there is no target key).
 * Since Gin will use {@code GWT.create()}, we need to
 * make Guice think that something is bound.
 *
 * This class is {@code public} so that {@code BindingProcessor}
 * can detect this.
 *
 * Some details are in
 * <a href="http://code.google.com/p/google-gin/issues/detail?id=22">issue 22</a>.
 */
public class GwtDotCreateProvider<T> implements Provider<T> {
  static <T> ScopedBindingBuilder bind(LinkedBindingBuilder<T> builder) {
    return builder.toProvider(new GwtDotCreateProvider<T>());
  }

  // Private constructor, only created via static method
  private GwtDotCreateProvider() {}

  public T get() {
    throw new AssertionError("should never be actually called");
  }
}
