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
import com.google.inject.Provider;

/**
 * @author bstoler@google.com (Brian Stoler)
 */
public class InjectTest extends GWTTestCase {
  public void testSimpleInjector() {
    SimpleInjector injector = GWT.create(SimpleInjector.class);

    SimpleObject simple = injector.getSimple();
    assertNotNull(simple);

    // Since we have not set scope, we should get a new instance each time
    assertNotSame(simple, injector.getSimple());
  }

  public void testMyAppInjector() {
    MyAppInjector injector = GWT.create(MyAppInjector.class);

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

    assertSame(MySingleton.getInstance(), injector.getSingleton());
  }

  public void testBindingAnnotations() {
    MyAppInjector injector = GWT.create(MyAppInjector.class);

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
    MyAppInjector injector = GWT.create(MyAppInjector.class);

    // Constructor should have been called before we even ask for instance
    assertEquals(1, EagerObject.constructorCalls);

    EagerObject eagerObject = injector.getEagerObject();
    assertNotNull(eagerObject);
    assertSame(eagerObject, injector.getEagerObject());

    // Still no more constructor calls
    assertEquals(1, EagerObject.constructorCalls);
  }

  public void testHierarchicalInjector() {
    HierarchicalMyAppInjector injector = GWT.create(HierarchicalMyAppInjector.class);

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
    MyAppInjector injector = GWT.create(MyAppInjector.class);

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
    MyAppInjector injector = GWT.create(MyAppInjector.class);
    assertEquals(MyAppInjector.ANNOTATED_STRING_VALUE, injector.getAnnotatedString());
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTestModule";
  }
}
