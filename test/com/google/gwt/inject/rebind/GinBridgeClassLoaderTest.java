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

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.javac.testing.GeneratorContextBuilder;
import com.google.gwt.dev.javac.testing.JavaSource;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import junit.framework.TestCase;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;


public class GinBridgeClassLoaderTest extends TestCase {

  public void testExceptedPackages() throws ClassNotFoundException, NoSuchMethodException {
    GeneratorContext context =
        GeneratorContextBuilder.newEmptyBuilder().add(createSimple()).buildGeneratorContext();

    GinBridgeClassLoader gwtLoader =
        new GinBridgeClassLoader(context, createLogger(), new HashSet<String>());

    assertClassVersion(gwtLoader, "a", "b", "com.google.gwt.inject.rebind.types.Simple");

    GinBridgeClassLoader javaLoader = new GinBridgeClassLoader(context, createLogger(),
        Collections.singleton("com.google.gwt.inject.rebind.types"));

    assertClassVersion(javaLoader, "b", "a", "com.google.gwt.inject.rebind.types.Simple");
  }

  public void testExceptedPackagesWithDot() throws ClassNotFoundException, NoSuchMethodException {
    GeneratorContext context =
        GeneratorContextBuilder.newEmptyBuilder().add(createSimple()).buildGeneratorContext();

    GinBridgeClassLoader loader = new GinBridgeClassLoader(context, createLogger(),
        Collections.singleton("com.google.gwt.inject.rebind.types."));

    assertClassVersion(loader, "b", "a", "com.google.gwt.inject.rebind.types.Simple");
  }

  public void testJavaCoreClass() throws ClassNotFoundException {
    GeneratorContext context =
        GeneratorContextBuilder.newEmptyBuilder().add(createFakeString()).buildGeneratorContext();

    GinBridgeClassLoader loader =
        new GinBridgeClassLoader(context, createLogger(), new HashSet<String>());

    assertTrue(loader.loadClass("java.lang.String").getMethods().length > 0);
  }

  private JavaSource createFakeString() {
    return new JavaSource("java.lang.String") {
      public String getSource() {
        return "package java.lang;"
             + "public class String {}";
      }
    };
  }

  private void assertClassVersion(GinBridgeClassLoader javaLoader, String missingMethod,
      String expectedMethod, String className) throws ClassNotFoundException,
      NoSuchMethodException {
    Class<?> javaSimple = javaLoader.loadClass(className);
    try {
      javaSimple.getDeclaredMethod(missingMethod);
      fail("Expected NoSuchMethodException.");
    } catch (NoSuchMethodException e) {
      // expected
    }
    assertNotNull(javaSimple.getDeclaredMethod(expectedMethod));
  }

  private JavaSource createSimple() {
    return new JavaSource("com.google.gwt.inject.rebind.types.Simple") {
      public String getSource() {
        return "package com.google.gwt.inject.rebind.types;"
             + "public class Simple {"
             + "  public void b() {}"
             + "}";
      }
    };
  }

  private static TreeLogger createLogger() {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(new PrintWriter(System.err, true));
    logger.setMaxDetail(TreeLogger.ERROR);
    return logger;
  }
}
