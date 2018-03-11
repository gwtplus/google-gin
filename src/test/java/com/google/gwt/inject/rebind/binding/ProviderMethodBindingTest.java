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

package com.google.gwt.inject.rebind.binding;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.inject.rebind.ErrorManager;
import com.google.inject.Key;
import com.google.inject.internal.ProviderMethod;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.lang.reflect.Method;

/**
 * Tests for {@link ProviderMethodBinding}.
 */
public class ProviderMethodBindingTest extends TestCase {

  private ErrorManager errorManager;

  @Override
  public void setUp() {
    errorManager = new ErrorManager(TreeLogger.NULL);
  }

  public void testModuleHasNoConstructor() throws NoSuchMethodException {
    createProviderMethodBinding(new ModuleWithNoConstructor(),
        ModuleWithNoConstructor.class.getMethod("provideInt"));

    assertFalse(hasErrors());
  }

  public void testModuleWithDefaultConstructor() throws NoSuchMethodException {
    createProviderMethodBinding(new ModuleWithDefaultConstructor(),
        ModuleWithDefaultConstructor.class.getMethod("provideInt"));

    assertFalse(hasErrors());
  }

  public void testModuleWithPrivateDefaultConstructor() throws NoSuchMethodException {
    createProviderMethodBinding(new ModuleWithPrivateDefaultConstructor(),
        ModuleWithPrivateDefaultConstructor.class.getMethod("provideInt"));

    assertTrue(hasErrors());
  }

  public void testModuleWithNonDefaultConstructor() throws NoSuchMethodException {
    createProviderMethodBinding(new ModuleWithNonDefaultConstructor(0),
        ModuleWithNonDefaultConstructor.class.getMethod("provideInt"));

    assertTrue(hasErrors());
  }

  private ProviderMethodBinding createProviderMethodBinding(Object instance, Method method) {
    // Ew, but the constructor of ProviderMethod is private, and this is a
    // simple way to create a custom one.
    IMocksControl control = EasyMock.createControl();
    @SuppressWarnings("unchecked")
    ProviderMethod<Integer> providerMethod =
        (ProviderMethod<Integer>) control.createMock("providerMethod", ProviderMethod.class);

    EasyMock.expect(providerMethod.getInstance()).andStubReturn(instance);
    EasyMock.expect(providerMethod.getKey()).andStubReturn(Key.get(Integer.class));
    EasyMock.expect(providerMethod.getMethod()).andStubReturn(method);

    control.replay();

    // Note: guiceUtil and methodCallUtil are used in parts of the binding class
    // that we don't test currently, so are set to null.  When tests for
    // getCreationStatements() and getDependencies() are written, concrete
    // values (mocks?) will be required.
    ProviderMethodBinding result = new ProviderMethodBinding(
        errorManager,
        null,
        null,
        providerMethod,
        Context.forText("dummy context"));

    control.verify();
    return result;
  }

  private boolean hasErrors() {
    try {
      errorManager.checkForError();
    } catch (UnableToCompleteException exception) {
      return true;
    }

    return false;
  }

  public static class ModuleWithNoConstructor {
    public int provideInt() {
      return 0;
    }
  }

  public static class ModuleWithDefaultConstructor {
    ModuleWithDefaultConstructor() {
    }

    public int provideInt() {
      return 0;
    }
  }

  public static class ModuleWithPrivateDefaultConstructor {
    private ModuleWithPrivateDefaultConstructor() {
    }

    public int provideInt() {
      return 0;
    }
  }

  public static class ModuleWithNonDefaultConstructor {
    ModuleWithNonDefaultConstructor(int i) {
    }

    public int provideInt() {
      return 0;
    }
  }
}
