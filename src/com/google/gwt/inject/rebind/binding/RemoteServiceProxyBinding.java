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
import com.google.gwt.inject.rebind.util.KeyUtil;
import com.google.gwt.inject.rebind.util.MemberCollector;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.inject.Inject;

/**
 * A binding that calls {@code GWT.create()} for the non-"Async" version
 * of the requested type.
 */
public class RemoteServiceProxyBinding extends CallGwtDotCreateBinding {
  /**
   * Suffix that is appended to the name of a GWT-RPC service interface to build
   * the name of the asynchronous proxy interface.
   */
  private static final String ASYNC_SERVICE_PROXY_SUFFIX = "Async";

  private final GeneratorContext ctx;

  @Inject
  public RemoteServiceProxyBinding(@Injectable MemberCollector memberCollector,
      SourceWriteUtil sourceWriteUtil, KeyUtil keyUtil, GeneratorContext ctx) {
    super(memberCollector, sourceWriteUtil, keyUtil);
    this.ctx = ctx;
  }

  @Override
  protected String getTypeNameToCreate() {
    String name = super.getTypeNameToCreate();
    String serviceInterfaceName =
        name.substring(0, name.length() - ASYNC_SERVICE_PROXY_SUFFIX.length());
    TypeOracle typeOracle = ctx.getTypeOracle();
    JClassType serviceInterface = typeOracle.findType(serviceInterfaceName);
    JClassType marker = typeOracle.findType(RemoteService.class.getName());
    if (serviceInterface != null && marker != null && serviceInterface.isAssignableTo(marker)) {
      name = serviceInterface.getQualifiedSourceName();
    }

    return name;
  }

  public static boolean isRemoteServiceProxy(JClassType type) {
    return type.isInterface() != null
        && type.getQualifiedSourceName().endsWith(ASYNC_SERVICE_PROXY_SUFFIX);
  }
}
