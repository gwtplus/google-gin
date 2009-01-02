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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.JavaSourceFile;
import com.google.gwt.dev.javac.impl.SourceFileCompilationUnit;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractUtilTester extends TestCase {
  private static final String PACKAGE = "com.google.gwt.inject.rebind.util.types";
  
  private TypeOracle typeOracle;

  protected TypeOracle getTypeOracle() {
    checkTypeOracle();
    return typeOracle;
  }

  protected JParameterizedType getParameterizedType(Class base, Class... parameters) {
    checkTypeOracle();

    JClassType[] params = new JClassType[parameters.length];
    int i = 0;
    for (Class parameter : parameters) {
      params[i] = getClassType(parameter);
    }

    return typeOracle.getParameterizedType((JGenericType) getClassType(base), params);
  }

  protected JArrayType getArrayType(Class componentType) {
    checkTypeOracle();
    return typeOracle.getArrayType(getType(componentType));
  }

  private JType getType(Class clazz) {
    JType type;
    if (clazz.isPrimitive()) {
      type = getPrimitiveType(clazz);
    } else {
      type = getClassType(clazz);
    }
    return type;
  }

  protected JPrimitiveType getPrimitiveType(Class type) {
    return JPrimitiveType.valueOf(type.getName());
  }

  protected JClassType getClassType(Class type) {
    return getClassType(type.getName());
  }

  protected JClassType getClassType(String className) {
    checkTypeOracle();

    try {
      return typeOracle.getType(className);
    } catch (NotFoundException e) {
      throw new RuntimeException("Failed during type retrieval!", e);
    }
  }

  private void checkTypeOracle() {
    if (typeOracle == null) {
      PrintWriterTreeLogger logger = new PrintWriterTreeLogger();
      logger.setMaxDetail(TreeLogger.Type.WARN);

      CompilationState compilationState;
      try {
        ModuleDef userModule =
            ModuleDefLoader.loadFromClassPath(logger, "com.google.gwt.core.Core");

        compilationState = userModule.getCompilationState();

        for (CompilationUnit unit : getTestUnits()) {
          compilationState.addGeneratedCompilationUnit(unit);
        }

        compilationState.compile(logger);
      } catch (UnableToCompleteException e) {
        throw new RuntimeException("Failed during compiler intialization!", e);
      }

      typeOracle = compilationState.getTypeOracle();
    }
  }

  private Set<CompilationUnit> getTestUnits() {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    units.add(new SourceFileCompilationUnit(new MyJavaSourceFile(PACKAGE, "SuperInterface")));
    units.add(new SourceFileCompilationUnit(new MyJavaSourceFile(PACKAGE, "SimpleInterface")));
    units.add(new SourceFileCompilationUnit(new MyJavaSourceFile(PACKAGE, "SubInterface")));
    units.add(new SourceFileCompilationUnit(new MyJavaSourceFile(PACKAGE, "SuperClass")));
    units.add(new SourceFileCompilationUnit(new MyJavaSourceFile(PACKAGE, "SubClass")));
    units.add(new SourceFileCompilationUnit(new MyJavaSourceFile(PACKAGE, "WildcardFieldClass")));
    return units;
  }

  private static class MyJavaSourceFile extends JavaSourceFile {

    private final String packageName;
    private final String shortName;

    public MyJavaSourceFile(String packageName, String shortName) {
      this.packageName = packageName;
      this.shortName = shortName;
    }

    public String getLocation() {
      return getTypeName();
    }

    public String getPackageName() {
      return packageName;
    }

    public String getShortName() {
      return shortName;
    }

    public String getTypeName() {
      return packageName + "." + shortName;
    }

    public String readSource() {
      String fileName = getTypeName().replaceAll("\\.", "/") + ".java";
      String filePath = getClass().getClassLoader().getResource(fileName).getPath();
      return Util.readFileAsString(new File(filePath));
    }
  }
}
