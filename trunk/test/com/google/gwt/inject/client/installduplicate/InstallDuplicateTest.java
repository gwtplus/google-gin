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
package com.google.gwt.inject.client.installduplicate;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.GinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.PrivateGinModule;
import com.google.gwt.inject.client.binder.GinBinder;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

/**
 * Tests that modules that compare equal are actually installed once only.
 * <p>
 * This is done by installing a bunch of modules twice, using different
 * scenarios:
 * <ul>
 * <li>Referenced twice in a {@literal @}{@link GinModules} annotation
 * <li>Referenced once in a {@literal @}{@link GinModules} annotation and then
 * {@link GinBinder#install(GinModule) installed}.
 * <li>{@link GinBinder#install(GinModule) Installed} twice using the very same
 * instance.
 * <li>{@link GinBinder#install(GinModule) Installed} twice using two instances
 * that compare equal.
 * </ul>
 * <p>
 * The {@link Ginjector}s explicitly declare the dependencies on what the
 * modules install, to make sure they're all actually installed.
 * <p>
 * The test fails when compilation fails, because of double-bound keys that
 * would result from {@link #configure(GinBinder) configuring} the modules
 * twice. GIN should actually {@link #configure(GinBinder) configure} modules
 * once per scenario only, because the modules compare equal.
 */
public class InstallDuplicateTest extends GWTTestCase {

  public void testDuplicateModuleThroughGinModulesAnnotation() {
    GWT.create(TestGinjector.class);
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }

  @GinModules( {
      ReferencedTwiceFromGinModulesAnnotation.class, TestModule.class,
      InstalledFromDistinctModules.A.class, InstalledFromDistinctModules.B.class})
  public interface TestGinjector extends BaseTestGinjector {
    @Named("InstalledFromDistinctModules")
    Foo installedFromDistinctModules();
    
    @Named("SameInstanceInstalledTwice")
    Foo sameInstanceInstalledTwice();
    
    @Named("TwoInstancesInstalledOnBinder")
    Foo twoInstancesInstalledOnBinder();
  }

  @GinModules( {
      ReferencedTwiceFromGinModulesAnnotation.class,
      ReferencedFromAnnotationAndInstalledOnBinder.class})
  public interface BaseTestGinjector extends Ginjector {
    @Named("ReferencedTwiceFromGinModulesAnnotation")
    Foo referencedTwiceFromGinModulesAnnotation();

    @Named("ReferencedFromAnnotationAndInstalledOnBinder")
    Foo referencedFromAnnotationAndInstalledOnBinder();
  }

  static class TestModule implements GinModule {

    public void configure(GinBinder binder) {
      binder.install(new ReferencedFromAnnotationAndInstalledOnBinder());
      SameInstanceInstalledTwice module = new SameInstanceInstalledTwice();
      binder.install(module);
      binder.install(new TwoInstancesInstalledOnBinder());
      binder.install(module);
      binder.install(new TwoInstancesInstalledOnBinder());
      binder.install(new PrivateModuleInstalledTwiceOnBinder());
      binder.install(new PrivateModuleInstalledTwiceOnBinder());
    }
  }

  /**
   * This class is injected using different binding annotations, one for each
   * scenario.
   */
  static class Foo {
  }

  /**
   * Two instances are referenced from {@literal @}{@link GinModules} on the
   * {@link Ginjector}.
   * <p>
   * This class doesn't implement equals and hashCode because it is expected
   * that module classes referenced from GinModules are instantiated once only.
   */
  static class ReferencedTwiceFromGinModulesAnnotation implements GinModule {
    public void configure(GinBinder binder) {
      binder.bind(Foo.class).annotatedWith(
          Names.named("ReferencedTwiceFromGinModulesAnnotation")).to(Foo.class);
    }
  }

  /**
   * One instance is referenced from {@literal @}{@link GinModules} on the
   * {@link Ginjector}, another is installed on the {@link GinBinder}.
   */
  static class ReferencedFromAnnotationAndInstalledOnBinder implements GinModule {
    public void configure(GinBinder binder) {
      binder.bind(Foo.class).annotatedWith(
          Names.named("ReferencedFromAnnotationAndInstalledOnBinder")).to(Foo.class);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof ReferencedFromAnnotationAndInstalledOnBinder;
    }

    @Override
    public int hashCode() {
      return 31;
    }
  }

  /**
   * Does not implement equals/hashCode but the same instance is installed
   * twice.
   */
  static class SameInstanceInstalledTwice implements GinModule {
    public void configure(GinBinder binder) {
      binder.bind(Foo.class).annotatedWith(Names.named("SameInstanceInstalledTwice")).to(Foo.class);
    }
  }

  /**
   * All instances are equal to each another.
   */
  static class TwoInstancesInstalledOnBinder implements GinModule {
    public void configure(GinBinder binder) {
      binder.bind(Foo.class).annotatedWith(
          Names.named("TwoInstancesInstalledOnBinder")).to(Foo.class);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof TwoInstancesInstalledOnBinder;
    }

    @Override
    public int hashCode() {
      return 42;
    }
  }
  
  static class PrivateModuleInstalledTwiceOnBinder extends PrivateGinModule {
    @Override
    protected void configure() {
      bind(Foo.class).annotatedWith(Names.named("BoundInPrivateModule"))
          .to(Foo.class);
      expose(Foo.class).annotatedWith(Names.named("BoundInPrivateModule"));
    }
    
    @Override
    public boolean equals(Object obj) {
      return obj instanceof PrivateModuleInstalledTwiceOnBinder;
    }

    @Override
    public int hashCode() {
      return 42;
    }
  }

  static class InstalledFromDistinctModules implements GinModule {
    public void configure(GinBinder binder) {
      binder.bind(Foo.class).annotatedWith(
          Names.named("InstalledFromDistinctModules")).to(Foo.class);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof InstalledFromDistinctModules;
    }

    @Override
    public int hashCode() {
      return 42;
    }

    static class A implements GinModule {
      public void configure(GinBinder binder) {
        binder.install(new InstalledFromDistinctModules());
      }
    }

    static class B implements GinModule {
      public void configure(GinBinder binder) {
        binder.install(new InstalledFromDistinctModules());
      }
    }
  }
}
