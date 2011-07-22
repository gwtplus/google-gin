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

package com.google.gwt.inject.client.gwtdotcreate;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.NoGinModules;
import com.google.gwt.junit.client.GWTTestCase;

public class CarTest extends GWTTestCase {

  public void testReplacement() {
    CarGinjector ginjector = GWT.create(CarGinjector.class);

    Car car = ginjector.getCar();
    assertTrue(car instanceof Beetle);
  }

  @NoGinModules
  interface CarGinjector extends Ginjector {
    Car getCar();
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.inject.client.gwtdotcreate.CarTest";
  }

  public static class Beetle implements Car {}

}
