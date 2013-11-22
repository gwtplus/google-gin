/*
 * Copyright 2011 Google Inc.
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

import static com.google.gwt.inject.rebind.util.SourceWriteUtil.join;

import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility code to create method and constructor calls.
 */
public class MethodCallUtil {

  /**
   * Creates a constructor injecting method and returns a string that invokes
   * the new method.  The new method returns the constructed object.
   *
   * @param constructor constructor to call
   * @param nameGenerator NameGenerator to be used for ensuring method name uniqueness
   * @param methodsOutput a list where all new methods created by this
   *     call are added
   * @return source snippet calling the generated method
   */
  public SourceSnippet createConstructorInjection(
      MethodLiteral<?, Constructor<?>> constructor, NameGenerator nameGenerator,
      List<InjectorMethod> methodsOutput) throws NoSourceNameException {
    return createMethodCallWithInjection(constructor, null, nameGenerator, methodsOutput);
  }

  /**
   * Creates a method that calls the passed method, injecting its parameters
   * using getters, and returns a string that invokes the new method.  The new
   * method returns the passed method's return value, if any.  If a method
   * without parameters is provided, that method will be called and no
   * parameters will be passed.
   *
   * @param method method to call (can be constructor)
   * @param invokeeName expression that evaluates to the object on which
   *     the method is to be called.  If null the method will be called
   *     in the current scope.
   * @param nameGenerator NameGenerator to be used for ensuring method name uniqueness
   * @param methodsOutput a list where all new methods created by this
   *     call are added
   * @return source snippet calling the generated method
   */
  public SourceSnippet createMethodCallWithInjection(MethodLiteral<?, ?> method,
      String invokeeName, NameGenerator nameGenerator, List<InjectorMethod> methodsOutput)
      throws NoSourceNameException {
    String[] params = new String[method.getParameterTypes().size()];
    return createMethodCallWithInjection(method, invokeeName, params, nameGenerator,
        methodsOutput);
  }

  /**
   * Creates a method that calls the passed method, injecting its parameters
   * using getters as necessary, and returns a string that invokes the new
   * method.  The new method returns the passed method's return value, if any.
   * If a method without parameters is provided, that method will be called and
   * no parameters will be passed. If the passed method declared any checked
   * exceptions, the generated method will catch and rethrow those as
   * {@link com.google.gwt.inject.client.CreationException}.
   *
   * @param method method to call (can be constructor)
   * @param invokeeName expression that evaluates to the object on which
   *     the method is to be called.  If null the method will be called
   *     in the current scope.
   * @param parameterNames array with parameter names that can replace getter
   *     methods (usually used to fetch injected values) in the returned
   *     string. The array length must match the number of method
   *     parameters. A {@code null} value denotes that the getter method
   *     should be used.
   * @param nameGenerator NameGenerator to use for ensuring method name uniqueness
   * @param methodsOutput a list where all new methods created by this
   *     call are added
   * @return string calling the generated method
   */
  public SourceSnippet createMethodCallWithInjection(MethodLiteral<?, ?> method,
      String invokeeName, String[] parameterNames, NameGenerator nameGenerator,
      List<InjectorMethod> methodsOutput) throws NoSourceNameException {

    boolean hasInvokee = invokeeName != null;
    boolean useNativeMethod = method.isPrivate() ||
        ReflectUtil.isPrivate(method.getDeclaringType());
    boolean isThrowing = hasCheckedExceptions(method);

    // Determine method signature parts.
    String invokeeTypeName = ReflectUtil.getSourceName(method.getRawDeclaringType());
    int invokerParamCount = method.getParameterTypes().size() + (hasInvokee ? 1 : 0);
    TypeLiteral<?> returnType = method.getReturnType();
    String returnTypeString = ReflectUtil.getSourceName(returnType);
    boolean returning = !returnType.getRawType().equals(Void.TYPE);

    String invokerMethodName = getInvokerMethodName(method, nameGenerator);
    // The invoker method is placed in the fragment of the package that declares
    // the method, so it has access to the same package-private types as the
    // method declaration.
    String invokerPackageName;

    if (useNativeMethod && !hasInvokee) {
      // In this case, the type of the invokee is not mentioned by the type
      // signature of the invoker, and since we're using native code, we can
      // write a call to the target method even if the invokee is fully private.
      //
      // This handles the case of a user statically injecting a private inner
      // class.
      //
      // Pick a package somewhat arbitrarily; since we're using native code, it
      // doesn't really matter where it goes.  The declaring type's package is
      // easy to get to:
      invokerPackageName = method.getDeclaringType().getRawType().getPackage().getName();
    } else {
      invokerPackageName = ReflectUtil.getUserPackageName(method.getDeclaringType());
      // TODO(dburrows): won't this silently fail if some *parameters* to the
      // invokee have limited visibility?  Currently I believe that we'll just
      // generate noncompiling code.
    }
    methodsOutput.add(createInvoker(invokeeName, invokeeTypeName, hasInvokee, useNativeMethod,
        isThrowing, invokerMethodName, invokerPackageName, invokerParamCount, method,
        returnTypeString, returning, isLongAccess(method)));

    return new InvokerCall(hasInvokee, invokeeName, invokerMethodName, invokerPackageName,
        invokerParamCount, method, parameterNames);
  }

  /**
   * Check whether a method needs to have Long access.
   */
  private boolean isLongAccess(MethodLiteral<?, ?> method) {
    boolean result = method.getReturnType().getRawType().equals(Long.TYPE);
    for (TypeLiteral<?> paramLiteral : method.getParameterTypes()) {
      result |= paramLiteral.getRawType().equals(Long.TYPE);
    }

    return result;
  }

  private static final class InvokerCall implements SourceSnippet {
    private final boolean hasInvokee;
    private final String invokeeName;
    private final String invokerMethodName;
    private final String invokerPackageName;
    private final int invokerParamCount;
    private final MethodLiteral<?, ?> method;
    private final String[] parameterNames;

    public InvokerCall(boolean hasInvokee, String invokeeName, String invokerMethodName,
        String invokerPackageName, int invokerParamCount, MethodLiteral<?, ?> method,
        String[] parameterNames) {
      this.hasInvokee = hasInvokee;
      this.invokeeName = invokeeName;
      this.invokerMethodName = invokerMethodName;
      this.invokerPackageName = invokerPackageName;
      this.invokerParamCount = invokerParamCount;
      this.method = method;
      this.parameterNames = parameterNames;
    }

    @Override
    public String getSource(InjectorWriteContext writeContext) {
      // Collect method parameters to be passed to the actual method.
      List<String> invokerCallParams = new ArrayList<String>(invokerParamCount);

      if (hasInvokee) {
        invokerCallParams.add(invokeeName);
      }

      int paramCount = 0;
      for (Key<?> paramKey : method.getParameterKeys()) {
        String paramName = ReflectUtil.formatParameterName(paramCount);
        if (parameterNames[paramCount] != null) {
          invokerCallParams.add(parameterNames[paramCount]);
        } else {
          invokerCallParams.add(writeContext.callGetter(paramKey));
        }

        paramCount++;
      }

      return writeContext.callMethod(invokerMethodName, invokerPackageName, invokerCallParams)
          + ";";
    }
  }

  /**
   * Create an invoker method.  See {@link #createMethodCallWithInjection}.
   */
  private InjectorMethod createInvoker(String invokeeName, String invokeeTypeName,
      boolean hasInvokee, boolean isNative, boolean isThrowing, String invokerMethodName,
      String invokerPackageName, int invokerParamCount, MethodLiteral<?, ?> method,
      String returnTypeString, boolean returning, boolean isLongAccess)
      throws NoSourceNameException {

    List<String> invokerSignatureParams = new ArrayList<String>(invokerParamCount);

    if (hasInvokee) {
      invokerSignatureParams.add(invokeeTypeName + " invokee");
    }

    List<String> invokeeCallParams = new ArrayList<String>(method.getParameterTypes().size());

    int paramCount = 0;
    for (Key<?> paramKey : method.getParameterKeys()) {
      String paramName = ReflectUtil.formatParameterName(paramCount);
      // We cannot use the type literal of the key here: It is canonicalized
      // during key creation, destroying some information, for example
      // auto-boxing any primitives. This leads to type-mismatches when calling
      // into JSNI. Instead we'll access the parameter's original type.
      TypeLiteral<?> paramLiteral = method.getParameterTypes().get(paramCount);
      invokerSignatureParams.add(ReflectUtil.getSourceName(paramLiteral) + " " + paramName);
      invokeeCallParams.add(paramName);
      paramCount++;
    }

    String annotation = isLongAccess ? "@com.google.gwt.core.client.UnsafeNativeLong " : "";

    String invokerSignature = annotation + "public " + (isNative ? "native " : "")
        + returnTypeString + " " + invokerMethodName + "(" + join(", ", invokerSignatureParams)
        + ")";

    return new InvokerMethod(hasInvokee, invokeeCallParams, invokeeTypeName, invokerPackageName,
        invokerSignature, isNative, isThrowing, method, returning, returnTypeString);
  }

  private static final class InvokerMethod extends AbstractInjectorMethod {
    private final boolean hasInvokee;
    private final List<String> invokeeCallParams;
    private final String invokeeTypeName;
    private final boolean isThrowing;
    private final MethodLiteral<?, ?> method;
    private final boolean returning;
    private final String returnTypeString;

    public InvokerMethod(boolean hasInvokee, List<String> invokeeCallParams, String invokeeTypeName,
        String invokerPackageName, String invokerSignature, boolean isNative, boolean isThrowing,
        MethodLiteral<?, ?> method, boolean returning, String returnTypeString) {
      super(isNative, invokerSignature, invokerPackageName);

      this.hasInvokee = hasInvokee;
      this.invokeeCallParams = invokeeCallParams;
      this.invokeeTypeName = invokeeTypeName;
      this.isThrowing = isThrowing;
      this.method = method;
      this.returning = returning;
      this.returnTypeString = returnTypeString;
    }

    public String getMethodBody(InjectorWriteContext writeContext) throws NoSourceNameException {
      StringBuilder result = new StringBuilder();

      if (isThrowing) {
        result.append("try {\n  ");
      }

      if (returning) {
        result.append("return ");
      }
      if (!isNative()) {
        if (hasInvokee) {
          result.append("invokee.").append(method.getName());
        } else if (method.isConstructor()) {
          result.append("new ").append(invokeeTypeName);
        } else {
          result.append(invokeeTypeName).append(".").append(method.getName());
        }
      } else {
        if (hasInvokee) {
          result.append("invokee.");
        }
        result.append(getJsniSignature(method));
      }
      result.append("(").append(join(", ", invokeeCallParams)).append(");");

      if (isThrowing) {
        if (!isNative()) {
          result.append("\n} catch (Exception e) {\n")
              .append("  throw new com.google.gwt.inject.client.CreationException(e);\n")
              .append("}");
        } else {
          result.append("\n} catch (e) {\n")
              .append("  throw @com.google.gwt.inject.client.CreationException")
              .append("::new(Ljava/lang/Throwable;)(e);\n")
              .append("}");
        }
      }

      return result.toString();
    }
  }

  /**
   * Get the name of an invoker method.  This has a side-effect: it returns a
   * newly unique value each time it is called, so be sure not to call it twice
   * for the same method call (you'll get two different names).
   */
  private String getInvokerMethodName(MethodLiteral<?, ?> method,
      NameGenerator nameGenerator) throws NoSourceNameException {
    Class<?> methodDeclaringType = method.getRawDeclaringType();
    String invokeeTypeName = ReflectUtil.getSourceName(methodDeclaringType);
    String methodBaseName = nameGenerator.convertToValidMemberName(
        String.format("%s_%s_methodInjection", invokeeTypeName, method.getName()));

    return nameGenerator.createMethodName(methodBaseName);
  }

  private boolean hasCheckedExceptions(MethodLiteral<?, ?> method) {
    return method.getExceptionTypes().size() > 0;
  }

  private static String getJsniSignature(MethodLiteral<?, ?> method) throws NoSourceNameException {
    StringBuilder signature = new StringBuilder();
    signature.append("@");
    signature.append(ReflectUtil.getSourceName(method.getRawDeclaringType()));

    String name = method.isConstructor() ? "new" : method.getName();
    signature.append("::").append(name).append("(");

    // Using raw parameter types here since JNI doesn't know about
    // parametrization at lookup time.
    for (Type param : method.getRawParameterTypes()) {
      signature.append(getJniSignature(param));
    }

    signature.append(")");

    return signature.toString();
  }

  private static String getJniSignature(Type type) throws NoSourceNameException {
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

    if (type instanceof TypeVariable) {

      // TODO(schmitt): This is likely incorrect in some cases.
      return getJniSignature(((TypeVariable) type).getBounds()[0]);
    }

    throw new NoSourceNameException(type);
  }

  private static String getBinaryName(Class<?> type) {
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
}
