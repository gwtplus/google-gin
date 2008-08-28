// Copyright 2008 Google Inc. All Rights Reserved.

package com.google.gwt.inject.rebind;

import com.google.inject.Key;

import java.util.Map;
import java.util.HashMap;

/**
 * Helper to generate various names for a binding that are needed when
 * outputting the Java code for that binding.
 *
 * @author bstoler@google.com (Brian Stoler)
 */
public class NameGenerator {
  private final Map<Key<?>, String> cache = new HashMap<Key<?>, String>();

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
}
