/*
 * Copyright 2008 Google Inc.
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
import com.google.gwt.inject.rebind.NameGenerator;

import java.util.Set;

/**
 * Interface used by {@code InjectorGeneratorImpl} to represent different kinds
 * of bindings. 
 */
public interface Binding {

  /**
   * @return body of the method to create for this binding
   * @param nameGenerator
   */
  String getCreatorMethodBody(NameGenerator nameGenerator);

  /**
   * @return The set of keys that this binding requires. This set is used to
   *     find more classes that need to be bound.
   */
  Set<Key<?>> getRequiredKeys();
}
