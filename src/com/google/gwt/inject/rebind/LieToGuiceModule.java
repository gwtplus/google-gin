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

import com.google.gwt.core.ext.TreeLogger;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;

import java.util.ArrayList;
import java.util.List;

/**
 * A module to tell Guice about implicit bindings Gin has invented.
 */
class LieToGuiceModule extends AbstractModule {
  private final List<Module> lies = new ArrayList<Module>();
  private final TreeLogger logger;

  @Inject
  LieToGuiceModule(TreeLogger logger) {
    this.logger = logger;
  }

  protected void configure() {
    for (Module lie : lies) {
      install(lie);
    }
  }

  /**
   * Registers an implicit binding. This binding will result in a special
   * instance of Provider being registered with Guice. This provider will
   * never be called -- we give it to Guice just so that Guice knows this
   * key is bound.
   *
   * @param key Key to bind
   */
  <T> void registerImplicitBinding(Key<T> key) {
    logger.log(TreeLogger.Type.TRACE, "Implicit binding registered with Guice for " + key);
    lies.add(new ImplicitBindingModule<T>(key));
  }

  private class ImplicitBindingModule<T> implements Module, Provider<T> {
    private final Key<T> key;

    private ImplicitBindingModule(Key<T> key) {
      this.key = key;
    }

    public void configure(Binder binder) {
      logger.log(TreeLogger.Type.TRACE, "Binding " + key + "in Guice");
      binder.bind(key).toProvider(this);
    }

    public T get() {
      throw new ProvisionException("Gin implicit binding provider should not be called directly!");
    }
  }
}
