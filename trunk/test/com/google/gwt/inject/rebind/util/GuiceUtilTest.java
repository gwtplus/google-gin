/*
 * Copyright 2009 Google Inc.
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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.MyBindingAnnotation;
import com.google.gwt.inject.client.MyOtherAnnotation;
import com.google.gwt.inject.rebind.binding.Dependency;
import com.google.gwt.inject.rebind.reflect.FieldLiteral;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Unit tests for {@link JType} to {@link Key} translation.
 */
public class GuiceUtilTest extends TestCase {

  public void testGetKey_unAnnotatedMethod() throws NoSuchMethodException {
    Key<?> key = getMethodKey("unAnnotated");
    assertEquals(TypeLiteral.get(String.class), key.getTypeLiteral());
    assertNull(key.getAnnotation());
  }

  public void testGetKey_bindingAnnotatedMethod() throws NoSuchMethodException {
    Key<?> key = getMethodKey("bindingAnnotated");
    assertEquals(TypeLiteral.get(String.class), key.getTypeLiteral());
    assertEquals(MyBindingAnnotation.class, key.getAnnotationType());
  }
  
  public void testGetKey_legallyDoublyAnnotatedMethod() throws NoSuchMethodException {
    Key<?> key = getMethodKey("legallyDoublyAnnotated");
    assertEquals(TypeLiteral.get(String.class), key.getTypeLiteral());
    assertEquals(MyBindingAnnotation.class, key.getAnnotationType());
  }
  
  public void testGetKey_illegallyDoublyAnnotatedMethod() throws NoSuchMethodException {
    try {
      getMethodKey("illegallyDoublyAnnotated");
      fail("Expected ProvisionException.");
    } catch (ProvisionException e) {
      // Expected.
    }
  }
  
  public void testGetKey_memberInjectUnAnnotatedMethod() throws NoSuchMethodException {
    Key<?> key = getMethodKey("memberInjectUnAnnotated", String.class);
    assertEquals(TypeLiteral.get(String.class), key.getTypeLiteral());
    assertNull(key.getAnnotationType());
  }

  public void testGetKey_memberInjectAnnotatedMethod() throws NoSuchMethodException {
    Key<?> key = getMethodKey("memberInjectAnnotated", String.class);
    assertEquals(TypeLiteral.get(String.class), key.getTypeLiteral());
    assertEquals(MyBindingAnnotation.class, key.getAnnotationType());
  }

  private Key<?> getMethodKey(String name, Class... parameters) throws NoSuchMethodException {
    return new GuiceUtil(createInjectableCollector()).getKey(
        (MethodLiteral<?, Method>) getMethod(name, parameters));
  }

  private MethodLiteral<?, Method> getMethod(String name, Class... parameters)
      throws NoSuchMethodException {
    return MethodLiteral.get(TestData.class.getDeclaredMethod(name, parameters),
        TypeLiteral.get(TestData.class));
  }

  private MethodLiteral<?, ?> getNonGinjectorMethod() throws NoSuchMethodException {
    return MethodLiteral.get(GuiceUtilTest.class.getDeclaredMethod("nonGinjectorMethod"),
        TypeLiteral.get(GuiceUtilTest.class));
  }

  public void testGetKey_unAnnotatedField() throws NoSuchFieldException {
    Key<?> key = getFieldKey("unAnnotated");
    assertEquals(TypeLiteral.get(String.class), key.getTypeLiteral());
    assertNull(key.getAnnotation());
  }

  public void testGetKey_bindingAnnotatedField() throws NoSuchFieldException {
    Key<?> key = getFieldKey("bindingAnnotated");
    assertEquals(TypeLiteral.get(String.class), key.getTypeLiteral());
    assertEquals(MyBindingAnnotation.class, key.getAnnotationType());
  }
  
  public void testGetKey_legallyDoublyAnnotatedField() throws NoSuchFieldException {
    Key<?> key = getFieldKey("legallyDoublyAnnotated");
    assertEquals(TypeLiteral.get(String.class), key.getTypeLiteral());
    assertEquals(MyBindingAnnotation.class, key.getAnnotationType());
  }
  
  public void testGetKey_illegallyDoublyAnnotatedField() throws NoSuchFieldException {
    try {
      getFieldKey("illegallyDoublyAnnotated");
      fail("Expected ProvisionException.");
    } catch (ProvisionException e) {
      // Expected.
    }
  }

  private Key<?> getFieldKey(String name) throws NoSuchFieldException {
    return new GuiceUtil(createInjectableCollector()).getKey((FieldLiteral<?>) getField(name));
  }

  private FieldLiteral<?> getField(String name) throws NoSuchFieldException {
    return FieldLiteral.get(TestData.class.getDeclaredField(name), TypeLiteral.get(TestData.class));
  }

  public void testIsMemberInject() throws NoSuchMethodException {
    GuiceUtil guiceUtil = new GuiceUtil(createInjectableCollector());
    assertFalse(guiceUtil.isMemberInject(getMethod("unAnnotated")));
    assertTrue(guiceUtil.isMemberInject(getMethod("memberInjectUnAnnotated", String.class)));
  }

  public void testIsOptional_method() throws NoSuchMethodException {
    GuiceUtil guiceUtil = new GuiceUtil(createInjectableCollector());
    assertFalse(guiceUtil.isOptional(getMethod("nonOptionalInject")));
    assertTrue(guiceUtil.isOptional(getMethod("optionalInject")));
    assertFalse(guiceUtil.isOptional(getMethod("javaxInject")));
  }
  
  public void testIsOptional_field() throws NoSuchFieldException {
    GuiceUtil guiceUtil = new GuiceUtil(createInjectableCollector());
    assertFalse(guiceUtil.isOptional(getField("nonOptionalInject")));
    assertTrue(guiceUtil.isOptional(getField("optionalInject")));
    assertFalse(guiceUtil.isOptional(getField("javaxInject")));
  }
  
  private Set<Key<?>> extractKeys(Collection<Dependency> requiredKeys, boolean optional) {
    Set<Key<?>> keySet = new HashSet<Key<?>>();
    for (Dependency key : requiredKeys) {
      if (key.isOptional() == optional) {
        keySet.add(key.getTarget());
      }
    }
    return keySet;
  }

  public void testGetRequiredKeys_method() throws NoSuchMethodException {
    GuiceUtil guiceUtil = new GuiceUtil(createInjectableCollector());
    Collection<Dependency> keys = guiceUtil.getDependencies(
        Dependency.GINJECTOR, getMethod("nonOptionalKeys", String.class, Foo.class));
    Set<Key<?>> optionalKeys = extractKeys(keys, true);
    Set<Key<?>> requiredKeys = extractKeys(keys, false);
    assertTrue(optionalKeys.isEmpty());
    assertEquals(2, requiredKeys.size());
    assertTrue(requiredKeys.contains(Key.get(String.class)));
    assertTrue(requiredKeys.contains(Key.get(Foo.class)));
  }

  public void testGetRequiredKeys_optionalMethod() throws NoSuchMethodException {
    GuiceUtil guiceUtil = new GuiceUtil(createInjectableCollector());
    Collection<Dependency> keys = guiceUtil.getDependencies(
        Dependency.GINJECTOR, getMethod("optionalKeys", String.class, Foo.class));
    Set<Key<?>> optionalKeys = extractKeys(keys, true);
    Set<Key<?>> requiredKeys = extractKeys(keys, false);
    assertTrue(requiredKeys.isEmpty());
    assertEquals(2, optionalKeys.size());
    assertTrue(optionalKeys.contains(Key.get(String.class)));
    assertTrue(optionalKeys.contains(Key.get(Foo.class)));
  }

  public void testGetRequiredKeys_noKeyMethod() throws NoSuchMethodException {
    GuiceUtil guiceUtil = new GuiceUtil(createInjectableCollector());
    Collection<Dependency> keys =
        guiceUtil.getDependencies(Dependency.GINJECTOR, getMethod("noKeys"));
    assertTrue(keys.isEmpty());
  }

  public void testGetRequiredKeys_annotatedKeysMethod() throws NoSuchMethodException {
    GuiceUtil guiceUtil = new GuiceUtil(createInjectableCollector());
    Collection<Dependency> keys =
        guiceUtil.getDependencies(Dependency.GINJECTOR, getMethod("annotatedKeys", Foo.class));
    Set<Key<?>> optionalKeys = extractKeys(keys, true);
    Set<Key<?>> requiredKeys = extractKeys(keys, false);
    assertTrue(optionalKeys.isEmpty());
    assertEquals(1, requiredKeys.size());
    assertTrue(requiredKeys.contains(Key.get(Foo.class, MyBindingAnnotation.class)));
  }

  public void testGetRequiredKeys_type() throws NoSuchMethodException, NoSuchFieldException {
    MemberCollector memberCollector = createMock(MemberCollector.class);
    Set<MethodLiteral<?, Method>> methods = new HashSet<MethodLiteral<?, Method>>();
    methods.add(getMethod("nonOptionalKeys", String.class, Foo.class));
    methods.add(getMethod("annotatedKeys", Foo.class));
    Set<FieldLiteral<?>> fields = new HashSet<FieldLiteral<?>>();
    fields.add(getField("unAnnotatedKey"));
    fields.add(getField("annotatedKey"));
    fields.add(getField("optionalKey"));
    expect(memberCollector.getFields(TypeLiteral.get(TestData.class))).andReturn(fields);
    expect(memberCollector.getMethods(TypeLiteral.get(TestData.class))).andReturn(methods);
    replay(memberCollector);

    GuiceUtil guiceUtil = new GuiceUtil(memberCollector);
    Collection<Dependency> keys = guiceUtil.getMemberInjectionDependencies(
        Dependency.GINJECTOR, TypeLiteral.get(TestData.class));
    Set<Key<?>> optionalKeys = extractKeys(keys, true);
    Set<Key<?>> requiredKeys = extractKeys(keys, false);

    assertEquals(1, optionalKeys.size());
    assertEquals(5, requiredKeys.size());

    assertTrue(optionalKeys.contains(Key.get(Bar.class)));
    assertTrue(requiredKeys.contains(Key.get(Baz.class)));
    assertTrue(requiredKeys.contains(Key.get(Baz.class, MyBindingAnnotation.class)));
    assertTrue(requiredKeys.contains(Key.get(String.class)));
    assertTrue(requiredKeys.contains(Key.get(Foo.class)));
    assertTrue(requiredKeys.contains(Key.get(Foo.class, MyBindingAnnotation.class)));
  }

  // TODO(schmitt): same collector as in the guice module, centralize.
  private MemberCollector createInjectableCollector() {
    MemberCollector collector = new MemberCollector(TreeLogger.NULL);
    collector.setMethodFilter(
        new MemberCollector.MethodFilter() {
          public boolean accept(MethodLiteral<?, Method> method) {
            // TODO(schmitt): Do injectable methods require at least one parameter?
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

  public static class TestData implements Ginjector {
    String unAnnotated;
    @MyBindingAnnotation String bindingAnnotated;
    @MyBindingAnnotation @MyOtherAnnotation String legallyDoublyAnnotated;
    @MyBindingAnnotation @Named("") String illegallyDoublyAnnotated;

    @Inject String nonOptionalInject;
    @Inject(optional = true) String optionalInject;
    @javax.inject.Inject String javaxInject;

    @Inject Baz unAnnotatedKey;
    @Inject @MyBindingAnnotation Baz annotatedKey;
    @Inject(optional = true) Bar optionalKey;

    String unAnnotated() { return null; }
    @MyBindingAnnotation String  bindingAnnotated() { return null; }
    @MyBindingAnnotation @MyOtherAnnotation String legallyDoublyAnnotated() { return null; }
    @MyBindingAnnotation @Named("") String illegallyDoublyAnnotated() { return null; }

    void memberInjectUnAnnotated(String string) {}
    @MyBindingAnnotation void memberInjectAnnotated(String string) {}

    @Inject void nonOptionalInject() {}
    @Inject(optional = true) void optionalInject() {}
    @javax.inject.Inject void javaxInject() {}

    @Inject void nonOptionalKeys(String string, Foo foo) {}
    @Inject(optional = true) void optionalKeys(String string, Foo foo) {}
    @Inject void noKeys() {}
    @Inject void annotatedKeys(@MyBindingAnnotation Foo foo) {}
  }

  public static class Foo {}

  public static class Bar {}

  public static class Baz {}
}
