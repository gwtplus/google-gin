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
import com.google.gwt.inject.client.Ginjector;
import com.google.inject.Inject;
import com.google.inject.Singleton;

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

  /**
   * Interface of the injector that this class is implementing.
   */
  private final Class<? extends Ginjector> ginjectorInterface;
  private final GinjectorOutputter outputter;

  @Inject
  public GinjectorGeneratorImpl(TreeLogger logger, GeneratorContext ctx,
      BindingsProcessor bindingsProcessor,
      @GinjectorInterfaceType Class<? extends Ginjector> ginjectorInterface,
      GinjectorOutputter outputter) {
    this.logger = logger;
    this.ctx = ctx;
    this.bindingsProcessor = bindingsProcessor;
    this.ginjectorInterface = ginjectorInterface;
    this.outputter = outputter;
  }

  public String generate() throws UnableToCompleteException {
    validateInjectorClass();

    Package interfacePackage = ginjectorInterface.getPackage();
    String packageName = interfacePackage == null ? "" : interfacePackage.getName();
    String implClassName = ginjectorInterface.getName().replace(".", "_") + "Impl";
    String generatedClassName = packageName + "." + implClassName;

    PrintWriter printWriter = ctx.tryCreate(logger, packageName, implClassName);
    if (printWriter == null) {
      // We've already created it, so nothing to do
    } else {
      bindingsProcessor.process();
      outputter.output(packageName, implClassName, printWriter);
    }

    return generatedClassName;
  }

  private void validateInjectorClass() throws UnableToCompleteException {
    if (!ginjectorInterface.isInterface()) {
      logger.log(TreeLogger.ERROR,
          ginjectorInterface.getCanonicalName() + " is not an interface", null);
      throw new UnableToCompleteException();
    }
  }
}
