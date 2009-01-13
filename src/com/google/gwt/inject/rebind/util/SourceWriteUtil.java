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
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
   * Appends a field injecting method for each passed field to the
   * {@code sourceWriter} and returns a string that invokes all written
   * methods.
   *
   * @param sourceWriter writer to which the injecting method is written
   * @param fields fields to be injected
   * @param injecteeName variable that references the object into which values
   *          are injected, in the context of the returned call string
   * @return string calling the generated method
   */
  public String appendFieldInjection(SourceWriter sourceWriter, Iterable<JField> fields,
      String injecteeName) {

    StringBuilder methodInvocations = new StringBuilder();

    for (JField field : fields) {
      methodInvocations.append(createFieldInjection(sourceWriter, field, injecteeName))
          .append("\n");
    }

    return methodInvocations.toString();
  }

  /**
   * Appends a field injecting method to the {@code sourceWriter} and returns a
   * string that invokes the written method.
   *
   * @param sourceWriter writer to which the injecting method is written
   * @param field field to be injected
   * @param injecteeName variable that references the object into which values
   *          are injected, in the context of the returned call string
   * @return string calling the generated method
   */
  public String createFieldInjection(SourceWriter sourceWriter, JField field, String injecteeName) {
    boolean hasInjectee = injecteeName != null;

    // Determine native method signature parts.
    String injecteeTypeName
        = nameGenerator.binaryNameToSourceName(field.getEnclosingType().getQualifiedSourceName());
    String fieldTypeName
        = nameGenerator.binaryNameToSourceName(field.getType().getQualifiedSourceName());
    String nativeName = nameGenerator.convertToValidMemberName(injecteeTypeName + "_"
        + field.getName() + "_fieldInjection");
    String nativeMethodName = nameGenerator.createMethodName(nativeName);
    String nativeSignatureParams = fieldTypeName + " value";
    String nativeCallParams = nameGenerator.getGetterMethodName(keyUtil.getKey(field)) + "()";

    if (hasInjectee) {
      nativeSignatureParams = injecteeTypeName + " injectee, " + nativeSignatureParams;
      nativeCallParams = injecteeName + ", " + nativeCallParams;
    }

    // Compose method implementation and invocation.

    String nativeSignature = "private native void " + nativeMethodName + "("
        + nativeSignatureParams + ")";

    String nativeCall = nativeMethodName + "(" + nativeCallParams + ");";

    String nativeBody = (hasInjectee ? "injectee." : "") + getJsniSignature(field) + " = value;";

    writeNativeMethod(sourceWriter, nativeSignature, nativeBody);

    return nativeCall;
  }

  /**
   * Appends a method injecting method to the {@code sourceWriter} and returns
   * a string that invokes the written method.  The values for the passed
   * method's parameters are retrieved through the
   * {@link com.google.gwt.inject.client.Ginjector}.
   * <p/>
   * If the passed method collection contains only one actual method, the native
   * method will pass on (i.e. return) the result of the actual method's
   * invocation, if any.
   * <p/>
   * The passed method collection can contain constructors (they'll be treated
   * correctly) if no {@code injecteeName} is passed.  The same applies for
   * static methods.
   * <p/>
   * If a method without parameters is provided, that method will be called and
   * no parameters will be passed.
   *
   * @param sourceWriter writer to which the injecting method is written
   * @param methods methods to be called & injected
   * @param injecteeName variable that references the object into which values
   *            are injected, in the context of the returned call string. If
   *            {@code null} all passed methods are called as static/constructors.
   * @return string calling the generated method
   */
  public String createMethodInjection(SourceWriter sourceWriter,
      Iterable<? extends JAbstractMethod> methods, String injecteeName) {

    StringBuilder methodInvocations = new StringBuilder();

    for (JAbstractMethod method : methods) {
      methodInvocations.append(createMethodCallWithInjection(sourceWriter, method, injecteeName))
          .append("\n");
    }

    return methodInvocations.toString();
  }

  /**
   * Appends a constructor injecting method to the {@code sourceWriter} and
   * returns a string that invokes the written method.  The written method
   * returns the constructed object.
   *
   * @param sourceWriter writer to which the injecting method is written
   * @param constructor constructor to call
   * @return string calling the generated method
   */
  public String createConstructorInjection(SourceWriter sourceWriter, JConstructor constructor) {
    return createMethodCallWithInjection(sourceWriter, constructor, null);
  }

  /**
   * Appends a new method to the {@code sourceWriter} that calls the passed
   * method and returns a string that invokes the written method.  The written
   * method returns the passed method's return value, if any.
   * <p/>
   * If a method without parameters is provided, that method will be called and
   * no parameters will be passed.
   *
   * @param sourceWriter writer to which the injecting method is written
   * @param method method to call (can be constructor)
   * @param injecteeName variable that references the object into which values
   *          are injected, in the context of the returned call string. If null
   *          all passed methods are called as static/constructors.
   * @return string calling the generated method
   */
  public String createMethodCallWithInjection(SourceWriter sourceWriter, JAbstractMethod method,
      String injecteeName) {
    boolean returning = false;
    boolean hasInjectee = injecteeName != null;

    // Determine native method signature parts.
    String injecteeTypeName
        = nameGenerator.binaryNameToSourceName(method.getEnclosingType().getQualifiedSourceName());
    String nativeName = nameGenerator.convertToValidMemberName(injecteeTypeName + "_"
        + method.getName() + "_methodInjection");
    String nativeMethodName = nameGenerator.createMethodName(nativeName);
    String nativeReturnType = "void";
    if (method.isConstructor() != null) {
      nativeReturnType = injecteeTypeName;
      returning = true;
    } else {
      JType returnType = ((JMethod) method).getReturnType();
      if (returnType != JPrimitiveType.VOID) {
        nativeReturnType
            = nameGenerator.binaryNameToSourceName(returnType.getQualifiedSourceName());
        returning = true;
      }
    }

    // Collect method parameters to be passed to the native and actual method.
    int nativeParamCount = method.getParameters().length + (hasInjectee ? 1 : 0);
    List<String> nativeCallParams = new ArrayList<String>(nativeParamCount);
    List<String> nativeSignatureParams = new ArrayList<String>(nativeParamCount);
    List<String> methodCallParams = new ArrayList<String>(method.getParameters().length);

    if (hasInjectee) {
      nativeCallParams.add(injecteeName);
      nativeSignatureParams.add(injecteeTypeName + " injectee");
    }

    int paramCount = 0;
    for (JParameter param : method.getParameters()) {
      String paramName = "_" + paramCount;
      nativeCallParams.add(nameGenerator.getGetterMethodName(keyUtil.getKey(param)) + "()");
      nativeSignatureParams.add(
          nameGenerator.binaryNameToSourceName(param.getType().getQualifiedSourceName()) + " "
              + paramName);
      methodCallParams.add(paramName);
      paramCount++;
    }

    // Compose method implementation and invocation.
    String nativeSignature = "private native " + nativeReturnType + " " + nativeMethodName
        + "(" + join(", ", nativeSignatureParams) + ")";

    String nativeCall = nativeMethodName + "(" + join(", ", nativeCallParams) + ");";

    String nativeBody = (returning ? "return " : "") + (hasInjectee ? "injectee." : "")
        + getJsniSignature(method) + "(" + join(", ", methodCallParams) + ");";

    writeNativeMethod(sourceWriter, nativeSignature, nativeBody);

    return nativeCall;
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

  private String getJsniSignature(JAbstractMethod method) {

    StringBuilder signature = new StringBuilder();

    signature.append("@");

    signature.append(nameGenerator.binaryNameToSourceName(
        method.getEnclosingType().getQualifiedSourceName()));

    String name = method instanceof JConstructor ? "new" : method.getName();
    signature.append("::").append(name).append("(");

    for (JParameter param : method.getParameters()) {
      signature.append(param.getType().getJNISignature());
    }

    signature.append(")");

    return signature.toString();
  }

  private String getJsniSignature(JField field) {

    StringBuilder signature = new StringBuilder();

    signature.append("@");

    signature.append(nameGenerator.binaryNameToSourceName(
        field.getEnclosingType().getQualifiedSourceName()));

    signature.append("::").append(field.getName());

    return signature.toString();
  }

  private static CharSequence join(CharSequence delimiter, Iterable<? extends CharSequence> list) {
    Iterator<? extends CharSequence> it = list.iterator();
    if (it.hasNext()) {
      StringBuilder sb = new StringBuilder(it.next());
      while (it.hasNext()) {
        sb.append(delimiter);
        sb.append(it.next());
      }
      return sb.toString();
    }
    return "";
  }
}
