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
import com.google.gwt.inject.rebind.util.KeyUtil;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Key;

import java.util.HashSet;
import java.util.Set;

/**
 * Abstract binder that takes a type and performs the required key analysis and
 * method injection.  Calls {@link #appendCreationStatement} on subclass to
 * retrieve the actual object creation statement (e.g.
 * {@code new MyObject();}).
 */
abstract class CreatorBinding implements Binding {

  private final SourceWriteUtil sourceWriteUtil;
  private final KeyUtil keyUtil;
  private final Set<Key<?>> requiredKeys = new HashSet<Key<?>>();
  private final Set<Key<?>> optionalKeys = new HashSet<Key<?>>();
  private JClassType classType;
  private Key<?> classKey;

  protected CreatorBinding(SourceWriteUtil sourceWriteUtil, KeyUtil keyUtil) {
    this.sourceWriteUtil = sourceWriteUtil;
    this.keyUtil = keyUtil;
  }

  public void setClassType(JClassType classType, Key<?> classKey) {
    this.classType = classType;
    this.classKey = classKey;
    RequiredKeys classRequiredKeys = keyUtil.getRequiredKeys(classType);
    requiredKeys.addAll(classRequiredKeys.getRequiredKeys());
    optionalKeys.addAll(classRequiredKeys.getOptionalKeys());
  }

  public final void writeCreatorMethods(SourceWriter writer, String creatorMethodSignature) {
    assert (classType != null);

    String memberInjectMethodName = sourceWriteUtil.appendMemberInjection(writer, classKey);

    StringBuilder sb = new StringBuilder();
    appendCreationStatement(writer, sb);
    sb.append("\n");
    sb.append(memberInjectMethodName).append("(result);\n");

    sb.append("return result;");

    sourceWriteUtil.writeMethod(writer, creatorMethodSignature, sb.toString());
  }

  public RequiredKeys getRequiredKeys() {
    return new RequiredKeys(requiredKeys, optionalKeys);
  }

  public JClassType getClassType() {
    assert (classType != null);
    return classType;
  }

  protected abstract void appendCreationStatement(SourceWriter sourceWriter, StringBuilder sb);

  protected String getTypeName() {
    assert (classType != null);
    return classType.getQualifiedSourceName();
  }

  protected void addParamTypes(JAbstractMethod method) {
    RequiredKeys methodRequiredKeys = keyUtil.getRequiredKeys(method);
    requiredKeys.addAll(methodRequiredKeys.getRequiredKeys());
    optionalKeys.addAll(methodRequiredKeys.getOptionalKeys());
  }
}
