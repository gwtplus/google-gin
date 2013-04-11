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

import static com.google.gwt.inject.client.multibindings.TypeLiterals.providerOf;
import static com.google.gwt.inject.client.multibindings.TypeLiterals.setOf;

import com.google.gwt.inject.client.binder.GinBinder;
import com.google.gwt.inject.client.binder.GinLinkedBindingBuilder;
import com.google.gwt.inject.client.multibindings.InternalModule.SingletonInternalModule;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

import java.lang.annotation.Annotation;

/**
 * A utility that mimics the behavior and API of Guice Multibinder for GIN.
 *
 * <p>Example usage:
 * <pre>
 *   interface X {};
 *
 *   class X1Impl implements X {};
 *
 *   class X2Impl implements X {};
 *
 *   class X3Provider implements Provider&lt;X&gt; { ... };
 *
 *   Set&lt;X&gt; multibinder = GinMultibinder.newSetBinder(binder(), X.class);
 *   multibinder.addBinding().to(X1Impl.class);
 *   multibinder.addBinding().to(X2Impl.class);
 *   multibinder.addBinding().toProvier(X3Provider.class);
 * </pre>
 *
 * @param <T> type of value for Set
 */
public final class GinMultibinder<T> {

  /**
   * Returns a new multibinder that collects instances of {@code type} in a {@link java.util.Set}
   * that is itself bound with no binding annotation.
   */
  public static <T> GinMultibinder<T> newSetBinder(GinBinder binder, TypeLiteral<T> type) {
    return newSetBinder(binder, type, Key.get(providerOf(type)));
  }

  /**
   * Returns a new multibinder that collects instances of {@code type} in a {@link java.util.Set}
   * that is itself bound with no binding annotation.
   */
  public static <T> GinMultibinder<T> newSetBinder(GinBinder binder, Class<T> type) {
    return newSetBinder(binder, TypeLiteral.get(type));
  }

  /**
   * Returns a new multibinder that collects instances of {@code type} in a {@link java.util.Set}
   * that is itself bound with {@code annotation}.
   */
  public static <T> GinMultibinder<T> newSetBinder(
      GinBinder binder, TypeLiteral<T> type, Annotation annotation) {
    return newSetBinder(binder, type, Key.get(providerOf(type), annotation));
  }

  /**
   * Returns a new multibinder that collects instances of {@code type} in a {@link java.util.Set}
   * that is itself bound with {@code annotation}.
   */
  public static <T> GinMultibinder<T> newSetBinder(
      GinBinder binder, Class<T> type, Annotation annotation) {
    return newSetBinder(binder, TypeLiteral.get(type), annotation);
  }

  /**
   * Returns a new multibinder that collects instances of {@code type} in a {@link java.util.Set}
   * that is itself bound with {@code annotationType}.
   */
  public static <T> GinMultibinder<T> newSetBinder(
      GinBinder binder, TypeLiteral<T> type, Class<? extends Annotation> annotationType) {
    return newSetBinder(binder, type, Key.get(providerOf(type), annotationType));
  }

  /**
   * Returns a new multibinder that collects instances of {@code type} in a {@link Set} that is
   * itself bound with {@code annotationType}.
   */
  public static <T> GinMultibinder<T> newSetBinder(
      GinBinder binder, Class<T> type, Class<? extends Annotation> annotationType) {
    return newSetBinder(binder, TypeLiteral.get(type), annotationType);
  }

  private static <T> GinMultibinder<T> newSetBinder(
      GinBinder ginBinder, TypeLiteral<T> elementType, Key<Provider<T>> keyForMultibinding) {
    GinMultibinder<T> multiBinder =
        new GinMultibinder<T>(ginBinder, elementType, keyForMultibinding);
    multiBinder.install();
    return multiBinder;
  }

  private final GinBinder ginBinder;
  private final TypeLiteral<T> elementType;
  private final Key<Provider<T>> multibindingKey;

  public GinMultibinder(
      GinBinder ginBinder, TypeLiteral<T> elementType, Key<Provider<T>> keyForMultibinding) {
    this.ginBinder = ginBinder;
    this.elementType = elementType;
    this.multibindingKey = keyForMultibinding;
  }

  private void install() {
    ginBinder.install(new RuntimeBindingsRegistryModule<Provider<T>>(multibindingKey));
    ginBinder.install(new SetModule());
  }


  /**
   * Configures the bound set to silently discard duplicate elements. When multiple equal values are
   * bound, the one that gets included is arbitrary. When multiple modules contribute elements to
   * the set, this configuration option impacts all of them.
   */
  public GinMultibinder<T> permitDuplicates() {
    ginBinder.install(new PermitDuplicatesModule<Provider<T>>(multibindingKey));
    return this;
  }

  /**
   * Returns a binding builder used to add a new element in the set. Each bound element must have a
   * distinct value. Bound providers will be evaluated each time the set is injected.
   * <p>
   * It is an error to call this method without also calling one of the {@code to} methods on the
   * returned binding builder.
   * <p>
   * Scoping elements independently is supported. Use the {@code in} method to specify a binding
   * scope.
   */
  public GinLinkedBindingBuilder<T> addBinding() {
    return new BindingRecorder(ginBinder, multibindingKey).bind(elementType);
  }

  // TODO(user): not private due to http://code.google.com/p/google-gin/issues/detail?id=184
  final class SetModule extends SingletonInternalModule<Provider<T>> {

    public SetModule() {
      super(multibindingKey);
    }

    @Override
    protected void configure() {
      bindInternalBindingsRegistry();
      bindAndExpose(setOf(elementType)).toProvider(Key.get(providerForSetOf(elementType)));
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> TypeLiteral<ProviderForSet<T>> providerForSetOf(TypeLiteral<T> type) {
    return TypeLiterals.newParameterizedType(ProviderForSet.class, type);
  }
}
