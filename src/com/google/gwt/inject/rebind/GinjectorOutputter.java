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
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.Injectable;
import com.google.gwt.inject.rebind.util.KeyUtil;
import com.google.gwt.inject.rebind.util.MemberCollector;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.spi.InjectionPoint;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Outputs the generated {@code Ginjector} implementation's Java code.
 */
@Singleton
class GinjectorOutputter {
  private final TreeLogger logger;
  private final GeneratorContext ctx;
  private final BindingsProcessor bindingsProcessor;

  /**
   * Generates names for code we produce to resolve injection requests.
   */
  private final NameGenerator nameGenerator;

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

  private final KeyUtil keyUtil;

  /**
   * Interface of the injector that this class is implementing.
   */
  private final JClassType ginjectorInterface;

  /**
   * Writer to append Java code for our implementation class.
   */
  private SourceWriter writer;

  /**
   * Body for the Ginjector's constructor.
   */
  private StringBuilder constructorBody = new StringBuilder();

  @Inject
  GinjectorOutputter(NameGenerator nameGenerator, TreeLogger logger,
      Provider<MemberCollector> collectorProvider, SourceWriteUtil sourceWriteUtil,
      final KeyUtil keyUtil, GeneratorContext ctx, BindingsProcessor bindingsProcessor,
      @GinjectorInterfaceType JClassType ginjectorInterface) {
    this.nameGenerator = nameGenerator;
    this.logger = logger;
    this.sourceWriteUtil = sourceWriteUtil;
    this.keyUtil = keyUtil;
    this.ctx = ctx;
    this.bindingsProcessor = bindingsProcessor;
    this.ginjectorInterface = ginjectorInterface;

    constructorInjectCollector = collectorProvider.get();
    constructorInjectCollector.setMethodFilter(new MemberCollector.MethodFilter() {
        public boolean accept(JMethod method) {
          return method.getParameters().length == 0;
        }
      });

    memberInjectCollector = collectorProvider.get();
    memberInjectCollector.setMethodFilter(new MemberCollector.MethodFilter() {
        public boolean accept(JMethod method) {
          return keyUtil.isMemberInject(method);
        }
      });
  }

  void output(String packageName, String implClassName, PrintWriter printWriter)
      throws UnableToCompleteException {
    ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(
        packageName, implClassName);

    composerFactory.addImplementedInterface(
        ginjectorInterface.getParameterizedQualifiedSourceName());
    composerFactory.addImport(GWT.class.getCanonicalName());

    writer = composerFactory.createSourceWriter(ctx, printWriter);

    outputInterfaceMethods();
    outputBindings();
    outputStaticInjections();
    outputMemberInjections();

    writeConstructor(implClassName);

    writer.commit(logger);
  }

  private void outputBindings() throws UnableToCompleteException {
    // Write out each binding
    for (Map.Entry<Key<?>, Binding> entry : bindingsProcessor.getBindings().entrySet()) {
      Key<?> key = entry.getKey();

      // toString on TypeLiteral outputs the binary name, not the source name
      String typeName = sourceWriteUtil.getSourceName(key.getTypeLiteral());
      Binding binding = entry.getValue();

      String getter = nameGenerator.getGetterMethodName(key);
      String creator = nameGenerator.getCreatorMethodName(key);

      // Regardless of the scope, we have a creator method
      binding.writeCreatorMethods(writer, "private " + typeName + " " + creator + "()");

      // Name of the field that we might need
      String field = nameGenerator.getSingletonFieldName(key);

      GinScope scope = bindingsProcessor.determineScope(key);
      switch (scope) {
        case EAGER_SINGLETON:
          constructorBody.append(getter).append("();\n");
          // Intentionally fall through.
        case SINGLETON:
          writer.println("private " + typeName + " " + field + " = null;");
          writer.println();
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
          sourceWriteUtil.writeMethod(writer, "private " + typeName + " " + getter + "()",
              "return " + creator + "();");
          break;

        default:
          throw new IllegalStateException();
      }

      writer.println();
    }
  }

  private void outputInterfaceMethods() {
    // Add a forwarding method for each zero-arg method in the ginjector interface
    for (JMethod method : constructorInjectCollector.getMethods(ginjectorInterface)) {
      StringBuilder body = new StringBuilder();
      body.append("return ")
          .append(nameGenerator.getGetterMethodName(keyUtil.getKey(method)))
          .append("();");

      sourceWriteUtil.writeMethod(writer,
          method.getReadableDeclaration(false, false, false, false, true),
          body.toString());
    }

    // Implements methods of the form "void foo(BarType bar)"
    for (JMethod method : memberInjectCollector.getMethods(ginjectorInterface)) {
      JParameter injectee = method.getParameters()[0];
      String body = nameGenerator.getMemberInjectMethodName(keyUtil.getKey(injectee))
          + "(" + injectee.getName() + ");";

      sourceWriteUtil.writeMethod(writer,
          method.getReadableDeclaration(false, false, false, false, true),
          body);
    }
  }

  private void outputStaticInjections() throws UnableToCompleteException {
    boolean foundError = false;

    for (Class<?> type : bindingsProcessor.getStaticInjectionRequests()) {
      String methodName = nameGenerator.convertToValidMemberName("injectStatic_" + type.getName());
      StringBuilder body = new StringBuilder();
      for (InjectionPoint injectionPoint : InjectionPoint.forStaticMethodsAndFields(type)) {
        Member member = injectionPoint.getMember();
        if (member instanceof Method) {
          try {
            body.append(sourceWriteUtil.createMethodCallWithInjection(writer,
                keyUtil.javaToGwtMethod((Method) member), null));
          } catch (NotFoundException e) {
            foundError = true;
            logger.log(TreeLogger.Type.ERROR, e.getMessage(), e);
          }
        } else if (member instanceof Field) {
          body.append(sourceWriteUtil.createFieldInjection(writer,
              keyUtil.javaToGwtField((Field) member), null));
        }
      }

      sourceWriteUtil.writeMethod(writer, "private void " + methodName + "()", body.toString());
      constructorBody.append(methodName).append("();\n");
    }

    if (foundError) {
      throw new UnableToCompleteException();
    }
  }

  private void outputMemberInjections() {
    for (Key<?> key : bindingsProcessor.getMemberInjectRequests()) {
      sourceWriteUtil.appendMemberInjection(writer, key);
    }
  }

  private void writeConstructor(String implClassName) {
    sourceWriteUtil.writeMethod(writer, "public " + implClassName + "()",
        constructorBody.toString());
  }

}
