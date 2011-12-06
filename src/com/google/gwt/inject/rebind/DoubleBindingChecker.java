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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.util.Preconditions;
import com.google.gwt.dev.util.collect.HashSet;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.ExposedChildBinding;
import com.google.gwt.inject.rebind.binding.ParentBinding;
import com.google.gwt.inject.rebind.util.PrettyPrinter;
import com.google.inject.Inject;
import com.google.inject.Key;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validate that a Ginjector hierarchy doesn't contain any duplicate bindings.
 */
public class DoubleBindingChecker {

  private final ErrorManager errorManager;

  private final TreeLogger logger;

  @Inject
  public DoubleBindingChecker(ErrorManager errorManager, TreeLogger logger) {
    this.errorManager = errorManager;
    this.logger = logger;
  }

  public void checkBindings(GinjectorBindings ginjector) {
    Map<Key<?>, GinjectorBindings> bindingSources =
        new LinkedHashMap<Key<?>, GinjectorBindings>();
    checkBindings(ginjector, bindingSources);
    Preconditions.checkState(bindingSources.isEmpty());
  }

  public void checkBindings(GinjectorBindings ginjector,
                            Map<Key<?>, GinjectorBindings> bindingSources) {
    // Add bindings from this ginjector, reporting errors if detected
    List<Key<?>> keysFromGinjector = new ArrayList<Key<?>>();
    for (Key<?> key : ginjector.getBoundKeys()) {
      GinjectorBindings newSource = findSource(ginjector, key);
      GinjectorBindings oldSource = bindingSources.put(key, newSource);
      if (oldSource != null && oldSource != newSource) {
        // TODO(bchambers, dburrows): We should revisit where double-bindings
        // come from, and how they get reported more systematically, to make
        // sure that we produce the most useful error messages possible.
        errorManager.logDoubleBind(key,
            newSource.getBinding(key), newSource,
            oldSource.getBinding(key), oldSource);
      } else {
        // If there was already a matching value in the map, we shouldn't remove it
        // here.  Instead, let the person who put the binding in clear it out.
        keysFromGinjector.add(key);
      }
    }

    // Visit each child ginjector in the context we've built up
    for (GinjectorBindings child : ginjector.getChildren()) {
      checkBindings(child, bindingSources);
    }

    // Before going back up, remove any state that was added at this level
    for (Key<?> key : keysFromGinjector) {
      bindingSources.remove(key);
    }
  }

  /**
   * Find the ginjector that we "really" get the binding for key from.  That is,
   * if it is inherited from a child/parent, return that injector.
   */
  private GinjectorBindings findSource(GinjectorBindings ginjector, Key<?> key) {
    Set<GinjectorBindings> visited = new HashSet<GinjectorBindings>();

    GinjectorBindings lastGinjector = null;
    while (ginjector != null) {
      if (!visited.add(ginjector)) {
        logger.log(Type.ERROR, PrettyPrinter.format(
          "Cycle detected in bindings for %s", key));
        return ginjector; // at this point, just return *something*
      }
      
      lastGinjector = ginjector;
      ginjector = linkedGinjector(ginjector.getBinding(key));
    }
    return lastGinjector;
  }

  private GinjectorBindings linkedGinjector(Binding binding) {
    GinjectorBindings nextGinjector = null;
    if (binding instanceof ExposedChildBinding) {
      ExposedChildBinding childBinding = (ExposedChildBinding) binding;
      nextGinjector = childBinding.getChildBindings();
    } else if (binding instanceof ParentBinding) {
      ParentBinding parentBinding = (ParentBinding) binding;
      nextGinjector = parentBinding.getParentBindings();
    }
    return nextGinjector;
  }
}