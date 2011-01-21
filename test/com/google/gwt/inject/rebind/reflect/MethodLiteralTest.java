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

import com.google.gwt.inject.client.MyOtherAnnotation;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;

import junit.framework.TestCase;

import java.lang.reflect.Method;

public class MethodLiteralTest extends TestCase {

  public void testGetParameterKeys() throws NoSuchMethodException {
    assertEquals(Named.class,
        getMethod("methodWithBindingAnnotation").getParameterKeys().get(0).getAnnotationType());

    assertNull(
        getMethod("methodWithOtherAnnotation").getParameterKeys().get(0).getAnnotationType());

    assertNull(
        getMethod("methodWithoutAnnotation").getParameterKeys().get(0).getAnnotationType());
  }

  public void testToString() throws NoSuchMethodException {
    assertEquals("com.google.gwt.inject.rebind.reflect.MethodLiteralTest#"
        + "methodWithBindingAnnotation(java.lang.String)",
        getMethod("methodWithBindingAnnotation").toString()); 
  }

  private MethodLiteral<MethodLiteralTest, Method> getMethod(String name)
      throws NoSuchMethodException {
    return MethodLiteral.get(MethodLiteralTest.class.getDeclaredMethod(name, String.class),
        TypeLiteral.get(MethodLiteralTest.class));
  }

  public void methodWithBindingAnnotation(@Named("bar") String foo) {}

  public void methodWithOtherAnnotation(@MyOtherAnnotation String foo) {}

  public void methodWithoutAnnotation(String foo) {}
}
