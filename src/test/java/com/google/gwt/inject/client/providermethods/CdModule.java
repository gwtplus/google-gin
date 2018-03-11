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
package com.google.gwt.inject.client.providermethods;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

/**
 * Test module with provider methods.
 */
public class CdModule extends AbstractGinModule {
  static final String SPACE_VALUE = " ";
  static final String C_VALUE = "Brian";
  static final String D_VALUE = "Robbie";

  protected void configure() {
    // A non-provider method binding to show that they interoperate
    bindConstant().annotatedWith(Names.named("Space")).to(SPACE_VALUE);
  }

  @Provides
  @Named("c")
  private String provideC() {
    return C_VALUE;
  }

  @Provides
  @Named("d")
  private String provideD() {
    return D_VALUE;
  }
}
