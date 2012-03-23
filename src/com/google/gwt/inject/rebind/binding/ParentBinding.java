/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.inject.rebind.binding;

import com.google.gwt.dev.util.Preconditions;
import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.util.InjectorWriteContext;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.inject.Key;

import java.util.Collection;
import java.util.Collections;

/**
 * Binding that represents a value inherited from higher in the injector hierarchy.
 * 
 * TODO(bchambers): It would be nice if we didn't need to have the creator/parent
 * paradigm for parent and child bindings, but it is the easiest way to add this
 * to Gin.
 */
public class ParentBinding extends AbstractSingleMethodBinding implements Binding {

  private final Key<?> key;
  private final GinjectorBindings parentBindings;

  ParentBinding(Key<?> key, GinjectorBindings parentBindings, Context context) {
    super(context);

    this.key = Preconditions.checkNotNull(key);
    this.parentBindings = Preconditions.checkNotNull(parentBindings);
  }
  
  public GinjectorBindings getParentBindings() {
    return parentBindings;
  }

  public void appendCreatorMethodBody(StringBuilder builder,
      InjectorWriteContext injectorWriteContext) {
    String parentMethodCall = injectorWriteContext.callParentGetter(key, parentBindings);
    builder.append("return ").append(parentMethodCall).append(";");
  }

  public Collection<Dependency> getDependencies() {
    // ParentBindings are only added *after* resolution has happened, so their dependencies don't
    // matter
    return Collections.emptyList();
  }
}
