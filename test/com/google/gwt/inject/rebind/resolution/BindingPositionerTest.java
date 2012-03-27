package com.google.gwt.inject.rebind.resolution;

import static com.google.gwt.inject.rebind.resolution.TestUtils.bar;
import static com.google.gwt.inject.rebind.resolution.TestUtils.baz;
import static com.google.gwt.inject.rebind.resolution.TestUtils.foo;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.util.Preconditions;
import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.Context;
import com.google.gwt.inject.rebind.binding.Dependency;
import com.google.gwt.inject.rebind.binding.ExposedChildBinding;
import com.google.gwt.inject.rebind.resolution.DependencyExplorer.DependencyExplorerOutput;
import com.google.inject.Key;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tests for {@link BindingPositioner}.
 */
public class BindingPositionerTest extends TestCase {

  private static final String SOURCE = "dummy";

  private IMocksControl control;
  
  private GinjectorBindings root;
  private GinjectorBindings child;
  private GinjectorBindings grandchild;
  private GinjectorBindings othergrandchild; // Only used as the source of an ExposedChildBinding.

  private final TreeLogger treeLogger = TreeLogger.NULL;
    
  @Override
  protected void setUp() throws Exception {
    control = EasyMock.createControl();
        
    root = control.createMock("root", GinjectorBindings.class);
    child = control.createMock("child", GinjectorBindings.class);
    grandchild = control.createMock("grandchild", GinjectorBindings.class);
    othergrandchild = control.createMock("other", GinjectorBindings.class);

    expect(grandchild.getParent()).andStubReturn(child);
    expect(child.getParent()).andStubReturn(root);
    expect(root.getParent()).andStubReturn(null);
    expect(root.isBoundLocallyInChild(isA(Key.class))).andStubReturn(false);
    expect(child.isBoundLocallyInChild(isA(Key.class))).andStubReturn(false);
    expect(grandchild.isBoundLocallyInChild(isA(Key.class))).andStubReturn(false);
    expect(root.getBinding(isA(Key.class))).andStubReturn(null);
    expect(child.getBinding(isA(Key.class))).andStubReturn(null);
    expect(grandchild.getBinding(isA(Key.class))).andStubReturn(null);
    expect(root.isPinned(isA(Key.class))).andStubReturn(false);
    expect(child.isPinned(isA(Key.class))).andStubReturn(false);
    expect(grandchild.isPinned(isA(Key.class))).andStubReturn(false);
  }
  
  public void testNoDependencies() throws Exception {
    new PositionerExpectationsBuilder(child).test();
  }
  
  public void testAlreadyPositioned() throws Exception {
    // Verifies that a single, already positioned node stays where it is
    new PositionerExpectationsBuilder(child)
        .keysBoundAt(child, foo())
        .test();
  }
  
  private PositionerExpectationsBuilder testTree() {
    return new PositionerExpectationsBuilder(grandchild)
        .addEdge(new Dependency(foo(), bar(), SOURCE))
        .addEdge(new Dependency(foo(), baz(), SOURCE));
  }
  
  public void testPositionTree() throws Exception {
    // Foo (which depends on Bar and Baz) ends up "no-higher than the lowest" of bar and baz.
    testTree()
        .implicitlyBoundAt(child, foo())
        .keysBoundAt(child, bar())
        .keysBoundAt(root, baz())
        .test();
  }
  
  public void testPositionTree_BoundInChild() throws Exception {
    // Bar can't be placed at root (already bound in child), so it and foo get placed in child. 
    expect(root.isBoundLocallyInChild(bar())).andReturn(true).anyTimes(); // bar() must be in child
    testTree()
        .implicitlyBoundAt(child, foo(), bar())
        .implicitlyBoundAt(root, baz())
        .test();
  }
  
  private PositionerExpectationsBuilder testChain() {
    return new PositionerExpectationsBuilder(grandchild)
        .addEdge(new Dependency(foo(), bar(), SOURCE))
        .addEdge(new Dependency(bar(), baz(), SOURCE));
  }
  
  public void testPositionChain() throws Exception {
    testChain()
        .implicitlyBoundAt(root, foo(), bar(), baz())
        .test();
  }

  public void testPositionChain_FooBoundInSibling() throws Exception {
    expect(root.isBoundLocallyInChild(foo())).andReturn(true).anyTimes();
    expect(child.isBoundLocallyInChild(foo())).andReturn(true).anyTimes();
    testChain()
        .implicitlyBoundAt(root, bar(), baz())
        .implicitlyBoundAt(grandchild, foo())
        .test();
  }
  
  public void testPositionChain_BazBoundInRoot() throws Exception {
    testChain()
        .implicitlyBoundAt(child, foo(), bar())
        .keysBoundAt(child, baz())
        .test();
  }
  
  private PositionerExpectationsBuilder testCycle() {
    return new PositionerExpectationsBuilder(grandchild)
        .addEdge(new Dependency(foo(), bar(), SOURCE))
        .addEdge(new Dependency(bar(), baz(), SOURCE))
        .addEdge(new Dependency(baz(), foo(), SOURCE));
  }
  
  public void testPositionCycle() throws Exception {
    testCycle()
        .implicitlyBoundAt(root, foo(), bar(), baz())
        .test();
  }
  
  public void testPositionCycle_BarBoundInSibling() throws Exception {
    // Cycle through foo -> bar -> baz, and bar must be placed in child, so everything is placed
    // in child.
    expect(root.isBoundLocallyInChild(bar())).andReturn(true).anyTimes(); // bar() must be in child
    testCycle()
        .implicitlyBoundAt(child, bar(), baz(), foo())
        .test();
  }
  
  public void testPositionCycle_BazBoundInChild() throws Exception {
    testCycle()
        .keysBoundAt(child, baz())
        .implicitlyBoundAt(child, bar(), foo())
        .test();
  }
  
  public void testPositionCycle_BarAndBazBoundInSibling() throws Exception {
    expect(root.isBoundLocallyInChild(bar())).andReturn(true).anyTimes();
    expect(root.isBoundLocallyInChild(baz())).andReturn(true).anyTimes();
    expect(child.isBoundLocallyInChild(baz())).andReturn(true).anyTimes();
    testCycle()
        .implicitlyBoundAt(grandchild, foo(), bar(), baz())
        .test();
  }
  
  public void testPositionCycle_OutsideDep() throws Exception {
    testCycle()
        .addEdge(new Dependency(bar(), Key.get(A.class), SOURCE))
        .keysBoundAt(child, Key.get(A.class))
        .implicitlyBoundAt(child, foo(), bar(), baz())
        .test();
  }
  
  public void testPositionPinned_noBindingInParent() throws Exception {
    // Bar is bound (and pinned) at grandchild, but because it is not exposed to
    // the root, foo should be created in grandchild.
    testChain()
        .addEdge(new Dependency(foo(), bar(), SOURCE))
        .pinnedAt(grandchild, bar())
        .implicitlyBoundAt(grandchild, bar())
        .implicitlyBoundAt(grandchild, foo())
        .test();
  }

  public void testPositionPinned_exposedBindingInParent() throws Exception {
    // Bar is bound (and pinned) at grandchild, but because it is exposed to the
    // child, foo should be created up there.
    testChain()
        .addEdge(new Dependency(foo(), bar(), SOURCE))
        .pinnedAt(grandchild, bar())
        .exposed(grandchild, child, bar())
        .implicitlyBoundAt(grandchild, bar())
        .implicitlyBoundAt(child, foo())
        .test();
  }

  public void testPositionPinned_exposedBindingInParent_fromOtherChild() throws Exception {
    // Bar is bound (and pinned) at grandchild.  It is exposed to the parent
    // from a different grandchild, and the positioner should throw an exception
    // in this case.
    //
    // (this should be an error in other parts of the code, but check that this
    // module behaves in a well-defined way)
    testChain()
        .addEdge(new Dependency(foo(), bar(), SOURCE))
        .pinnedAt(grandchild, bar())
        .exposed(othergrandchild, child, bar())
        .shouldThrow(Exception.class)
        .implicitlyBoundAt(grandchild, bar())
        .implicitlyBoundAt(grandchild, foo())
        .test();
  }

  public void testPositionPinned_bindingInParent_notExposedBinding() throws Exception {
    // Bar is bound (and pinned) at grandchild.  It is available in the parent
    // with a different binding, and the positioner should throw an exception in
    // this case.
    //
    // (this should be an error in other parts of the code, but check that this
    // module behaves in a well-defined way)
    testChain()
        .addEdge(new Dependency(foo(), bar(), SOURCE))
        .pinnedAt(grandchild, bar())
        .notExposedBinding(child, bar())
        .shouldThrow(Exception.class)
        .implicitlyBoundAt(grandchild, bar())
        .implicitlyBoundAt(grandchild, foo())
        .test();
  }

  public void testPositionPinned_exposedBindingInParentAndGrandparent() throws Exception {
    // Bar is bound (and pinned) at grandchild, but because it is exposed to the
    // child and the root, foo should be created up there.
    testChain()
        .addEdge(new Dependency(foo(), bar(), SOURCE))
        .pinnedAt(grandchild, bar())
        .exposed(grandchild, child, bar())
        .exposed(child, root, bar())
        .implicitlyBoundAt(grandchild, bar())
        .implicitlyBoundAt(root, foo())
        .test();
  }
  
  private static class A {}
  private static class B {}
  private static class C {}
  private static class D {}
  
  public void testTwoCycles() throws Exception {
    testCycle()
        .addEdge(new Dependency(bar(), Key.get(A.class), SOURCE))
        .addEdge(new Dependency(Key.get(A.class), Key.get(B.class), SOURCE))
        .addEdge(new Dependency(Key.get(B.class), Key.get(C.class), SOURCE))
        .addEdge(new Dependency(Key.get(B.class), Key.get(D.class), SOURCE))
        .addEdge(new Dependency(Key.get(C.class), Key.get(A.class), SOURCE))
        .keysBoundAt(child, Key.get(D.class))
        .implicitlyBoundAt(child, foo(), bar(), baz(), 
            Key.get(A.class), Key.get(B.class), Key.get(C.class))
        .test();
  }
  
  /**
   * Builder for constructing the expectations used above in a more readable manner.
   */
  private class PositionerExpectationsBuilder {
    
    private final DependencyGraph.Builder graphBuilder;
    private final Map<Key<?>, GinjectorBindings> implicitlyBoundKeys =
        new HashMap<Key<?>, GinjectorBindings>();
    private final Map<Key<?>, GinjectorBindings> preExistingLocations = 
        new HashMap<Key<?>, GinjectorBindings>();
    private final Map<GinjectorBindings, Map<Key<?>, GinjectorBindings>> exposedTo =
        new HashMap<GinjectorBindings, Map<Key<?>, GinjectorBindings>>();
    private final Map<GinjectorBindings, Set<Key<?>>> notExposedBinding =
        new HashMap<GinjectorBindings, Set<Key<?>>>();
    private Class<?> thrownType = null;
    
    public PositionerExpectationsBuilder(GinjectorBindings origin) {
      graphBuilder = new DependencyGraph.Builder(origin);
    }
    
    public PositionerExpectationsBuilder addEdge(Dependency dependency) {
      graphBuilder.addEdge(dependency);
      return this;
    }
    
    public PositionerExpectationsBuilder implicitlyBoundAt(
        GinjectorBindings expected, Key<?>... keys) {
      for (Key<?> key : keys) {
        Preconditions.checkState(!preExistingLocations.containsKey(key),
            "Key %s cannot be implicitly bound -- already bound in ginjector %s!", key, 
            preExistingLocations.get(key));
        implicitlyBoundKeys.put(key, expected);
      }
      return this;
    }

    public PositionerExpectationsBuilder exposed(
        GinjectorBindings child, GinjectorBindings parent, Key<?>... keys) {

      for (Key<?> key : keys) {
        Preconditions.checkState(
            !(notExposedBinding.containsKey(parent) && notExposedBinding.get(parent).contains(key)),
            "Key %s cannot be exposed: it has a not-exposed binding in %s!", key, parent);
      }

      Map<Key<?>, GinjectorBindings> keyToChildMap = exposedTo.get(parent);
      if (keyToChildMap == null) {
        keyToChildMap = new HashMap<Key<?>, GinjectorBindings>();
        exposedTo.put(parent, keyToChildMap);
      }

      for (Key<?> key : keys) {
        keyToChildMap.put(key, child);
      }
      return this;
    }

    public PositionerExpectationsBuilder notExposedBinding(
        GinjectorBindings parent, Key<?>... keys) {

      Set<Key<?>> keySet = notExposedBinding.get(parent);
      if (keySet == null) {
        keySet = new HashSet<Key<?>>();
        notExposedBinding.put(parent, keySet);
      }

      for (Key<?> key : keys) {
        Preconditions.checkState(
            !(exposedTo.containsKey(parent) && exposedTo.get(parent).containsKey(key)),
            "Key %s cannot have a not-exposed binding: it already is exposed to %s!", key, parent);

        keySet.add(key);
      }
      return this;
    }

    public PositionerExpectationsBuilder keysBoundAt(GinjectorBindings ginjector, Key<?>... keys) {
      for (Key<?> key : keys) {
        Preconditions.checkState(!implicitlyBoundKeys.containsKey(key),
            "Key %s cannot be bound at %s -- already in implicitly bound set!", key, ginjector);
        preExistingLocations.put(key, ginjector);
      }
      return this;
    }
    
    public PositionerExpectationsBuilder pinnedAt(GinjectorBindings ginjector, Key<?>... keys) {
      for (Key<?> key : keys) {
        expect(ginjector.isPinned(key)).andReturn(true).anyTimes();
      }
      return this;
    }

    public PositionerExpectationsBuilder shouldThrow(Class<?> thrownType) {
      this.thrownType = thrownType;
      return this;
    }
    
    public void test() {
      DependencyExplorerOutput output = control.createMock(DependencyExplorerOutput.class);
      expect(output.getGraph()).andStubReturn(graphBuilder.build());
      expect(output.getImplicitlyBoundKeys()).andStubReturn(implicitlyBoundKeys.keySet());
      expect(output.getPreExistingLocations()).andStubReturn(preExistingLocations);
      expectExposedBindingsExist();
      expectNotExposedBindingsExist();
      control.replay();
      BindingPositioner positioner = new BindingPositioner(treeLogger);

      RuntimeException actuallyThrownException = null;
      try {
        positioner.position(output);
      } catch (RuntimeException exception) {
        actuallyThrownException = exception;
      }

      // If we expected an exception, make sure it happened.  Otherwise, verify the results.
      if (thrownType != null) {
        // Distinguish the "wrong type" vs "nothing at all" cases to get better
        // error messages.
        if (actuallyThrownException == null) {
          fail("Expected " + thrownType);
        } else if (!thrownType.isInstance(actuallyThrownException)) {
          // The positioner failed in an unexpected way -- let the user see the
          // stack trace.
          throw new RuntimeException("Wrong exception type (expected " + thrownType + ")",
              actuallyThrownException);
        }
      } else {
        if (actuallyThrownException != null) {
          throw new RuntimeException("Unexpected exception", actuallyThrownException);
        }

        // Check that already positioned things didn't move
        for (Map.Entry<Key<?>, GinjectorBindings> entry : preExistingLocations.entrySet()) {
          assertSame(String.format("Expected already-bound %s to remain in location %s, but was %s", 
                  entry.getKey(), entry.getValue(), positioner.getInstallPosition(entry.getKey())),
              entry.getValue(), positioner.getInstallPosition(entry.getKey()));
        }

        // Check that implicitly bound keys ended up where we expect
        for (Map.Entry<Key<?>, GinjectorBindings> entry : implicitlyBoundKeys.entrySet()) {
          assertSame(String.format("Expected %s to be placed at %s, but was %s",
              entry.getKey(), entry.getValue(), positioner.getInstallPosition(entry.getKey())),
          entry.getValue(), positioner.getInstallPosition(entry.getKey()));
        }
      }

      control.verify();
    }

    /**
     * For each binding exposed from a child, expect it to be represented
     * in the parent by an {@link ExposedChildBinding}.
     */
    private void expectExposedBindingsExist() {
      for (Map.Entry<GinjectorBindings, Map<Key<?>, GinjectorBindings>> entry :
          exposedTo.entrySet()) {
        GinjectorBindings parent = entry.getKey();
        Map<Key<?>, GinjectorBindings> keyToChildMap = entry.getValue();

        for (Map.Entry<Key<?>, GinjectorBindings> keyAndChild : keyToChildMap.entrySet()) {
          Key<?> key = keyAndChild.getKey();
          GinjectorBindings child = keyAndChild.getValue();

          expect(parent.getBinding(key))
              .andReturn(new ExposedChildBinding(Key.get(Long.class), child, Context.forText("")))
              .anyTimes();
        }
      }
    }

    /**
     * For each non-exposed binding, expect it to return an arbitrary binding
     * implementation that is never accessed.
     */
    private void expectNotExposedBindingsExist() {
      for (Map.Entry<GinjectorBindings, Set<Key<?>>> entry : notExposedBinding.entrySet()) {
        GinjectorBindings bindings = entry.getKey();
        Set<Key<?>> keys = entry.getValue();

        for(Key<?> key : keys) {
          expect(bindings.getBinding(key))
              .andReturn(createMock(Binding.class))
              .anyTimes();
        }
      }
    }
  }
}
