// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.gwt.inject.client.binding;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.inject.name.Named;

@GinModules(FruitGinModule.class)
public interface FruitGinjector extends Ginjector {

  @Named("seeds") int getSeeds();

  @Named("weight") float getWeight();

  @Named("volume") double getVolume();

  @Named("worms") long getWorms();

  @Named("initial") char getInital();

  // TODO(schmitt):  Maybe fix this eventually.
  // Guice does not support byte constants.
  /*@Named("id") byte getId();*/

  @Named("leaves") short getLeaves();

  @Named("eaten") boolean isEaten();

  @Named("name") String getName();

  @Named("color") Color getColor();
}
