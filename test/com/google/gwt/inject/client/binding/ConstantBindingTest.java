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
package com.google.gwt.inject.client.binding;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

public class ConstantBindingTest extends GWTTestCase {

  private FruitGinjector injector;

  @Override protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    injector = GWT.create(FruitGinjector.class);
  }

  public void testBoolean() {
    assertEquals(FruitGinModule.EATEN, injector.isEaten());
  }

  // TODO(schmitt):  Maybe fix this eventually.
  // Guice does not support byte constants.
  /*public void testByte() {
    assertEquals(FruitGinModule.ID, injector.getId());
  }*/

  public void testChar() {
    assertEquals(FruitGinModule.INITIAL, injector.getInital());
  }

  public void testDouble() {
    assertEquals(FruitGinModule.VOLUME, injector.getVolume());
  }

  public void testFloat() {
    assertEquals(FruitGinModule.WEIGHT, injector.getWeight());
  }

  public void testInt() {
    assertEquals(FruitGinModule.SEEDS, injector.getSeeds());
  }

  public void testLong() {
    assertEquals(FruitGinModule.WORMS, injector.getWorms());
  }

  public void testShort() {
    assertEquals(FruitGinModule.LEAVES, injector.getLeaves());
  }

  public void testString() {
    assertEquals(FruitGinModule.NAME, injector.getName());
  }

  public void testEnum() {
    assertEquals(FruitGinModule.COLOR, injector.getColor());
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}
