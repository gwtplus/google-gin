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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.inject.rebind.util.KeyUtil;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.internal.ProviderMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
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
  private final TreeLogger logger;

  private Class<?> moduleClass;
  private Set<Key<?>> parameterKeys;
  private JMethod gwtProviderMethod;

  @Inject
  public ProviderMethodBinding(KeyUtil keyUtil, SourceWriteUtil sourceWriteUtil,
      TreeLogger logger) {
    this.keyUtil = keyUtil;
    this.sourceWriteUtil = sourceWriteUtil;
    this.logger = logger;
  }

  public void setProviderMethod(ProviderMethod providerMethod) throws UnableToCompleteException {
    try {
      this.gwtProviderMethod = keyUtil.javaToGwtMethod(providerMethod.getMethod());
    } catch (NotFoundException e) {
      logger.log(TreeLogger.Type.ERROR, e.getMessage(), e);
      throw new UnableToCompleteException();
    }

    moduleClass = providerMethod.getInstance().getClass();

    Method method = providerMethod.getMethod();

    Type[] parameterTypes = method.getGenericParameterTypes();
    Annotation[][] parameterAnnotations = method.getParameterAnnotations();
    assert parameterTypes.length == parameterAnnotations.length;
    parameterKeys = new HashSet<Key<?>>(parameterTypes.length);

    for (int i = 0; i < parameterTypes.length; i++) {
      parameterKeys.add(keyUtil.getKey(parameterTypes[i], parameterAnnotations[i]));
    }
  }

  public void writeCreatorMethods(SourceWriter writer, String creatorMethodSignature) {
    String createModule = "new " + moduleClass.getName() + "()";
    sourceWriteUtil.writeMethod(writer, creatorMethodSignature,
        "return " + sourceWriteUtil.createMethodCallWithInjection(writer, gwtProviderMethod,
            createModule));
  }

  public Set<Key<?>> getRequiredKeys() {
    return parameterKeys;
  }
}
