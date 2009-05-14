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
package com.google.gwt.inject.client.providermethods;

import com.google.gwt.core.client.GWT;
import static com.google.gwt.inject.client.providermethods.AbModule.A_VALUE;
import static com.google.gwt.inject.client.providermethods.AbModule.B_VALUE;
import static com.google.gwt.inject.client.providermethods.CdModule.C_VALUE;
import static com.google.gwt.inject.client.providermethods.CdModule.D_VALUE;
import static com.google.gwt.inject.client.providermethods.CdModule.SPACE_VALUE;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Integration test showing that provider methods work.
 */
public class ProviderMethodsTest extends GWTTestCase {

  public void testProviderMethodsNoArgs() {
    ProviderMethodsGinjector ginjector = GWT.create(ProviderMethodsGinjector.class);
    assertEquals(A_VALUE, ginjector.getA());
    assertEquals(B_VALUE, ginjector.getB());
  }

  public void testProviderMethodsPrivate() {
    ProviderMethodsGinjector ginjector = GWT.create(ProviderMethodsGinjector.class);
    assertEquals(C_VALUE, ginjector.getC());
    assertEquals(D_VALUE, ginjector.getD());
  }

  public void testProviderMethodsWithArgs() {
    ProviderMethodsGinjector ginjector = GWT.create(ProviderMethodsGinjector.class);
    assertEquals(A_VALUE + SPACE_VALUE + B_VALUE + SPACE_VALUE + C_VALUE + SPACE_VALUE + D_VALUE,
        ginjector.getAll());
  }

  public void testInjectAnnotatedProviderMethods() {
    DamagedGinjector ginjector = GWT.create(DamagedGinjector.class);

    // This should either simply work or we will go into an infinite loop in
    // the ginjector generation. The latter is obviously bad. :)
    assertEquals("8 foo", ginjector.getString());
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}
