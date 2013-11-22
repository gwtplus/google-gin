/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.inject.rebind.output;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.output.subpackage.SubPackageClass;
import com.google.gwt.inject.rebind.reflect.FieldLiteral;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.util.InjectorMethod;
import com.google.gwt.inject.rebind.util.MethodCallUtil;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceSnippets;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import junit.framework.TestCase;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class GinjectorBindingsOutputterTest extends TestCase {

  private IMocksControl control;
  private IMocksControl niceControl;

  public void setUp() throws Exception {
    super.setUp();

    control = EasyMock.createControl();
    niceControl = EasyMock.createNiceControl();
  }

  private void replay() {
    control.replay();
    niceControl.replay();
  }

  private void verify() {
    control.verify();
    niceControl.verify();
  }

  private <T> T createMock(Class<T> clazz, String name) {
    return control.createMock(name, clazz);
  }

  private <T> T createNiceMock(Class<T> clazz, String name) {
    return niceControl.createMock(name, clazz);
  }

  // Verify that outputting static injections creates and dispatches to the
  // correct fragment classes.
  public void testOutputStaticInjections() throws Exception {
    PrintWriter printWriter = new PrintWriter(new ByteArrayOutputStream());

    GeneratorContext ctx = createMock(GeneratorContext.class, "ctx");
    expect(ctx.tryCreate((TreeLogger) anyObject(), (String) anyObject(), (String) anyObject()))
        .andStubReturn(printWriter);

    Capture<FieldLiteral<SuperClass>> fieldCapture = new Capture<FieldLiteral<SuperClass>>();
    Capture<MethodLiteral<SuperClass, Method>> methodCapture =
        new Capture<MethodLiteral<SuperClass, Method>>();

    NameGenerator nameGenerator = createMock(NameGenerator.class, "nameGenerator");
    expect(nameGenerator
        .convertToValidMemberName(
            "injectStatic_com.google.gwt.inject.rebind.output."
            + "GinjectorBindingsOutputterTest$SubClass"))
        .andStubReturn("test_injectSubClass");
    expect(nameGenerator
        .convertToValidMemberName(
            "injectStatic_com.google.gwt.inject.rebind.output.subpackage."
            + "SubPackageClass"))
        .andStubReturn("test_injectSubPackageClass");

    SourceWriteUtil sourceWriteUtil = createMock(SourceWriteUtil.class, "sourceWriteUtil");
    expect(sourceWriteUtil.createFieldInjection(capture(fieldCapture), (String) anyObject(),
        (NameGenerator) anyObject(), (List<InjectorMethod>) anyObject()))
        .andReturn(SourceSnippets.forText(""));

    MethodCallUtil methodCallUtil = createMock(MethodCallUtil.class, "methodCallUtil");
    expect(methodCallUtil.createMethodCallWithInjection(capture(methodCapture),
        (String) anyObject(), (NameGenerator) anyObject(), (List<InjectorMethod>) anyObject()))
        .andReturn(SourceSnippets.forText(""));

    GinjectorBindings bindings = createMock(GinjectorBindings.class, "bindings");
    expect(bindings.getNameGenerator())
        .andStubReturn(nameGenerator);
    expect(bindings.getStaticInjectionRequests()).andStubReturn(
        Arrays.<Class<?>>asList(SubClass.class, SubPackageClass.class));

    String ginjectorPackageName = "com.google.gwt.inject.rebind.output";
    String ginjectorClassName = "GinjectorFragmentOutputterTest$FakeGinjector";

    GinjectorFragmentOutputter.Factory fragmentOutputterFactory =
        createMock(GinjectorFragmentOutputter.Factory.class, "fragmentOutputterFactory");
    GinjectorFragmentOutputter fragmentOutputter =
        createMock(GinjectorFragmentOutputter.class, "fragmentOutputter");
    GinjectorFragmentOutputter fragmentOutputterSubpackage =
        createMock(GinjectorFragmentOutputter.class, "fragmentOutputterSubpackage");

    expect(fragmentOutputterFactory.create(bindings,
        new FragmentPackageName(null, "com.google.gwt.inject.rebind.output"), ginjectorPackageName,
            ginjectorClassName))
        .andStubReturn(fragmentOutputter);

    expect(fragmentOutputterFactory.create(bindings,
        new FragmentPackageName(null, "com.google.gwt.inject.rebind.output.subpackage"),
            ginjectorPackageName, ginjectorClassName))
        .andStubReturn(fragmentOutputterSubpackage);

    fragmentOutputter.outputMethod((InjectorMethod) anyObject());
    fragmentOutputterSubpackage.outputMethod((InjectorMethod) anyObject());

    fragmentOutputter.invokeInInitializeStaticInjections("test_injectSubClass");
    fragmentOutputterSubpackage.invokeInInitializeStaticInjections("test_injectSubPackageClass");

    replay();

    GinjectorBindingsOutputter outputter = new GinjectorBindingsOutputter(ctx,
        null, fragmentOutputterFactory, new TestFragmentPackageNameFactory(), null,
        TreeLogger.NULL, methodCallUtil, null, null);
    GinjectorBindingsOutputter.FragmentMap fragments =
        new GinjectorBindingsOutputter.FragmentMap(bindings, ginjectorPackageName,
            ginjectorClassName, fragmentOutputterFactory);
    outputter.outputStaticInjections(bindings, fragments, sourceWriteUtil);

    verify();

    TypeLiteral<SuperClass> superClass = TypeLiteral.get(SuperClass.class);

    assertEquals(superClass, methodCapture.getValue().getDeclaringType());
    assertEquals(superClass, fieldCapture.getValue().getDeclaringType());
  }

  private static class TestFragmentPackageNameFactory implements FragmentPackageName.Factory {
    public FragmentPackageName create(String packageName) {
      return new FragmentPackageName(Ginjector.class, packageName);
    }
  }

  public static class SuperClass {
    @Inject static String foo;

    @Inject
    static void setBar(String ignored) {}
  }

  public static class SubClass extends SuperClass {}
}
