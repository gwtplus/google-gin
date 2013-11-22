/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.inject.client.privatemanylevel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.PrivateGinModule;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import javax.inject.Provider;

public class PrivateManyLevelTest extends GWTTestCase {

  public void testPrivateManyLevelModules() {
    CarGinjector ginjector = GWT.create(CarGinjector.class);
    Car redCar = ginjector.getRedCar();
    Car greenCar = ginjector.getGreenCar();
    
    assertTrue(redCar.getEngine() instanceof DieselEngine);
    assertTrue(greenCar.getEngine() instanceof HybridEngine);
    assertTrue(greenCar.getTransmission() instanceof AutomaticTransmission);
    assertTrue(redCar.getTransmission() instanceof AutomaticTransmission);
    assertSame(greenCar.getTransmission(), redCar.getTransmission());
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
  
  @GinModules({PrivateManyLevelModule.class, StereoModule.class, CarsModule.class})
  public interface CarGinjector extends Ginjector {
    @Red Car getRedCar();
    @Green Car getGreenCar();
  }

  static class PrivateManyLevelModule extends AbstractGinModule {
    @Override
    protected void configure() {
      bind(Drive.class).to(FourWheelDrive.class);
    }
  }
  
  static class CarsModule extends PrivateGinModule {
    @Override
    protected void configure() {
      bind(Transmission.class).to(AutomaticTransmission.class).in(Singleton.class);
      install(new GreenCarModule());
      install(new RedCarModule());
      expose(Car.class).annotatedWith(Red.class);
      expose(Car.class).annotatedWith(Green.class);
    }
  }
  
  static class StereoModule extends PrivateGinModule {
    @Override
    protected void configure() {
      expose(Stereo.class);
    }
    
    @Provides @Named("bose")
    public Speaker provideSpeaker(BoseSpeakers bose) {
      return bose;
    }
    
    @Provides
    public Stereo provideStereo(Radio radio, @Named("bose") Provider<Speaker> speakers) {
      return new Stereo(radio, speakers.get());
    }
  }
  
  static class GreenCarModule extends PrivateGinModule {
    @Override
    protected void configure() {
      bind(Car.class).annotatedWith(Green.class).to(Car.class);
      expose(Car.class).annotatedWith(Green.class);
      
      bind(Engine.class).to(HybridEngine.class);
    }
  }
  
  static class RedCarModule extends PrivateGinModule {    
    @Override
    protected void configure() {
      bind(Car.class).annotatedWith(Red.class).to(Car.class);
      expose(Car.class).annotatedWith(Red.class);
      
      bind(Engine.class).to(DieselEngine.class);
    }
  }
  
  // Types/interface used for the test
  interface Transmission {}
  public static class AutomaticTransmission implements Transmission {}
  public static class ManualTransmission implements Transmission {}
  
  interface Engine {}
  public static class DieselEngine implements Engine {}
  public static class HybridEngine implements Engine {}
  
  interface Drive {}
  public static class FourWheelDrive implements Drive {}
  
  interface Speaker {}
  public static class BoseSpeakers implements Speaker {}
  
  public static class Radio {}
  
  public static class Stereo {
    private final Speaker speaker;
    private final Radio radio;

    @Inject
    public Stereo(Radio radio, Speaker speaker) {
      this.radio = radio;
      this.speaker = speaker;
    }
    
    public Speaker getSpeaker() {
      return speaker;
    }
    
    public Radio getRadio() {
      return radio;
    }
  }
}
