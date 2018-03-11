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

package com.google.gwt.inject.rebind.reflect.subpackage;

import com.google.gwt.inject.rebind.reflect.ReflectUtilTest;

import com.google.inject.TypeLiteral;

public class BogusTypeLiteralMaker extends ReflectUtilTest.HasProtectedInnerClass {
  /**
   * Gets a TypeLiteral for ProtectedSubPackageClass<ProtectedInnerClass>, which
   * is only accessible from this class.
   */
  public static TypeLiteral<?> getBogusTypeLiteral() {
    return new TypeLiteral<ProtectedSubPackageClass<ProtectedInnerClass>>() {};
  }

  public static TypeLiteral<?> getProtectedClassTypeLiteral() {
    return new TypeLiteral<ProtectedSubPackageClass<Integer>>() {};
  }

  static class ProtectedSubPackageClass<T> {
  }
}
