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

package com.google.gwt.inject.rebind.util;

import com.google.inject.TypeLiteral;

import java.util.ArrayList;

/**
 * Utility class for source snippets.
 */
public final class SourceSnippets {
  private SourceSnippets() {
  }

  /**
   * Create a snippet that invokes {@link InjectorWriteContext#callMemberInject}.
   */
  public static SourceSnippet callMemberInject(final TypeLiteral<?> type, final String input) {
    return new SourceSnippet() {
      public String getSource(InjectorWriteContext writeContext) {
        return writeContext.callMemberInject(type, input);
      }
    };
  }

  /**
   * Create a snippet that invokes {@link InjectorWriteContext#callMethod}.
   */
  public static SourceSnippet callMethod(final String methodName, final String fragmentPackageName,
      final Iterable<String> parameters) {
    return new SourceSnippet() {
      public String getSource(InjectorWriteContext writeContext) {
        return writeContext.callMethod(methodName, fragmentPackageName, parameters);
      }
    };
  }

  public static SourceSnippet forText(final String text) {
    return new SourceSnippet() {
      public String getSource(InjectorWriteContext writeContext) {
        return text;
      }
    };
  }

  public static InjectorMethod asMethod(boolean isNative, String signature, String pkg,
      final SourceSnippet body) {
    return new AbstractInjectorMethod(isNative, signature, pkg) {
      public String getMethodBody(InjectorWriteContext writeContext) {
        return body.getSource(writeContext);
      }
    };
  }
}
