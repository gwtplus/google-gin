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

package com.google.gwt.inject.rebind;

import com.google.gwt.core.ext.typeinfo.JAbstractMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Simple helper object for source writing.
 */
@Singleton
public class SourceWriteUtil {

  private final KeyUtil keyUtil;
  private final NameGenerator nameGenerator;

  @Inject
  private SourceWriteUtil(KeyUtil keyUtil, NameGenerator nameGenerator) {
    this.keyUtil = keyUtil;
    this.nameGenerator = nameGenerator;
  }

  public void appendInvoke(StringBuilder sb, JAbstractMethod method) {
    JParameter[] params = method.getParameters();
    sb.append("(");
    for (int i = 0; i < params.length; i++) {
      if (i != 0) {
        sb.append(", ");
      }

      sb.append(nameGenerator.getGetterMethodName(keyUtil.getKey(params[i]))).append("()");
    }
    sb.append(");");
  }
}
