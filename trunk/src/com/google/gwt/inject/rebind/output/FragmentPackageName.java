/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.inject.rebind.output;

import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.rebind.GinjectorInterfaceType;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * Wrapping a String in {@link FragmentPackageName} converts it to a legal name
 * for a fragment package.  Any code that manipulates the package name of a
 * fragment should store and/or pass it around using this class, to ensure that
 * the name is legal.
 *
 * <p>Normally the requested name is used as the package name, but the JVM
 * forbids us from placing generated code in certain packages.  Luckily, we
 * never actually need to place code in those packages anyway, even if our rules
 * would normally cause us to do so (because users of Gin can only access public
 * parts of those packages).  Since it doesn't matter where those methods go, we
 * arbitrarily put them in the fragment corresponding to the ginjector
 * interface.
 */
public class FragmentPackageName {

  private final String name;

  private static final String[] prohibitedPackageNames = new String[] {
    "java.lang",
    "java.util",
  };

  @Inject
  FragmentPackageName(@GinjectorInterfaceType Class<? extends Ginjector> ginjectorInterface,
      @Assisted String requestedName) {

    name = sanitizePackageName(ginjectorInterface, requestedName);
  }

  private static String sanitizePackageName(Class<?> ginjectorInterface, String packageName) {
    for (String prohibitedPackageName : prohibitedPackageNames) {
      if (packageName.equals(prohibitedPackageName)
          || packageName.startsWith(prohibitedPackageName + ".")) {
        return ginjectorInterface.getPackage().getName();
      }
    }

    return packageName;
  }

  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (!(obj instanceof FragmentPackageName)) {
      return false;
    }

    return name.equals(((FragmentPackageName) obj).name);
  }

  public int hashCode() {
    return name.hashCode();
  }

  public String toString() {
    return name;
  }

  public interface Factory {
    /**
     * Request a fragment package name that can access values in the given
     * package name.
     */
    FragmentPackageName create(String requestedName);
  }
}

