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
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.inject.Inject;
import com.google.inject.Provider;

import java.util.List;

/**
 * Integrated tests for GIN.
 */
public class InjectTest extends GWTTestCase {
  public void testSimpleInjector() {
    SimpleGinjector injector = GWT.create(SimpleGinjector.class);

    SimpleObject simple = injector.getSimple();
    assertNotNull(simple);

    // Since we have not set scope, we should get a new instance each time
    assertNotSame(simple, injector.getSimple());
  }

  public void testMyAppInjector() {
    MyAppGinjector injector = GWT.create(MyAppGinjector.class);

    SimpleObject simple = injector.getSimple();
    assertNotNull(simple);

    // Ensure we get the same instance each time
    assertSame(simple, injector.getSimple());

    MyApp app = injector.getMyApp();
    assertNotNull(app);
    assertSame(simple, app.getSimple());

    assertNotNull(app.getMsgs());
    assertEquals(MyMessages.FUN_MSG, app.getMsgs().getFunMessage());

    MyService service = injector.getMyService();
    assertTrue(service instanceof MyServiceImpl);
    assertSame(service, app.getService());

    // Even the separately bound MyServiceImpl is the same instance
    assertSame(service, injector.getMyServiceImpl());

    assertSame(MyProvidedObject.getInstance(), injector.getSingleton());
  }

  public void testBindingAnnotations() {
    MyAppGinjector injector = GWT.create(MyAppGinjector.class);

    SimpleObject simple = injector.getSimple();
    assertNotNull(simple);

    // Ensure we get the same instance each time
    assertSame(simple, injector.getSimple());

    // Ensure simple is not the same as blue
    SimpleObject blue = injector.getSimpleBlue();
    assertNotNull(blue);
    assertNotSame(simple, blue);
    assertSame(blue, injector.getSimpleBlue());

    // Ensure simple, red and blue are all different
    SimpleObject red = injector.getSimpleRed();
    assertNotNull(red);
    assertNotSame(simple, red);
    assertNotSame(red, blue);
    assertSame(red, injector.getSimpleRed());
  }

  public void testEagerSingleton() {
    EagerObject.constructorCalls = 0;
    MyAppGinjector injector = GWT.create(MyAppGinjector.class);

    // Constructor should have been called before we even ask for instance
    assertEquals(1, EagerObject.constructorCalls);

    EagerObject eagerObject = injector.getEagerObject();
    assertNotNull(eagerObject);
    assertSame(eagerObject, injector.getEagerObject());

    // Still no more constructor calls
    assertEquals(1, EagerObject.constructorCalls);
  }

  public void testHierarchicalInjector() {
    HierarchicalMyAppGinjector injector = GWT.create(HierarchicalMyAppGinjector.class);

    SimpleObject simple = injector.getSimple();
    assertNotNull(simple);

    SimpleObject unnamedSimple = injector.getUnnamedSimple();
    assertNotNull(unnamedSimple);
    assertSame(simple, unnamedSimple);

    SimpleObject purple = injector.getSimplePurple();
    assertNotNull(purple);
    assertNotSame(simple, purple);

    SimpleObject red = injector.getSimpleRed();
    assertNotNull(red);
    assertNotSame(purple, red);

    SimpleObject blue = injector.getSimpleBlue();
    assertNotNull(blue);
    assertSame(blue, red);
  }

  public void testSyntheticProvider() {
    MyAppGinjector injector = GWT.create(MyAppGinjector.class);

    // Provider with no binding annotation
    Provider<SimpleObject> simpleProvider = injector.getSimpleProvider();
    assertNotNull(simpleProvider);

    // Make sure provider works and gives us the same instance as direct request
    SimpleObject simple = simpleProvider.get();
    assertNotNull(simple);
    assertSame(simple, injector.getSimple());

    // Provider with blue annotation
    Provider<SimpleObject> blueProvider = injector.getSimpleBlueProvider();
    assertNotNull(blueProvider);

    // Make sure provider works and gives us the same instance as direct request
    SimpleObject blue = blueProvider.get();
    assertNotNull(blue);
    assertNotSame(simple, injector.getSimpleBlue());
    assertSame(blue, injector.getSimpleBlue());
  }

  public void testAnnotatedConstants() {
    MyAppGinjector injector = GWT.create(MyAppGinjector.class);
    assertEquals(MyAppGinjector.ANNOTATED_STRING_VALUE, injector.getAnnotatedString());
  }

  public void testRemoteService() {
    MyAppGinjector injector = GWT.create(MyAppGinjector.class);

    // A remote service proxy
    MyRemoteServiceAsync service = injector.getMyRemoteServiceAsync();

    // Make sure that the returned object is a service proxy generated by GWT
    Object gwtCreatedService = GWT.create(MyRemoteService.class);
    assertEquals(gwtCreatedService.getClass(), service.getClass());

    // Make sure that the @RemoteServiceRelativePath annotation worked
    assertEquals(GWT.getModuleBaseURL() + "myRemoteService",
        ((ServiceDefTarget) service).getServiceEntryPoint());

    // Make sure that the @Singleton annotation worked
    assertSame(service, injector.getMyRemoteServiceAsync());
  }

  public void testInnerClassInjection() {
    InnerGinjector injector = GWT.create(InnerGinjector.class);
    InnerType innerType = injector.getG();
    assertTrue(innerType.injected);
  }

  public void testNestedProviderInjection() {
    InnerGinjector injector = GWT.create(InnerGinjector.class);
    MyType myType = injector.getMyType();
    assertTrue(myType != null);
  }

  public void testTypeLiteral() {
    InnerGinjector injector = GWT.create(InnerGinjector.class);
    List<String> list = injector.getList();
    assertTrue(list != null);
  }
  
  public static class InnerType {
    boolean injected;

    @Inject
    InnerType(Dependency d) {
      if (d != null) {
        injected = true;
      }
    }
  }

  @GinModules(MyAppGinModule.MyModule.class)
  public interface InnerGinjector extends Ginjector {
    @MyBindingAnnotation
    InnerType getG();

    MyType getMyType();

    List<String> getList();
  }

  public static class Dependency {
  }

  public interface MyType {
  }

  public static class MyTypeImpl implements MyType {
  }

  public static class MyProvider implements Provider<InjectTest.MyType> {
    public InjectTest.MyType get() {
      return new InjectTest.MyTypeImpl();
    }
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTestModule";
  }
}
