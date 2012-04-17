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

import com.google.gwt.inject.rebind.binding.BindingIndex;
import com.google.gwt.inject.rebind.binding.Context;
import com.google.gwt.inject.rebind.binding.Injectable;
import com.google.gwt.inject.rebind.reflect.FieldLiteral;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.Assisted;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Helper object for source writing.
 */
public class SourceWriteUtil {

  private final GuiceUtil guiceUtil;
  private final MemberCollector memberCollector;
  private final MethodCallUtil methodCallUtil;
  private final BindingIndex bindingIndex;

  @Inject
  protected SourceWriteUtil(GuiceUtil guiceUtil, @Injectable MemberCollector memberCollector,
      MethodCallUtil methodCallUtil, @Assisted BindingIndex bindingIndex) {
    this.guiceUtil = guiceUtil;
    this.memberCollector = memberCollector;
    this.methodCallUtil = methodCallUtil;
    this.bindingIndex = bindingIndex;
  }

  /**
   * Appends a field injecting method for each passed field to the
   * {@code sourceWriter} and returns a string that invokes all written
   * methods.
   *
   * @param fields fields to be injected
   * @param injecteeName variable that references the object into which values
   *          are injected, in the context of the returned call string
   * @return string calling the generated method
   */
  public SourceSnippet createFieldInjections(Iterable<FieldLiteral<?>> fields, String injecteeName,
      NameGenerator nameGenerator, List<InjectorMethod> methodsOutput)
      throws NoSourceNameException {

    SourceSnippetBuilder methodInvocations = new SourceSnippetBuilder();

    for (FieldLiteral<?> field : fields) {
      methodInvocations
          .append(createFieldInjection(field, injecteeName, nameGenerator, methodsOutput))
          .append("\n");
    }

    return methodInvocations.build();
  }

  /**
   * Creates a field injecting method and returns a string that invokes the
   * written method.
   *
   * @param field field to be injected
   * @param injecteeName variable that references the object into which values
   *          are injected, in the context of the returned call string
   * @param nameGenerator NameGenerator to be used for ensuring method name uniqueness
   * @return string calling the generated method
   */
  public SourceSnippet createFieldInjection(final FieldLiteral<?> field, final String injecteeName,
      NameGenerator nameGenerator, List<InjectorMethod> methodsOutput)
      throws NoSourceNameException {
    final boolean hasInjectee = injecteeName != null;
    final boolean useNativeMethod = field.isPrivate()
        || ReflectUtil.isPrivate(field.getDeclaringType())
        || field.isLegacyFinalField();

    // Determine method signature parts.
    final String injecteeTypeName = ReflectUtil.getSourceName(field.getRawDeclaringType());
    String fieldTypeName = ReflectUtil.getSourceName(field.getFieldType());
    String methodBaseName = nameGenerator.convertToValidMemberName(injecteeTypeName + "_"
        + field.getName() + "_fieldInjection");
    final String methodName = nameGenerator.createMethodName(methodBaseName);
    // Field injections are performed in the package of the class declaring the
    // field.  Any private types referenced by the injection must be visible
    // from there.
    final String packageName = ReflectUtil.getUserPackageName(field.getDeclaringType());

    String signatureParams = fieldTypeName + " value";
    boolean isLongAcccess = field.getFieldType().getRawType().equals(Long.TYPE);

    if (hasInjectee) {
        signatureParams = injecteeTypeName + " injectee, " + signatureParams;
    }

    // Compose method implementation and invocation.
    String annotation;
    if (isLongAcccess) {
      annotation = "@com.google.gwt.core.client.UnsafeNativeLong ";
    } else {
      annotation = "";
    }
    String header = useNativeMethod ? "public native " : "public ";
    String signature = annotation + header + "void " + methodName + "(" + signatureParams + ")";

    InjectorMethod injectionMethod =
        new AbstractInjectorMethod(useNativeMethod, signature, packageName) {
          public String getMethodBody(InjectorWriteContext writeContext)
              throws NoSourceNameException {
            if (!useNativeMethod) {
              return (hasInjectee
                  ? "injectee." : injecteeTypeName + ".") + field.getName() + " = value;";
            } else {
              return (hasInjectee ? "injectee." : "") + getJsniSignature(field) + " = value;";
            }
          }
        };
    methodsOutput.add(injectionMethod);

    return new SourceSnippet() {
      public String getSource(InjectorWriteContext writeContext) {
        List<String> callParams = new ArrayList<String>();
        if (hasInjectee) {
          callParams.add(injecteeName);
        }

        callParams.add(writeContext.callGetter(guiceUtil.getKey(field)));

        return writeContext.callMethod(methodName, packageName, callParams) + ";\n";
      }
    };
  }

  /**
   * Creates a method injecting method and returns a string that invokes the new
   * method.  The values for the passed method's parameters are retrieved
   * through the {@link com.google.gwt.inject.client.Ginjector}.
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
   * @param methods methods to be called & injected
   * @param injecteeName variable that references the object into which values
   *     are injected, in the context of the returned call string. If
   *     {@code null} all passed methods are called as static/constructors.
   * @param nameGenerator NameGenerator to be used for ensuring method name uniqueness
   * @param methodsOutput a list where all new methods created by this
   *     call are added
   * @return source snippet calling the generated method
   */
  public SourceSnippet createMethodInjections(Iterable<? extends MethodLiteral<?, ?>> methods,
      String injecteeName, NameGenerator nameGenerator, List<InjectorMethod> methodsOutput)
      throws NoSourceNameException {

    SourceSnippetBuilder methodInvocations = new SourceSnippetBuilder();

    for (MethodLiteral<?, ?> method : methods) {
      methodInvocations
          .append(methodCallUtil.createMethodCallWithInjection(method, injecteeName, nameGenerator,
              methodsOutput))
          .append("\n");
    }

    return methodInvocations.build();
  }

  /**
   * Writes out a binding context, followed by a newline.
   *
   * <p>Binding contexts may contain newlines; this routine translates those for
   * the SourceWriter to ensure that indents, Javadoc comments, etc are handled
   * properly.
   */
  public void writeBindingContext(SourceWriter writer, Context context) {
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
  public void writeBindingContextJavadoc(SourceWriter writer, Context bindingContext,
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
  public void writeBindingContextJavadoc(SourceWriter writer, Context bindingContext,
      Key<?> key) {
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
   * Writes the given method to the given source writer.
   */
  public void writeMethod(InjectorMethod method, SourceWriter writer,
      InjectorWriteContext writeContext) throws NoSourceNameException {
    if (method.isNative()) {
      writeNativeMethod(writer, method.getMethodSignature(), method.getMethodBody(writeContext));
    } else {
      writeMethod(writer, method.getMethodSignature(), method.getMethodBody(writeContext));
    }
  }

  /**
   * Writes the given methods to the given source writer.
   *
   * @param methods the methods to write
   * @param writer the source writer to which the methods should be written
   * @param writeContext the context in which to write the methods
   */
  public void writeMethods(Iterable<InjectorMethod> methods, SourceWriter writer,
      InjectorWriteContext writeContext) throws NoSourceNameException {
    for (InjectorMethod method : methods) {
      writeMethod(method, writer, writeContext);
    }
  }

  /**
   * Generates all the required injector methods to inject members of the given
   * type, and a standard member-inject method that invokes them.
   *
   * @param type type for which the injection is performed
   * @param nameGenerator the name generator used to create method names
   * @param methodsOutput a list to which the new injection method and all its
   *     helpers are added
   * @return name of the method created
   */
  public String createMemberInjection(TypeLiteral<?> type, NameGenerator nameGenerator,
      List<InjectorMethod> methodsOutput) throws NoSourceNameException {
    String memberInjectMethodName = nameGenerator.getMemberInjectMethodName(type);
    String memberInjectMethodSignature = "public void " + memberInjectMethodName + "("
        + ReflectUtil.getSourceName(type) + " injectee)";

    SourceSnippetBuilder sb = new SourceSnippetBuilder();

    sb.append(createMethodInjections(getMethodsToInject(type), "injectee", nameGenerator,
        methodsOutput));
    sb.append(createFieldInjections(getFieldsToInject(type), "injectee", nameGenerator,
        methodsOutput));

    // Generate the top-level member inject method in the package containing the
    // type we're injecting:
    methodsOutput.add(SourceSnippets.asMethod(false, memberInjectMethodSignature,
        ReflectUtil.getUserPackageName(type), sb.build()));

    return memberInjectMethodName;
  }

  private String getJsniSignature(FieldLiteral<?> field) throws NoSourceNameException {
    StringBuilder signature = new StringBuilder();
    signature.append("@");
    signature.append(ReflectUtil.getSourceName(field.getRawDeclaringType()));
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
    Set<FieldLiteral<?>> fields = new LinkedHashSet<FieldLiteral<?>>();
    for (FieldLiteral<?> field : memberCollector.getFields(type)) {
      if (!guiceUtil.isOptional(field) || bindingIndex.isBound(guiceUtil.getKey(field))) {
        fields.add(field);
      }
    }
    return fields;
  }

  private Set<MethodLiteral<?, Method>> getMethodsToInject(TypeLiteral<?> type) {
    Set<MethodLiteral<?, Method>> methods = new LinkedHashSet<MethodLiteral<?, Method>>();
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

  /**
   * Factory for {@link SourceWriteUtil}.
   */
  public interface Factory {
    SourceWriteUtil create(BindingIndex bindingIndex);
  }
}
