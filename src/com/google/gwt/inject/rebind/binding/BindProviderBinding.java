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
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Key;

import javax.inject.Provider;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A binding to call the requested {@link com.google.inject.Provider}.
 */
public class BindProviderBinding implements Binding {

  private final SourceWriteUtil sourceWriteUtil;
  private final Key<? extends Provider<?>> providerKey;
  private final Key<?> sourceKey;

  BindProviderBinding(SourceWriteUtil sourceWriteUtil, Key<? extends Provider<?>> providerKey,
      Key<?> sourceKey) {
    this.sourceWriteUtil = sourceWriteUtil;
    this.providerKey = Preconditions.checkNotNull(providerKey);
    this.sourceKey = Preconditions.checkNotNull(sourceKey);
  }

  public void writeCreatorMethods(SourceWriter writer, String creatorMethodSignature, 
      NameGenerator nameGenerator) {
    sourceWriteUtil.writeMethod(writer, creatorMethodSignature,
        "return " + nameGenerator.getGetterMethodName(providerKey) + "().get();");
  }

  public Collection<Dependency> getDependencies() {
    Collection<Dependency> dependencies = new ArrayList<Dependency>();
    dependencies.add(new Dependency(Dependency.GINJECTOR, sourceKey));
    dependencies.add(new Dependency(sourceKey, providerKey));
    return dependencies;
  }
}
