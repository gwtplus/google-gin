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

package com.google.gwt.inject.rebind.util;

import com.google.gwt.core.ext.typeinfo.JAbstractMethod;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Collection;

/**
 * Simple helper object for source writing.
 */
@Singleton
public class SourceWriteUtil {

  private final KeyUtil keyUtil;
  private final NameGenerator nameGenerator;

  @Inject
  protected SourceWriteUtil(KeyUtil keyUtil, NameGenerator nameGenerator) {
    this.keyUtil = keyUtil;
    this.nameGenerator = nameGenerator;
  }

  /**
   * Appends a method invocation call to the provided string builder.  The
   * value for each parameter is retrieved by writing a call to the parameter
   * type's getter method
   * (see {@link NameGenerator#getGetterMethodName(com.google.inject.Key)}).
   * The written call is terminated with a semicolon.
   *
   * @param sb string builder
   * @param method method that gets called
   */
  public void appendInvoke(StringBuilder sb, JAbstractMethod method) {
    String name = method.getName();
    if (method instanceof JConstructor) {
      name = method.getEnclosingType().getQualifiedSourceName();
    }

    sb.append(name);

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

  /**
   * Appends a field injecting method to the {@code sourceWriter} and returns
   * a string that invokes the written method.
   *
   * @param sourceWriter writer to which the injecting method is written
   * @param injecteeType type of the object into which fields are injected
   * @param fields fields to be injected
   * @param injecteeName variable that references the object into which values
   *          are injected, in the context of the returned call string
   * @return string calling the generated method
   */
  public String appendFieldInjection(SourceWriter sourceWriter, JClassType injecteeType,
      Collection<JField> fields, String injecteeName) {
    String nativeName = nameGenerator.convertToValidMemberName(injecteeType + "_native");
    String nativeMethodName = nameGenerator.createMethodName(nativeName);

    StringBuilder paramsCall = new StringBuilder();
    paramsCall.append(injecteeName);
    StringBuilder paramsSignature = new StringBuilder();
    paramsSignature.append(injecteeType.getQualifiedSourceName())
        .append(" result");

    int i = 0;
    StringBuilder body = new StringBuilder();
    for (JField field : fields) {

      // Register parameter
      paramsSignature.append(", ")
          .append(field.getType().getParameterizedQualifiedSourceName())
          .append(" _")
          .append(++i);
      paramsCall.append(", ")
          .append(nameGenerator.getGetterMethodName(keyUtil.getKey(field)))
          .append("()");

      // Access field.
      body.append("result.@")
          .append(nameGenerator
              .binaryNameToSourceName(field.getEnclosingType().getQualifiedSourceName()))
          .append("::")
          .append(field.getName())

              // Assign value from parameter.
          .append(" = _")
          .append(i)
          .append(";");
    }

    writeNativeMethod(sourceWriter,
        "private native void " + nativeMethodName + "(" + paramsSignature + ")", body.toString());

    return nativeMethodName + "(" + paramsCall + ");";
  }

  /**
   * Writes a method with the given signature and body to the source writer.
   *
   * @param writer writer that the method is written to
   * @param signature method's signature
   * @param body method's body
   */
  // Static for access from BindConstantBinding.
  // TODO(schmitt):  Make BCB non-static to get rid of this.
  public static void writeMethod(SourceWriter writer, String signature, String body) {
    writer.println(signature + " {");
    writer.indent();
    writer.println(body);
    writer.outdent();
    writer.println("}");
    writer.println();
  }

  /**
   * Writes a native method with the given signature and body to the source
   * writer.
   *
   * @param writer writer that the method is written to
   * @param signature method's signature
   * @param body method's body
   */
  public void writeNativeMethod(SourceWriter writer, String signature, String body) {
    writer.println(signature + " /*-{");
    writer.indent();
    writer.println(body);
    writer.outdent();
    writer.println("}-*/;");
    writer.println();
  }
}
