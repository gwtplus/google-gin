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

import com.google.gwt.inject.rebind.util.KeyUtil;
import com.google.gwt.inject.rebind.util.MemberCollector;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.inject.Inject;

/**
 * A binding that just calls {@code GWT.create()} for the requested type.
 * This is the default binding for interfaces or classes that don't have
 * a non-default constructor annotated with {@code @Inject}.
 */
public class CallGwtDotCreateBinding extends CreatorBinding {
  @Inject
  public CallGwtDotCreateBinding(@InjectionPoint MemberCollector memberCollector,
      SourceWriteUtil sourceWriteUtil, KeyUtil keyUtil) {
    super(memberCollector, sourceWriteUtil, keyUtil);
  }

  @Override protected final void appendCreationStatement(StringBuilder sb) {
    String name = getTypeNameToCreate();
    sb.append("GWT.create(")
        .append(name)
        .append(".class);");
  }

  protected String getTypeNameToCreate() {
    return getClassType().getQualifiedSourceName();
  }
}
