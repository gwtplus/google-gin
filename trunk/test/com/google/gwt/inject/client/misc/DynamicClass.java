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

public class DynamicClass {

  private static @Inject @Named("bar") String name1 = null;

  private static String name2 = null;

  public static String getName1() {
    return name1;
  }

  public static String getName2() {
    return name2;
  }

  @Inject
  public static void setName2(@Named("bar") String name2) {
    DynamicClass.name2 = name2;
  }
}
