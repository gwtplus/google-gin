package com.google.gwt.inject.client.privatedoublebinds;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.PrivateGinModule;
import com.google.gwt.junit.client.GWTTestCase;

// We can't run this test because it causes an expected error.
// It's here for when we can detect such problems.
public class PrivateDoubleBindsTest extends GWTTestCase {

  public void testDoubleBound() throws Exception {
    MyGinjector ginjector = GWT.create(MyGinjector.class);
    fail("Should not compile at all");
  }

  @GinModules({BarModule.class})
  interface MyGinjector extends Ginjector {
    Bar getBar();
  }

  interface Bar {}

  static class BarImpl implements Bar{}

  static class PrivateBarModule extends PrivateGinModule {
    @Override
    protected void configure() {
      bind(Bar.class).to(BarImpl.class);
    }
  }
  static class BarModule extends AbstractGinModule {
    @Override
    protected void configure() {
      bind(Bar.class).to(BarImpl.class);
      install(new PrivateBarModule());
    }
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}
