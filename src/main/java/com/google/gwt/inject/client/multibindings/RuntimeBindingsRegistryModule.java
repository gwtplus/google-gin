// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.gwt.inject.client.multibindings;

import com.google.gwt.inject.client.multibindings.InternalModule.SingletonInternalModule;
import com.google.inject.Key;
import com.google.inject.Singleton;

/**
 * A module to bind and expose {@link RuntimeBindingsRegistry} for a multibinding key.
 *
 * @param <T> type of binding
 */
class RuntimeBindingsRegistryModule<T> extends SingletonInternalModule<T> {

  public RuntimeBindingsRegistryModule(Key<T> multibindingKey) {
    super(multibindingKey);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void configure() {
    // Safe to use unchecked RuntimeBindingsRegistry.class as its new instances are always a valid
    // substitution for any RuntimeBindingsRegistry<T>.
    bindAndExpose(bindingsRegistry()).to(RuntimeBindingsRegistry.class).in(Singleton.class);
  }
}
