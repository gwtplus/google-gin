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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.inject.Inject;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class can be used to collect a type's methods, including those of its
 * (recursive) superclasses and interfaces.  The collector treats overrides
 * correctly, i.e. it returns the method defined the closest to the provided
 * type.
 * <p>Note:  The collector uses internal caching and can be called with the same
 * arguments repeatedly without repeated performance costs.
 * <p>This class is not thread-safe.
 */
public class MethodCollector {

  /**
   * Comparator which detects methods that are override-equal.
   */
  private static final Comparator<JMethod> METHOD_COMPARATOR = new Comparator<JMethod>() {
    public int compare(JMethod o1, JMethod o2) {
      if (o1 == o2) {
        return 0;
      }

      if (!o1.getName().equals(o2.getName())) {
        return o1.getName().compareTo(o2.getName());
      }

      if (o1.getParameters().length != o2.getParameters().length) {
        return o1.getParameters().length - o2.getParameters().length;
      }

      for (int i = 0; i < o1.getParameters().length; i++) {
        String param1 = o1.getParameters()[i].getType().getQualifiedSourceName();
        String param2 = o2.getParameters()[i].getType().getQualifiedSourceName();

        if (!param1.equals(param2)) {
          return param1.compareTo(param2);
        }
      }

      // Methods have same name and parameter types
      return 0;
    }
  };

  /**
   * Filter used during the collection of methods to restrict the kind of
   * collected methods.
   */
  public static interface Filter {
    boolean accept(JMethod method);
  }

  /**
   * Internal type cache: Type -> Method Identifier -> Method
   */
  private final Map<String, Set<JMethod>> typeMap = new HashMap<String, Set<JMethod>>();

  private final TreeLogger logger;

  /**
   * Filter that this collector operates with.
   */
  private Filter filter;

  @Inject
  public MethodCollector(TreeLogger logger) {
    this.logger = logger;
  }

  /**
   * Sets this collectors filter.  This method can only be called once and must
   * be called before any methods are requested.
   *
   * @param filter this collectors filter
   * @throws IllegalStateException if the filter is set a second time
   */
  public void setFilter(Filter filter) {
    if (this.filter != null) {
      logger.log(TreeLogger.Type.ERROR, "A filter can only be set once on this collector!");
      throw new IllegalStateException("A filter can only be set once on this collector!");
    }
    this.filter = filter;
  }

  /**
   * Returns all methods in the provided type, including those of the type's
   * (recursive) super classes and interfaces.  Treats overloads correctly.  If
   * no filter is set uses a default filter that allows all methods.
   *
   * @param type type for which methods are collected
   * @return all methods for the given type
   */
  public Collection<JMethod> getMethods(JClassType type) {
    String typeName = type.getParameterizedQualifiedSourceName();

    if (!typeMap.containsKey(typeName)) {
      Set<JMethod> typeMethods = new TreeSet<JMethod>(METHOD_COMPARATOR);
      accumulateMethods(type, typeMethods);
      typeMap.put(typeName, typeMethods);
    }

    return typeMap.get(typeName);
  }

  private Filter getFilter() {
    if (filter == null) {
      filter = new Filter() {
        public boolean accept(JMethod method) {
          return true;
        }
      };
    }
    return filter;
  }

  private void accumulateMethods(JClassType type, Set<JMethod> accu) {
    String typeName = type.getParameterizedQualifiedSourceName();
    if (typeMap.containsKey(typeName)) {
      for (JMethod method : typeMap.get(typeName)) {
        accu.add(method);
      }
      return;
    }

    for (JMethod method : type.getMethods()) {
      if (getFilter().accept(method)) {
        accu.add(method);
        logger.log(TreeLogger.TRACE, "Found method: " + type.getName() + "#"
            + method.getReadableDeclaration());
      } else {
        logger.log(TreeLogger.DEBUG, "Ignoring method: " + type.getName() + "#"
            + method.getReadableDeclaration());
      }
    }

    for (JClassType ancestor : type.getImplementedInterfaces()) {
      accumulateMethods(ancestor, accu);
    }

    JClassType ancestor = type.getSuperclass();
    if (ancestor != null) {
      accumulateMethods(ancestor, accu);
    }
  }
}

