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

import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.inject.rebind.util.InjectorMethod;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.Preconditions;
import com.google.gwt.inject.rebind.util.SourceSnippet;
import com.google.gwt.inject.rebind.util.SourceSnippetBuilder;
import com.google.gwt.inject.rebind.util.SourceSnippets;
import com.google.inject.Key;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.inject.Provider;

/**
 * A binding to call the requested {@link com.google.inject.Provider}.
 */
public class BindProviderBinding extends AbstractBinding implements Binding {

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

  public SourceSnippet getCreationStatements(NameGenerator nameGenerator,
      List<InjectorMethod> methodsOutput) throws NoSourceNameException {
    String providedType = ReflectUtil.getSourceName(sourceKey.getTypeLiteral());

    return new SourceSnippetBuilder()
        .append(providedType).append(" result = ").append(SourceSnippets.callGetter(providerKey))
        .append(".get();")
        .build();
  }

  public Collection<Dependency> getDependencies() {
    Context context = getContext();

    Collection<Dependency> dependencies = new ArrayList<Dependency>();
    dependencies.add(new Dependency(Dependency.GINJECTOR, sourceKey, context));
    dependencies.add(new Dependency(sourceKey, providerKey, context));
    return dependencies;
  }
}
