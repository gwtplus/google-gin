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

import com.google.gwt.dev.util.Preconditions;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.gwt.inject.rebind.util.SourceWriteUtil.join;

/**
 * Generic method and constructor representation that preserves the member's
 * parametrization and allows common operations on methods and constructors
 * where appropriate.
 *
 * @see TypeLiteral
 */
public abstract class MethodLiteral<T, M extends Member & AnnotatedElement & GenericDeclaration>
    extends MemberLiteral<T, M> {

  /**
   * Creates a new {@code MethodLiteral} based on the passed method and its
   * declaring type.
   *
   * @param method method for which the literal is constructed
   * @param declaringType type declaring the passed method
   * @return new method literal
   */
  public static <T> MethodLiteral<T, Method> get(Method method, TypeLiteral<T> declaringType) {
    Preconditions.checkArgument(method.getDeclaringClass().equals(declaringType.getRawType()),
        "declaringType (%s) must be the type literal where method was declared (%s)!",
        declaringType, method.getDeclaringClass());
    return new ConcreteMethodLiteral<T>(method, declaringType);
  }

  /**
   * Creates a new {@code MethodLiteral} based on the passed constructor and
   * its declaring type.
   *
   * @param constructor constructor for which the literal is constructed
   * @param declaringType type declaring the constructor
   * @return new method literal
   */
  public static <T> MethodLiteral<T, Constructor<?>> get(Constructor<?> constructor,
      TypeLiteral<T> declaringType) {
    Preconditions.checkArgument(
        constructor.getDeclaringClass().equals(declaringType.getRawType()),
        "declaringType (%s) must be the type literal where constructor was declared (%s)!",
        declaringType, constructor.getDeclaringClass());
    return new ConstructorLiteral<T>(constructor, declaringType);
  }

  /**
   * Cache of parameter keys.
   */
  private List<Key<?>> parameterKeys;

  /**
   * Cache of parameter types.
   */
  private List<TypeLiteral<?>> parameterTypes;

  protected MethodLiteral(M member, TypeLiteral<T> declaringType) {
    super(member, declaringType);
  }

  /**
   * Returns this method's parameter keys, if appropriate parametrized with the
   * declaring class's type parameters.
   *
   * @return parameter keys
   */
  public List<Key<?>> getParameterKeys() {
    if (parameterKeys == null) {
      compileParameterKeys();
    }
    return parameterKeys;
  }

  /**
   * Returns this method's parameter types, if appropriate parametrized with
   * the declaring class's type parameters.
   *
   * @return parameter types
   */
  public List<TypeLiteral<?>> getParameterTypes() {
    if (parameterTypes == null) {
      parameterTypes = getDeclaringType().getParameterTypes(getMember());
    }
    return parameterTypes;
  }

  /**
   * Returns this method's parameter types without any type parametrization
   * applied.
   *
   * For example, {@code &lt;T> void foo(T t)} will return a list with a single
   * entry, a {@link TypeVariable} with name {@code T} and bounded by
   * {@code java.lang.Object}.
   *
   * @return raw parameter types
   */
  public abstract List<Type> getRawParameterTypes();

  /**
   * Returns this method's exception types, if appropriate parametrized with
   * the declaring class's type parameters.
   *
   * @return exception types
   */
  public List<TypeLiteral<?>> getExceptionTypes() {
    return getDeclaringType().getExceptionTypes(getMember());
  }

  /**
   * Returns this method's type parameters.
   *
   * @return type parameters
   */
  public TypeVariable<?>[] getTypeParameters() {
    return getMember().getTypeParameters();
  }

  /**
   * Returns this method's return type, if appropriate parametrized with the
   * declaring class's type parameters.
   *
   * @return return type
   */
  public abstract TypeLiteral<?> getReturnType();

  /**
   * Returns {@code true} if this method literal is based on a constructor.
   */
  public abstract boolean isConstructor();

  protected abstract Annotation[][] getParameterAnnotations();

  private void compileParameterKeys() {
    parameterKeys = new ArrayList<Key<?>>(getParameterTypes().size());
    int i = 0;
    for (TypeLiteral<?> parameterType : getParameterTypes()) {
      Annotation bindingAnnotation = getBindingAnnotation(getParameterAnnotations()[i]);
      if (bindingAnnotation != null) {
        parameterKeys.add(Key.get(parameterType, bindingAnnotation));
      } else {
        parameterKeys.add(Key.get(parameterType));
      }
      i++;
    }
  }

  /**
   * Returns the method's declaring type and name in the format used in
   * javadoc, e.g. {@code com.bar.Foo#baz(com.bar.Foo, com.bar.Bar)}, with
   * resolved type parameters.
   *
   * @return string representation for this method including the declaring type
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getName()).append("(");

    List<String> parameters = new ArrayList<String>();
    for (TypeLiteral<?> parameterType : getParameterTypes()) {
      // TODO(schmitt): We are not respecting varargs here.
      parameters.add(parameterType.toString());
    }
    sb.append(join(", ", parameters)).append(")");

    return String.format("%s#%s", getDeclaringType(), sb.toString());
  }

  /**
   * Constructor-specific implementation of {@link MethodLiteral}.
   */
  private static class ConstructorLiteral<T> extends MethodLiteral<T, Constructor<?>> {
  
    ConstructorLiteral(Constructor<?> constructor, TypeLiteral<T> declaringType) {
      super(constructor, declaringType);
    }

    @Override
    public TypeLiteral<?> getReturnType() {
      return getDeclaringType();
    }

    @Override
    public boolean isConstructor() {
      return true;
    }

    @Override
    protected Annotation[][] getParameterAnnotations() {
      return getMember().getParameterAnnotations();
    }

    @Override
    public List<Type> getRawParameterTypes() {
      return Arrays.asList(getMember().getGenericParameterTypes());
    }
  }

  /**
   * Method-specific implementation of {@link MethodLiteral}.
   */
  private static class ConcreteMethodLiteral<T> extends MethodLiteral<T, Method> {

    ConcreteMethodLiteral(Method method, TypeLiteral<T> typeLiteral) {
      super(method, typeLiteral);
    }

    @Override
    public TypeLiteral<?> getReturnType() {
      return getDeclaringType().getReturnType(getMember());
    }

    @Override
    public boolean isConstructor() {
      return false;
    }

    @Override
    protected Annotation[][] getParameterAnnotations() {
      return getMember().getParameterAnnotations();
    }

    @Override
    public List<Type> getRawParameterTypes() {
      return Arrays.asList(getMember().getGenericParameterTypes());
    }
  }
}
