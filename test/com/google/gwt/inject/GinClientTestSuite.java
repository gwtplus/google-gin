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
import com.google.gwt.inject.client.binding.BindConstantBindingTest;
import com.google.gwt.inject.client.field.FieldInjectTest;
import com.google.gwt.inject.client.gwtdotcreate.GwtDotCreateInjectTest;
import com.google.gwt.inject.client.hierarchical.HierarchicalTest;
import com.google.gwt.inject.client.method.MethodInjectTest;
import com.google.gwt.inject.client.nomodules.NoModulesTest;
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
    suite.addTestSuite(BindConstantBindingTest.class);
    suite.addTestSuite(FieldInjectTest.class);
    suite.addTestSuite(GwtDotCreateInjectTest.class);
    suite.addTestSuite(HierarchicalTest.class);
    suite.addTestSuite(MethodInjectTest.class);
    suite.addTestSuite(NoModulesTest.class);
    suite.addTestSuite(ScopedImplicitTest.class);
    
    // TODO(bstoler): Add back once other patch is submitted
    // http://code.google.com/p/google-gin/issues/detail?id=16
//    suite.addTestSuite(ProviderMethodsTest.class);

    return suite;
  }
}
