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
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.inject.rebind.binding.Injectable;
import com.google.gwt.inject.rebind.util.MemberCollector;
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
  private final JClassType ginjectorInterface;

  public GinjectorGeneratorModule(TreeLogger logger, GeneratorContext ctx,
      JClassType ginjectorInterface) {
    this.logger = logger;
    this.ctx = ctx;
    this.ginjectorInterface = ginjectorInterface;
  }

  @Override
  protected void configure() {
    bind(TreeLogger.class).toInstance(logger);
    bind(GeneratorContext.class).toInstance(ctx);
    bind(TypeOracle.class).toInstance(ctx.getTypeOracle());
    bind(JClassType.class).annotatedWith(GinjectorInterfaceType.class)
        .toInstance(ginjectorInterface);
  }

  @Provides
  @Injectable
  @Singleton
  MemberCollector provideInjectablesCollector(MemberCollector collector) {
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
}
