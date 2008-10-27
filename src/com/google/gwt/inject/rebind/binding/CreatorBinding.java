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

import com.google.gwt.core.ext.typeinfo.JAbstractMethod;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.inject.rebind.KeyUtil;
import com.google.gwt.inject.rebind.MethodCollector;
import com.google.gwt.inject.rebind.SourceWriteUtil;
import com.google.inject.Key;

import java.util.HashSet;
import java.util.Set;

/**
 * Abstract binder that takes a type and performs the required key analysis and
 * method injection.  Calls
 * {@link #appendCreationStatement(StringBuilder, com.google.gwt.inject.rebind.NameGenerator)}
 * on subclass to retrieve the actual object creation statement (e.g.
 * {@code new MyObject();}).
 */
abstract class CreatorBinding implements Binding {

  private final MethodCollector methodCollector;
  private final SourceWriteUtil sourceWriteUtil;
  private final KeyUtil keyUtil;
  private Set<Key<?>> requiredKeys;
  private JClassType classType;

  protected CreatorBinding(MethodCollector methodCollector, SourceWriteUtil sourceWriteUtil,
      KeyUtil keyUtil) {
    this.methodCollector = methodCollector;
    this.sourceWriteUtil = sourceWriteUtil;
    this.keyUtil = keyUtil;
    this.requiredKeys = new HashSet<Key<?>>();
  }

  public void setClassType(JClassType classType) {
    this.classType = classType;
    for (JMethod method : methodCollector.getMethods(classType)) {
      addParamTypes(method);
    }
  }

  public String getCreatorMethodBody() {
    assert (classType != null);

    StringBuilder sb = new StringBuilder();
    sb.append(getTypeName()).append(" result = ");
    appendCreationStatement(sb);

    for (JMethod method : methodCollector.getMethods(classType)) {
      sb.append("result.").append(method.getName());
      sourceWriteUtil.appendInvoke(sb, method);
    }

    sb.append("return result;");

    return sb.toString();
  }

  public Set<Key<?>> getRequiredKeys() {
    return requiredKeys;
  }

  public JClassType getClassType() {
    assert (classType != null);
    return classType;
  }

  protected abstract void appendCreationStatement(StringBuilder sb);

  protected String getTypeName() {
    assert (classType != null);
    return classType.getQualifiedSourceName();
  }

  protected void addParamTypes(JAbstractMethod method) {
    for (JParameter param : method.getParameters()) {
      requiredKeys.add(keyUtil.getKey(param));
    }
  }
}
