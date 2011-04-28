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

import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.internal.ProviderMethod;

import javax.inject.Provider;

import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * Factory interface for creating bindings. The parameters to each method are the not guice
 * controlled parameters to each binding's constructor, analogous to how {@link Assisted} factories
 * work. We cannot use true assisted inject since some of the types we use ({@link Key} and
 * {@link TypeLiteral}) are not compatible with Guice injection.
 */
public interface BindingFactory {

  AsyncProviderBinding getAsyncProviderBinding(Key<?> providerKey);

  BindClassBinding getBindClassBinding(Key<?> boundClassKey, Key<?> sourceClassKey,
      BindingContext context);

  <T> BindConstantBinding getBindConstantBinding(Key<T> key, T instance, BindingContext context);

  BindProviderBinding getBindProviderBinding(Key<? extends Provider<?>> providerKey,
      Key<?> sourceKey, BindingContext context);

  CallConstructorBinding getCallConstructorBinding(MethodLiteral<?, Constructor<?>> constructor);

  CallGwtDotCreateBinding getCallGwtDotCreateBinding(TypeLiteral<?> type);

  ExposedChildBinding getExposedChildBinding(Key<?> key, GinjectorBindings childBindings,
      BindingContext context);

  FactoryBinding getFactoryBinding(Map<Key<?>, TypeLiteral<?>> collector, Key<?> factoryKey,
      BindingContext context);

  ImplicitProviderBinding getImplicitProviderBinding(Key<?> providerKey);

  ParentBinding getParentBinding(Key<?> key, GinjectorBindings parentBindings,
      BindingContext context);

  ProviderMethodBinding getProviderMethodBinding(ProviderMethod<?> providerMethod,
      BindingContext context);

  RemoteServiceProxyBinding getRemoteServiceProxyBinding(TypeLiteral<?> type);

  GinjectorBinding getGinjectorBinding();
}
