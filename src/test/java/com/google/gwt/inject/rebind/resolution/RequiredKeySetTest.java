package com.google.gwt.inject.rebind.resolution;

import static com.google.gwt.inject.rebind.resolution.TestUtils.bar;
import static com.google.gwt.inject.rebind.resolution.TestUtils.baz;
import static com.google.gwt.inject.rebind.resolution.TestUtils.foo;

import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.binding.Dependency;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

/**
 * Tests for {@link RequiredKeySet}.
 */
public class RequiredKeySetTest extends TestCase {

  private static final String SOURCE = "dummy";

  private IMocksControl control;
  private GinjectorBindings origin;

  @Override
  protected void setUp() throws Exception {
    control = EasyMock.createControl();
    origin = control.createMock(GinjectorBindings.class);
  }
  
  public void testChainOfSingleDeps() throws Exception {
    EasyMock.expect(origin.getDependencies()).andStubReturn(TestUtils.dependencyList(
        new Dependency(Dependency.GINJECTOR, foo(), SOURCE)));
    control.replay();
    DependencyGraph graph = new DependencyGraph.Builder(origin)
        .addEdge(new Dependency(Dependency.GINJECTOR, foo(), SOURCE))
        .addEdge(new Dependency(foo(), bar(), SOURCE))
        .addEdge(new Dependency(bar(), baz(), SOURCE)).build();
    RequiredKeySet requiredKeys = new RequiredKeySet(graph);
    assertTrue(requiredKeys.isRequired(foo()));
    assertTrue(requiredKeys.isRequired(bar()));
    assertTrue(requiredKeys.isRequired(baz()));
    control.verify();
  }
  
  public void testWithMultipleDeps() throws Exception {
    EasyMock.expect(origin.getDependencies()).andStubReturn(TestUtils.dependencyList(
        new Dependency(Dependency.GINJECTOR, foo(), SOURCE)));
    control.replay();
    DependencyGraph graph = new DependencyGraph.Builder(origin)
        .addEdge(new Dependency(Dependency.GINJECTOR, foo(), SOURCE))
        .addEdge(new Dependency(foo(), bar(), SOURCE))
        .addEdge(new Dependency(foo(), baz(), SOURCE)).build();
    RequiredKeySet requiredKeys = new RequiredKeySet(graph);
    assertTrue(requiredKeys.isRequired(foo()));
    assertTrue(requiredKeys.isRequired(bar()));
    assertTrue(requiredKeys.isRequired(baz()));
    control.verify();
  }
  
  public void testRequiredSkipsOptional() throws Exception {
    EasyMock.expect(origin.getDependencies()).andStubReturn(TestUtils.dependencyList(
        new Dependency(Dependency.GINJECTOR, foo(), SOURCE)));
    control.replay();
    DependencyGraph graph = new DependencyGraph.Builder(origin)
        .addEdge(new Dependency(Dependency.GINJECTOR, foo(), SOURCE))
        .addEdge(new Dependency(foo(), bar(), true, false, SOURCE))
        .addEdge(new Dependency(foo(), baz(), SOURCE)).build();
    RequiredKeySet requiredKeys = new RequiredKeySet(graph);
    assertTrue(requiredKeys.isRequired(foo()));
    assertFalse(requiredKeys.isRequired(bar()));
    assertTrue(requiredKeys.isRequired(baz()));
    control.verify();
  }
  
  public void testRequiredNotHiddenByOptional() throws Exception {
    EasyMock.expect(origin.getDependencies()).andStubReturn(TestUtils.dependencyList(
        new Dependency(Dependency.GINJECTOR, foo(), SOURCE)));
    control.replay();
    DependencyGraph graph = new DependencyGraph.Builder(origin)
        .addEdge(new Dependency(Dependency.GINJECTOR, foo(), SOURCE))
        .addEdge(new Dependency(foo(), bar(), true, false, SOURCE))
        .addEdge(new Dependency(baz(), bar(), SOURCE))
        .addEdge(new Dependency(foo(), baz(), SOURCE)).build();
    RequiredKeySet requiredKeys = new RequiredKeySet(graph);
    assertTrue(requiredKeys.isRequired(foo()));
    assertTrue(requiredKeys.isRequired(bar()));
    assertTrue(requiredKeys.isRequired(baz()));
    control.verify();
  }
}
