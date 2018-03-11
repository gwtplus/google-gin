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
package com.google.gwt.inject.client.gwtdotcreate;

import com.google.gwt.inject.client.MyMessages;
import com.google.gwt.inject.client.MyRemoteServiceAsync;
import com.google.inject.Inject;

/**
 */
public class MainImpl implements Main {
  private final MyMessages messages;
  private final MyConstants constants;
  private final MyRemoteServiceAsync remoteService;

  @Inject
  public MainImpl(MyMessages messages, MyConstants constants, MyRemoteServiceAsync remoteService) {
    this.messages = messages;
    this.constants = constants;
    this.remoteService = remoteService;
  }

  public MyMessages getMessages() {
    return messages;
  }

  public MyConstants getConstants() {
    return constants;
  }

  public MyRemoteServiceAsync getRemoteService() {
    return remoteService;
  }
}
