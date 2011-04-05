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
package com.google.gwt.inject.client.generics;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.generics.GenericsGinModule.InjectedWithGenerics;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Some tests for bindings using generics.
 */
public class GenericsTest extends GWTTestCase {

  public void testKeyAndProviderGenerics() {
    GenericsGinjector ginjector = GWT.create(GenericsGinjector.class);

    // Shows we could bind from generic key to another generic key
    List<Integer> list = ginjector.getListOfInteger();
    assertNotNull(list);

    // And that we can use a provider method to return generics
    LinkedList<Integer> linkedList = ginjector.getLinkedListOfInteger();
    assertSame(list, linkedList);

    assertEquals(2, list.size());

    // Need to use Integer.valueOf to force boxing to resolve overload
    assertEquals(Integer.valueOf(10), list.get(0));
    assertEquals(Integer.valueOf(20), list.get(1));
  }

  public void testTypeLiteralBinding() {
    GenericsGinjector ginjector = GWT.create(GenericsGinjector.class);

    List<String> list = ginjector.getListOfString();
    assertNotNull(list);
    assertTrue(list instanceof ArrayList);
  }

  // Testing http://code.google.com/p/google-gin/issues/detail?id=54.
  public void testInheritedTypeParameters() {
    GenericsGinjector ginjector = GWT.create(GenericsGinjector.class);

    Parameterized<String> parameterized = ginjector.getParameterized();
    assertEquals("foo", parameterized.getFoo());
  }

  public void testInheritedTypeParametersInner() {
    GenericsGinjector ginjector = GWT.create(GenericsGinjector.class);
    Parameterized.InnerParameterized<Integer> parameterized = ginjector.getInnerParameterized();
    assertEquals(3, (int) parameterized.getValue());
  }

  public void testComplicateParameterized() {
    GenericsGinjector ginjector = GWT.create(GenericsGinjector.class);
    Parameterized.ComplicatedParameterized<String, Parameterized.StringComparator,
        Map<String, Parameterized.StringComparator>> complicatedParameterized =
            ginjector.getComplicatedParameterized();

    assertSame(ginjector.getStringComparatorMap(), complicatedParameterized.getValue());
  }

  public void testSuperWildcards() {
    GenericsGinjector ginjector = GWT.create(GenericsGinjector.class);

    List<? super String> list = ginjector.getSuperStringList();
    assertTrue(list.contains("Hello"));
  }

  public void testNestedWildcard() {
    GenericsGinjector ginjector = GWT.create(GenericsGinjector.class);
    ginjector.getWildcardList();
  }

  // Testing http://code.google.com/p/google-gin/issues/detail?id=136
  public void testJsniGenerics() {
    GenericsGinjector ginjector = GWT.create(GenericsGinjector.class);
    ginjector.getInjectedWithGeneric();
  }

  public void testWildcardParametrized() {
    WildcardGinjector ginjector = GWT.create(WildcardGinjector.class);
    ginjector.getBusinessObject();
  }

  // This is illegal in Guice (as it should be) but for that reason difficult to test here.
  public void testWildcardParametrized_injectType() {
//    FancyWildcardGinjector ginjector = GWT.create(FancyWildcardGinjector.class);
//    ginjector.getBusinessObject();
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }

  static class ValueObject<T> {

    private T value;

    public T getValue() {
      return value;
    }

    public void setValue(T value) {
      this.value = value;
    }
  }

  static class InjectedValueObject<T> extends ValueObject<T> {

    @Override
    @Inject
    public void setValue(T value) {
      super.setValue(value);
    }
  }

  static class BusinessObject {

    final ValueObject<?> value;

    @Inject
    public BusinessObject(ValueObject<?> value) {
      this.value = value;
    }
  }

  interface WildcardGinjector extends Ginjector {
    BusinessObject getBusinessObject();
  }

  @GinModules(FancyWildcardModule.class)
  interface FancyWildcardGinjector extends WildcardGinjector {}

  static class FancyWildcardModule extends AbstractGinModule {

    @Override
    protected void configure() {
      bind(ValueObject.class).to(InjectedValueObject.class);
    }
  }
}
