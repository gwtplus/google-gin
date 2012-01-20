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

package com.google.gwt.inject.rebind.util;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.inject.rebind.reflect.FieldLiteral;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.inject.Inject;
import com.google.inject.TypeLiteral;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class can be used to collect a type's members, including those of its
 * (recursive) superclasses and interfaces.  The collector treats overrides
 * correctly, i.e. it returns the method defined the closest to the provided
 * type.
 * <p>Note:  The collector uses internal caching and can be called with the same
 * arguments repeatedly without repeated performance costs.
 * <p>This class is not thread-safe.
 */
public class MemberCollector {

  public static final MethodFilter ALL_METHOD_FILTER =
    new MethodFilter() {
      public boolean accept(MethodLiteral<?, Method> method) {
        return true;
      }
    };

  // TODO(schmitt):  Add constructor collector?

  /**
   * Filter used during the collection of methods to restrict the kind of
   * collected methods.
   * <p/>
   * Note: The method filter influences override detection!  If a method A
   * overrides a method A*, without a filter only A would be collected.  If the
   * filter only accepts A* and not A, A will not be collected, and A* will be
   * collected <b>despite being overridden</b> by A.
   */
  public static interface MethodFilter {
    boolean accept(MethodLiteral<?, Method> method);
  }

  /**
   * Filter used during the collection of fields to restrict the kind of
   * collected fields.
   */
  public static interface FieldFilter {
    boolean accept(FieldLiteral<?> field);
  }

  /**
   * Comparator which detects methods that are override-equal.
   *
   * The comparator assumes that both classes have been investigated for java
   * specification compliance.
   */
  private static final Comparator<MethodLiteral<?, Method>> METHOD_COMPARATOR =
      new Comparator<MethodLiteral<?, Method>>() {
        public int compare(MethodLiteral<?, Method> m1, MethodLiteral<?, Method> m2) {
          if (m1 == m2) {
            return 0;
          }

          int nameCompare = m1.getName().compareTo(m2.getName());
          if (nameCompare != 0) {
            return nameCompare;
          }

          List<TypeLiteral<?>> parameters1 = m1.getParameterTypes();
          List<TypeLiteral<?>> parameters2 = m2.getParameterTypes();
          

          if (parameters1.size() != parameters2.size()) {
            return parameters1.size() - parameters2.size();
          }

          for (int i = 0; i < parameters1.size(); i++) {
            TypeLiteral<?> param1 = parameters1.get(i);
            TypeLiteral<?> param2 = parameters2.get(i);
            if (!param1.equals(param2)) {
              return param1.toString().compareTo(param2.toString());
            }
          }

          /* If either of the methods is private, it is either (a) in the
           * superclass, and thus invisible to the subclass, or (b) in the
           * subclass, which implies that the method must be private in the
           * superclass as well.
           *
           * If either of the methods has default access and the classes are not in
           * the same package then they are invisible to each other and thus not
           * override-equivalent.
           */

          if ((m1.isPrivate() || m2.isPrivate())
              || ((m1.isDefaultAccess() || m2.isDefaultAccess()) && !samePackage(m1, m2))) {
            return m1.getRawDeclaringType().getCanonicalName().compareTo(
                m2.getRawDeclaringType().getCanonicalName());
          }

          // Methods have same name, parameter types and compatible visibility
          return 0;
        }

        private boolean samePackage(MethodLiteral<?, Method> m1, MethodLiteral<?, Method> m2) {
          return m1.getRawDeclaringType().getPackage() == m2.getRawDeclaringType().getPackage();
        }
      };

  /**
   * Internal method cache: Type name -> Method Set
   */
  private final Map<TypeLiteral<?>, Set<MethodLiteral<?, Method>>> methodMultiMap =
      new LinkedHashMap<TypeLiteral<?>, Set<MethodLiteral<?, Method>>>();

  /**
   * Internal field cache: Type name -> Method Set
   */
  private final Map<TypeLiteral<?>, Set<FieldLiteral<?>>> fieldMultiMap =
      new LinkedHashMap<TypeLiteral<?>, Set<FieldLiteral<?>>>();

  private final TreeLogger logger;

  /**
   * Method filter that this collector operates with.
   */
  private MethodFilter methodFilter;

  /**
   * Field filter that this collector operates with.
   */
  private FieldFilter fieldFilter;

  /**
   * Locking status.  The collector is locked once it started to accumulate
   * members.  No filters can be set on the collector after it has been locked.
   */
  private boolean locked;

  @Inject
  public MemberCollector(TreeLogger logger) {
    this.logger = logger;
    this.locked = false;
  }

  /**
   * Sets this collector's method filter.  This method can only be called
   * before members are requested.
   *
   * @param filter new method filter for this collector
   * @throws IllegalStateException if the filter is set after members have been
   *    requested
   */
  public void setMethodFilter(MethodFilter filter) throws IllegalStateException {
    assertNotLocked();
    this.methodFilter = filter;
  }

  /**
   * Sets this collector's field filter.  This method can only be called before
   * members are requested.
   *
   * @param filter new field filter for this collector
   * @throws IllegalStateException if the filter is set after members have been
   *    requested
   */
  public void setFieldFilter(FieldFilter filter) throws IllegalStateException {
    assertNotLocked();
    this.fieldFilter = filter;
  }

  private void assertNotLocked() {
    if (locked) {
      String msg = "A filter can only be set on this collector before members are requested!";
      logger.log(TreeLogger.Type.ERROR, msg);
      throw new IllegalStateException(msg);
    }
  }

  /**
   * Returns all methods in the provided type, including those of the type's
   * (recursive) super classes and interfaces.  Treats overloads correctly.  If
   * no method filter is set will return an empty set.
   *
   * @param typeLiteral type for which methods are collected
   * @return all methods for the given type
   */
  public Collection<MethodLiteral<?, Method>> getMethods(TypeLiteral<?> typeLiteral) {
    collect(typeLiteral);
    return Collections.unmodifiableCollection(methodMultiMap.get(typeLiteral));
  }

  /**
   * Returns all fields in the provided type, including those of the type's
   * (recursive) super classes.  If no field filter is set will return an empty
   * set.
   *
   * @param typeLiteral type for which fields are collected
   * @return all fields for the given type
   */
  public Collection<FieldLiteral<?>> getFields(TypeLiteral<?> typeLiteral) {
    collect(typeLiteral);
    return Collections.unmodifiableCollection(fieldMultiMap.get(typeLiteral));
  }

  private void collect(TypeLiteral<?> typeLiteral) {
    locked = true;

    if (methodMultiMap.containsKey(typeLiteral)) {
      return;
    }

    // Type hasn't been collected yet.
    Set<MethodLiteral<?, Method>> typeMethods =
        new TreeSet<MethodLiteral<?, Method>>(METHOD_COMPARATOR);
    Set<FieldLiteral<?>> typeFields = new LinkedHashSet<FieldLiteral<?>>();
    accumulateMembers(typeLiteral, typeMethods, typeFields);
    methodMultiMap.put(typeLiteral, typeMethods);
    fieldMultiMap.put(typeLiteral, typeFields);
  }

  private void accumulateMembers(TypeLiteral<?> typeLiteral,
      Set<MethodLiteral<?, Method>> methodAccu, Set<FieldLiteral<?>> fieldAccu) {

    if (methodFilter != null) {
      if (methodMultiMap.containsKey(typeLiteral)) {
        for (MethodLiteral<?, Method> method : methodMultiMap.get(typeLiteral)) {
          methodAccu.add(method);
        }
      } else {
        for (MethodLiteral<?, Method> method : getTypeMethods(typeLiteral)) {
          if (methodFilter.accept(method)) {
            methodAccu.add(method);
            logger.log(TreeLogger.TRACE, String.format("Found method: %s", method));
          } else {
            logger.log(TreeLogger.DEBUG, String.format("Ignoring method: %s", method));
          }
        }
      }
    }

    if (fieldFilter != null) {
      if (fieldMultiMap.containsKey(typeLiteral)) {
        for (FieldLiteral<?> field : fieldMultiMap.get(typeLiteral)) {
          fieldAccu.add(field);
        }
      } else {
        for (FieldLiteral<?> field : getTypeFields(typeLiteral)) {
          if (fieldFilter.accept(field)) {
            fieldAccu.add(field);
            logger.log(TreeLogger.TRACE, String.format("Found field: %s", field));
          } else {
            logger.log(TreeLogger.DEBUG, String.format("Ignoring field: %s", field));
          }
        }
      }
    }

    for (Class<?> ancestor : typeLiteral.getRawType().getInterfaces()) {
      accumulateMembers(typeLiteral.getSupertype(ancestor), methodAccu, fieldAccu);
    }

    Class<?> ancestor = typeLiteral.getRawType().getSuperclass();

    if (ancestor != null) {
      accumulateMembers(typeLiteral.getSupertype(ancestor), methodAccu, fieldAccu);
    }
  }

  private <T> Iterable<MethodLiteral<T, Method>> getTypeMethods(TypeLiteral<T> typeLiteral) {
    List<MethodLiteral<T, Method>> methods = new ArrayList<MethodLiteral<T, Method>>();
    for (Method method : typeLiteral.getRawType().getDeclaredMethods()) {
      methods.add(MethodLiteral.get(method, typeLiteral));
    }
    return methods;
  }
  
  private <T> Iterable<FieldLiteral<T>> getTypeFields(TypeLiteral<T> typeLiteral) {
    List<FieldLiteral<T>> fields = new ArrayList<FieldLiteral<T>>();
    for (Field field : typeLiteral.getRawType().getDeclaredFields()) {
      fields.add(FieldLiteral.get(field, typeLiteral));
    }
    return fields;
  }
}

