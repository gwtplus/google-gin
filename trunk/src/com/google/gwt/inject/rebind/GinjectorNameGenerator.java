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

import com.google.gwt.inject.client.Ginjector;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages class and field names for GinjectorBindings.
 */
@Singleton
public class GinjectorNameGenerator {
  // TODO(dburrows): field name calculations could be moved into
  // GinjectorBindings, now that fields are private to individual Ginjectors.
  private final Map<String, Integer> numberOfAppearances = new LinkedHashMap<String, Integer>();
  private final Map<GinjectorBindings, String> nameCache =
      new LinkedHashMap<GinjectorBindings, String>();

  // Used to prevent different runs of GinjectorGenerator on different ginjector
  // interfaces from producing classes with the same name.  For instance, if
  // FooGinModule is a private module, installing it in both Ginjector1 and
  // Ginjector2 must produce two distinct classes.
  private final Class<?> ginjectorInterface;

  @Inject
  GinjectorNameGenerator(@GinjectorInterfaceType Class<? extends Ginjector> ginjectorInterface) {
    this.ginjectorInterface = ginjectorInterface;
  }

  /**
   * @return the class name to use for the given ginjector
   */  
  public String getClassName(GinjectorBindings bindings) {
    return getName(bindings);
  }

  /**
   * @return the canonical class name (including package) to use for the given ginjector
   */
  public String getCanonicalClassName(GinjectorBindings bindings) {
    return bindings.getModule().getPackage().getName() + "." + getClassName(bindings);
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

    // Mangle the Ginjector name into the generated class name to ensure
    // uniqueness between different runs of the generator.
    //
    // TODO(dburrows): it would be nice if we could be even more defensive and
    // avoid any name that anyone ever chose, but my experiments indicate that
    // TypeOracle doesn't know about generated classes when this code runs.
    name = ginjectorInterface.getCanonicalName() + "_" + bindings.getModule().getSimpleName();
    name = name.replace(".", "_");
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
