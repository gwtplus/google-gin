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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;

import static com.google.gwt.inject.rebind.util.SourceWriteUtil.join;

/**
 * Utility providing helper methods around reflection.
 */
public class ReflectUtil {

  /**
   * Alternate toString method for TypeLiterals that fixes a JDK bug that was
   * replicated in Guice. See
   * <a href="http://code.google.com/p/google-guice/issues/detail?id=293">
   * the related Guice bug</a> for details.
   *
   * Also replaces all binary with source names in the types involved (base
   * type and type parameters).
   *
   * @param typeLiteral type for which string will be returned
   * @return String representation of type
   * @throws NoSourceNameException if source name is not available for type
   */
  public static String getSourceName(TypeLiteral<?> typeLiteral) throws NoSourceNameException {
    return getSourceName(typeLiteral.getType());
  }

  /**
   * Returns a string representation of the passed type's name while ensuring
   * that all type names (base and parameters) are converted to source type
   * names.
   *
   * @param type type for which string will be returned
   * @return String representation of type
   * @throws NoSourceNameException if source name is not available for type
   */
  public static String getSourceName(Type type) throws NoSourceNameException {
    if (type instanceof Class<?>) {
      Class<?> clazz = (Class<?>) type;
      if (clazz.isPrimitive()) {
        return clazz.getName(); // Returns "int" for integer etc.
      }

      String name = clazz.getCanonicalName();

      // We get a null for anonymous inner classes or other types that don't
      // have source names.
      if (name == null) {
        throw new NoSourceNameException(type);
      }

      return name;
    }

    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      Type[] arguments = parameterizedType.getActualTypeArguments();
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append(getSourceName(parameterizedType.getRawType()));

      if (arguments.length == 0) {
        return stringBuilder.toString();
      }

      stringBuilder.append("<").append(getSourceName(arguments[0]));
      for (int i = 1; i < arguments.length; i++) {
        stringBuilder.append(", ").append(getSourceName(arguments[i]));
      }
      return stringBuilder.append(">").toString();
    }

    if (type instanceof GenericArrayType) {
      return getSourceName(((GenericArrayType) type).getGenericComponentType()) + "[]";
    }

    if (type instanceof WildcardType) {
      Type[] lowerBounds = ((WildcardType) type).getLowerBounds();
      Type[] upperBounds = ((WildcardType) type).getUpperBounds();

      if (lowerBounds.length > 0 && upperBounds.length > 0
          && lowerBounds.length !=  1 && lowerBounds[0] != Object.class) {
        throw new NoSourceNameException(type);
      }

      if (lowerBounds.length > 0) {
        return getBoundedSourceName("?", lowerBounds, "super");
      } else if (upperBounds.length == 1 && upperBounds[0] == Object.class) {
        return "?";
      } else {
        return getBoundedSourceName("?", upperBounds, "extends");
      }
    }

    // This returns the name of a reference to a type variable, not the
    // definition thereof. For the latter see #getTypeVariableDefinition(..).
    if (type instanceof TypeVariable) {
      return ((TypeVariable) type).getName();
    }

    throw new NoSourceNameException(type);
  }

  /**
   * Creates a bounded source name of the form {@code T extends Foo & Bar},
   * {@code ? super Baz} or {@code ?} as appropriate.
   */
  private static String getBoundedSourceName(String token, Type[] bounds, String direction)
      throws NoSourceNameException {
    List<String> boundNames = new ArrayList<String>();
    for (Type boundary : bounds) {
      boundNames.add(getSourceName(boundary));
    }
    return String.format("%s %s %s", token, direction, join(" & ", boundNames));
  }

  /**
   * Returns a type variable's definition, e.g. {@code M extends Foo}.
   */
  // Visible for testing
  static String getTypeVariableDefinition(TypeVariable variable)
      throws NoSourceNameException {
    Type[] bounds = variable.getBounds();
    if (bounds.length == 0 || (bounds.length == 1 && bounds[0].equals(Object.class))) {
      return variable.getName();
    }
    return getBoundedSourceName(variable.getName(), bounds, "extends");
  }

  /**
   * Returns {@code true} if the passed type's visibility is {@code public}.
   */
  public static boolean isPublic(TypeLiteral<?> type) {
    return Modifier.isPublic(type.getRawType().getModifiers());
  }

  /**
   * Returns {@code true} if the passed type's visibility is {@code private}.
   */
  public static boolean isPrivate(Class<?> type) {
    return Modifier.isPrivate(type.getModifiers());
  }

  /**
   * Returns {@code true} if the passed type's visibility is {@code private}.
   */
  public static boolean isPrivate(TypeLiteral<?> type) {
    return isPrivate(type.getRawType());
  }

  /**
   * Returns {@code true} if the passed member's visibility is {@code private}.
   */
  public static boolean isPrivate(Member member) {
    return Modifier.isPrivate(member.getModifiers());
  }

  /**
   * Returns the passed method's signature with all types in source form.
   *
   * <p>If the method has parameters, makes up generic names for these in the
   * signature.
   *
   * @param method method for which signature is created
   * @return method's signature
   * @see #getSourceName(Type)
   * @throws NoSourceNameException if any type's source name cannot be
   *    determined
   */
  public static String getSignature(MethodLiteral<?, ?> method) throws NoSourceNameException {
    return getSignature(method, getDefaultParameterNames(method.getParameterTypes().size()));
  }

  /**
   * Returns the passed method's signature with all types in source form.
   *
   * @param method method for which signature is created
   * @param paramNames names to be used for the method's parameters. Length
   *    must be the same as method's parameter count
   * @return method's signature
   * @see #getSourceName(Type)
   * @throws NoSourceNameException if any type's source name cannot be
   *    determined
   */
  public static String getSignature(MethodLiteral<?, ?> method, String[] paramNames)
      throws NoSourceNameException {
    return getSignature(method, paramNames, method.getModifiers());
  }

  /**
   * Returns the passed method's signature with all types in source form.
   *
   * <p>If the method has parameters, makes up generic names for these in the
   * signature.
   *
   * @param method method for which signature is created
   * @param overrideModifiers modifiers used instead of the method's original
   *    modifiers, see also {@link Modifier}
   * @return method's signature
   * @see #getSourceName(Type)
   * @throws NoSourceNameException if any type's source name cannot be
   *    determined
   */
  public static String getSignature(MethodLiteral<?, ?> method, int overrideModifiers)
      throws NoSourceNameException {
    return getSignature(method, getDefaultParameterNames(method.getParameterTypes().size()),
        overrideModifiers);
  }

  /**
   * Returns the passed method's signature with all types in source form.
   *
   * @param method method for which signature is created
   * @param paramNames names to be used for the method's parameters. Length
   *    must be the same as method's parameter count
   * @param overrideModifiers modifiers used instead of the method's original
   *    modifiers, see also {@link Modifier}
   * @return method's signature
   * @see #getSourceName(Type)
   * @throws NoSourceNameException if any type's source name cannot be
   *    determined
   */
  // TODO(schmitt): Print method and parameter annotations.
  public static String getSignature(MethodLiteral<?, ?> method, String[] paramNames,
      int overrideModifiers) throws NoSourceNameException {
    if (paramNames.length != method.getParameterTypes().size()) {
      throw new IllegalArgumentException(
          String.format("Wrong number of parameters provided for method signature, "
              + "expected %d but got %d.", method.getParameterTypes().size(), paramNames.length));
    }

    StringBuilder sb = new StringBuilder();

    if (overrideModifiers != 0) {
      sb.append(Modifier.toString(overrideModifiers)).append(" ");
    }

    if (method.getTypeParameters().length > 0) {
      List<String> typeParameters = new ArrayList<String>();
      for (TypeVariable<?> typeVariable : method.getTypeParameters()) {
        typeParameters.add(getTypeVariableDefinition(typeVariable));
      }
      sb.append("<").append(join(", ", typeParameters)).append("> ");
    }

    if (!method.isConstructor()) {
      sb.append(getSourceName(method.getReturnType())).append(" ").append(method.getName());
    } else {
      sb.append(getSourceName(method.getRawDeclaringType()));
    }
    sb.append("(");

    // TODO(schmitt): We are not respecting varargs here.
    List<String> parameters = new ArrayList<String>();
    int i = 0;
    for (TypeLiteral<?> parameterType : method.getParameterTypes()) {
      parameters.add(
          String.format("%s %s", getSourceName(parameterType), paramNames[i]));
      i++;
    }
    sb.append(join(", ", parameters)).append(")");

    if (method.getExceptionTypes().size() > 0) {
      List<String> exceptions = new ArrayList<String>();
      for (TypeLiteral<?> exceptionType : method.getExceptionTypes()) {
        exceptions.add(getSourceName(exceptionType));
      }
      sb.append(" throws ").append(join(", ", exceptions));
    }

    return sb.toString();
  }

  private static String[] getDefaultParameterNames(int count) {
    String[] paramNames = new String[count];
    for (int i = 0; i < paramNames.length; i++) {
      paramNames[i] = formatParameterName(i);
    }
    return paramNames;
  }

  /**
   * Returns a string representing a parameter name for a method signature.
   *
   * <p>Use this method to keep parameter names the same throughout Gin code.
   * Creating synthetic parameter names is necessary since java reflection does
   * not expose source parameter names.
   *
   * @param position position of the parameter in the signature
   * @return parameter name
   */
  public static String formatParameterName(int position) {
    return "_" + position;
  }

  /**
   * If present, strips the "abstract" modifier from the passed method's
   * modifiers.
   *
   * <p>Useful since interface methods are abstract but we're often writing an
   * implementation for them.
   */
  public static int nonAbstractModifiers(MethodLiteral<?, Method> method) {
    return method.getModifiers() & ~Modifier.ABSTRACT;
  }

  /**
   * Returns {@code true} if the passed type is either a class or an interface
   * (but not a primitive, enum or similar).
   *
   * @param type class to be checked
   * @return {@code true} if the passed type is a class or interface
   */
  public static boolean isClassOrInterface(Type type) {
    if (!(type instanceof Class)) {
      return false;
    }

    Class clazz = (Class) type;
    return !clazz.isPrimitive() && !clazz.isAnnotation() && !clazz.isArray() && !clazz.isEnum()
        && !clazz.isAnonymousClass();
  }
  
  /**
   * Given a parameterized type (such as a {@code Provider<Foo>}) return the 
   * parameter ({@code Foo}).
   */
  public static Key<?> getProvidedKey(Key<?> key) {
    Type providerType = key.getTypeLiteral().getType();

    // If the Provider has no type parameter (raw Provider)...
    Preconditions.checkArgument(providerType instanceof ParameterizedType,
        "Expected all providers to be parameterized, but %s wasn't", key);

    Type entryType = ((ParameterizedType) providerType).getActualTypeArguments()[0];

    return key.ofType(entryType);
  }
}
