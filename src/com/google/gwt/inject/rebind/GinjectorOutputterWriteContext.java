/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.inject.rebind;

import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.GinjectorNameGenerator;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.gwt.inject.rebind.util.InjectorWriteContext;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.Assisted;

/**
 * An {@link InjectorWriteContext} implementing the default output rules for
 * {@link GinjectorOutputter}.
 */
class GinjectorOutputterWriteContext implements InjectorWriteContext {

  private final GinjectorBindings bindings;
  private final GinjectorNameGenerator ginjectorNameGenerator;
  private final SourceWriteUtil sourceWriteUtil;

  @Inject
  public GinjectorOutputterWriteContext(
      GinjectorNameGenerator ginjectorNameGenerator,
      SourceWriteUtil.Factory sourceWriteUtilFactory,
      @Assisted GinjectorBindings bindings) {
    this.bindings = bindings;
    this.ginjectorNameGenerator = ginjectorNameGenerator;
    this.sourceWriteUtil = sourceWriteUtilFactory.create(bindings);
  }

  public String callGetter(Key<?> key) {
    return bindings.getNameGenerator().getGetterMethodName(key) + "()";
  }

  public String callChildGetter(GinjectorBindings childBindings, Key<?> key) {
    NameGenerator nameGenerator = bindings.getNameGenerator();
    NameGenerator childNameGenerator = childBindings.getNameGenerator();

    String childFieldName = ginjectorNameGenerator.getFieldName(childBindings);
    String getter = childNameGenerator.getGetterMethodName(key);

    return String.format("%s.%s()", childFieldName, getter);
  }

  public String callMemberInject(TypeLiteral<?> type, String input) {
    String memberInjectMethodName = bindings.getNameGenerator().getMemberInjectMethodName(type);

    return String.format("%s(%s);", memberInjectMethodName, input);
  }

  public String callParentGetter(Key<?> key, GinjectorBindings parentBindings) {
    NameGenerator parentNameGenerator = parentBindings.getNameGenerator();
    String parentClassName = ginjectorNameGenerator.getClassName(parentBindings);
    String parentMethodName = parentNameGenerator.getGetterMethodName(key);
    return String.format("%s.this.%s()", parentClassName, parentMethodName);
  }

  public String getGinjectorInterface() {
    return "this";
  }

  /**
   * Factory for {@link GinjectorOutputterWriteContext}.
   */
  interface Factory {
    InjectorWriteContext create(GinjectorBindings bindings);
  }
}
