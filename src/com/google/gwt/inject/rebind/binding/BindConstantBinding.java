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
package com.google.gwt.inject.rebind.binding;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.dev.util.Preconditions;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Key;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;

/**
 * Binding for a constant value.
 */
public class BindConstantBinding<T> extends AbstractBinding implements Binding {

  private final SourceWriteUtil sourceWriteUtil;
  private final String valueToOutput;
  private final Key<?> key;

  /**
   * Returns true if the provided key is a valid constant key, i.e. if a
   * constant binding can be legally created for it.
   *
   * @param key key to check
   * @return true if constant key
   */
  public static boolean isConstantKey(Key<?> key) {
    Type type = key.getTypeLiteral().getType();

    if (!(type instanceof Class)) {
      return false;
    }

    Class clazz = (Class) type;
    return clazz == String.class || clazz == Class.class || clazz.isPrimitive()
        || Number.class.isAssignableFrom(clazz) || Character.class.isAssignableFrom(clazz)
        || Boolean.class.isAssignableFrom(clazz) || clazz.isEnum();
  }

  BindConstantBinding(SourceWriteUtil sourceWriteUtil, Key<T> key, T instance, Context context) {
    super(context);

    this.sourceWriteUtil = sourceWriteUtil;
    this.key = Preconditions.checkNotNull(key);
    this.valueToOutput = getValueToOutput(key, Preconditions.checkNotNull(instance));
  }

  private static <T> String getValueToOutput(Key<T> key, T instance) {
    Type type = key.getTypeLiteral().getType();

    if (type == String.class) {
      return "\"" + Generator.escape(instance.toString()) + "\"";
    } else if (type == Class.class) {
      return toClassReference((Class<?>) instance);
    } else if (type == Character.class) {
      return "'" + (Character.valueOf('\'').equals(instance) ? "\\" : "") + instance + "'";
    } else if (type == Float.class) {
      return instance.toString() + "f";
    } else if (type == Long.class) {
      return instance.toString() + "L";
    } else if (type == Double.class) {
      return instance.toString() + "d";
    } else if (instance instanceof Number || instance instanceof Boolean) {
      return instance.toString(); // Includes int & short.
    } else if (instance instanceof Enum) {
      return toEnumReference(((Enum<?>) instance));
    } else {
      throw new IllegalArgumentException("Attempted to create a constant binding with a "
          + "non-constant type: " + type);
    }
  }

  public void writeCreatorMethods(SourceWriter writer, String creatorMethodSignature,
      NameGenerator nameGenerator) {
    sourceWriteUtil.writeMethod(writer, creatorMethodSignature, "return " + valueToOutput + ";");
  }

  public Collection<Dependency> getDependencies() {
    return Collections.singletonList(
        new Dependency(Dependency.GINJECTOR, key, getContext().toString()));
  }

  private static String toEnumReference(Enum<?> instance) {
    return instance.getDeclaringClass().getCanonicalName() + "." + instance.name();
  }

  private static String toClassReference(Class<?> instance) {
    String canonicalName = instance.getCanonicalName();
    if (canonicalName == null) {
      throw new IllegalArgumentException("Attempted to create a constant binding with a "
          + "a local or anonymous class: " + instance);
    }
    return canonicalName + ".class";
  }
}
