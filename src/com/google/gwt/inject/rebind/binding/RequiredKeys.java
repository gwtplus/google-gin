/*
 * Copyright 2009 Google Inc.
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

import java.util.Collections;
import java.util.Set;

/**
 * Simple tuple wrapper for {@link Binding#getRequiredKeys()} return values.
 */
public class RequiredKeys {

  private final Set<Key<?>> requiredKeys;
  private final Set<Key<?>> optionalKeys;

  public RequiredKeys(Set<Key<?>> requiredKeys, Set<Key<?>> optionalKeys) {
    this.requiredKeys = Collections.unmodifiableSet(requiredKeys);
    this.optionalKeys = Collections.unmodifiableSet(optionalKeys);
  }

  public RequiredKeys(Set<Key<?>> requiredKeys) {
    this(requiredKeys, Collections.<Key<?>>emptySet());
  }

  public Set<Key<?>> getRequiredKeys() {
    return requiredKeys;
  }

  public Set<Key<?>> getOptionalKeys() {
    return optionalKeys;
  }
  
  public boolean isEmpty() {
    return requiredKeys.isEmpty() && optionalKeys.isEmpty();
  }
}
