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

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

public class MethodInjectTest extends GWTTestCase {

  // TODO(schmitt):  Uncomment all tests currently disable once private
  // injection is implemented.
  // See issue http://code.google.com/p/google-gin/issues/detail?id=14

  public void testSimpleInject() {
    ShapeGinjector injector = GWT.create(ShapeGinjector.class);

    assertEquals(ShapeGinModule.WIDTH, injector.getRectangle().getWidth());
    assertEquals(ShapeGinModule.WIDTH, injector.getJigsaw().getRectangle().getWidth());
  }

  public void testPrivateInject() {
    ShapeGinjector injector = GWT.create(ShapeGinjector.class);
    Rectangle rectangle = injector.getRectangle();

    // assertEquals(ShapeGinModule.HEIGHT, rectangle.getHeight());
  }

  public void testSuperInject() {
    ShapeGinjector injector = GWT.create(ShapeGinjector.class);

    // assertEquals(ShapeGinModule.HEIGHT, injector.getThinRectangle().getHeight());
    // assertEquals(ShapeGinModule.HEIGHT, injector.getJigsaw().getThinRectangle().getHeight());
  }

  public void testInnerClassInject() {
    ShapeGinjector injector = GWT.create(ShapeGinjector.class);

    assertEquals(ShapeGinModule.COLOR, injector.getBorder().getColor());
    assertEquals(ShapeGinModule.COLOR, injector.getJigsaw().getBorder().getColor());
  }

  public void testOverrideInject() {
    ShapeGinjector injector = GWT.create(ShapeGinjector.class);

    assertOverrideInject(injector.getSquare());
    assertOverrideInject(injector.getJigsaw().getSquare());
  }

  public void testInterfaceInject() {
    ShapeGinjector injector = GWT.create(ShapeGinjector.class);

    assertInterfaceInject(injector.getCircle());
    assertInterfaceInject(injector.getJigsaw().getCircle());
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }

  private void assertOverrideInject(Square square) {
    // assertEquals(ShapeGinModule.HEIGHT, square.getHeight());
    assertEquals(0, square.getWidth());
    assertEquals(ShapeGinModule.OTHER_HEIGHT, square.getOtherHeight());
    assertEquals(ShapeGinModule.OTHER_WIDTH, square.getOtherWidth());
  }

  private void assertInterfaceInject(Circle circle) {
    assertNotNull(circle.getColor());
    assertEquals(ShapeGinModule.COLOR, circle.getColor());
  }
}
