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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.inject.rebind.binding.BindingIndex;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Key;

public class SourceWriteUtilTest extends AbstractUtilTester {

  private SourceWriteUtil sourceWriteUtil;

  // TODO(schmitt):  Add unit tests for method and field inject generation.

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
    BindingIndex bindingIndex = new BindingIndex() {
      public boolean isBound(Key<?> key) {
        return false;
      }
    };

    NameGenerator nameGenerator = new NameGenerator();
    KeyUtil keyUtil = new KeyUtil(getTypeOracle(), createInjectableCollector());
    sourceWriteUtil = new SourceWriteUtil(keyUtil, nameGenerator, createInjectableCollector(),
        bindingIndex);
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

    public void indentln(String s, Object... args) {
      indentln(String.format(s, args));
    }

    public void outdent() {
      // No indents for unit test.
    }

    public void print(String s) {
      sb.append(s);
    }

    public void print(String s, Object... args) {
      print(String.format(s, args));
    }

    public void println() {
      sb.append("\\n");
    }

    public void println(String s) {
      print(s);
      println();
    }

    public void println(String s, Object... args) {
      println(String.format(s, args));
    }

    public String toString() {
      return sb.toString();
    }
  }
}
