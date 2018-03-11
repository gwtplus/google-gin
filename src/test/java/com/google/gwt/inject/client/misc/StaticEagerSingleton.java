/*
 * Copyright 2012 Google Inc.
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

import com.google.gwt.inject.client.misc.subpackage.StaticSubEagerSingleton;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Used to verify that eager singletons are injected after static injection
 * takes place.
 */
public class StaticEagerSingleton {
  @Inject @Named("bar") private static String bar;

  private final String barCopy;
  private final String fooCopy;

  public StaticEagerSingleton() {
    barCopy = bar;
    fooCopy = StaticSubEagerSingleton.getFooStatic();
  }

  public String getBarCopy() {
    return barCopy;
  }

  public String getFooCopy() {
    return fooCopy;
  }

  public static String getBarStatic() {
    return bar;
  }

  static void resetBar() {
    bar = null;
  }
}
