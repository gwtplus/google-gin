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

import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Key;

public class SourceWriteUtilTest extends AbstractUtilTester {

  private NameGenerator nameGenerator;
  private SourceWriteUtil sourceWriteUtil;

  public void testAppendInvokeZeroParam() throws NotFoundException {
    JMethod method = getClassType(String.class).getMethod("toString", new JType[0]);

    StringBuilder sb = new StringBuilder();
    sourceWriteUtil.appendInvoke(sb, method);
    assertEquals("toString();", sb.toString());
  }

  public void testAppendInvokeOneParam() throws NotFoundException {
    JMethod concatMethod =
        getClassType(String.class).getMethod("concat", new JType[] {getClassType(String.class)});

    String methodInvocation =
        "concat(" + nameGenerator.getGetterMethodName(Key.get(String.class)) + "());";

    StringBuilder sb = new StringBuilder();
    sourceWriteUtil.appendInvoke(sb, concatMethod);
    assertEquals(methodInvocation, sb.toString());
  }

  public void testAppendInvokeTwoParams() throws NotFoundException {
    JMethod concatMethod =
        getClassType(String.class).getMethod("substring",
            new JType[] {getPrimitiveType(int.class), getPrimitiveType(int.class)});

    String methodInvocation =
        "substring(" + nameGenerator.getGetterMethodName(Key.get(int.class)) + "(), "
            + nameGenerator.getGetterMethodName(Key.get(int.class)) + "());";

    StringBuilder sb = new StringBuilder();
    sourceWriteUtil.appendInvoke(sb, concatMethod);
    assertEquals(methodInvocation, sb.toString());
  }

  public void testWriteMethod() {
    SourceWriter writer = new UnitTestSourceWriter();

    String signature = "public void foo()";
    String body = "int bar;\\nString baz = \"la\";";

    sourceWriteUtil.writeMethod(writer, signature, body);

    assertEquals(signature + " {\\n" + body + "\\n}\\n\\n", writer.toString());
  }

  public void testWriteNativeMethod() {
    SourceWriter writer = new UnitTestSourceWriter();

    String signature = "public native void foo()";
    String body = "int bar;\\nString baz = \"la\";";

    sourceWriteUtil.writeNativeMethod(writer, signature, body);

    assertEquals(signature + " /*-{\\n" + body + "\\n}-*/;\\n\\n", writer.toString());
  }

  protected void setUp() throws Exception {
    super.setUp();
    nameGenerator = new NameGenerator();
    KeyUtil keyUtil = new KeyUtil(getTypeOracle(), nameGenerator);
    sourceWriteUtil = new SourceWriteUtil(keyUtil, nameGenerator);
  }

  private static class UnitTestSourceWriter implements SourceWriter {

    private StringBuilder sb = new StringBuilder();

    public void beginJavaDocComment() {
      throw new UnsupportedOperationException();
    }

    public void commit(TreeLogger logger) {
      throw new UnsupportedOperationException();
    }

    public void endJavaDocComment() {
      throw new UnsupportedOperationException();
    }

    public void indent() {
      // No indents for unit test.
    }

    public void indentln(String s) {
      // No indents for unit test.
      println(s);
    }

    public void outdent() {
      // No indents for unit test.
    }

    public void print(String s) {
      sb.append(s);
    }

    public void println() {
      sb.append("\\n");
    }

    public void println(String s) {
      print(s);
      println();
    }

    public String toString() {
      return sb.toString();
    }
  }
}
