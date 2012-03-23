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
import com.google.gwt.inject.rebind.GinjectorNameGenerator;
import com.google.gwt.inject.rebind.util.InjectorWriteContext;
import com.google.inject.Key;

import java.util.Collection;
import java.util.Collections;

/**
 * Binding that represents a value exposed to this level from lower in the injector hierarchy.
 * 
 * TODO(bchambers): As with {@link ParentBinding} it would be nice if this didn't need the
 * no-op creator method.
 */
public class ExposedChildBinding extends AbstractSingleMethodBinding implements Binding {

  private final Key<?> key;
  private final GinjectorBindings childBindings;

  public ExposedChildBinding(Key<?> key, GinjectorBindings childBindings, Context context) {
    super(context);

    this.key = Preconditions.checkNotNull(key);
    this.childBindings = Preconditions.checkNotNull(childBindings);
  }

  public GinjectorBindings getChildBindings() {
    return childBindings;
  }

  public void appendCreatorMethodBody(StringBuilder sb, InjectorWriteContext injectorWriteContext) {
    sb.append("return ").append(injectorWriteContext.callChildGetter(childBindings, key))
        .append(";");
  }

  public Collection<Dependency> getDependencies() {
    // Don't need to do anything.  The binding is positioned in an earlier stage of resolution.
    return Collections.emptySet();
  }
}
