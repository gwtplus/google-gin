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
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Inject;
import com.google.inject.Key;

import java.util.Collection;
import java.util.Collections;

/**
 * Simple binding that allows injection of the ginjector.
 */
public class GinjectorBinding extends AbstractBinding implements Binding {

  private final SourceWriteUtil sourceWriteUtil;
  private final Class<? extends Ginjector> ginjectorInterface;

  @Inject
  public GinjectorBinding(SourceWriteUtil sourceWriteUtil, 
      @GinjectorInterfaceType Class<? extends Ginjector> ginjectorInterface) {
    // This message is used to generate double-binding errors.  We output a very
    // specific message, because people were confused and tried to bind their
    // Ginjectors manually.
    //
    // TODO(dburrows): probably it's better to explicitly error if the user
    // manually binds the ginjector, instead of relying on the double-binding
    // check to catch it.
    super(BindingContext.forText("Automatic binding for " + ginjectorInterface
        + "; you should not need to bind this manually."));

    this.sourceWriteUtil = sourceWriteUtil;
    this.ginjectorInterface = ginjectorInterface;
  }

  public void writeCreatorMethods(SourceWriter writer, String creatorMethodSignature,
      NameGenerator nameGenerator) {
    sourceWriteUtil.writeMethod(writer, creatorMethodSignature, "return this;");
  }

  public Collection<Dependency> getDependencies() {
    return Collections.singletonList(
        new Dependency(Dependency.GINJECTOR, Key.get(ginjectorInterface),
            "Automatic binding for " + ginjectorInterface));
  }
}
