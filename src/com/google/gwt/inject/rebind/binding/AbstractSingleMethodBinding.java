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
package com.google.gwt.inject.rebind.binding;

import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.gwt.inject.rebind.util.AbstractInjectorMethod;
import com.google.gwt.inject.rebind.util.InjectorMethod;
import com.google.gwt.inject.rebind.util.InjectorWriteContext;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import java.util.Collections;

/**
 * Common base class for bindings that only output one non-native method.
 */
abstract class AbstractSingleMethodBinding extends AbstractBinding {

  protected AbstractSingleMethodBinding(Context context) {
    super(context);
  }

  protected AbstractSingleMethodBinding(Context context, Key<?> keyForPackage) {
    super(context, keyForPackage);
  }

  protected AbstractSingleMethodBinding(Context context, TypeLiteral<?> typeForPackage) {
    super(context, typeForPackage);
  }

  /**
   * Appends the body of the creator method for this binding to the given
   * {@link StringBuilder}.
   */

  // appendCreatorMethodBody doesn't pass along the NameGenerator because it's
  // only for use in generating additional method names, and by definition this
  // only has one method name.

  protected abstract void appendCreatorMethodBody(StringBuilder builder,
      InjectorWriteContext writeContext) throws NoSourceNameException;

  public final Iterable<InjectorMethod> getCreatorMethods(String creatorMethodSignature,
      NameGenerator nameGenerator) throws NoSourceNameException {
    return Collections.<InjectorMethod>singletonList(new CreatorMethod(creatorMethodSignature));
  }

  private final class CreatorMethod extends AbstractInjectorMethod {
    CreatorMethod(String creatorMethodSignature) {
      super(false, creatorMethodSignature, getGetterMethodPackage());
    }

    @Override
    public String getMethodBody(InjectorWriteContext writeContext) throws NoSourceNameException {
      StringBuilder builder = new StringBuilder();
      appendCreatorMethodBody(builder, writeContext);
      return builder.toString();
    }
  }
}
