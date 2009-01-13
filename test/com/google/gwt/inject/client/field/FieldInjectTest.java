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
package com.google.gwt.inject.client.field;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

public class FieldInjectTest extends GWTTestCase {

  public void testSimpleInjection() {
    FruitGinjector injector = GWT.create(FruitGinjector.class);

    assertNotNull(injector.getTree().kind);
    assertNotNull(injector.getBasket().getTree().kind);
  }

  public void testPrivateInjection() {
    FruitGinjector injector = GWT.create(FruitGinjector.class);

    assertPrivateInjection(injector.getFruit());
    assertPrivateInjection(injector.getBasket().getFruit());
  }

  public void testSuperInjection() {
    FruitGinjector injector = GWT.create(FruitGinjector.class);

    assertSuperInjection(injector.getPear());
    assertSuperInjection(injector.getBasket().getPear());
  }

  public void testInnerClassInjection() {
    FruitGinjector injector = GWT.create(FruitGinjector.class);

    assertNotNull(injector.getWorm().getName());
    assertNotNull(injector.getBasket().getWorm().getName());
  }

  public void testMemberInject() {
    FruitGinjector injector = GWT.create(FruitGinjector.class);
    Fruit fruit = new Fruit();

    assertNull(fruit.getTree());
    injector.injectMembers(fruit);
    assertNotNull(fruit.getTree());
  }

  public void testSubclassMemberInject() {
    FruitGinjector injector = GWT.create(FruitGinjector.class);
    Pear pear = new Pear();

    assertNull(pear.getTree());
    injector.injectMembers(pear);
    assertNotNull(pear.getTree());
    assertNull(pear.getAlternativeColor());
    injector.injectPear(pear);
    assertNotNull(pear.getAlternativeColor());
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }

  private void assertPrivateInjection(Fruit fruit) {
    assertNotNull(fruit.getTree());
    assertNotNull(fruit.getColor());
  }

  private void assertSuperInjection(Pear pear) {
    assertPrivateInjection(pear);
    assertNotNull(pear.getAlternativeColor());
  }
}
