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
package com.google.gwt.inject.client.misc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.misc.subpackage.StaticSubEagerSingleton;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class StaticInjectTest extends GWTTestCase {

  @Override
  public void gwtTearDown() {
    StaticEagerSingleton.resetBar();
    StaticSubEagerSingleton.resetFoo();
  }

  public void testPublicStaticFieldInject() {
    GWT.create(StaticInjectGinjector.class);

    assertNotNull(StaticClass.getName1());
  }

  public void testPrivateStaticFieldInject() {
    GWT.create(StaticInjectGinjector.class);

    assertNotNull(StaticClass.getName2());
  }

  public void testPublicStaticMethodInject() {
    GWT.create(StaticInjectGinjector.class);

    assertNotNull(StaticClass.getName3());
  }

  public void testPrivateStaticMethodInject() {
    GWT.create(StaticInjectGinjector.class);

    assertNotNull(StaticClass.getName4());
  }

  public void testNonRegisteredStaticInject() {
    GWT.create(StaticInjectGinjector.class);

    assertNull(DynamicClass.getName1());
    assertNull(DynamicClass.getName2());
  }

  public void testStaticGinjectorInjection() {
    GWT.create(StaticInjectGinjector.class);

    assertNotNull(StaticClass.getInjector());
  }

  public void testEagerSingletonOrdering() {
    StaticInjectGinjector injector = GWT.create(StaticInjectGinjector.class);

    // Verify that eager singletons are injected after static injection takes
    // place.
    assertEquals("bar", injector.getStaticEagerSingleton().getBarCopy());
    assertEquals("foo", injector.getStaticSubEagerSingleton().getFooCopy());
  }

  public void testEagerSingletonOrdering_crossPackage() {
    StaticInjectGinjector injector = GWT.create(StaticInjectGinjector.class);

    // Verify that eager singletons are injected after static injection takes
    // place, even across packages.
    assertEquals("bar", injector.getStaticSubEagerSingleton().getBarCopy());
    assertEquals("foo", injector.getStaticEagerSingleton().getFooCopy());
  }

  public void testSuperClassInjection() {
    GWT.create(SuperClassGinjector.class);

    assertEquals("f00", SuperClass.foo);
    assertEquals("b4r", SuperClass.bar);
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }


  public static class SuperClass {
    @Inject @Named("foo") static String foo;
    static String bar;

    @Inject
    static void setBar(@Named("bar") String string) {
      bar = string;
    }
  }

  public static class SubClass extends SuperClass {}

  public static class SuperClassGinModule extends AbstractGinModule {

    @Override
    protected void configure() {
      bindConstant().annotatedWith(Names.named("foo")).to("f00");
      bindConstant().annotatedWith(Names.named("bar")).to("b4r");
      requestStaticInjection(SubClass.class);
    }
  }

  @GinModules(SuperClassGinModule.class)
  public static interface SuperClassGinjector extends Ginjector {}
}
