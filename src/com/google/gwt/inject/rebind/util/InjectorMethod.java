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

import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.gwt.inject.rebind.util.NameGenerator;

/**
 * A method that will be written to an injector implementation.
 */
public interface InjectorMethod {
  /**
   * Returns whether this is a native method.
   */
  boolean isNative();

  /**
   * Returns the signature of the method (e.g., "public void foo()")
   */
  String getMethodSignature();

  /**
   * Returns the body of the method in the given context (e.g., "return this;")
   */
  String getMethodBody(InjectorWriteContext context) throws NoSourceNameException;

  /** Get the name of the package in which this method should be created. */
  String getPackageName();
}
