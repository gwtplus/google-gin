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
package com.google.gwt.inject.client.generics;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GenericsGinModule extends AbstractGinModule {
  protected void configure() {
    // Example of TypeLiteral --> TypeLiteral
    bind(new TypeLiteral<List<String>>() {})
        .to(new TypeLiteral<ArrayList<String>>() {});

    // Example of TypeLiteral --> Key
    // Note that this will make it resolve to what we bound LinkedList to below
    bind(new TypeLiteral<List<Integer>>() {})
        .to(Key.get(new TypeLiteral<LinkedList<Integer>>() {}));

    bindConstant().annotatedWith(Names.named("foo")).to("foo");
    bindConstant().annotatedWith(Names.named("bar")).to(3);

    bind(new TypeLiteral<List<? super String>>(){}).to(CharSequenceList.class);
  }

  @Provides
  @Singleton
  LinkedList<Integer> provideLinkedListOfInteger() {
    LinkedList<Integer> list = new LinkedList<Integer>();
    list.add(10);
    list.add(20);
    return list;
  }

  @Provides
  CharSequenceList provideCharSequenceList() {
    CharSequenceList list = new CharSequenceList();
    list.add("Hello");
    list.add("World");
    return list;
  }

  @Provides
  @Singleton
  Map<String, Parameterized.StringComparator> provideStringComparatorMap() {
    return new HashMap<String, Parameterized.StringComparator>();
  }

  public static class CharSequenceList extends ArrayList<CharSequence> {}
}
