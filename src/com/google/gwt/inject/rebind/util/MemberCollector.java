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
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.inject.Inject;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
      public boolean accept(JMethod method) {
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
   * collected <b>despite being overriden</b> by A.
   */
  public static interface MethodFilter {
    boolean accept(JMethod method);
  }

  /**
   * Filter used during the collection of fields to restrict the kind of
   * collected fields.
   */
  public static interface FieldFilter {
    boolean accept(JField field);
  }

  /**
   * Comparator which detects methods that are override-equal.
   *
   * The comparator assumes that both classes have been investigated for java
   * specification compliance.
   */
  private static final Comparator<JMethod> METHOD_COMPARATOR = new Comparator<JMethod>() {
    public int compare(JMethod m1, JMethod m2) {
      if (m1 == m2) {
        return 0;
      }

      int nameCompare = m1.getName().compareTo(m2.getName());
      if (nameCompare != 0) {
        return nameCompare;
      }

      if (m1.getParameters().length != m2.getParameters().length) {
        return m1.getParameters().length - m2.getParameters().length;
      }

      for (int i = 0; i < m1.getParameters().length; i++) {
        String param1 = m1.getParameters()[i].getType().getQualifiedSourceName();
        String param2 = m2.getParameters()[i].getType().getQualifiedSourceName();

        int paramCompare = param1.compareTo(param2);
        if (paramCompare != 0) {
          return paramCompare;
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
      if (m1.isPrivate() || m2.isPrivate()
          || ((m1.isDefaultAccess() || m2.isDefaultAccess()) && !samePackage(m1, m2))) {
        return m1.getEnclosingType().getQualifiedSourceName().compareTo(
            m2.getEnclosingType().getQualifiedSourceName());
      }

      // Methods have same name, parameter types and compatible visibility
      return 0;
    }

    private boolean samePackage(JMethod m1, JMethod m2) {
      JPackage p1 = m1.getEnclosingType().getPackage();
      JPackage p2 = m2.getEnclosingType().getPackage();

      if (p1 == null || p2 == null) {
        return p1 == p2;
      }

      return (p1.isDefault() && p2.isDefault()) || (p1.getName().equals(p2.getName()));
    }
  };

  /**
   * Internal method cache: Type name -> Method Set
   */
  private final Map<String, Set<JMethod>> methodMultiMap = new HashMap<String, Set<JMethod>>();

  /**
   * Internal field cache: Type name -> Method Set
   */
  private final Map<String, Set<JField>> fieldMultiMap = new HashMap<String, Set<JField>>();

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
   *           requested
   */
  public void setMethodFilter(MethodFilter filter) throws IllegalStateException {
    if (locked) {
      String msg = "A filter can only be set on this collector before members are requested!";
      logger.log(TreeLogger.Type.ERROR, msg);
      throw new IllegalStateException(msg);
    }
    this.methodFilter = filter;
  }

  /**
   * Sets this collector's field filter.  This method can only be called before
   * members are requested.
   *
   * @param filter new field filter for this collector
   * @throws IllegalStateException if the filter is set after members have been
   *           requested
   */
  public void setFieldFilter(FieldFilter filter) throws IllegalStateException {
    if (locked) {
      String msg = "A filter can only be set on this collector before members are requested!";
      logger.log(TreeLogger.Type.ERROR, msg);
      throw new IllegalStateException(msg);
    }
    this.fieldFilter = filter;
  }

  /**
   * Returns all methods in the provided type, including those of the type's
   * (recursive) super classes and interfaces.  Treats overloads correctly.  If
   * no method filter is set will return an empty set.
   *
   * @param type type for which methods are collected
   * @return all methods for the given type
   */
  public Collection<JMethod> getMethods(JClassType type) {
    collect(type);
    String typeName = type.getParameterizedQualifiedSourceName();
    return Collections.unmodifiableCollection(methodMultiMap.get(typeName));
  }

  /**
   * Returns all fields in the provided type, including those of the type's
   * (recursive) super classes.  If no field filter is set will return an empty
   * set.
   *
   * @param type type for which fields are collected
   * @return all fields for the given type
   */
  public Collection<JField> getFields(JClassType type) {
    collect(type);
    String typeName = type.getParameterizedQualifiedSourceName();
    return Collections.unmodifiableCollection(fieldMultiMap.get(typeName));
  }

  private void collect(JClassType type) {
    locked = true;
    String typeName = type.getParameterizedQualifiedSourceName();

    if (methodMultiMap.containsKey(typeName)) {
      return;
    }

    // Type hasn't been collected yet.
    Set<JMethod> typeMethods = new TreeSet<JMethod>(METHOD_COMPARATOR);
    Set<JField> typeFields = new HashSet<JField>();
    accumulateMembers(type, typeMethods, typeFields);
    methodMultiMap.put(typeName, typeMethods);
    fieldMultiMap.put(typeName, typeFields);
  }

  private void accumulateMembers(JClassType type, Set<JMethod> methodAccu, Set<JField> fieldAccu) {
    String typeName = type.getParameterizedQualifiedSourceName();

    if (methodFilter != null) {
      if (methodMultiMap.containsKey(typeName)) {
        for (JMethod method : methodMultiMap.get(typeName)) {
          methodAccu.add(method);
        }
      } else {
        for (JMethod method : type.getMethods()) {
          if (methodFilter.accept(method)) {
            methodAccu.add(method);
            logger.log(TreeLogger.TRACE, "Found method: " + type.getName() + "#"
                + method.getReadableDeclaration());
          } else {
            logger.log(TreeLogger.DEBUG, "Ignoring method: " + type.getName() + "#"
                + method.getReadableDeclaration());
          }
        }
      }
    }

    if (fieldFilter != null) {
      if (fieldMultiMap.containsKey(typeName)) {
        for (JField field : fieldMultiMap.get(typeName)) {
          fieldAccu.add(field);
        }
      } else {
        for (JField field : type.getFields()) {
          if (fieldFilter.accept(field)) {
            fieldAccu.add(field);
            logger.log(TreeLogger.TRACE, "Found field: " + type.getName() + "#"
                + field.getName());
          } else {
            logger.log(TreeLogger.DEBUG, "Ignoring field: " + type.getName() + "#"
                + field.getName());
          }
        }
      }
    }

    for (JClassType ancestor : type.getImplementedInterfaces()) {
      accumulateMembers(ancestor, methodAccu, fieldAccu);
    }

    JClassType ancestor = type.getSuperclass();


    if (ancestor != null) {
      accumulateMembers(ancestor, methodAccu, fieldAccu);
    }
  }
}

