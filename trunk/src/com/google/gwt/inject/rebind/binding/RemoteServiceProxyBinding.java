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

import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.inject.rebind.util.GuiceUtil;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.inject.TypeLiteral;

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

  public static boolean isRemoteServiceProxy(TypeLiteral<?> type) {
    Class<?> rawType = type.getRawType();
    return rawType.isInterface()
        && rawType.getName().endsWith(ASYNC_SERVICE_PROXY_SUFFIX)
        && hasSynchronousServiceInterface(type);
  }

  RemoteServiceProxyBinding(GuiceUtil guiceUtil, TypeLiteral<?> type) {
    super(guiceUtil, type, Context.format("Implicit remote service proxy binding for %s", type));
  }

  @Override
  protected String getTypeNameToCreate() throws NoSourceNameException {
    if (!hasSynchronousServiceInterface(getType())) {
      throw new RuntimeException("Failed to load synchronous service for " + getTypeName());
    }

    return ReflectUtil.getSourceName(getSynchronousServiceClass(getType()));
  }

  @Override
  protected String getExpectedTypeName() throws NoSourceNameException {
    return super.getTypeNameToCreate();
  }

  private static boolean hasSynchronousServiceInterface(TypeLiteral<?> type) {
    Class<?> synchronousType = getSynchronousServiceClass(type);
    return synchronousType != null && RemoteService.class.isAssignableFrom(synchronousType);
  }

  private static Class<?> getSynchronousServiceClass(TypeLiteral<?> type) {
    String name = type.getRawType().getName();
    String serviceInterfaceName =
        name.substring(0, name.length() - ASYNC_SERVICE_PROXY_SUFFIX.length());
    try {
      return RemoteServiceProxyBinding.class.getClassLoader().loadClass(serviceInterfaceName);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }
}
