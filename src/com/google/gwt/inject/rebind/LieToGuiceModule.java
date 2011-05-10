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
package com.google.gwt.inject.rebind;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;

import java.util.Set;

/**
 * A module to tell Guice to pretend that some bindings exist (especially bindings Gin "invented").
 */
public class LieToGuiceModule extends AbstractModule {
  private final Set<Key<?>> lies;

  /**
   * @param lies the keys that Gin has invented, and which should be lied about
   */
  LieToGuiceModule(Set<Key<?>> lies) {
    this.lies = lies;
  }

  protected void configure() {
    for (Key<?> lie : lies) {
      installLie(lie);
    }
  }
  
  private <T> void installLie(Key<T> key) {
    install(new FakeBindingModule<T>(key));
  }

  private static class FakeBindingModule<T> implements Module, Provider<T> {
    private final Key<T> key;

    private FakeBindingModule(Key<T> key) {
      this.key = key;
    }

    public void configure(Binder binder) {
      binder.bind(key).toProvider(this);
    }

    public T get() {
      throw new ProvisionException("Gin implicit binding provider should not be called directly!");
    }
  }
}
