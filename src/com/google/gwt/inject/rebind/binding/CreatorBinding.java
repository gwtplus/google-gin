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
import com.google.gwt.inject.rebind.util.InjectorMethod;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceSnippet;
import com.google.gwt.inject.rebind.util.SourceSnippetBuilder;
import com.google.gwt.inject.rebind.util.SourceSnippets;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract binder that takes a type and performs the required key analysis and
 * method injection.  Calls {@link #getCreationStatement} on subclass to
 * retrieve the actual object creation statement (e.g.
 * {@code new MyObject();}).
 */
abstract class CreatorBinding extends AbstractBinding implements Binding {

  private final GuiceUtil guiceUtil;
  private final Set<Dependency> dependencies = new LinkedHashSet<Dependency>();
  private final TypeLiteral<?> type;

  protected CreatorBinding(GuiceUtil guiceUtil, TypeLiteral<?> type, Context context) {
    super(context);

    this.guiceUtil = guiceUtil;

    this.type = Preconditions.checkNotNull(type);
    dependencies.addAll(guiceUtil.getMemberInjectionDependencies(Key.get(type), type));
  }

  public final Iterable<InjectorMethod> getCreatorMethods(String creatorMethodSignature,
      NameGenerator nameGenerator) throws NoSourceNameException {
    List<InjectorMethod> methods = new ArrayList<InjectorMethod>();

    SourceSnippet creatorMethod = new SourceSnippetBuilder()
        .append(getCreationStatement(methods, nameGenerator))
        .append("\n")
        .append(SourceSnippets.callMemberInject(type, "result"))
        .append("\n")
        .append("return result;")
        .build();

    methods.add(SourceSnippets.asMethod(false, creatorMethodSignature, creatorMethod));

    return Collections.unmodifiableList(methods);
  }

  public Collection<Dependency> getDependencies() {
    return dependencies;
  }

  public TypeLiteral<?> getType() {
    return type;
  }

  /**
   * Gets a {@link SourceSnippet} that creates the bound value and stores it in
   * a new local variable named "result", and creates any auxiliary methods
   * required by the snippet.
   */
  protected abstract SourceSnippet getCreationStatement(List<InjectorMethod> methodsOutput,
      NameGenerator nameGenerator) throws NoSourceNameException;

  protected String getTypeName() throws NoSourceNameException {
    return ReflectUtil.getSourceName(type);
  }

  protected void addParamTypes(MethodLiteral<?, ?> method) {
    dependencies.addAll(guiceUtil.getDependencies(Key.get(type), method));
  }

  @Override
  public Collection<TypeLiteral<?>> getMemberInjectRequests() {
    return Collections.<TypeLiteral<?>>singleton(type);
  }
}
