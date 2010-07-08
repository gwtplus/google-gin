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

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.binding.Color;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

import java.util.Collection;
import java.util.Set;

public class CarFactoryTest extends GWTTestCase {

  // FactoryProvider2 allows assisted parameters to be converted into
  // providers. AFAIK the new FactoryModuleBuilder doesn't allow that, nor can
  // I see an application.

  public void testAnnotatedInjectedParameters() {
    CarFactoryGinjector ginjector = GWT.create(CarFactoryGinjector.class);

    CamaroFactory factory = ginjector.getCamaroFactory();

    Camaro greenCamaro = factory.create(Color.Green);
    assertEquals(Color.Green, greenCamaro.color);
    assertEquals(1998, greenCamaro.modelYear);
    assertEquals(300, greenCamaro.horsePower);

    Camaro redCamaro = factory.create(Color.Red);
    assertEquals(Color.Red, redCamaro.color);
    assertEquals(1998, redCamaro.modelYear);
    assertEquals(300, redCamaro.horsePower);
  }

  public void testSelectInjectConstructor() {
    CarFactoryGinjector ginjector = GWT.create(CarFactoryGinjector.class);

    CorvetteFactory factory = ginjector.getCorvetteFactory();

    Corvette corvette = factory.create(Color.Red, true);
    assertEquals(Color.Red, corvette.color);
    assertTrue(corvette.isConvertible);
    assertEquals(10.4f, corvette.maxMph);
  }

  public void testSwappedParameters() {
    CarFactoryGinjector ginjector = GWT.create(CarFactoryGinjector.class);

    AlternateCorvetteFactory factory = ginjector.getAlternateCorvetteFactory();

    Corvette corvette = factory.create(true, Color.Green);
    assertEquals(Color.Green, corvette.color);
    assertTrue(corvette.isConvertible);
    assertEquals(10.4f, corvette.maxMph);
  }

  public void testNamedFactories() {
    CarFactoryGinjector ginjector = GWT.create(CarFactoryGinjector.class);

    ColoredCarFactory beetleFactory = ginjector.getNamedBeetleFactory();

    Beetle beetle = (Beetle) beetleFactory.create(Color.Red);
    assertEquals(Color.Red, beetle.color);
    assertEquals(5.61, beetle.engineSize);

    ColoredCarFactory mustangFactory = ginjector.getNamedMustangFactory();

    Mustang mustang = (Mustang) mustangFactory.create(Color.Yellow);
    assertEquals(Color.Yellow, mustang.color);
    assertEquals(5.61, mustang.engineSize);
  }

  public void testMethodAndFieldInjection() {
    CarFactoryGinjector ginjector = GWT.create(CarFactoryGinjector.class);

    ColoredCarFactory factory = ginjector.getNamedPorscheFactory();

    Porsche porsche = (Porsche) factory.create(Color.Green);
    assertEquals("fooBar", porsche.name);
    assertEquals(Color.Green, porsche.color);
    assertEquals(911, porsche.model);
    assertEquals(5.61, porsche.price);
  }

  public void testProviderInjection() {
    CarFactoryGinjector ginjector = GWT.create(CarFactoryGinjector.class);

    ColoredCarFactory factory = ginjector.getNamedFireBirdFactory();

    FireBird fireBird = (FireBird) factory.create(Color.Yellow);
    assertEquals(Color.Yellow, fireBird.color);
    assertEquals("fooBar", fireBird.modifiersProvider.get());
  }

  public void testParameterizedInjection() {
    CarFactoryGinjector ginjector = GWT.create(CarFactoryGinjector.class);

    ColoredCarFactory factory = ginjector.getNamedDeLoreanFactory();

    DeLorean deLorean = (DeLorean) factory.create(Color.Green);
    assertEquals(Color.Green, deLorean.color);
    assertEquals("barFoo", deLorean.features.iterator().next());
    assertEquals((Integer) 54, deLorean.featureActivationSpeeds.iterator().next());
  }

  public void testPrivateConstructorInject() {
    CarFactoryGinjector ginjector = GWT.create(CarFactoryGinjector.class);

    ColoredCarFactory factory = ginjector.getNamedPriusFactory();

    Prius prius = (Prius) factory.create(Color.Red);
    assertEquals(Color.Red, prius.color);
    
  }

  public void testNamedAssistedAnnotations() {
    CarFactoryGinjector ginjector = GWT.create(CarFactoryGinjector.class);

    BmwFactory factory = ginjector.getBmwFactory();
    M3 m3 = factory.paintOutsideIn(Color.Red, Color.Yellow);
    assertEquals(Color.Red, m3.exterior);
    assertEquals(Color.Yellow, m3.interior);

    M3 m3insideOut = factory.paintInsideOut(Color.Red, Color.Yellow);
    assertEquals(Color.Red, m3insideOut.interior);
    assertEquals(Color.Yellow, m3insideOut.exterior);
  }

  public void testAssistedInjectConstructorSelection() {
    CarFactoryGinjector ginjector = GWT.create(CarFactoryGinjector.class);

    AudiFactory factory = ginjector.getAudiFactory();

    A4 a4basic = factory.createBasic(Color.Green);
    assertEquals(Color.Green, a4basic.color);
    assertEquals("none", a4basic.modification);
    assertEquals(5.61, a4basic.speed);

    A4 a4modified = factory.createModified(Color.Red, "longer");
    assertEquals(Color.Red, a4modified.color);
    assertEquals("longer", a4modified.modification);
    assertEquals(5.61, a4modified.speed);
  }

  public void testNoBindingAssistedInject() {
    CarFactoryGinjector ginjector = GWT.create(CarFactoryGinjector.class);

    MustangFactory factory = ginjector.getMustangFactory();

    Mustang mustang = factory.create(Color.Green);
    assertEquals(Color.Green, mustang.color);
    assertEquals(5.61, mustang.engineSize);
  }

  public void testConfigureAnnotatedReturnValue() {
    CarFactoryGinjector ginjector = GWT.create(CarFactoryGinjector.class);
    AnnotatedVersatileCarFactory factory = ginjector.getAnnotatedVersatileCarFactory();

    assertTrue(factory.getGermanCar(Color.Green) instanceof Beetle);
    assertTrue(factory.getAmericanCar(Color.Red) instanceof Mustang);
  }

  public void testBindingAssistedInject() {
    CarFactoryGinjector ginjector = GWT.create(CarFactoryGinjector.class);

    ColoredCarFactory factory = ginjector.getColoredCarFactory();

    Mustang mustang = (Mustang) factory.create(Color.Red);
    assertEquals(Color.Red, mustang.color);
  }

  public void testMultipleReturnTypes() {
    CarFactoryGinjector ginjector = GWT.create(CarFactoryGinjector.class);

    VersatileCarFactory factory = ginjector.getVersatileCarFactory();

    Mustang mustang = factory.getMustang(Color.Yellow);
    assertEquals(Color.Yellow, mustang.color);
    assertEquals(5.61, mustang.engineSize);

    Beetle beetle = factory.getBeetle(Color.Red);
    assertEquals(Color.Red, beetle.color);
    assertEquals(5.61, beetle.engineSize);
  }

  public void testBoundTypeInjection() {
    CarFactoryGinjector ginjector = GWT.create(CarFactoryGinjector.class);

    HummerFactory factory = ginjector.getHummerFactory();
    Hummer hummer = factory.create(5000);

    assertEquals(5000, hummer.size);
    assertTrue(hummer.insurance instanceof GreatWest);
  }

  public void testIgnoredParameterInject() {
    CarFactoryGinjector ginjector = GWT.create(CarFactoryGinjector.class);

    ComplicatedMustangFactory factory = ginjector.getComplicatedMustangFactory();
    Mustang mustang = factory.create(Color.Red, false);
    assertEquals(Color.Red, mustang.color);
    assertEquals(5.61, mustang.engineSize);
  }

  public void testParameterizedReturnValue() {
    CarFactoryGinjector ginjector = GWT.create(CarFactoryGinjector.class);

    ParameterizableFactory<String> factory = ginjector.getParameterizableFactoryForString();
    VersatileCar<String> car = factory.create(Color.Green);
    assertEquals(Color.Green, car.color);
    assertEquals("barFoo", car.versatility.iterator().next());
  }

  // ----------- Types --------------------------------------------------------

  public interface Car {}

  public interface ColoredCarFactory {
    Car create(Color color);
  }

  public interface CamaroFactory {
    Camaro create(Color color);
  }

  public interface CorvetteFactory {
    Corvette create(Color color, boolean isConvertible);
  }

  public interface AlternateCorvetteFactory {
    Corvette create(boolean isConvertible, Color color);
  }

  public interface MustangFactory {
    Mustang create(Color color);
  }

  public interface ComplicatedMustangFactory {
    Mustang create(Color color, boolean stripes);
  }

  public interface VersatileCarFactory {
    Mustang getMustang(Color color);
    Beetle getBeetle(Color color);
  }

  public interface BmwFactory {
    M3 paintOutsideIn(@Assisted("exterior") Color exterior, @Assisted("interior") Color interior);
    M3 paintInsideOut(@Assisted("interior") Color exterior, @Assisted("exterior") Color interior);
  }

  public interface AudiFactory {
    A4 createBasic(Color color);
    A4 createModified(Color color, String modifications);
  }

  public interface HummerFactory {
    Hummer create(int size);
  }

  public interface AnnotatedVersatileCarFactory {
    @Named("german") Car getGermanCar(Color color);
    @Named("american") Car getAmericanCar(Color color);
  }

  public interface ParameterizableFactory<T> {
    VersatileCar<T> create(Color color);
  }
  
  public static class Mustang implements Car {
    private final double engineSize;
    private final Color color;

    @Inject
    public Mustang(double engineSize, @Assisted Color color) {
      this.engineSize = engineSize;
      this.color = color;
    }
  }

  public static class Beetle implements Car {
    private final Color color;
    private final double engineSize;
    @Inject
    public Beetle(@Assisted Color color, double engineSize) {
      this.color = color;
      this.engineSize = engineSize;
    }
  }

  public static class Camaro implements Car {
    private final int horsePower;
    private final int modelYear;
    private final Color color;

    @Inject
    public Camaro(@Named("horsePower") int horsePower, @Named("modelYear") int modelYear,
        @Assisted Color color) {
      this.horsePower = horsePower;
      this.modelYear = modelYear;
      this.color = color;
    }
  }

  public static class Corvette implements Car {
    private boolean isConvertible;
    private Color color;
    private float maxMph;

    public Corvette(Color color, boolean isConvertible) {
      throw new IllegalStateException("Not an @Inject constructor");
    }

    @Inject
    public Corvette(@Assisted Color color, Float maxMph, @Assisted boolean isConvertible) {
      this.isConvertible = isConvertible;
      this.color = color;
      this.maxMph = maxMph;
    }
  }

  public static class Porsche implements Car {
    private final Color color;
    private final double price;
    private @Inject String name;
    private int model;

    @Inject
    public Porsche(@Assisted Color color, double price) {
      this.color = color;
      this.price = price;
    }

    @Inject void setModel(int model) {
      this.model = model;
    }
  }

  public static class FireBird implements Car {
    private final Provider<String> modifiersProvider;
    private final Color color;

    @Inject
    public FireBird(Provider<String> modifiersProvider, @Assisted Color color) {
      this.modifiersProvider = modifiersProvider;
      this.color = color;
    }
  }

  public static class DeLorean implements Car {
    private final Set<String> features;
    private final Set<Integer> featureActivationSpeeds;
    private final Color color;

    @Inject
    public DeLorean(Set<String> extraFeatures, Set<Integer> featureActivationSpeeds,
        @Assisted Color color) {
      this.features = extraFeatures;
      this.featureActivationSpeeds = featureActivationSpeeds;
      this.color = color;
    }
  }

  public static class Prius implements Car {
    final Color color;

    @Inject
    private Prius(@Assisted Color color) {
      this.color = color;
    }
  }

  public static class M3 implements Car {
    final Color interior;
    final Color exterior;

    @Inject
    public M3(@Assisted("exterior") Color exterior, @Assisted("interior") Color interior) {
      this.interior = interior;
      this.exterior = exterior;
    }
  }

  public static class A4 implements Car {
    final Color color;
    final double speed;
    final String modification;

    @AssistedInject
    public A4(@Assisted Color color, Double speed) {
      this.color = color;
      this.speed = speed;
      this.modification = "none";
    }

    @AssistedInject
    public A4(@Assisted Color color, @Assisted String modification, Double speed) {
      this.color = color;
      this.speed = speed;
      this.modification = modification;
    }
  }

  interface InsuranceCompany {}

  public static class GreatWest implements InsuranceCompany {}

  public static class Hummer {
    final int size;
    final InsuranceCompany insurance;

    @Inject
    public Hummer(@Assisted int size, InsuranceCompany insurance) {
      this.size = size;
      this.insurance = insurance;
    }
  }

  public static class VersatileCar<T> {
    final Color color;
    final Set<T> versatility;

    @Inject
    public VersatileCar(@Assisted Color color, Set<T> versatility) {
      this.color = color;
      this.versatility = versatility;
    }
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}
