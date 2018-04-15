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

import com.google.gwt.inject.client.multibindings.InternalModule.SingletonInternalModule;
import com.google.inject.Key;
import com.google.inject.Singleton;

/**
 * A module to bind and expose {@link RuntimeBindingsRegistry} for a multibinding key.
 *
 * @param <T> type of binding
 */
class RuntimeBindingsRegistryModule<T> extends SingletonInternalModule<T> {

  RuntimeBindingsRegistryModule(Key<T> multibindingKey) {
    super(multibindingKey);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void configure() {
    // Safe to use unchecked RuntimeBindingsRegistry.class as its new instances are always a valid
    // substitution for any RuntimeBindingsRegistry<T>.
    bindAndExpose(bindingsRegistry()).to(RuntimeBindingsRegistry.class).in(Singleton.class);
  }
}
