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

import com.google.gwt.inject.client.MyBindingAnnotation;
import com.google.gwt.inject.client.MyOtherAnnotation;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;

import junit.framework.TestCase;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;


public class MemberLiteralTest extends TestCase {

  public void testGetBindingAnnotation() throws NoSuchMethodException {
    MemberLiteral<?, ?> fakeLiteral = getMemberLiteral();

    Annotation[] noAnnotations = new Annotation[0];
    assertNull(fakeLiteral.getBindingAnnotation(noAnnotations));

    Annotation[] normalAnnotations =
        new Annotation[] { getAnnotation(MyBindingAnnotation.class) };
    assertEquals(getAnnotation(MyBindingAnnotation.class),
        fakeLiteral.getBindingAnnotation(normalAnnotations));

    Annotation[] twoAnnotations = new Annotation[] {
        getAnnotation(MyBindingAnnotation.class), getAnnotation(MyOtherAnnotation.class) };
    assertEquals(getAnnotation(MyBindingAnnotation.class),
        fakeLiteral.getBindingAnnotation(twoAnnotations));

    try {
      fakeLiteral.getBindingAnnotation(new Annotation[] {
          getAnnotation(MyBindingAnnotation.class), getAnnotation(Named.class) });
      fail("Expected ProvisionException.");
    } catch (ProvisionException e) {
      // Expected.
    }

    Annotation[] javaxAnnotation = new Annotation[] { getAnnotation(javax.inject.Named.class) };
    assertEquals(getAnnotation(javax.inject.Named.class),
        fakeLiteral.getBindingAnnotation(javaxAnnotation));
  }

  private Annotation getAnnotation(Class<? extends Annotation> annotationClass)
      throws NoSuchMethodException {
    Method method = MemberLiteralTest.class.getDeclaredMethod("getMemberLiteral");
    return method.getAnnotation(annotationClass);
  }

  // Convenience declaration for easier access to annotation instances.
  @MyBindingAnnotation
  @MyOtherAnnotation
  @Named("foo")
  @javax.inject.Named
  private MethodLiteral<MemberLiteralTest, Method> getMemberLiteral() throws NoSuchMethodException {
    return MethodLiteral.get(MemberLiteralTest.class.getDeclaredMethod("getMemberLiteral"),
        TypeLiteral.get(MemberLiteralTest.class));
  }
}
