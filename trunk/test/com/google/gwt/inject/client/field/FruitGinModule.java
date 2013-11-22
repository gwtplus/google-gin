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
package com.google.gwt.inject.client.field;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.name.Names;

public class FruitGinModule extends AbstractGinModule {

  private static final String TREE_KIND = "cherry";
  private static final String COLOR = "yellow";
  private static final String ALTERNATIVE_COLOR = "green";
  private static final String WORM_NAME = "Max";

  protected void configure() {
    bindConstant().annotatedWith(Names.named("kind")).to(TREE_KIND);
    bindConstant().annotatedWith(Names.named("color")).to(COLOR);
    bindConstant().annotatedWith(Names.named("alternativeColor")).to(ALTERNATIVE_COLOR);
    bindConstant().annotatedWith(Names.named("wormName")).to(WORM_NAME);
  }
}