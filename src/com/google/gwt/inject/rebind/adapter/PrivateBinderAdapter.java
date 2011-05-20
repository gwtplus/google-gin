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
package com.google.gwt.inject.rebind.adapter;

import com.google.gwt.inject.client.binder.GinAnnotatedElementBuilder;
import com.google.gwt.inject.client.binder.PrivateGinBinder;
import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.inject.Key;
import com.google.inject.PrivateBinder;
import com.google.inject.TypeLiteral;

/**
 * Provides the {@link PrivateGinBinder} interface and adapts it to a regular Guice
 * {@link PrivateBinder}.
 */
public class PrivateBinderAdapter extends BinderAdapter implements PrivateGinBinder {

  private final PrivateBinder privateBinder;

  PrivateBinderAdapter(PrivateBinder privateBinder, GinjectorBindings bindings) {
    super(privateBinder, bindings);
    this.privateBinder = privateBinder;
  }

  public void expose(Key<?> key) {
    privateBinder.expose(key);
  }

  public GinAnnotatedElementBuilder expose(Class<?> type) {
    return new AnnotatedElementBuilderAdapter(privateBinder.expose(type));
  }

  public GinAnnotatedElementBuilder expose(TypeLiteral<?> type) {
    return new AnnotatedElementBuilderAdapter(privateBinder.expose(type));
  }
}
