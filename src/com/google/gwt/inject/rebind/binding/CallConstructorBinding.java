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

import com.google.gwt.dev.util.Preconditions;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.gwt.inject.rebind.util.GuiceUtil;
import com.google.gwt.inject.rebind.util.InjectorMethod;
import com.google.gwt.inject.rebind.util.MethodCallUtil;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceSnippet;
import com.google.gwt.inject.rebind.util.SourceSnippetBuilder;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * A binding that calls a single constructor directly. Values for constructor
 * parameters are retrieved by going back through the injector.
 */
public class CallConstructorBinding extends CreatorBinding {

  private final MethodLiteral<?, Constructor<?>> constructor;
  private final MethodCallUtil methodCallUtil;

  CallConstructorBinding(GuiceUtil guiceUtil, MethodLiteral<?, Constructor<?>> constructor,
      MethodCallUtil methodCallUtil) {
    super(guiceUtil, constructor.getDeclaringType(),
          Context.format("Implicit binding for %s", constructor.getDeclaringType()));
    this.constructor = Preconditions.checkNotNull(constructor);
    this.methodCallUtil = methodCallUtil;
    addParamTypes(constructor);
  }

  @Override protected SourceSnippet getCreationStatement(List<InjectorMethod> methodsOutput,
      NameGenerator nameGenerator) throws NoSourceNameException {
    return new SourceSnippetBuilder()
        .append(getTypeName()).append(" result = ")
        .append(
            methodCallUtil.createConstructorInjection(constructor, nameGenerator, methodsOutput))
        .build();
  }
}
