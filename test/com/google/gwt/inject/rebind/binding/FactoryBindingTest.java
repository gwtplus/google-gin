/*
 * Copyright 2010 Google Inc.
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

package com.google.gwt.inject.rebind.binding;

import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.inject.ConfigurationException;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import junit.framework.TestCase;

import java.util.Collections;
import java.util.List;

public class FactoryBindingTest extends TestCase {

  public void testTooManyParams() throws NotFoundException {
    FactoryBinding binding = new FactoryBinding(null, null);

    try {
      binding.setKeyAndCollector(Key.get(BrokenBeetleFactory.class),
          Collections.<Key<?>, TypeLiteral<?>>emptyMap());
      fail("Expected ConfigurationException.");
    } catch (ConfigurationException e) {
      assertTrue(e.getMessage().contains("no constructors"));
    }
  }

  public void testMismatchingParams() throws NotFoundException {
    FactoryBinding binding = new FactoryBinding(null, null);

    try {
      binding.setKeyAndCollector(Key.get(BrokenGolfFactory.class),
          Collections.<Key<?>, TypeLiteral<?>>emptyMap());
      fail("Expected ConfigurationException.");
    } catch (ConfigurationException e) {
      assertTrue(e.getMessage().contains("has @AssistedInject constructors"));
    }
  }

  public interface BrokenBeetleFactory {
    Beetle create(int year);
  }

  public static class Beetle {

    @Inject
    public Beetle(@Assisted String name, @Assisted Integer year) {}
  }

  public interface BrokenGolfFactory {
    Golf create(List<Integer> foo, String bar);
  }

  public static class Golf {

    @AssistedInject
    public Golf(@Assisted List<String> foo, @Assisted String bar) {}
  }
}
