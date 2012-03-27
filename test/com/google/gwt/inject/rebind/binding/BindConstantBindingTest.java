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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import com.google.gwt.inject.rebind.util.InjectorWriteContext;
import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Key;
import junit.framework.TestCase;

public class BindConstantBindingTest extends TestCase {

  private static final String SOURCE = "dummy";
  private static final Context CONTEXT = Context.forText(SOURCE);

  // TODO(schmitt):  Add tests for other constant types.

  public void testEnum() {
    Key<Color> colorKey = Key.get(Color.class);
    InjectorWriteContext writeContextMock = createMock(InjectorWriteContext.class);

    replay(writeContextMock);

    BindConstantBinding<Color> binding =
        new BindConstantBinding<Color>(colorKey, Color.Green, CONTEXT);

    assertEquals(1, binding.getDependencies().size());
    // This doesn't actually check that the source is passed along, due to
    // dependency sources being excluded from equals().
    assertTrue(binding.getDependencies().contains(
        new Dependency(Dependency.GINJECTOR, colorKey, SOURCE)));

    StringBuilder methodBody = new StringBuilder();
    binding.appendCreatorMethodBody(methodBody, writeContextMock);
    assertEquals("return com.google.gwt.inject.rebind.binding.Color.Green;", methodBody.toString());

    verify(writeContextMock);
  }

  public void testInnerEnum() {
    Key<Font> fontKey = Key.get(Font.class);
    InjectorWriteContext writeContextMock = createMock(InjectorWriteContext.class);

    replay(writeContextMock);

    BindConstantBinding<Font> binding =
        new BindConstantBinding<Font>(fontKey, Font.Verdana, CONTEXT);

    StringBuilder methodBody = new StringBuilder();
    binding.appendCreatorMethodBody(methodBody, writeContextMock);
    assertEquals(
        "return com.google.gwt.inject.rebind.binding.BindConstantBindingTest.Font.Verdana;",
        methodBody.toString());

    verify(writeContextMock);
  }

  public void testInnerEnumWithCustomImplementation() {
    Key<Font> fontKey = Key.get(Font.class);
    InjectorWriteContext writeContextMock = createMock(InjectorWriteContext.class);

    replay(writeContextMock);

    BindConstantBinding<Font> binding =
        new BindConstantBinding<Font>(fontKey, Font.Arial, CONTEXT);

    StringBuilder methodBody = new StringBuilder();
    binding.appendCreatorMethodBody(methodBody, writeContextMock);
    assertEquals("return com.google.gwt.inject.rebind.binding.BindConstantBindingTest.Font.Arial;",
        methodBody.toString());
    verify(writeContextMock);
  }

  public void testCharacter() {
    Key<Character> charKey = Key.get(Character.class);
    InjectorWriteContext writeContextMock = createMock(InjectorWriteContext.class);

    char value = '\u1234';

    replay(writeContextMock);

    BindConstantBinding<Character> binding =
        new BindConstantBinding<Character>(charKey, value, CONTEXT);

    assertEquals(1, binding.getDependencies().size());
    // This doesn't actually check that the source is passed along, due to
    // dependency sources being excluded from equals().
    assertTrue(binding.getDependencies().contains(
        new Dependency(Dependency.GINJECTOR, charKey, SOURCE)));

    StringBuilder methodBody = new StringBuilder();
    binding.appendCreatorMethodBody(methodBody, writeContextMock);
    assertEquals("return '" + value + "';", methodBody.toString());

    verify(writeContextMock);
  }

  public void testCharacterEscaped() {
    Key<Character> charKey = Key.get(Character.class);
    InjectorWriteContext writeContextMock = createMock(InjectorWriteContext.class);

    char value = '\'';

    replay(writeContextMock);

    BindConstantBinding<Character> binding =
        new BindConstantBinding<Character>(charKey, value, CONTEXT);

    assertEquals(1, binding.getDependencies().size());
    // This doesn't actually check that the source is passed along, due to
    // dependency sources being excluded from equals().
    assertTrue(binding.getDependencies().contains(
        new Dependency(Dependency.GINJECTOR, charKey, SOURCE)));

    StringBuilder methodBody = new StringBuilder();
    binding.appendCreatorMethodBody(methodBody, writeContextMock);
    assertEquals("return '\\'';", methodBody.toString());

    verify(writeContextMock);
  }

  public enum Font {
    Arial {

      @Override public Font getAlternative() {
        return Verdana;
      }},
    Verdana,
    TimesNewRoman;

    public Font getAlternative() {
      return this;
    }
  }
}
