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

import static com.google.gwt.inject.client.multibindings.Preconditions.checkState;

import com.google.inject.Inject;
import com.google.inject.Provider;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A provider for the map of a key&value binding pair.
 *
 * @param <K> type of key for map binding
 * @param <V> type of value for map binding
 */
class ProviderForMap<K, V> implements Provider<Map<K, V>> {

  @Inject @Internal
  private RuntimeBindingsRegistry<MapEntry<K, V>> registry;

  @Override
  public Map<K, V> get() {
    Map<K, V> map = new LinkedHashMap<K, V>();
    for (MapEntry<K, V> entry : registry.getBindings()) {
      V previous = map.put(entry.getKey(), entry.getValueProvider().get());
      checkState(previous == null || registry.isDuplicatesPermitted(),
          "Map injection failed due to duplicated key: ", entry.getKey());
    }
    return Collections.unmodifiableMap(map);
  }
}
