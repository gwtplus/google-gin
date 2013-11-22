/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.rebind.binding.Dependency;
import com.google.gwt.inject.rebind.binding.Injectable;
import com.google.gwt.inject.rebind.reflect.FieldLiteral;
import com.google.gwt.inject.rebind.reflect.MemberLiteral;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Util object that offers helper methods which can retrieve {@link Key Keys}
 * and additional dependency injection information on types or members.
 */
// TODO(schmitt): Figure out how to make this class entirely static (or move
// hasInject()).
@Singleton
public class GuiceUtil {
  private final MemberCollector memberCollector;

  @Inject
  public GuiceUtil(@Injectable MemberCollector memberCollector) {
    this.memberCollector = memberCollector;
  }

  /**
   * Retrieves a key based on the passed {@link Ginjector} method. If the
   * passed method is used for member injection, returns a key for the
   * parameter, otherwise for the method return type. Always uses the method's
   * binding annotation if present.
   *
   * @param method method for which to retrieve the key
   * @return key based on passed method
   */
  public Key<?> getKey(MethodLiteral<?, ?> method) {
    if (isMemberInject(method)) {
      return getKey(method.getParameterTypes().get(0).getType(), method.getBindingAnnotation());
    }
    return getKey(method.getReturnType().getType(), method.getBindingAnnotation());
  }

  /**
   * Returns a key based on the passed field, taking any binding annotations
   * into account.
   *
   * @param field field for which to retrieve the key
   * @return key for passed field
   */
  public Key<?> getKey(FieldLiteral<?> field) {
    return getKey(field.getFieldType().getType(), field.getBindingAnnotation());
  }

  /**
   * Gets the Guice binding key for a given Java type with optional
   * annotations.
   *
   * @param type Java type to convert in to a key
   * @param bindingAnnotation binding annotation for this key
   * @return Guice Key instance for this type/annotations
   * @throws ProvisionException in case of any failure
   */
  private Key<?> getKey(Type type, Annotation bindingAnnotation) throws ProvisionException {
    if (bindingAnnotation == null) {
      return Key.get(type);
    } else {
      return Key.get(type, bindingAnnotation);
    }
  }

  /**
   * Returns true if the passed {@link Ginjector} method is used for member
   * injection (i.e. takes exactly one parameter and returns void) or is a
   * regular {@link Ginjector} method that returns a type.
   *
   * @param method method to be checked
   * @return true if the passed method is used for member injection
   */
  public boolean isMemberInject(MethodLiteral<?, ?> method) {
    // TODO(schmitt): Consider returning an enum for the type of ginjector
    // method instead.
    return method.getReturnType().getRawType().equals(Void.TYPE);
  }

  /**
   * Returns true if the passed method has an {@literal @}{@code Inject}
   * annotation and the injection is marked as optional (
   * {@literal @}{@code Inject(optional = true)}).
   *
   * Note that {@link javax.inject.Inject} does not have an optional parameter
   * and therefore cannot be optional.
   *
   * @param member method to be checked
   * @return true if method is injected optionally
   */
  public boolean isOptional(MemberLiteral<?, ?> member) {
    Inject annotation = member.getAnnotation(Inject.class);
    return annotation != null && annotation.optional();
  }

  /**
   * Collects and returns all keys required to member-inject the given class.
   *
   * @param typeKey key causing member injection
   * @param type class for which required keys are calculated
   * @return keys required to inject given class
   */
  public Collection<Dependency> getMemberInjectionDependencies(
      Key<?> typeKey, TypeLiteral<?> type) {
    Set<Dependency> required = new LinkedHashSet<Dependency>();
    for (MethodLiteral<?, Method> method : memberCollector.getMethods(type)) {
      required.addAll(getDependencies(typeKey, method));
    }

    for (FieldLiteral<?> field : memberCollector.getFields(type)) {
      Key<?> key = getKey(field);
      required.add(new Dependency(typeKey, key, isOptional(field), false,
          "member injection of " + field));
    }
    return required;
  }

  /**
   * Collects and returns all keys required to inject the given method.
   *
   * @param typeKey the key that depends on injecting the arguments of method
   * @param method method for which required keys are calculated
   * @return required keys
   */
  public Collection<Dependency> getDependencies(Key<?> typeKey, MethodLiteral<?, ?> method) {
    String context;

    if (method.isConstructor()) {
      context = "@Inject constructor of " + method.getDeclaringType();
    } else if (typeKey == Dependency.GINJECTOR) {
      context = "Member injector " + method;
    } else {
      context = "Member injection via " + method;
    }

    Set<Dependency> required = new LinkedHashSet<Dependency>();
    for (Key<?> key : method.getParameterKeys()) {
      required.add(new Dependency(typeKey, key, isOptional(method), false, context));
    }
    return required;
  }

  /**
   * Returns {@code true} if the passed member has a inject annotation.
   */
  public static boolean hasInject(MemberLiteral<?, ?> member) {
    return member.isAnnotationPresent(Inject.class)
        || member.isAnnotationPresent(javax.inject.Inject.class);
  }
}
