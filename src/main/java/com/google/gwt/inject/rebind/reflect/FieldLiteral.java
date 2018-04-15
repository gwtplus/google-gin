/*
 * Copyright 2013 Google Inc.
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

import com.google.gwt.inject.rebind.util.Preconditions;
import com.google.inject.TypeLiteral;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Generic field representation preserving the fields type parametrization.
 * 
 * @param <T> type
 * 
 * @see TypeLiteral
 */
public class FieldLiteral<T> extends MemberLiteral<T, Field> {

  /**
   * Returns a new field literal based on the passed field and its declaring
   * type.
   *
   * @param field field for which the literal is created
   * @param declaringType the field's declaring type
   * @return new field literal
   */
  public static <T> FieldLiteral<T> get(Field field, TypeLiteral<T> declaringType) {
    Preconditions.checkArgument(
        field.getDeclaringClass().equals(declaringType.getRawType()),
        "declaringType (%s) must be the type literal where field was declared (%s)!",
        declaringType, field.getDeclaringClass());
    return new FieldLiteral<T>(field, declaringType);
  }

  private FieldLiteral(Field field, TypeLiteral<T> typeLiteral) {
    super(field, typeLiteral);
  }

  /**
   * Returns the field's type, if appropriate parametrized with the declaring
   * class's type parameters.
   *
   * @return field type
   */
  public TypeLiteral<?> getFieldType() {
    return getDeclaringType().getFieldType(getMember());
  }

  /**
   * Returns {@code true} if this is a final field that past versions of Gin
   * allowed to be set by member injection.
   *
   * <p>Past versions of Gin used native Javascript to set the values of
   * inaccessible fields.  If those fields also happened to be final, the native
   * code would ignore the final modifier and assign the field anyway.
   *
   * <p>As I (dburrows) don't feel that this is something to encourage, I'm
   * narrowly allowing this usage only in cases where to do otherwise would
   * break previously compiling code.  It might be prudent to remove this
   * exclusion in the future, but not as part of my current work.
   */
  public boolean isLegacyFinalField() {
    return !isPublic() && Modifier.isFinal(getModifiers());
  }

  /**
   * Returns the field's declaring type and name in the format used in javadoc,
   * e.g. {@code com.bar.Foo#baz}, with resolved type parameters.
   *
   * @return string representation for this method including the declaring type
   */
  @Override
  public String toString() {
    return String.format("%s#%s", getDeclaringType(), getName());
  }
}
