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

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.inject.rebind.output.GinjectorImplOutputter;
import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

import java.io.PrintWriter;

/**
 * Does the heavy lifting involved in generating implementations of
 * {@link Ginjector}. This class is instantiated
 * once per class to generate, so it can keep useful state around in its fields.
 * (It is a singleton from Guice's perspective since we create a new
 * injector for each generation run.)
 */
@Singleton
class GinjectorGeneratorImpl {
  private final TreeLogger logger;
  private final GeneratorContext ctx;
  private final BindingsProcessor bindingsProcessor;
  private final GinjectorImplOutputter outputter;

  /**
   * Convenience cache of rootBindings.getGinjectorInterface().
   */
  private final TypeLiteral<?> ginjectorInterface;

  /**
   * The bindings associated with the Ginjector to output.
   */
  private final GinjectorBindings rootBindings;

  @Inject
  public GinjectorGeneratorImpl(TreeLogger logger, GeneratorContext ctx,
      BindingsProcessor bindingsProcessor,
      @RootBindings GinjectorBindings rootBindings,
      GinjectorImplOutputter outputter) {
    this.logger = logger;
    this.ctx = ctx;
    this.bindingsProcessor = bindingsProcessor;
    this.ginjectorInterface = rootBindings.getGinjectorInterface();
    this.outputter = outputter;
    this.rootBindings = rootBindings;
  }

  public String generate() throws UnableToCompleteException {
    validateInjectorClass();

    Package interfacePackage = ginjectorInterface.getRawType().getPackage();
    String packageName = interfacePackage == null ? "" : interfacePackage.getName();
    String implClassName = getImplClassName();
    String generatedClassName = packageName + "." + implClassName;

    PrintWriter printWriter = ctx.tryCreate(logger, packageName, implClassName);
    if (printWriter == null) {
      // We've already created it, so nothing to do
    } else {
      bindingsProcessor.process();
      outputter.write(packageName, implClassName, printWriter, rootBindings);
    }

    return generatedClassName;
  }

  private String getImplClassName()
      throws UnableToCompleteException {
    try {
      return ReflectUtil.getSourceName(ginjectorInterface).replace(".", "_") + "Impl";
    } catch (NoSourceNameException e) {
      logger.log(TreeLogger.Type.ERROR, "Could not determine source name for ginjector", e);
      throw new UnableToCompleteException();
    }
  }

  private void validateInjectorClass()
      throws UnableToCompleteException {
    if (!ginjectorInterface.getRawType().isInterface()) {
      logger.log(TreeLogger.ERROR,
          ginjectorInterface.getRawType().getCanonicalName() + " is not an interface", null);
      throw new UnableToCompleteException();
    }
  }
}
