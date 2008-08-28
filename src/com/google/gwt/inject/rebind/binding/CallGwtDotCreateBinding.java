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

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.inject.rebind.NameGenerator;
import com.google.inject.Key;

import java.util.Collections;
import java.util.Set;

/**
 * A binding that just calls {@code GWT.create()} for the requested type.
 * This is the default binding for interfaces or classes that don't have
 * a non-default constructor annotated with {@code @Inject}.
 */
public class CallGwtDotCreateBinding implements Binding {
  private final JClassType classType;

  public CallGwtDotCreateBinding(JClassType classType) {
    this.classType = classType;
  }

  public String getCreatorMethodBody(NameGenerator nameGenerator) {
    return "return " + "GWT.create(" +
        classType.getParameterizedQualifiedSourceName() + ".class);";
  }

  public Set<Key<?>> getRequiredKeys() {
    return Collections.emptySet();
  }
}
