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
import com.google.gwt.inject.rebind.ErrorManager;
import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.GinjectorNameGenerator;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.GinjectorBinding;
import com.google.gwt.inject.rebind.reflect.FieldLiteral;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.inject.rebind.util.InjectorMethod;
import com.google.gwt.inject.rebind.util.MethodCallUtil;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceSnippetBuilder;
import com.google.gwt.inject.rebind.util.SourceSnippets;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Outputs the generated classes for one or more {@link GinjectorBindings}.
 */
@Singleton
class GinjectorBindingsOutputter {

  private final GeneratorContext ctx;
  private final ErrorManager errorManager;
  private final GinjectorFragmentOutputter.Factory fragmentOutputterFactory;
  private final FragmentPackageName.Factory fragmentPackageNameFactory;
  private final GinjectorNameGenerator ginjectorNameGenerator;
  private final TreeLogger logger;
  private final MethodCallUtil methodCallUtil;
  private final ReachabilityAnalyzer reachabilityAnalyzer;
  private final SourceWriteUtil.Factory sourceWriteUtilFactory;

  @Inject
  GinjectorBindingsOutputter(GeneratorContext ctx,
      ErrorManager errorManager,
      GinjectorFragmentOutputter.Factory fragmentOutputterFactory,
      FragmentPackageName.Factory fragmentPackageNameFactory,
      GinjectorNameGenerator ginjectorNameGenerator,
      TreeLogger logger,
      MethodCallUtil methodCallUtil,
      ReachabilityAnalyzer reachabilityAnalyzer,
      SourceWriteUtil.Factory sourceWriteUtilFactory) {

    this.ctx = ctx;
    this.errorManager = errorManager;
    this.fragmentOutputterFactory = fragmentOutputterFactory;
    this.fragmentPackageNameFactory = fragmentPackageNameFactory;
    this.ginjectorNameGenerator = ginjectorNameGenerator;
    this.logger = logger;
    this.methodCallUtil = methodCallUtil;
    this.reachabilityAnalyzer = reachabilityAnalyzer;
    this.sourceWriteUtilFactory = sourceWriteUtilFactory;
  }

  /**
   * Writes the Ginjector class for the given bindings object, and all its
   * package-specific fragments.
   */
  void write(GinjectorBindings bindings) throws UnableToCompleteException {

    TypeLiteral<?> ginjectorInterface = bindings.getGinjectorInterface();
    String implClassName = ginjectorNameGenerator.getClassName(bindings);
    if (implClassName.contains(".")) {
      errorManager.logError("Internal error: the injector class name \"%s\" contains a full stop.",
          implClassName);
    }
    String packageName = ReflectUtil.getUserPackageName(TypeLiteral.get(bindings.getModule()));

    PrintWriter printWriter = ctx.tryCreate(logger, packageName, implClassName);
    if (printWriter == null) {
      // We already created this Ginjector.
      return;
    }

    ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(packageName,
        implClassName);
    SourceWriter writer = composerFactory.createSourceWriter(ctx, printWriter);

    FragmentMap fragments = new FragmentMap(bindings, packageName, implClassName,
        fragmentOutputterFactory);

    outputBindings(bindings, fragments, writer);
    errorManager.checkForError();

    fragments.commitAll();
    writer.commit(logger);
  }

  /**
   * Outputs the top-level injector for the given {@link GinjectorBindings},
   * along with all of its fragments.
   *
   * <p>The top-level injector contains one field for each fragment of the
   * injector, which stores a reference to an instance of that fragment.  In
   * addition, it contains a getter for every public type created by one of its
   * fragments, each of which forwards to a getter in the corresponding
   * fragment.  In addition to being the injector's public interface, these
   * getters are used by each fragment of the injector to retrieve objects
   * created by other fragments.
   */
  private void outputBindings(GinjectorBindings bindings, FragmentMap fragments,
      SourceWriter writer) {
    NameGenerator nameGenerator = bindings.getNameGenerator();

    // The initialize*() methods contain code that needs to run before the root
    // injector is returned to the client, but after the injector hierarchy is
    // fully constructed.

    // Collects the text of the body of initializeEagerSingletons().
    StringBuilder initializeEagerSingletonsBody = new StringBuilder();

    // Collects the text of the body of initializeStaticInjections().
    StringBuilder initializeStaticInjectionsBody = new StringBuilder();

    SourceWriteUtil sourceWriteUtil = sourceWriteUtilFactory.create(bindings);

    // Output child modules.
    for (GinjectorBindings child : bindings.getChildren()) {
      String className = ginjectorNameGenerator.getClassName(child);
      String canonicalClassName = ginjectorNameGenerator.getCanonicalClassName(child);
      String fieldName = ginjectorNameGenerator.getFieldName(child);
      String getterName = nameGenerator.getChildInjectorGetterMethodName(className);

      writer.beginJavaDocComment();
      writer.print("Child injector for %s", child.getModule());
      writer.endJavaDocComment();
      writer.println("private %s %s = null;", canonicalClassName, fieldName);

      writer.beginJavaDocComment();
      writer.print("Getter for child injector for %s", child.getModule());
      writer.endJavaDocComment();
      sourceWriteUtil.writeMethod(writer,
          String.format("public %s %s()", canonicalClassName, getterName),
          String.format(
              "if (%2$s == null) {\n"
            + "    %2$s = new %1$s(this);\n"
            + "}\n\n"
            + "return %2$s;", canonicalClassName, fieldName));

      // Ensure that the initializer initializes this child.
      outputSubInitialize(child, getterName,
          initializeEagerSingletonsBody, initializeStaticInjectionsBody);
    }

    initializeEagerSingletonsBody.append("\n");
    initializeStaticInjectionsBody.append("\n");

    outputInterfaceField(bindings, sourceWriteUtil, writer);

    outputMemberInjections(bindings, fragments, sourceWriteUtil);
    outputStaticInjections(bindings, fragments, sourceWriteUtil);

    // Output the bindings in the fragments.
    for (Map.Entry<Key<?>, Binding> entry : bindings.getBindings()) {
      Binding binding = entry.getValue();
      if (!reachabilityAnalyzer.isReachable(binding)) {
        continue;
      }

      FragmentPackageName fragmentPackageName =
          fragmentPackageNameFactory.create(binding.getGetterMethodPackage());
      Key<?> key = entry.getKey();

      outputCreatorMethods(bindings, key, binding, fragments);
      fragments.get(fragmentPackageName)
          .outputBindingGetter(key, binding, bindings.determineScope(key));
    }

    // Output the fragment members.
    outputFragments(bindings, fragments, initializeEagerSingletonsBody,
        initializeStaticInjectionsBody, sourceWriteUtil, writer);

    writeConstructor(bindings, sourceWriteUtil, writer);
    writeInitializers(initializeEagerSingletonsBody, initializeStaticInjectionsBody,
        sourceWriteUtil, writer);
  }

  /**
   * Writes the creator methods associated with the given binding.
   */
  private void outputCreatorMethods(GinjectorBindings bindings, Key<?> key, Binding binding,
      FragmentMap fragments) {

    try {
      String typeName = ReflectUtil.getSourceName(key.getTypeLiteral());
      String creator = bindings.getNameGenerator().getCreatorMethodName(key);

      outputMethods(binding.getCreatorMethods("private " + typeName + " " + creator + "()",
          bindings.getNameGenerator()), fragments);
    } catch (NoSourceNameException e) {
      errorManager.logError("Error trying to write creators for [%s] -> [%s];"
          + " binding declaration: %s", e, key, binding, binding.getContext());
    }
  }

  /**
   * Writes code to store and retrieve the current injector interface, if one is
   * bound.
   */
  private void outputInterfaceField(GinjectorBindings bindings, SourceWriteUtil sourceWriteUtil,
      SourceWriter writer) {

    // Only the root injector has an interface binding.
    if (bindings.getParent() != null) {
      return;
    }

    Class<?> boundGinjectorInterface = getBoundGinjector(bindings);

    if (boundGinjectorInterface == null) {
      // Sanity-check: if this fails, then we somehow didn't bind the injector
      // interface in the root module (the root module should always generate a
      // binding for the injector).
      errorManager.logError("Injector interface not bound in the root module.");
      return;
    }

    NameGenerator nameGenerator = bindings.getNameGenerator();
    String fieldName = nameGenerator.getGinjectorInterfaceFieldName();
    String getterName = nameGenerator.getGinjectorInterfaceGetterMethodName();

    writer.beginJavaDocComment();
    writer.print("The implementation of " + boundGinjectorInterface);
    writer.endJavaDocComment();
    writer.println("private final %s %s;", boundGinjectorInterface.getCanonicalName(), fieldName);

    sourceWriteUtil.writeMethod(writer,
        String.format("public %s %s()", boundGinjectorInterface.getCanonicalName(), getterName),
        String.format("return %s;", fieldName));
  }

  /**
   * For each fragment in the given {@link FragmentMap}, writes the field that
   * stores it and a getter for that field, and adds code to invoke the
   * fragment's initializers.
   */
  private void outputFragments(GinjectorBindings bindings,
      FragmentMap fragments, StringBuilder initializeEagerSingletonsBody,
      StringBuilder initializeStaticInjectionsBody, SourceWriteUtil sourceWriteUtil,
      SourceWriter writer) {
    String implClassName = ginjectorNameGenerator.getClassName(bindings);
    NameGenerator nameGenerator = bindings.getNameGenerator();

    for (FragmentPackageName fragmentPackageName : fragments.getFragmentPackages()) {
      String fragmentCanonicalClassName =
          nameGenerator.getFragmentCanonicalClassName(implClassName,
              fragmentPackageName);
      String fieldName = nameGenerator.getFragmentFieldName(fragmentPackageName);
      String getterName = nameGenerator.getFragmentGetterMethodName(fragmentPackageName);

      // Create the field.
      writer.beginJavaDocComment();
      writer.print("Injector fragment for %s", fragmentPackageName);
      writer.endJavaDocComment();
      writer.print("private %s %s = null;", fragmentCanonicalClassName, fieldName);

      // Write the getter.
      writer.beginJavaDocComment();
      writer.print("Getter for injector fragment for %s", fragmentPackageName);
      writer.endJavaDocComment();
      sourceWriteUtil.writeMethod(writer,
          "public " + fragmentCanonicalClassName + " " + getterName + "()", String.format(
          "if (%2$s == null) {\n"
        + "    %2$s = new %1$s(this);\n"
        + "}\n\n"
        + "return %2$s;", fragmentCanonicalClassName, fieldName));

      // TODO(dburrows): throw away calls to and definitions of initializers
      // that have nothing to do.

      initializeEagerSingletonsBody.append(getterName + "().initializeEagerSingletons();\n");
      initializeStaticInjectionsBody.append(getterName + "().initializeStaticInjections();\n");
    }
  }

  /**
   * Adds member injections to each fragment.
   */
  private void outputMemberInjections(GinjectorBindings bindings, FragmentMap fragments,
      SourceWriteUtil sourceWriteUtil) {
    NameGenerator nameGenerator = bindings.getNameGenerator();
    for (TypeLiteral<?> type : bindings.getMemberInjectRequests()) {
      if (!reachabilityAnalyzer.isReachableMemberInject(bindings, type)) {
        continue;
      }

      List<InjectorMethod> memberInjectionHelpers = new ArrayList<InjectorMethod>();

      try {
        sourceWriteUtil.createMemberInjection(type, nameGenerator, memberInjectionHelpers);
        outputMethods(memberInjectionHelpers, fragments);
      } catch (NoSourceNameException e) {
        errorManager.logError(e.getMessage(), e);
      }
    }
  }

  void outputStaticInjections(GinjectorBindings bindings, FragmentMap fragments,
      SourceWriteUtil sourceWriteUtil) {
    for (Class<?> type : bindings.getStaticInjectionRequests()) {
      outputStaticInjectionMethods(type, fragments, bindings.getNameGenerator(), sourceWriteUtil);
    }
  }

  /**
   * Outputs all the static injection methods for the given class.
   */
  void outputStaticInjectionMethods(Class<?> type, FragmentMap fragments,
      NameGenerator nameGenerator, SourceWriteUtil sourceWriteUtil) {
    String methodName = nameGenerator.convertToValidMemberName("injectStatic_" + type.getName());
    SourceSnippetBuilder body = new SourceSnippetBuilder();
    for (InjectionPoint injectionPoint : InjectionPoint.forStaticMethodsAndFields(type)) {
      Member member = injectionPoint.getMember();
      try {
        List<InjectorMethod> staticInjectionHelpers = new ArrayList<InjectorMethod>();

        if (member instanceof Method) {
          MethodLiteral<?, Method> method =
              MethodLiteral.get((Method) member, TypeLiteral.get(member.getDeclaringClass()));
          body.append(methodCallUtil.createMethodCallWithInjection(method, null, nameGenerator,
              staticInjectionHelpers));
        } else if (member instanceof Field) {
          FieldLiteral<?> field =
              FieldLiteral.get((Field) member, TypeLiteral.get(member.getDeclaringClass()));
          body.append(sourceWriteUtil.createFieldInjection(field, null, nameGenerator,
              staticInjectionHelpers));
        }

        outputMethods(staticInjectionHelpers, fragments);
      } catch (NoSourceNameException e) {
        errorManager.logError(e.getMessage(), e);
      }
    }

    // Note that the top-level method that performs static injection will only
    // invoke a bunch of other injector methods.  Therefore, it doesn't matter
    // which package it goes in, and we don't need to invoke getUserPackageName
    // (which is good, because in practice users statically inject types that
    // have no user package name because they're private inner classes!)
    String packageName = type.getPackage().getName();
    InjectorMethod method = SourceSnippets.asMethod(false, "private void " + methodName + "()",
        packageName, body.build());
    GinjectorFragmentOutputter fragment =
        fragments.get(fragmentPackageNameFactory.create(packageName));
    fragment.outputMethod(method);
    fragment.invokeInInitializeStaticInjections(methodName);
  }

  /**
   * Outputs some methods to the fragments they belong to.
   */
  void outputMethods(Iterable<InjectorMethod> methods, FragmentMap fragments) {
    for (InjectorMethod method : methods) {
      FragmentPackageName fragmentPackageName =
          fragmentPackageNameFactory.create(method.getPackageName());
      GinjectorFragmentOutputter fragment = fragments.get(fragmentPackageName);
      fragment.outputMethod(method);
    }
  }

  /**
   * Outputs code to invoke the given child's initialize*() routines via its
   * member variable.
   */
  private void outputSubInitialize(GinjectorBindings child, String childGetterName,
      StringBuilder initializeEagerSingletonsBody, StringBuilder initializeStaticInjectionsBody) {

    initializeEagerSingletonsBody
        .append(childGetterName)
        .append("().initializeEagerSingletons();\n");

    initializeStaticInjectionsBody
        .append(childGetterName)
        .append("().initializeStaticInjections();\n");
  }

  /**
   * Gets the Ginjector interface that is bound by the given bindings, if any.
   */
  private static Class<?> getBoundGinjector(GinjectorBindings bindings) {

    if (bindings.getGinjectorInterface() == null) {
      return null;
    }

    TypeLiteral<?> ginjectorInterface = bindings.getGinjectorInterface();
    Key<?> ginjectorKey = Key.get(ginjectorInterface);

    if (!bindings.isBound(ginjectorKey)) {
      return null;
    }

    if (!(bindings.getBinding(ginjectorKey) instanceof GinjectorBinding)) {
      return null;
    }

    return ginjectorInterface.getRawType();
  }

  /**
   * Writes the class constructor.  If there is a parent injector, also writes a
   * field that stores it and a getter (used by fragments in this injector and
   * its children).
   *
   * <p>The arguments to the constructor are:
   *
   * <p>For injectors other than the root, the parent injector.
   *
   * <p>For the root injector, the implementation of the ginjector interface.
   */
  private void writeConstructor(GinjectorBindings bindings, SourceWriteUtil sourceWriteUtil,
      SourceWriter writer) {
    String implClassName = ginjectorNameGenerator.getClassName(bindings);
    if (bindings.getParent() == null) {
      // In outputInterfaceField, we verify that we have a bound injector if we
      // are the root module, so this should never be null:
      Class<?> boundGinjector = getBoundGinjector(bindings);

      String interfaceCanonicalClassName = boundGinjector.getCanonicalName();
      String fieldName = bindings.getNameGenerator().getGinjectorInterfaceFieldName();

      sourceWriteUtil.writeMethod(writer,
          String.format("public %s(%s %s)", implClassName, interfaceCanonicalClassName, fieldName),
          String.format("this.%1$s = %1$s;", fieldName));
    } else {
      String parentImplCanonicalClassName = ginjectorNameGenerator.getCanonicalClassName(
          bindings.getParent());

      writer.print(String.format("private final %s parent;\n", parentImplCanonicalClassName));

      sourceWriteUtil.writeMethod(writer, String.format("public %s getParent()",
          parentImplCanonicalClassName), "return parent;");

      sourceWriteUtil.writeMethod(writer, String.format("public %1$s(%2$s parent)",
          implClassName, parentImplCanonicalClassName), "this.parent = parent;");
    }
  }

  // Setting up the injector works as follows:
  //
  // When the injectors are constructed, each injector creates its children and
  // fragments via field initializers.  Then, if the injector is the top-level
  // injector, it initializes itself and its children.  Initialization is
  // performed as a separate step to ensure that the entire injector hierarchy
  // is created before we try to invoke any injection method to, e.g., create
  // eager singletons.  For more details, see
  // <http://code.google.com/p/google-gin/issues/detail?id=156>.

  private void writeInitializers(
      StringBuilder initializeEagerSingletonsBody, StringBuilder initializeStaticInjectionsBody,
      SourceWriteUtil sourceWriteUtil, SourceWriter writer) {

    sourceWriteUtil.writeMethod(writer,
        "public void initializeEagerSingletons()", initializeEagerSingletonsBody.toString());

    sourceWriteUtil.writeMethod(writer,
        "public void initializeStaticInjections()", initializeStaticInjectionsBody.toString());
  }

  /**
   * Creates and tracks the fragment outputter associated with each package
   * containing bindings.  Visible for testing.
   */
  static final class FragmentMap {
    private final GinjectorBindings bindings;
    private final GinjectorFragmentOutputter.Factory fragmentFactory;
    private final Map<FragmentPackageName, GinjectorFragmentOutputter> fragments =
        new HashMap<FragmentPackageName, GinjectorFragmentOutputter>();
    private final String ginjectorPackageName;
    private final String ginjectorClassName;

    FragmentMap(GinjectorBindings bindings, String ginjectorPackageName,
        String ginjectorClassName, GinjectorFragmentOutputter.Factory fragmentFactory) {
      this.bindings = bindings;
      this.ginjectorPackageName = ginjectorPackageName;
      this.ginjectorClassName = ginjectorClassName;
      this.fragmentFactory = fragmentFactory;
    }

    /**
     * Gets the fragment outputter associated with the given package name,
     * creating one if there isn't one yet.
     */
    GinjectorFragmentOutputter get(FragmentPackageName packageName) {
      if (fragments.containsKey(packageName)) {
        return fragments.get(packageName);
      } else {
        GinjectorFragmentOutputter result = fragmentFactory.create(bindings, packageName,
            ginjectorPackageName, ginjectorClassName);
        fragments.put(packageName, result);
        return result;
      }
    }

    /**
     * Gets the package names associated with fragments that were created by
     * this map.
     */
    Iterable<FragmentPackageName> getFragmentPackages() {
      return fragments.keySet();
    }

    /**
     * Commits all the fragments that were created by this map.
     */
    void commitAll() {
      for (GinjectorFragmentOutputter fragment : fragments.values()) {
        fragment.commit();
      }
    }
  }
}
