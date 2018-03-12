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

import com.google.gwt.inject.client.AbstractGinModule;

/**
 * This gin module binds an implementation for the
 * {@link com.google.gwt.inject.example.simple.client.SimpleService} used in
 * this example application. Note that we don't have to bind implementations
 * for {@link com.google.gwt.inject.example.simple.client.SimpleConstants} and
 * {@link com.google.gwt.inject.example.simple.client.SimpleMessages} - they
 * are constructed by Gin through GWT.create.
 */
public class SimpleGinModule extends AbstractGinModule {

  protected void configure() {
    bind(SimpleService.class).to(SimpleServiceImpl.class);
  }
}
