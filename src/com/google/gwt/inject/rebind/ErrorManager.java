/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.inject.rebind.util.PrettyPrinter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Manages errors that occur during the binding process.  Keeps track of whether
 * we've found an error so we can eventually throw an
 * {@link UnableToCompleteException}. We do this instead of throwing
 * immediately so that we can find more than one error per compilation cycle.
 */
@Singleton
public class ErrorManager {
  private boolean foundError = false;
  private final TreeLogger logger;

  @Inject
  public ErrorManager(TreeLogger logger) {
    this.logger = logger;
  }

  public void logError(String message, Object... args) {
    logError(message, null, (Object[]) args);
  }

  public void logError(String message, Throwable t, Object... args) {
    logger.log(TreeLogger.ERROR, PrettyPrinter.format(message, args), t);
    foundError = true;
  }

  public void checkForError() throws UnableToCompleteException {
    if (foundError) {
      throw new UnableToCompleteException();
    }
  }
}
