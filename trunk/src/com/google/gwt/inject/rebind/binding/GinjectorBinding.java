/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.rebind.GinjectorInterfaceType;
import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.inject.rebind.util.InjectorMethod;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceSnippet;
import com.google.gwt.inject.rebind.util.SourceSnippetBuilder;
import com.google.gwt.inject.rebind.util.SourceSnippets;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Simple binding that allows injection of the ginjector.
 */
public class GinjectorBinding extends AbstractBinding implements Binding {

  private final Class<? extends Ginjector> ginjectorInterface;

  @Inject
  public GinjectorBinding(@GinjectorInterfaceType Class<? extends Ginjector> ginjectorInterface) {
    // This message is used to generate double-binding errors.  We output a very
    // specific message, because people were confused and tried to bind their
    // Ginjectors manually.
    //
    // TODO(dburrows): probably it's better to explicitly error if the user
    // manually binds the ginjector, instead of relying on the double-binding
    // check to catch it.
    super(Context.format("Automatic binding for %s; you should not need to bind this manually.",
        ginjectorInterface), TypeLiteral.get(ginjectorInterface));

    this.ginjectorInterface = ginjectorInterface;
  }

  public SourceSnippet getCreationStatements(NameGenerator nameGenerator,
      List<InjectorMethod> methodsOutput) throws NoSourceNameException {
    String type = ReflectUtil.getSourceName(ginjectorInterface);

    return new SourceSnippetBuilder()
        .append(type).append(" result = ").append(SourceSnippets.callGinjectorInterfaceGetter())
        .append(";")
        .build();
  }

  public Collection<Dependency> getDependencies() {
    return Collections.singletonList(
        new Dependency(Dependency.GINJECTOR, Key.get(ginjectorInterface),
            "Automatic binding for %s", ginjectorInterface));
  }
}
