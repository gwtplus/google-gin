/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gwt.inject.rebind.resolution;

import static org.easymock.EasyMock.expect;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.inject.rebind.binding.BindingFactory;
import com.google.gwt.inject.rebind.binding.CallGwtDotCreateBinding;
import com.google.gwt.inject.rebind.resolution.ImplicitBindingCreator.BindingCreationException;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

public class ImplicitBindingCreatorTest extends TestCase {

  private ImplicitBindingCreator bindingCreator;
  private IMocksControl control;
  private CallGwtDotCreateBinding callGwtDotCreateBinding;

  // Mocks:
  private BindingFactory bindingFactory;
  private GeneratorContext generatorContext;
  private TreeLogger treeLogger;

  private void replay() {
    control.replay();
  }

  private void verify() {
    control.verify();
  }

  protected void setUp() throws Exception {
    super.setUp();

    control = EasyMock.createControl();

    this.bindingFactory = control.createMock(BindingFactory.class);
    this.callGwtDotCreateBinding = control.createMock(CallGwtDotCreateBinding.class);
    this.generatorContext = control.createMock(GeneratorContext.class);
    this.treeLogger = control.createMock(TreeLogger.class);

    bindingCreator = new ImplicitBindingCreator(bindingFactory, this.generatorContext, treeLogger);
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    verify();
  }

  public void testCreate_interface_hasRebindRule() throws BindingCreationException {
    expect(generatorContext.checkRebindRuleAvailable(Interface.class.getCanonicalName()))
        .andStubReturn(true);
    expect(bindingFactory.getCallGwtDotCreateBinding(TypeLiteral.get(Interface.class)))
        .andReturn(callGwtDotCreateBinding);
    replay();

    assertEquals(callGwtDotCreateBinding, bindingCreator.create(Key.get(Interface.class)));
  }

  public void testCreate_interface_noRebindRule(){
    expect(generatorContext.checkRebindRuleAvailable(Interface.class.getCanonicalName()))
        .andStubReturn(false);
    replay();

    // Verify that creating an implicit binding for a type without a rebind rule
    // fails.
    try {
      bindingCreator.create(Key.get(Interface.class));
      fail("Expected BindingCreationException.");
    } catch (BindingCreationException expected) {
    }
  }

  public void testCreate_concrete_nullaryConstructor() throws BindingCreationException {
    expect(generatorContext.checkRebindRuleAvailable(NullaryConstructor.class.getCanonicalName()))
        .andStubReturn(false);
    expect(bindingFactory.getCallGwtDotCreateBinding(TypeLiteral.get(NullaryConstructor.class)))
        .andReturn(callGwtDotCreateBinding);
    replay();

    // Verify that creating an implicit binding succeeds if the target class has
    // a nullary constructor, regardless of whether it has a rebind rule.
    assertEquals(callGwtDotCreateBinding, bindingCreator.create(Key.get(NullaryConstructor.class)));
  }

  public void testCreate_concrete_noConstructor() throws BindingCreationException {
    expect(generatorContext.checkRebindRuleAvailable(NoConstructor.class.getCanonicalName()))
        .andStubReturn(false);
    expect(bindingFactory.getCallGwtDotCreateBinding(TypeLiteral.get(NoConstructor.class)))
        .andReturn(callGwtDotCreateBinding);
    replay();

    // Verify that creating an implicit binding succeeds if the target class has
    // only a default constructor, regardless of whether it has a rebind rule.
    assertEquals(callGwtDotCreateBinding, bindingCreator.create(Key.get(NoConstructor.class)));
  }

  public void testCreate_concrete_noNullaryConstructor_noRebindRule(){
    expect(generatorContext.checkRebindRuleAvailable(NoNullaryConstructor.class.getCanonicalName()))
        .andStubReturn(false);
    replay();

    // Verify that creating an implicit binding fails if the target class has
    // only non-nullary constructors and no rebind rule.
    try {
      bindingCreator.create(Key.get(NoNullaryConstructor.class));
      fail("Expected BindingCreationException");
    } catch (BindingCreationException expected) {
    }
  }

  public void testCreate_concrete_noNullaryConstructor_hasRebindRule()
      throws BindingCreationException {
    expect(generatorContext.checkRebindRuleAvailable(NoNullaryConstructor.class.getCanonicalName()))
        .andStubReturn(true);
    expect(bindingFactory.getCallGwtDotCreateBinding(TypeLiteral.get(NoNullaryConstructor.class)))
        .andReturn(callGwtDotCreateBinding);
    replay();

    // Verify that creating an implicit binding succeeds if the target class has
    // only non-nullary constructors and a rebind rule.
    assertEquals(callGwtDotCreateBinding,
        bindingCreator.create(Key.get(NoNullaryConstructor.class)));
  }

  private static class NoConstructor {
  }

  private static class NullaryConstructor {
    public NullaryConstructor() {
    }

    public NullaryConstructor(int x) {
    }
  }

  private static class NoNullaryConstructor {
    public NoNullaryConstructor(int x) {
    }
  }

  private interface Interface {
  }
}
