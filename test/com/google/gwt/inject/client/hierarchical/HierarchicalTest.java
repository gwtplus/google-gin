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
package com.google.gwt.inject.client.hierarchical;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.SimpleObject;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Gin tests using a {@code Ginjector} type hierarchy.
 */
public class HierarchicalTest extends GWTTestCase {
  public void testHierarchicalInjector() {
    HierarchicalMyAppGinjector injector = GWT.create(HierarchicalMyAppGinjector.class);

    SimpleObject simple = injector.getSimple();
    assertNotNull(simple);

    SimpleObject unnamedSimple = injector.getUnnamedSimple();
    assertNotNull(unnamedSimple);
    assertSame(simple, unnamedSimple);

    SimpleObject purple = injector.getSimplePurple();
    assertNotNull(purple);
    assertSame(simple, purple);

    SimpleObject red = injector.getSimpleRed();
    assertNotNull(red);
    assertSame(purple, red);

    SimpleObject blue = injector.getSimpleBlue();
    assertNotNull(blue);
    assertSame(blue, red);
  }

  public void testOverlappingModules() {
    ChildGinjector ginjector = GWT.create(ChildGinjector.class);

    assertEquals("foo", ginjector.getRed());
    assertEquals("bar", ginjector.getBlue());
    assertEquals("baz", ginjector.getGreen());
  }

  public void testModuleInitializationOrder() {
    ChildGinjector ginjector = GWT.create(ChildGinjector.class);

    List<?> expectedOrder = Arrays.asList(GinModuleA.class, GinModuleC.class, GinModuleB.class);
    assertEquals(expectedOrder, ginjector.getModuleInitializationList());
  }

  public void testInheritedNonGinjector() {
    InheritingGinjector ginjector = GWT.create(InheritingGinjector.class);
    assertEquals("foo", ginjector.getString());
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }


  @Singleton
  public static class ModuleInitializationList extends ArrayList<Object> {}

  public static class InitializationMarker<T> {
    @Inject
    InitializationMarker(T module, ModuleInitializationList list) {
      list.add(module.getClass());
    }
  }

  public static class GinModuleA extends AbstractGinModule {
    @Override
    protected void configure() {
      bindConstant().annotatedWith(Names.named("red")).to("foo");
      bind(new TypeLiteral<InitializationMarker<GinModuleA>>(){}).asEagerSingleton();
    }
  }

  public static class GinModuleB extends AbstractGinModule {
    @Override
    protected void configure() {
      bindConstant().annotatedWith(Names.named("blue")).to("bar");
      bind(new TypeLiteral<InitializationMarker<GinModuleB>>(){}).asEagerSingleton();
    }
  }

  public static class GinModuleC extends AbstractGinModule {
    @Override
    protected void configure() {
      bindConstant().annotatedWith(Names.named("green")).to("baz");
      bind(new TypeLiteral<InitializationMarker<GinModuleC>>(){}).asEagerSingleton();
    }
  }

  @GinModules({GinModuleA.class, GinModuleC.class})
  public static interface SuperGinjector extends Ginjector {
    @Named("red") String getRed();

    @Named("green") String getGreen();

    ModuleInitializationList getModuleInitializationList();
  }

  @GinModules({GinModuleB.class, GinModuleC.class})
  public static interface ChildGinjector extends SuperGinjector {
    @Named("blue") String getBlue();
  }

  static class SimpleGinModule extends AbstractGinModule {

    @Override
    protected void configure() {}

    @Provides
    String provideString() {
      return "foo";
    }
  }

  interface RandomInterface {
    String getString();
  }

  @GinModules(SimpleGinModule.class)
  interface InheritingGinjector extends RandomInterface, Ginjector {}
}
