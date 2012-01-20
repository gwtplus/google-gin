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

package com.google.gwt.inject.rebind;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.inject.client.GinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.NoGinModules;
import com.google.inject.Guice;
import com.google.inject.Module;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Generator for implementations of {@link com.google.gwt.inject.client.Ginjector}.
 */
public class GinjectorGenerator extends Generator {

  // Visible for testing.
  ClassLoader classLoader;

  private PropertyOracle propertyOracle;

  private TreeLogger logger;

  @Override
  public String generate(TreeLogger logger, GeneratorContext context, String typeName)
      throws UnableToCompleteException {

    propertyOracle = context.getPropertyOracle();
    this.logger = logger;
    
    classLoader = createGinClassLoader(logger, context);

    Class<? extends Ginjector> ginjectorInterface;
    try {
      ginjectorInterface = getGinjectorType(typeName);
    } catch (ClassNotFoundException e) {
      this.logger.log(TreeLogger.ERROR, String.format("Unable to load ginjector type [%s], "
          + "maybe you haven't compiled your client java sources?", typeName), e);
      throw new UnableToCompleteException();
    } catch (IllegalArgumentException e) {
      this.logger.log(TreeLogger.Type.ERROR, e.getMessage(), e);
      throw new UnableToCompleteException();
    }

    // This is the Injector we use for the Generator internally,
    // it has nothing to do with user code.
    Module module = new GinjectorGeneratorModule(logger, context, ginjectorInterface,
        getModuleClasses(ginjectorInterface));
    return Guice.createInjector(module).getInstance(GinjectorGeneratorImpl.class).generate();
  }

  /**
   * Creates a new gin-specific class loader that will load GWT and non-GWT types such that there is
   * never a conflict, especially with super source.
   *
   * @param logger logger for errors that occur during class loading
   * @param context generator context in which classes are loaded
   * @return new gin class loader
   * @see GinBridgeClassLoader
   */
  private ClassLoader createGinClassLoader(TreeLogger logger, GeneratorContext context) {
    Set<String> exceptions = new LinkedHashSet<String>();
    exceptions.add("com.google.inject"); // Need the non-super-source version during generation.
    exceptions.add("javax.inject"); // Need the non-super-source version during generation.
    exceptions.add("com.google.gwt.inject.client"); // Excluded to allow class-literal comparison.

    // Add any excepted packages or classes registered by other developers.
    exceptions.addAll(getValuesForProperty("gin.classloading.exceptedPackages"));
    return new GinBridgeClassLoader(context, logger, exceptions);
  }

  @SuppressWarnings("unchecked")
  // Due to deferred binding we assume that the requested class has to be a ginjector.
  private Class<? extends Ginjector> getGinjectorType(String requestedClass)
      throws ClassNotFoundException {

    // We choose not to initialize ginjectors since we do not require it for reflective analysis and
    // some people statically call GWT.create in them (which is illegal during Gin generator runs).
    Class<?> type = loadClass(requestedClass, false);
    if (!Ginjector.class.isAssignableFrom(type)) {
      throw new IllegalArgumentException("The type passed does not inherit from Ginjector - "
          + "please check the deferred binding rules.");
    }

    return (Class<? extends Ginjector>) type;
  }

  private Set<Class<? extends GinModule>> getModuleClasses(Class<? extends Ginjector> ginjectorType)
      throws UnableToCompleteException {
    Set<Class<? extends GinModule>> ginModules = new LinkedHashSet<Class<? extends GinModule>>();
    getPropertyModuleClasses(ginjectorType, ginModules);
    getModuleClassesFromInjectorInterface(ginjectorType, ginModules);

    if (ginModules.isEmpty() && !ginjectorType.isAnnotationPresent(NoGinModules.class)) {
      logger.log(TreeLogger.Type.WARN,
          String.format("No gin modules are annotated on Ginjector %s, "
              + "did you forget the @GinModules annotation?", ginjectorType));
    }

    return ginModules;
  }

  @SuppressWarnings("unchecked") // We check that the class is a GinModule before casting it.
  private void getPropertyModuleClasses(Class<?> ginjectorType,
      Set<Class<? extends GinModule>> ginModules) throws UnableToCompleteException {
    Set<String> propertyModuleNames = getPropertyModuleNames(ginjectorType);
    for (String moduleName : propertyModuleNames) {
      try {

        // Gin modules must be initialized when loading since we will instantiate it. It is
        // officially illegal to call any GWT-client code in a Gin module.
        Class<?> ginModule = loadClass(moduleName, true);
        if (!GinModule.class.isAssignableFrom(ginModule)) {
          logger.log(TreeLogger.Type.ERROR, String.format("The gin module type [%s] does not "
              + "inherit from GinModule.", moduleName));
          throw new UnableToCompleteException();
        }
        ginModules.add((Class<? extends GinModule>) ginModule);
      } catch (ClassNotFoundException e) {
        logger.log(TreeLogger.ERROR, String.format("Unable to load gin module type [%s], "
            + "maybe you haven't compiled your client java sources?", moduleName), e);
        throw new UnableToCompleteException();
      }
    }
  }

  private Set<String> getPropertyModuleNames(Class<?> ginjectorType)
      throws UnableToCompleteException {
    Set<String> propertyNames = new LinkedHashSet<String>();
    getPropertyNamesFromInjectorInterface(ginjectorType, propertyNames);

    Set<String> configurationModuleNames = new LinkedHashSet<String>();
    for (String propertyName : propertyNames) {
      Set<String> moduleNames = getValuesForProperty(propertyName);
      if (moduleNames.isEmpty()) {
        logger.log(TreeLogger.Type.ERROR, String.format("The GinModules annotation requests "
            + "property %s, but this property cannot be found in the GWT module.", propertyName));
        throw new UnableToCompleteException();
      }
      configurationModuleNames.addAll(moduleNames);
    }
    return configurationModuleNames;
  }

  private Set<String> getValuesForProperty(String propertyName) {
    try {
      // Result of getConfigurationProperty can never be null.
      return new LinkedHashSet<String>(
          propertyOracle.getConfigurationProperty(propertyName).getValues());
    } catch (BadPropertyValueException e) {
      // Thrown when the configuration property is not defined.
      return Collections.emptySet();
    }
  }

  private void getPropertyNamesFromInjectorInterface(Class<?> ginjectorType,
      Set<String> propertyNames) {
    GinModules ginModulesAnnotation = ginjectorType.getAnnotation(GinModules.class);
    if (ginModulesAnnotation != null) {
      propertyNames.addAll(Arrays.asList(ginModulesAnnotation.properties()));
    }

    for (Class<?> ancestor : ginjectorType.getInterfaces()) {
      getPropertyNamesFromInjectorInterface(ancestor, propertyNames);
    }
  }

  private void getModuleClassesFromInjectorInterface(Class<?> ginjectorType,
      Set<Class<? extends GinModule>> moduleClasses) {
    GinModules ginModulesAnnotation = ginjectorType.getAnnotation(GinModules.class);
    if (ginModulesAnnotation != null) {
      moduleClasses.addAll(Arrays.asList(ginModulesAnnotation.value()));
    }

    for (Class<?> ancestor : ginjectorType.getInterfaces()) {
      getModuleClassesFromInjectorInterface(ancestor, moduleClasses);
    }
  }

  /**
   * Loads the class with the given name. If the provided name is a source name
   * this method tries to find the appropriate class.
   *
   * <p>For example, given a string {@code A.B.C}, this method will try to load
   * (in this order, returning upon found class): {@code A.B.C}, {@code A.B$C}
   * and {@code A$B$C}.
   *
   * @param requestedClass binary or source name of class to be loaded
   * @param initialize whether to initialize the loaded class or not
   * @return loaded class
   * @throws ClassNotFoundException if no class could be found for the provided name
   */
  // Package accessible for testing.
  Class<?> loadClass(String requestedClass, boolean initialize) throws ClassNotFoundException {
    String binaryName = requestedClass;
    while (true) {
      try {
        return Class.forName(binaryName, initialize, classLoader);
      } catch (ClassNotFoundException e) {
        if (!binaryName.contains(".")) {
          throw e;
        } else {
          binaryName = replaceLastPeriodWithDollar(binaryName);
        }
      }
    }
  }

  private String replaceLastPeriodWithDollar(String input) {
    int where = input.lastIndexOf('.');
    if (where != -1) {
      return input.substring(0, where) + '$' + input.substring(where + 1);
    } else {
      return input;
    }
  }
}
