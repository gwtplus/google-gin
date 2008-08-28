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

import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.inject.rebind.Util;
import com.google.gwt.inject.rebind.NameGenerator;
import com.google.inject.Key;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A binding that calls a single constructor directly. Values for constructor
 * parameters are retrieved by going back through the injector.
 */
public class CallConstructorBinding implements Binding {
  private final List<Key<?>> paramKeys;
  private final String typeName;

  public CallConstructorBinding(JConstructor constructor) {
    JParameter[] params = constructor.getParameters();
    paramKeys = new ArrayList<Key<?>>(params.length);

    for (JParameter param : params) {
      paramKeys.add(Util.getKey(param));
    }

    typeName = constructor.getEnclosingType().getParameterizedQualifiedSourceName();
  }

  public String getCreatorMethodBody(NameGenerator nameGenerator) {
    StringBuilder sb = new StringBuilder();
    sb.append("return new ").append(typeName).append("(");

    for (int i = 0; i < paramKeys.size(); i++) {
      if (i != 0) {
        sb.append(", ");
      }

      sb.append(nameGenerator.getGetterMethodName(paramKeys.get(i))).append("()");
    }

    sb.append(");");
    return sb.toString();
  }

  public Set<Key<?>> getRequiredKeys() {
    return new HashSet<Key<?>>(paramKeys);
  }
}
