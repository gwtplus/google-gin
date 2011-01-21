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
package com.google.gwt.inject.rebind.util;

import com.google.inject.Key;

import junit.framework.TestCase;

public class NameGeneratorTest extends TestCase {

  // TODO(schmitt):  Test mangle.
  // TODO(schmitt):  Test name translation.

  public void testRepeatedCalls() {
    NameGenerator nameGenerator = new NameGenerator();
    String foo = nameGenerator.getGetterMethodName(Key.get(String.class));
    String foo2 = nameGenerator.getGetterMethodName(Key.get(String.class));
    assertEquals(foo, foo2);

    foo = nameGenerator.getCreatorMethodName(Key.get(String.class));
    foo2 = nameGenerator.getCreatorMethodName(Key.get(String.class));
    assertEquals(foo, foo2);

    foo = nameGenerator.getSingletonFieldName(Key.get(String.class));
    foo2 = nameGenerator.getSingletonFieldName(Key.get(String.class));
    assertEquals(foo, foo2);
  }

  public void testCreateMethodName() {
    NameGenerator nameGenerator = new NameGenerator();
    String foo = nameGenerator.createMethodName("foo");
    String foo2 = nameGenerator.createMethodName("foo");
    assertFalse(foo.equals(foo2));
  }

  public void testReserveMethodName() {
    NameGenerator nameGenerator = new NameGenerator();
    nameGenerator.markAsUsed("foo");

    // Test repeat (should be ok).
    nameGenerator.markAsUsed("foo");

    String foo = nameGenerator.createMethodName("foo");

    assertFalse("foo".equals(foo));

    // Test reserve for existing name (should be ok).
    nameGenerator.markAsUsed(foo);
  }
  
  public void testGetCreatorMethodName() {
    NameGenerator nameGenerator = new NameGenerator();
    Key<?> key = Key.get(String.class);
    String reserved = "create_" 
        + nameGenerator.convertToValidMemberName(key.toString()); 
    nameGenerator.markAsUsed(reserved);
    String getStringCreator = nameGenerator.getCreatorMethodName(key);
    assertFalse(String.format("Expected '%s' to not equal '%s'",
        reserved, getStringCreator), reserved.equals(getStringCreator));
  }
  
  public void testCaching() {
    NameGenerator nameGenerator = new NameGenerator();
    Key<?> key = Key.get(String.class);
    String getStringCreator = nameGenerator.getCreatorMethodName(key);
    String getStringGetter = nameGenerator.getGetterMethodName(key);
    assertFalse(getStringGetter.equals(getStringCreator));
    assertEquals(getStringGetter, nameGenerator.getGetterMethodName(key));
  }
}
