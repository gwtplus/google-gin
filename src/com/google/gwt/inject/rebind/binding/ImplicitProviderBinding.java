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
import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.inject.rebind.util.InjectorMethod;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceSnippet;
import com.google.gwt.inject.rebind.util.SourceSnippetBuilder;
import com.google.gwt.inject.rebind.util.SourceSnippets;
import com.google.inject.Key;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Binding implementation for {@code Provider<T>} that just uses the binding
 * to {@code T}.
 */
public class ImplicitProviderBinding extends AbstractBinding implements Binding {

  private final ParameterizedType providerType;
  private final Key<?> targetKey;
  private final Key<?> providerKey;

  private ImplicitProviderBinding(Key<?> providerKey, Key<?> targetKey) {
    super(Context.format("Implicit provider for %s", providerKey), targetKey);


    this.providerKey = Preconditions.checkNotNull(providerKey);
    this.providerType = (ParameterizedType) providerKey.getTypeLiteral().getType();
    this.targetKey = Preconditions.checkNotNull(targetKey);
  }

  ImplicitProviderBinding(Key<?> providerKey) {
    this(providerKey, ReflectUtil.getProvidedKey(providerKey));
  }

  public SourceSnippet getCreationStatements(NameGenerator nameGenerator,
      List<InjectorMethod> methodsOutput) throws NoSourceNameException {
    String providerTypeName = ReflectUtil.getSourceName(providerType);
    String targetKeyName = ReflectUtil.getSourceName(targetKey.getTypeLiteral());

    return new SourceSnippetBuilder()
        .append(providerTypeName).append(" result = new ")
        .append(providerTypeName).append("() { \n")
        .append("  public ").append(targetKeyName).append(" get() { \n")
        .append("    return ").append(SourceSnippets.callGetter(targetKey)).append(";\n")
        .append("  }\n")
        .append("};")
        .build();
  }

  public Collection<Dependency> getDependencies() {
    return Collections.singleton(new Dependency(providerKey, targetKey, false, true, getContext()));
  }
}
