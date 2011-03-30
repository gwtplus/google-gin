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

import junit.framework.TestCase;

public class GinjectorGeneratorTest extends TestCase {

  static boolean initializedA = false;
  static boolean initializedB = false;

  private GinjectorGenerator ginjectorGenerator;

  protected void setUp() throws Exception {
    super.setUp();
    ginjectorGenerator = new GinjectorGenerator();
    ginjectorGenerator.classLoader = this.getClass().getClassLoader();
  }

  public void testLoadClass() throws ClassNotFoundException {
    assertEquals(GinjectorGeneratorTest.class, ginjectorGenerator.loadClass(
        "com.google.gwt.inject.rebind.GinjectorGeneratorTest", true));
  }

  public void testLoadClass_nestedClass() throws ClassNotFoundException {
    assertEquals(Nested.class, ginjectorGenerator.loadClass(
            "com.google.gwt.inject.rebind.GinjectorGeneratorTest.Nested" , true));

    assertEquals(Nested.DoublyNested.class, ginjectorGenerator.loadClass(
            "com.google.gwt.inject.rebind.GinjectorGeneratorTest.Nested.DoublyNested", true));
  }

  public void testLoadClass_initialize() throws ClassNotFoundException {
    assertFalse(initializedA);
    ginjectorGenerator.loadClass("com.google.gwt.inject.rebind.InitializableA", true);
    assertTrue(initializedA);

    assertFalse(initializedB);
    ginjectorGenerator.loadClass("com.google.gwt.inject.rebind.InitializableB", false);
    assertFalse(initializedB);
  }

  static class Nested {
    static class DoublyNested {}
  }
}
