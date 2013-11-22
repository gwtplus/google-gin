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

import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import java.util.ArrayList;

/**
 * Utility class for source snippets.
 */
public final class SourceSnippets {
  private SourceSnippets() {
  }

  /**
   * Creates a snippet that evaluates to an injected instance of the given key,
   * as produced by the given child.
   */
  public static SourceSnippet callChildGetter(final GinjectorBindings childBindings,
      final Key<?> key) {
    return new SourceSnippet() {
      public String getSource(InjectorWriteContext writeContext) {
        return writeContext.callChildGetter(childBindings, key);
      }
    };
  }

  /**
   * Creates a snippet that evaluates to an injected instance of the given key
   * in the current {@link GinjectorBindings}.
   */
  public static SourceSnippet callGetter(final Key<?> key) {
    return new SourceSnippet() {
      public String getSource(InjectorWriteContext writeContext) {
        return writeContext.callGetter(key);
      }
    };
  }

  /**
   * Creates a snippet (including a trailing semicolon) that performs member
   * injection on a value of the given type.
   *
   * @param type the type of value to perform member injection on
   * @param input a Java expression that evaluates to the object that should be
   *     member-injected
   */
  public static SourceSnippet callMemberInject(final TypeLiteral<?> type, final String input) {
    return new SourceSnippet() {
      public String getSource(InjectorWriteContext writeContext) {
        return writeContext.callMemberInject(type, input);
      }
    };
  }

  /**
   * Creates a snippet that evaluates to an invocation of the named method on
   * the given package fragment.
   *
   * <p>Used when generating an intermediate invoker method; see
   * {@link MethodCallUtil#createMethodCallWithInjection}.
   */
  public static SourceSnippet callMethod(final String methodName, final String fragmentPackageName,
      final Iterable<String> parameters) {
    return new SourceSnippet() {
      public String getSource(InjectorWriteContext writeContext) {
        return writeContext.callMethod(methodName, fragmentPackageName, parameters);
      }
    };
  }

  /**
   * Creates a snippet that evaluates to an injected instance of the given key,
   * as produced by the given parent injector.
   */
  public static SourceSnippet callParentGetter(final Key<?> key,
      final GinjectorBindings parentBindings) {
    return new SourceSnippet() {
      public String getSource(InjectorWriteContext writeContext) {
        return writeContext.callParentGetter(key, parentBindings);
      }
    };
  }

  /**
   * Creates a snippet that evaluates to the implementation of the current
   * Ginjector interface.
   */
  public static SourceSnippet callGinjectorInterfaceGetter() {
    return new SourceSnippet() {
      public String getSource(InjectorWriteContext writeContext) {
        return writeContext.callGinjectorInterfaceGetter();
      }
    };
  }

  /** Creates a snippet that generates a constant text string. */
  public static SourceSnippet forText(final String text) {
    return new SourceSnippet() {
      public String getSource(InjectorWriteContext writeContext) {
        return text;
      }
    };
  }

  /**
   * Creates an {@link InjectorMethod} using the given {@link SourceSnippet} as
   * its body.
   *
   * @param isNative whether the returned method is a native method
   * @param signature the signature of the returned method
   * @param pkg the package in which the returned method should be created
   * @param body the body text of the new method
   */
  public static InjectorMethod asMethod(boolean isNative, String signature, String pkg,
      final SourceSnippet body) {
    return new AbstractInjectorMethod(isNative, signature, pkg) {
      public String getMethodBody(InjectorWriteContext writeContext) {
        return body.getSource(writeContext);
      }
    };
  }
}
