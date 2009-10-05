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
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.JavaSourceFile;
import com.google.gwt.dev.javac.impl.SourceFileCompilationUnit;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.inject.Inject;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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

        compilationState = userModule.getCompilationState(logger);

        compilationState.addGeneratedCompilationUnits(logger, getTestUnits());

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
    units.add(new SourceFileCompilationUnit(new MyJavaSourceFile(PACKAGE, "MethodsClass")));
    units.add(new SourceFileCompilationUnit(new MyJavaSourceFile(PACKAGE, "Parameterized")));
    units.add(
        new SourceFileCompilationUnit(new MyJavaSourceFile(PACKAGE + ".secret", "SecretSubClass")));
    return units;
  }

  protected MemberCollector createInjectableCollector() {
    MemberCollector collector = new MemberCollector(TreeLogger.NULL);
    collector.setMethodFilter(
        new MemberCollector.MethodFilter() {
          public boolean accept(JMethod method) {
            // TODO(schmitt): Do injectable methods require at least one parameter?
            return method.isAnnotationPresent(Inject.class) && !method.isStatic();
          }
        });

    collector.setFieldFilter(
        new MemberCollector.FieldFilter() {
          public boolean accept(JField field) {
            return field.isAnnotationPresent(Inject.class) && !field.isStatic();
          }
        });
    return collector;
  }

  /**
   * Please note: Some of the methods here are redundant and/or superfluous but
   * are included in anticipation of the next GWT release, where Resource will
   * replace JavaSourceFile.
   */
  private static class MyJavaSourceFile extends JavaSourceFile {

    private final String packageName;
    private final String shortName;
    private File file;

    public MyJavaSourceFile(String packageName, String shortName) {
      this.packageName = packageName;
      this.shortName = shortName;

      String fileName = getLocation().replaceAll("\\.", "/") + ".java";
      URI fileUri;
      try {
        fileUri = getClass().getClassLoader().getResource(fileName).toURI();
      } catch (URISyntaxException e) {
        throw new RuntimeException(e); 
      }
      file = new File(fileUri);
    }

    public String getLocation() {
      return packageName + "." + shortName;
    }

    public String getPackageName() {
      return packageName;
    }

    public String getShortName() {
      return shortName;
    }

    public String getTypeName() {
      return getLocation();
    }

    public boolean isSuperSource() {
      return false;
    }

    public String readSource() {
      return Util.readStreamAsString(openContents());
    }

    public String getPath() {
      return getLocation() + ".java";
    }

    public URL getURL() {
      try {
        return file.toURL();
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }

    public InputStream openContents() {
      try {
        return new FileInputStream(file);
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }
    }

    public boolean wasRerooted() {
      return false;
    }

    public long getLastModified() {
      return 0;
    }
  }
}
