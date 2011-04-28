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
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.inject.rebind.util.GuiceUtil;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract binder that takes a type and performs the required key analysis and
 * method injection.  Calls {@link #appendCreationStatement} on subclass to
 * retrieve the actual object creation statement (e.g.
 * {@code new MyObject();}).
 */
abstract class CreatorBinding implements Binding {

  private final SourceWriteUtil sourceWriteUtil;
  private final GuiceUtil guiceUtil;
  private final Set<Dependency> dependencies = new HashSet<Dependency>();
  private final TypeLiteral<?> type;

  protected CreatorBinding(SourceWriteUtil sourceWriteUtil, GuiceUtil guiceUtil,
      TypeLiteral<?> type) {
    this.sourceWriteUtil = sourceWriteUtil;
    this.guiceUtil = guiceUtil;

    this.type = Preconditions.checkNotNull(type);
    dependencies.addAll(guiceUtil.getMemberInjectionDependencies(Key.get(type), type));
  }

  public final void writeCreatorMethods(SourceWriter writer, String creatorMethodSignature,
      NameGenerator nameGenerator) throws NoSourceNameException {
    String memberInjectMethodName = sourceWriteUtil.appendMemberInjection(writer, Key.get(type),
        nameGenerator);

    StringBuilder sb = new StringBuilder();
    appendCreationStatement(writer, sb, nameGenerator);
    sb.append("\n");
    sb.append(memberInjectMethodName).append("(result);\n");

    sb.append("return result;");

    sourceWriteUtil.writeMethod(writer, creatorMethodSignature, sb.toString());
  }

  public Collection<Dependency> getDependencies() {
    return dependencies;
  }

  public TypeLiteral<?> getType() {
    return type;
  }

  protected abstract void appendCreationStatement(SourceWriter sourceWriter, StringBuilder sb,
      NameGenerator nameGenerator) throws NoSourceNameException;

  protected String getTypeName() throws NoSourceNameException {
    return ReflectUtil.getSourceName(type);
  }

  protected void addParamTypes(MethodLiteral<?, ?> method) {
    dependencies.addAll(guiceUtil.getDependencies(Key.get(type), method));
  }
}
