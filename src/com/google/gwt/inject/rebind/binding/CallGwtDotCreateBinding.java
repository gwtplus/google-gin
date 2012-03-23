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

import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.inject.rebind.util.GuiceUtil;
import com.google.gwt.inject.rebind.util.InjectorMethod;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceSnippet;
import com.google.gwt.inject.rebind.util.SourceSnippetBuilder;
import com.google.inject.TypeLiteral;

import java.util.List;

/**
 * A binding that just calls {@code GWT.create()} for the requested type.
 * This is the default binding for interfaces or classes that don't have
 * a non-default constructor annotated with {@code @Inject}.
 */
public class CallGwtDotCreateBinding extends CreatorBinding {

  CallGwtDotCreateBinding(GuiceUtil guiceUtil, TypeLiteral<?> type, Context context) {
    super(guiceUtil, type, context);
  }

  @Override protected final SourceSnippet getCreationStatement(List<InjectorMethod> methodsOutput,
      NameGenerator nameGenerator) throws NoSourceNameException {

    return new SourceSnippetBuilder()
        .append("Object created = GWT.create(").append(getTypeNameToCreate()).append(".class);\n")
        // Gin cannot deal with cases where the type returned by GWT.create is
        // not equal or a subtype of the requested type. Assert this here, in
        // production code (without asserts) the line below the assert will
        // throw a ClassCastException instead.
        .append("assert created instanceof ").append(getExpectedTypeName()).append(";\n")
        .append(getTypeName()).append(" result = (").append(getTypeName()).append(") created;\n")
        .build();
  }

  protected String getTypeNameToCreate() throws NoSourceNameException {

    // Using a raw type because GWT.create and instanceof cannot take
    // parameterized arguments.
    return ReflectUtil.getSourceName(getType().getRawType());
  }

  protected String getExpectedTypeName() throws NoSourceNameException {
    return getTypeNameToCreate();
  }
}
