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

import java.util.ArrayList;
import java.util.List;

/**
 * A registry to keep track of all bindings for a multibinding type in the runtime.
 * <p>
 * During runtime, when the Ginjector is instantiated, all registerers marked asEagerSingleton will
 * be immediately instantiated, that will result in registration of all bindings with this class.
 * These bindings are later used by providers to construct the actual sets & maps.
 *
 * @param <T> type of the binding to track
 */
class RuntimeBindingsRegistry<T> {

  private final List<T> bindings = new ArrayList<T>();
  private boolean duplicatesPermitted;

  public void register(T binding) {
    bindings.add(binding);
  }

  public void permitDuplicates() {
    duplicatesPermitted = true;
  }

  public List<T> getBindings() {
    return bindings;
  }

  public boolean isDuplicatesPermitted() {
    return duplicatesPermitted;
  }
}
