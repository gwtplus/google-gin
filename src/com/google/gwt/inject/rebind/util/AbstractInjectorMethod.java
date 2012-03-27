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
package com.google.gwt.inject.rebind.util;

/**
 * Common base class for injector methods.
 */
public abstract class AbstractInjectorMethod implements InjectorMethod {
  private final boolean isNative;
  private final String methodSignature;
  private final String packageName;

  protected AbstractInjectorMethod(boolean isNative, String methodSignature, String packageName) {
    this.isNative = isNative;
    this.methodSignature = methodSignature;
    this.packageName = packageName;
  }

  public boolean isNative() {
    return isNative;
  }

  public String getMethodSignature() {
    return methodSignature;
  }

  public String getPackageName() {
    return packageName;
  }
}
