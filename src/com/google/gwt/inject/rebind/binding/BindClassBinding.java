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
    // In which package should the creator method for a bind class binding be
    // placed, when the bound class key (the "interface type") and the source
    // class key (the "implementation type") might be in different packages, and
    // one or both might be package-private?
    //
    // The answer is that it's always right to use the source class key to
    // choose the creator's package.  Consider these cases:
    //
    //  1) Both public: we can place the creator in any package.
    //
    //  2) boundClass private, sourceClass public: they must be in the same
    //     package, since sourceClass implements boundClass.
    //
    //  3) boundClass public, sourceClass private: we can create an instance of
    //     sourceClass only in its own package.
    //
    //  4) boundClass private, sourceClass private: again, they must be in the
    //     same package (and we have to create an instance of sourceClass from
    //     its own package).
    super(context, sourceClassKey);

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
