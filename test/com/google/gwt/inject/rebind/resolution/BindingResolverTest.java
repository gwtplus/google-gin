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

import static com.google.gwt.inject.rebind.resolution.TestUtils.asyncProviderFoo;
import static com.google.gwt.inject.rebind.resolution.TestUtils.bar;
import static com.google.gwt.inject.rebind.resolution.TestUtils.baz;
import static com.google.gwt.inject.rebind.resolution.TestUtils.foo;
import static com.google.gwt.inject.rebind.resolution.TestUtils.fooImpl;
import static com.google.gwt.inject.rebind.resolution.TestUtils.providerBar;
import static com.google.gwt.inject.rebind.resolution.TestUtils.providerBaz;
import static com.google.gwt.inject.rebind.resolution.TestUtils.providerFoo;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.classextension.EasyMock.createControl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.inject.rebind.ErrorManager;
import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.BindingContext;
import com.google.gwt.inject.rebind.binding.BindingFactory;
import com.google.gwt.inject.rebind.binding.Dependency;
import com.google.gwt.inject.rebind.binding.ExposedChildBinding;
import com.google.gwt.inject.rebind.binding.ParentBinding;
import com.google.gwt.inject.rebind.resolution.ImplicitBindingCreator.BindingCreationException;
import com.google.inject.Key;
import com.google.inject.util.Providers;
import junit.framework.TestCase;
import org.easymock.Capture;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BindingResolverTest extends TestCase {

  private static final String SOURCE = "source";

  // List to record binding collections that we mock so that they don't get GC'd and have
  // false failures due to finalize being called
  private List<GinjectorBindings> nodes = new ArrayList<GinjectorBindings>();
  private List<Binding> bindings = new ArrayList<Binding>();

  private ImplicitBindingCreator bindingCreator;
  private ErrorManager errorManager;
  private ParentBinding parentBinding;
  private IMocksControl control;
  private BindingResolver bindingResolver;
  private TreeLogger treeLogger;

  private BindingFactory bindingFactory;

  private void replay() {
    control.replay();
  }
  
  private void verify() {
    control.verify();
  }
  
  protected void setUp() throws Exception {
    super.setUp();
    treeLogger = EasyMock.createNiceMock(TreeLogger.class);
    control = createControl();
    parentBinding = control.createMock("parentBinding", ParentBinding.class);
    bindingCreator = control.createMock("bindingCreator", ImplicitBindingCreator.class);
    errorManager = control.createMock("errorManager", ErrorManager.class);
    bindingFactory = control.createMock("bindingFactory", BindingFactory.class);

    bindingResolver = new BindingResolver(
        Providers.of(new DependencyExplorer(bindingCreator, treeLogger)), 
        Providers.of(new UnresolvedBindingValidator(
            new EagerCycleFinder(errorManager), errorManager)), 
        Providers.of(new BindingInstaller(new BindingPositioner(), bindingFactory)));
  }
  
  private GinjectorBindings createInjectorNode(String name) {
    GinjectorBindings node = control.createMock(name, GinjectorBindings.class);
    nodes.add(node);
    expect(node.getParent()).andStubReturn(null);
    expect(node.getChildren()).andStubReturn(new ArrayList<GinjectorBindings>());
    expect(node.isBound(isA(Key.class))).andStubReturn(false);
    expect(node.isBoundInChild(isA(Key.class))).andStubReturn(false);
    expect(node.getBinding(isA(Key.class))).andStubReturn(null);
    expect(node.getModuleName()).andStubReturn(name);
    return node;
  }
  
  // TODO(bchambers): This may be cleaner if we extract an interface for BindingResolver
  // to use when resolving bindings, and then create a "test" version of that.
  private void setChildren(GinjectorBindings parent, GinjectorBindings... children) {
    expect(parent.getChildren()).andReturn(Arrays.asList(children)).anyTimes();
    for (GinjectorBindings child : children) {
      expect(child.getParent()).andReturn(parent).anyTimes();
    }
  }
  
  // Creates the following hierarchy of binding collections and returns a
  // StandardTree object with names for all the nodes.
  //            root
  //           /    \
  //       childL  childR
  //       /    \
  // childLL    childLR
  public StandardTree createExampleTree() {
    StandardTree tree = new StandardTree();
    tree.root = createInjectorNode("root");
    tree.childL = createInjectorNode("childL");
    tree.childR = createInjectorNode("childR");
    tree.childLL = createInjectorNode("childLL");
    tree.childLR = createInjectorNode("childLR");
    setChildren(tree.root, tree.childL, tree.childR);
    setChildren(tree.childL, tree.childLL, tree.childLR);
    return tree;
  }
  
  private static class StandardTree {
    private GinjectorBindings root;
    private GinjectorBindings childL;
    private GinjectorBindings childLL;
    private GinjectorBindings childLR;
    private GinjectorBindings childR;
  }
  
  private void bind(Key<?> key, GinjectorBindings in) {
    Binding binding = control.createMock(Binding.class);
    expect(in.isBound(key)).andReturn(true).anyTimes();
    expect(in.getBinding(key)).andReturn(binding).anyTimes();
    bindings.add(binding);
  }
  
  private void bindChild(Key<?> key, GinjectorBindings parent, GinjectorBindings child) {
    ExposedChildBinding binding = control.createMock(ExposedChildBinding.class);
    expect(parent.isBound(key)).andReturn(true).anyTimes();
    expect(parent.getBinding(key)).andReturn(binding).anyTimes();
    expect(binding.getChildBindings()).andReturn(child).anyTimes();
    bindings.add(binding);
  }
  
  private void bindParent(Key<?> key, GinjectorBindings parent, GinjectorBindings child) {
    ParentBinding binding = control.createMock(ParentBinding.class);
    expect(binding.getParentBindings()).andReturn(parent).anyTimes();
    expect(child.isBound(key)).andReturn(true).anyTimes();
    expect(child.getBinding(key)).andReturn(binding).anyTimes();
    bindings.add(binding);
  }
  
  private void expectParentBinding(Key<?> key, GinjectorBindings parent, GinjectorBindings dest) {
    expect(bindingFactory.getParentBinding(eq(key), eq(parent), isA(BindingContext.class)))
        .andReturn(parentBinding);
    dest.addBinding(key, parentBinding);
  }
  
  private void replayAndResolve(GinjectorBindings origin, Dependency... unresolved) {
    expect(origin.getDependencies()).andStubReturn(TestUtils.dependencyList(unresolved));
    replay();
    bindingResolver.resolveBindings(origin);
    verify();
  }
  
  public void testResolveFoundInRoot() {
    StandardTree tree = createExampleTree();
    
    bind(foo(), tree.root);
    
    expectParentBinding(foo(), tree.root, tree.childLL);

    replayAndResolve(tree.childLL, required(Dependency.GINJECTOR, foo()));
  }
  
  public void testResolveDependenciesInRoot() throws Exception {
    StandardTree tree = createExampleTree();

    Binding fooBinding = expectCreateBinding(foo(), required(foo(), bar()), required(foo(), baz()));
    bind(bar(), tree.root);
    bind(baz(), tree.root);
    
    tree.root.addBinding(foo(), fooBinding);
    expectParentBinding(foo(), tree.root, tree.childLL);
    
    replayAndResolve(tree.childLL, required(Dependency.GINJECTOR, foo()));
  }
  
  public void testResolveDependenciesInRoot_InheritedByChild() throws Exception {
    StandardTree tree = createExampleTree();
    
    // Bar already in root, inherited in childL.  Baz already in root.  Foo can still be in root.
    Binding fooBinding = expectCreateBinding(foo(), required(foo(), bar()), required(foo(), baz()));
    bind(bar(), tree.root);
    bindParent(bar(), tree.root, tree.childL);
    bind(baz(), tree.root);
    

    tree.root.addBinding(foo(), fooBinding);
    expectParentBinding(foo(), tree.root, tree.childLL);
    
    replayAndResolve(tree.childLL, required(Dependency.GINJECTOR, foo()));
  }
  
  public void testResolveDependenciesInOriginExposedToParent() throws Exception {
    StandardTree tree = createExampleTree();
    // Bar is bound in the child, but exposed to the root.  Foo should still be in root
    bind(bar(), tree.childR);
    bindChild(bar(), tree.root, tree.childR);
    bind(baz(), tree.root);
    Binding fooBinding = expectCreateBinding(foo(), required(foo(), bar()), required(foo(), baz()));

    tree.root.addBinding(foo(), fooBinding);
    expectParentBinding(foo(), tree.root, tree.childR);

    replayAndResolve(tree.childR, required(Dependency.GINJECTOR, foo()));
  }

  public void testResolveDependenciesResolveInOriginExposedToParent() throws Exception {
    StandardTree tree = createExampleTree();
    // Bar is bound in the child, but exposed to the root.  Foo should still be in root
    bindChild(bar(), tree.root, tree.childR);
    bind(baz(), tree.root);
    Binding fooBinding = expectCreateBinding(foo(), required(foo(), bar()), required(foo(), baz()));
    Binding barBinding = expectCreateBinding(bar());

    tree.root.addBinding(foo(), fooBinding);
    tree.childR.addBinding(bar(), barBinding);
    expectParentBinding(foo(), tree.root, tree.childR);

    replayAndResolve(tree.childR, required(Dependency.GINJECTOR, foo()));
  }

  public void testResolveDependenciesInChildL_ExposedToRoot() throws Exception {
    StandardTree tree = createExampleTree();
    // Bar is bound in the child, but exposed to the root.  Foo should still be in root
    bind(bar(), tree.childL);
    bindChild(bar(), tree.root, tree.childL);
    bind(baz(), tree.root);
    Binding fooBinding = expectCreateBinding(foo(), required(foo(), bar()), required(foo(), baz()));

    tree.root.addBinding(foo(), fooBinding);
    expectParentBinding(foo(), tree.root, tree.childLL);
        
    replayAndResolve(tree.childLL, required(Dependency.GINJECTOR, foo()));
  }
  
  public void testResolveOneDependencyInChildL() throws Exception {
    StandardTree tree = createExampleTree();
    bind(bar(), tree.root);
    bind(baz(), tree.childL);
    Binding fooBinding = expectCreateBinding(foo(), required(foo(), bar()), required(foo(), baz()));
    
    expectParentBinding(bar(), tree.root, tree.childL); // childL gets Bar from root
    tree.childL.addBinding(foo(), fooBinding);
    expectParentBinding(foo(), tree.childL, tree.childLL); // childLL gets foo from childL
        
    replayAndResolve(tree.childLL, required(Dependency.GINJECTOR, foo()));
  }
  
  public void testResolveOptionalKey_Fails() throws Exception {
    StandardTree tree = createExampleTree();
    expect(bindingCreator.create(foo())).andThrow(new BindingCreationException("Unable to create"));
        
    replayAndResolve(tree.childLL, optional(Dependency.GINJECTOR, foo()));
    // No error because foo() is optional.
  }
  
  public void testResolveBindingWithOptionalDependency_DepFails() throws Exception{
    StandardTree tree = createExampleTree();
    // Baz is optional and fails to resolve
    Binding fooBinding = expectCreateBinding(foo(), required(foo(), bar()), optional(foo(), baz()));
    expect(bindingCreator.create(baz())).andThrow(new BindingCreationException("Unable to create"));
    bind(bar(), tree.root);

    tree.root.addBinding(foo(), fooBinding);
    expectParentBinding(foo(), tree.root, tree.childLL); // childLL gets foo from childL
        
    replayAndResolve(tree.childLL, required(Dependency.GINJECTOR, foo()));
  }
  
  // Tries to create Foo, which has an optional dependency on Bar, which requires Baz.
  // Baz can't be created, so it should create Foo, without the Bar.
  public void testResolveBindingWithOptionalDependencyThatFails() throws Exception {    
    StandardTree tree = createExampleTree();
    
    Binding fooBinding = expectCreateBinding(foo(), optional(foo(), bar()));
    expectCreateBinding(bar(), required(bar(), baz()));
    expect(bindingCreator.create(baz())).andThrow(new BindingCreationException("Unable to create"));
    tree.root.addBinding(foo(), fooBinding);
    expectParentBinding(foo(), tree.root, tree.childLL);
        
    replayAndResolve(tree.childLL, required(Dependency.GINJECTOR, foo()));
  }
  
  public void testResolveBindingWithOptionalThatDoesntBlockPosition() throws Exception {
    StandardTree tree = createExampleTree();
    Binding fooBinding = expectCreateBinding(foo(), optional(foo(), bar()));
    expectCreateBinding(bar(), required(bar(), baz()));
    expectCreateBinding(baz());
    
    // Can't bar() because baz() is already bound in childLL.  Therefore, bar() should not constrain
    // the position of foo(), and we should place it in the root.
    bind(baz(), tree.childLL);
    expect(tree.childL.isBoundInChild(baz())).andReturn(true);

    tree.root.addBinding(foo(), fooBinding);
    expectParentBinding(foo(), tree.root, tree.childL);
    replayAndResolve(tree.childL, required(Dependency.GINJECTOR, foo()));
  }
  
  public void testFailToCreateImplicitBinding() throws Exception {
    StandardTree tree = createExampleTree();
    expect(bindingCreator.create(foo())).andThrow(new BindingCreationException("Unable to create"));
    
    errorManager.logError(EasyMock.contains("" + foo()));
    replayAndResolve(tree.childLL, required(Dependency.GINJECTOR, foo()));
  }
  
  public void testFailToResolveDependency() throws Exception {
    StandardTree tree = createExampleTree();
    expectCreateBinding(foo(), required(foo(), bar()));
    
    expect(bindingCreator.create(bar()))
        .andThrow(new BindingCreationException("Unable to create"));
        
    errorManager.logError(EasyMock.contains("" + bar()));
    replayAndResolve(tree.childLL, required(Dependency.GINJECTOR, foo()));
  }
  
  public void testCircularDependency() throws Exception {
    GinjectorBindings root = createInjectorNode("root");
    
    expectCreateBinding(bar(), required(bar(), baz()));
    expectCreateBinding(baz(), required(baz(), bar()));
    
    Capture<String> errorMessage = new Capture<String>();
    errorManager.logError(EasyMock.isA(String.class));

    // Intentionally use a different key, so that == won't work
    replayAndResolve(root, required(Dependency.GINJECTOR, bar()));
  }
  
  public void testCycleDetectionForBindFooToFooImpl() throws Exception {
    GinjectorBindings root = createInjectorNode("root");
    
    bind(foo(), root);
    expectCreateBinding(fooImpl(), required(fooImpl(), bar()));
    expectCreateBinding(bar(), required(bar(), foo()));
    
    Capture<String> errorMessage = new Capture<String>();
    errorManager.logError(EasyMock.isA(String.class));

    // Intentionally use a different key, so that == won't work
    replayAndResolve(root, required(foo(), fooImpl()));
  }
  
  public void testOneNode() throws Exception {
    GinjectorBindings root = createInjectorNode("root");
    Binding fooBinding = expectCreateBinding(foo(), required(foo(), bar()), required(foo(), baz()));
    bind(bar(), root);
    bind(baz(), root);
    root.addBinding(foo(), fooBinding);
        
    replayAndResolve(root, required(Dependency.GINJECTOR, foo()));
  }
 
  public void testDependencyInOtherChild() throws Exception {
    // Test one of the "weird" behaviors in Guice. Foo depends on Bar and Baz.  Because
    // Bar is bound in a sibling, we can't create Bar in the parent.  Therefore,
    // we create Bar (and Foo) in the origin
    GinjectorBindings root = createInjectorNode("root");
    GinjectorBindings childL = createInjectorNode("childL");
    GinjectorBindings childR = createInjectorNode("childR");
    setChildren(root, childL, childR);
    
    bind(baz(), root);
    bind(bar(), childL);
    expect(root.isBoundInChild(bar())).andReturn(true).anyTimes();
    
    Binding fooBinding = expectCreateBinding(foo(), required(foo(), bar()), required(foo(), baz()));
    Binding barBinding = expectCreateBinding(bar());
    
    childR.addBinding(bar(), barBinding);
    expectParentBinding(baz(), root, childR);
    childR.addBinding(foo(), fooBinding);
        
    replayAndResolve(childR, required(Dependency.GINJECTOR, foo()));
  }
  
  public void testDepHiddenInChildBlocksResolvingInRoot() throws Exception {
    GinjectorBindings root = createInjectorNode("root");
    GinjectorBindings child = createInjectorNode("child");
    setChildren(root, child);
    
    bind(baz(), root);
    bind(bar(), child);
    expect(root.isBoundInChild(bar())).andReturn(true).anyTimes();
    
    expectCreateBinding(foo(), required(foo(), bar()), required(foo(), baz()));
    expectCreateBinding(bar());
    errorManager.logError(isA(String.class)); // failure to create bar b/c already bound
        
    replayAndResolve(root, required(Dependency.GINJECTOR, foo()));
  }
  
  public void testDepHiddenInChildBlocksResolvingInRoot_NoErrorIfOptional() throws Exception {
    GinjectorBindings root = createInjectorNode("root");
    GinjectorBindings child = createInjectorNode("child");
    setChildren(root, child);
    
    bind(baz(), root);
    bind(bar(), child);
    expect(root.isBoundInChild(bar())).andReturn(true).anyTimes();
    
    Binding fooBinding = expectCreateBinding(foo(), required(foo(), baz()), optional(foo(), bar()));
    expectCreateBinding(bar());

    root.addBinding(foo(), fooBinding);
        
    replayAndResolve(root, required(Dependency.GINJECTOR, foo()));
  }
  
  public void testManyChildren() throws Exception {
    GinjectorBindings root = createInjectorNode("root");
    GinjectorBindings child1 = createInjectorNode("child1");
    GinjectorBindings child2 = createInjectorNode("child2");
    GinjectorBindings child3 = createInjectorNode("child3");
    setChildren(root, child1, child2, child3);
    
    bind(bar(), child1);
    bindChild(bar(), root, child1);
    bind(baz(), child2);
    bindChild(baz(), root, child2);
    
    Binding fooBinding = expectCreateBinding(foo(), required(foo(), bar()), required(foo(), baz()));
    root.addBinding(foo(), fooBinding);
    expectParentBinding(foo(), root, child3);
        
    replayAndResolve(child3, required(Dependency.GINJECTOR, foo()));
  }
  
  public void testResolveCycleThroughProvider() throws Exception {
    // Foo -> Bar ->Provider<Foo> -> Foo, cycle is OK because of Provider.  
    // Provider<Foo> is in the "unpositioned pending Foo" set.
    StandardTree tree = createExampleTree();
    
    Binding fooBinding = expectCreateBinding(foo(), required(foo(), bar()));
    Binding barBinding = expectCreateBinding(bar(), required(bar(), providerFoo()));
    Binding providerFooBinding = expectCreateBinding(providerFoo(), 
        requiredLazy(providerFoo(), foo()));
    
    tree.root.addBinding(foo(), fooBinding);
    tree.root.addBinding(bar(), barBinding);
    tree.root.addBinding(providerFoo(), providerFooBinding);
    expectParentBinding(foo(), tree.root, tree.childLL);
        
    replayAndResolve(tree.childLL, required(Dependency.GINJECTOR, foo()));
  }
  
  public void testResolveCycleThroughAsyncProvider() throws Exception {
    // Foo -> AsyncProvider<Foo> -> Foo, cycle is OK because of AsyncProvider.  
    // AsyncProvider<Foo> is in the "unpositioned pending Foo" set.  Identical to
    // testResolveCycleThroughProvider, but verifies that AsyncProvider is also acceptable.
    StandardTree tree = createExampleTree();
    
    Binding fooBinding = expectCreateBinding(foo(), required(foo(), asyncProviderFoo()));
    Binding providerFooBinding = expectCreateBinding(
        asyncProviderFoo(), requiredLazy(asyncProviderFoo(), foo()));
    
    tree.root.addBinding(foo(), fooBinding);
    tree.root.addBinding(asyncProviderFoo(), providerFooBinding);
    expectParentBinding(foo(), tree.root, tree.childLL);
        
    replayAndResolve(tree.childLL, required(Dependency.GINJECTOR, foo()));
  }
  
  public void testResolveCycleDepOfProviderBound() throws Exception {
    // Foo -> Provider<Bar> -> Bar -> {Foo, Baz}, Baz is bound at childL
    // This test makes sure that we at least ensure that Bar doesn't move higher than
    // *any* of it's dependencies, even after detecting a cycle.
    StandardTree tree = createExampleTree();
    
    Binding fooBinding = expectCreateBinding(foo(), required(foo(), providerBar()));
    Binding providerBarBinding = expectCreateBinding(providerBar(), 
        requiredLazy(providerBar(), bar()));
    Binding barBinding = expectCreateBinding(bar(), required(bar(), foo()), required(bar(), baz()));
    bind(baz(), tree.childL);
    
    tree.childL.addBinding(foo(), fooBinding);
    tree.childL.addBinding(providerBar(), providerBarBinding);
    tree.childL.addBinding(bar(), barBinding);
    expectParentBinding(foo(), tree.childL, tree.childLL);
        
    replayAndResolve(tree.childLL, required(Dependency.GINJECTOR, foo()));
  }
  
  public void testResolveCycleDepBeforeProviderBound() throws Exception {
    // Foo -> {Baz, Provider<Bar>}; Provider<Bar> -> Bar -> Foo. Baz is bound at childL
    // Similar to the last one, although this time the already bound dependency comes before
    // earlier in the cycle (Before the provider).  We should still make sure that Foo is
    // in the correct position.
    StandardTree tree = createExampleTree();
    
    Binding fooBinding = expectCreateBinding(foo(), required(foo(), baz()), 
        required(foo(), providerBar()));
    Binding providerBarBinding = expectCreateBinding(providerBar(), 
        requiredLazy(providerBar(), bar()));
    Binding barBinding = expectCreateBinding(bar(), required(bar(), foo()));
    bind(baz(), tree.childL);
    
    tree.childL.addBinding(foo(), fooBinding);
    tree.childL.addBinding(providerBar(), providerBarBinding);
    tree.childL.addBinding(bar(), barBinding);
    expectParentBinding(foo(), tree.childL, tree.childLL);
        
    replayAndResolve(tree.childLL, required(Dependency.GINJECTOR, foo()));
  }
  
  public void testResolveIndirectCycleThroughProvider() throws Exception {
    // Foo -> Baz -> Provider<Bar> -> Bar -> Foo
    StandardTree tree = createExampleTree();
    
    Binding fooBinding = expectCreateBinding(foo(), required(foo(), baz()));
    Binding bazBinding = expectCreateBinding(baz(), required(baz(), providerBar()));
    Binding providerBarBinding = expectCreateBinding(
        providerBar(), requiredLazy(providerBar(), bar()));
    Binding barBinding = expectCreateBinding(bar(), required(bar(), foo()));
    
    tree.root.addBinding(foo(), fooBinding);
    tree.root.addBinding(providerBar(), providerBarBinding);
    tree.root.addBinding(bar(), barBinding);
    tree.root.addBinding(baz(), bazBinding);
    expectParentBinding(foo(), tree.root, tree.childLL);
        
    replayAndResolve(tree.childLL, required(Dependency.GINJECTOR, foo()));
  }
  
  public void testResolveMultipleCycles() throws Exception {
    // Foo -> Bar -> Provider<Baz> -> Baz -> {Provider<Foo>, Provider<Bar>, Provider<Baz>}
    // Resolves multiple cycles, including a cycle that starts with a Provider
    StandardTree tree = createExampleTree();
    
    Binding fooBinding = expectCreateBinding(foo(), required(foo(), bar()));
    Binding barBinding = expectCreateBinding(bar(), required(bar(), providerBaz()));
    Binding providerBazBinding = expectCreateBinding(
        providerBaz(), requiredLazy(providerBaz(), baz()));
    Binding bazBinding = expectCreateBinding(baz(), required(baz(), providerFoo()), 
        required(baz(), providerFoo()), required(baz(), providerBar()));
    Binding providerFooBinding = expectCreateBinding(
        providerFoo(), requiredLazy(providerFoo(), foo()));
    Binding providerBarBinding = expectCreateBinding(
        providerBar(), requiredLazy(providerBar(), bar()));

    tree.root.addBinding(foo(), fooBinding);
    tree.root.addBinding(providerBar(), providerBarBinding);
    tree.root.addBinding(providerFoo(), providerFooBinding);
    tree.root.addBinding(providerBaz(), providerBazBinding);
    tree.root.addBinding(bar(), barBinding);
    tree.root.addBinding(baz(), bazBinding);
    expectParentBinding(foo(), tree.root, tree.childLL);
        
    replayAndResolve(tree.childLL, required(Dependency.GINJECTOR, foo()));
  }
    
  private Dependency required(Key<?> source, Key<?> key) {
    return new Dependency(source, key, SOURCE);
  }
  
  private Dependency optional(Key<?> source, Key<?> key) {
    return new Dependency(source, key, true, false, SOURCE);
  }
  
  private Dependency requiredLazy(Key<?> source, Key<?> key) {
    return new Dependency(source, key, false, true, SOURCE);
  }
  
  private Binding expectCreateBinding(Key<?> key, Dependency... keys) throws Exception {
    Binding binding = control.createMock(Binding.class);
    expect(bindingCreator.create(key)).andReturn(binding);
    Set<Dependency> requiredKeys = new HashSet<Dependency>(keys.length);
    Collections.addAll(requiredKeys, keys);
    expect(binding.getDependencies()).andReturn(requiredKeys).atLeastOnce();
    bindings.add(binding);
    return binding;
  }
}
