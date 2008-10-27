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

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.inject.Guice;
import com.google.inject.Stage;

/**
 * Generator for implementations of {@link com.google.gwt.inject.client.Ginjector}.
 */
public class GinjectorGenerator extends Generator {

  public String generate(TreeLogger logger, GeneratorContext ctx, String requestedClass)
      throws UnableToCompleteException {

    // This is the Injector we use for the Generator internally,
    // it has nothing to do with user code.
    return Guice.createInjector(Stage.PRODUCTION, new GinjectorGeneratorModule(logger, ctx))
        .getInstance(GinjectorGeneratorImpl.class).generate(requestedClass);
  }
}
