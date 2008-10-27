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

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.inject.rebind.KeyUtil;
import com.google.gwt.inject.rebind.MethodCollector;
import com.google.gwt.inject.rebind.SourceWriteUtil;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.inject.Inject;

/**
 * A binding that just calls {@code GWT.create()} for the requested type.
 * This is the default binding for interfaces or classes that don't have
 * a non-default constructor annotated with {@code @Inject}.
 */
public class CallGwtDotCreateBinding extends CreatorBinding {

  /**
   * Suffix that is appended to the name of a GWT-RPC service interface to build
   * the name of the asynchronous proxy interface.
   */
  private static final String ASYNC_SERVICE_PROXY_SUFFIX = "Async";

  private final GeneratorContext ctx;

  @Inject
  public CallGwtDotCreateBinding(@Injectables MethodCollector methodCollector,
      SourceWriteUtil sourceWriteUtil, KeyUtil keyUtil, GeneratorContext ctx) {
    super(methodCollector, sourceWriteUtil, keyUtil);
    this.ctx = ctx;
  }

  @Override protected void appendCreationStatement(StringBuilder sb) {
    // Special case: When injecting a remote service proxy call GWT.create on
    // the synchronous service interface
    String name = getClassType().getQualifiedSourceName();
    if (getClassType().isInterface() != null && name.endsWith(ASYNC_SERVICE_PROXY_SUFFIX)) {
      String serviceInterfaceName =
          name.substring(0, name.length() - ASYNC_SERVICE_PROXY_SUFFIX.length());
      TypeOracle typeOracle = ctx.getTypeOracle();
      JClassType serviceInterface = typeOracle.findType(serviceInterfaceName);
      JClassType marker = typeOracle.findType(RemoteService.class.getName());
      if (serviceInterface != null && marker != null && serviceInterface.isAssignableTo(marker)) {
        name = serviceInterface.getQualifiedSourceName();
      }
    }

    sb.append("GWT.create(")
        .append(name)
        .append(".class);");
  }
}
