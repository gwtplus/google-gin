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

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.SimpleObject;
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

  @Named("family") Fruit.Family getFamily();

  FruitGinjector getGinjector();

  Plant getPlant();

  void injectMembers(Bee bee);

  // Random method causing this ginjector compilation to fail if not all
  // injectMembers methods are processed.
  void injectMembers(SimpleObject foo);
}
