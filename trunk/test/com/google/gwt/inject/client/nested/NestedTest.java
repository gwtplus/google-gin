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

package com.google.gwt.inject.client.nested;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

public class NestedTest extends GWTTestCase {

  public void testTwiceNestedInjection() {
    NestedGinjector ginjector = GWT.create(NestedGinjector.class);
    Outer.Inner.InnerInner inner = ginjector.getInnerInner();
    assertEquals("H3llo", inner.getHello());
    assertEquals("w0rld", inner.getWorld());
  }

  // see http://code.google.com/p/google-gin/issues/detail?id=107
  public void testParallelInnerGinjectors() {
    A.InnerGinjector ginjector1 = GWT.create(A.InnerGinjector.class);
    B.InnerGinjector ginjector2 = GWT.create(B.InnerGinjector.class);

    assertNotSame(ginjector1, ginjector2);
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}
