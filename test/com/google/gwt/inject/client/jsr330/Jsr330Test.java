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

package com.google.gwt.inject.client.jsr330;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.Provides;
import com.google.inject.name.Names;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

public class Jsr330Test extends GWTTestCase {

  public void testInjectAnnotation() {
    Jsr330Ginjector ginjector = GWT.create(Jsr330Ginjector.class);

    A a = ginjector.getA();
    assertEquals("Robert", a.name);
  }

  public void testNamed() {
    Jsr330Ginjector ginjector = GWT.create(Jsr330Ginjector.class);

    B b = ginjector.getB();
    assertEquals(4, b.size);
  }

  public void testNamedProviderMethod() {
    Jsr330Ginjector ginjector = GWT.create(Jsr330Ginjector.class);

    F f = ginjector.getF();
    assertEquals(42, f.small);
  }

  public void testProviderInjection() {
    Jsr330Ginjector ginjector = GWT.create(Jsr330Ginjector.class);

    C c = ginjector.getC();
    assertEquals("Robert", c.nameProvider.get());
  }

  public void testProviderBinding() {
    Jsr330Ginjector ginjector = GWT.create(Jsr330Ginjector.class);

    G g = ginjector.getG();
    assertFalse(g.truthProvider.get());
  }

  public void testSingletonAnnotation() {
    Jsr330Ginjector ginjector = GWT.create(Jsr330Ginjector.class);

    assertSame(ginjector.getD(), ginjector.getD());
  }

  public void testSingletonAnnotationProviderMethod() {
    Jsr330Ginjector ginjector = GWT.create(Jsr330Ginjector.class);

    assertSame(ginjector.getH(), ginjector.getH());
  }

  public void testSingletonBinding() {
    Jsr330Ginjector ginjector = GWT.create(Jsr330Ginjector.class);

    assertSame(ginjector.getE(), ginjector.getE());
  }

  public void testNameAnnotatedGinjectorMethod() {
    Jsr330Ginjector ginjector = GWT.create(Jsr330Ginjector.class);

    assertEquals((int) 4, ginjector.getSize());
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }

  public static class A {

    final String name;

    @Inject
    public A(String name) {
      this.name = name;
    }
  }

  public static class B {

    final int size;

    @Inject
    public B(@Named("size") int size) {
      this.size = size;
    }
  }

  public static class C {

    final Provider<String> nameProvider;

    @Inject
    public C(Provider<String> nameProvider) {
      this.nameProvider = nameProvider;
    }
  }

  @Singleton
  public static class D {}

  public static class E {}

  public static class F {

    final long small;

    @Inject
    public F(@Named("small") long small) {
      this.small = small;
    }
  }

  public static class G {

    final Provider<Boolean> truthProvider;

    @Inject
    public G(Provider<Boolean> truthProvider) {
      this.truthProvider = truthProvider;
    }
  }

  public static class TruthProvider implements Provider<Boolean> {

    public Boolean get() {
      return false;
    }
  }

  public static class H {}

  @GinModules(Jsr330GinModule.class)
  public static interface Jsr330Ginjector extends Ginjector {

    A getA();

    B getB();

    C getC();

    D getD();

    E getE();

    F getF();

    G getG();

    H getH();

    @Named("size") int getSize();
  }

  public static class Jsr330GinModule extends AbstractGinModule {

    @Override
    protected void configure() {
      bindConstant().annotatedWith(Names.named("size")).to(4);
      bind(Boolean.class).toProvider(TruthProvider.class);
      bind(E.class).in(Singleton.class);
    }

    @Provides
    String provideString() {
      return "Robert";
    }

    @Provides
    @Named("small")
    Long provideSmallLong() {
      return 42L;
    }

    @Provides
    @Singleton
    H provideH() {
      return new H();
    }
  }
}
