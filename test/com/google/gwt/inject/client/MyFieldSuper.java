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

/**
 * Variant of {@link com.google.gwt.inject.client.MyApp} that uses field
 * injection.  This class serves as test for the inclusion of superclasses in
 * field injection.
 */
public abstract class MyFieldSuper {

  @Inject private SimpleObject simple;

  public SimpleObject getSimple() {
    return simple;
  }

  public abstract MyMessages getMsgs();

  public abstract MyService getService();
}