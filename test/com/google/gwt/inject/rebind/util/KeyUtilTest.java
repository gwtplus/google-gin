/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.inject.client.MyBindingAnnotation;
import com.google.gwt.inject.rebind.util.types.MethodsClass;
import com.google.gwt.inject.rebind.util.types.Parameterized;
import com.google.gwt.inject.rebind.util.types.SuperInterface;
import com.google.gwt.inject.rebind.util.types.WildcardFieldClass;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link JType} to {@link Key} translation.
 */
// Annotate our class just to make it easy to get annotation instances :)
@MyBindingAnnotation
@MyOtherAnnotation
public class KeyUtilTest extends AbstractUtilTester {
  private KeyUtil keyUtil;
  private Annotation myBindingAnnotation;
  private Annotation myOtherAnnotation;
  private Annotation namedAnn;

  public void testString() throws Exception {
    checkClass(getClassType(String.class), Key.get(String.class));
  }

  public void testInteger() throws Exception {
    checkClass(getClassType(Integer.class), Key.get(Integer.class));
  }

  public void testInt() throws Exception {
    checkType(getPrimitiveType(int.class), Key.get(int.class));
  }

  public void testListOfInteger() throws Exception {
    checkType(getParameterizedType(List.class, Integer.class),
        Key.get(new TypeLiteral<List<Integer>>() {}));
  }

  public void testArrayOfInteger() throws Exception {
    checkType(getArrayType(Integer.class), Key.get(Integer[].class));
  }

  public void testArrayOfInt() throws Exception {
    checkType(getArrayType(int.class), Key.get(int[].class));
  }

  public void testStringNamed() throws Exception {
    Key<?> key = keyUtil.getKey(getClassType(String.class), namedAnn);
    assertEquals(key, Key.get(String.class, namedAnn));
  }

  public void testStringMyBindingAnnotation() throws Exception {
    Key<?> key = keyUtil.getKey(getClassType(String.class), myBindingAnnotation);
    assertEquals(key, Key.get(String.class, myBindingAnnotation));
    assertEquals(key, Key.get(String.class, MyBindingAnnotation.class));
  }

  public void testStringNonBindingAnnotation() throws Exception {
    Key<?> key = keyUtil.getKey(getClassType(String.class), myOtherAnnotation);
    assertEquals(key, Key.get(String.class));
  }

  public void testStringTwoAnnotations() throws Exception {
    Key<?> key = keyUtil.getKey(getClassType(String.class), myBindingAnnotation, myOtherAnnotation);
    assertEquals(key, Key.get(String.class, myBindingAnnotation));
    assertEquals(key, Key.get(String.class, MyBindingAnnotation.class));

    // Test annotations in the other order too
    key = keyUtil.getKey(getClassType(String.class), myOtherAnnotation, myBindingAnnotation);
    assertEquals(key, Key.get(String.class, myBindingAnnotation));
    assertEquals(key, Key.get(String.class, MyBindingAnnotation.class));
  }

  public void testTooManyBindingAnnotations() throws Exception {
    try {
      Key<?> key = keyUtil.getKey(getClassType(String.class), myBindingAnnotation, namedAnn);
      fail("Expected exception, but got: " + key);
    } catch (ProvisionException e) {
      // good, expected
    }
  }

  public void testWildcardParameterizedType() {
    // TODO(schmitt):  Check correctness of lower bound and unbound wildcard?

    // Loading actual example instead of creating a wildcard type by hand.
    JClassType classType = getClassType(WildcardFieldClass.class);
    JField field = classType.getField("map");
    Key<?> key = keyUtil.getKey(field);
    assertEquals(key, Key.get(new TypeLiteral<Map<String, ? extends SuperInterface>>() {}));
  }

  public void testJavaToGwtMethod() throws NoSuchMethodException, NotFoundException {
    Method foo = MethodsClass.class.getMethod("foo");
    Method foo2 = MethodsClass.class.getMethod("foo", String.class);
    Method bar = MethodsClass.class.getMethod("bar", String.class);
    JMethod gwtFoo = keyUtil.javaToGwtMethod(foo);
    JMethod gwtFoo2 = keyUtil.javaToGwtMethod(foo2);
    JMethod gwtBar = keyUtil.javaToGwtMethod(bar);

    assertEquals("foo", gwtFoo.getName());
    assertEquals(0, gwtFoo.getParameters().length);

    assertEquals("bar", gwtBar.getName());
    assertEquals(1, gwtBar.getParameters().length);
    assertEquals("java.lang.String", gwtBar.getParameters()[0].getType().getQualifiedSourceName());

    assertEquals("foo", gwtFoo2.getName());
    assertEquals(1, gwtFoo2.getParameters().length);
  }

  public void testGetParameterizedClassType() {
    Type type = new TypeLiteral<Parameterized<String>>(){}.getType();
    JClassType classType = keyUtil.getClassType(type);
    assertTrue(classType instanceof JParameterizedType);
    JParameterizedType parameterizedType = (JParameterizedType) classType;
    assertEquals(getClassType(String.class), parameterizedType.getTypeArgs()[0]);
  }

  private void checkClass(JClassType type, Key<?> key) {
    checkType(type, key);
    assertEquals(type, keyUtil.getRawClassType(key));
  }

  private void checkType(JType type, Key<?> key) {
    Key<?> keyForType = keyUtil.getKey(type);
    assertEquals(key, keyForType);
  }

  protected void setUp() throws Exception {
    super.setUp();
    keyUtil = new KeyUtil(getTypeOracle(), new NameGenerator());

    namedAnn = Names.named("brian");
    myBindingAnnotation = getClass().getAnnotation(MyBindingAnnotation.class);
    myOtherAnnotation = getClass().getAnnotation(MyOtherAnnotation.class);
    assertNotNull(myBindingAnnotation);
    assertNotNull(myOtherAnnotation);
  }
}
