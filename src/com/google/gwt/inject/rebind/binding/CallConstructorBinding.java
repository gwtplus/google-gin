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
import com.google.gwt.inject.rebind.KeyUtil;
import com.google.gwt.inject.rebind.MethodCollector;
import com.google.gwt.inject.rebind.SourceWriteUtil;
import com.google.inject.Inject;

/**
 * A binding that calls a single constructor directly. Values for constructor
 * parameters are retrieved by going back through the injector.
 */
public class CallConstructorBinding extends CreatorBinding {

  private final SourceWriteUtil sourceWriteUtil;

  private JConstructor constructor;

  @Inject
  public CallConstructorBinding(@Injectables MethodCollector methodCollector,
      SourceWriteUtil sourceWriteUtil, KeyUtil keyUtil) {
    super(methodCollector, sourceWriteUtil, keyUtil);
    this.sourceWriteUtil = sourceWriteUtil;
  }

  public void setConstructor(JConstructor constructor) {
    this.constructor = constructor;
    setClassType(constructor.getEnclosingType());
    addParamTypes(constructor);
  }

  @Override protected void appendCreationStatement(StringBuilder sb) {
    assert (constructor != null);
    sb.append("new ").append(getTypeName());
    sourceWriteUtil.appendInvoke(sb, constructor);
  }
}
