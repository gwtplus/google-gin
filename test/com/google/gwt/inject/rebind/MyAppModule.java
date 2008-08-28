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
package com.google.gwt.inject.rebind;

import com.google.gwt.inject.client.MyServiceImpl;
import com.google.gwt.inject.client.MyService;
import com.google.gwt.inject.client.MySingletonProvider;
import com.google.gwt.inject.client.MySingleton;
import com.google.gwt.inject.client.SimpleObject;
import com.google.gwt.inject.client.EagerObject;
import com.google.gwt.inject.client.MyAppInjector;
import com.google.gwt.inject.client.MyBindingAnnotation;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

/**
 * GIN module for test cases.
 *
 * @author bstoler@google.com (Brian Stoler)
*/
public class MyAppModule extends AbstractModule {
  protected void configure() {
    // Note that MyServiceImpl has @Singleton on itself
    bind(MyService.class).to(MyServiceImpl.class);

    bind(MySingleton.class).toProvider(MySingletonProvider.class).in(Singleton.class);

    // SimpleObject (all three flavors) are singletons
    bind(SimpleObject.class).in(Singleton.class);
    bind(SimpleObject.class).annotatedWith(Names.named("red")).in(Singleton.class);
    bind(SimpleObject.class).annotatedWith(Names.named("blue")).in(Singleton.class);

    bind(EagerObject.class).asEagerSingleton();

    bindConstant().annotatedWith(MyBindingAnnotation.class)
        .to(MyAppInjector.ANNOTATED_STRING_VALUE);
  }
}
