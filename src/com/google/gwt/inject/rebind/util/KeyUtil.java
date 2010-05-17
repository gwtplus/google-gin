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

import com.google.gwt.core.ext.typeinfo.HasAnnotations;
import com.google.gwt.core.ext.typeinfo.JAbstractMethod;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JWildcardType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.inject.rebind.binding.Injectable;
import com.google.gwt.inject.rebind.binding.RequiredKeys;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;
import com.google.inject.util.Types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Named;

/**
 * Util object that offers {@link Key} retrieval and manipulation methods.
 */
@Singleton
public class KeyUtil {
  private final TypeOracle typeOracle;
  private final MemberCollector memberCollector;

  @Inject
  public KeyUtil(TypeOracle typeOracle, @Injectable MemberCollector memberCollector) {
    this.typeOracle = typeOracle;
    this.memberCollector = memberCollector;
  }

  public Key<?> getKey(JMethod method) {
    if (isMemberInject(method)) {
      return getKey(method.getParameters()[0]);
    }

    return getKey(method.getReturnType(), getAnnotations(JAbstractMethod.class, method));
  }

  public Key<?> getKey(JParameter param) {
    return getKey(param.getType(), getAnnotations(JParameter.class, param));
  }

  public Key<?> getKey(JField field) {
    return getKey(field.getType(), getAnnotations(JField.class, field));
  }

  public boolean isMemberInject(JMethod method) {
    return method.getReturnType() == JPrimitiveType.VOID;
  }

  public Class<?> getRawType(Key<?> key) {
    Type type = key.getTypeLiteral().getType();
    if (type instanceof Class) {
      return (Class) type;
    } else if (type instanceof ParameterizedType) {
      return (Class) ((ParameterizedType) type).getRawType();
    }

    throw new ProvisionException("Can't get raw type for " + key);
  }

  public JClassType getRawClassType(Key<?> key) {
    return getClassType(getRawType(key));
  }

  public JClassType getClassType(Key<?> key) {
    return getClassType(key.getTypeLiteral().getType());
  }

  public JClassType getClassType(Type type) {
    if (type instanceof Class) {
      return typeOracle.findType(((Class) type).getCanonicalName());
    } else if (type instanceof ParameterizedType) {
      ParameterizedType parametrized = (ParameterizedType) type;

      JClassType[] parameters = new JClassType[parametrized.getActualTypeArguments().length];
      int i = 0;
      for (Type paramType : parametrized.getActualTypeArguments()) {
        parameters[i++] = getClassType(paramType);
      }

      Class rawClass = (Class) parametrized.getRawType();
      JClassType classType =
          typeOracle.findType(rawClass.getCanonicalName());
      JGenericType genericType = classType.isGenericType();
      if (genericType == null) {
        throw new ProvisionException("Can't get class type for " + type);
      }

      return typeOracle.getParameterizedType(genericType, genericType.getEnclosingType(),
          parameters);
    }

    throw new ProvisionException("Can't get class type for " + type);
  }

  /**
   * Gets the Guice binding key for a given GWT type with optional annotations.
   *
   * @param gwtType GWT type to convert in to a key
   * @param annotations Optional array of {@code Annotation}s. If this contains
   *     one and only one {@link BindingAnnotation}, it will be included in the
   *     key. If it includes more than one, an exception will be thrown.
   * @return Guice Key instance for this type/annotations
   * @throws ProvisionException in case of any failure
   */
  public Key<?> getKey(JType gwtType, Annotation... annotations) throws ProvisionException {
    try {
      Type type = gwtTypeToJavaType(gwtType);
      return getKey(type, annotations);
    } catch (ClassNotFoundException e) {
      throw new ProvisionException("Error creating key for " + gwtType, e);
    } catch (NoSuchFieldException e) {
      throw new ProvisionException("Error creating key for " + gwtType, e);
    } catch (IllegalAccessException e) {
      throw new ProvisionException("Error creating key for " + gwtType, e);
    }
  }

  /**
   * Gets the Guice binding key for a given Java type with optional annotations.
   *
   * @param type Java type to convert in to a key
   * @param annotations Optional array of {@code Annotation}s. If this contains
   *     one and only one {@link BindingAnnotation}, it will be included in the
   *     key. If it includes more than one, an exception will be thrown.
   * @return Guice Key instance for this type/annotations
   * @throws ProvisionException in case of any failure
   */
  public Key<?> getKey(Type type, Annotation... annotations) throws ProvisionException {
    Annotation bindingAnnotation = getBindingAnnotation(annotations);

    if (bindingAnnotation == null) {
      return Key.get(type);
    } else {
      return Key.get(type, bindingAnnotation);
    }
  }

  /**
   * Returns a {@link JMethod} that represents the same method as the provided
   * {@link Method} reflection object.
   *
   * @param javaMethod method as used by reflection
   * @return method as used by the GWT compiler
   * @throws NotFoundException if method cannot be found in source
   */
  public JMethod javaToGwtMethod(Method javaMethod) throws NotFoundException {
    JClassType gwtEnclosingType =
        typeOracle.findType(javaMethod.getDeclaringClass().getCanonicalName());

    JMethod resultingMethod = null;
    for (JMethod gwtMethod : gwtEnclosingType.getMethods()) {
      if (gwtMethod.getName().equals(javaMethod.getName())) {
        JParameter[] gwtParameters = gwtMethod.getParameters();
        Class<?>[] javaParameters = javaMethod.getParameterTypes();

        if (gwtParameters.length != javaParameters.length) {
          continue;
        }

        boolean found = true;
        for (int i = 0; i < gwtParameters.length; i++) {
          found = found && gwtParameters[i].getType().getQualifiedSourceName().equals(
              javaParameters[i].getCanonicalName());
        }
        if (found) {
          resultingMethod = gwtMethod;
          break;
        }
      }
    }

    if (resultingMethod == null) {
      throw new NotFoundException("Couldn't locate requested method in source: " + javaMethod);
    }

    return resultingMethod;
  }

  /**
   * Returns a {@link JConstructor} that represents the same constructor as the
   * provided {@link Constructor} reflection object.
   *
   * Note: This is almost the same method as {@link #javaToGwtMethod(Method)}
   * but cannot be merged since constructors and methods do not derive from a
   * common interface in reflection.
   *
   * @param javaConstructor method as used by reflection
   * @return constructor as used by the GWT compiler
   * @throws NotFoundException if constructor cannot be found in source
   */
  public JConstructor javaToGwtConstructor(Constructor javaConstructor) throws NotFoundException {
    JClassType gwtEnclosingType =
        typeOracle.findType(javaConstructor.getDeclaringClass().getCanonicalName());

    JConstructor resultingConstructor = null;
    for (JConstructor gwtConstructor : gwtEnclosingType.getConstructors()) {
      JParameter[] gwtParameters = gwtConstructor.getParameters();
      Class<?>[] javaParameters = javaConstructor.getParameterTypes();

      if (gwtParameters.length != javaParameters.length) {
        continue;
      }

      boolean found = true;
      for (int i = 0; i < gwtParameters.length; i++) {
        found = found && gwtParameters[i].getType().getQualifiedSourceName().equals(
            javaParameters[i].getCanonicalName());
      }
      if (found) {
        resultingConstructor = gwtConstructor;
        break;
      }
    }

    if (resultingConstructor == null) {
      throw new NotFoundException("Couldn't locate requested constructor in source: "
          + javaConstructor);
    }

    return resultingConstructor;
  }

  /**
   * Returns a {@link JField} that represents the same method as the provided
   * {@link Field} reflection object.
   *
   * @param javaField field as used by reflection
   * @return field as used by the GWT compiler
   */
  public JField javaToGwtField(Field javaField) {
    JClassType gwtEnclosingType =
        typeOracle.findType(javaField.getDeclaringClass().getCanonicalName());

    return gwtEnclosingType.getField(javaField.getName());
  }

  /**
   * Returns true if the passed class member has an {@literal @}{@code Inject}
   * annotation and the injection is marked as optional (
   * {@literal @}{@code Inject(optional=true)}).
   *
   * Note that {@link javax.inject.Inject} does not have an optional parameter
   * and therefore cannot be optional.
   *
   * @param member member to be checked
   * @return true if member is injected optionally
   */
  public boolean isOptional(HasAnnotations member) {
    Inject annot = member.getAnnotation(Inject.class);
    return annot != null && annot.optional();
  }

  /**
   * Collects and returns all keys required to inject the given class.
   *
   * @param classType class for which required keys are calculated
   * @return keys required to inject given class
   */
  public RequiredKeys getRequiredKeys(JClassType classType) {
    Set<Key<?>> required = new HashSet<Key<?>>();
    Set<Key<?>> optional = new HashSet<Key<?>>();
    for (JMethod method : memberCollector.getMethods(classType)) {
      RequiredKeys requiredKeys = getRequiredKeys(method);
      required.addAll(requiredKeys.getRequiredKeys());
      optional.addAll(requiredKeys.getOptionalKeys());
    }

    for (JField field : memberCollector.getFields(classType)) {
      Key<?> key = getKey(field);
      if (isOptional(field)) {
        optional.add(key);
      } else {
        required.add(key);
      }
    }
    return new RequiredKeys(required, optional);
  }

  /**
   * Collects and returns all keys required to inject the given method.
   *
   * @param method method for which required keys are calculated
   * @return required keys
   */
  public RequiredKeys getRequiredKeys(JAbstractMethod method) {
    Set<Key<?>> required = new HashSet<Key<?>>();
    Set<Key<?>> optional = new HashSet<Key<?>>();
    for (JParameter param : method.getParameters()) {
      Key<?> key = getKey(param);
      if (isOptional(method)) {
        optional.add(key);
      } else {
        required.add(key);
      }
    }
    return new RequiredKeys(required, optional);
  }

  private Annotation getBindingAnnotation(Annotation[] annotations) {
    if (annotations == null || annotations.length == 0) {
      return null;
    }

    Annotation bindingAnnotation = null;
    for (Annotation annotation : annotations) {
      if (annotation.annotationType().getAnnotation(BindingAnnotation.class) != null
          || annotation.annotationType() == Named.class) {
        if (bindingAnnotation != null) {
          throw new ProvisionException(">1 binding annotation found: "
              + annotation + ", " + bindingAnnotation);
        }

        bindingAnnotation = annotation;

        // Keep going so we can find any rogue additional binding annotations
      }
    }

    return bindingAnnotation;
  }

  private Type gwtTypeToJavaType(JType gwtType)
      throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
    JPrimitiveType primitiveType = gwtType.isPrimitive();
    if (primitiveType != null) {
      return loadClass(primitiveType);
    }

    JArrayType arrayType = gwtType.isArray();
    if (gwtType.isArray() != null) {
      Type componentType = gwtTypeToJavaType(arrayType.getComponentType());
      return Types.arrayOf(componentType);
    }

    JWildcardType wildcardType = gwtType.isWildcard();
    if (wildcardType != null) {
      Type baseType = gwtTypeToJavaType(wildcardType.getBaseType());

      switch (wildcardType.getBoundType()) {
        case EXTENDS:
          return Types.subtypeOf(baseType);
        case SUPER:
          return Types.supertypeOf(baseType);
        case UNBOUND:

      }
    }

    JParameterizedType parameterizedType = gwtType.isParameterized();
    if (gwtType.isParameterized() != null) {
      JClassType[] typeArgs = parameterizedType.getTypeArgs();
      List<Type> javaTypeArgs = new ArrayList<Type>();

      for (JClassType typeArg : typeArgs) {
        JWildcardType wildcard = typeArg.isWildcard();

        if (wildcard == null || wildcard.getBoundType() != JWildcardType.BoundType.UNBOUND) {
          javaTypeArgs.add(gwtTypeToJavaType(typeArg));
        }
      }

      Type rawType = gwtTypeToJavaType(parameterizedType.getRawType());

      if (parameterizedType.getEnclosingType() != null) {
        return Types.newParameterizedTypeWithOwner(
            gwtTypeToJavaType(parameterizedType.getEnclosingType()), rawType,
            javaTypeArgs.toArray(new Type[javaTypeArgs.size()]));
      } else {
        return Types.newParameterizedType(rawType,
            javaTypeArgs.toArray(new Type[javaTypeArgs.size()]));
      }
    }

    JClassType jClassType = gwtType.isClassOrInterface();
    if (gwtType.isClassOrInterface() != null) {
      return loadClass(jClassType);
    }

    throw new ProvisionException("Unknown GWT type: " + gwtType);
  }

  // Wrapper around Class.forName that passes initialize=false. This is critical
  // because GWT client code (whose class names we may be referencing here)
  // can not necessarily have its static initializers run at rebind time.
  private static Class<?> loadClass(JType type) throws ClassNotFoundException,
      NoSuchFieldException, IllegalAccessException {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    // Handle primitive types.
    JPrimitiveType primitiveType = type.isPrimitive();
    if (primitiveType != null) {
      String boxClassName = primitiveType.getQualifiedBoxedSourceName();
      Class<?> boxClass = Class.forName(boxClassName, false, classLoader);
      return (Class) boxClass.getField("TYPE").get(null);
    }

    JClassType classType = type.isClassOrInterface();
    if (classType == null) {
      throw new UnsupportedOperationException("Cannot load " + type + ".");
    }

    return Class.forName(getBinaryName((JClassType) type), false, classLoader);
  }

  private static String getBinaryName(JClassType type) {
    JClassType enclosingType = type.getEnclosingType();
    if (enclosingType != null) {
     return getBinaryName(enclosingType) + "$" + type.getSimpleSourceName();
    }
    return type.getQualifiedSourceName();
  }

  // Reflective hack until getAnnotations is exposed from GWT
  private static <T> Annotation[] getAnnotations(Class<T> clazz, T instance) {
    try {
      Method method = clazz.getDeclaredMethod("getAnnotations");
      method.setAccessible(true);
      return (Annotation[]) method.invoke(instance);
    } catch (NoSuchMethodException e) {
      throw new ProvisionException("Failed to get annotations from " + instance, e);
    } catch (IllegalAccessException e) {
      throw new ProvisionException("Failed to get annotations from " + instance, e);
    } catch (InvocationTargetException e) {
      throw new ProvisionException("Failed to get annotations from " + instance, e);
    }
  }
}
