/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.inject.client.privatefactory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.PrivateGinModule;
import com.google.gwt.inject.client.assistedinject.GinFactoryModuleBuilder;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.Exposed;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;

import javax.inject.Named;

public class PrivateFactoryTest extends GWTTestCase {

  public void testFactoryInParent_Exposed() throws Exception {
    PrivateFactoryInParentGinjector ginjector = GWT.create(PrivateFactoryInParentGinjector.class);
    Widget fooWidget = ginjector.getWidgetFactory().createWidget("foo");
    Widget barWidget = ginjector.getWidgetFactory().createWidget("bar");
    assertSame(fooWidget.part, barWidget.part);
    assertEquals("foo", fooWidget.name);
    assertEquals("bar", barWidget.name);
  }
  
  public void testFactoryInChild() throws Exception {
    PrivateFactoryInChildGinjector ginjector = GWT.create(PrivateFactoryInChildGinjector.class);
    Widget banana = ginjector.getBananaWidget();
    Widget orange = ginjector.getOrangeWidget();
    WidgetPair pair = ginjector.getWidgetPair();
    assertEquals("banana", banana.name);
    assertEquals("orange", orange.name);
    assertSame(banana, pair.banana);
    assertSame(orange, pair.orange);
  }
  
  interface Part {}
  
  // Each Part depends on the factory so that we can verify that (1) the lies are correctly
  // installed and (2) the factories are installed early enough to be depended on.
  static class PartA implements Part {
    @Inject public PartA(WidgetFactory factory) {}
  }
  static class PartB implements Part {
    @Inject public PartB(WidgetFactory factory) {}
  }
  
  static class Widget {
    
    Part part;
    String name;
    
    @Inject
    public Widget(Part part, @Assisted String name) {
      this.part = part;
      this.name = name;
    }
  }
  
  interface WidgetFactory {
    Widget createWidget(String name);
  }
  
  static class WidgetPair {
    Widget banana;
    Widget orange;
    
    WidgetPair(Widget banana, Widget orange) {
      this.banana = banana;
      this.orange = orange;
    }
  }
  
  @GinModules({ParentWithFactoryModule.class, ChildWithoutFactoryModule.class})
  interface PrivateFactoryInParentGinjector extends Ginjector {
    WidgetFactory getWidgetFactory();
  }
  
  @GinModules({ParentWithoutFactoryModule.class, ChildWithFactoryModule.class})
  interface PrivateFactoryInChildGinjector extends Ginjector {
    @Named("banana") Widget getBananaWidget();
    @Named("orange") Widget getOrangeWidget();
    WidgetPair getWidgetPair();
  }
  
  static class ParentWithFactoryModule extends AbstractGinModule {
    @Override
    protected void configure() {
      install(new GinFactoryModuleBuilder().build(WidgetFactory.class));
    }
  }
  
  static class ChildWithoutFactoryModule extends PrivateGinModule {
    @Override
    protected void configure() {
      bind(Part.class).to(PartA.class).in(Singleton.class);
      expose(Part.class);
    }
  }
  
  static class ParentWithoutFactoryModule extends AbstractGinModule {
    @Override
    protected void configure() {
      install(new OtherChildModule());
    } 
    
    @Provides @Named("banana") @Singleton
    Widget provideBananaWidget(WidgetFactory factory) {
      return factory.createWidget("banana");
    }
  }
  
  static class ChildWithFactoryModule extends PrivateGinModule {
    @Override
    protected void configure() {
      install(new GinFactoryModuleBuilder().build(WidgetFactory.class));  
      expose(WidgetFactory.class);
    }
    
    @Provides @Named("orange") @Singleton @Exposed
    Widget provideOrangeWidget(WidgetFactory factory) {
      return factory.createWidget("orange");
    }
  }
  
  static class OtherChildModule extends PrivateGinModule {
    @Override
    protected void configure() {
      bind(Part.class).to(PartB.class);
      expose(Part.class);
    }
    
    @Provides @Exposed
    WidgetPair provideWidgetPair(@Named("banana") Widget banana, @Named("orange") Widget orange) {
      return new WidgetPair(banana, orange);
    }
  }
  
  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}