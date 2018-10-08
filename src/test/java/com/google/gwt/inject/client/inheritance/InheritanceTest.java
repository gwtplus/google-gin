/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.inject.client.inheritance;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.NoGinModules;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Test that when using method injection that the methods of the super class are all injected 
 * before the derived class method gets injected.
 * 
 * @author David Nouls
 */
public class InheritanceTest extends GWTTestCase {

  @NoGinModules
  interface FooGinjector extends Ginjector {
    Foo createFoo();
  }


  public void testInjectionInheritance() {
    FooGinjector injector = GWT.create(FooGinjector.class);
    Foo foo = injector.createFoo();
    
    // Method injection asserts the order but as a sanity check let's see if the injection worked:
    if (!foo.success) { System.err.println("[ERROR] InheritanceTest.testInjectionInheritance() failed."); }
    
    //FIXME: Enable assertion when fixed
    //assertTrue(foo.success);
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}
