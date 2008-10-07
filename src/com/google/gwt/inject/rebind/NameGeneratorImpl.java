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
package com.google.gwt.inject.rebind;

import com.google.inject.Key;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper to generate various names for a binding that are needed when
 * outputting the Java code for that binding.
 */
public class NameGeneratorImpl implements NameGenerator {
  private final Map<Key<?>, String> cache = new HashMap<Key<?>, String>();

  public String sourceNameToBinaryName(ClassType type, String fullyQualifiedClassName) {
    return type.getBinaryClassName(fullyQualifiedClassName);
  }

  public String binaryNameToSourceName(String fullyQualifiedClassName) {
    return replaceLast(fullyQualifiedClassName, '$', '.');
  }

  public String getGetterMethodName(Key<?> key) {
    return "get_" + mangle(key);
  }

  public String getCreatorMethodName(Key<?> key) {
    return "create_" + mangle(key);
  }

  public String getSingletonFieldName(Key<?> key) {
    return "singleton_" + mangle(key);
  }

  private String mangle(Key<?> key) {
    String cached = cache.get(key);
    if (cached != null) {
      return cached;
    }

    // TODO(bstoler): This algorithm is kinda crazy because the annotation
    // values are of unbounded length. One option
    // is to use mangled(type) + mangled(annotation type) + counter, where
    // counter is used just to distinguish different annotation values.
    String name = key.toString();
    name = name.replaceAll("\\s+", "_");
    name = name.replaceAll("[^\\p{Alnum}_]", "\\$");
    cache.put(key, name);
    return name;
  }

  static String replaceLast(String source, char toReplace, char with) {
    StringBuilder sb = new StringBuilder(source);
    int index = sb.lastIndexOf(String.valueOf(toReplace));
    if (index != -1) {
      sb.setCharAt(index, with);
    }
    return sb.toString(); 
  }
}
