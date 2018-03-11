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
package com.google.gwt.inject.client.privatepinned;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.PrivateGinModule;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Verify that pinning and exposing work together.
 *
 * <p>Sets up an implicit, unconstrained binding (the Implementation classes)
 * that depends on a pinned, private binding.  The unconstrained binding has to
 * move into the private module; we had errors in the past because it would
 * "escape" the module and drag the pinned binding with it.
 */
public class PrivatePinnedTest extends GWTTestCase {

  public void testBindingsStayInSubModulesA() throws Exception {
    // Both Module1 and Module2 are installed in the Ginjector
    verifyBindingsStayInSubModules((TestInterface) GWT.create(TestGinjectorA.class));
  }

  public void testBindingsStayInSubModulesB() throws Exception {
    // Both Module1 is installed in the ginjector, and Module2 is installed
    // through an extra layer of indirection.  Verifies that the pins work.
    verifyBindingsStayInSubModules((TestInterface) GWT.create(TestGinjectorB.class));
  }

  public void testNoDoubleBindingFromInheritedImplicitBinding() {
    // Verifies that if the parent pins a singleton that it also implicitly
    // requires, and a child implicitly requires it, the singleton is
    // instantiated in the parent and doesn't cause a double binding error.
    GWT.create(TestGinjectorC.class);
  }

  private void verifyBindingsStayInSubModules(TestInterface ginjector) throws Exception {
    Interface1 interface1 = ginjector.getInterface1();
    Interface2 interface2 = ginjector.getInterface2();

    SubImplementation subImplementation1 = interface1.getSubImplementation();
    SubImplementation subImplementation2 = interface2.getSubImplementation();

    assertNotNull(subImplementation1);
    assertNotNull(subImplementation2);

    Interface1 otherInterface1 = ginjector.getInterface1();
    Interface2 otherInterface2 = ginjector.getInterface2();

    assertNotSame(interface1, otherInterface1);
    assertNotSame(interface2, otherInterface2);

    SubImplementation otherSubImplementation1 = otherInterface1.getSubImplementation();
    SubImplementation otherSubImplementation2 = otherInterface2.getSubImplementation();

    assertSame(subImplementation1, otherSubImplementation1);
    assertSame(subImplementation2, otherSubImplementation2);

    assertNotSame(subImplementation1, subImplementation2);
  }

  interface TestInterface {
    Interface1 getInterface1();
    Interface2 getInterface2();
  }

  @GinModules({Module1.class, Module2a.class})
  interface TestGinjectorA extends Ginjector, TestInterface {}

  @GinModules({Module1.class, TopModule.class})
  interface TestGinjectorB extends Ginjector, TestInterface {}

  @GinModules({TopModuleCheckForDoubleBinding.class})
  interface TestGinjectorC extends Ginjector, TestInterface {}

  static class TopModule extends PrivateGinModule {
    @Override
    protected void configure() {
      bind(SubImplementation.class).in(Singleton.class);
      install(new Module2b());
      expose(Interface2.class);
    }
  }

  // Verifies that nothing bad happens if the parent requires the same key that
  // it promises to bind.
  static class TopModuleCheckForDoubleBinding extends PrivateGinModule {
    @Override
    protected void configure() {
      bind(Interface1.class).to(Implementation1.class);
      bind(Implementation1.class);
      // This is the pinned binding that the child will require.  The parent
      // requires it because Implementation1 is pinned here (see above), while
      // the child requires it because Implementation2 is pinned there (see
      // below).
      bind(SubImplementation.class);
      install(new Module2b());

      expose(Interface1.class);
      expose(Interface2.class);
    }
  }

  static class Module1 extends PrivateGinModule {
    @Override
    protected void configure() {
      bind(Interface1.class).to(Implementation1.class);
      expose(Interface1.class);
      bind(SubImplementation.class).in(Singleton.class);
    }
  }

  static class Module2a extends PrivateGinModule {
    @Override
    protected void configure() {
      bind(Interface2.class).to(Implementation2.class);
      expose(Interface2.class);
      bind(SubImplementation.class).in(Singleton.class);
    }
  }

  static class Module2b extends PrivateGinModule {
    @Override
    protected void configure() {
      bind(Interface2.class).to(Implementation2.class);
      // If we don't pin this here, it floats out of the module, so we don't
      // need a local binding for Subimplementation.
      bind(Implementation2.class);
      expose(Interface2.class);
    }
  }

  interface Interface1 {
    SubImplementation getSubImplementation();
  }

  interface Interface2 {
    SubImplementation getSubImplementation();
  }

  static class Implementation1 implements Interface1 {
    private final SubImplementation subImplementation;

    @Inject
    public Implementation1(SubImplementation subImplementation) {
      this.subImplementation = subImplementation;
    }

    @Override
    public SubImplementation getSubImplementation() {
      return subImplementation;
    }
  }

  static class Implementation2 implements Interface2 {
    private final SubImplementation subImplementation;

    @Inject
    public Implementation2(SubImplementation subImplementation) {
      this.subImplementation = subImplementation;
    }

    @Override
    public SubImplementation getSubImplementation() {
      return subImplementation;
    }
  }

  static class SubImplementation {
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}
