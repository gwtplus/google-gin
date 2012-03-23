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
package com.google.gwt.inject.rebind.binding;

import com.google.gwt.dev.util.Preconditions;
import com.google.gwt.inject.rebind.util.InjectorWriteContext;
import com.google.inject.Key;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Binding implementation that replaces one type with another.
 */
public class BindClassBinding extends AbstractSingleMethodBinding implements Binding {

  private final Key<?> sourceClassKey;
  private final Key<?> boundClassKey;

  BindClassBinding(Key<?> boundClassKey, Key<?> sourceClassKey, Context context) {
    super(context);

    this.boundClassKey = Preconditions.checkNotNull(boundClassKey);
    this.sourceClassKey = Preconditions.checkNotNull(sourceClassKey);
  }

  public void appendCreatorMethodBody(StringBuilder builder,
      InjectorWriteContext injectorWriteContext) {
    builder.append("return ").append(injectorWriteContext.callGetter(boundClassKey)).append(";");
  }

  public Collection<Dependency> getDependencies() {
    Context context = getContext();

    Collection<Dependency> dependencies = new ArrayList<Dependency>();
    dependencies.add(new Dependency(Dependency.GINJECTOR, sourceClassKey, context));
    dependencies.add(new Dependency(sourceClassKey, boundClassKey, context));
    return dependencies;
  }
}
