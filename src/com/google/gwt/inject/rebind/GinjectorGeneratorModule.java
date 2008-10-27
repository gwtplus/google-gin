/**
 * Copyright (C) 2008 Wideplay Interactive.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.gwt.inject.rebind;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.inject.rebind.binding.Injectables;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * Guice module used in the implementation of {@link GinjectorGenerator}.
 * <p>
 * IMPLEMENTATION NOTE: thread safety concerns depend on how we create the Injector in
 * the {@link GinjectorGenerator} class, so make sure you consult that class before
 * adding bindings.
 */
class GinjectorGeneratorModule extends AbstractModule {

  private final TreeLogger logger;
  private final GeneratorContext ctx;

  public GinjectorGeneratorModule(TreeLogger logger, GeneratorContext ctx) {
    this.logger = logger;
    this.ctx = ctx;
  }

  @Override
  protected void configure() {
    bind(TreeLogger.class).toInstance(logger);
    bind(GeneratorContext.class).toInstance(ctx);
    bind(NameGenerator.class).to(NameGeneratorImpl.class).in(Singleton.class);
  }

  @Provides @Injectables @Singleton
  MethodCollector provideInjectablesMethodCollector(@Injectables MethodCollector.Filter filter,
      MethodCollector collector) {
    collector.setFilter(filter);
    return collector;
  }

  @Provides @Injectables @Singleton
  MethodCollector.Filter provideInjectablesMethodFilter() {
    return new MethodCollector.Filter() {
       public boolean accept(JMethod method) {
         return method.isAnnotationPresent(Inject.class);
       }
     };
  }
}
