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

import com.google.gwt.inject.rebind.ErrorManager;
import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.GinjectorNameGenerator;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.util.InjectorWriteContext;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.Assisted;

/**
 * An {@link InjectorWriteContext} for use when writing an injector fragment.
 */
class GinjectorFragmentContext implements InjectorWriteContext {

  private final GinjectorBindings bindings;
  private final ErrorManager errorManager;
  private final FragmentPackageName fragmentPackageName;
  private final FragmentPackageName.Factory fragmentPackageNameFactory;
  private final GinjectorNameGenerator ginjectorNameGenerator;
  private final SourceWriteUtil sourceWriteUtil;
  private final SourceWriter sourceWriter;

  @Inject
  public GinjectorFragmentContext(
      ErrorManager errorManager,
      FragmentPackageName.Factory fragmentPackageNameFactory,
      GinjectorNameGenerator ginjectorNameGenerator,
      SourceWriteUtil.Factory sourceWriteUtilFactory,
      @Assisted GinjectorBindings bindings,
      @Assisted FragmentPackageName fragmentPackageName,
      @Assisted SourceWriter sourceWriter) {

    this.bindings = bindings;
    this.errorManager = errorManager;
    this.fragmentPackageName = fragmentPackageName;
    this.fragmentPackageNameFactory = fragmentPackageNameFactory;
    this.ginjectorNameGenerator = ginjectorNameGenerator;
    this.sourceWriteUtil = sourceWriteUtilFactory.create(bindings);
    this.sourceWriter = sourceWriter;
  }

  public String callGetter(Key<?> key) {
    Binding keyBinding = bindings.getBinding(key);
    if (keyBinding == null) {
      errorManager.logError("No binding found for %s in %s", key, bindings);
      return "null /* No binding found */";
    }
    FragmentPackageName keyPackageName =
        fragmentPackageNameFactory.create(keyBinding.getGetterMethodPackage());

    String getterCall = bindings.getNameGenerator().getGetterMethodName(key) + "()";

    if (keyPackageName.equals(fragmentPackageName)) {
      return getterCall;
    } else {
      String fragmentGetterName =
          bindings.getNameGenerator().getFragmentGetterMethodName(keyPackageName);

      return String.format("injector.%s().%s", fragmentGetterName, getterCall);
    }
  }

  public String callChildGetter(GinjectorBindings childBindings, Key<?> key) {
    Binding childKeyBinding = childBindings.getBinding(key);
    if (childKeyBinding == null) {
      errorManager.logError("No binding found for %s", key);
      return "null /* No binding found */";
    }
    FragmentPackageName childKeyPackageName =
        fragmentPackageNameFactory.create(childKeyBinding.getGetterMethodPackage());

    NameGenerator nameGenerator = bindings.getNameGenerator();
    NameGenerator childNameGenerator = childBindings.getNameGenerator();

    String childInjectorClassName = ginjectorNameGenerator.getClassName(childBindings);
    String childGetter = nameGenerator.getChildInjectorGetterMethodName(childInjectorClassName);
    String fragmentGetter = childNameGenerator.getFragmentGetterMethodName(childKeyPackageName);
    String getter = childNameGenerator.getGetterMethodName(key);

    return String.format("injector.%s().%s().%s()", childGetter, fragmentGetter, getter);
  }

  public String callMethod(String methodName, String methodFragmentPackage,
      Iterable<String> parameters) {
    NameGenerator nameGenerator = bindings.getNameGenerator();
    StringBuilder resultBuilder = new StringBuilder();

    FragmentPackageName methodFragmentPackageName =
        fragmentPackageNameFactory.create(methodFragmentPackage);

    if (!methodFragmentPackageName.equals(fragmentPackageName)) {
      String fragmentGetter = nameGenerator.getFragmentGetterMethodName(methodFragmentPackageName);
      resultBuilder.append("injector.").append(fragmentGetter).append("().");
    }

    return resultBuilder.append(methodName).append("(")
        .append(SourceWriteUtil.join(", ", parameters)).append(")").toString();
  }

  public String callMemberInject(TypeLiteral<?> type, String input) {
    String memberInjectMethodName = bindings.getNameGenerator().getMemberInjectMethodName(type);

    return String.format("%s(%s);", memberInjectMethodName, input);
  }

  public String callParentGetter(Key<?> key, GinjectorBindings parentBindings) {
    Binding parentKeyBinding = parentBindings.getBinding(key);
    if (parentKeyBinding == null) {
      errorManager.logError("No binding found for %s in %s", key, parentBindings);
      return "null /* No binding found */";
    }
    FragmentPackageName parentKeyPackageName = fragmentPackageNameFactory.create(
        parentKeyBinding.getGetterMethodPackage());

    StringBuilder result = new StringBuilder().append("injector");
    // Walk up the injector hierarchy until we hit the requested parent.
    GinjectorBindings current = bindings;
    while (current != null && current != parentBindings) {
      result.append(".getParent()");
      current = current.getParent();
    }
    if (current == null) {
      // This should never happen; it indicates that the given parent injector
      // isn't actually a parent of the current bindings object.
      errorManager.logError(
          "Internal Gin error: %s is not a parent of %s.", parentBindings, bindings);
      return "null /* Internal error: unreachable parent bindings */";
    }

    NameGenerator parentNameGenerator = parentBindings.getNameGenerator();
    String fragmentGetter = parentNameGenerator.getFragmentGetterMethodName(parentKeyPackageName);
    String getter = parentNameGenerator.getGetterMethodName(key);

    return result.append(String.format(".%s().%s()", fragmentGetter, getter)).toString();
  }

  public String callGinjectorInterfaceGetter() {
    return String.format("injector.%s()",
        bindings.getNameGenerator().getGinjectorInterfaceGetterMethodName());
  }

  public void writeMethod(String signature, String body) {
    sourceWriteUtil.writeMethod(sourceWriter, signature, body);
  }

  /**
   * Factory for {@link GinjectorFragmentContext}.
   */
  interface Factory {
    InjectorWriteContext create(
        GinjectorBindings bindings,
        FragmentPackageName fragmentPackageName,
        SourceWriter sourceWriter);
  }
}
