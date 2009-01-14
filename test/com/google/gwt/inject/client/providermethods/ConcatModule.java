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

/**
 * Test module with provider methods.
 */
public class ConcatModule extends AbstractGinModule {
  protected void configure() {
  }

  @Provides
  @Named("all")
  String provideAll(@Named("Space") String space, @Named("a") String a,
      @Named("b") String b, @Named("c") String c, @Named("d") String d) {
    return a + space + b + space + c + space + d;
  }
}
