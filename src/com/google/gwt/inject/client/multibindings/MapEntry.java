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

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Represents a runtime binding information for map bindings.
 *
 * @param <K> type of map key
 * @param <V> type of map value
 */
class MapEntry<K, V> {
  private final K key;
  private final Provider<V> valueProvider;

  @Inject
  public MapEntry(@Internal K key, @Internal Provider<V> valueProvider) {
    this.key = key;
    this.valueProvider = valueProvider;
  }

  public K getKey() {
    return key;
  }

  public Provider<V> getValueProvider() {
    return valueProvider;
  }
}
