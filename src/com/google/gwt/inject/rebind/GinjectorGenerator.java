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
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.inject.Guice;
import com.google.inject.Module;

/**
 * Generator for implementations of {@link com.google.gwt.inject.client.Ginjector}.
 */
public class GinjectorGenerator extends Generator {

  public String generate(TreeLogger logger, GeneratorContext ctx, String requestedClass)
      throws UnableToCompleteException {
    
    Class<? extends Ginjector> ginjectorInterface;
    try {
      ginjectorInterface = getGinjectorType(requestedClass);
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
        getConfigurationModules(ginjectorInterface, ctx.getPropertyOracle(), logger));
    return Guice.createInjector(module).getInstance(GinjectorGeneratorImpl.class).generate();
  }

  @SuppressWarnings("unchecked")
  // Due to deferred binding we assume that the requested class has to be a
  // ginjector.
  private Class<? extends Ginjector> getGinjectorType(String requestedClass)
      throws ClassNotFoundException {
    Class<?> type = ReflectUtil.loadClass(requestedClass);
    if (!Ginjector.class.isAssignableFrom(type)) {
      throw new IllegalArgumentException("The type passed does not inherit from Ginjector - "
          + "please check the deferred binding rules.");
    }

    return (Class<? extends Ginjector>) type;
  }

  @SuppressWarnings("unchecked")
  // We check that the class is a GinModule before casting it.
  private Set<Class<? extends GinModule>> getConfigurationModules(
      Class<? extends Ginjector> ginjectorInterface,
      PropertyOracle propertyOracle,
      TreeLogger logger) throws UnableToCompleteException {

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
        Class<?> ginModule = ReflectUtil.loadClass(moduleName);
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
      // TODO(schmitt): Only look at ancestors extending Ginjector?
      getPropertyNamesFromInjectorInterface(ancestor, propertyNames);
    }
  }

}
