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
package com.google.gwt.inject.rebind;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.BindingContext;
import com.google.gwt.inject.rebind.reflect.FieldLiteral;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.inject.rebind.util.GuiceUtil;
import com.google.gwt.inject.rebind.util.MemberCollector;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionPoint;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;

import javax.inject.Provider;

/**
 * Outputs the generated {@code Ginjector} implementation's Java code.
 */
@Singleton
class GinjectorOutputter {
  private final TreeLogger logger;
  private final GeneratorContext ctx;
  private final GinjectorBindings rootBindings;

  /**
   * Collector that gathers methods from an injector interface and its
   * ancestors, recording only those methods that use constructor injection
   * (i.e. that return an object and take no parameters).  Used to determine
   * injection root types and to write the implementation for the collected
   * methods.
   */
  private final MemberCollector constructorInjectCollector;

  /**
   * Collector that gathers methods from an injector interface and its
   * ancestors, recording only those methods that use member injection (i.e.
   * that return void and take one object as parameter).  Used to determine
   * injection root types and to write the implementation for the collected
   * methods.
   */
  private final MemberCollector memberInjectCollector;

  private final SourceWriteUtil sourceWriteUtil;

  private final GuiceUtil guiceUtil;

  /**
   * Interface of the injector that this class is implementing.
   */
  private final TypeLiteral<? extends Ginjector> ginjectorInterface;

  /**
   * Writer to append Java code for our implementation class.
   */
  private SourceWriter writer;

  /**
   * Body for the Ginjector's constructor.
   */
  private StringBuilder constructorBody = new StringBuilder();

  private final ErrorManager errorManager;
  private final GinjectorNameGenerator ginjectorNameGenerator;
  
  @Inject
  GinjectorOutputter(TreeLogger logger,
      Provider<MemberCollector> collectorProvider, SourceWriteUtil sourceWriteUtil,
      final GuiceUtil guiceUtil, GeneratorContext ctx,
      @RootBindings GinjectorBindings bindingsCollection,
      @GinjectorInterfaceType Class<? extends Ginjector> ginjectorInterface,
      GinjectorNameGenerator ginjectorNameGenerator,
      ErrorManager errorManager) {
    this.logger = logger;
    this.sourceWriteUtil = sourceWriteUtil;
    this.guiceUtil = guiceUtil;
    this.ctx = ctx;
    this.rootBindings = bindingsCollection;
    this.ginjectorNameGenerator = ginjectorNameGenerator;
    this.errorManager = errorManager;
    this.ginjectorInterface = TypeLiteral.get(ginjectorInterface);

    constructorInjectCollector = collectorProvider.get();
    constructorInjectCollector.setMethodFilter(new MemberCollector.MethodFilter() {
        public boolean accept(MethodLiteral<?, Method> method) {
          return !guiceUtil.isMemberInject(method);
        }
      });

    memberInjectCollector = collectorProvider.get();
    memberInjectCollector.setMethodFilter(new MemberCollector.MethodFilter() {
        public boolean accept(MethodLiteral<?, Method> method) {
          return guiceUtil.isMemberInject(method);
        }
      });
  }

  void output(String packageName, String implClassName, PrintWriter printWriter)
      throws UnableToCompleteException {
    ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(
        packageName, implClassName);

    try {
      composerFactory.addImplementedInterface(ReflectUtil.getSourceName(ginjectorInterface));
      composerFactory.addImport(GWT.class.getCanonicalName());

      writer = composerFactory.createSourceWriter(ctx, printWriter);

      outputInterfaceMethods();
      ginjectorNameGenerator.registerName(rootBindings, implClassName);
      outputBindings(rootBindings);
      errorManager.checkForError();
    } catch (NoSourceNameException e) {
      // TODO(schmitt): Collect errors and log list of them.
      logger.log(TreeLogger.Type.ERROR, e.getMessage(), e);
    }

    writeConstructor(implClassName);

    writer.commit(logger);
  }

  /**
   * Output the bindings for the given {@link GinjectorBindings}.  If it has any children
   * in the hierarchy, it will first output the necessary classes. 
   */
  private void outputBindings(GinjectorBindings bindings) {
    // Output child modules.
    for (GinjectorBindings child : bindings.getChildren()) {
      String className = ginjectorNameGenerator.getClassName(child);
      String fieldName = ginjectorNameGenerator.getFieldName(child);
      
      writer.beginJavaDocComment();
      writer.println("Generated class for child injector for ");
      writer.print("  %s", child.getModule());
      writer.endJavaDocComment();
      writer.println("public class %s {", className);
      writer.indent();
      outputBindings(child);
      writer.outdent();
      writer.println("}");

      writer.beginJavaDocComment();
      writer.println("Field for child injector for");
      writer.print("  %s", child.getModule());
      writer.endJavaDocComment();
      writer.println("private final %1$s %2$s = new %1$s();", className, fieldName);
    }
    
    outputMemberInjections(bindings);
    outputStaticInjections(bindings);

    // Output the actual bindings
    for (Map.Entry<Key<?>, BindingEntry> entry : bindings.getBindings().entrySet()) {
      outputBinding(bindings.getNameGenerator(), entry.getKey(), entry.getValue(),
          bindings.determineScope(entry.getKey()));
    }
  }

  /**
   * Output the the creator/getter methods for the given binding.
   */
  private void outputBinding(NameGenerator nameGenerator, Key<?> key, BindingEntry bindingEntry,
      GinScope scope) {
    Binding binding = bindingEntry.getBinding();
    BindingContext bindingContext = bindingEntry.getBindingContext();

    String getter = nameGenerator.getGetterMethodName(key);
    String creator = nameGenerator.getCreatorMethodName(key);

    String typeName;
    try {
      typeName = ReflectUtil.getSourceName(key.getTypeLiteral());

      sourceWriteUtil.writeBindingContextJavadoc(writer, bindingContext, key);

      // Regardless of the scope, we have a creator method.
      binding.writeCreatorMethods(writer, "private " + typeName + " " + creator + "()", 
          nameGenerator);
    } catch (NoSourceNameException e) {
      errorManager.logError("Error trying to write source for [" + key + "] -> ["
          + binding + "]; binding declaration: " + bindingContext, e);
      return;
    }

    // Name of the field that we might need.
    String field = nameGenerator.getSingletonFieldName(key);

    switch (scope) {
      case EAGER_SINGLETON:
        constructorBody.append("// Eager singleton bound at:\n");
        appendBindingContextCommentToConstructor(bindingContext);
        constructorBody.append(getter).append("();\n");
        // $FALL-THROUGH$
      case SINGLETON:
        writer.println("private " + typeName + " " + field + " = null;");
        writer.println();
        sourceWriteUtil.writeBindingContextJavadoc(writer, bindingContext, "Singleton bound at:");
        writer.println("private " + typeName + " " + getter + "()" + " {");
        writer.indent();
        writer.println("if (" + field + " == null) {");
        writer.indent();
        writer.println(field + " = " + creator + "();");
        writer.outdent();
        writer.println("}");
        writer.println("return " + field + ";");
        writer.outdent();
        writer.println("}");
        break;

      case NO_SCOPE:
        // For none, getter just returns creator
        sourceWriteUtil.writeBindingContextJavadoc(writer, bindingContext, key);

        sourceWriteUtil.writeMethod(writer, "private " + typeName + " " + getter + "()",
          "return " + creator + "();");
        break;

      default:
        throw new IllegalStateException();
    }

    writer.println();
  }

  private void appendBindingContextCommentToConstructor(BindingContext bindingContext) {
    for(String line : bindingContext.toString().split("\n")) {
      constructorBody.append("//   ").append(line).append("\n");
    }
  }

  private void outputInterfaceMethods() throws NoSourceNameException {
    NameGenerator nameGenerator = rootBindings.getNameGenerator();
    // Add a forwarding method for each zero-arg method in the ginjector interface
    for (MethodLiteral<?, Method> method :
        constructorInjectCollector.getMethods(ginjectorInterface)) {
      StringBuilder body = new StringBuilder();
      body.append("return ")
          .append(nameGenerator.getGetterMethodName(guiceUtil.getKey(method)))
          .append("();");

      String readableDeclaration =
          ReflectUtil.getSignature(method, ReflectUtil.nonAbstractModifiers(method));
      sourceWriteUtil.writeMethod(writer, readableDeclaration, body.toString());
    }

    // Implements methods of the form "void foo(BarType bar)"
    for (MethodLiteral<?, Method> method : memberInjectCollector.getMethods(ginjectorInterface)) {
      Key<?> injectee = method.getParameterKeys().get(0);

      String body = nameGenerator.getMemberInjectMethodName(injectee) + "(param);";

      String readableDeclaration = ReflectUtil.getSignature(method, new String[]{"param"},
          ReflectUtil.nonAbstractModifiers(method));
      sourceWriteUtil.writeMethod(writer, readableDeclaration, body);
    }
  }

  // Visible for tests.
  void outputStaticInjections(GinjectorBindings bindings) {
    NameGenerator nameGenerator = bindings.getNameGenerator();

    for (Class<?> type : bindings.getStaticInjectionRequests()) {
      String methodName = nameGenerator.convertToValidMemberName("injectStatic_" + type.getName());
      StringBuilder body = new StringBuilder();
      for (InjectionPoint injectionPoint : InjectionPoint.forStaticMethodsAndFields(type)) {
        Member member = injectionPoint.getMember();
        try {
          if (member instanceof Method) {
            MethodLiteral<?, Method> method =
                MethodLiteral.get((Method) member, TypeLiteral.get(member.getDeclaringClass()));
            body.append(sourceWriteUtil.createMethodCallWithInjection(writer, method, null,
                nameGenerator));
          } else if (member instanceof Field) {
            FieldLiteral<?> field =
                FieldLiteral.get((Field) member, TypeLiteral.get(member.getDeclaringClass()));
            body.append(sourceWriteUtil.createFieldInjection(writer, field, null, nameGenerator));
          }
        } catch (NoSourceNameException e) {
          errorManager.logError(e.getMessage(), e);
        }
      }

      sourceWriteUtil.writeMethod(writer, "private void " + methodName + "()", body.toString());
      constructorBody.append(methodName).append("();\n");
    }
  }

  private void outputMemberInjections(GinjectorBindings bindings) {
    NameGenerator nameGenerator = bindings.getNameGenerator();
    for (Key<?> key : bindings.getMemberInjectRequests()) {
      try {
        sourceWriteUtil.appendMemberInjection(writer, key, nameGenerator);
      } catch (NoSourceNameException e) {
        errorManager.logError(e.getMessage(), e);
      }
    }
  }

  private void writeConstructor(String implClassName) {
    sourceWriteUtil.writeMethod(writer, "public " + implClassName + "()",
        constructorBody.toString());
  }

}
