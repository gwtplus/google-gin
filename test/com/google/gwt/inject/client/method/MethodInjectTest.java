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
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.CreationException;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class MethodInjectTest extends GWTTestCase {

  public void testSimpleInject() {
    ShapeGinjector injector = GWT.create(ShapeGinjector.class);

    assertEquals(ShapeGinModule.WIDTH, injector.getRectangle().getWidth());
    assertEquals(ShapeGinModule.WIDTH, injector.getJigsaw().getRectangle().getWidth());
  }

  public void testPrivateInject() {
    ShapeGinjector injector = GWT.create(ShapeGinjector.class);
    Rectangle rectangle = injector.getRectangle();

    assertEquals(ShapeGinModule.HEIGHT, rectangle.getHeight());
  }

  public void testSuperInject() {
    ShapeGinjector injector = GWT.create(ShapeGinjector.class);

    assertEquals(ShapeGinModule.HEIGHT, injector.getThinRectangle().getHeight());
    assertEquals(ShapeGinModule.HEIGHT, injector.getJigsaw().getThinRectangle().getHeight());
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

  public void testMemberInject() {
    ShapeGinjector injector = GWT.create(ShapeGinjector.class);
    Rectangle rectangle = new Rectangle();

    assertEquals(0, rectangle.getHeight());
    injector.injectMembers(rectangle);
    assertEquals(ShapeGinModule.WIDTH, rectangle.getWidth());
  }

  public void testSubclassMemberInject() {
    ShapeGinjector injector = GWT.create(ShapeGinjector.class);
    Square square = new Square();

    assertEquals(0, square.getHeight());

    // Note that this will be filled as if it were a plain rectangle!
    injector.injectMembers(square);
    assertEquals(ShapeGinModule.HEIGHT, square.getHeight());
    assertEquals(0, square.getOtherHeight());

    injector.injectSquare(square);
    assertEquals(ShapeGinModule.OTHER_HEIGHT, square.getOtherHeight());
  }

  public void testMemberInjectWithForwarding() {
    ForwardingGinjector ginjector = GWT.create(ForwardingGinjector.class);
    Forwarding forwarded = ginjector.getForwarding();
    Forwarding forwarding = new Forwarding();
    ginjector.injectMembers(forwarding);
    assertEquals("red", forwarding.value);
    assertEquals("blue", forwarded.value);
  }

  public void testNoArgsInject() {
    ShapeGinjector injector = GWT.create(ShapeGinjector.class);
    Triangle triangle = injector.getTriangle();
    assertEquals(ShapeGinModule.WIDTH, triangle.getWidth());
    assertTrue(triangle.isMethodInitialized());
  }

  public void testThrowingConstructor() {
    DangerousGinjector ginjector = GWT.create(DangerousGinjector.class);

    try {
      ginjector.getDangerous();
      fail("Expected CreationException.");
    } catch (CreationException expected) {
      // Good.
    }
  }

  public void testThrowingMethod() {
    DangerousGinjector ginjector = GWT.create(DangerousGinjector.class);

    try {
      ginjector.getVeryDangerous();
      fail("Expected CreationException.");
    } catch (CreationException expected) {
      // Good.
    }
  }

  public void testSecretThrowingMethod() {
    DangerousGinjector ginjector = GWT.create(DangerousGinjector.class);

    try {
      ginjector.getHiddenDanger();
      fail("Expected CreationException.");
    } catch (CreationException expected) {
      // Good.
    }
  }

  public void testUnsafeNativeLong() {
    LongInjector injector = GWT.create(LongInjector.class);
    assertEquals(3, injector.getLong());
    assertEquals(5, injector.getHexagon().getValue());
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }

  private void assertOverrideInject(Square square) {
    assertEquals(ShapeGinModule.HEIGHT, square.getHeight());
    assertEquals(0, square.getWidth());
    assertEquals(ShapeGinModule.OTHER_HEIGHT, square.getOtherHeight());
    assertEquals(ShapeGinModule.OTHER_WIDTH, square.getOtherWidth());
  }

  private void assertInterfaceInject(Circle circle) {
    assertNotNull(circle.getColor());
    assertEquals(ShapeGinModule.COLOR, circle.getColor());
  }

  interface DangerousGinjector extends Ginjector {
    Dangerous getDangerous();
    VeryDangerous getVeryDangerous();
    HiddenDanger getHiddenDanger();
  }

  public static class Dangerous {

    @Inject
    public Dangerous() throws Exception {
      throw new Exception("test");
    }
  }

  public static class VeryDangerous {

    @Inject
    public void boom() throws Exception {
      throw new Exception("test");
    }
  }

  public static class HiddenDanger {

    @Inject
    private HiddenDanger() throws Exception {
      throw new Exception("test");
    }
  }

  @GinModules(ForwardingModule.class)
  interface ForwardingGinjector extends Ginjector {
    Forwarding getForwarding();
    void injectMembers(Forwarding forwarding);
  }

  static class ForwardingModule extends AbstractGinModule {

    @Override
    protected void configure() {
      bind(Forwarding.class).to(Forwarded.class);
      bindConstant().annotatedWith(Names.named("blue")).to("blue");
      bindConstant().annotatedWith(Names.named("red")).to("red");
    }
  }

  static class Forwarding {
    String value = "";

    @Inject
    public void setValue(@Named("red") String value) {
      this.value = value;
    }
  }

  static class Forwarded extends Forwarding {

    @Inject
    @Override
    public void setValue(@Named("blue") String value) {
      this.value = value;
    }
  }

  @GinModules(LongModule.class)
  interface LongInjector extends Ginjector {
    Hexagon getHexagon();
    long getLong();
  }

  static class LongModule extends AbstractGinModule {

    @Override
    protected void configure() {
      bindConstant().annotatedWith(Names.named("foo")).to(5L);
    }

    @Provides
    private long provideLong() {
      return 3L;
    }
  }
}
