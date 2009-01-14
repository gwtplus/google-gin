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

import com.google.gwt.inject.rebind.util.KeyUtil;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.internal.ProviderMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A binding that calls a provider method. This binding depends on
 * the {@code GinModule}'s key, meaning that it will cause the module
 * itself to be instantiated at runtime so it can call the provider
 * method.
 */
public class ProviderMethodBinding implements Binding {
  private final KeyUtil keyUtil;
  private final SourceWriteUtil sourceWriteUtil;
  private final NameGenerator nameGenerator;

  private String methodName;
  private Key<?> moduleClassKey;
  private List<Key<?>> parameterKeys;

  @Inject
  public ProviderMethodBinding(KeyUtil keyUtil, SourceWriteUtil sourceWriteUtil,
      NameGenerator nameGenerator) {
    this.keyUtil = keyUtil;
    this.sourceWriteUtil = sourceWriteUtil;
    this.nameGenerator = nameGenerator;
  }

  public void setProviderMethod(ProviderMethod providerMethod) {
    moduleClassKey = Key.get(providerMethod.getInstance().getClass());

    Method method = providerMethod.getMethod();
    methodName = method.getName();

    Type[] parameterTypes = method.getGenericParameterTypes();
    Annotation[][] parameterAnnotations = method.getParameterAnnotations();
    assert parameterTypes.length == parameterAnnotations.length;
    parameterKeys = new ArrayList<Key<?>>(parameterTypes.length);

    for (int i = 0; i < parameterTypes.length; i++) {
      parameterKeys.add(keyUtil.getKey(parameterTypes[i], parameterAnnotations[i]));
    }
  }

  public void writeCreatorMethods(SourceWriter writer, String creatorMethodSignature) {
    StringBuilder sb = new StringBuilder();

    sb.append("return ");

    // Get the module
    sb.append(nameGenerator.getGetterMethodName(moduleClassKey)).append("()");

    // Start method call
    sb.append(".").append(methodName).append("(");

    // Output each parameter
    for (int i = 0; i < parameterKeys.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }

      Key<?> parameterKey = parameterKeys.get(i);
      sb.append(nameGenerator.getGetterMethodName(parameterKey)).append("()");
    }

    sb.append(");");

    sourceWriteUtil.writeMethod(writer, creatorMethodSignature, sb.toString());
  }

  public Set<Key<?>> getRequiredKeys() {
    Set<Key<?>> keys = new HashSet<Key<?>>(parameterKeys.size() + 1);
    keys.add(moduleClassKey);
    keys.addAll(parameterKeys);
    
    return keys;
  }
}
