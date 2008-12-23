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

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;

/**
 * Variant of {@link com.google.gwt.inject.client.MyApp} that uses field injection.
 */
public class MyFieldApp extends MyFieldSuper {
  static {
    // Real client code may not be able to run unless really in the browser.
    // To simulate this, freak out if this class is fully loaded in a non-client
    // scenario. This prevents reintroducing a bug (originally present) where
    // this class could be fully initialized at rebind time.
    if (!GWT.isClient()) {
      throw new IllegalStateException("attempt to initialize client class at rebind time!");
    }
  }

  @Inject private MyMessages msgs;
  @Inject private MyService service;

  public MyMessages getMsgs() {
    return msgs;
  }

  public MyService getService() {
    return service;
  }
}