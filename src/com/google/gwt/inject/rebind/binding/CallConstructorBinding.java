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

import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.inject.rebind.util.KeyUtil;
import com.google.gwt.inject.rebind.util.MemberCollector;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Inject;
import com.google.inject.Key;

/**
 * A binding that calls a single constructor directly. Values for constructor
 * parameters are retrieved by going back through the injector.
 */
public class CallConstructorBinding extends CreatorBinding {

  private final SourceWriteUtil sourceWriteUtil;

  private JConstructor constructor;

  @Inject
  public CallConstructorBinding(@Injectable MemberCollector memberCollector,
      SourceWriteUtil sourceWriteUtil, KeyUtil keyUtil, BindingIndex bindingIndex,
      NameGenerator nameGenerator) {
    super(memberCollector, sourceWriteUtil, keyUtil, bindingIndex, nameGenerator);
    this.sourceWriteUtil = sourceWriteUtil;
  }

  public void setConstructor(JConstructor constructor, Key<?> key) {
    this.constructor = constructor;
    setClassType(constructor.getEnclosingType(), key);
    addParamTypes(constructor);
  }

  @Override protected void appendCreationStatement(SourceWriter sourceWriter, StringBuilder sb) {
    assert (constructor != null);
    sb.append(sourceWriteUtil.createConstructorInjection(sourceWriter, constructor));
  }
}
