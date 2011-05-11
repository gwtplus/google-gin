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
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.rebind.SourceWriter;

import java.lang.reflect.Constructor;

/**
 * A binding that calls a single constructor directly. Values for constructor
 * parameters are retrieved by going back through the injector.
 */
public class CallConstructorBinding extends CreatorBinding {

  private final SourceWriteUtil sourceWriteUtil;
  private final MethodLiteral<?, Constructor<?>> constructor;

  CallConstructorBinding(SourceWriteUtil sourceWriteUtil, GuiceUtil guiceUtil,
      MethodLiteral<?, Constructor<?>> constructor) {
    super(sourceWriteUtil, guiceUtil, constructor.getDeclaringType(),
          Context.format("Implicit binding for %s", constructor.getDeclaringType()));
    this.sourceWriteUtil = sourceWriteUtil;
    this.constructor = Preconditions.checkNotNull(constructor);
    addParamTypes(constructor);
  }

  @Override protected void appendCreationStatement(SourceWriter sourceWriter, StringBuilder sb,
      NameGenerator nameGenerator) throws NoSourceNameException {
    Preconditions.checkNotNull(constructor);
    sb.append(getTypeName()).append(" result = ")
        .append(sourceWriteUtil.createConstructorInjection(
            sourceWriter, constructor, nameGenerator));
  }
}
