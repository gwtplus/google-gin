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
import com.google.gwt.inject.rebind.ErrorManager;
import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.GinjectorInterfaceType;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.util.GuiceUtil;
import com.google.gwt.inject.rebind.util.MethodCallUtil;
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

  private final ErrorManager errorManager;
  private final GuiceUtil guiceUtil;
  private final Class<? extends Ginjector> ginjectorInterface;
  private final MethodCallUtil methodCallUtil;

  // Visible for testing.
  @Inject
  public BindingFactoryImpl(
      ErrorManager errorManager,
      GuiceUtil guiceUtil,
      @GinjectorInterfaceType Class<? extends Ginjector> ginjectorInterface,
      MethodCallUtil methodCallUtil) {
    this.errorManager = errorManager;
    this.guiceUtil = guiceUtil;
    this.ginjectorInterface = ginjectorInterface;
    this.methodCallUtil = methodCallUtil;
  }

  public AsyncProviderBinding getAsyncProviderBinding(Key<?> providerKey) {
    return new AsyncProviderBinding(providerKey);
  }

  public BindClassBinding getBindClassBinding(Key<?> boundClassKey, Key<?> sourceClassKey,
      Context context) {
    return new BindClassBinding(boundClassKey, sourceClassKey, context);
  }

  public <T> BindConstantBinding getBindConstantBinding(Key<T> key, T instance, Context context) {
    return new BindConstantBinding<T>(key, instance, context);
  }

  public BindProviderBinding getBindProviderBinding(Key<? extends Provider<?>> providerKey,
      Key<?> sourceKey, Context context) {
    return new BindProviderBinding(providerKey, sourceKey, context);
  }

  public CallConstructorBinding getCallConstructorBinding(
      MethodLiteral<?, Constructor<?>> constructor) {
    return new CallConstructorBinding(guiceUtil, constructor, methodCallUtil);
  }

  public CallGwtDotCreateBinding getCallGwtDotCreateBinding(TypeLiteral<?> type) {
    return new CallGwtDotCreateBinding(guiceUtil, type,
        Context.forText("Implicit GWT.create binding for " + type));
  }
  
  public ExposedChildBinding getExposedChildBinding(Key<?> key, GinjectorBindings childBindings,
      Context context) {
    return new ExposedChildBinding(errorManager, key, childBindings, context);
  }
  
  public FactoryBinding getFactoryBinding(Map<Key<?>, TypeLiteral<?>> collector, Key<?> factoryKey,
      Context context) {
    return new FactoryBinding(collector, factoryKey, context, guiceUtil, methodCallUtil);
  }

  public GinjectorBinding getGinjectorBinding() {
    return new GinjectorBinding(ginjectorInterface);
  }

  public ImplicitProviderBinding getImplicitProviderBinding(Key<?> providerKey) {
    return new ImplicitProviderBinding(providerKey);
  }
  
  public ParentBinding getParentBinding(Key<?> key, GinjectorBindings parentBindings,
      Context context) {
    return new ParentBinding(errorManager, key, parentBindings, context);
  }
  
  public ProviderMethodBinding getProviderMethodBinding(ProviderMethod<?> providerMethod,
      Context context) {
    return new ProviderMethodBinding(
        errorManager, guiceUtil, methodCallUtil, providerMethod, context);
  }

  public RemoteServiceProxyBinding getRemoteServiceProxyBinding(TypeLiteral<?> type) {
    return new RemoteServiceProxyBinding(guiceUtil, type);
  }
}
