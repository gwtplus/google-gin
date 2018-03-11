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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A very simple multimap implementation that supports conversion to unmodifiable map.
 */
class SimpleMultimap<K, V> extends LinkedHashMap<K, Set<V>> {

  public void putItem(K key, V value) {
    Set<V> set = get(key);
    if (set == null) {
      set = new LinkedHashSet<V>();
      put(key, set);
    }
    set.add(value);
  }

  public Map<K, Set<V>> toUnmodifiable() {
    for (Entry<K, Set<V>> entry : entrySet()) {
      entry.setValue(Collections.unmodifiableSet(entry.getValue()));
    }
    return Collections.unmodifiableMap(this);
  }
}
