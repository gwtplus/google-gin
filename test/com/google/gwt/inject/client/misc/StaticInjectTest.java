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
package com.google.gwt.inject.client.misc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

public class StaticInjectTest extends GWTTestCase {

  public void testPublicStaticFieldInject() {
    GWT.create(StaticInjectGinjector.class);

    assertNotNull(StaticClass.getName1());
  }

  public void testPrivateStaticFieldInject() {
    GWT.create(StaticInjectGinjector.class);

    assertNotNull(StaticClass.getName2());
  }

  public void testPublicStaticMethodInject() {
    GWT.create(StaticInjectGinjector.class);

    assertNotNull(StaticClass.getName3());
  }

  public void testPrivateStaticMethodInject() {
    GWT.create(StaticInjectGinjector.class);

    assertNotNull(StaticClass.getName4());
  }

  public void testNonRegisteredStaticInject() {
    GWT.create(StaticInjectGinjector.class);

    assertNull(DynamicClass.getName1());
    assertNull(DynamicClass.getName2());
  }

  public void testStaticGinjectorInjection() {
    GWT.create(StaticInjectGinjector.class);

    assertNotNull(StaticClass.getInjector());
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}
