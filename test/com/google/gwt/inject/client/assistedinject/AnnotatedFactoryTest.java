package com.google.gwt.inject.client.assistedinject;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.BindingAnnotation;
import com.google.inject.Key;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class AnnotatedFactoryTest extends GWTTestCase {
  
  public void testAnnotatedFactory() {
    AnnotatedFactoryGinjector ginjector = GWT.create(AnnotatedFactoryGinjector.class);
    assertNotNull(ginjector.getFactory());
  }
  
  public static class AnnotatedFactoryModule extends AbstractGinModule {
    @Override
    protected void configure() {
      install(new GinFactoryModuleBuilder().build(Key.get(MyFactory.class, MyAnno.class)));
    }
  }
  
  @GinModules({AnnotatedFactoryModule.class})
  public static interface AnnotatedFactoryGinjector extends Ginjector {
    @MyAnno MyFactory getFactory();
  }
  
  public static class FactoryProvidedType {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
  @BindingAnnotation
  public @interface MyAnno {}
  
  public interface MyFactory {
    FactoryProvidedType getCycleType();
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}
