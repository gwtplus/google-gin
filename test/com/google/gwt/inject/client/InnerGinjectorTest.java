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
package com.google.gwt.inject.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.binder.GinBinder;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

import java.util.Arrays;
import java.util.List;

/**
 * Tests involving inner classes and interfaces.
 */
public class InnerGinjectorTest extends GWTTestCase {
  public void testInnerClassInjection() {
    InnerGinjector injector = GWT.create(InnerGinjector.class);
    InnerType innerType = injector.getInnerType();
    assertTrue(innerType.injected);
  }

  public void testNestedProviderInjection() {
    InnerGinjector injector = GWT.create(InnerGinjector.class);
    MyType myType = injector.getMyType();
    assertNotNull(myType);
  }

  public void testTypeLiteral() {
    InnerGinjector injector = GWT.create(InnerGinjector.class);
    List<String> list = injector.getList();
    assertNotNull(list);
  }

  public static class InnerType {
    boolean injected;

    @Inject
    InnerType(Dependency d) {
      assertNotNull(d);
      injected = true;
    }
  }

  @GinModules(InnerModule.class)
  public interface InnerGinjector extends Ginjector {
    @MyBindingAnnotation
    InnerType getInnerType();

    MyType getMyType();

    List<String> getList();
  }

  static class InnerModule implements GinModule {
    public void configure(GinBinder binder) {
      binder.bind(new TypeLiteral<List<String>>() {}).toProvider(ListOfStringProvider.class);
      binder.bind(InnerType.class).annotatedWith(MyBindingAnnotation.class).to(InnerType.class);
      binder.bind(MyType.class).toProvider(MyProvider.class);
    }
  }

  public static class Dependency {
  }

  public interface MyType {
  }

  public static class MyTypeImpl implements MyType {
  }

  public static class MyProvider implements Provider<InnerGinjectorTest.MyType> {
    public MyType get() {
      return new MyTypeImpl();
    }
  }

  static class ListOfStringProvider implements Provider<List<String>> {
    public List<String> get() {
      return Arrays.asList("blah");
    }
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}
