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

package com.google.gwt.inject.rebind;

import com.google.gwt.inject.rebind.reflect.ReflectUtilTest;
import junit.framework.TestCase;

public class GinjectorGeneratorTest extends TestCase {

  static boolean initializedA = false;
  static boolean initializedB = false;

  public void testLoadClass() throws ClassNotFoundException {
    assertEquals(GinjectorGeneratorTest.class,
        GinjectorGenerator.loadClass("com.google.gwt.inject.rebind.GinjectorGeneratorTest",
            ReflectUtilTest.class.getClassLoader(), true));
  }

  public void testLoadClass_nestedClass() throws ClassNotFoundException {
    assertEquals(Nested.class,
        GinjectorGenerator.loadClass("com.google.gwt.inject.rebind.GinjectorGeneratorTest.Nested",
            GinjectorGeneratorTest.class.getClassLoader(), true));

    assertEquals(Nested.DoublyNested.class,
        GinjectorGenerator.loadClass(
            "com.google.gwt.inject.rebind.GinjectorGeneratorTest.Nested.DoublyNested",
            GinjectorGeneratorTest.class.getClassLoader(), true));
  }


  public void testLoadClass_initialize() throws ClassNotFoundException {
    assertFalse(initializedA);
    GinjectorGenerator.loadClass("com.google.gwt.inject.rebind.InitializableA",
        GinjectorGeneratorTest.class.getClassLoader(), true);
    assertTrue(initializedA);

    assertFalse(initializedB);
    GinjectorGenerator.loadClass("com.google.gwt.inject.rebind.InitializableB",
        GinjectorGeneratorTest.class.getClassLoader(), false);
    assertFalse(initializedB);
  }

  static class Nested {
    static class DoublyNested {}
  }
}
