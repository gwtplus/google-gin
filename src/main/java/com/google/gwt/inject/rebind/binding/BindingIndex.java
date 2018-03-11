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

/**
 * Simple interface for an index of bound keys in the Ginjector.
 */
public interface BindingIndex {

  /**
   * Returns true if the passed key is bound in the Ginjector.
   * <p/>
   * Note: This only works reliably in the source-generation phase of the
   * ginjector generation since during the binding processing phase not all
   * keys are guaranteed to have been looked at.
   *
   * @param key key to be checked
   * @return true if key is bound.
   */
  public boolean isBound(Key<?> key);
}
