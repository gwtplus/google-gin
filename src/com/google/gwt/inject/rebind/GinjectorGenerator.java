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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.inject.client.GinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.inject.Guice;
import com.google.inject.Module;

/**
 * Generator for implementations of {@link com.google.gwt.inject.client.Ginjector}.
 */
public class GinjectorGenerator extends Generator {

  public String generate(TreeLogger logger, GeneratorContext ctx, String requestedClass)
      throws UnableToCompleteException {

    ClassLoader classLoader = new GinBridgeClassLoader(ctx, logger);

    Class<? extends Ginjector> ginjectorInterface;
    try {
      ginjectorInterface = getGinjectorType(requestedClass, classLoader);
    } catch (ClassNotFoundException e) {
      logger.log(TreeLogger.ERROR, String.format("Unable to load ginjector type [%s], "
          + "maybe you haven't compiled your client java sources?", requestedClass), e);
      throw new UnableToCompleteException();
    } catch (IllegalArgumentException e) {
      logger.log(TreeLogger.Type.ERROR, e.getMessage(), e);
      throw new UnableToCompleteException();
    }

    // This is the Injector we use for the Generator internally,
    // it has nothing to do with user code.
    Module module = new GinjectorGeneratorModule(logger, ctx, ginjectorInterface,
        getConfigurationModules(ctx.getPropertyOracle(), logger, classLoader, ginjectorInterface));
    return Guice.createInjector(module).getInstance(GinjectorGeneratorImpl.class).generate();
  }

  @SuppressWarnings("unchecked")
  // Due to deferred binding we assume that the requested class has to be a
  // ginjector.
  private Class<? extends Ginjector> getGinjectorType(String requestedClass,
      ClassLoader classLoader) throws ClassNotFoundException {

    // We choose not to initialize ginjectors since we do not require it for reflective analysis and
    // some people statically call GWT.create in them (which is illegal during Gin generator runs).
    Class<?> type = loadClass(requestedClass, classLoader, false);
    if (!Ginjector.class.isAssignableFrom(type)) {
      throw new IllegalArgumentException("The type passed does not inherit from Ginjector - "
          + "please check the deferred binding rules.");
    }

    return (Class<? extends Ginjector>) type;
  }

  @SuppressWarnings("unchecked")
  // We check that the class is a GinModule before casting it.
  private Set<Class<? extends GinModule>> getConfigurationModules(PropertyOracle propertyOracle,
      TreeLogger logger, ClassLoader classLoader, Class<? extends Ginjector> ginjectorInterface)
      throws UnableToCompleteException {

    Set<String> propertyNames = new HashSet<String>();
    getPropertyNamesFromInjectorInterface(ginjectorInterface, propertyNames);
    
    Set<String> configurationModuleNames = new HashSet<String>();
    for (String propertyName : propertyNames) {
      Set<String> moduleNames = getModuleNamesFromPropertyName(propertyName, propertyOracle);
      if (moduleNames.isEmpty()) {
        logger.log(TreeLogger.Type.ERROR, String.format("The GinModules annotation requests "
            + "property %s, but this property cannot be found in the GWT module.", propertyName));
        throw new UnableToCompleteException();
      }
      configurationModuleNames.addAll(moduleNames);
    }

    Set<Class<? extends GinModule>> ginModules = 
          new HashSet<Class<? extends GinModule>>(configurationModuleNames.size());
    for (String moduleName : configurationModuleNames) {
      try {

        // Gin modules must be initialized when loading since we will instantiate it. It is
        // officially illegal to call any GWT-client code in a Gin module.
        Class<?> ginModule = loadClass(moduleName, classLoader, true);
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
    return ginModules;
  }

  private Set<String> getModuleNamesFromPropertyName(String propertyName,
      PropertyOracle propertyOracle) {
    try {
      // Result of getConfigurationProperty can never be null.
      return new HashSet<String>( 
          propertyOracle.getConfigurationProperty(propertyName).getValues());
    } catch (BadPropertyValueException e) {
      // Thrown when the configuration property is not defined.
      return Collections.emptySet();
    }
  }

  private void getPropertyNamesFromInjectorInterface(Class<?> ginjectorInterface,
      Set<String> propertyNames) {
    GinModules ginModulesAnnotation = ginjectorInterface.getAnnotation(GinModules.class);
    if (ginModulesAnnotation != null) {
      propertyNames.addAll(Arrays.asList(ginModulesAnnotation.properties()));
    }

    for (Class<?> ancestor : ginjectorInterface.getInterfaces()) {
      getPropertyNamesFromInjectorInterface(ancestor, propertyNames);
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
   * @param classLoader class loader to use to retrieve requested class
   * @param initialize whether to initialize the loaded class or not
   * @return loaded class
   * @throws ClassNotFoundException if no class could be found for the provided
   *    name
   */
  // Package accessible for testing.
  static Class<?> loadClass(String requestedClass, ClassLoader classLoader,
      boolean initialize) throws ClassNotFoundException {
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

  private static String replaceLastPeriodWithDollar(String input) {
    int where = input.lastIndexOf('.');
    if (where != -1) {
      return input.substring(0, where) + '$' + input.substring(where + 1);
    } else {
      return input;
    }
  }
}
