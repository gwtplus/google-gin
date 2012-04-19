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

import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.gwt.inject.rebind.util.InjectorMethod;
import com.google.gwt.inject.rebind.util.InjectorWriteContext;
import com.google.inject.Key;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class BindConstantBindingTest extends TestCase {

  private static final String SOURCE = "dummy";
  private static final Context CONTEXT = Context.forText(SOURCE);

  // TODO(schmitt):  Add tests for other constant types.

  public void testEnum() throws NoSourceNameException {
    Key<Color> colorKey = Key.get(Color.class);

    BindConstantBinding<Color> binding =
        new BindConstantBinding<Color>(colorKey, Color.Green, CONTEXT);

    assertEquals(1, binding.getDependencies().size());
    // This doesn't actually check that the source is passed along, due to
    // dependency sources being excluded from equals().
    assertTrue(binding.getDependencies().contains(
        new Dependency(Dependency.GINJECTOR, colorKey, SOURCE)));

    assertCreationStatements(binding, "com.google.gwt.inject.rebind.binding.Color result ="
        + " com.google.gwt.inject.rebind.binding.Color.Green;");
  }

  public void testInnerEnum() throws NoSourceNameException {
    Key<Font> fontKey = Key.get(Font.class);

    BindConstantBinding<Font> binding =
        new BindConstantBinding<Font>(fontKey, Font.Verdana, CONTEXT);

    assertCreationStatements(binding,
        "com.google.gwt.inject.rebind.binding.BindConstantBindingTest.Font result "
        + "= com.google.gwt.inject.rebind.binding.BindConstantBindingTest.Font.Verdana;");
  }

  public void testInnerEnumWithCustomImplementation() throws NoSourceNameException {
    Key<Font> fontKey = Key.get(Font.class);

    BindConstantBinding<Font> binding =
        new BindConstantBinding<Font>(fontKey, Font.Arial, CONTEXT);

    assertCreationStatements(binding,
        "com.google.gwt.inject.rebind.binding.BindConstantBindingTest.Font result "
        + "= com.google.gwt.inject.rebind.binding.BindConstantBindingTest.Font.Arial;");
  }

  public void testCharacter() throws NoSourceNameException {
    Key<Character> charKey = Key.get(Character.class);

    char value = '\u1234';

    BindConstantBinding<Character> binding =
        new BindConstantBinding<Character>(charKey, value, CONTEXT);

    assertEquals(1, binding.getDependencies().size());
    // This doesn't actually check that the source is passed along, due to
    // dependency sources being excluded from equals().
    assertTrue(binding.getDependencies().contains(
        new Dependency(Dependency.GINJECTOR, charKey, SOURCE)));

    assertCreationStatements(binding, "java.lang.Character result = '" + value + "';");
  }

  public void testCharacterEscaped() throws NoSourceNameException {
    Key<Character> charKey = Key.get(Character.class);

    char value = '\'';

    BindConstantBinding<Character> binding =
        new BindConstantBinding<Character>(charKey, value, CONTEXT);

    assertEquals(1, binding.getDependencies().size());
    // This doesn't actually check that the source is passed along, due to
    // dependency sources being excluded from equals().
    assertTrue(binding.getDependencies().contains(
        new Dependency(Dependency.GINJECTOR, charKey, SOURCE)));

    assertCreationStatements(binding, "java.lang.Character result = '\\'';");
  }

  /**
   * Verifies that invoking binding.getCreationStatements() produces no helper
   * methods, does not invoke any methods on the write context, and produces the
   * given statements.
   */
  private void assertCreationStatements(Binding binding, String expectedStatements)
      throws NoSourceNameException {
    InjectorWriteContext writeContextMock = createMock(InjectorWriteContext.class);

    replay(writeContextMock);

    List<InjectorMethod> methods = new ArrayList<InjectorMethod>();
    String actualStatements =
        binding.getCreationStatements(null, methods).getSource(writeContextMock);

    assertEquals(expectedStatements, actualStatements);
    assertEquals(0, methods.size());

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
