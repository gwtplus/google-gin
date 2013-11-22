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
package com.google.gwt.inject.client.nomodules;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Simple tests of a Ginjector without any modules.
 */
public class NoModulesTest extends GWTTestCase {
  public void testSimpleInjector() {
    NoModulesGinjector injector = GWT.create(NoModulesGinjector.class);

    Something thing = injector.getSomething();
    assertNotNull(thing);

    // Since we have not set scope, we should get a new instance each time
    assertNotSame(thing, injector.getSomething());
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}
