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

import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Inject;
import com.google.inject.Key;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

/**
 * Binding implementation for {@code Provider<T>} that just uses the binding
 * to {@code T}.
 */
public class ImplicitProviderBinding implements Binding {

  private final NameGenerator nameGenerator;

  private final SourceWriteUtil sourceWriteUtil;

  private ParameterizedType providerType;
  private Key<?> targetKey;

  @Inject
  public ImplicitProviderBinding(NameGenerator nameGenerator, SourceWriteUtil sourceWriteUtil) {
    this.nameGenerator = nameGenerator;
    this.sourceWriteUtil = sourceWriteUtil;
  }

  public void setProviderKey(Key<?> providerKey) {
    providerType = (ParameterizedType) providerKey.getTypeLiteral().getType();

    // Pass any binding annotation on the Provider to the thing we create
    Type targetType = providerType.getActualTypeArguments()[0];
    targetKey = getKeyWithSameAnnotation(targetType, providerKey);
  }

  public void writeCreatorMethods(SourceWriter writer, String creatorMethodSignature) {
    assert (providerType != null);
    sourceWriteUtil.writeMethod(writer, creatorMethodSignature,
        "return new " + providerType + "() { \n"
        + "  public " + targetKey.getTypeLiteral() + " get() { \n"
        + "    return " + nameGenerator.getGetterMethodName(targetKey) + "();\n"
        + "  }\n"
        + "};");
  }

  public Set<Key<?>> getRequiredKeys() {
    assert (targetKey != null);
    return Collections.<Key<?>>singleton(targetKey);
  }

  private Key<?> getKeyWithSameAnnotation(Type keyType, Key<?> baseKey) {
    Annotation annotation = baseKey.getAnnotation();
    if (annotation != null) {
      return Key.get(keyType, annotation);
    }

    Class<? extends Annotation> annotationType = baseKey.getAnnotationType();
    if (annotationType != null) {
      return Key.get(keyType, annotationType);
    }

    return Key.get(keyType);
  }
}
