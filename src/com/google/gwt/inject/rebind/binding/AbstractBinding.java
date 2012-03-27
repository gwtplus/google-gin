/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.dev.util.Preconditions;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import java.util.Collection;
import java.util.Collections;

/**
 * Common base class for implementations of {@link Binding}.
 *
 * <p>Provides an implementation of {@link Binding#getContext()} and an
 * implementation of {@link Binding#getGetterMethodPackage}, which return values
 * passed to this class's constructor.  Subclasses may choose to generate their
 * getter method package dynamically, in which case they should override
 * {@link #getGetterMethodPackage} and omit that parameter when invoking the
 * base class's constructor.
 */
abstract class AbstractBinding implements Binding {
  private final Context context;
  private final String getterMethodPackage;

  private AbstractBinding(Context context, String getterMethodPackage) {
    this.context = Preconditions.checkNotNull(context);
    this.getterMethodPackage = Preconditions.checkNotNull(getterMethodPackage);
  }

  /**
   * Creates an abstract binding with no getter method package.
   *
   * <p>This should only be used by bindings that override
   * {@link #getGetterMethodPackage}.
   */
  protected AbstractBinding(Context context) {
    this.context = Preconditions.checkNotNull(context);
    this.getterMethodPackage = null;
  }

  /**
   * Creates an abstract binding that is constructed in the package of the type
   * component of the given key.
   */
  protected AbstractBinding(Context context, Key<?> keyForPackage) {
    this(context, ReflectUtil.getUserPackageName(keyForPackage));
  }

  /**
   * Creates an abstract binding that is constructed in the package of the given
   * type.
   */
  protected AbstractBinding(Context context, TypeLiteral<?> typeForPackage) {
    this(context, ReflectUtil.getUserPackageName(typeForPackage));
  }

  public Context getContext() {
    return context;
  }

  public String getGetterMethodPackage() {
    return getterMethodPackage;
  }

  public Collection<TypeLiteral<?>> getMemberInjectRequests() {
    return Collections.emptySet();
  }
}
