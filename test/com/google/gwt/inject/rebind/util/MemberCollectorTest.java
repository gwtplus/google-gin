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
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.inject.rebind.util.types.SubClass;
import com.google.gwt.inject.rebind.util.types.SubInterface;
import com.google.gwt.inject.rebind.util.types.SuperClass;
import com.google.gwt.inject.rebind.util.types.SuperInterface;
import com.google.gwt.inject.rebind.util.types.secret.SecretSubClass;

import java.util.Collection;

public class MemberCollectorTest extends AbstractUtilTester {

  public void testSetFilterAfterAccess() {

    // TODO(schmitt):  Would like to use UnittestTreeLogger, but it requires
    // private access in TreeLogger.
    MemberCollector collector = new MemberCollector(TreeLogger.NULL);

    collector.getMethods(getClassType(SuperClass.class));

    try {
      collector.setMethodFilter(new MemberCollector.MethodFilter() {
        public boolean accept(JMethod method) {
          return false;
        }
      });

      // This only gets executed if no method is thrown.
      fail();
    } catch (IllegalStateException e) {
      // good
    }

    try {
      collector.setFieldFilter(new MemberCollector.FieldFilter() {
        public boolean accept(JField field) {
          return false;
        }
      });

      // This only gets executed if no method is thrown.
      fail();
    } catch (IllegalStateException e) {
      // good
    }
  }

  public void testMethodOverride() {
    MemberCollector collector = createMethodCollector();

    Collection<JMethod> methods = collector.getMethods(getClassType(SubClass.class));

    int numFooA = 0;
    int numFooIB = 0;
    int numFooIC = 0;
    int numFooBSamePackage = 0;
    int numFooBOtherPackage = 0;
    for (JMethod method : methods) {
      if (method.getName().equals("fooA")) {
        assertEquals("SubClass", method.getEnclosingType().getName());
        numFooA++;
      }

      if (method.getName().equals("fooIB")) {
        numFooIB++;
      }

      if (method.getName().equals("fooIC")) {
        numFooIC++;
      }

      if (method.getName().equals("fooB")) {
        assertEquals("SubClass", method.getEnclosingType().getName());
        numFooBSamePackage++;
      }
    }

    methods = collector.getMethods(getClassType(SecretSubClass.class));
    for (JMethod method : methods) {
      if (method.getName().equals("fooB")) {
        numFooBOtherPackage++;
      }
    }

    assertEquals(1, numFooA);
    assertEquals(1, numFooIB);
    assertEquals(2, numFooIC);
    assertEquals(1, numFooBSamePackage);
    assertEquals(2, numFooBOtherPackage);
  }

  public void testInterfaceCollect() {
    MemberCollector collector = createMethodCollector();

    Collection<JMethod> methods = collector.getMethods(getClassType(SubInterface.class));

    assertEquals(4, methods.size());
  }

  public void testClassCollect() {
    MemberCollector collector = createAllCollector();

    JClassType type = getClassType(SubClass.class);
    Collection<JMethod> methods = collector.getMethods(type);
    Collection<JField> fields = collector.getFields(type);

    // Two fields are collected from java.lang.Object, 6 from the test types
    assertEquals(8, fields.size());

    int a = 0;
    int b = 0;

    for (JField field : fields) {
      if (field.getEnclosingType().getName().equals("SuperClass")) {
        a++;
      }

      if (field.getEnclosingType().getName().equals("SubClass")) {
        b++;
      }
    }

    assertEquals(3, a);
    assertEquals(3, b);

    // 5 from Object, others from test classes
    assertEquals(15, methods.size());
  }

  public void testMethodFilter() {
    MemberCollector collector = new MemberCollector(TreeLogger.NULL);

    collector.setMethodFilter(new MemberCollector.MethodFilter() {
      public boolean accept(JMethod method) {
        return method.getParameters().length == 0;
      }
    });

    Collection<JMethod> methods = collector.getMethods(getClassType(SuperInterface.class));

    assertEquals(2, methods.size());

    for (JMethod method : methods) {
      if (method.getName().equals("noCollect")) {
        fail();
      }
    }
  }

  private static MemberCollector createMethodCollector() {
    MemberCollector collector = new MemberCollector(TreeLogger.NULL);

    collector.setMethodFilter(new MemberCollector.MethodFilter() {
      public boolean accept(JMethod method) {
        return true;
      }
    });
    return collector;
  }

  private static MemberCollector createAllCollector() {
    MemberCollector collector = new MemberCollector(TreeLogger.NULL);

    collector.setMethodFilter(new MemberCollector.MethodFilter() {
      public boolean accept(JMethod method) {
        return true;
      }
    });

    collector.setFieldFilter(new MemberCollector.FieldFilter() {
      public boolean accept(JField field) {
        return true;
      }
    });

    return collector;
  }
}
