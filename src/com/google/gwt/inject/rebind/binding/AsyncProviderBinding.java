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

import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Inject;
import com.google.inject.Key;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;

/**
 * Binding implementation for {@code AsyncProvider<T>} that generates
 * the following code for the provider:
 * 
 * <pre style=code>
 *   return new %provider_name% () {
 *      public void get(final AsyncCallback<%object_to_create%> callback) {
 *        GWT.runAsync(new RunAsyncCallback() {
 *          public void onSuccess() {
 *            callback.onSuccess(%provider_of_object_to_create%.get());
 *          }
 *          public void onFailure(Throwable ex) {
 *            callback.onFailure(ex);
 *          }
 *        }
 *      }
 *   }
 * 
 * </pre>
 */
public class AsyncProviderBinding implements Binding {

  private final NameGenerator nameGenerator;
  private final SourceWriteUtil sourceWriteUtil;

  private ParameterizedType providerType;
  private Key<?> targetKey;

  @Inject
  public AsyncProviderBinding(NameGenerator nameGenerator, SourceWriteUtil sourceWriteUtil) {
    this.nameGenerator = nameGenerator;
    this.sourceWriteUtil = sourceWriteUtil;
  }

  public void setProviderKey(Key<?> providerKey) {
    providerType = (ParameterizedType) providerKey.getTypeLiteral().getType();

    // Pass any binding annotation on the Provider to the thing we create
    Type targetType = providerType.getActualTypeArguments()[0];
    targetKey = getKeyWithSameAnnotation(targetType, providerKey);
  }

  public void writeCreatorMethods(SourceWriter writer, String creatorMethodSignature)
      throws NoSourceNameException {
     assert (providerType != null);

     String providerTypeName = ReflectUtil.getSourceName(providerType);
     String targetKeyName = ReflectUtil.getSourceName(targetKey.getTypeLiteral());

     StringBuilder methodCode = new StringBuilder()
       .append("return new ").append(providerTypeName).append("() { \n") 
       .append("    public void get(")
       .append("final com.google.gwt.user.client.rpc.AsyncCallback<? super ")
       .append(targetKeyName).append("> callback) { \n")
       .append("      com.google.gwt.core.client.GWT.runAsync(")
       .append(targetKey.getTypeLiteral().getRawType().getCanonicalName())
       .append(".class,")
       .append("new com.google.gwt.core.client.RunAsyncCallback() { \n")
       .append("        public void onSuccess() { \n")
       .append("          callback.onSuccess(")
       .append(nameGenerator.getGetterMethodName(targetKey)).append("()); \n")
       .append("        }\n")
       .append("        public void onFailure(Throwable ex) { \n ")
       .append("          callback.onFailure(ex); \n" )
       .append("        } \n")
       .append("    }); \n")
       .append("    }\n")
       .append(" };\n");

     sourceWriteUtil.writeMethod(writer, creatorMethodSignature, methodCode.toString());
  }

  public RequiredKeys getRequiredKeys() {
    assert (targetKey != null);
    return new RequiredKeys(Collections.<Key<?>>singleton(targetKey));
  }

  private Key<?> getKeyWithSameAnnotation(Type keyType, Key<?> baseKey) {
    Annotation annotation = baseKey.getAnnotation();
    if (annotation != null) {
      return Key.get(keyType, annotation);
    }

    Class<? extends Annotation> annotationType = baseKey.getAnnotationType();
    if (annotationType != null) {
      return Key.get(keyType, annotationType);
    }

    return Key.get(keyType);
  }
}
