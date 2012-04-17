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
package com.google.gwt.inject.rebind.output;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.GinjectorNameGenerator;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.inject.rebind.util.GuiceUtil;
import com.google.gwt.inject.rebind.util.MemberCollector;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.PrettyPrinter;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

import java.io.PrintWriter;
import java.lang.reflect.Method;

/**
 * Outputs all the generated classes for an implementation of a Ginjector
 * interface.
 */
@Singleton
public class GinjectorImplOutputter {

  private final GinjectorBindingsOutputter bindingsOutputter;

  /**
   * Collector that gathers methods from an injector interface and its
   * ancestors, recording only those methods that use constructor injection
   * (i.e. that return an object and take no parameters).  Used to determine
   * injection root types and to write the implementation for the collected
   * methods.
   */
  private final MemberCollector constructorInjectCollector;

  private final GeneratorContext ctx;
  private final FragmentPackageName.Factory fragmentPackageNameFactory;
  private final GinjectorNameGenerator ginjectorNameGenerator;
  private final GuiceUtil guiceUtil;
  private final TreeLogger logger;
  private final ReachabilityAnalyzer reachabilityAnalyzer;
  private final SourceWriteUtil.Factory sourceWriteUtilFactory;

  /**
   * Collector that gathers methods from an injector interface and its
   * ancestors, recording only those methods that use member injection (i.e.
   * that return void and take one object as parameter).  Used to determine
   * injection root types and to write the implementation for the collected
   * methods.
   */
  private final MemberCollector memberInjectCollector;

  @Inject
  public GinjectorImplOutputter(GinjectorBindingsOutputter bindingsOutputter,
      GeneratorContext ctx, FragmentPackageName.Factory fragmentPackageNameFactory,
      GinjectorNameGenerator ginjectorNameGenerator, final GuiceUtil guiceUtil, TreeLogger logger,
      Provider<MemberCollector> collectorProvider, ReachabilityAnalyzer reachabilityAnalyzer,
      SourceWriteUtil.Factory sourceWriteUtilFactory) {
    this.bindingsOutputter = bindingsOutputter;
    this.ctx = ctx;
    this.fragmentPackageNameFactory = fragmentPackageNameFactory;
    this.ginjectorNameGenerator = ginjectorNameGenerator;
    this.guiceUtil = guiceUtil;
    this.logger = logger;
    this.reachabilityAnalyzer = reachabilityAnalyzer;
    this.sourceWriteUtilFactory = sourceWriteUtilFactory;

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

  /**
   * Writes the implementation of the {@link Ginjector} interface associated
   * with the given {@link GinjectorBindings} object, if any, along with all the
   * injector classes and fragment classes required by the implementation.
   */
  public void write(String packageName, String implClassName, PrintWriter printWriter,
      GinjectorBindings rootBindings) throws UnableToCompleteException {
    writeInjectorHierarchy(rootBindings);

    TypeLiteral<?> ginjectorInterface = rootBindings.getGinjectorInterface();
    if (ginjectorInterface != null) {
      writeInterface(ginjectorInterface, packageName, implClassName, printWriter, rootBindings);
    }
  }

  private void writeInjectorHierarchy(GinjectorBindings bindings) throws UnableToCompleteException {
    for (GinjectorBindings child : bindings.getChildren()) {
      writeInjectorHierarchy(child);
    }

    bindingsOutputter.write(bindings);
  }

  private void writeInterface(TypeLiteral<?> ginjectorInterface, String packageName,
      String implClassName, PrintWriter printWriter, GinjectorBindings rootBindings)
      throws UnableToCompleteException {
    ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(packageName,
        implClassName);

    SourceWriter writer = null;

    try {
      composerFactory.addImplementedInterface(ReflectUtil.getSourceName(ginjectorInterface));

      writer = composerFactory.createSourceWriter(ctx, printWriter);

      String rootInjectorClass = ginjectorNameGenerator.getClassName(rootBindings);
      String rootFieldName = ginjectorNameGenerator.getFieldName(rootBindings);
      writer.beginJavaDocComment();
      writer.print("Top-level injector instance for injector " + rootBindings.getModule() + ".");
      writer.endJavaDocComment();
      writer.println("private final %1$s %2$s = new %1$s(this);", rootInjectorClass, rootFieldName);

      SourceWriteUtil sourceWriteUtil = sourceWriteUtilFactory.create(rootBindings);
      sourceWriteUtil.writeMethod(writer, "public " + implClassName + "()",
          rootFieldName + ".initialize();");

      outputInterfaceMethods(rootBindings, ginjectorInterface, sourceWriteUtil, writer);
    } catch (NoSourceNameException e) {
      // TODO(schmitt): Collect errors and log list of them.
      logger.log(TreeLogger.Type.ERROR, e.getMessage(), e);
    }

    if (writer != null) {
      writer.commit(logger);
    }
  }

  private void outputInterfaceMethods(GinjectorBindings bindings, TypeLiteral<?> ginjectorInterface,
      SourceWriteUtil sourceWriteUtil, SourceWriter writer)
      throws NoSourceNameException, UnableToCompleteException {
    NameGenerator nameGenerator = bindings.getNameGenerator();

    // Implement a provider method for each zero-arg method in the ginjector
    // interface.
    for (MethodLiteral<?, Method> method :
        constructorInjectCollector.getMethods(ginjectorInterface)) {

      Key<?> methodKey = guiceUtil.getKey(method);
      Binding binding = bindings.getBinding(methodKey);
      if (binding == null) {
        // This should not happen, but fail with a meaningful message if it
        // does.
        logger.log(TreeLogger.Type.ERROR, "Unable to find a binding for the required key "
            + methodKey);
        throw new UnableToCompleteException();
      }

      if (!reachabilityAnalyzer.isReachable(binding)) {
        // Sanity-check reachability: every binding in the Ginjector ought to be
        // reachable.
        PrettyPrinter.log(logger, TreeLogger.Type.ERROR,
            "The key %s is required by the Ginjector, but is not reachable.", methodKey);
        throw new UnableToCompleteException();
      }

      FragmentPackageName fragmentPackageName = fragmentPackageNameFactory.create(
          binding.getGetterMethodPackage());

      String body = String.format("return %s.%s().%s();",
          ginjectorNameGenerator.getFieldName(bindings),
          nameGenerator.getFragmentGetterMethodName(fragmentPackageName),
          nameGenerator.getGetterMethodName(guiceUtil.getKey(method)));

      String readableDeclaration = ReflectUtil.signatureBuilder(method)
          .removeAbstractModifier()
          .build();
      sourceWriteUtil.writeMethod(writer, readableDeclaration, body.toString());
    }

    // Implements methods of the form "void foo(BarType bar)", which run member
    // injection on the given BarType.
    for (MethodLiteral<?, Method> method : memberInjectCollector.getMethods(ginjectorInterface)) {
      Key<?> injectee = guiceUtil.getKey(method);

      if (!reachabilityAnalyzer.isReachableMemberInject(bindings, injectee.getTypeLiteral())) {
        // Sanity-check reachability: every member injection in the Ginjector
        // ought to be reachable.
        PrettyPrinter.log(logger, TreeLogger.Type.ERROR,
            "Method injection of %s is required by the Ginjector, but is not reachable.",
            injectee.getTypeLiteral());
        throw new UnableToCompleteException();
      }

      FragmentPackageName fragmentPackageName = fragmentPackageNameFactory.create(
          ReflectUtil.getUserPackageName(injectee.getTypeLiteral()));

      String body = String.format("%s.%s().%s(param);",
          ginjectorNameGenerator.getFieldName(bindings),
          nameGenerator.getFragmentGetterMethodName(fragmentPackageName),
          nameGenerator.getMemberInjectMethodName(injectee.getTypeLiteral()));

      String readableDeclaration = ReflectUtil.signatureBuilder(method)
          .withParameterNames(new String[]{"param"})
          .removeAbstractModifier()
          .build();
      sourceWriteUtil.writeMethod(writer, readableDeclaration, body);
    }
  }

  private String getImplClassName(Class<?> ginjectorInterface) throws UnableToCompleteException {
    try {
      return ReflectUtil.getSourceName(ginjectorInterface).replace(".", "_") + "Impl";
    } catch (NoSourceNameException e) {
      logger.log(TreeLogger.Type.ERROR, "Could not determine source name for ginjector", e);
      throw new UnableToCompleteException();
    }
  }
}
