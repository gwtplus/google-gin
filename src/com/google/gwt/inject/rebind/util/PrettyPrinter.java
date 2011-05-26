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

import com.google.gwt.inject.rebind.binding.Dependency;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import java.lang.StringBuilder;
import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Pretty-printer that formats internal types for human consumption in error
 * messages.
 *
 * <p>{@link #format(String, Object...)} acts like {@link String#format}, except
 * that it detects and pretty-prints the following argument types:
 *
 * <ul>
 * <li>{@link Class}: formatted as "org.example.Class$SubClass"</li>
 * <li>{@link Key}: formatted as "@org.example.Annotation org.example.Class$SubClass"</li>
 * <li>{@link List<Dependency>}: formatted as a dependency path preceded by a newline.  If
 *     the path begins at {@link Dependency.GINJECTOR}, that key is hidden, and the context
 *     of the outgoing dependency is given as the context of the first key in the displayed
 *     path.</li>
 * </ul>
 *
 * All other arguments are passed unchanged to {@link String#format}.
 */
public final class PrettyPrinter {

  private PrettyPrinter() {
  }

  /**
   * Generate a string based on a format template as {@link String#format}
   * would, using the pretty-printing rules specified in the class
   * documentation.
   */
  public static String format(String formatString, Object... args) {
    // Rewrites the varargs so that objects that need special formatting are
    // replaced with formatted strings, then hands off to {@link String#format}.

    Object[] formattedArgs = new Object[args.length];
    for (int i = 0; i < args.length; ++i) {
      formattedArgs[i] = formatObject(args[i]);
    }

    return String.format(formatString, (Object[]) formattedArgs);
  }

  /**
   * Pretty-print a single object.
   */
  private static Object formatObject(Object object) {
    if (object instanceof Class) {
      return formatArg((Class<?>) object);
    } else if (object instanceof Key) {
      return formatArg((Key<?>) object);
    } else if (object instanceof List) {
      List<?> list = (List<?>) object;
      // Empirically check if this is a List<Dependency>.
      boolean allDependencies = true;
      for (Object entry : list) {
        if (!(entry instanceof Dependency)) {
          allDependencies = false;
          break;
        }
      }

      if (allDependencies) {
        return formatArg((List<Dependency>) list);
      } else {
        return object;
      }
    } else {
      return object;
    }
  }

  private static String formatArg(Class<?> type) {
    // Make sure classes are formatted in a manner that's consistent with type
    // literals.
    return TypeLiteral.get(type).toString();
  }

  private static String formatArg(Key<?> key) {
    StringBuilder builder = new StringBuilder();
    formatArgTo(key, builder);
    return builder.toString();
  }

  /**
   * Formats a list of dependencies as a dependency path; see the class
   * comments.
   */
  private static String formatArg(List<Dependency> path) {
    StringBuilder builder = new StringBuilder();
    formatArgTo(path, builder);
    return builder.toString();
  }

  private static void formatArgTo(Key<?> key, StringBuilder builder) {
    if (key.getAnnotation() != null) {
      builder.append("@");
      builder.append(formatArg(key.getAnnotation().annotationType()));
      builder.append(" ");
    } else if (key.getAnnotationType() != null) {
      builder.append("@");
      builder.append(formatArg(key.getAnnotationType()));
      builder.append(" ");
    }
    builder.append(key.getTypeLiteral());
  }

  /**
   * Formats a list of dependencies as a dependency path; see the class
   * comments.
   */
  private static void formatArgTo(List<Dependency> path, StringBuilder builder) {
    if (path.isEmpty()) {
      return;
    }

    builder.append("\n");
    boolean first = true;
    Key<?> previousTarget = null; // For sanity-checking.
    for (Dependency dependency : path) {
      Key<?> source = dependency.getSource();
      Key<?> target = dependency.getTarget();

      // Sanity-check.
      if (previousTarget != null && !previousTarget.equals(source)) {
        throw new IllegalArgumentException("Dependency list is not a path.");
      }

      // There are two possible overall shapes of the list:
      //
      // If it starts with GINJECTOR, we get this:
      //
      // Key1 [context]
      //  -> Key2 [context]
      //  ...
      //
      // Otherwise (e.g., if we're dumping a cycle), we get this:
      //
      // Key1
      //  -> Key2 [context]
      //  -> Key3 [context]
      //  ...
      if (first) {
        if (source == Dependency.GINJECTOR) {
          formatArgTo(target, builder);
          builder.append(String.format(" [%s]%n", dependency.getContext()));
        } else {
          formatArgTo(source, builder);
          builder.append("\n -> ");
          formatArgTo(target, builder);
          builder.append(String.format(" [%s]%n", dependency.getContext()));
        }

        first = false;
      } else {
        builder.append(" -> ");
        formatArgTo(target, builder);
        builder.append(String.format(" [%s]%n", dependency.getContext()));
      }

      previousTarget = target;
    }
  }
}
