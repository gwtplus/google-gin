package com.google.gwt.inject.rebind;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.classextension.EasyMock.createControl;
import static org.easymock.classextension.EasyMock.createNiceMock;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.BindingContext;
import com.google.gwt.inject.rebind.binding.ExposedChildBinding;
import com.google.gwt.inject.rebind.binding.ParentBinding;
import com.google.gwt.inject.rebind.binding.RequiredKeys;
import com.google.inject.Key;
import com.google.inject.util.Providers;

import junit.framework.TestCase;

import org.easymock.Capture;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BindingResolverTest extends TestCase {

  private static final BindingContext CONTEXT = BindingContext.forText("test");
  private static final Key<Foo> FOO = Key.get(Foo.class);
  private static final Key<Bar> BAR = Key.get(Bar.class);
  private static final Key<Baz> BAZ = Key.get(Baz.class);
  
  // List to record binding collections that we mock so that they don't get GC'd and have
  // false failures due to finalize being called
  private List<GinjectorBindings> nodes = new ArrayList<GinjectorBindings>();

  private TreeLogger logger;
  private ImplicitBindingCreator bindingCreator;
  private Binding binding;
  private ParentBinding parentBinding;
  private ErrorManager errorManager;
  private IMocksControl control;
  
  private static class Foo {}
  private static class Bar {}
  private static class Baz {}
  
  private void replay() {
    EasyMock.replay();
    control.replay();
  }
  
  private void verify() {
    EasyMock.verify();
    control.verify();
  }
  
  protected void setUp() throws Exception {
    super.setUp();
    
    logger = createNiceMock("logger", TreeLogger.class);
    
    control = createControl();
    
    bindingCreator = control.createMock("bindingCreator", ImplicitBindingCreator.class);
    binding = control.createMock("binding", Binding.class);
    parentBinding = control.createMock("parentBinding", ParentBinding.class);
    errorManager = control.createMock("errorManager", ErrorManager.class);
  }
  
  private GinjectorBindings createNode(String name) {
    GinjectorBindings node = control.createMock(name, GinjectorBindings.class);
    nodes.add(node);
    expect(node.getParent()).andStubReturn(null);
    expect(node.getChildren()).andStubReturn(new ArrayList<GinjectorBindings>());
    expect(node.getBinding(isA(Key.class))).andStubReturn(null);
    expect(node.isBound(isA(Key.class))).andStubReturn(false);
    expect(node.isBoundInChild(isA(Key.class))).andStubReturn(false);
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
    tree.root = createNode("root");
    tree.childL = createNode("childL");
    tree.childR = createNode("childR");
    tree.childLL = createNode("childLL");
    tree.childLR = createNode("childLR");
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
  
  private Binding bind(Key<?> key, GinjectorBindings in) {
    Binding binding = control.createMock("bindingFor" + key.getClass().getSimpleName() + "In" + in,
        Binding.class);
    BindingEntry entry = new BindingEntry(binding, 
        BindingContext.forText("Binding for " + key));
    expect(in.getBinding(key)).andReturn(entry).anyTimes();
    expect(in.isBound(key)).andReturn(true).anyTimes();
    return binding;
  }
  
  private ExposedChildBinding bindChild(Key<?> key, GinjectorBindings parent) {
    ExposedChildBinding binding = control.createMock(
        "exposedBindingFor" + key.getClass().getSimpleName() + "FromChild", 
        ExposedChildBinding.class);
    BindingEntry entry = new BindingEntry(binding, CONTEXT);
    expect(parent.getBinding(key)).andReturn(entry).anyTimes();
    expect(parent.isBound(key)).andReturn(true).anyTimes();
    return binding;
  }
  
  private ParentBinding bindParent(Key<?> key, GinjectorBindings parent, GinjectorBindings child) {
    ParentBinding binding = control.createMock(
        "exposedBindingFor" + key.getClass().getSimpleName() + "FromParent" + parent,
        ParentBinding.class);
    BindingEntry entry = new BindingEntry(binding, CONTEXT);
    expect(child.getBinding(key)).andReturn(entry).anyTimes();
    expect(binding.getParentBindings()).andReturn(parent).anyTimes();
    expect(child.isBound(key)).andReturn(true).anyTimes();
    return binding;
  }
  
  private RequiredKeys requiredKeys(boolean barOptional, boolean bazOptional) {
    Set<Key<?>> required = new HashSet<Key<?>>();
    Set<Key<?>> requiredOptional = new HashSet<Key<?>>();
    (barOptional ? requiredOptional : required).add(Key.get(Bar.class));
    (bazOptional ? requiredOptional : required).add(Key.get(Baz.class));
    return new RequiredKeys(required, requiredOptional);
  }
  
  private void expectParentBinding(Key<?> key, GinjectorBindings parent, GinjectorBindings dest) {
    parentBinding.setKey(key);
    parentBinding.setParent(parent);
    // TODO(bchambers): Refactor to allow verifying what binding is added.
    dest.addBinding(eq(key), isA(BindingEntry.class));
  }
  
  
  private GinjectorBindings resolve(GinjectorBindings origin, Key<?> key, boolean optional) {
    BindingResolver resolver = new BindingResolver(logger, bindingCreator, 
        Providers.of(parentBinding), origin, errorManager);
    return resolver.resolveAndInherit(key, optional, CONTEXT);
  }
  
  public void testResolveFoundInRoot() {
    StandardTree tree = createExampleTree();
    Binding fooBinding = bind(FOO, tree.root);
    expectParentBinding(FOO, tree.root, tree.childLL);
    replay();
    
    assertSame(tree.root, resolve(tree.childLL, FOO, false));
    verify();
  }
  
  public void testResolveDependenciesInRoot() {
    StandardTree tree = createExampleTree();
    expect(binding.getRequiredKeys()).andReturn(requiredKeys(false, false)).atLeastOnce();
    expect(bindingCreator.create(Key.get(Foo.class), false)).andReturn(binding);
    Binding barBinding = bind(BAR, tree.root);
    Binding bazBinding = bind(BAZ, tree.root);
    tree.root.addBinding(eq(FOO), isA(BindingEntry.class));
    expectParentBinding(FOO, tree.root, tree.childLL);
    replay();
    
    assertSame(tree.root, resolve(tree.childLL, FOO, false));
    verify();
  }
  
  public void testResolveDependenciesInRoot_InheritedByChild() {
    StandardTree tree = createExampleTree();
    expect(binding.getRequiredKeys()).andReturn(requiredKeys(false, false)).atLeastOnce();
    expect(bindingCreator.create(FOO, false)).andReturn(binding);
    Binding barBinding = bind(BAR, tree.root);
    
    // Bar bound in root, inherited in childL.  Baz bound in root.  Foo can still be in root.
    ParentBinding binding = bindParent(BAR, tree.root, tree.childL);
    Binding bazBinding = bind(BAZ, tree.root);
    tree.root.addBinding(eq(FOO), isA(BindingEntry.class));
    expectParentBinding(FOO, tree.root, tree.childLL);
    replay();
    
    assertSame(tree.root, resolve(tree.childLL, FOO, false));
    verify();
  }
  
  public void testResolveDependenciesInChildL_ExposedToRoot() {
    StandardTree tree = createExampleTree();
    expect(binding.getRequiredKeys()).andReturn(requiredKeys(false, false)).atLeastOnce();
    expect(bindingCreator.create(FOO, false)).andReturn(binding);
    // Bar is bound in the child, but exposed to the root.  Foo should still be in root
    Binding barBinding = bind(BAR, tree.childL);
    ExposedChildBinding barExposedToRoot = bindChild(BAR, tree.root);
    Binding bazBinding = bind(BAZ, tree.root);
    tree.root.addBinding(eq(FOO), isA(BindingEntry.class));
    expectParentBinding(FOO, tree.root, tree.childLL);
    replay();
    
    assertSame(tree.root, resolve(tree.childLL, FOO, false));
    verify();
  }
  
  public void testResolveOneDependencyInChildL() {
    StandardTree tree = createExampleTree();
    expect(binding.getRequiredKeys()).andReturn(requiredKeys(false, false)).atLeastOnce();
    expect(bindingCreator.create(FOO, false)).andReturn(binding);
    Binding barBinding = bind(BAR, tree.root);
    Binding bazBinding = bind(BAZ, tree.childL);
    
    expectParentBinding(BAR, tree.root, tree.childL); // childL gets Bar from root
    tree.childL.addBinding(eq(FOO), isA(BindingEntry.class)); // childL binds foo
    expectParentBinding(FOO, tree.childL, tree.childLL); // childLL gets foo from childL
    replay();
    
    assertSame(tree.childL, resolve(tree.childLL, FOO, false));
    verify();
  }
  
  public void testResolveOptionalKey_Fails() {
    StandardTree tree = createExampleTree();
    expect(bindingCreator.create(FOO, true)).andReturn(null);
    replay();
    
    assertNull(resolve(tree.childLL, FOO, true));
    // No error because FOO is optional.
    verify();
  }
  
  public void testResolveBindingWithOptionalDependency_DepFails() {
    StandardTree tree = createExampleTree();
    // Baz is optional and fails to resolve
    expect(binding.getRequiredKeys()).andReturn(requiredKeys(false, true)).atLeastOnce();
    expect(bindingCreator.create(FOO, false)).andReturn(binding);
    expect(bindingCreator.create(BAZ, true)).andReturn(null);
    Binding barBinding = bind(BAR, tree.root);
    
    tree.root.addBinding(eq(Key.get(Foo.class)), isA(BindingEntry.class));
    expectParentBinding(FOO, tree.root, tree.childLL); // childLL gets foo from childL
    replay();
    
    assertSame(tree.root, resolve(tree.childLL, FOO, false));
    verify();
  }
  
  // Tries to create Foo, which has an optional dependency on Bar, which requires Baz.
  // Baz can't be created, so it should create Foo, without the Bar.
  public void testResolveBindingWithOptionalDependencyThatFails() {
    Binding barBinding = control.createMock(Binding.class);
    Set<Key<?>> emptySet = new HashSet<Key<?>>();
    Set<Key<?>> fooOptional = new HashSet<Key<?>>();
    fooOptional.add(BAR);
    Set<Key<?>> barRequired = new HashSet<Key<?>>();
    barRequired.add(BAZ);
    
    StandardTree tree = createExampleTree();
    
    expect(bindingCreator.create(FOO, false)).andReturn(binding);
    expect(binding.getRequiredKeys()).andReturn(new RequiredKeys(emptySet, fooOptional));
    expect(bindingCreator.create(BAR, true)).andReturn(barBinding);
    expect(barBinding.getRequiredKeys()).andReturn(new RequiredKeys(barRequired));
    expect(bindingCreator.create(BAZ, true)).andReturn(null);
    tree.root.addBinding(eq(FOO), isA(BindingEntry.class));
    expectParentBinding(FOO, tree.root, tree.childLL);
    replay();
    
    assertSame(tree.root, resolve(tree.childLL, FOO, false));
    verify();
  }
  
  public void testFailToCreateImplicitBinding() {
    StandardTree tree = createExampleTree();
    expect(bindingCreator.create(FOO, false)).andReturn(null);
    
    // Error: Unable to create required binding
    errorManager.logError(isA(String.class));
    replay();
    
    assertNull(resolve(tree.childLL, FOO, false));
    verify();
  }
  
  public void testFailToResolveDependency() {
    StandardTree tree = createExampleTree();
    expect(binding.getRequiredKeys()).andReturn(requiredKeys(false, false)).atLeastOnce();
    expect(bindingCreator.create(FOO, false)).andReturn(binding);
    
    // The dependencies Bar and Baz are in a set, so we don't know which one will happen
    // first and cause Foo to fail.  Allow either:
    expect(bindingCreator.create(Key.get(Bar.class), false)).andReturn(null).times(0, 1);
    expect(bindingCreator.create(Key.get(Baz.class), false)).andReturn(null).times(0, 1);
    
    errorManager.logError(isA(String.class)); // Unable to create binding for dependency
    errorManager.logError(isA(String.class)); // Unable to create binding for foo
    replay();
    
    assertNull(resolve(tree.childLL, FOO, false));
    verify();
  }
  
  public void testCircularDependency() {
    GinjectorBindings root = createNode("root");
    
    Set<Key<?>> dependencyOnBaz = new HashSet<Key<?>>();
    dependencyOnBaz.add(BAZ);
    Set<Key<?>> dependencyOnBar = new HashSet<Key<?>>();
    dependencyOnBar.add(BAR);
    
    Binding bar = control.createMock(Binding.class);
    expect(bindingCreator.create(BAR, false)).andReturn(bar);
    expect(bar.getRequiredKeys()).andReturn(new RequiredKeys(dependencyOnBaz));
    Binding baz = control.createMock(Binding.class);
    expect(bindingCreator.create(BAZ, false)).andReturn(baz);
    expect(baz.getRequiredKeys()).andReturn(new RequiredKeys(dependencyOnBar));
    Capture<String> errorMessage = new Capture<String>();
    errorManager.logError(EasyMock.endsWith(String.format("%s -> %s -> %s", BAR, BAZ, BAR)));
    errorManager.logError(isA(String.class)); // fail to create Baz
    errorManager.logError(isA(String.class)); // fail to create Bar
    replay();
    
    assertNull(resolve(root, BAR, false));
    verify();
  }
  
  public void testOneNode() {
    GinjectorBindings root = createNode("root");
    expect(binding.getRequiredKeys()).andReturn(requiredKeys(false, false)).atLeastOnce();
    expect(bindingCreator.create(FOO, false)).andReturn(binding);
    Binding bar = bind(BAR, root);
    Binding baz = bind(BAZ, root);
    root.addBinding(eq(FOO), isA(BindingEntry.class));
    replay();
    
    assertSame(root, resolve(root, FOO, false));
    verify();
  }
 
  public void testDependencyInOtherChild() {
    // Test one of the "weird" behaviors in Guice. Foo depends on Bar and Baz.  Because
    // Bar is bound in a sibling, we can't create Bar in the parent.  Therefore,
    // we create Bar (and Foo) in the origin
    GinjectorBindings root = createNode("root");
    GinjectorBindings childL = createNode("childL");
    GinjectorBindings childR = createNode("childR");
    setChildren(root, childL, childR);
    
    Binding baz = bind(BAZ, root);
    Binding barInChildL = bind(BAR, childL);
    expect(root.isBoundInChild(BAR)).andReturn(true).anyTimes();
    Binding barInChildR = control.createMock(Binding.class);

    expect(bindingCreator.create(FOO, false)).andReturn(binding);
    expect(binding.getRequiredKeys()).andReturn(requiredKeys(false, false)).atLeastOnce();
    expect(bindingCreator.create(BAR, false)).andReturn(barInChildR);
    expect(barInChildR.getRequiredKeys()).andReturn(new RequiredKeys(new HashSet<Key<?>>()));
    
    childR.addBinding(eq(BAR), isA(BindingEntry.class));
    expectParentBinding(BAZ, root, childR);
    childR.addBinding(eq(FOO), isA(BindingEntry.class));
    replay();
    
    assertSame(childR, resolve(childR, FOO, false));
    verify();
  }
  
  public void testDepHiddenInChildBlocksResolvingInRoot() {
    GinjectorBindings root = createNode("root");
    GinjectorBindings child = createNode("child");
    setChildren(root, child);
    
    Binding baz = bind(BAZ, root);
    Binding barInChild = bind(BAR, child);
    expect(root.isBoundInChild(BAR)).andReturn(true).anyTimes();
    Binding barInParent = control.createMock(Binding.class);

    expect(bindingCreator.create(FOO, false)).andReturn(binding);
    expect(binding.getRequiredKeys()).andReturn(requiredKeys(false, false)).atLeastOnce();
    
    errorManager.logError(isA(String.class)); // failure to create bar b/c already bound
    errorManager.logError(isA(String.class)); // failure to create foo
    replay();
    
    assertNull(resolve(root, FOO, false));
    verify();
  }
  
  public void testDepHiddenInChildBlocksResolvingInRoot_NoErrorIfOptional() {
    GinjectorBindings root = createNode("root");
    GinjectorBindings child = createNode("child");
    setChildren(root, child);
    
    Binding baz = bind(BAZ, root);
    Binding barInChild = bind(BAR, child);
    expect(root.isBoundInChild(BAR)).andReturn(true).anyTimes();
    Binding barInParent = control.createMock(Binding.class);

    expect(bindingCreator.create(FOO, false)).andReturn(binding);
    expect(binding.getRequiredKeys()).andReturn(requiredKeys(true, false)).atLeastOnce();
    root.addBinding(eq(FOO), isA(BindingEntry.class));
    replay();
    
    assertSame(root, resolve(root, FOO, false));
    verify();
  }
  
  public void testManyChildren() {
    GinjectorBindings root = createNode("root");
    GinjectorBindings child1 = createNode("child1");
    GinjectorBindings child2 = createNode("child2");
    GinjectorBindings child3 = createNode("child3");
    setChildren(root, child1, child2, child3);
    
    Binding bar = bind(BAR, child1);
    ExposedChildBinding barParent = bindChild(BAR, root);
    Binding baz = bind(BAZ, child2);
    ExposedChildBinding bazParent = bindChild(BAZ, root);
    root.addBinding(eq(FOO), isA(BindingEntry.class));
    expectParentBinding(FOO, root, child3);
    
    expect(binding.getRequiredKeys()).andReturn(requiredKeys(false, false)).atLeastOnce();
    expect(bindingCreator.create(FOO, false)).andReturn(binding);
    replay();
    
    assertSame(root, resolve(child3, FOO, false));
    verify();
  }
}
