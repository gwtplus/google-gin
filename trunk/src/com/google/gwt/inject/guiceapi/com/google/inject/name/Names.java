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
package com.google.inject.name;

/**
 * Equivalent of Guice's Names type.
 * Used to keep the GWT compiler happy.
 */
public class Names {
  public static Named named(String name) { 
    throw new UnsupportedOperationException("You are executing Names.named() in GWT code. " +
        "GWT does not emulate enough of Java to make that work."); 
  }
}
