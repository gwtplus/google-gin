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

import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.Comparator;
import java.util.Map;

public class Parameterized<S extends CharSequence> {

  private S foo;

  @Inject
  public Parameterized(@Named("foo") S chars) {
    this.foo = chars;
  }

  public S getFoo() {
    return foo;
  }

  public static class InnerParameterized<T> {

    private T value;

    @Inject
    public InnerParameterized(@Named("bar") T value) {
      this.value = value;
    }

    public T getValue() {
      return value;
    }
  }

  public static class
      ComplicatedParameterized<X, Z extends Comparator<String>, V extends Map<X, Z>> {

    private V value;

    @Inject
    public ComplicatedParameterized(V value) {
      this.value = value;
    }

    public V getValue() {
      return value;
    }
  }

  public static class StringComparator implements Comparator<String> {

    public int compare(String s, String s1) {
      return s.compareTo(s1);
    }
  }
}