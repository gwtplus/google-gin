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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.inject.rebind.ErrorManager;
import com.google.gwt.inject.rebind.GinScope;
import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.Context;
import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.inject.rebind.util.InjectorMethod;
import com.google.gwt.inject.rebind.util.InjectorWriteContext;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceSnippet;
import com.google.gwt.inject.rebind.util.SourceSnippetBuilder;
import com.google.gwt.inject.rebind.util.SourceSnippets;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.assistedinject.Assisted;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes the definition of a single fragment of the Ginjector.  A Ginjector
 * fragment contains all the code to create objects bound in that Ginjector that
 * belong to a particular package, exposing only methods to create objects that
 * are not package-private.
 *
 * <p>Visible for testing, so it can be returned from a mock factory.
 */
class GinjectorFragmentOutputter {

  private final GeneratorContext ctx;
  private final InjectorWriteContext injectorWriteContext;
  private final ErrorManager errorManager;
  private final TreeLogger logger;
  private final NameGenerator nameGenerator;
  private final SourceWriteUtil sourceWriteUtil;

  private final String fragmentClassName;
  private final FragmentPackageName fragmentPackageName;
  private final String ginjectorClassName;

  /**
   * Collects the text of the body of initializeEagerSingletons().
   */
  private final StringBuilder initializeEagerSingletonsBody = new StringBuilder();

  /**
   * Collects the text of the body of initializeStaticInjections().
   */
  private final StringBuilder initializeStaticInjectionsBody = new StringBuilder();

  /**
   * The {@link SourceWriter} used to generate the source code.
   */
  private final SourceWriter writer;

  private boolean committed = false;

  @Inject
  GinjectorFragmentOutputter(
      GeneratorContext ctx,
      GinjectorFragmentContext.Factory ginjectorFragmentContextFactory,
      ErrorManager errorManager,
      TreeLogger logger,
      SourceWriteUtil.Factory sourceWriteUtilFactory,
      @Assisted GinjectorBindings bindings,
      @Assisted FragmentPackageName fragmentPackageName,
      @Assisted("ginjectorPackageName") String ginjectorPackageName,
      @Assisted("ginjectorClassName") String ginjectorClassName) {

    this.ctx = ctx;
    this.errorManager = errorManager;
    this.logger = logger;
    this.sourceWriteUtil = sourceWriteUtilFactory.create(bindings);

    this.fragmentPackageName = fragmentPackageName;
    this.ginjectorClassName = ginjectorClassName;
    this.nameGenerator = bindings.getNameGenerator();

    fragmentClassName = nameGenerator.getFragmentClassName(ginjectorClassName, fragmentPackageName);
    if (fragmentClassName.contains(".")) {
      errorManager.logError("Internal error: the fragment class name \"%s\" contains a full stop.",
          fragmentClassName);
    }

    PrintWriter printWriter =
        ctx.tryCreate(logger, fragmentPackageName.toString(), fragmentClassName);
    if (printWriter == null) {
      // Something is very wrong!  We already created this fragment, but the
      // GinjectorBindingsOutputter should only create each fragment once.
      // Something bad will probably happen later on if we continue, so just
      // abort.
      logger.log(TreeLogger.Type.ERROR, "The fragment " + fragmentPackageName + "." +
          fragmentClassName + " already exists.");
      throw new IllegalStateException("The fragment " + fragmentClassName + " already exists.");
    }

    ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(
        fragmentPackageName.toString(), fragmentClassName);

    composerFactory.addImport(GWT.class.getCanonicalName());
    composerFactory.addImport(ginjectorPackageName + "." + ginjectorClassName);
    writer = composerFactory.createSourceWriter(ctx, printWriter);

    injectorWriteContext = ginjectorFragmentContextFactory.create(bindings, fragmentPackageName,
        writer);
  }

  String getFragmentClassName() {
    return fragmentClassName;
  }

  FragmentPackageName getFragmentPackageName() {
    return fragmentPackageName;
  }

  /** Returns true if the eager singletons initializer is nonempty. */
  boolean hasEagerSingletonInitialization() {
    return initializeEagerSingletonsBody.length() > 0;
  }

  /** Returns true if the static injections initializer is nonempty. */
  boolean hasStaticInjectionInitialization() {
    return initializeStaticInjectionsBody.length() > 0;
  }

  /**
   * Writes a method describing the getter for the given key, along with any
   * other code necessary to support it.  Produces a list of helper methods that
   * still need to be written.
   */
  void writeBindingGetter(Key<?> key, Binding binding, GinScope scope,
      List<InjectorMethod> helperMethodsOutput) {
    Context bindingContext = binding.getContext();

    SourceSnippetBuilder getterBuilder = new SourceSnippetBuilder();
    SourceSnippet creationStatements;
    String getter = nameGenerator.getGetterMethodName(key);

    String typeName;
    try {
      typeName = ReflectUtil.getSourceName(key.getTypeLiteral());

      creationStatements = binding.getCreationStatements(nameGenerator, helperMethodsOutput);
    } catch (NoSourceNameException e) {
      errorManager.logError("Error trying to write getter for [%s] -> [%s];"
          + " binding declaration: %s", e, key, binding, bindingContext);
      return;
    }

    // Name of the field that we might need.
    String field = nameGenerator.getSingletonFieldName(key);

    switch (scope) {
      case EAGER_SINGLETON:
        initializeEagerSingletonsBody.append("// Eager singleton bound at:\n");
        appendBindingContextCommentToMethod(bindingContext, initializeEagerSingletonsBody);
        initializeEagerSingletonsBody.append(getter).append("();\n");
        // $FALL-THROUGH$
      case SINGLETON:
        writer.println("private " + typeName + " " + field + " = null;");
        writer.println();
        getterBuilder.append(String.format("\nif (%s == null) {\n", field))
            .append(creationStatements).append("\n")
            .append(String.format("    %s = result;\n", field))
            .append("}\n")
            .append(String.format("return %s;\n", field));
        break;

      case NO_SCOPE:
        sourceWriteUtil.writeBindingContextJavadoc(writer, bindingContext, key);

        getterBuilder.append(creationStatements).append("\n").append("return result;\n");
        break;

      default:
        throw new IllegalStateException();
    }

    outputMethod(SourceSnippets.asMethod(false, String.format("public %s %s()", typeName, getter),
        fragmentPackageName.toString(), getterBuilder.build()));
  }

  void outputMethod(InjectorMethod method) {
    try {
      sourceWriteUtil.writeMethod(method, writer, injectorWriteContext);
    } catch (NoSourceNameException e) {
      errorManager.logError(e.getMessage(), e);
    }
  }

  /**
   * Add the given method name to the methods invoked in initializeStaticInjections().
   */
  void invokeInInitializeStaticInjections(String methodName) {
    initializeStaticInjectionsBody.append(methodName).append("();\n");
  }

  /**
   * Outputs all the top-level methods and fields of the class, and commits the
   * writer.  Must be the last method invoked on this object.
   */
  void commit() {
    if (committed) {
      errorManager.logError("Committed the fragment for %s twice.", fragmentPackageName);
      return;
    }

    committed = true;

    // Write the field where the enclosing injector is stored.
    writer.beginJavaDocComment();
    writer.print("Field for the enclosing injector.");
    writer.endJavaDocComment();
    writer.println("private final %s injector;", ginjectorClassName);

    // Write the constructor, which takes the enclosing injector and does
    // nothing but store it in a field.  It's important that the constructor has
    // no other side-effects; in particular, it must not call any injector
    // methods, since the injector might not be fully constructed.
    sourceWriteUtil.writeMethod(writer,
        String.format("public %s(%s injector)", fragmentClassName, ginjectorClassName),
        "this.injector = injector;");

    if (hasEagerSingletonInitialization()) {
      // Write a method to initialize eager singletons.
      sourceWriteUtil.writeMethod(
          writer,
          "public void initializeEagerSingletons()",
          initializeEagerSingletonsBody.toString());
    }

    if (hasStaticInjectionInitialization()) {
      // Write a method to initialize static injection.
      sourceWriteUtil.writeMethod(
          writer,
          "public void initializeStaticInjections()",
          initializeStaticInjectionsBody.toString());
    }

    writer.commit(logger);
  }

  private void appendBindingContextCommentToMethod(Context bindingContext,
      StringBuilder methodBody) {
    for (String line : bindingContext.toString().split("\n")) {
      methodBody.append("//   ").append(line).append("\n");
    }
  }

  interface Factory {
    GinjectorFragmentOutputter create(
        GinjectorBindings bindings,
        FragmentPackageName fragmentPackageName,
        @Assisted("ginjectorPackageName") String ginjectorPackageName,
        @Assisted("ginjectorClassName") String ginjectorClassName);
  }
}
