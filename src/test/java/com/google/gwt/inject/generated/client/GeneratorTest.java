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

package com.google.gwt.inject.generated.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class GeneratorTest extends GWTTestCase {

  public void testGenerated() {
    Car car = GWT.create(Car.class);
    car.initialize();

    assertEquals("9.0", car.getWindows());
  }

  public static abstract class Car implements Framework {
    private String windows;

    public String getWindows() {
      return windows;
    }

    @Inject
    public void setWindows(@Named("3") String windows) {
      this.windows = windows;
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.inject.generated.GeneratedTest";
  }
}
