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

import com.google.inject.name.Named;
import com.google.inject.Provider;

/**
 * A more complex injector for test case.
*/
@GinModules(MyAppGinModule.class)
public interface MyAppGinjector extends Ginjector {

  String ANNOTATED_STRING_VALUE = "abc";

  MyApp getMyApp();

  SimpleObject getSimple();

  MyService getMyService();

  MyServiceImpl getMyServiceImpl();

  MyProvidedObject getSingleton();

  @Named("blue") SimpleObject getSimpleBlue();
  @Named("red") SimpleObject getSimpleRed();

  EagerObject getEagerObject();

  // Providers we never bound explicitly -- they should be synthesized
  // since we bound the keys directly
  Provider<SimpleObject> getSimpleProvider();
  @Named("blue") Provider<SimpleObject> getSimpleBlueProvider();

  @MyBindingAnnotation String getAnnotatedString();

  MyRemoteServiceAsync getMyRemoteServiceAsync();
}
