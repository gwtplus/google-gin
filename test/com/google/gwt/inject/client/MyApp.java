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

import com.google.inject.Inject;
import com.google.gwt.core.client.GWT;

/**
 * A more complex object that we create from {@link MyAppInjector}.
 * 
 * @author bstoler@google.com (Brian Stoler)
*/
public class MyApp {
  static {
    // Real client code may not be able to run unless really in the browser.
    // To simulate this, freak out if this class is fully loaded in a non-client
    // scenario. This prevents reintroducing a bug (originally present) where
    // this class could be fully initialized at rebind time.
    if (!GWT.isClient()) {
      throw new IllegalStateException("attempt to initialize client class at rebind time!");
    }
  }

  private final SimpleObject simple;
  private final MyMessages msgs;
  private final MyService service;

  @Inject
  public MyApp(SimpleObject simple, MyMessages msgs, MyService service) {
    this.simple = simple;
    this.msgs = msgs;
    this.service = service;
  }

  public SimpleObject getSimple() {
    return simple;
  }

  public MyMessages getMsgs() {
    return msgs;
  }

  public MyService getService() {
    return service;
  }
}