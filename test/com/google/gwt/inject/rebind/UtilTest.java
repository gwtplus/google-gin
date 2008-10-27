// Copyright 2008 Google Inc. All Rights Reserved.

package com.google.gwt.inject.rebind;

import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.inject.client.MyBindingAnnotation;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import junit.framework.TestCase;

import static org.easymock.classextension.EasyMock.createControl;
import static org.easymock.classextension.EasyMock.expect;
import org.easymock.classextension.IMocksControl;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Unit tests for {@link JType} to {@link Key} translation. Since it is not easy
 * to create {@link JType} instances, we use EasyMock (classextension) to mock
 * them out.
 */
// Annotate our class just to make it easy to get annotation instances :)
@MyBindingAnnotation
@MyOtherAnnotation
public class UtilTest extends TestCase {

  private IMocksControl control;
  private KeyUtil keyUtil;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    keyUtil = new KeyUtil();
    control = createControl();
  }

  @Override
  protected void runTest() throws Throwable {
    super.runTest();
    control.verify();
  }

  public void testString() throws Exception {
    JClassType type = createClassType(String.class);
    control.replay();

    Key<?> key = keyUtil.getKey(type, null);
    assertEquals(key, Key.get(String.class));
  }

  public void testInteger() throws Exception {
    JClassType type = createClassType(Integer.class);
    control.replay();

    Key<?> key = keyUtil.getKey(type, null);
    assertEquals(key, Key.get(Integer.class));
  }

  public void testInt() throws Exception {
    JPrimitiveType type = createPrimitiveType(Integer.class);
    control.replay();

    Key<?> key = keyUtil.getKey(type, null);
    assertEquals(key, Key.get(int.class));
  }

  public void testListOfInteger() throws Exception {
    JClassType listType = createClassType(List.class);
    JClassType integerType = createClassType(Integer.class);
    JParameterizedType paramType = createParameterizedType(listType, integerType);
    control.replay();

    Key<?> key = keyUtil.getKey(paramType, null);
    assertEquals(key, Key.get(new TypeLiteral<List<Integer>>() {}));
  }

  public void testArrayOfInteger() throws Exception {
    JPrimitiveType intType = createPrimitiveType(Integer.class);
    JArrayType arrayType = createArrayType(intType);
    control.replay();

    Key<?> key = keyUtil.getKey(arrayType, null);
    assertEquals(key, Key.get(int[].class));
  }

  public void testArrayOfInt() throws Exception {
    JClassType integerType = createClassType(Integer.class);
    JArrayType arrayType = createArrayType(integerType);
    control.replay();

    Key<?> key = keyUtil.getKey(arrayType, null);
    assertEquals(key, Key.get(Integer[].class));
  }

  public void testStringNamed() throws Exception {
    JClassType type = createClassType(String.class);
    control.replay();

    Annotation ann = Names.named("brian");
    Key<?> key = keyUtil.getKey(type, new Annotation[] { ann });
    assertEquals(key, Key.get(String.class, ann));
  }

  public void testStringMyBindingAnnotation() throws Exception {
    JClassType type = createClassType(String.class);
    control.replay();

    Annotation ann = getClass().getAnnotation(MyBindingAnnotation.class);
    assertNotNull(ann);

    Key<?> key = keyUtil.getKey(type, new Annotation[]{ann});
    assertEquals(key, Key.get(String.class, ann));
    assertEquals(key, Key.get(String.class, MyBindingAnnotation.class));
  }

  public void testStringNonBindingAnnotation() throws Exception {
    JClassType type = createClassType(String.class);
    control.replay();

    Annotation ann = getClass().getAnnotation(MyOtherAnnotation.class);
    assertNotNull(ann);

    Key<?> key = keyUtil.getKey(type, new Annotation[]{ann});
    assertEquals(key, Key.get(String.class));
  }

  public void testStringTwoAnnotations() throws Exception {
    JClassType type = createClassType(String.class);
    control.replay();

    Annotation bindingAnn = getClass().getAnnotation(MyBindingAnnotation.class);
    Annotation otherAnn = getClass().getAnnotation(MyOtherAnnotation.class);
    assertNotNull(bindingAnn);
    assertNotNull(otherAnn);

    Key<?> key = keyUtil.getKey(type, new Annotation[]{bindingAnn, otherAnn});
    assertEquals(key, Key.get(String.class, bindingAnn));
    assertEquals(key, Key.get(String.class, MyBindingAnnotation.class));

    // Test annotations in the other order too
    key = keyUtil.getKey(type, new Annotation[]{otherAnn, bindingAnn});
    assertEquals(key, Key.get(String.class, bindingAnn));
    assertEquals(key, Key.get(String.class, MyBindingAnnotation.class));
  }

  public void testTooManyBindingAnnotations() throws Exception {
    JClassType type = createClassType(String.class);
    control.replay();

    Annotation bindingAnn = getClass().getAnnotation(MyBindingAnnotation.class);
    Annotation namedAnn = Names.named("brian");

    try {
      Key<?> key = keyUtil.getKey(type, new Annotation[]{bindingAnn, namedAnn});
      fail("Expected exception, but got: " + key);
    } catch (ProvisionException e) {
      // good, expected
    }
  }

  // TODO(schmitt):  Add tests for method collector.

  private JArrayType createArrayType(JType componentType) {
    JArrayType type = control.createMock(JArrayType.class);
    expect(type.isPrimitive()).andReturn(null).anyTimes();
    expect(type.isArray()).andReturn(type).anyTimes();
    expect(type.isParameterized()).andReturn(null).anyTimes();
    expect(type.isLocalType()).andReturn(null).anyTimes();
    expect(type.isMemberType()).andReturn(null).anyTimes();
    expect(type.isClassOrInterface()).andReturn(type).anyTimes();
    expect(type.getComponentType()).andReturn(componentType).anyTimes();
    return type;
  }

  private JClassType createClassType(Class<?> clazz) {
    JClassType type = control.createMock(JClassType.class);
    expect(type.isPrimitive()).andReturn(null).anyTimes();
    expect(type.isArray()).andReturn(null).anyTimes();
    expect(type.isParameterized()).andReturn(null).anyTimes();
    expect(type.isLocalType()).andReturn(null).anyTimes();
    expect(type.isMemberType()).andReturn(null).anyTimes();
    expect(type.isClassOrInterface()).andReturn(type).anyTimes();
    expect(type.getQualifiedSourceName()).andReturn(clazz.getName()).anyTimes();
    return type;
  }

  private JParameterizedType createParameterizedType(JClassType rawType, JClassType... typeArgs) {
    JParameterizedType type = control.createMock(JParameterizedType.class);

    // Don't mock isArray or isPrimitive since they are final
    expect(type.isParameterized()).andReturn(type).anyTimes();
    expect(type.isClassOrInterface()).andReturn(type).anyTimes();
    expect(type.getRawType()).andReturn(rawType).anyTimes();
    expect(type.getTypeArgs()).andReturn(typeArgs).anyTimes();
    expect(type.isLocalType()).andReturn(null).anyTimes();
    expect(type.isMemberType()).andReturn(null).anyTimes();
    return type;
  }

  private JPrimitiveType createPrimitiveType(Class<?> boxClass) {
    JPrimitiveType type = control.createMock(JPrimitiveType.class);
    expect(type.isPrimitive()).andReturn(type).anyTimes();
    expect(type.isArray()).andReturn(null).anyTimes();
    expect(type.isParameterized()).andReturn(null).anyTimes();
    expect(type.isClassOrInterface()).andReturn(null).anyTimes();
    expect(type.getQualifiedBoxedSourceName()).andReturn(boxClass.getName()).anyTimes();
    return type;
  }
}
