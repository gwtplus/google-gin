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

package com.google.gwt.inject.rebind.reflect;

import com.google.inject.BindingAnnotation;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

import javax.inject.Named;

/**
 * Generic member representation preserving the member's type parameters.
 *
 * @see TypeLiteral
 */
public abstract class MemberLiteral<T, M extends Member & AnnotatedElement> {

  private final M member;
  private final TypeLiteral<T> declaringType;

  protected MemberLiteral(M member, TypeLiteral<T> declaringType) {
    this.member = member;
    this.declaringType = declaringType;
  }

  /**
   * Returns the type declaring this member.
   *
   * @return declaring type
   */
  public TypeLiteral<T> getDeclaringType() {
    return declaringType;
  }

  /**
   * Returns this member's name.
   *
   * @return name
   */
  public String getName() {
    return member.getName();
  }

  /**
   * Returns this member's raw (i.e. non-parametrized) declaring type.
   *
   * @return raw declaring type
   */
  public Class<?> getRawDeclaringType() {
    return getDeclaringType().getRawType();
  }

  /**
   * Returns this member's modifiers.
   *
   * @return modifiers
   */
  public int getModifiers() {
    return member.getModifiers();
  }

  /**
   * Returns this member's annotations.
   *
   * @return annotations
   */
  public Annotation[] getAnnotations() {
    return member.getAnnotations();
  }

  /**
   * Returns this member's annotation of the passed type or {@code null} if no
   * matching annotation is present.
   *
   * @param type annotation type
   * @return annotation instance or {@code null} if no matching annotation
   *    exists
   */
  public <T extends Annotation> T getAnnotation(Class<T> type) {
    return member.getAnnotation(type);
  }

  /**
   * Returns {@code true} if an annotation of the passed type is present on
   * this member.
   *
   * @param type annotation type to check for
   * @return {@code true} if the annotation is present
   */
  public boolean isAnnotationPresent(Class<? extends Annotation> type) {
    return member.isAnnotationPresent(type);
  }

  /**
   * Returns this member's binding annotation or {@code null} if no such
   * annotation is present.
   *
   * @return binding annotation or {@code null}
   */
  public Annotation getBindingAnnotation() {
    return getBindingAnnotation(getAnnotations());
  }

  /**
   * Returns {@code true} if this member is declared as default access.
   *
   * @return {@code true} if default access
   */
  public boolean isDefaultAccess() {
    return !Modifier.isPrivate(getModifiers())
        && !Modifier.isProtected(getModifiers())
        && !Modifier.isPublic(getModifiers());
  }

  /**
   * Returns {@code true} if this member is declared public.
   *
   * @return {@code true} if public
   */
  public boolean isPublic() {
    return Modifier.isPublic(getModifiers());
  }

  /**
   * Returns {@code true} if this member is declared private.
   *
   * @return {@code true} if private
   */
  public boolean isPrivate() {
    return Modifier.isPrivate(getModifiers());
  }

  /**
   * Returns {@code true} if this member is declared static.
   *
   * @return {@code true} if static
   */
  public boolean isStatic() {
    return Modifier.isStatic(getModifiers());
  }

  protected Annotation getBindingAnnotation(Annotation[] annotations) {
    Annotation bindingAnnotation = null;
    for (Annotation annotation : annotations) {
      if (annotation.annotationType().getAnnotation(BindingAnnotation.class) != null
          || annotation.annotationType() == Named.class) {
        if (bindingAnnotation != null) {
          throw new ProvisionException(
              String.format("More than one binding annotation found on %s: %s, %s.",
                   this, annotation, bindingAnnotation));
        }

        bindingAnnotation = annotation;

        // Keep going so we can find any rogue additional binding annotations
      }
    }

    return bindingAnnotation;
  }

  protected M getMember() {
    return member;
  }
}
