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
import com.google.gwt.inject.rebind.reflect.FieldLiteral;
import com.google.gwt.inject.rebind.reflect.MemberLiteral;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.util.types.SubClass;
import com.google.gwt.inject.rebind.util.types.SubInterface;
import com.google.gwt.inject.rebind.util.types.SuperClass;
import com.google.gwt.inject.rebind.util.types.SuperInterface;
import com.google.gwt.inject.rebind.util.types.secret.SecretSubClass;
import com.google.inject.TypeLiteral;

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.util.Collection;

public class MemberCollectorTest extends TestCase {

  public void testSetFilterAfterAccess() {

    // TODO(schmitt):  Would like to use UnittestTreeLogger, but it requires
    // private access in TreeLogger.
    MemberCollector collector = new MemberCollector(TreeLogger.NULL);

    collector.getMethods(TypeLiteral.get(SuperClass.class));

    try {
      collector.setMethodFilter(new MemberCollector.MethodFilter() {
        public boolean accept(MethodLiteral<?, Method> method) {
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
        public boolean accept(FieldLiteral<?> field) {
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

    Collection<MethodLiteral<?, Method>> methods =
        collector.getMethods(TypeLiteral.get(SubClass.class));

    int numFooA = 0;
    int numFooIB = 0;
    int numFooIC = 0;
    int numFooBSamePackage = 0;
    int numFooBOtherPackage = 0;
    for (MethodLiteral<?, Method> method : methods) {
      if (method.getName().equals("fooA")) {
        assertEquals("SubClass", method.getRawDeclaringType().getSimpleName());
        numFooA++;
      }

      if (method.getName().equals("fooIB")) {
        numFooIB++;
      }

      if (method.getName().equals("fooIC")) {
        numFooIC++;
      }

      if (method.getName().equals("fooB")) {
        assertEquals("SubClass", method.getRawDeclaringType().getSimpleName());
        numFooBSamePackage++;
      }
    }

    methods = collector.getMethods(TypeLiteral.get(SecretSubClass.class));
    for (MethodLiteral<?, Method> method : methods) {
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

    Collection<MethodLiteral<?, Method>> methods =
        collector.getMethods(TypeLiteral.get(SubInterface.class));

    assertEquals(4, methods.size());
  }

  public void testClassCollect() {
    MemberCollector collector = createAllCollector();

    TypeLiteral<SubClass> type = TypeLiteral.get(SubClass.class);
    Collection<MethodLiteral<?, Method>> methods = collector.getMethods(type);
    Collection<FieldLiteral<?>> fields = collector.getFields(type);

    assertEquals(6, fields.size());

    int a = 0;
    int b = 0;

    for (FieldLiteral<?> field : fields) {
      if (field.getRawDeclaringType().getSimpleName().equals("SuperClass")) {
        a++;
      }

      if (field.getRawDeclaringType().getSimpleName().equals("SubClass")) {
        b++;
      }
    }

    assertEquals(3, a);
    assertEquals(3, b);

    assertEquals(10, methods.size());
  }

  public void testMethodFilter() {
    MemberCollector collector = new MemberCollector(TreeLogger.NULL);

    collector.setMethodFilter(new MemberCollector.MethodFilter() {
      public boolean accept(MethodLiteral<?, Method> method) {
        return isObject(method) && method.getParameterTypes().size() == 0;
      }
    });

    Collection<MethodLiteral<?, Method>> methods =
        collector.getMethods(TypeLiteral.get(SuperInterface.class));

    assertEquals(2, methods.size());

    for (MethodLiteral<?, Method> method : methods) {
      if (method.getName().equals("noCollect")) {
        fail();
      }
    }
  }

  // Collect everything but "java.lang.Object" members (they can throw our
  // counts off and should not matter for Guice injection in production code).
  private static boolean isObject(MemberLiteral<?, ?> member) {
    return !member.getRawDeclaringType().getSimpleName().equals("Object");
  }

  private static MemberCollector createMethodCollector() {
    MemberCollector collector = new MemberCollector(TreeLogger.NULL);

    collector.setMethodFilter(new MemberCollector.MethodFilter() {
      public boolean accept(MethodLiteral<?, Method> method) {
        return isObject(method);
      }
    });
    return collector;
  }

  private static MemberCollector createAllCollector() {
    MemberCollector collector = new MemberCollector(TreeLogger.NULL);

    collector.setMethodFilter(new MemberCollector.MethodFilter() {
      public boolean accept(MethodLiteral<?, Method> method) {
        return isObject(method);
      }
    });

    collector.setFieldFilter(new MemberCollector.FieldFilter() {
      public boolean accept(FieldLiteral<?> field) {
        return isObject(field);
      }
    });

    return collector;
  }
}
