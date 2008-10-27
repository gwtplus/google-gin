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

import com.google.gwt.inject.rebind.NameGenerator;
import com.google.inject.Inject;
import com.google.inject.Key;

import java.util.Collections;
import java.util.Set;

/**
 * Binding implementation that replaces one type with another.
 */
public class BindClassBinding implements Binding {

  private final NameGenerator nameGenerator;

  private Key<?> boundClassKey;

  @Inject
  public BindClassBinding(NameGenerator nameGenerator) {
    this.nameGenerator = nameGenerator;
  }

  public void setBoundClassKey(Key<?> boundClassKey) {
    this.boundClassKey = boundClassKey;
  }

  public String getCreatorMethodBody() {
    assert (boundClassKey != null);
    return "return " + nameGenerator.getGetterMethodName(boundClassKey) + "();";
  }

  public Set<Key<?>> getRequiredKeys() {
    assert (boundClassKey != null);
    return Collections.<Key<?>>singleton(boundClassKey);
  }
}
