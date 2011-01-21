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

import com.google.gwt.inject.rebind.binding.BindingContext;
import com.google.gwt.inject.rebind.binding.BindingIndex;
import com.google.gwt.inject.rebind.binding.Injectable;
import com.google.gwt.inject.rebind.reflect.FieldLiteral;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Helper object for source writing.
 */
@Singleton
public class SourceWriteUtil {

  private final GuiceUtil guiceUtil;
  private final NameGenerator nameGenerator;
  private final MemberCollector memberCollector;
  private final BindingIndex bindingIndex;

  @Inject
  protected SourceWriteUtil(GuiceUtil guiceUtil, NameGenerator nameGenerator,
      @Injectable MemberCollector memberCollector, BindingIndex bindingIndex) {
    this.guiceUtil = guiceUtil;
    this.nameGenerator = nameGenerator;
    this.memberCollector = memberCollector;
    this.bindingIndex = bindingIndex;
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
  public String appendFieldInjection(SourceWriter sourceWriter, Iterable<FieldLiteral<?>> fields,
      String injecteeName) throws NoSourceNameException {

    StringBuilder methodInvocations = new StringBuilder();

    for (FieldLiteral<?> field : fields) {
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
  public String createFieldInjection(SourceWriter sourceWriter, FieldLiteral<?> field, 
      String injecteeName) throws NoSourceNameException {
    boolean hasInjectee = injecteeName != null;
    Class<?> fieldDeclaringType = field.getRawDeclaringType();
    boolean isPublic = field.isPublic() && ReflectUtil.isPublic(field.getDeclaringType());

    // Determine method signature parts.
    String injecteeTypeName = ReflectUtil.getSourceName(fieldDeclaringType);
    String fieldTypeName = ReflectUtil.getSourceName(field.getFieldType());
    String methodBaseName = nameGenerator.convertToValidMemberName(injecteeTypeName + "_"
        + field.getName() + "_fieldInjection");
    String methodName = nameGenerator.createMethodName(methodBaseName);
    String signatureParams = fieldTypeName + " value";
    String callParams = nameGenerator.getGetterMethodName(guiceUtil.getKey(field)) + "()";

    if (hasInjectee) {
      signatureParams = injecteeTypeName + " injectee, " + signatureParams;
      callParams = injecteeName + ", " + callParams;
    }

    // Compose method implementation and invocation.
    String signature = "private" + (isPublic ? "" : " native") + " void " + methodName + "("
        + signatureParams + ")";

    String call = methodName + "(" + callParams + ");";

    if (isPublic) {
      String body =
          (hasInjectee ? "injectee." : injecteeTypeName + ".") + field.getName() + " = value;";
      writeMethod(sourceWriter, signature, body);
    } else {
      String body = (hasInjectee ? "injectee." : "") + getJsniSignature(field) + " = value;";
      writeNativeMethod(sourceWriter, signature, body);
    }

    return call;
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
      Iterable<? extends MethodLiteral<?, ?>> methods, String injecteeName)
      throws NoSourceNameException {

    StringBuilder methodInvocations = new StringBuilder();

    for (MethodLiteral<?, ?> method : methods) {
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
  public String createConstructorInjection(SourceWriter sourceWriter,
      MethodLiteral<?, Constructor<?>> constructor) throws NoSourceNameException {
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
  public String createMethodCallWithInjection(SourceWriter sourceWriter, MethodLiteral<?, ?> method,
      String injecteeName) throws NoSourceNameException {
    String[] params = new String[method.getParameterTypes().size()];
    return createMethodCallWithInjection(sourceWriter, method, injecteeName, params);
  }

  /**
   * Appends a new method to the {@code sourceWriter} that calls the passed
   * method and returns a string that invokes the written method.  The written
   * method returns the passed method's return value, if any.
   * <p/>
   * If a method without parameters is provided, that method will be called and
   * no parameters will be passed. If the passed method declared any checked
   * exceptions, the generated method will catch and rethrow those as
   * {@link com.google.gwt.inject.client.CreationException}.
   *
   * @param sourceWriter writer to which the injecting method is written
   * @param method method to call (can be constructor)
   * @param injecteeName variable that references the object into which values
   *          are injected, in the context of the returned call string. If null
   *          all passed methods are called as static/constructors.
   * @param parameterNames array with parameter names that can replace getter
   *          methods (usually used to fetch injected values) in the returned
   *          string. The array length must match the number of method
   *          parameters. A {@code null} value denotes that the getter method
   *          should be used.
   * @return string calling the generated method
   */
  public String createMethodCallWithInjection(SourceWriter sourceWriter, MethodLiteral<?, ?> method,
      String injecteeName, String[] parameterNames) throws NoSourceNameException {
    boolean hasInjectee = injecteeName != null;
    Class<?> methodDeclaringType = method.getRawDeclaringType();
    boolean isPublic = method.isPublic() && ReflectUtil.isPublic(method.getDeclaringType());
    boolean isThrowing = hasCheckedExceptions(method);

    // Determine method signature parts.
    String injecteeTypeName = ReflectUtil.getSourceName(methodDeclaringType);
    String methodBaseName = nameGenerator.convertToValidMemberName(injecteeTypeName + "_"
        + method.getName() + "_methodInjection");
    String methodName = nameGenerator.createMethodName(methodBaseName);
    TypeLiteral<?> returnType = method.getReturnType();
    String returnTypeString = ReflectUtil.getSourceName(returnType);
    boolean returning = !returnType.getRawType().equals(Void.TYPE);

    // Collect method parameters to be passed to the native and actual method.
    int invokerParamCount = method.getParameterTypes().size() + (hasInjectee ? 1 : 0);
    List<String> invokerCallParams = new ArrayList<String>(invokerParamCount);
    List<String> invokerSignatureParams = new ArrayList<String>(invokerParamCount);
    List<String> invokeeCallParams = new ArrayList<String>(method.getParameterTypes().size());

    if (hasInjectee) {
      invokerCallParams.add(injecteeName);
      invokerSignatureParams.add(injecteeTypeName + " injectee");
    }

    int paramCount = 0;
    for (Key<?> paramKey : method.getParameterKeys()) {
      String paramName = ReflectUtil.formatParameterName(paramCount);
      if (parameterNames[paramCount] != null) {
        invokerCallParams.add(parameterNames[paramCount]);
      } else {
        invokerCallParams.add(nameGenerator.getGetterMethodName(paramKey) + "()");
      }

      // We cannot use the type literal of the key here: It is canonicalized
      // during key creation, destroying some information, for example
      // auto-boxing any primitives. This leads to type-mismatches when calling
      // into JSNI. Instead we'll access the parameter's original type.
      TypeLiteral<?> paramLiteral = method.getParameterTypes().get(paramCount);
      invokerSignatureParams.add(ReflectUtil.getSourceName(paramLiteral) + " " + paramName);
      invokeeCallParams.add(paramName);
      paramCount++;
    }

    // Compose method implementation and invocation.
    String invokerSignature = "private " + (isPublic ? "" : "native ") + returnTypeString + " "
        + methodName + "(" + join(", ", invokerSignatureParams) + ")";

    String invokerCall = methodName + "(" + join(", ", invokerCallParams) + ");";

    StringBuilder invokerBody = new StringBuilder();
    if (isThrowing) {
      invokerBody.append("try {\n  ");
    }

    if (returning) {
      invokerBody.append("return ");
    }
    if (isPublic) {
      if (hasInjectee) {
        invokerBody.append("injectee.").append(method.getName());
      } else if (method.isConstructor()) {
        invokerBody.append("new ").append(injecteeTypeName);
      } else {
        invokerBody.append(injecteeTypeName).append(".").append(method.getName());
      }
    } else {
      if (hasInjectee) {
        invokerBody.append("injectee.");
      }
      invokerBody.append(getJsniSignature(method));
    }
    invokerBody.append("(").append(join(", ", invokeeCallParams)).append(");");

    if (isThrowing) {
      if (isPublic) {
        invokerBody.append("\n} catch (Exception e) {\n")
            .append("  throw new com.google.gwt.inject.client.CreationException(e);\n")
            .append("}");
      } else {
        invokerBody.append("\n} catch (e) {\n")
            .append("  throw @com.google.gwt.inject.client.CreationException")
            .append("::new(Ljava/lang/Throwable;)(e);\n")
            .append("}");
      }
    }

    if (isPublic) {
      writeMethod(sourceWriter, invokerSignature, invokerBody.toString());
    } else {
      writeNativeMethod(sourceWriter, invokerSignature, invokerBody.toString());
    }

    return invokerCall;
  }

  private boolean hasCheckedExceptions(MethodLiteral<?, ?> method) {
    return method.getExceptionTypes().size() > 0;
  }

  /**
   * Writes out a binding context, followed by a newline.
   *
   * <p>Binding contexts may contain newlines; this routine translates those for
   * the SourceWriter to ensure that indents, Javadoc comments, etc are handled
   * properly.
   */
  public void writeBindingContext(SourceWriter writer, BindingContext context) {
    // Avoid a trailing \n -- the GWT class source file composer will output an
    // ugly extra newline if we do that.
    String text = context.toString();
    boolean first = true;
    for (String line : text.split("\n")) {
      if (first) {
        first = false;
      } else {
        writer.println();
      }
      // Indent the line relative to its current location.  writer.indent()
      // won't work, since it does the wrong thing in Javadoc.
      writer.print("  ");
      writer.print(line);
    }
  }

  /**
   * Write a Javadoc comment for a binding, including its context.
   *
   * @param description The description of the binding printed before its
   *     location, such as "Foo bound at: "
   * @param writer The writer to use in displaying the context.
   * @param bindingContext The context of the binding.
   */
  public void writeBindingContextJavadoc(SourceWriter writer, BindingContext bindingContext,
      String description) {
    writer.beginJavaDocComment();
    writer.println(description);
    writeBindingContext(writer, bindingContext);
    writer.endJavaDocComment();
  }

  /**
   * Write the Javadoc for the binding of a particular key, showing the context
   * of the binding.
   *
   * @param key The bound key.
   * @param writer The writer to use to write this comment.
   * @param bindingContext The context of the binding.
   */
  public void writeBindingContextJavadoc(SourceWriter writer, BindingContext bindingContext,
      Key key) {
    writeBindingContextJavadoc(writer, bindingContext,
        "Binding for " + key.getTypeLiteral() + " declared at:");
  }

  /**
   * Writes a method with the given signature and body to the source writer.
   *
   * @param writer writer that the method is written to
   * @param signature method's signature
   * @param body method's body
   */
  public void writeMethod(SourceWriter writer, String signature, String body) {
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

  /**
   * Appends a full member injection (methods and fields) to the provided
   * writer.
   *
   * @param writer source writer to write to
   * @param key key for which the injection is performed
   * @return name of the method created
   */
  public String appendMemberInjection(SourceWriter writer, Key<?> key)
      throws NoSourceNameException {
    TypeLiteral<?> type = key.getTypeLiteral();
    String memberInjectMethodName = nameGenerator.getMemberInjectMethodName(key);

    StringBuilder sb = new StringBuilder();

    sb.append(createMethodInjection(writer, getMethodsToInject(type), "injectee"));
    sb.append(appendFieldInjection(writer, getFieldsToInject(type), "injectee"));

    writeMethod(writer,
        "private void " + memberInjectMethodName +
            "(" + ReflectUtil.getSourceName(type) + " injectee)",
        sb.toString());

    return memberInjectMethodName;
  }

  private String getJsniSignature(MethodLiteral<?, ?> method) throws NoSourceNameException {
    StringBuilder signature = new StringBuilder();
    signature.append("@");
    signature.append(ReflectUtil.getSourceName(method.getDeclaringType()));

    String name = method.isConstructor() ? "new" : method.getName();
    signature.append("::").append(name).append("(");

    for (TypeLiteral<?> param : method.getParameterTypes()) {
      signature.append(getJniSignature(param.getType()));
    }

    signature.append(")");

    return signature.toString();
  }

  private String getJniSignature(Type type) throws NoSourceNameException {
    if (type instanceof Class<?>) {
      if (((Class) type).isPrimitive()) {
        if (type.equals(Boolean.TYPE)) {
          return "Z";
        } else if (type.equals(Byte.TYPE)) {
          return "B";
        } else if (type.equals(Character.TYPE)) {
          return "C";
        } else if (type.equals(Double.TYPE)) {
          return "D";
        } else if (type.equals(Float.TYPE)) {
          return "F";
        } else if (type.equals(Integer.TYPE)) {
          return "I";
        } else if (type.equals(Long.TYPE)) {
          return "J";
        } else if (type.equals(Short.TYPE)) {
          return "S";
        }
      }

      return "L" + getBinaryName((Class) type) + ";";
    }

    if (type instanceof GenericArrayType) {
      return "[" + getJniSignature(((GenericArrayType) type).getGenericComponentType());
    }

    if (type instanceof ParameterizedType) {
      return getJniSignature(((ParameterizedType) type).getRawType());
    }

    if (type instanceof WildcardType) {

      // TODO(schmitt): This is likely incorrect in some cases.
      return getJniSignature(((WildcardType) type).getUpperBounds()[0]);
    }

    throw new NoSourceNameException(type);
  }

  private String getBinaryName(Class<?> type) {
    List<String> classes = new ArrayList<String>();
    for (Class<?> clazz = type; clazz != null; clazz = clazz.getEnclosingClass()) {
      classes.add(clazz.getSimpleName());
    }
    Collections.reverse(classes);

    String packageName = type.getPackage().getName().replace('.', '/');
    if (packageName.length() > 0) {
      packageName += "/";
    }
    return packageName + join("$", classes);
  }

  private String getJsniSignature(FieldLiteral<?> field) throws NoSourceNameException {
    StringBuilder signature = new StringBuilder();
    signature.append("@");
    signature.append(ReflectUtil.getSourceName(field.getDeclaringType()));
    signature.append("::").append(field.getName());
    return signature.toString();
  }

  // TODO(schmitt): Move to a better location.
  public static CharSequence join(CharSequence delimiter, Iterable<? extends CharSequence> list) {
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

  private Set<FieldLiteral<?>> getFieldsToInject(TypeLiteral<?> type) {
    // Only inject fields that are non optional or where the key is bound.
    Set<FieldLiteral<?>> fields = new HashSet<FieldLiteral<?>>();
    for (FieldLiteral<?> field : memberCollector.getFields(type)) {
      if (!guiceUtil.isOptional(field) || bindingIndex.isBound(guiceUtil.getKey(field))) {
        fields.add(field);
      }
    }
    return fields;
  }

  private Set<MethodLiteral<?, Method>> getMethodsToInject(TypeLiteral<?> type) {
    Set<MethodLiteral<?, Method>> methods = new HashSet<MethodLiteral<?, Method>>();
    for (MethodLiteral<?, Method> method : memberCollector.getMethods(type)) {
      if (shouldInject(method)) {
        methods.add(method);
      }
    }
    return methods;
  }

  private boolean shouldInject(MethodLiteral<?, Method> method) {
    // Only inject methods that are non optional or where all keys are bound.
    if (guiceUtil.isOptional(method)) {
      for (Key<?> paramKey : method.getParameterKeys()) {
        if (!bindingIndex.isBound(paramKey)) {
          return false;
        }
      }
    }

    return true;
  }
}
