/*
 * Copyright 2008 Google Inc.
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
import com.google.gwt.inject.client.MyBindingAnnotation;
import com.google.gwt.inject.client.foo.WildcardFieldClass;
import com.google.gwt.inject.client.foo.SuperInterface;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link JType} to {@link Key} translation.
 */
// Annotate our class just to make it easy to get annotation instances :)
@MyBindingAnnotation
@MyOtherAnnotation
public class KeyUtilTest extends AbstractUtilTester {

  public void testString() throws Exception {
    KeyUtil keyUtil = new KeyUtil();
    Key<?> key = keyUtil.getKey(getClassType(String.class), null);
    assertEquals(key, Key.get(String.class));
  }

  public void testInteger() throws Exception {
    KeyUtil keyUtil = new KeyUtil();
    Key<?> key = keyUtil.getKey(getClassType(Integer.class), null);
    assertEquals(key, Key.get(Integer.class));
  }

  public void testInt() throws Exception {
    KeyUtil keyUtil = new KeyUtil();
    Key<?> key = keyUtil.getKey(getPrimitiveType(int.class), null);
    assertEquals(key, Key.get(int.class));
  }

  public void testListOfInteger() throws Exception {
    KeyUtil keyUtil = new KeyUtil();
    Key<?> key = keyUtil.getKey(getParameterizedType(List.class, Integer.class), null);
    assertEquals(key, Key.get(new TypeLiteral<List<Integer>>() {}));
  }

  public void testArrayOfInteger() throws Exception {
    KeyUtil keyUtil = new KeyUtil();
    Key<?> key = keyUtil.getKey(getArrayType(Integer.class), null);
    assertEquals(key, Key.get(Integer[].class));
  }

  public void testArrayOfInt() throws Exception {
    KeyUtil keyUtil = new KeyUtil();
    Key<?> key = keyUtil.getKey(getArrayType(int.class), null);
    assertEquals(key, Key.get(int[].class));
  }

  public void testStringNamed() throws Exception {
    KeyUtil keyUtil = new KeyUtil();
    Annotation ann = Names.named("brian");
    Key<?> key = keyUtil.getKey(getClassType(String.class), new Annotation[] {ann});
    assertEquals(key, Key.get(String.class, ann));
  }

  public void testStringMyBindingAnnotation() throws Exception {
    KeyUtil keyUtil = new KeyUtil();
    Annotation ann = getClass().getAnnotation(MyBindingAnnotation.class);
    assertNotNull(ann);

    Key<?> key = keyUtil.getKey(getClassType(String.class), new Annotation[] {ann});
    assertEquals(key, Key.get(String.class, ann));
    assertEquals(key, Key.get(String.class, MyBindingAnnotation.class));
  }

  public void testStringNonBindingAnnotation() throws Exception {
    KeyUtil keyUtil = new KeyUtil();
    Annotation ann = getClass().getAnnotation(MyOtherAnnotation.class);
    assertNotNull(ann);

    Key<?> key = keyUtil.getKey(getClassType(String.class), new Annotation[] {ann});
    assertEquals(key, Key.get(String.class));
  }

  public void testStringTwoAnnotations() throws Exception {
    KeyUtil keyUtil = new KeyUtil();

    Annotation bindingAnn = getClass().getAnnotation(MyBindingAnnotation.class);
    Annotation otherAnn = getClass().getAnnotation(MyOtherAnnotation.class);
    assertNotNull(bindingAnn);
    assertNotNull(otherAnn);

    Key<?> key =
        keyUtil.getKey(getClassType(String.class), new Annotation[] {bindingAnn, otherAnn});
    assertEquals(key, Key.get(String.class, bindingAnn));
    assertEquals(key, Key.get(String.class, MyBindingAnnotation.class));

    // Test annotations in the other order too
    key = keyUtil.getKey(getClassType(String.class), new Annotation[] {otherAnn, bindingAnn});
    assertEquals(key, Key.get(String.class, bindingAnn));
    assertEquals(key, Key.get(String.class, MyBindingAnnotation.class));
  }

  public void testTooManyBindingAnnotations() throws Exception {
    KeyUtil keyUtil = new KeyUtil();
    Annotation bindingAnn = getClass().getAnnotation(MyBindingAnnotation.class);
    Annotation namedAnn = Names.named("brian");

    try {
      Key<?> key =
          keyUtil.getKey(getClassType(String.class), new Annotation[] {bindingAnn, namedAnn});
      fail("Expected exception, but got: " + key);
    } catch (ProvisionException e) {
      // good, expected
    }
  }

  public void testWildcardParameterizedType() {
    KeyUtil keyUtil = new KeyUtil();

    // TODO(schmitt):  Check correctness of lower bound and unbound wildcard?

    // Loading actual example instead of creating a wildcard type by hand.
    JClassType classType = getClassType(WildcardFieldClass.class);
    JField field = classType.getField("map");
    Key<?> key = keyUtil.getKey(field);
    assertEquals(key, Key.get(new TypeLiteral<Map<String, ? extends SuperInterface>>() {}));
  }
}
