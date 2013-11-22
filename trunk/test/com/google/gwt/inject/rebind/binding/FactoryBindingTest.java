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

package com.google.gwt.inject.rebind.binding;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.inject.rebind.reflect.FieldLiteral;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.util.GuiceUtil;
import com.google.gwt.inject.rebind.util.MemberCollector;
import com.google.inject.ConfigurationException;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public class FactoryBindingTest extends TestCase {

  private static final Context CONTEXT = Context.forText("dummy");

  public void testTooManyParams() {

    try {
      new FactoryBinding(Collections.<Key<?>, TypeLiteral<?>>emptyMap(),
          Key.get(BrokenBeetleFactory.class), CONTEXT, null, null);
      fail("Expected ConfigurationException.");
    } catch (ConfigurationException e) {
      assertTrue(e.getMessage().contains("no constructors"));
    }
  }

  public void testMismatchingParams() {

    try {
      new FactoryBinding(Collections.<Key<?>, TypeLiteral<?>>emptyMap(),
          Key.get(BrokenGolfFactory.class), CONTEXT, null, null);
      fail("Expected ConfigurationException.");
    } catch (ConfigurationException e) {
      assertTrue(e.getMessage().contains("has @AssistedInject constructors"));
    }
  }

  public void testTwoUnnamedStringAnnotations() {
    try {
      new FactoryBinding(Collections.<Key<?>, TypeLiteral<?>>emptyMap(),
          Key.get(TwoUnnamedStringsFactory.class), CONTEXT, 
          new GuiceUtil(createInjectableCollector()), null);
      fail("Expected ConfigurationException.");
    } catch (ConfigurationException e) {
      assertTrue(e.getMessage().contains("has more than one parameter of type " +
          "java.lang.String annotated with @Assisted(\"\")."));
    }
  }

  public void testDuplicateNamedAssistedAnnotations() {
    try {
      new FactoryBinding(Collections.<Key<?>, TypeLiteral<?>>emptyMap(),
          Key.get(TwoAssistedFooStringsFactory.class), CONTEXT, 
          new GuiceUtil(createInjectableCollector()), null);
      fail("Expected ConfigurationException.");
    } catch (ConfigurationException e) {
      assertTrue(e.getMessage().contains("has more than one parameter of type " +
          "java.lang.String annotated with @Assisted(\"foo\")."));
    }
  }

  public void testWorkingEmptyAssistedAnnotations() {
    new FactoryBinding(Collections.<Key<?>, TypeLiteral<?>>emptyMap(),
        Key.get(UnnamedStringAndIntegerFactory.class), CONTEXT, 
        new GuiceUtil(createInjectableCollector()), null);
    // Just testing that there are no exceptions thrown during configuration.
  }

  public void testWorkingNamedAssistedAnnotations() {
    new FactoryBinding(Collections.<Key<?>, TypeLiteral<?>>emptyMap(),
        Key.get(StringIntegerWithSameAssistedValueFactory.class), CONTEXT, 
        new GuiceUtil(createInjectableCollector()), null);
    // Just testing that there are no exceptions thrown during configuration.
  }

  public void testMismatchedFactoryAndClassAnnotations() {
    try {
      new FactoryBinding(Collections.<Key<?>, TypeLiteral<?>>emptyMap(),
          Key.get(MismatchedFactoryAndClassAssistedValuesFactory.class), CONTEXT, 
          new GuiceUtil(createInjectableCollector()), null);
      fail("Expected ConfigurationException.");
    } catch (ConfigurationException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("has no constructors matching " +
          "the parameters in method"));
    }
  }

  public interface BrokenBeetleFactory {
    Beetle create(int year);
  }

  public static class Beetle {

    @Inject
    public Beetle(@Assisted String name, @Assisted Integer year) {}
  }

  public interface BrokenGolfFactory {
    Golf create(List<Integer> foo, String bar);
  }

  public static class Golf {

    @AssistedInject
    public Golf(@Assisted List<String> foo, @Assisted String bar) {}
  }

  public interface TwoUnnamedStringsFactory {
    TwoUnnamedStrings create(String foo, String bar);
  }

  public static class TwoUnnamedStrings {

    @Inject
    public TwoUnnamedStrings(@Assisted String foo, @Assisted String bar) {}
  }

  public interface TwoAssistedFooStringsFactory {
    TwoAssistedFooStrings create(@Assisted("foo") String foo, @Assisted("foo") String bar);
  }

  public static class TwoAssistedFooStrings {

    @Inject
    public TwoAssistedFooStrings(@Assisted("foo") String foo, @Assisted("foo") String bar) {}
  }

  public interface UnnamedStringAndIntegerFactory {
    UnnamedStringAndInteger create(String foo, Integer bar);
  }

  public static class UnnamedStringAndInteger {

    @Inject
    public UnnamedStringAndInteger(@Assisted String foo, @Assisted Integer bar) {}
  }

  public interface StringIntegerWithSameAssistedValueFactory {
    StringIntegerWithSameAssistedValue create(@Assisted("foo") String foo, 
        @Assisted("foo") Integer bar);
  }

  public static class StringIntegerWithSameAssistedValue {

    @Inject
    public StringIntegerWithSameAssistedValue(@Assisted("foo") String foo, 
        @Assisted("foo") Integer bar) {}
  }

  public interface MismatchedFactoryAndClassAssistedValuesFactory {
    MismatchedFactoryAndClassAssistedValues create(@Assisted("foo") String foo);
  }

  public static class MismatchedFactoryAndClassAssistedValues {

    @Inject
    public MismatchedFactoryAndClassAssistedValues(@Assisted("bar") String foo) {}
  }
  
  // Lifted from GuiceUtilTest
  private MemberCollector createInjectableCollector() {
    MemberCollector collector = new MemberCollector(TreeLogger.NULL);
    collector.setMethodFilter(
        new MemberCollector.MethodFilter() {
          public boolean accept(MethodLiteral<?, Method> method) {
            return method.isAnnotationPresent(Inject.class) && !method.isStatic();
          }
        });

    collector.setFieldFilter(
        new MemberCollector.FieldFilter() {
          public boolean accept(FieldLiteral<?> field) {
            return field.isAnnotationPresent(Inject.class) && !field.isStatic();
          }
        });
    return collector;
  }
}
