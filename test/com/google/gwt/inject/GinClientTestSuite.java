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
package com.google.gwt.inject;

import com.google.gwt.inject.client.InjectTest;
import com.google.gwt.inject.client.InnerGinjectorTest;
import com.google.gwt.inject.client.assistedinject.CarFactoryTest;
import com.google.gwt.inject.client.binding.ConstantBindingTest;
import com.google.gwt.inject.client.binding.EagerBindingTest;
import com.google.gwt.inject.client.binding.GinjectorBindingTest;
import com.google.gwt.inject.client.binding.InjectMembersTest;
import com.google.gwt.inject.client.configurationmodules.ConfigurationModulesTest;
import com.google.gwt.inject.client.eager.EagerSingletonTest;
import com.google.gwt.inject.client.field.FieldInjectTest;
import com.google.gwt.inject.client.generics.GenericsTest;
import com.google.gwt.inject.client.gwtdotcreate.BikeTest;
import com.google.gwt.inject.client.gwtdotcreate.CarTest;
import com.google.gwt.inject.client.gwtdotcreate.GwtDotCreateInjectTest;
import com.google.gwt.inject.client.hierarchical.HierarchicalTest;
import com.google.gwt.inject.client.implicit.AsyncProviderTest;
import com.google.gwt.inject.client.implicit.ImplicitBindingTest;
import com.google.gwt.inject.client.installduplicate.InstallDuplicateTest;
import com.google.gwt.inject.client.jsr330.Jsr330Test;
import com.google.gwt.inject.client.method.MethodInjectTest;
import com.google.gwt.inject.client.misc.StaticInjectTest;
import com.google.gwt.inject.client.nomodules.NoModulesTest;
import com.google.gwt.inject.client.nonpublic.NonPublicTest;
import com.google.gwt.inject.client.optional.OptionalInjectionTest;
import com.google.gwt.inject.client.privatebasic.PrivateBasicTest;
import com.google.gwt.inject.client.privatefactory.PrivateFactoryTest;
import com.google.gwt.inject.client.privatemanylevel.PrivateManyLevelTest;
import com.google.gwt.inject.client.provider.ProviderTest;
import com.google.gwt.inject.client.providermethods.ProviderMethodsTest;
import com.google.gwt.inject.client.scopedimplicit.ScopedImplicitTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * Test suite to roll up all the client tests.
 * Note that this needs to not be under {@code .client} so GWT doesn't
 * try to compile it to JS (which will not work).
 */
// TODO(bstoler): Some way to not manually maintain this list?
public class GinClientTestSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Gin client tests");
    suite.addTestSuite(InjectTest.class);
    suite.addTestSuite(InnerGinjectorTest.class);
    suite.addTestSuite(ConstantBindingTest.class);
    suite.addTestSuite(FieldInjectTest.class);
    suite.addTestSuite(GwtDotCreateInjectTest.class);
    suite.addTestSuite(HierarchicalTest.class);
    suite.addTestSuite(MethodInjectTest.class);
    suite.addTestSuite(NoModulesTest.class);
    suite.addTestSuite(ScopedImplicitTest.class);
    suite.addTestSuite(ProviderMethodsTest.class);
    suite.addTestSuite(StaticInjectTest.class);
    suite.addTestSuite(NonPublicTest.class);
    suite.addTestSuite(GenericsTest.class);
    suite.addTestSuite(OptionalInjectionTest.class);
    suite.addTestSuite(GinjectorBindingTest.class);
    suite.addTestSuite(EagerBindingTest.class);
    suite.addTestSuite(EagerSingletonTest.class);
    suite.addTestSuite(ImplicitBindingTest.class);
    suite.addTestSuite(AsyncProviderTest.class);
    suite.addTestSuite(ProviderTest.class);
    suite.addTestSuite(InjectMembersTest.class);
    suite.addTestSuite(Jsr330Test.class);
    suite.addTestSuite(CarFactoryTest.class);
    suite.addTestSuite(CarTest.class);
    suite.addTestSuite(BikeTest.class);
    suite.addTestSuite(ConfigurationModulesTest.class);
    suite.addTestSuite(InstallDuplicateTest.class);
    suite.addTestSuite(PrivateBasicTest.class);
    suite.addTestSuite(PrivateManyLevelTest.class);
    suite.addTestSuite(PrivateFactoryTest.class);

    return suite;
  }
}
