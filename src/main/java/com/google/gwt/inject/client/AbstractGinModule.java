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
package com.google.gwt.inject.client;

import com.google.gwt.inject.client.binder.GinAnnotatedBindingBuilder;
import com.google.gwt.inject.client.binder.GinAnnotatedConstantBindingBuilder;
import com.google.gwt.inject.client.binder.GinLinkedBindingBuilder;
import com.google.gwt.inject.client.binder.GinBinder;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * A support class for {@link GinModule}s which reduces repetition and results in
 * a more readable configuration. Simply extend this class, implement {@link
 * #configure()}, and call the inherited methods which mirror those found in
 * {@link GinBinder}. For example:
 *
 * <pre>
 * public class MyModule extends AbstractGinModule {
 *   protected void configure() {
 *     bind(Service.class).to(ServiceImpl.class).in(Singleton.class);
 *     bind(CreditCardPaymentService.class);
 *     bind(PaymentService.class).to(CreditCardPaymentService.class);
 *   }
 * }
 * </pre>
 */
public abstract class AbstractGinModule implements GinModule {
  private GinBinder binder;

  public final void configure(GinBinder binder) {
    this.binder = binder;
    configure();
  }

  protected abstract void configure();

  /**
   * @see GinBinder#bind(Class)
   */
  protected final <T> GinAnnotatedBindingBuilder<T> bind(Class<T> clazz) {
    return binder.bind(clazz);
  }

  /**
   * @see GinBinder#bind(TypeLiteral)
   */
  protected final <T> GinAnnotatedBindingBuilder<T> bind(TypeLiteral<T> type) {
    return binder.bind(type);
  }

  /**
   * @see GinBinder#bind(Key)
   */
  protected final <T> GinLinkedBindingBuilder<T> bind(Key<T> key) {
    return binder.bind(key);
  }

  /**
   * @see GinBinder#bindConstant()
   */
  protected final GinAnnotatedConstantBindingBuilder bindConstant() {
    return binder.bindConstant();
  }

  /**
   * @see GinBinder#install(Module)
   */
  protected final void install(GinModule install) {
    binder.install(install);
  }

  /**
   * @see GinBinder#requestStaticInjection(Class[])
   */
  protected void requestStaticInjection(Class<?>... types) {
    binder.requestStaticInjection(types);
  }

  /**
   * Gets direct access to the underlying {@code GinBinder}.
   */
  protected GinBinder binder() {
    return binder;
  }
}
