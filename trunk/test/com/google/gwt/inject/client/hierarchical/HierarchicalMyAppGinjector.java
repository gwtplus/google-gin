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
package com.google.gwt.inject.client.hierarchical;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.MyAppGinjector;
import com.google.gwt.inject.client.SimpleObject;
import com.google.inject.name.Named;

/**
 * An injector interface that is part of an injector heirarchy.
*/
@GinModules(HierarchicalMyAppGinModule.class)
public interface HierarchicalMyAppGinjector extends MyAppGinjector {
  
  // Adds an additional method.
  @Named("purple")
  SimpleObject getSimplePurple();
  
  // Redefine a method of the super-interface. Unneccessary but legal and the 
  // generator needs to handle this, and not output 2 implementations.
  SimpleObject getSimple();

  // Adds an unnamed SimpleObject. Calls to this should return the same singleton
  // object as getSimple() calls.
  SimpleObject getUnnamedSimple();
  
  // Redefines a super-interface method with a different @Named annotation.
  // Since @Named("red") is bound as a singleton this should return the same
  // instance as getSimpleRed() from the superinterface.
  @Named("red") SimpleObject getSimpleBlue();
}
