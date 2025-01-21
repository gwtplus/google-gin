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

import com.google.gwt.inject.client.privatemanylevel.PrivateManyLevelTest.Drive;
import com.google.gwt.inject.client.privatemanylevel.PrivateManyLevelTest.Engine;
import com.google.gwt.inject.client.privatemanylevel.PrivateManyLevelTest.Transmission;
import com.google.inject.Inject;

import jakarta.inject.Provider;

public class Car {
  private final Engine engine;
  private final Transmission transmission;
  private final Drive driveline;
  
  @Inject
  public Car(Engine engine, Provider<Transmission> transmission, Provider<Drive> driveline) {
    this.engine = engine;
    this.transmission = transmission.get();
    this.driveline = driveline.get();
  }
  
  public Engine getEngine() {
    return engine;
  }
  
  public Transmission getTransmission() {
    return transmission;
  }
  
  public Drive getDriveline() {
    return driveline;
  }
}
