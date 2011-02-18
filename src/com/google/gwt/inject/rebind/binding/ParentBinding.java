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
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Inject;
import com.google.inject.Key;

import java.util.HashSet;

/**
 * Binding that represents a value inherited from higher in the injector hierarchy.
 * 
 * TODO(bchambers): It would be nice if we didn't need to have the creator/parent
 * paradigm for parent and child bindings, but it is the easiest way to add this
 * to Gin.
 */
public class ParentBinding implements Binding {

  private Key<?> key;
  private GinjectorBindings parentBindings;
  private SourceWriteUtil sourceWriteUtil;
  private GinjectorNameGenerator ginjectorNameGenerator;

  @Inject
  public ParentBinding(SourceWriteUtil sourceWriteUtil,
      GinjectorNameGenerator ginjectorNameGenerator) {
    this.sourceWriteUtil = sourceWriteUtil;
    this.ginjectorNameGenerator = ginjectorNameGenerator;
  }

  public void setKey(Key<?> key) {
    this.key = key;
  }

  public void setParent(GinjectorBindings parentBindings) {
    this.parentBindings = parentBindings;
  }
  
  public GinjectorBindings getParentBindings() {
    return parentBindings;
  }

  public void writeCreatorMethods(SourceWriter writer, String creatorMethodSignature,
      NameGenerator nameGenerator) {
    Preconditions.checkNotNull(parentBindings);
    String parentMethodName = parentBindings.getNameGenerator().getGetterMethodName(key);
    sourceWriteUtil.writeMethod(writer, creatorMethodSignature, 
        String.format("return %s.this.%s();", ginjectorNameGenerator.getClassName(parentBindings),
            parentMethodName));
  }

  public RequiredKeys getRequiredKeys() {
    return new RequiredKeys(new HashSet<Key<?>>());
  }
}
