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

public class Jigsaw {

  private final Circle circle;
  private final Rectangle rectangle;
  private final Square square;
  private final ThinRectangle thinRectangle;
  private final Rectangle.Border border;

  @Inject
  public Jigsaw(Circle circle, Rectangle rectangle, Square square, ThinRectangle thinRectangle,
      Rectangle.Border border) {
    this.circle = circle;
    this.rectangle = rectangle;
    this.square = square;
    this.thinRectangle = thinRectangle;
    this.border = border;
  }

  public Circle getCircle() {
    return circle;
  }

  public Rectangle getRectangle() {
    return rectangle;
  }

  public Square getSquare() {
    return square;
  }

  public ThinRectangle getThinRectangle() {
    return thinRectangle;
  }

  public Rectangle.Border getBorder() {
    return border;
  }
}
