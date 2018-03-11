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
package com.google.gwt.inject.rebind.util;

import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * Generates Java expressions and statements that perform injection-related
 * duties.
 */
public interface InjectorWriteContext {

  /**
   * Generates a Java expression that evaluates to an injected instance of the
   * given key.
   */
  String callGetter(Key<?> key);

  /**
   * Generates a Java expression that evaluates to an injected instance of the
   * given key, as produced by the given child.
   */
  String callChildGetter(GinjectorBindings childBindings, Key<?> key);

  /**
   * Generates a Java expression that evaluates to an injected instance of the
   * given key, as produced by the given parent injector.
   */
  String callParentGetter(Key<?> key, GinjectorBindings parentBindings);

  /**
   * Generates a Java statement (including trailing semicolon) that performs
   * member injection on a value of the given type.
   *
   * @param type the type of value to perform member injection on
   * @param input a Java expression that evaluates to the object that should
   *     be member-injected
   */
  String callMemberInject(TypeLiteral<?> type, String input);

  /**
   * Generates a Java expression that evaluates to an invocation of the named
   * method on the given package fragment.
   *
   * <p>Used when generating an intermediate invoker method; see
   * {@link MethodCallUtil#createMethodCallWithInjection}.
   */
  String callMethod(String methodName, String fragmentPackageName, Iterable<String> parameters);

  /**
   * Generates a Java statement that evaluates to the implementation of the
   * current Ginjector interface.
   */
  String callGinjectorInterfaceGetter();
}
