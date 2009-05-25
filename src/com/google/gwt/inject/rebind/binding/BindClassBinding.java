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

import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Inject;
import com.google.inject.Key;

import java.util.Collections;

/**
 * Binding implementation that replaces one type with another.
 */
public class BindClassBinding implements Binding {

  private final NameGenerator nameGenerator;
  private final SourceWriteUtil sourceWriteUtil;

  private Key<?> boundClassKey;

  @Inject
  public BindClassBinding(NameGenerator nameGenerator, SourceWriteUtil sourceWriteUtil) {
    this.nameGenerator = nameGenerator;
    this.sourceWriteUtil = sourceWriteUtil;
  }

  public void setBoundClassKey(Key<?> boundClassKey) {
    this.boundClassKey = boundClassKey;
  }

  public void writeCreatorMethods(SourceWriter writer, String creatorMethodSignature) {
    assert (boundClassKey != null);
    sourceWriteUtil.writeMethod(writer, creatorMethodSignature,
        "return " + nameGenerator.getGetterMethodName(boundClassKey) + "();");
  }

  public RequiredKeys getRequiredKeys() {
    assert (boundClassKey != null);
    return new RequiredKeys(Collections.<Key<?>>singleton(boundClassKey));
  }
}
