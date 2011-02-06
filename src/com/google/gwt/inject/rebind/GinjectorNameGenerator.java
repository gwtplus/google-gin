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
package com.google.gwt.inject.rebind;

import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages class and field names for GinjectorBindings.
 */
@Singleton
public class GinjectorNameGenerator {
  private final Map<String, Integer> numberOfAppearances = new HashMap<String, Integer>();
  private final Map<GinjectorBindings, String> nameCache = new HashMap<GinjectorBindings, String>();
 
  /**
   * Register a specific name for use with the given ginjector.  This is used on the root injector
   * which doesn't get a field.  The only name that matters is the class name, which gets added
   * to the cache.
   */ 
  public void registerName(GinjectorBindings bindings, String name) {
    assert !numberOfAppearances.containsKey(bindings.getModule().getSimpleName());
    numberOfAppearances.put(bindings.getModule().getSimpleName(), 1);
    nameCache.put(bindings, name);
  }

  /**
   * @return the class name to use for the given ginjector
   */  
  public String getClassName(GinjectorBindings bindings) {
    return getName(bindings);
  }
  
  /**
   * @return the field name to use for the given ginjector
   */  
  public String getFieldName(GinjectorBindings bindings) {
    return "field" + getName(bindings);
  }
  
  private String getName(GinjectorBindings bindings) {
    String name = nameCache.get(bindings);
    if (name != null) {
      return name;
    }
    
    name = bindings.getModule().getSimpleName();
    Integer appearance = numberOfAppearances.get(name);
    if (appearance == null) {
      numberOfAppearances.put(name, 1);
      name += "Ginjector";
    } else {
      numberOfAppearances.put(name, appearance + 1);
      name += "Ginjector" + appearance;
    }
    nameCache.put(bindings, name);
    return name;
  }
}
