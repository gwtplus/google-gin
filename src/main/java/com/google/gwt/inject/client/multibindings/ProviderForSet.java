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
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A provider for the set of multi-binded values.
 *
 * @param <T> type of key for multibinding
 */
class ProviderForSet<T> implements Provider<Set<T>> {

  @Inject @Internal
  private RuntimeBindingsRegistry<Provider<T>> registry;

  @Override
  public Set<T> get() {
    Set<T> set = new LinkedHashSet<T>();
    for (Provider<T> entry : registry.getBindings()) {
      T newValue = entry.get();
      checkState(newValue != null, "Set injection failed due to null element");
      checkState(set.add(newValue) || registry.isDuplicatesPermitted(),
          "Set injection failed due to duplicated element: ", newValue);
    }
    return Collections.unmodifiableSet(set);
  }
}
