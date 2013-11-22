package com.google.gwt.inject.client.privatedependsonexposed;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.PrivateGinModule;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class PrivateDependsOnExposedTest extends GWTTestCase {

  public void testSameBar() throws Exception {
    MyGinjector ginjector = GWT.create(MyGinjector.class);
    Bar bar0 = ginjector.getBar0();
    Bar bar1 = ginjector.getBar1();
    assertEquals(bar0, bar1);
  }

  @GinModules({Bar0Module.class, Bar1Module.class})
  interface MyGinjector extends Ginjector {
    @Named("0") Bar getBar0();
    @Named("1") Bar getBar1();
  }

  static class Foo {}

  @Singleton
  static class Bar {
    @Inject Foo foo;
  }

  static class Bar0Module extends PrivateGinModule {
    @Override
    protected void configure() {
      bind(Foo.class);
      expose(Foo.class);

      bind(Bar.class).annotatedWith(Names.named("0")).to(Bar.class);
      expose(Bar.class).annotatedWith(Names.named("0"));
    }
  }
  static class Bar1Module extends PrivateGinModule {
    @Override
    protected void configure() {
      bind(Bar.class).annotatedWith(Names.named("1")).to(Bar.class);
      expose(Bar.class).annotatedWith(Names.named("1"));
    }
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}
