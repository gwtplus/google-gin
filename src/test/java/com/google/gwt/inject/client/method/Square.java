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
package com.google.gwt.inject.client.method;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class Square extends Rectangle {

  private int otherHeight;
  private int otherWidth;

  @Inject
  public void setHeight(@Named("otherHeight") int otherHeight) {
    this.otherHeight = otherHeight;
  }

  public int getOtherHeight() {
    return otherHeight;
  }

  @Inject
  public void setWidth(@Named("otherWidth") int width) {
    this.otherWidth = width;
  }

  public int getOtherWidth() {
    return otherWidth;
  }
}
