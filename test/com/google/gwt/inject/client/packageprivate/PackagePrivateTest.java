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
import com.google.gwt.inject.client.assistedinject.GinFactoryModuleBuilder;
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
    assertNotNull(injector.getPublicImplementation());
    assertNotNull(injector.getPublicInterface());
    assertNotNull(injector.getPublicInterfaceProvider().get());
    assertNotNull(injector.getPublicProvidedClass());
    assertNotNull(injector.getPublicProvidedClassProvider().get());
    assertNotNull(injector.getPublicServiceAsync());
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }

  @GinModules({SubPackageGinModule.class})
  interface CrossPackageGinjector extends Ginjector {
    CrossPackageDepender getCrossPackageDepender();
    SubPackageGinModule.PublicImplementation getPublicImplementation();
    PublicInterface getPublicInterface();
    Provider<PublicInterface> getPublicInterfaceProvider();
    PublicProvidedClass getPublicProvidedClass();
    Provider<PublicProvidedClass> getPublicProvidedClassProvider();
    SubPackageGinModule.PublicServiceAsync getPublicServiceAsync();
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
