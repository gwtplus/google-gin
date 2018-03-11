/*
 * Copyright 2009 Google Inc.
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

package com.google.gwt.inject.client.eager;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

public class EagerSingletonTest extends GWTTestCase {

  // Tests http://code.google.com/p/google-gin/issues/detail?id=48.
  // This method only fails with the incorrect implementation if other
  // singleton is bound before my singleton in the module.
  public void testEagerSingletonDependencies() {
    MyEagerSingleton.instances = 0;
    OtherEagerSingleton.instances = 0;

    EagerGinjector ginjector = GWT.create(EagerGinjector.class);

    assertEquals(1, MyEagerSingleton.instances);
    assertEquals(1, OtherEagerSingleton.instances);

    OtherEagerSingleton otherEagerSingleton = ginjector.getOtherEagerSingleton();
    assertNotNull(otherEagerSingleton.myEagerSingleton);
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}
