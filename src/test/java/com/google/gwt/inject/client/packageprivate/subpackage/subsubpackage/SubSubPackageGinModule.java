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

package com.google.gwt.inject.client.packageprivate.subpackage.subsubpackage;

import com.google.gwt.inject.client.PrivateGinModule;
import com.google.gwt.inject.client.assistedinject.GinFactoryModuleBuilder;
import com.google.gwt.inject.client.packageprivate.PackagePrivateTest.ProvidedInSubpackage;
import com.google.gwt.inject.client.packageprivate.PackagePrivateTest.PublicAssisted;
import com.google.gwt.inject.client.packageprivate.PackagePrivateTest.PublicInterface;
import com.google.gwt.inject.client.packageprivate.subpackage.subsubpackage.SubSubPackageGinModule.ChildPublicInterface;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class SubSubPackageGinModule extends PrivateGinModule {
  @Override
  public void configure() {
    bind(ChildPublicInterface.class).to(ChildHiddenImplementation.class);
    bind(ProvidedInSubpackage.class)
        .annotatedWith(Names.named("providerClass")).toProvider(SubpackageProvider.class);

    expose(ChildPublicInterface.class);
    expose(ProvidedInSubpackage.class).annotatedWith(Names.named("providerClass"));
    expose(ProvidedInSubpackage.class).annotatedWith(Names.named("providerMethod"));
  }

  // General hidden type for use below.
  static class ChildHiddenDependency {
  }

  // Test parent <-> child interactions in the presence of package-private
  // types.
  //
  //  1) The parent can inject a public interface implemented in the child by a
  //     package-private type.
  //
  //  2) The child can inject a public interface implemented in the parent by a
  //     package-private type.
  public interface ChildPublicInterface {
  }

  static class ChildHiddenImplementation implements ChildPublicInterface {
    @Inject
    ChildHiddenImplementation(ChildHiddenDependency dep, PublicInterface publicInterface) {
    }
  }

  // Check that a provider method invoked from the parent package to provide a
  // key from another package can inject package-private instances in this
  // package.
  static class ProvidedHere implements ProvidedInSubpackage {
  }

  @Provides
  @Named("providerMethod")
  ProvidedInSubpackage provideProvidedInSubpackage(ChildHiddenDependency dep) {
    return new ProvidedHere();
  }

  // Verify that a package-private provider also works and can inject a
  // package-private type.
  static class SubpackageProvider implements Provider<ProvidedInSubpackage> {
    @Inject SubpackageProvider(ChildHiddenDependency dep) {
    }

    public ProvidedInSubpackage get() {
      return new ProvidedHere();
    }
  }

  // Verify that we can inject multiple private created types from different
  // packages into the same factory interface (eek).
  static class AssistedImplementation implements PublicAssisted {
    @Inject AssistedImplementation(ChildHiddenDependency dep, @Assisted long value) {
    }
  }

  public static void implementB(GinFactoryModuleBuilder builder) {
    builder.implement(PublicAssisted.class, Names.named("b"), AssistedImplementation.class);
  }
}
