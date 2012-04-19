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
import com.google.gwt.inject.rebind.util.InjectorMethod;
import com.google.gwt.inject.rebind.util.MethodCallUtil;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceSnippet;
import com.google.gwt.inject.rebind.util.SourceSnippetBuilder;
import com.google.gwt.inject.rebind.util.SourceSnippets;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.internal.ProviderMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A binding that calls a provider method. This binding depends on
 * the {@code GinModule}'s key, meaning that it will cause the module
 * itself to be instantiated at runtime so it can call the provider
 * method.
 */
public class ProviderMethodBinding extends AbstractBinding implements Binding {
  private final GuiceUtil guiceUtil;

  private MethodLiteral<?, Method> providerMethod;
  private final MethodCallUtil methodCallUtil;
  private final Class<?> moduleType;
  private final Key<?> targetKey;
  
  ProviderMethodBinding(GuiceUtil guiceUtil, MethodCallUtil methodCallUtil,
      ProviderMethod<?> providerMethod, Context context) {
    super(context, TypeLiteral.get(providerMethod.getMethod().getDeclaringClass()));

    this.guiceUtil = guiceUtil;

    this.methodCallUtil = methodCallUtil;
    this.moduleType = providerMethod.getInstance().getClass();
    Method method = providerMethod.getMethod();
    this.providerMethod = MethodLiteral.get(method, TypeLiteral.get(method.getDeclaringClass()));
    this.targetKey = providerMethod.getKey();
  }
  
  // TODO(schmitt): This implementation creates a new module instance for
  // every provider method invocation. Instead we should likely create just a
  // single instance of the module, invoke it repeatedly and share it between
  // provider methods.
  public SourceSnippet getCreationStatements(NameGenerator nameGenerator,
      List<InjectorMethod> methodsOutput) throws NoSourceNameException {
    String moduleSourceName = ReflectUtil.getSourceName(moduleType);
    String createModule = "new " + moduleSourceName + "()";
    String type = ReflectUtil.getSourceName(targetKey.getTypeLiteral());

    return new SourceSnippetBuilder()
        .append(type).append(" result = ")
        .append(methodCallUtil.createMethodCallWithInjection(providerMethod, createModule,
            nameGenerator, methodsOutput))
        .build();
  }

  public Collection<Dependency> getDependencies() {
    Collection<Dependency> dependencies = guiceUtil.getDependencies(targetKey, providerMethod);
    dependencies.add(new Dependency(Dependency.GINJECTOR, targetKey, getContext()));
    return dependencies;
  }
}
