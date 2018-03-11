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

package com.google.gwt.inject.example.simple.client;

import com.google.gwt.inject.client.AsyncProvider;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

/**
 * A ginjector that uses the
 * {@link com.google.gwt.inject.example.simple.client.SimpleGinModule} to
 * construct {@link com.google.gwt.inject.example.simple.client.SimpleWidget}s.
 *
 */
@GinModules(SimpleGinModule.class)
public interface SimpleGinjector extends Ginjector {

  SimpleWidget getSimpleWidget();
  
  AsyncProvider<SimpleAsyncWidget> getAsyncWidget();
}
