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

import javax.inject.Provider;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A binding to call the requested {@link com.google.inject.Provider}.
 */
public class BindProviderBinding extends AbstractSingleMethodBinding implements Binding {

  private final Key<? extends Provider<?>> providerKey;
  private final Key<?> sourceKey;

  BindProviderBinding(Key<? extends Provider<?>> providerKey, Key<?> sourceKey, Context context) {
    // In which package should we place the code to inject and use a
    // user-supplied provider, given that the provider might be in a different
    // package from the type it provides?
    //
    // This binding needs to be able to create and use a provider, which means
    // that the creator method needs to go in the provider's package.  If the
    // provided type (the source key) is in a different package, it has to be
    // visible in the provider's package; otherwise, the provider couldn't
    // return it from get().
    super(context, providerKey);

    this.providerKey = Preconditions.checkNotNull(providerKey);
    this.sourceKey = Preconditions.checkNotNull(sourceKey);
  }

  public void appendCreatorMethodBody(StringBuilder builder, InjectorWriteContext writeContext) {
    builder.append("return ").append(writeContext.callGetter(providerKey)).append(".get();");
  }

  public Collection<Dependency> getDependencies() {
    Context context = getContext();

    Collection<Dependency> dependencies = new ArrayList<Dependency>();
    dependencies.add(new Dependency(Dependency.GINJECTOR, sourceKey, context));
    dependencies.add(new Dependency(sourceKey, providerKey, context));
    return dependencies;
  }
}
