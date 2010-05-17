/* Copyright 2010 Google Inc.
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

package com.google.gwt.inject.client.assistedinject;

import com.google.gwt.inject.client.GinModule;
import com.google.gwt.inject.client.binder.GinBinder;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import java.util.Map;

/**
 * For internal Gin use only.
 *
 * Fake module that transports assisted inject information from user-defined
 * modules to the bindings processor.
 */
public class FactoryModule<F> implements GinModule {

  private final Key<F> factoryType;
  private final Map<Key<?>, TypeLiteral<?>> bindings;

  public FactoryModule(Map<Key<?>, TypeLiteral<?>> bindings, Key<F> factoryType) {
    this.bindings = bindings;
    this.factoryType = factoryType;
  }

  public Key<F> getFactoryType() {
    return factoryType;
  }

  public Map<Key<?>, TypeLiteral<?>> getBindings() {
    return bindings;
  }

  public void configure(GinBinder binder) {}
}
