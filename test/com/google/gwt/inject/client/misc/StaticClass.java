/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.inject.client.misc;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class StaticClass {

  public static @Inject @Named("bar") String name1 = null;

  private static @Inject @Named("bar") String name2 = null;

  private static String name3 = null;

  private static String name4 = null;

  private static @Inject StaticInjectGinjector injector;

  public static String getName1() {
    return name1;
  }

  public static String getName2() {
    return name2;
  }

  public static String getName3() {
    return name3;
  }

  @Inject
  public static void setName3(@Named("bar") String name3) {
    StaticClass.name3 = name3;
  }

  public static String getName4() {
    return name4;
  }

  public static StaticInjectGinjector getInjector() {
    return injector;
  }

  @Inject
  private static void setName4(@Named("bar") String name4) {
    StaticClass.name4 = name4;
  }
}
