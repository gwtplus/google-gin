// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.gwt.inject.client.binding;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.name.Names;

public class FruitGinModule extends AbstractGinModule {

  public static final boolean EATEN = false;

  // TODO(schmitt):  Maybe fix this eventually.
  // Guice does not support byte constants.
  /*public static final byte ID = 0x41;*/
  public static final char INITIAL = 'a';
  public static final double VOLUME = 20.2;
  public static final float WEIGHT = 200.5f;
  public static final int SEEDS = 4;
  public static final long WORMS = 1;
  public static final short LEAVES = 2;
  public static final String NAME = "Apple";
  public static final Color COLOR = Color.Red;

  protected void configure() {
    bindConstant().annotatedWith(Names.named("eaten")).to(EATEN);
    /*bindConstant().annotatedWith(Names.named("id")).to(ID);*/
    bindConstant().annotatedWith(Names.named("initial")).to(INITIAL);
    bindConstant().annotatedWith(Names.named("volume")).to(VOLUME);
    bindConstant().annotatedWith(Names.named("weight")).to(WEIGHT);
    bindConstant().annotatedWith(Names.named("seeds")).to(SEEDS);
    bindConstant().annotatedWith(Names.named("worms")).to(WORMS);
    bindConstant().annotatedWith(Names.named("leaves")).to(LEAVES);
    bindConstant().annotatedWith(Names.named("name")).to(NAME);
    bindConstant().annotatedWith(Names.named("color")).to(COLOR);
  }
}
