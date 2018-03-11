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
package com.google.gwt.inject.client.configurationmodules;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.name.Names;

public class ConfigurationModulesTest extends GWTTestCase {

  public static class NestedRegularModule extends AbstractGinModule {
    protected void configure() {
      bindConstant().annotatedWith(Names.named("two")).to(2);
    }
  }

  public static class NestedConfigurationModuleA extends AbstractGinModule {
    protected void configure() {
      bindConstant().annotatedWith(Names.named("four")).to(4);
    }
  }

  public static class NestedConfigurationModuleB extends AbstractGinModule {
    protected void configure() {
      bindConstant().annotatedWith(Names.named("five")).to(5);
    }
  }

  public void testPropertyModules() throws Exception {
    ConfigurationModulesGinjector ginjector = GWT.create(ConfigurationModulesGinjector.class);
    assertEquals(1, ginjector.getOne());
    assertEquals(2, ginjector.getTwo());
    assertEquals(3, ginjector.getThree());
    assertEquals(4, ginjector.getFour());
    assertEquals(5, ginjector.getFive());
  }

  public String getModuleName() {
    return "com.google.gwt.inject.client.configurationmodules.ConfigurationModulesTest";
  }
}
