package com.google.gwt.inject.client.privateeager;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.PrivateGinModule;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.Inject;
import com.google.inject.Singleton;

public class PrivateEagerSingletonInstantiationTest extends GWTTestCase {

  public void testInject() throws Exception {
    Inject0 i = GWT.create(Inject0.class);
    assertEquals(1, MySingleton.counter);
    assertEquals("x", i.get().data); // X is eager, and should be instantiated
  }

  @Singleton
  public static class MySingleton {
    static int counter = 0;
    String data;
  }

  public static class X {
    @Inject
    public X(MySingleton m) {
      m.counter++;
      m.data = "x";
    }
  }

  public static class PrivateGinModuleForX extends PrivateGinModule {
    @Override
    protected void configure() {
      bind(X.class).asEagerSingleton();
    }
  }
  public static class GinModule extends AbstractGinModule {
    @Override
    protected void configure() {
      install(new PrivateGinModuleForX());
    }
  }

  @GinModules(GinModule.class)
  public interface Inject0 extends Ginjector {
    MySingleton get();
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}

