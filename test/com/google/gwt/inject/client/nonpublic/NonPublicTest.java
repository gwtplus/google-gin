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
package com.google.gwt.inject.client.nonpublic;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for issue #1. We don't actually support injecting non-public types,
 * so this Ginjector creation is expected to fail. This test does illustrate
 * that we give good error messages in this case. (They are not asserted but
 * you can see them in the output.)
 */
public class NonPublicTest extends GWTTestCase {
  public void testNonPublic() throws Exception {
    try {
      NonPublicGinjector ginjector = GWT.create(NonPublicGinjector.class);
    } catch (RuntimeException e) {
      // Expected it to fail. Nothing much more specific to assert.
    }

    // This is what we would test if we actually supported injecting non-public classes
    /*
    SecretMain main = ginjector.getMain();

    assertNotNull(main.getInnerViaConstructor());
    assertNotNull(main.getInnerViaField());
    assertNotNull(main.getInnerViaMethod());
    assertNotNull(main.getPrivateViaConstructor());
    assertNotNull(main.getPrivateViaField());
    assertNotNull(main.getPrivateViaMethod());
    */
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}
