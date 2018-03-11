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
package com.google.gwt.inject.rebind.binding;

import com.google.gwt.inject.rebind.util.PrettyPrinter;
import com.google.inject.spi.Element;

/**
 * Describes the context in which a binding or dependency was created.
 */
public class Context {

  private final String contextFormat;
  private final Object[] contextArgs;

  protected Context(String contextFormat, Object... contextArgs) {
    this.contextFormat = contextFormat;
    this.contextArgs = contextArgs;
  }

  @Override
  public String toString() {
    return PrettyPrinter.format(contextFormat, contextArgs);
  }

  /**
   * Create a {@code Context} storing the context of the given Guice
   * {@link Element}.
   */
  public static Context forElement(Element element) {
    return new Context("%s", element.getSource());
  }

  /**
   * Create a {@code Context} from a text string.
   */
  public static Context forText(String text) {
    return new Context("%s", text);
  }

  /**
   * Create a {@code Context} from formatted text.  The text will not be
   * formatted until {@link #toString()} is invoked.
   *
   * @param contextFmt format string, as with {@link PrettyPrinter#format(String, Object...)}
   * @param args arguments for the format string, as with
   *     {@link PrettyPrinter#format(String, Object...)}
   */
  public static Context format(String contextFmt, Object... args) {
    return new Context(contextFmt, args);
  }
}
