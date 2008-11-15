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
package com.google.gwt.inject.client;

import com.google.gwt.inject.client.binder.GinBinder;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.Provider;
import com.google.inject.name.Names;

import java.util.List;
import java.util.Arrays;

/**
 * GIN module for test cases.
 */
public class MyAppGinModule extends AbstractGinModule {

  protected void configure() {
    // Note that MyServiceImpl has @Singleton on itself
    bind(MyService.class).to(MyServiceImpl.class);

    bind(MyProvidedObject.class).toProvider(MyProvidedProvider.class).in(Singleton.class);

    // SimpleObject (all three flavors) are singletons
    bind(SimpleObject.class).in(Singleton.class);
    bind(SimpleObject.class).annotatedWith(Names.named("red")).to(SimpleObject.class);
    bind(SimpleObject.class).annotatedWith(Names.named("blue")).to(SimpleObject.class);

    bind(EagerObject.class).asEagerSingleton();

    bindConstant().annotatedWith(MyBindingAnnotation.class)
        .to(MyAppGinjector.ANNOTATED_STRING_VALUE);
  }

  static class MyModule implements GinModule {
    public void configure(GinBinder binder) {
      binder.bind(new TypeLiteral<List<String>>() { }).toProvider(ListOfStringProvider.class);
      binder.bind(InjectTest.InnerType.class).annotatedWith(MyBindingAnnotation.class).to(InjectTest.InnerType.class);
      binder.bind(InjectTest.MyType.class).toProvider(InjectTest.MyProvider.class);
    }
  }

  static class ListOfStringProvider implements Provider<List<String>> {
    public List<String> get() {
      return Arrays.asList("blah");
    }
  }
}
