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

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

/**
 * Binding implementation that replaces one type with another.
 */
public class BindConstantBinding implements Binding {
  private final String valueToOutput;

  /**
   * Creates a constant binding if {@code key} is of a supported type, or
   * returns {@code null} otherwise.
   *
   * @param key Key to bind to
   * @param value value to bind to
   * @return binding, if {@code key} is a constant type, or {@code null} if not
   */
  public static <T> Binding create(Key<T> key, T value) {
    Type type = key.getTypeLiteral().getType();
    String valueToOutput;

    if (type == String.class) {
      // TODO(bstoler): Need to escape to work in all cases
      valueToOutput = "\"" + value.toString() + "\"";
    } else if (type == Character.class) {
      // TODO(bstoler): Need to escape to work in all cases
      valueToOutput = "'" + value.toString() + "'";
    } else if (value instanceof Number || value instanceof Boolean) {
      // TODO(bstoler): May need type qualifier on numbers
      valueToOutput = value.toString();
    } else {
      // Unsupported type
      return null;
    }

    return new BindConstantBinding(valueToOutput);
  }

  private BindConstantBinding(String valueToOutput) {
    this.valueToOutput = valueToOutput;
  }

  public String getCreatorMethodBody() {
    return "return " + valueToOutput + ";";
  }

  public Set<Key<?>> getRequiredKeys() {
    return Collections.emptySet();
  }
}
