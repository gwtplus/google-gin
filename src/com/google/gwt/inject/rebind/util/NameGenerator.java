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

import com.google.gwt.dev.util.Preconditions;
import com.google.gwt.inject.rebind.output.FragmentPackageName;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Helper to generate various names for members of a {@code Ginjector}
 * implementation.
 */
public class NameGenerator {

  private class CacheKey {
    private final String prefix;
    private final Key<?> key;
    
    CacheKey(String prefix, Key<?> key) {
      this.prefix = prefix;
      this.key = key;
    }
    
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof CacheKey) {
        CacheKey other = (CacheKey) obj;
        return other.prefix.equals(prefix) && other.key.equals(key);
      }
      return false;
    }
    
    @Override
    public int hashCode() {
      return prefix.hashCode() * 31 +  key.hashCode();
    }
  }
  
  /**
   * "Mangled key name" cache:  Key -> mangled name
   */
  private final Map<CacheKey, String> methodKeyCache = new LinkedHashMap<CacheKey, String>();
  
  /**
   * Map of known method names.
   */
  private final Set<String> methodNames = new LinkedHashSet<String>();

  /**
   * Returns the name of an assisted injection helper method.
   */
  public String getAssistedInjectMethodName(Key<?> factoryKey, String methodName) {
    return mangle("assistedInject_" + methodName, factoryKey);
  }

  /**
   * Returns the name of a getter for a child injector.
   */
  public String getChildInjectorGetterMethodName(String childInjectorClassName) {
    return convertToValidMemberName("getChild_" + childInjectorClassName);
  }

  /**
   * Returnst he name of a getter for an injector fragment.
   */
  public String getFragmentGetterMethodName(FragmentPackageName fragmentPackageName) {
    return "getFragment_" + fragmentPackageName.toString().replace(".", "_");
  }

  /**
   * Returns the key's getter method name.  The method with that name can be
   * called to retrieve an instance of the type described by the key.
   *
   * @return getter method name
   */
  public String getGetterMethodName(Key<?> key) {
    return mangle("get_", key);
  }

  /**
   * Returns the key's creator method name.  The method with that name can be
   * called to create and retrieve an instance of the type described by the
   * key.
   *
   * @return creator method name
   */
  public String getCreatorMethodName(Key<?> key) {
    return mangle("create_", key);
  }

  /**
   * Computes the name of a single fragment of a Ginjector.
   *
   * @param injectorClassName the simple name of the injector's class (not
   *     including its package)
   */
  public String getFragmentClassName(String injectorClassName,
      FragmentPackageName fragmentPackageName) {
    // Sanity check.
    Preconditions.checkArgument(!injectorClassName.contains("."),
        "The injector class must be a simple name, but it was \"%s\"", injectorClassName);
    // Relies on the fact that injector class names are globally unique, so we
    // don't need to include the injector's package.
    return injectorClassName + "_fragment_" + fragmentPackageName.toString().replace(".", "_");
  }

  /**
   * Computes the canonical name (including package) of a single fragment of a
   * Ginjector.
   */
  public String getFragmentCanonicalClassName(String injectorClassName,
      FragmentPackageName fragmentPackageName) {
    return fragmentPackageName + "." + getFragmentClassName(injectorClassName, fragmentPackageName);
  }

  /**
   * Computes the field name of a single fragment of an injector.
   */
  public String getFragmentFieldName(FragmentPackageName fragmentPackageName) {
    return convertToValidMemberName("fieldFragment_" + fragmentPackageName);
  }

  /**
   * Computes the name of the field in which the Ginjector interface
   * implementation is stored.
   */
  public String getGinjectorInterfaceFieldName() {
    return "fieldGinjectorInterface";
  }

  /**
   * Computes the name of the method used to retrieve the Ginjector interface
   * implementation.
   */
  public String getGinjectorInterfaceGetterMethodName() {
    return "getGinjectorInterface";
  }

  /**
   * Returns the type's member inject method name.  The method with that name
   * can be called with a single parameter to inject members of that parameter.
   *
   * @return member inject method name
   */
  public String getMemberInjectMethodName(TypeLiteral<?> type) {
    return mangle("memberInject_", Key.get(type));
  }

  /**
   * Returns the key's singleton field name.
   *
   * @return singleton field name
   */
  public String getSingletonFieldName(Key<?> key) {
    return mangle("singleton_", key);
  }
  
  /**
   * Returns a new valid (i.e. unique) method name based on {@code base}.
   *
   * Note: Method names are considered "used" after being returned by this
   * method, whether they're actually used to write a new method or not.
   *
   * @param base base on which method name gets created
   * @return valid method name
   */
  public String createMethodName(String base) {
    while (methodNames.contains(base)) {

      // TODO(schmitt):  Make more efficient for repeated calls with same base?
      base += "_";
    }
    methodNames.add(base);
    return base;
  }

  /**
   * Reserves the given name to prevent new methods to be created with it.
   *
   * @param name name to be reserved
   */
  public void markAsUsed(String name) throws IllegalArgumentException {
    methodNames.add(name);
  }

  private String mangle(String prefix, Key<?> key) {
    CacheKey cacheKey = new CacheKey(prefix, key);
    String cached = methodKeyCache.get(cacheKey);
    if (cached != null) {
      return cached;
    }

    // TODO(bstoler): This algorithm is kinda crazy because the annotation
    // values are of unbounded length. One option
    // is to use mangled(type) + mangled(annotation type) + counter, where
    // counter is used just to distinguish different annotation values.
    String name = prefix + key.toString();
    name = convertToValidMemberName(name);
    
    name = createMethodName(name);

    methodKeyCache.put(cacheKey, name);
    return name;
  }

  public String convertToValidMemberName(String name) {
    name = name.replaceAll("\\s+", "_");
    name = name.replaceAll("[^\\p{Alnum}_]", "\\$");
    return name;
  }

  // Static for access from enum.
  public static String replaceLast(String source, char toReplace, char with) {
    StringBuilder sb = new StringBuilder(source);
    int index = sb.lastIndexOf(String.valueOf(toReplace));
    if (index != -1) {
      sb.setCharAt(index, with);
    }
    return sb.toString();
  }
}
