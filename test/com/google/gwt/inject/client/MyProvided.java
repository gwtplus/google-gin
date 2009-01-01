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
package com.google.gwt.inject.client;

/**
 * This is a simple test object to prove that you can write a provider
 * that calls an existing static method. Note that there is no reason to write
 * new code with manual singletons like this.
 */
public class MyProvided {
  private static final MyProvided INSTANCE = new MyProvided();

  public static MyProvided getInstance() {
    return INSTANCE;
  }

  private MyProvided() {
  }
}
