package com.google.gwt.inject.rebind.reflect;

import com.google.inject.TypeLiteral;

import java.lang.reflect.Field;

/**
 * Generic field representation preserving the fields type parametrization.
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
    assert field.getDeclaringClass().equals(declaringType.getRawType());
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
