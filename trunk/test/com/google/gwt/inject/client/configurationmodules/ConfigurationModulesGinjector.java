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

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.inject.name.Named;

/**
 * Some modules are defined by the "com.google.gwt.inject.ginmodules" configuration
 * property in the GWT module.
 */
@GinModules(
    value={RegularModule.class, ConfigurationModulesTest.NestedRegularModule.class},
    properties={"ginmodules","extra.ginmodule"})
public interface ConfigurationModulesGinjector extends Ginjector {
  @Named("one") int getOne();
  @Named("two") int getTwo();
  @Named("three") int getThree();
  @Named("four") int getFour();
  @Named("five") int getFive();
}
