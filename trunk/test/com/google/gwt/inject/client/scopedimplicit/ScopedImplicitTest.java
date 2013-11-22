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
package com.google.gwt.inject.client.scopedimplicit;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.MyMessages;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Test to exhibit <a href="http://code.google.com/p/google-gin/issues/detail?id=22">issue 22</a>
 */
public class ScopedImplicitTest extends GWTTestCase {
  public void testA() throws Exception {
    ScopedImplicitGinjector ginjector = GWT.create(ScopedImplicitGinjector.class);
    MyMessages messages = ginjector.messages();
    assertNotNull(messages);
    assertSame(messages, ginjector.messages());
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}
