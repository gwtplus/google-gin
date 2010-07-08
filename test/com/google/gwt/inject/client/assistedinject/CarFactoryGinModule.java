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

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.gwt.inject.client.assistedinject.CarFactoryTest.*;

import java.util.Collections;
import java.util.Set;

public class CarFactoryGinModule extends AbstractGinModule {

  @Override
  protected void configure() {
    install(new GinFactoryModuleBuilder()
        .implement(Car.class, Names.named("german"), Beetle.class)
        .implement(Car.class, Names.named("american"), Mustang.class)
        .build(AnnotatedVersatileCarFactory.class));

    install(new GinFactoryModuleBuilder().build(MustangFactory.class));

    install(new GinFactoryModuleBuilder()
        .implement(Car.class, Mustang.class)
        .build(ColoredCarFactory.class));

    install(new GinFactoryModuleBuilder().build(VersatileCarFactory.class));

    bindConstant().annotatedWith(Names.named("horsePower")).to(300);
    bindConstant().annotatedWith(Names.named("modelYear")).to(1998);
    install(new GinFactoryModuleBuilder().build(CamaroFactory.class));

    install(new GinFactoryModuleBuilder().build(CorvetteFactory.class));

    install(new GinFactoryModuleBuilder().build(AlternateCorvetteFactory.class));

    install(new GinFactoryModuleBuilder()
        .implement(Car.class, Beetle.class)
        .build(Key.get(ColoredCarFactory.class, Names.named("beetle"))));
    install(new GinFactoryModuleBuilder()
        .implement(Car.class, Mustang.class)
        .build(Key.get(ColoredCarFactory.class, Names.named("mustang"))));

    install(new GinFactoryModuleBuilder()
        .implement(Car.class, Porsche.class)
        .build(Key.get(ColoredCarFactory.class, Names.named("porsche"))));

    install(new GinFactoryModuleBuilder()
        .implement(Car.class, FireBird.class)
        .build(Key.get(ColoredCarFactory.class, Names.named("fireBird"))));

    install(new GinFactoryModuleBuilder()
        .implement(Car.class, DeLorean.class)
        .build(Key.get(ColoredCarFactory.class, Names.named("deLorean"))));

    install(new GinFactoryModuleBuilder()
        .implement(Car.class, Prius.class)
        .build(Key.get(ColoredCarFactory.class, Names.named("prius"))));

    install(new GinFactoryModuleBuilder().build(BmwFactory.class));

    install(new GinFactoryModuleBuilder().build(AudiFactory.class));

    bind(InsuranceCompany.class).to(GreatWest.class);

    install(new GinFactoryModuleBuilder().build(HummerFactory.class));

    install(new GinFactoryModuleBuilder().build(ComplicatedMustangFactory.class));

    install(new GinFactoryModuleBuilder()
        .build(new TypeLiteral<ParameterizableFactory<String>>(){}));
  }

  @Provides
  Double provideDouble() {
    return 5.61;
  }

  @Provides
  Float provideFloat() {
    return 10.4f;
  }

  @Provides
  Integer provideInteger() {
    return 911;
  }

  @Provides
  String provideString() {
    return "fooBar";
  }

  @Provides
  Set<String> provideStringSet() {
    return Collections.singleton("barFoo");
  }

  @Provides
  Set<Integer> provideIntegerSet() {
    return Collections.singleton(54);
  }
}
