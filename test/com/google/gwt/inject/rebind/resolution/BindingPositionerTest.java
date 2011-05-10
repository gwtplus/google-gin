package com.google.gwt.inject.rebind.resolution;

import static com.google.gwt.inject.rebind.resolution.TestUtils.bar;
import static com.google.gwt.inject.rebind.resolution.TestUtils.baz;
import static com.google.gwt.inject.rebind.resolution.TestUtils.foo;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import com.google.gwt.dev.util.Preconditions;
import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.binding.Dependency;
import com.google.gwt.inject.rebind.resolution.DependencyExplorer.DependencyExplorerOutput;
import com.google.inject.Key;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link BindingPositioner}.
 */
public class BindingPositionerTest extends TestCase {

  private static final String SOURCE = "dummy";

  private IMocksControl control;
  
  private GinjectorBindings root;
  private GinjectorBindings child;
  private GinjectorBindings grandchild;
    
  @Override
  protected void setUp() throws Exception {
    control = EasyMock.createControl();
        
    root = control.createMock("root", GinjectorBindings.class);
    child = control.createMock("child", GinjectorBindings.class);
    grandchild = control.createMock("grandchild", GinjectorBindings.class);
    expect(grandchild.getParent()).andStubReturn(child);
    expect(child.getParent()).andStubReturn(root);
    expect(root.getParent()).andStubReturn(null);
    expect(root.isBoundInChild(isA(Key.class))).andStubReturn(false);
    expect(child.isBoundInChild(isA(Key.class))).andStubReturn(false);
    expect(grandchild.isBoundInChild(isA(Key.class))).andStubReturn(false);
    expect(root.isBound(isA(Key.class))).andStubReturn(false);
    expect(child.isBound(isA(Key.class))).andStubReturn(false);
    expect(grandchild.isBound(isA(Key.class))).andStubReturn(false);
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
    expect(root.isBoundInChild(bar())).andReturn(true).anyTimes(); // bar() must be in child
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
    expect(root.isBoundInChild(foo())).andReturn(true).anyTimes();
    expect(child.isBoundInChild(foo())).andReturn(true).anyTimes();
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
    expect(root.isBoundInChild(bar())).andReturn(true).anyTimes(); // bar() must be in child
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
    expect(root.isBoundInChild(bar())).andReturn(true).anyTimes();
    expect(root.isBoundInChild(baz())).andReturn(true).anyTimes();
    expect(child.isBoundInChild(baz())).andReturn(true).anyTimes();
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
    
    public PositionerExpectationsBuilder keysBoundAt(GinjectorBindings ginjector, Key<?>... keys) {
      for (Key<?> key : keys) {
        Preconditions.checkState(!implicitlyBoundKeys.containsKey(key),
            "Key %s cannot be bound at %s -- already in implicitly bound set!", key, ginjector);
        preExistingLocations.put(key, ginjector);
      }
      return this;
    }
    
    public void test() {
      DependencyExplorerOutput output = control.createMock(DependencyExplorerOutput.class);
      expect(output.getGraph()).andStubReturn(graphBuilder.build());
      expect(output.getImplicitlyBoundKeys()).andStubReturn(implicitlyBoundKeys.keySet());
      expect(output.getPreExistingLocations()).andStubReturn(preExistingLocations);
      control.replay();
      BindingPositioner positioner = new BindingPositioner();
      positioner.position(output);

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
      
      control.verify();
    }
  }
}
