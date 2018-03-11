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

import static com.google.gwt.inject.client.multibindings.TypeLiterals.mapOf;
import static com.google.gwt.inject.client.multibindings.TypeLiterals.providerOf;
import static com.google.gwt.inject.client.multibindings.TypeLiterals.setOf;

import com.google.gwt.inject.client.binder.GinBinder;
import com.google.gwt.inject.client.binder.GinLinkedBindingBuilder;
import com.google.gwt.inject.client.binder.GinScopedBindingBuilder;
import com.google.gwt.inject.client.multibindings.InternalModule.SingletonInternalModule;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * A utility that mimics the behavior and API of Guice MapBinder for GIN.
 *
 * <p>Example usage:
 * <pre>
 *   interface X {};
 *
 *   class X1Impl implements X {};
 *
 *   class X2Impl implements X {};
 *
 *   class X3Provider implements Provider&lt;X&gt; {};
 *
 *   GinMapBinder&lt;String, X&gt; mapBinder =
 *       GinMapBinder.newMapBinder(binder(), String.class, X.class);
 *   mapBinder.addBinding("id1").to(X1Impl.class);
 *   mapBinder.addBinding("id2").to(X2Impl.class);
 *   mapBinder.addBinding("id3").toProvider(X3Provider.class);
 * </pre>
 *
 * <p>
 * GIN supports instance binding for only limited set of types. To overcome this limitation,
 * GinMapBinder provides {@link #addBinding(Class)} method so bindings can be added via a key
 * provider class that will instantiate the actual key during runtime. This alternative approach
 * is needed to used for all key types that cannot be bound via
 * {@link com.google.gwt.inject.client.binder.GinConstantBindingBuilder}:
 * <pre>
 *   class Place {
 *     public Place(String key) { ... }
 *   }
 *
 *   class HomePlaceProvider implements Provider&lt;Place&gt; {
 *     public Place get() {
 *       return new Place("home");
 *     }
 *   }
 *
 *   class AboutPlaceProvider implements Provider&lt;Place&gt; {
 *     public Place get() {
 *       return new Place("about");
 *     }
 *   }
 *
 *   GinMapBinder&lt;Place, X&gt; mapBinder =
 *       GinMapBinder.newMapBinder(binder(), Place.class, X.class);
 *   mapBinder.addBinding(HomePlaceProvider.class).to(XImpl1.class);
 *   mapBinder.addBinding(AboutPlaceProvider.class).to(XImpl2.class);
 * </pre>
 * <p>
 *
 * @param <K> type of key for map
 * @param <V> type of value for map
 */
public final class GinMapBinder<K, V> {

  /**
   * Returns a new mapbinder that collects entries of {@code keyType}/{@code valueType} in a
   * {@link Map} that is itself bound with no binding annotation.
   */
  public static <K, V> GinMapBinder<K, V> newMapBinder(
      GinBinder binder, TypeLiteral<K> keyType, TypeLiteral<V> valueType) {
    return newMapBinder(binder, keyType, valueType, Key.get(entryOf(keyType, valueType)));
  }

  /**
   * Returns a new mapbinder that collects entries of {@code keyType}/{@code valueType} in a
   * {@link Map} that is itself bound with no binding annotation.
   */
  public static <K, V> GinMapBinder<K, V> newMapBinder(
      GinBinder binder, Class<K> keyType, Class<V> valueType) {
    return newMapBinder(binder, TypeLiteral.get(keyType), TypeLiteral.get(valueType));
  }

  /**
   * Returns a new mapbinder that collects entries of {@code keyType}/{@code valueType} in a
   * {@link Map} that is itself bound with {@code annotation}.
   */
  public static <K, V> GinMapBinder<K, V> newMapBinder(
      GinBinder binder, TypeLiteral<K> keyType, TypeLiteral<V> valueType, Annotation annotation) {
    return newMapBinder(
        binder, keyType, valueType, Key.get(entryOf(keyType, valueType), annotation));
  }

  /**
   * Returns a new mapbinder that collects entries of {@code keyType}/{@code valueType} in a
   * {@link Map} that is itself bound with {@code annotation}.
   */
  public static <K, V> GinMapBinder<K, V> newMapBinder(
      GinBinder binder, Class<K> keyType, Class<V> valueType, Annotation annotation) {
    return newMapBinder(binder, TypeLiteral.get(keyType), TypeLiteral.get(valueType), annotation);
  }

  /**
   * Returns a new mapbinder that collects entries of {@code keyType}/{@code valueType} in a
   * {@link Map} that is itself bound with {@code annotationType}.
   */
  public static <K, V> GinMapBinder<K, V> newMapBinder(GinBinder binder, TypeLiteral<K> keyType,
      TypeLiteral<V> valueType, Class<? extends Annotation> annotationType) {
    return newMapBinder(
        binder, keyType, valueType, Key.get(entryOf(keyType, valueType), annotationType));
  }

  /**
   * Returns a new mapbinder that collects entries of {@code keyType}/{@code valueType} in a
   * {@link Map} that is itself bound with {@code annotationType}.
   */
  public static <K, V> GinMapBinder<K, V> newMapBinder(GinBinder binder, Class<K> keyType,
      Class<V> valueType, Class<? extends Annotation> annotationType) {
    return newMapBinder(
        binder, TypeLiteral.get(keyType), TypeLiteral.get(valueType), annotationType);
  }

  private static <K, V> GinMapBinder<K, V> newMapBinder(GinBinder binder, TypeLiteral<K> keyType,
      TypeLiteral<V> valueType, Key<MapEntry<K, V>> registryKey) {
    GinMapBinder<K, V> mapBinder = new GinMapBinder<K, V>(binder, keyType, valueType, registryKey);
    mapBinder.install();
    return mapBinder;
  }

  private final GinBinder ginBinder;
  private final TypeLiteral<K> keyType;
  private final TypeLiteral<V> valueType;
  private final Key<MapEntry<K, V>> multibindingKey;

  private GinMapBinder(GinBinder ginBinder, TypeLiteral<K> keyType, TypeLiteral<V> valueType,
      Key<MapEntry<K, V>> keyForMultibinding) {
    this.ginBinder = ginBinder;
    this.keyType = keyType;
    this.valueType = valueType;
    this.multibindingKey = keyForMultibinding;
  }

  private void install() {
    ginBinder.install(new RuntimeBindingsRegistryModule<MapEntry<K, V>>(multibindingKey));
    ginBinder.install(new MapModule());
  }

  /**
   * Configures the {@code MapBinder} to handle duplicate entries.
   * <p>
   * When multiple equal keys are bound, the value that gets included in the map is arbitrary.
   * <p>
   * In addition to the {@code Map<K, V>} and {@code Map<K, Provider<V>>} maps that are normally
   * bound, a {@code Map<K, Set<V>>} and {@code Map<K, Set<Provider<V>>>} are <em>also</em> bound,
   * which contain all values bound to each key.
   * <p>
   * When multiple modules contribute elements to the map, this configuration option impacts all of
   * them.
   *
   * @return this map binder
   */
  public GinMapBinder<K, V> permitDuplicates() {
    ginBinder.install(new PermitDuplicatesModule<MapEntry<K, V>>(multibindingKey));
    ginBinder.install(new MultimapModule());
    return this;
  }

  /**
   * Returns a binding builder used to add a new entry in the map. Each key must be distinct (and
   * non-null). Bound providers will be evaluated each time the map is injected.
   * <p>
   * It is an error to call this method without also calling one of the {@code to} methods on the
   * returned binding builder.
   * <p>
   * Scoping elements independently is supported. Use the {@code in} method to specify a binding
   * scope.
   */
  public GinLinkedBindingBuilder<V> addBinding(K key) {
    BindingRecorder recorder = createRecorder();
    if (key instanceof String) {
      recorder.bindConstant().to((String) key);
    } else if (key instanceof Enum<?>) {
      recorder.bindConstant().to((Enum) key);
    } else if (key instanceof Integer) {
      recorder.bindConstant().to((Integer) key);
    } else if (key instanceof Long) {
      recorder.bindConstant().to((Long) key);
    } else if (key instanceof Float) {
      recorder.bindConstant().to((Float) key);
    } else if (key instanceof Double) {
      recorder.bindConstant().to((Double) key);
    } else if (key instanceof Short) {
      recorder.bindConstant().to((Short) key);
    } else if (key instanceof Boolean) {
      recorder.bindConstant().to((Boolean) key);
    } else if (key instanceof Character) {
      recorder.bindConstant().to((Character) key);
    } else if (key instanceof Class<?>) {
      recorder.bindConstant().to((Class<?>) key);
    } else {
      throw new IllegalArgumentException(
          "Key type " + keyType + " is non-constant and can only be added using providers");
    }
    return recorder.bind(valueType);
  }

  /**
   * Returns a binding builder used to add a new entry in the map using a key provider.
   * <p>
   * This API is not compatible with Guice however it is provided as GIN has limitation to bind
   * 'instances'. For that reason for all key types that are not defined in
   * {@link com.google.gwt.inject.client.binder.GinConstantBindingBuilder} needs to use a provider
   * class for each key together with this method.
   *
   * @see #addBinding(Object)
   */
  public GinLinkedBindingBuilder<V> addBinding(
      Class<? extends javax.inject.Provider<? extends K>> keyProvider) {
    return addBinding(TypeLiteral.get(keyProvider));
  }

  /**
   * Returns a binding builder used to add a new entry in the map using a key provider.
   * <p>
   * This API is not compatible with Guice however it is provided as GIN has limitation to bind
   * 'instances'. For that reason for all key types that are not defined in
   * {@link com.google.gwt.inject.client.binder.GinConstantBindingBuilder} needs to use a provider
   * class for each key together with this method.
   *
   * @see #addBinding(Object)
   */
  public GinLinkedBindingBuilder<V> addBinding(
      TypeLiteral<? extends javax.inject.Provider<? extends K>> keyProvider) {
    BindingRecorder recorder = createRecorder();
    recorder.bind(keyType).toProvider(Key.get(keyProvider));
    return recorder.bind(valueType);
  }

  private BindingRecorder createRecorder() {
    BindingRecorder recorder = new BindingRecorder(ginBinder, multibindingKey);
    // binds @Internal MapEntry<K, V> to MapEntry
    recorder.bind(multibindingKey.getTypeLiteral()).to(multibindingKey.getTypeLiteral());
    return recorder;
  }

  // TODO(user): not private due to http://code.google.com/p/google-gin/issues/detail?id=184
  final class MapModule extends AbstractMapModule {
    @Override
    protected void configure() {
      bindInternalBindingsRegistry();
      bindMap(valueType, providerForMapOf(keyType, valueType));
      bindMap(providerOf(valueType), providerForProviderMapOf(keyType, valueType))
          .in(Singleton.class);
    }
  }

  // TODO(user): not private due to http://code.google.com/p/google-gin/issues/detail?id=184
  final class MultimapModule extends AbstractMapModule {
    @Override
    protected void configure() {
      bindInternalBindingsRegistry();
      bindMap(setOf(valueType), providerForMultiMapOf(keyType, valueType));
      bindMap(setOf(providerOf(valueType)), providerForProviderMultiMapOf(keyType, valueType))
          .in(Singleton.class);
    }
  }

  private abstract class AbstractMapModule extends SingletonInternalModule<MapEntry<K, V>> {
    public AbstractMapModule() {
      super(multibindingKey);
    }

    protected <V> GinScopedBindingBuilder bindMap(
        TypeLiteral<V> valueType, TypeLiteral<? extends Provider<Map<K, V>>> providerType) {
      return bindAndExpose(mapOf(keyType, valueType)).toProvider(Key.get(providerType));
    }
  }

  @SuppressWarnings("unchecked")
  private static <K, V> TypeLiteral<MapEntry<K, V>> entryOf(
      TypeLiteral<K> keyType, TypeLiteral<V> valueType) {
    return TypeLiterals.newParameterizedType(MapEntry.class, keyType, valueType);
  }

  @SuppressWarnings("unchecked")
  private static <K, V> TypeLiteral<ProviderForMap<K, V>> providerForMapOf(TypeLiteral<K> keyType,
      TypeLiteral<V> valueType) {
    return TypeLiterals.newParameterizedType(ProviderForMap.class, keyType, valueType);
  }

  @SuppressWarnings("unchecked")
  private static <K, V> TypeLiteral<ProviderForMultiMap<K, V>> providerForMultiMapOf(
      TypeLiteral<K> keyType, TypeLiteral<V> valueType) {
    return TypeLiterals.newParameterizedType(ProviderForMultiMap.class, keyType, valueType);
  }

  @SuppressWarnings("unchecked")
  private static <K, V> TypeLiteral<ProviderForProviderMap<K, V>> providerForProviderMapOf(
      TypeLiteral<K> keyType, TypeLiteral<V> valueType) {
    return TypeLiterals.newParameterizedType(ProviderForProviderMap.class, keyType, valueType);
  }

  @SuppressWarnings("unchecked")
  private static <K, V> TypeLiteral<ProviderForProviderMultiMap<K, V>>
      providerForProviderMultiMapOf(TypeLiteral<K> keyType, TypeLiteral<V> valueType) {
    return TypeLiterals.newParameterizedType(ProviderForProviderMultiMap.class, keyType, valueType);
  }
}
