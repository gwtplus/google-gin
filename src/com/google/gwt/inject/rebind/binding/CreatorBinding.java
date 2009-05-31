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
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.inject.rebind.util.KeyUtil;
import com.google.gwt.inject.rebind.util.MemberCollector;
import com.google.gwt.inject.rebind.util.NameGenerator;
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

  private final MemberCollector memberCollector;
  private final SourceWriteUtil sourceWriteUtil;
  private final KeyUtil keyUtil;
  private final BindingIndex bindingIndex;
  private final Set<Key<?>> requiredKeys;
  private final Set<Key<?>> optionalKeys;
  private final NameGenerator nameGenerator;
  private JClassType classType;
  private Key<?> classKey;

  protected CreatorBinding(MemberCollector memberCollector, SourceWriteUtil sourceWriteUtil,
      KeyUtil keyUtil, BindingIndex bindingIndex, NameGenerator nameGenerator) {
    this.memberCollector = memberCollector;
    this.sourceWriteUtil = sourceWriteUtil;
    this.keyUtil = keyUtil;
    this.bindingIndex = bindingIndex;
    this.nameGenerator = nameGenerator;
    this.requiredKeys = new HashSet<Key<?>>();
    this.optionalKeys = new HashSet<Key<?>>();
  }

  public void setClassType(JClassType classType, Key<?> classKey) {
    this.classType = classType;
    this.classKey = classKey;
    for (JMethod method : memberCollector.getMethods(classType)) {
      addParamTypes(method);
    }

    for (JField field : memberCollector.getFields(classType)) {
      Key<?> key = keyUtil.getKey(field);
      if (keyUtil.isOptional(field)) {
        optionalKeys.add(key);
      } else {
        requiredKeys.add(key);
      }
    }
  }

  public final void writeCreatorMethods(SourceWriter writer, String creatorMethodSignature) {
    assert (classType != null);

    String memberInjectMethodName = appendMemberInjection(writer);

    StringBuilder sb = new StringBuilder();
    sb.append(getTypeName()).append(" result = ");
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
    for (JParameter param : method.getParameters()) {
      Key<?> key = keyUtil.getKey(param);
      if (keyUtil.isOptional(method)) {
        optionalKeys.add(key);
      } else {
        requiredKeys.add(key);
      }
    }
  }

  private String appendMemberInjection(SourceWriter writer) {
    String memberInjectMethodName = nameGenerator.getMemberInjectMethodName(classKey);

    StringBuilder sb = new StringBuilder();

    sb.append(sourceWriteUtil.createMethodInjection(writer, getMethodsToInject(classType),
        "injectee"));
    sb.append(sourceWriteUtil.appendFieldInjection(writer, getFieldsToInject(classType),
        "injectee"));

    sourceWriteUtil.writeMethod(writer,
        "private void " + memberInjectMethodName +
            "(" + classType.getQualifiedSourceName() + " injectee)",
        sb.toString());

    return memberInjectMethodName;
  }

  private Set<JField> getFieldsToInject(JClassType classType) {
    // Only inject fields that are non optional or where the key is bound.
    Set<JField> fields = new HashSet<JField>();
    for (JField field : memberCollector.getFields(classType)) {
      if (!keyUtil.isOptional(field) || bindingIndex.isBound(keyUtil.getKey(field))) {
        fields.add(field);
      }
    }
    return fields;
  }

  private Set<JMethod> getMethodsToInject(JClassType classType) {
    Set<JMethod> methods = new HashSet<JMethod>();
    for (JMethod method : memberCollector.getMethods(classType)) {
      if (shouldInject(method)) {
        methods.add(method);
      }
    }
    return methods;
  }

  private boolean shouldInject(JMethod method) {
    // Only inject methods that are non optional or where all keys are bound.
    if (keyUtil.isOptional(method)) {
      for (JParameter param : method.getParameters()) {
        if(!bindingIndex.isBound(keyUtil.getKey(param))) {
          return false;
        }
      }
    }

    return true;
  }
}
