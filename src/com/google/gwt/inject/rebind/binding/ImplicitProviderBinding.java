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
import com.google.gwt.inject.rebind.util.InjectorWriteContext;
import com.google.inject.Key;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Collections;

/**
 * Binding implementation for {@code Provider<T>} that just uses the binding
 * to {@code T}.
 */
public class ImplicitProviderBinding extends AbstractSingleMethodBinding implements Binding {

  private final ParameterizedType providerType;
  private final Key<?> targetKey;
  private final Key<?> providerKey;

  ImplicitProviderBinding(Key<?> providerKey) {
    super(Context.format("Implicit provider for %s", providerKey));

    this.providerKey = Preconditions.checkNotNull(providerKey);
    this.providerType = (ParameterizedType) providerKey.getTypeLiteral().getType();

    // Pass any binding annotation on the Provider to the thing we create
    this.targetKey = ReflectUtil.getProvidedKey(providerKey);
  }

  public void appendCreatorMethodBody(StringBuilder builder, InjectorWriteContext writeContext)
      throws NoSourceNameException {

    String providerTypeName = ReflectUtil.getSourceName(providerType);
    String targetKeyName = ReflectUtil.getSourceName(targetKey.getTypeLiteral());
    builder
        .append("return new ")
        .append(providerTypeName).append("() { \n")
        .append("  public ").append(targetKeyName).append(" get() { \n")
        .append("    return ").append(writeContext.callGetter(targetKey)).append(";\n")
        .append("  }\n")
        .append("};");
  }

  public Collection<Dependency> getDependencies() {
    return Collections.singleton(new Dependency(providerKey, targetKey, false, true, getContext()));
  }
}
