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

package com.google.gwt.inject.client.packageprivate;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.packageprivate.subpackage.SubPackageGinModule;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class PackagePrivateTest extends GWTTestCase {

  // Tests http://code.google.com/p/google-gin/issues/detail?id=86

  public void testPackagePrivateInjection() {
    CrossPackageGinjector injector = GWT.create(CrossPackageGinjector.class);

    // Exercise all the injector methods.
    assertNotNull(injector.getCrossPackageDepender());
    assertNotNull(injector.getCrossPackageExtender());
    assertNotNull(injector.getPublicImplementation());
    assertNotNull(injector.getPublicInterface());
    assertNotNull(injector.getPublicInterfaceProvider().get());
    assertNotNull(injector.getPublicProvidedClass());
    assertNotNull(injector.getPublicProvidedClassProvider().get());
    assertNotNull(injector.getPublicServiceAsync());

    injector.injectCrossPackageDepender(injector.getCrossPackageDepender());
    injector.injectCrossPackageExtender(injector.getCrossPackageExtender());
    injector.injectCrossPackageExtenderChild(injector.getCrossPackageExtenderChild());
    injector.injectCrossPackageExtenderGrandchild(injector.getCrossPackageExtenderGrandchild());
    injector.injectCrossPackageExtenderUninjectedChild(
        injector.getCrossPackageExtenderUninjectedChild());
    injector.injectCrossPackageExtenderChildOfUninjectedChild(
        injector.getCrossPackageExtenderChildOfUninjectedChild());
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }

  @GinModules({PackagePrivateGinModule.class, SubPackageGinModule.class})
  interface CrossPackageGinjector extends Ginjector {
    CrossPackageDepender getCrossPackageDepender();
    CrossPackageExtender getCrossPackageExtender();
    CrossPackageExtenderChild getCrossPackageExtenderChild();
    CrossPackageExtenderGrandchild getCrossPackageExtenderGrandchild();
    CrossPackageExtenderUninjectedChild getCrossPackageExtenderUninjectedChild();
    CrossPackageExtenderChildOfUninjectedChild getCrossPackageExtenderChildOfUninjectedChild();
    SubPackageGinModule.PublicImplementation getPublicImplementation();
    PublicInterface getPublicInterface();
    Provider<PublicInterface> getPublicInterfaceProvider();
    PublicProvidedClass getPublicProvidedClass();
    Provider<PublicProvidedClass> getPublicProvidedClassProvider();
    SubPackageGinModule.PublicServiceAsync getPublicServiceAsync();

    void injectCrossPackageDepender(CrossPackageDepender injectee);
    void injectCrossPackageExtender(CrossPackageExtender injectee);
    void injectCrossPackageExtenderChild(CrossPackageExtenderChild injectee);
    void injectCrossPackageExtenderGrandchild(CrossPackageExtenderGrandchild injectee);
    void injectCrossPackageExtenderUninjectedChild(CrossPackageExtenderUninjectedChild injectee);
    void injectCrossPackageExtenderChildOfUninjectedChild(
        CrossPackageExtenderChildOfUninjectedChild injectee);
  }

  public static class PackagePrivateGinModule extends AbstractGinModule {
    @Override
    public void configure() {
      // Make sure that static injection doesn't cause compile errors.
      requestStaticInjection(CrossPackageDepender.class);
      requestStaticInjection(CrossPackageExtender.class);
      requestStaticInjection(CrossPackageExtenderChild.class);
      requestStaticInjection(CrossPackageExtenderGrandchild.class);
      requestStaticInjection(CrossPackageExtenderChildOfUninjectedChild.class);

      // Make sure we can static-inject a class from another package.
      requestStaticInjection(SubPackageGinModule.PublicImplementation.class);
    }
  }

  // Interface for use in factory tests (the implementation is in another
  // package, to verify that the creation code goes there and not here; we can
  // even have implementations from different packages with individual private
  // dependencies).
  public static interface PublicAssisted {
    public interface Factory {
      @Named("a") PublicAssisted createA(long value);
      @Named("b") PublicAssisted createB(long value);
    }
  }

  // Verify that the injector can return a package-private type.
  static class CrossPackageDepender {
    @Inject CrossPackageDepender(
        // Verify that we can inject the package-private injector.
        CrossPackageGinjector crossPackageGinjector,
        // Verify that from this package, we can inject a class that belongs to
        // another package via a public interface.
        PublicInterface publicInterface,
        // Verify that from this package, we can inject a class that belongs to
        // another package via a public factory.
        PublicAssisted.Factory publicAssisted) {

      publicAssisted.createA(10);
      publicAssisted.createB(10);
    }

    // Verify that field injection works.
    @Inject CrossPackageGinjector injectorField;
    @Inject PublicInterface publicInterfaceField;
    @Inject PublicAssisted.Factory publicAssistedField;

    // Verify that static field injection works.
    @Inject static CrossPackageGinjector staticInjectorField;
    @Inject static PublicInterface staticPublicInterfaceField;
    @Inject static PublicAssisted.Factory staticPublicAssistedField;

    // Verify that method injection works.
    @Inject public void setCrossPackageGinjector(CrossPackageGinjector injector) {
    }

    @Inject
    public void setPublicInterface(PublicInterface publicInterface) {
    }

    @Inject
    public void setPublicAssisted(PublicAssisted.Factory factory) {
      factory.createA(10);
      factory.createB(10);
    }
  }

  // Verify that nothing bad happens when we extend a type that also has
  // method/field injections.
  static class CrossPackageExtender extends SubPackageGinModule.PublicImplementation {
    @Inject
    CrossPackageExtender(PublicInterface publicInterface) {
    }

    protected CrossPackageExtender() {
    }

    @Inject PublicInterface fieldPublicInterface;
    @Inject static PublicInterface staticFieldPublicInterface;

    @Inject
    void setPublicInterface(PublicInterface publicInterface) {
    }
  }

  // Test some knotty corner cases involving subclassing and member injection:
  static class CrossPackageExtenderChild extends CrossPackageExtender {
    @Inject PublicInterface fieldPublicInterface2;
    @Inject static PublicInterface staticFieldPublicInterface2;

    void setPublicInterface2(PublicInterface publicInterface2) {
    }
  }

  static class CrossPackageExtenderGrandchild extends CrossPackageExtenderChild {
    @Inject PublicInterface fieldPublicInterface3;
    @Inject static PublicInterface staticFieldPublicInterface3;

    void setPublicInterface2(PublicInterface publicInterface3) {
    }
  }

  static class CrossPackageExtenderUninjectedChild extends CrossPackageExtender {
  }

  static class CrossPackageExtenderChildOfUninjectedChild
      extends CrossPackageExtenderUninjectedChild {
    @Inject PublicInterface fieldPublicInterface4;
    @Inject static PublicInterface staticFieldPublicInterface4;

    void setPublicInterface2(PublicInterface publicInterface4) {
    }
  }

  public interface PublicInterface {
  }

  public static class PublicGeneric<T> {
  }

  public static class PublicProvidedClass {
  }

  public static interface ProvidedInSubpackage {
  }
}
