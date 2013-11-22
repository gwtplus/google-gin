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

import com.google.gwt.inject.client.MyBindingAnnotation;
import com.google.gwt.inject.client.nested.Outer;
import com.google.gwt.inject.rebind.binding.Dependency;
import com.google.inject.BindingAnnotation;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import junit.framework.TestCase;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

public class PrettyPrinterTest extends TestCase {

  // Definitions for the dependency tests:
  private static final Key<?> KEY0 = Dependency.GINJECTOR;
  private static final Key<?> KEY1 = Key.get(Outer.class);
  private static final Key<?> KEY2 = Key.get(Outer.Inner.class);
  private static final Key<?> KEY3 = Key.get(Outer.class, MyBindingAnnotation.class);
  private static final Key<?> KEY4 = Key.get(new TypeLiteral<List<Outer>>() {});

  private static final String KEY_STRING1 = "com.google.gwt.inject.client.nested.Outer";
  private static final String KEY_STRING2 = "com.google.gwt.inject.client.nested.Outer$Inner";
  private static final String KEY_STRING3 = "@com.google.gwt.inject.client.MyBindingAnnotation"
      + " com.google.gwt.inject.client.nested.Outer";
  private static final String KEY_STRING4 =
      "java.util.List<com.google.gwt.inject.client.nested.Outer>";

  private static final String CONTEXT01 = "context01";
  private static final String CONTEXT12 = "context12";
  private static final String CONTEXT23 = "context23";
  private static final String CONTEXT34 = "context34";

  private static final Dependency DEP01 = new Dependency(KEY0, KEY1, CONTEXT01);
  private static final Dependency DEP12 = new Dependency(KEY1, KEY2, CONTEXT12);
  private static final Dependency DEP23 = new Dependency(KEY2, KEY3, CONTEXT23);
  private static final Dependency DEP34 = new Dependency(KEY3, KEY4, CONTEXT34);

  public void testPrettyPrintClass_toplevel() {
    assertEquals("X: com.google.gwt.inject.client.nested.Outer",
        PrettyPrinter.format("X: %s", Outer.class));
  }

  public void testPrettyPrintClass_inner() {
    assertEquals("X: com.google.gwt.inject.client.nested.Outer$Inner",
        PrettyPrinter.format("X: %s", Outer.Inner.class));
  }

  public void testPrettyPrintKey_notAnnotated_outer() {
    assertEquals("X: com.google.gwt.inject.client.nested.Outer",
        PrettyPrinter.format("X: %s", Key.get(Outer.class)));
  }

  public void testPrettyPrinterKey_annotated_outer() {
    assertEquals(
        "X: @com.google.gwt.inject.client.MyBindingAnnotation"
            + " com.google.gwt.inject.client.nested.Outer",
        PrettyPrinter.format("X: %s", Key.get(Outer.class, MyBindingAnnotation.class)));
  }

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  public @interface MyComplexBindingAnnotation {
    int field1();
    int field2();
  }

  @MyComplexBindingAnnotation(field1=1, field2=2)
  private int dummyField;

  public void testPrettyPrinterKey_instanceAnnotated_outer() throws Exception {
    Annotation annotation = getClass().getDeclaredField("dummyField").getAnnotations()[0];
    assertEquals(
        "X: @com.google.gwt.inject.rebind.util.PrettyPrinterTest$MyComplexBindingAnnotation"
            + "(field1=1, field2=2) com.google.gwt.inject.client.nested.Outer",
            PrettyPrinter.format("X: %s", Key.get(Outer.class, annotation)));
  }

  public void testPrettyPrintKey_notAnnotated_inner() {
    assertEquals("X: com.google.gwt.inject.client.nested.Outer$Inner",
        PrettyPrinter.format("X: %s", Key.get(Outer.Inner.class)));
  }

  public void testPrettyPrintKey_annotated_inner() {
    assertEquals(
        "X: @com.google.gwt.inject.client.MyBindingAnnotation"
            + " com.google.gwt.inject.client.nested.Outer$Inner",
        PrettyPrinter.format("X: %s", Key.get(Outer.Inner.class, MyBindingAnnotation.class)));
  }

  public void testPrettyPrintKey_generic() {
    assertEquals(
        "Y: java.util.List<com.google.gwt.inject.client.nested.Outer$Inner>",
        PrettyPrinter.format("Y: %s", Key.get(new TypeLiteral<List<Outer.Inner>>() {})));
  }

  public void testPrettyPrintKey_genericWithWildcards() {
    assertEquals(
        "Y: java.util.List<? extends java.util.List<"
            + "? super com.google.gwt.inject.client.nested.Outer$Inner>>",
        PrettyPrinter.format(
            "Y: %s",
            Key.get(new TypeLiteral<List<? extends List<? super Outer.Inner>>>() {})));
  }

  public void testPrettyPrintDependencies_notAPath() {
    try {
      PrettyPrinter.format("%s", dependencyList(DEP01, DEP12, DEP34));
      fail("No error when pretty-printing a non-path.");
    } catch (IllegalArgumentException expected) {
    }
  }

  // No test case of what happens if the GINJECTOR shows up in the middle,
  // because since it isn't allowed in the target field, that can only happen in
  // non-paths (tested above).

  public void testPrettyPrintDependencies_empty() {
    assertEquals("", PrettyPrinter.format("%s", dependencyList()));
  }

  public void testPrettyPrintDependencies_singleton_notFromGinjector() {
    assertEquals(
        "\n" +
            KEY_STRING1 + "\n"
            + " -> " + KEY_STRING2 + " [" + CONTEXT12 + "]\n",
        PrettyPrinter.format("%s", dependencyList(DEP12)));
  }

  public void testPrettyPrintDependencies_singleton_fromGinjector() {
    assertEquals(
        "\n" +
            KEY_STRING1 + " [" + CONTEXT01 + "]\n",
        PrettyPrinter.format("%s", dependencyList(DEP01)));
  }

  public void testPrettyPrintDependencies_several_notFromGinjector() {
    assertEquals(
        "\n" +
            KEY_STRING1 + "\n"
            + " -> " + KEY_STRING2 + " [" + CONTEXT12 + "]\n"
            + " -> " + KEY_STRING3 + " [" + CONTEXT23 + "]\n"
            + " -> " + KEY_STRING4 + " [" + CONTEXT34 + "]\n",
        PrettyPrinter.format("%s", dependencyList(DEP12, DEP23, DEP34)));
  }

  public void testPrettyPrintDependencies_several_fromGinjector() {
    assertEquals(
        "\n" +
            KEY_STRING1 + " [" + CONTEXT01 + "]\n"
            + " -> " + KEY_STRING2 + " [" + CONTEXT12 + "]\n"
            + " -> " + KEY_STRING3 + " [" + CONTEXT23 + "]\n"
            + " -> " + KEY_STRING4 + " [" + CONTEXT34 + "]\n",
        PrettyPrinter.format("%s", dependencyList(DEP01, DEP12, DEP23, DEP34)));
  }

  private static List<Dependency> dependencyList(Dependency... dependencies) {
    return Arrays.asList(dependencies);
  }
}
