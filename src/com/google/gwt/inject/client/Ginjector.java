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

/**
 * Where the GWT world stops and the GIN/Guice world begins.
 * Analogous to Guice's {@code Injector}, this type can be used to bootstrap injection. Unlike
 * Guice, however, this is not a type that you create, but rather a type that you extend. It's
 * best explained with an example. Consider this Guice code:
 * <pre>
 * // Define and create a Module
 * Module applicationModule = ...;
 * 
 * // create an Injector
 * Injector injector = Guice.createInjector(applicationModule);
 *
 * // bootstrap the injection
 * injector.getInstance(Application.class);
 * </pre>
 *
 * Here's the equivalent GIN code:
 * <pre>
 * // Define a GinModule (e.g. ApplicationModule) but don't create it.
 *
 * // create a Ginjector
 * ApplicationGinjector ginjector = GWT.create(ApplicationGinjector.class);
 * 
 * // bootstrap the injection
 * RootPanel.get().add(ginjector.getApplication());
 *
 * (somewhere else...)
 *
 * // define a Ginjector subtype
 * {@code @}GinModules(ApplicationModule.class)
 * public interface ApplicationGinjector extends Ginjector {
 *   Application getApplication();
 * }
 * </pre>
 * 
 * Note that this is not named "G-injector" -- its "GIN-jector."
 */
public interface Ginjector {
}
