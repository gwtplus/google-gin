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

package com.google.gwt.inject.generated.rebind;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

/**
 * Example generator creating a ginjector, Gin module and a class that uses
 * both.
 */
public class FrameworkGenerator extends Generator {

  @Override
  public String generate(TreeLogger logger, GeneratorContext context, String typeName)
      throws UnableToCompleteException {
    JClassType requestedType = context.getTypeOracle().findType(typeName);
    String packageName = requestedType.getPackage().getName();
    String requestedName = requestedType.getSimpleSourceName();
    String newClassName = requestedName + "Impl";

    String moduleName = createModule(logger, context, requestedType, packageName, requestedName);

    String ginjectorName =
        createGinjector(logger, context, packageName, requestedName, newClassName, moduleName);

    ClassSourceFileComposerFactory composerFactory =
        new ClassSourceFileComposerFactory(packageName, newClassName);
    composerFactory.setSuperclass(requestedType.getQualifiedSourceName());
    composerFactory.addImport(GWT.class.getCanonicalName());
    SourceWriter writer = composerFactory
        .createSourceWriter(context, context.tryCreate(logger, packageName, newClassName));
    writer.println("public void initialize() {");
    writer.indent();
    writer.println(
        "((" +  ginjectorName + ") GWT.create(" + ginjectorName + ".class)).injectMembers(this);");
    writer.outdent();
    writer.println("}");
    writer.commit(logger);

    return packageName + "." + newClassName;
  }

  private String createGinjector(TreeLogger logger, GeneratorContext context, String packageName,
      String requestedName, String newClassName, String moduleName) {
    String ginjectorName = requestedName + "Ginjector";
    ClassSourceFileComposerFactory ginjectorFactory =
        new ClassSourceFileComposerFactory(packageName, ginjectorName);
    ginjectorFactory.makeInterface();
    ginjectorFactory.addImplementedInterface(Ginjector.class.getCanonicalName());
    ginjectorFactory.addImport(GinModules.class.getCanonicalName());
    ginjectorFactory.addAnnotationDeclaration("@GinModules(" + moduleName + ".class)");
    SourceWriter ginjectorWriter = ginjectorFactory.createSourceWriter(context,
        context.tryCreate(logger, packageName, ginjectorName));
    ginjectorWriter.println("void injectMembers(" + newClassName + " obj);");
    ginjectorWriter.commit(logger);
    return ginjectorName;
  }

  private String createModule(TreeLogger logger, GeneratorContext context, JClassType requestedType,
      String packageName, String requestedName) {
    String moduleName = requestedName + "Module";
    ClassSourceFileComposerFactory moduleFactory =
        new ClassSourceFileComposerFactory(packageName, moduleName);
    moduleFactory.setSuperclass(AbstractGinModule.class.getCanonicalName());
    moduleFactory.addImport(Names.class.getCanonicalName());
    SourceWriter moduleWriter = moduleFactory.createSourceWriter(context,
        context.tryCreate(logger, packageName, moduleName));

    moduleWriter.println("public void configure() {");
    moduleWriter.indent();
    for (JMethod method : requestedType.getMethods()) {
      if (method.getName().startsWith("set")) {
        String name = method.getParameters()[0].getAnnotation(Named.class).value();
        moduleWriter.println("bindConstant().annotatedWith(Names.named(\"" + name + "\")).to(\""
            + Math.pow(Integer.parseInt(name), 2) + "\");");
      }
    }
    moduleWriter.outdent();
    moduleWriter.println("}");
    moduleWriter.commit(logger);
    return moduleName;
  }
}
