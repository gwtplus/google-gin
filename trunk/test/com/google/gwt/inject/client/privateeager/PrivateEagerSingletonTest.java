/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.inject.client.privateeager;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.PrivateGinModule;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.Inject;

public class PrivateEagerSingletonTest extends GWTTestCase {

  // Tests http://code.google.com/p/google-gin/issues/detail?id=156.
  //
  // The pattern that caused trouble was an eager singleton in one module which
  // depended on a binding exposed from another module.  This failed
  // intermittently depending on the order in which the private ginjectors were
  // instantiated; if the eager singleton's module was instantiated first, we
  // would get a NullPointerException when it tried to access the other module.
  //
  // This test creates two private modules, each of which contains an eager
  // singleton that depends on a binding exposed from the other module.  That
  // way, we would see a failure regardless of which order the injectors were
  // instantiated in.
  public void testEagerSingletonDependencies() {
    Singleton1.instances = 0;
    Singleton2.instances = 0;

    PrivateEagerGinjector ginjector = GWT.create(PrivateEagerGinjector.class);

    assertEquals(1, Singleton1.instances);
    assertEquals(1, Singleton2.instances);

    Singleton1 singleton11 = ginjector.getSingleton1();
    Singleton1 singleton12 = ginjector.getSingleton1();
    Singleton2 singleton21 = ginjector.getSingleton2();
    Singleton2 singleton22 = ginjector.getSingleton2();

    assertEquals(1, Singleton1.instances);
    assertEquals(1, Singleton2.instances);

    assertSame(singleton11, singleton12);
    assertSame(singleton21, singleton22);
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }

  @GinModules({TestGinModule1.class, TestGinModule2.class})
  public interface PrivateEagerGinjector extends Ginjector {
    Singleton1 getSingleton1();
    Singleton2 getSingleton2();
  }

  public static class Injected1 {
  }

  public static class Injected2 {
  }

  public static class Singleton1 {

    static int instances = 0;

    @Inject public Singleton1(Injected2 injected2) {
      instances++;
    }
  }

  public static class Singleton2 {

    static int instances = 0;

    @Inject public Singleton2(Injected1 injected2) {
      instances++;
    }
  }

  static class TestGinModule1 extends PrivateGinModule {
    @Override
    protected void configure() {
      bind(Injected1.class);
      bind(Singleton1.class).asEagerSingleton();

      expose(Injected1.class);
      expose(Singleton1.class);
    }
  }

  static class TestGinModule2 extends PrivateGinModule {
    @Override
    protected void configure() {
      bind(Injected2.class);
      bind(Singleton2.class).asEagerSingleton();

      expose(Injected2.class);
      expose(Singleton2.class);
    }
  }
}
