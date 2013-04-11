/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.inject.client.multibindings;

import com.google.gwt.inject.client.PrivateGinModule;
import com.google.gwt.inject.client.binder.GinLinkedBindingBuilder;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * A helper base module to help scoping of modules.
 * <p>
 * {@code InternalModule}s are used extensively in GIN multibindings to be able to inject any key in
 * a generic way regardless of their annotations without any interference from other bindings
 * already defined in external {@code GinModule}s modules or other multibindings. For example, using
 * an InternalModule, a String key for a map binding can be injected to its corresponding registerer
 * using a generic binding definition.
 *
 * @param <T> type of multibinding
 */
abstract class InternalModule<T> extends PrivateGinModule {

  private Key<T> multibindingKey;

  public InternalModule(Key<T> multibindingKey) {
    this.multibindingKey = multibindingKey;
  }

  protected final Key<T> multibindingKey() {
    return multibindingKey;
  }

  protected final TypeLiteral<T> multibindingType() {
    return multibindingKey.getTypeLiteral();
  }

  protected final TypeLiteral<RuntimeBindingsRegistry<T>> bindingsRegistry() {
    return registryOf(multibindingKey().getTypeLiteral());
  }

  protected final <V> Key<V> annotated(TypeLiteral<V> type) {
    return multibindingKey().ofType(type);
  }

  protected final <T> GinLinkedBindingBuilder<T> bindAndExpose(TypeLiteral<T> type) {
    expose(annotated(type));
    return bind(annotated(type));
  }

  /**
   * Make BindingsRegistry available inside our PrivateModule via @Internal annotation as there is
   * no other way to inject a registry to our generic internal classes when the multibindings is
   * being configured with user defined annotations.
   */
  protected final void bindInternalBindingsRegistry() {
    bind(bindingsRegistry()).annotatedWith(Internal.class).to(annotated(bindingsRegistry()));
  }

  /**
   * An {@link InternalModule} that is installed only once per multibinding key.
   */
  abstract static class SingletonInternalModule<T> extends InternalModule<T> {

    public SingletonInternalModule(Key<T> keyForMultibinding) {
      super(keyForMultibinding);
    }

    @Override
    public int hashCode() {
      return multibindingKey().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return obj != null && obj.getClass() == getClass()
          && ((InternalModule<?>) obj).multibindingKey().equals(multibindingKey());
    }
  }

  @SuppressWarnings("unchecked")
  private static <V> TypeLiteral<RuntimeBindingsRegistry<V>> registryOf(TypeLiteral<V> type) {
    return TypeLiterals.newParameterizedType(RuntimeBindingsRegistry.class, type);
  }
}