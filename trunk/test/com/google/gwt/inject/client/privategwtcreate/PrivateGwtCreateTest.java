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
package com.google.gwt.inject.client.privategwtcreate;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.PrivateGinModule;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

/**
 * Verify that when a private module exposes an implicit GWT.create() binding for a class, the
 * exposed binding can be injected outside the private module.  Also verifies that we lie to
 * Guice "correctly" while doing validation of multiple private modules.
 */
public class PrivateGwtCreateTest extends GWTTestCase {

  public void testView() throws Exception {
    ViewGinjector ginjector = GWT.create(ViewGinjector.class);
    UiView view = ginjector.getView();
    assertNotNull(view);
    assertNotNull(view.getPanel());
  }

  public void testView2() throws Exception {
    ViewGinjector2 ginjector = GWT.create(ViewGinjector2.class);
    UiView view1 = ginjector.getView();
    UiView view2 = ginjector.getView2();
    assertNotSame(view1, view2);
    assertNotNull(view1);
    assertNotNull(view1.getPanel());
    assertNotNull(view2);
    assertNotNull(view2.getPanel());
  }

  @GinModules({TopModule.class})
  interface ViewGinjector extends Ginjector {
    UiView getView();
  }

  @GinModules({TopModule1.class, ViewModule2.class})
  interface ViewGinjector2 extends Ginjector {
    @Named("1") UiView getView();
    @Named("2") UiView getView2();
  }

  // We need to use separate view modules in the two different cases to avoid
  // double bindings.
  static class TopModule extends AbstractGinModule {
    @Override
    protected void configure() {
      install(new ViewModule());
    }
  }

  static class TopModule1 extends AbstractGinModule {
    @Override
    protected void configure() {
      install(new ViewModule1());
    }
  }

  static class ViewModule extends PrivateGinModule {
    @Override
    protected void configure() {
      bind(UiView.class);
      expose(UiView.class);
    }
  }

  static class ViewModule1 extends PrivateGinModule {
    @Override
    protected void configure() {
      bind(UiView.class).annotatedWith(Names.named("1")).to(UiView.class);
      expose(UiView.class).annotatedWith(Names.named("1"));
    }
  }

  static class UiViewImpl extends UiView {
    @Inject
    public UiViewImpl(MyBinder binder) {
      super(binder);
    }
  }

  static class ViewModule2 extends PrivateGinModule {
    @Override
    protected void configure() {
      bind(UiView.class).to(UiViewImpl.class);
      bind(UiView.class).annotatedWith(Names.named("2")).to(UiView.class);
      expose(UiView.class).annotatedWith(Names.named("2"));
    }
  }

  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}
