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
package com.google.gwt.inject.rebind;

import com.google.gwt.inject.rebind.binding.BindingFactory;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.spi.Message;

import java.util.List;

/**
 * Factory for creating {@link GuiceBindingVisitor}s.  We can't use assisted
 * injection to create this, because one of the arguments is a Key, which makes
 * Guice unhappy.
 */
public class GuiceBindingVisitorFactory {
  private final BindingFactory bindingFactory;
  private final LieToGuiceModule lieToGuiceModule;

  @Inject
  public GuiceBindingVisitorFactory(LieToGuiceModule lieToGuiceModule,
      BindingFactory bindingFactory) {
    this.lieToGuiceModule = lieToGuiceModule;
    this.bindingFactory = bindingFactory;
  }

  /**
   * Create the {@link GuiceBindingVisitor}.
   * @param <T> the type of the target for the binding
   * @param targetKey the key that the Binding Visitor is visiting
   * @param messages the list of messages that should be added to if anything
   *     important happens
   * @param ginjectorBindings he {@link GinjectorBindings} to add bindings to
   */
  <T> GuiceBindingVisitor<T> create(Key<T> targetKey, List<Message> messages,
      GinjectorBindings ginjectorBindings) {
    return new GuiceBindingVisitor<T>(lieToGuiceModule, targetKey, messages, ginjectorBindings,
        bindingFactory);
  }
}
