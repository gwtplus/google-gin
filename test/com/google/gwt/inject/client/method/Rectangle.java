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

public class Rectangle {

  private int width;
  private int height;

  public int getWidth() {
    return width;
  }

  @Inject
  public void setWidth(@Named("width") int width) {
    this.width = width;
  }

  public int getHeight() {
    return height;
  }

  // TODO(schmitt):  Uncomment all tests currently disable once private
  // injection is implemented.
  // See issue http://code.google.com/p/google-gin/issues/detail?id=14

  /*@Inject
  private void setOtherHeight(@Named("height") int height) {
    this.height = height;
  }*/

  public static class Border {

    private String color;

    public String getColor() {
      return color;
    }

    @Inject
    public void setColor(@Named("color") String color) {
      this.color = color;
    }
  }
}
