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

import com.google.gwt.inject.rebind.util.SourceWriteUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Key;

import junit.framework.TestCase;

import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;

public class BindConstantBindingTest extends TestCase {

  // TODO(schmitt):  Add tests for other constant types.

  public void testEnum() {
    Key<Color> colorKey = Key.get(Color.class);
    SourceWriteUtil utilMock = createMock(SourceWriteUtil.class);
    SourceWriter writerMock = createMock(SourceWriter.class);

    String signature = "";
    utilMock.writeMethod(writerMock, signature,
        "return com.google.gwt.inject.rebind.binding.Color.Green;");
    replay(utilMock);

    BindConstantBinding binding = new BindConstantBinding(utilMock);
    binding.setKeyAndInstance(colorKey, Color.Green);

    assertTrue(binding.getRequiredKeys().getRequiredKeys().isEmpty());

    binding.writeCreatorMethods(writerMock, signature);

    verify(utilMock);
  }

  public void testInnerEnum() {
    Key<Font> fontKey = Key.get(Font.class);
    SourceWriteUtil utilMock = createMock(SourceWriteUtil.class);
    SourceWriter writerMock = createMock(SourceWriter.class);

    String signature = "";
    utilMock.writeMethod(writerMock, signature,
        "return com.google.gwt.inject.rebind.binding.BindConstantBindingTest.Font.Verdana;");
    replay(utilMock);

    BindConstantBinding binding = new BindConstantBinding(utilMock);
    binding.setKeyAndInstance(fontKey, Font.Verdana);

    binding.writeCreatorMethods(writerMock, signature);

    verify(utilMock);
  }

  public void testInnerEnumWithCustomImplementation() {
    Key<Font> fontKey = Key.get(Font.class);
    SourceWriteUtil utilMock = createMock(SourceWriteUtil.class);
    SourceWriter writerMock = createMock(SourceWriter.class);

    String signature = "";
    utilMock.writeMethod(writerMock, signature,
        "return com.google.gwt.inject.rebind.binding.BindConstantBindingTest.Font.Arial;");
    replay(utilMock);

    BindConstantBinding binding = new BindConstantBinding(utilMock);
    binding.setKeyAndInstance(fontKey, Font.Arial);

    binding.writeCreatorMethods(writerMock, signature);

    verify(utilMock);
  }

  public void testCharacter() {
    Key<Character> charKey = Key.get(Character.class);
    SourceWriteUtil utilMock = createMock(SourceWriteUtil.class);
    SourceWriter writerMock = createMock(SourceWriter.class);

    char value = '\u1234';
    String signature = "";

    utilMock.writeMethod(writerMock, signature, "return '" + value + "';");
    replay(utilMock);

    BindConstantBinding binding = new BindConstantBinding(utilMock);
    binding.setKeyAndInstance(charKey, value);

    assertTrue(binding.getRequiredKeys().getRequiredKeys().isEmpty());

    binding.writeCreatorMethods(writerMock, signature);

    verify(utilMock);
  }

  public void testCharacterEscaped() {
    Key<Character> charKey = Key.get(Character.class);
    SourceWriteUtil utilMock = createMock(SourceWriteUtil.class);
    SourceWriter writerMock = createMock(SourceWriter.class);

    char value = '\'';
    String signature = "";

    utilMock.writeMethod(writerMock, signature, "return '\\'';");
    replay(utilMock);

    BindConstantBinding binding = new BindConstantBinding(utilMock);
    binding.setKeyAndInstance(charKey, value);

    assertTrue(binding.getRequiredKeys().getRequiredKeys().isEmpty());

    binding.writeCreatorMethods(writerMock, signature);

    verify(utilMock);
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
