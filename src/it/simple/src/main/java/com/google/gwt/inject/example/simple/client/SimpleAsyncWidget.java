/*
 * Copyright 2010 Google Inc.
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
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;

/**
 * A Simple widget that gets created by {@link AsyncProvider}
 * 
 */
public class SimpleAsyncWidget {

  private final SimpleMessages messages;
  private static int instanceCounter = 0;
  
  @Inject
  public SimpleAsyncWidget(SimpleMessages messages) {
    this.messages = messages;
    instanceCounter++;
  }
  
  public void showMessage() {
    Window.alert(messages.messageTemplate("Hello Async! from instance " + instanceCounter));
  }
}