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
import com.google.gwt.inject.rebind.ErrorManager;
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

  private final ErrorManager errorManager;
  private final Key<?> key;
  private final GinjectorBindings childBindings;

  public ExposedChildBinding(ErrorManager errorManager, Key<?> key, GinjectorBindings childBindings,
      Context context) {
    super(context, key);

    this.errorManager = Preconditions.checkNotNull(errorManager);
    this.key = Preconditions.checkNotNull(key);
    this.childBindings = Preconditions.checkNotNull(childBindings);
  }

  /**
   * The getter must be placed in the same package as the child getter, to ensure that its return
   * type is visible.
   */
  public String getGetterMethodPackage() {
    Binding childBinding = childBindings.getBinding(key);
    if (childBinding == null) {
      // The child binding should exist before we try to expose it!
      errorManager.logError("No child binding found in %s for %s.", childBindings, key);
      return "";
    } else {
      return childBinding.getGetterMethodPackage();
    }
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
