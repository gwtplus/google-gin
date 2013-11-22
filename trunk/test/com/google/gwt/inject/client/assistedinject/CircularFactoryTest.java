package com.google.gwt.inject.client.assistedinject;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.Inject;

public class CircularFactoryTest extends GWTTestCase {
  
  public void testCycle() {
    CycleGinjector ginjector = GWT.create(CycleGinjector.class);
    assertNotNull(ginjector.getFactory());
    assertNotNull(ginjector.getFactory().getCycleType().factory);
  }
  
  public static class CycleModule extends AbstractGinModule {
    @Override
    protected void configure() {
      install(new GinFactoryModuleBuilder().build(CycleFactory.class));
    }
  }
  
  @GinModules({CycleModule.class})
  public static interface CycleGinjector extends Ginjector {
    CycleFactory getFactory();
  }
  
  public static class CycleType {
    final CycleFactory factory;

    @Inject
    public CycleType(CycleFactory factory) {
      this.factory = factory;
    }
  }
  
  public interface CycleFactory {
    CycleType getCycleType();
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}
