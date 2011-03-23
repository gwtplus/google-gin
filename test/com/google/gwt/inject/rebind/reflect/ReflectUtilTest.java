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

package com.google.gwt.inject.rebind.reflect;

import com.google.inject.TypeLiteral;

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

public class ReflectUtilTest extends TestCase {

  public void testGetSourceName() throws NoSourceNameException {
    assertEquals("com.google.gwt.inject.rebind.reflect.ReflectUtilTest",
        ReflectUtil.getSourceName(ReflectUtilTest.class));
  }

  public void testGetSourceName_nestedClass() throws NoSourceNameException {
    assertEquals("com.google.gwt.inject.rebind.reflect.ReflectUtilTest.Nested",
        ReflectUtil.getSourceName(Nested.class));
  }

  public void testGetSourceName_typeLiteral() throws NoSourceNameException {
    assertEquals("com.google.gwt.inject.rebind.reflect.ReflectUtilTest",
        ReflectUtil.getSourceName(TypeLiteral.get(ReflectUtilTest.class)));
  }

  public void testGetSourceName_parametrizedType() throws NoSourceNameException {
    assertEquals(
        "com.google.gwt.inject.rebind.reflect.ReflectUtilTest.Parametrized<java.lang.String>",
        ReflectUtil.getSourceName(new TypeLiteral<Parametrized<String>>() {}));
  }

  public void testGetSourceName_primitive() throws NoSourceNameException, NoSuchMethodException {
    assertEquals("int", ReflectUtil.getSourceName(Integer.TYPE));
    Method method = ReflectUtilTest.class.getDeclaredMethod("testGetSourceName_primitive");
    assertEquals("void", ReflectUtil.getSourceName(method.getReturnType()));
    assertEquals("java.lang.Integer", ReflectUtil.getSourceName(Integer.class));
  }

  public void testGetSourceName_typeVariable() throws NoSourceNameException, NoSuchMethodException {
    Method method = ReflectUtilTest.class.getDeclaredMethod("typeVariableMethod");
    assertEquals("T", ReflectUtil.getSourceName(method.getTypeParameters()[0]));
  }

  public void testGetSourceName_anonymous() {
    try {
      ReflectUtil.getSourceName(new ReflectUtilTest() {}.getClass());
      fail("Expected NoSourceNameException.");
    } catch (NoSourceNameException e) {
      // Expected.
    }
  }

  public void testGetSourceName_array() throws NoSourceNameException {
    assertEquals("java.lang.String[]", ReflectUtil.getSourceName(String[].class));
  }

  public void testGetSignature() throws NoSuchMethodException, NoSourceNameException {
    MethodLiteral<?, ?> method =
        getMethod(TypeLiteral.get(ReflectUtilTest.class), "simpleMethod");
    assertEquals("private void simpleMethod()", ReflectUtil.getSignature(method));
  }

  public void testGetSignature_constructor() throws NoSuchMethodException, NoSourceNameException {
    assertEquals("protected com.google.gwt.inject.rebind.reflect.ReflectUtilTest.ConstructorType()",
        ReflectUtil.getSignature(getConstructor(TypeLiteral.get(ConstructorType.class))));
  }

  public void testGetSignature_parameters() throws NoSuchMethodException, NoSourceNameException {
    MethodLiteral<?, ?> method =
        getMethod(TypeLiteral.get(ReflectUtilTest.class),
            "methodWithParameters", String.class, Integer.class);
    assertEquals("private void methodWithParameters(java.lang.String _0, java.lang.Integer _1)",
        ReflectUtil.getSignature(method));
  }

  public void testGetSignature_overriddenModifiers()
      throws NoSuchMethodException, NoSourceNameException {
    MethodLiteral<?, ?> method =
        getMethod(TypeLiteral.get(ReflectUtilTest.class), "simpleMethod");
    int modifiers = method.getModifiers() & ~Modifier.PRIVATE | Modifier.ABSTRACT | Modifier.PUBLIC;

    assertEquals("public abstract void simpleMethod()",
        ReflectUtil.getSignature(method, new String[0], modifiers)); 
  }

  public void testGetSignature_typeParametrizedMethod()
      throws NoSuchMethodException, NoSourceNameException {
    MethodLiteral<?, ?> method =
        getMethod(TypeLiteral.get(ReflectUtilTest.class), "parametrizedMethod", Object.class);
    assertEquals(
        "private <T extends java.lang.CharSequence & java.util.Map, V> T parametrizedMethod(V _0)",
        ReflectUtil.getSignature(method));
  }

  public void testGetSignature_paramNames() throws NoSuchMethodException, NoSourceNameException {
    MethodLiteral<?, ?> method =
        getMethod(TypeLiteral.get(ReflectUtilTest.class),
            "methodWithParameters", String.class, Integer.class);
    assertEquals("private void methodWithParameters(java.lang.String bar, java.lang.Integer foo)",
        ReflectUtil.getSignature(method, new String[] { "bar", "foo" }));

    try {
      ReflectUtil.getSignature(method, new String[] { "bar" });
      fail("Expected IllegalArgumentException.");
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  public void testGetSignature_exceptions() throws NoSuchMethodException, NoSourceNameException {
    MethodLiteral<?, ?> method =
        getMethod(TypeLiteral.get(ReflectUtilTest.class), "throwingMethod");
    assertEquals(
        "private void throwingMethod() throws java.lang.IllegalArgumentException, "
            + "com.google.gwt.inject.rebind.reflect.ReflectUtilTest.ExampleException",
        ReflectUtil.getSignature(method));
  }

  public void testGetSignature_parametrizedDeclaringType()
      throws NoSuchMethodException, NoSourceNameException {
    TypeLiteral<ParametrizedMethods<CharSequence, ExampleException, String>> type =
        new TypeLiteral<ParametrizedMethods<CharSequence, ExampleException, String>>() {};

    MethodLiteral<?, ?> methodWithParameters = getMethod(type, "methodWithParameters",
        Object.class, CharSequence.class);
    assertEquals("void methodWithParameters(java.lang.CharSequence _0, java.lang.String _1)",
        ReflectUtil.getSignature(methodWithParameters));

    MethodLiteral<?, ?> returningMethod = getMethod(type, "returningMethod");
    assertEquals("java.lang.CharSequence returningMethod()",
        ReflectUtil.getSignature(returningMethod));

    MethodLiteral<?, ?> throwingMethod = getMethod(type, "throwingMethod");
    assertEquals("void throwingMethod() "
        + "throws com.google.gwt.inject.rebind.reflect.ReflectUtilTest.ExampleException",
        ReflectUtil.getSignature(throwingMethod));

    MethodLiteral<?, ?> parametrizedMethod = getMethod(type, "parametrizedMethod");
    assertEquals("<V extends T> V parametrizedMethod()",
        ReflectUtil.getSignature(parametrizedMethod));
  }

  public void testGetTypeVariableDefinition() throws NoSourceNameException, NoSuchMethodException {
    Method method = ReflectUtilTest.class.getDeclaredMethod("typeVariableMethod");
    assertEquals("T extends java.lang.CharSequence",
        ReflectUtil.getTypeVariableDefinition(method.getTypeParameters()[0]));

    Method combinedMethod = ReflectUtilTest.class.getDeclaredMethod("combinedTypeVariableMethod");
    assertEquals(
        "T extends java.lang.CharSequence & java.util.Map<java.lang.String, java.lang.String>",
        ReflectUtil.getTypeVariableDefinition(combinedMethod.getTypeParameters()[0]));
  }

  private MethodLiteral<?, ?> getConstructor(TypeLiteral<?> type) throws NoSuchMethodException {
    return MethodLiteral.get(type.getRawType().getDeclaredConstructor(), type);
  }

  private MethodLiteral<?, ?> getMethod(TypeLiteral<?> type, String methodName, Class... paramTypes)
      throws NoSuchMethodException {
    return MethodLiteral.get(type.getRawType().getDeclaredMethod(methodName, paramTypes), type);
  }

  public void testLoadClass() throws ClassNotFoundException {
    assertEquals(ReflectUtilTest.class,
        ReflectUtil.loadClass("com.google.gwt.inject.rebind.reflect.ReflectUtilTest",
            ReflectUtilTest.class.getClassLoader()));
  }

  public void testLoadClass_nestedClass() throws ClassNotFoundException {
    assertEquals(Nested.class,
        ReflectUtil.loadClass("com.google.gwt.inject.rebind.reflect.ReflectUtilTest.Nested",
            ReflectUtilTest.class.getClassLoader()));

    assertEquals(Nested.DoublyNested.class,
        ReflectUtil.loadClass(
            "com.google.gwt.inject.rebind.reflect.ReflectUtilTest.Nested.DoublyNested",
            ReflectUtilTest.class.getClassLoader()));
  }

  //
  // All following types and members are used as test data.
  //

  static class Nested {
    static class DoublyNested {}
  }
  static abstract class Parametrized<T extends CharSequence> {}

  private <T extends CharSequence, V> T typeVariableMethod() {
    return null;
  }

  private <T extends CharSequence & Map<String, String>> T combinedTypeVariableMethod() {
    return null;
  }

  private void simpleMethod() {}

  private void methodWithParameters(String param1, Integer param2) {}

  private void throwingMethod() throws IllegalArgumentException, ExampleException {}

  private <T extends CharSequence & Map, V> T parametrizedMethod(V param) { return null; }

  static class ConstructorType {
    protected ConstructorType() {}
  }

  static class ExampleException extends Exception {}

  static class ParametrizedMethods<T, E extends Exception, C extends CharSequence> {
    void methodWithParameters(T param1, C param2) {}

    T returningMethod() {
      return null;
    }

    void throwingMethod() throws E {}

    <V extends T> V parametrizedMethod() {
      return null;
    }
  }
}
