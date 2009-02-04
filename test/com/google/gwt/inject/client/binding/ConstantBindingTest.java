// Copyright 2009 Google Inc. All Rights Reserved.

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
