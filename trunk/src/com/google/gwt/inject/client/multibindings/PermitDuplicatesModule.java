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

import com.google.gwt.inject.client.multibindings.InternalModule.SingletonInternalModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * A helper module to add 'permitDuplicates' configuration of T to {@link RuntimeBindingsRegistry}.
 * <p>
 * This class works very similar to
 * {@link com.google.gwt.inject.client.multibindings.BindingRecorder.BindingRegistererModule},
 * except that this one transfers "permitDuplicates" information to runtime instead of a binding.
 *
 * @param <T> type of multibinding
 */
class PermitDuplicatesModule<T> extends SingletonInternalModule<T> {

  public PermitDuplicatesModule(Key<T> multibindingKey) {
    super(multibindingKey);
  }

  @Override
  protected void configure() {
    bindInternalBindingsRegistry();
    bind(permitDuplicateRegistererOf(multibindingType())).asEagerSingleton();
  }

  static class PermitDuplicateRegisterer<T> {
    @Inject
    public PermitDuplicateRegisterer(@Internal RuntimeBindingsRegistry<T> registry) {
      registry.permitDuplicates();
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> TypeLiteral<PermitDuplicateRegisterer<T>> permitDuplicateRegistererOf(
      TypeLiteral<T> type) {
    return TypeLiterals.newParameterizedType(PermitDuplicateRegisterer.class, type);
  }
}
