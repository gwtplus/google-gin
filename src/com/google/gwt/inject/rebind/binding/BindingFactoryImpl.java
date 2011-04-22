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

package com.google.gwt.inject.rebind.binding;

import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.GinjectorInterfaceType;
import com.google.gwt.inject.rebind.GinjectorNameGenerator;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.util.GuiceUtil;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.internal.ProviderMethod;

import javax.inject.Provider;

import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * Guice-based implementation of the binding factory.
 */
public class BindingFactoryImpl implements BindingFactory {

  private final SourceWriteUtil sourceWriteUtil;
  private final GuiceUtil guiceUtil;
  private final GinjectorNameGenerator ginjectorNameGenerator;

  private final Class<? extends Ginjector> ginjectorInterface;

  @Inject
  BindingFactoryImpl(SourceWriteUtil sourceWriteUtil, GuiceUtil guiceUtil,
      GinjectorNameGenerator ginjectorNameGenerator,
      @GinjectorInterfaceType Class<? extends Ginjector> ginjectorInterface) {
    this.sourceWriteUtil = sourceWriteUtil;
    this.guiceUtil = guiceUtil;
    this.ginjectorNameGenerator = ginjectorNameGenerator;
    this.ginjectorInterface = ginjectorInterface;
  }

  public AsyncProviderBinding getAsyncProviderBinding(Key<?> providerKey) {
    return new AsyncProviderBinding(sourceWriteUtil, providerKey);
  }

  public BindClassBinding getBindClassBinding(Key<?> boundClassKey, Key<?> sourceClassKey) {
    return new BindClassBinding(sourceWriteUtil, boundClassKey, sourceClassKey);
  }

  public <T> BindConstantBinding getBindConstantBinding(Key<T> key, T instance) {
    return new BindConstantBinding<T>(sourceWriteUtil, key, instance);
  }

  public BindProviderBinding getBindProviderBinding(Key<? extends Provider<?>> providerKey,
      Key<?> sourceKey) {
    return new BindProviderBinding(sourceWriteUtil, providerKey, sourceKey);
  }

  public CallConstructorBinding getCallConstructorBinding(
      MethodLiteral<?, Constructor<?>> constructor) {
    return new CallConstructorBinding(sourceWriteUtil, guiceUtil, constructor);
  }

  public CallGwtDotCreateBinding getCallGwtDotCreateBinding(TypeLiteral<?> type) {
    return new CallGwtDotCreateBinding(sourceWriteUtil, guiceUtil, type);
  }
  
  public ExposedChildBinding getExposedChildBinding(Key<?> key, GinjectorBindings childBindings) {
    return new ExposedChildBinding(sourceWriteUtil, ginjectorNameGenerator, key, childBindings);
  }
  
  public FactoryBinding getFactoryBinding(Map<Key<?>, TypeLiteral<?>> collector,
      Key<?> factoryKey) {
    return new FactoryBinding(sourceWriteUtil, collector, factoryKey);
  }

  public GinjectorBinding getGinjectorBinding() {
    return new GinjectorBinding(sourceWriteUtil, ginjectorInterface);
  }

  public ImplicitProviderBinding getImplicitProviderBinding(Key<?> providerKey) {
    return new ImplicitProviderBinding(sourceWriteUtil, providerKey);
  }
  
  public ParentBinding getParentBinding(Key<?> key, GinjectorBindings parentBindings) {
    return new ParentBinding(sourceWriteUtil, ginjectorNameGenerator, key, parentBindings);
  }
  
  public ProviderMethodBinding getProviderMethodBinding(ProviderMethod<?> providerMethod) {
    return new ProviderMethodBinding(guiceUtil, sourceWriteUtil, providerMethod);
  }

  public RemoteServiceProxyBinding getRemoteServiceProxyBinding(TypeLiteral<?> type) {
    return new RemoteServiceProxyBinding(sourceWriteUtil, guiceUtil, type);
  }
}
