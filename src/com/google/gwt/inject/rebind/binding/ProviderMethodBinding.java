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

import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.inject.rebind.util.GuiceUtil;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.google.inject.internal.ProviderMethod;

import java.lang.reflect.Method;

/**
 * A binding that calls a provider method. This binding depends on
 * the {@code GinModule}'s key, meaning that it will cause the module
 * itself to be instantiated at runtime so it can call the provider
 * method.
 */
public class ProviderMethodBinding implements Binding {
  private final GuiceUtil guiceUtil;
  private final SourceWriteUtil sourceWriteUtil;

  private MethodLiteral<?, Method> providerMethod;
  private Class<?> moduleType;

  @Inject
  public ProviderMethodBinding(GuiceUtil guiceUtil, SourceWriteUtil sourceWriteUtil) {
    this.guiceUtil = guiceUtil;
    this.sourceWriteUtil = sourceWriteUtil;
  }

  public void setProviderMethod(ProviderMethod providerMethod) {
    moduleType = providerMethod.getInstance().getClass();
    Method method = providerMethod.getMethod();
    this.providerMethod = MethodLiteral.get(method, TypeLiteral.get(method.getDeclaringClass()));
  }
  
  // TODO(schmitt): This implementation creates a new module instance for
  // every provider method invocation. Instead we should likely create just a
  // single instance of the module, invoke it repeatedly and share it between
  // provider methods.
  public void writeCreatorMethods(SourceWriter writer, String creatorMethodSignature, 
      NameGenerator nameGenerator) throws NoSourceNameException {
    String moduleSourceName = ReflectUtil.getSourceName(moduleType);
    String createModule = "new " + moduleSourceName + "()";
    sourceWriteUtil.writeMethod(writer, creatorMethodSignature,
        "return " + sourceWriteUtil.createMethodCallWithInjection(writer, providerMethod,
            createModule, nameGenerator));
  }

  public RequiredKeys getRequiredKeys() {
    return guiceUtil.getRequiredKeys(providerMethod);
  }
}
