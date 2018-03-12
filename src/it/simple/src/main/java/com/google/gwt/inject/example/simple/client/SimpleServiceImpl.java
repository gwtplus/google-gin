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

import com.google.inject.Inject;

/**
 * Implementation for
 * {@link com.google.gwt.inject.example.simple.client.SimpleService} that uses
 * a constants properties accessor to generate messages and errors.
 */
public class SimpleServiceImpl implements SimpleService {

  private final SimpleConstants messages;

  /**
   * Constructs a simple service implementation backed by a properties
   * accessor.
   *
   * @param messages properties accessor
   */
  @Inject
  public SimpleServiceImpl(SimpleConstants messages) {
    this.messages = messages;
  }

  public String getRandomMessage() {
    String[] msgs = messages.messages();
    return msgs[((int) Math.floor(msgs.length * Math.random()))];
  }

  public String getRandomError() {
    String[] errors = messages.errors();
    return errors[((int) Math.floor(errors.length * Math.random()))];
  }
}
