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
package com.google.gwt.inject.client.hierarchical;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.SimpleObject;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Gin tests using a {@code Ginjector} type hierarchy.
 */
public class HierarchicalTest extends GWTTestCase {
  public void testHierarchicalInjector() {
    HierarchicalMyAppGinjector injector = GWT.create(HierarchicalMyAppGinjector.class);

    SimpleObject simple = injector.getSimple();
    assertNotNull(simple);

    SimpleObject unnamedSimple = injector.getUnnamedSimple();
    assertNotNull(unnamedSimple);
    assertSame(simple, unnamedSimple);

    SimpleObject purple = injector.getSimplePurple();
    assertNotNull(purple);
    assertSame(simple, purple);

    SimpleObject red = injector.getSimpleRed();
    assertNotNull(red);
    assertSame(purple, red);

    SimpleObject blue = injector.getSimpleBlue();
    assertNotNull(blue);
    assertSame(blue, red);
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}
