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

package com.google.gwt.inject.client.packageprivate.subpackage;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.assistedinject.GinFactoryModuleBuilder;
import com.google.gwt.inject.client.packageprivate.PackagePrivateTest.ProvidedInSubpackage;
import com.google.gwt.inject.client.packageprivate.PackagePrivateTest.PublicAssisted;
import com.google.gwt.inject.client.packageprivate.PackagePrivateTest.PublicGeneric;
import com.google.gwt.inject.client.packageprivate.PackagePrivateTest.PublicInterface;
import com.google.gwt.inject.client.packageprivate.PackagePrivateTest.PublicProvidedClass;
import com.google.gwt.inject.client.packageprivate.subpackage.subsubpackage.SubSubPackageGinModule;
import com.google.gwt.inject.client.packageprivate.subpackage.subsubpackage.SubSubPackageGinModule.ChildPublicInterface;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import java.util.List;

public class SubPackageGinModule extends AbstractGinModule {
  @Override
  public void configure() {
    install(new SubSubPackageGinModule());

    // Test binding a public interface to a package-private implementation.
    bind(PublicInterface.class).to(PrivateImplementation.class);

    // Test binding a package-private class as an eager singleton.
    bind(HiddenSingleton.class).asEagerSingleton();

    // Test binding a public class to a package-private provider.
    bind(PublicProvidedClass.class).toProvider(HiddenProvider.class);

    install(new GinFactoryModuleBuilder().build(HiddenAssisted.Factory.class));
    GinFactoryModuleBuilder builder = new GinFactoryModuleBuilder()
        .implement(PublicAssisted.class, Names.named("a"), PublicAssistedImplementation.class);
    SubSubPackageGinModule.implementB(builder);
    install(builder.build(PublicAssisted.Factory.class));

    requestStaticInjection(PublicImplementation.class);
  }

  @Provides
  public List<HiddenDependency> provideHiddenDependencyList() {
    return null;
  }

  public static class PublicImplementation {
    // Test that a public class can depend on package-private classes.
    @Inject
    PublicImplementation(
        ChildPublicInterface childPublicInterface,
        HiddenDependency hiddenDependency, HiddenSingleton hiddenSingleton,
        // Verify that nothing screwy happens if a public generic is
        // parameterized on a non-public type.  Also testing with List because
        // there were issues with java.util classes specifically.
        List<HiddenDependency> hiddenList, PublicGeneric<HiddenDependency> hiddenGeneric,
        // Early versions of this code would
        // put provider creators in com.google.inject, preventing them from
        // referencing private classes.  Verify that this works:
        Provider<HiddenDependency> hiddenDependencyProvider,
        @Named("providerClass") ProvidedInSubpackage providedByProviderClass,
        @Named("providerMethod") ProvidedInSubpackage providedByProviderMethod) {
    }

    // Verify that method injection compiles with package-private methods.
    @Inject
    void setHiddenDependency(HiddenDependency hiddenDependency) {
    }

    // Verify that field injection compiles with package-private fields.
    @Inject
    HiddenDependency hiddenDependency;

    // Verify that we can inject a static, package-private value.
    @Inject
    static HiddenDependency staticHiddenDependency;
  }

  static class PrivateImplementation implements PublicInterface {
  }

  static class HiddenSingleton {
    @Inject
    HiddenSingleton(HiddenDependency hiddenDependency) {
    }
  }

  // Verify that the hidden classes can inject hidden dependencies.
  static class HiddenDependency {
  }

  static class HiddenProvider implements Provider<PublicProvidedClass> {
    @Override
    public PublicProvidedClass get() {
      return new PublicProvidedClass();
    }
  }

  // Verify that hidden classes can be created with assisted injection.
  static class HiddenAssisted {
    @Inject
    HiddenAssisted(HiddenDependency dep, @Assisted HiddenDependency assisted) {
    }

    interface Factory {
      HiddenAssisted create(HiddenDependency assisted);
    }
  }

  // Verify that hidden classes with public interfaces and hidden dependencies
  // can be created with assisted injection.

  static class PublicAssistedImplementation implements PublicAssisted {
    @Inject
    PublicAssistedImplementation(HiddenDependency dep, @Assisted long value) {
    }
  }

  // Check that we can have a hidden synchronous RPC interface linking to a
  // public asynchronous RPC interface.
  @RemoteServiceRelativePath("subpackageRemoteService")
  interface PublicService extends RemoteService {
    String hello(String name);
  }

  public interface PublicServiceAsync {
    void hello(String name, AsyncCallback<String> callback);
  }
}
