/*
 * Copyright 2010 Google Inc.
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

package com.google.gwt.inject.client.assistedinject;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.assistedinject.CarFactoryTest.*;
import com.google.inject.name.Named;

@GinModules(CarFactoryGinModule.class)
public interface CarFactoryGinjector extends Ginjector {

  AnnotatedVersatileCarFactory getAnnotatedVersatileCarFactory();

  MustangFactory getMustangFactory();

  ColoredCarFactory getColoredCarFactory();

  VersatileCarFactory getVersatileCarFactory();

  CamaroFactory getCamaroFactory();

  CorvetteFactory getCorvetteFactory();

  AlternateCorvetteFactory getAlternateCorvetteFactory();

  @Named("beetle") ColoredCarFactory getNamedBeetleFactory();

  @Named("mustang") ColoredCarFactory getNamedMustangFactory();

  @Named("porsche") ColoredCarFactory getNamedPorscheFactory();

  @Named("fireBird") ColoredCarFactory getNamedFireBirdFactory();

  @Named("deLorean") ColoredCarFactory getNamedDeLoreanFactory();

  @Named("prius") ColoredCarFactory getNamedPriusFactory();

  BmwFactory getBmwFactory();

  AudiFactory getAudiFactory();

  HummerFactory getHummerFactory();

  ComplicatedMustangFactory getComplicatedMustangFactory();
}
