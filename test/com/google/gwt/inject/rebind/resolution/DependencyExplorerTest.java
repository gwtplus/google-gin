package com.google.gwt.inject.rebind.resolution;

import static com.google.gwt.inject.rebind.resolution.TestUtils.bar;
import static com.google.gwt.inject.rebind.resolution.TestUtils.baz;
import static com.google.gwt.inject.rebind.resolution.TestUtils.foo;
import static org.easymock.EasyMock.expect;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.Dependency;
import com.google.gwt.inject.rebind.resolution.DependencyExplorer.DependencyExplorerOutput;
import com.google.gwt.inject.rebind.resolution.ImplicitBindingCreator.BindingCreationException;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests for {@link DependencyExplorer}.
 */
public class DependencyExplorerTest extends TestCase {

  private static final String SOURCE = "dummy";

  private IMocksControl control;
  private ImplicitBindingCreator bindingCreator;
  private GinjectorBindings origin;
  private DependencyExplorer dependencyExplorer;
  private TreeLogger treeLogger;
  
  @Override
  protected void setUp() throws Exception {
    control = EasyMock.createControl();
    treeLogger = EasyMock.createNiceMock(TreeLogger.class);
    bindingCreator = control.createMock(ImplicitBindingCreator.class);
    origin = control.createMock(GinjectorBindings.class);
    dependencyExplorer = new DependencyExplorer(bindingCreator, treeLogger);
  }
  
  
  private <T> void assertContentsAnyOrder(Collection<T> actual, T... expected) {
    Set<T> expectedSet = new HashSet<T>();
    Set<T> actualSet = new HashSet<T>(actual);
    Collections.addAll(expectedSet, expected);
    assertEquals(String.format("Expected %s to equal %s", actualSet, expectedSet), 
        expectedSet, actualSet);
  }
  
  private <T> void assertEmpty(Collection<T> actual) {
    assertTrue(String.format("Expected to be empty, but was %s", actual), actual.isEmpty());
  }
  
  public void testAlreadyPositioned() throws Exception {
    expect(origin.getDependencies()).andStubReturn(TestUtils.dependencyList(
        new Dependency(Dependency.GINJECTOR, foo(), SOURCE)));
    expect(origin.getParent()).andReturn(null);
    expect(origin.isBound(foo())).andReturn(true);
    control.replay();
    DependencyExplorerOutput output = dependencyExplorer.explore(origin);
    assertSame(origin, output.getPreExistingLocations().get(foo()));
    assertEquals(1, output.getPreExistingLocations().size());
    assertEmpty(output.getBindingErrors());
    assertEmpty(output.getImplicitlyBoundKeys());
    assertSame(origin, output.getGraph().getOrigin());
    assertEmpty(output.getGraph().getDependenciesOf(foo()));
    assertContentsAnyOrder(output.getGraph().getDependenciesTargeting(foo()),
        new Dependency(Dependency.GINJECTOR, foo(), SOURCE));
    control.verify();
  }
  
  public void testAlreadyPositionedInParent() throws Exception {
    GinjectorBindings parent = control.createMock("parent", GinjectorBindings.class);
    expect(origin.getDependencies()).andStubReturn(TestUtils.dependencyList(
        new Dependency(Dependency.GINJECTOR, foo(), SOURCE)));
    expect(origin.getParent()).andReturn(parent);
    expect(parent.getParent()).andReturn(null);
    expect(origin.isBound(foo())).andReturn(false);
    expect(parent.isBound(foo())).andReturn(true);
    control.replay();
    DependencyExplorerOutput output = dependencyExplorer.explore(origin);
    assertSame(parent, output.getPreExistingLocations().get(foo()));
    control.verify();
  }
  
  public void testImplicitBinding() throws Exception {
    expect(origin.getDependencies()).andStubReturn(TestUtils.dependencyList(
            new Dependency(Dependency.GINJECTOR, foo(), SOURCE)));
    expect(origin.getParent()).andStubReturn(null);
    expect(origin.isBound(foo())).andReturn(false);
    Binding binding = control.createMock(Binding.class);
    expect(bindingCreator.create(foo())).andReturn(binding);
    expect(binding.getDependencies()).andReturn(TestUtils.dependencyList(
        new Dependency(foo(), bar(), SOURCE)));
    expect(origin.isBound(bar())).andReturn(true);
    control.replay();
    DependencyExplorerOutput output = dependencyExplorer.explore(origin);
    assertSame(origin, output.getPreExistingLocations().get(bar()));
    assertEmpty(output.getBindingErrors());
    assertEquals(1, output.getImplicitlyBoundKeys().size());
    assertEquals(foo(), output.getImplicitBindings().iterator().next().getKey());
    assertEquals(binding, output.getImplicitBindings().iterator().next().getValue());
    DependencyGraph graph = output.getGraph();
    assertContentsAnyOrder(graph.getDependenciesTargeting(foo()),
        new Dependency(Dependency.GINJECTOR, foo(), SOURCE));
    assertContentsAnyOrder(graph.getDependenciesTargeting(bar()),
        new Dependency(foo(), bar(), SOURCE));
    control.verify();
  }
  
  public void testImplicitBindingFailed() throws Exception {
    expect(origin.getDependencies()).andStubReturn(TestUtils.dependencyList(
        new Dependency(Dependency.GINJECTOR, foo(), SOURCE)));
    expect(origin.getParent()).andStubReturn(null);
    expect(origin.isBound(foo())).andReturn(false);
    expect(bindingCreator.create(foo())).andThrow(new BindingCreationException("failed"));
    control.replay();
    DependencyExplorerOutput output = dependencyExplorer.explore(origin);
    assertEquals(1, output.getBindingErrors().size());
    assertEquals(foo(), output.getBindingErrors().iterator().next().getKey());
    assertEquals("failed", output.getBindingErrors().iterator().next().getValue());
    control.verify();
  }
  
  public void testEdgeInUnresolvedAndOptional() throws Exception {
    expect(origin.getDependencies()).andStubReturn(TestUtils.dependencyList(
        new Dependency(foo(), bar(), SOURCE)));
    expect(origin.getParent()).andStubReturn(null);
    expect(origin.isBound(foo())).andReturn(true);
    expect(origin.isBound(bar())).andReturn(false);
    expect(origin.isBound(baz())).andReturn(true);
    Binding binding = control.createMock(Binding.class);
    expect(bindingCreator.create(bar())).andReturn(binding);
    expect(binding.getDependencies()).andReturn(TestUtils.dependencyList(
        new Dependency(bar(), baz(), true, false, SOURCE)));
    control.replay();
    DependencyExplorerOutput output = dependencyExplorer.explore(origin);
    DependencyGraph graph = output.getGraph();
    assertContentsAnyOrder(graph.getDependenciesOf(foo()), new Dependency(foo(), bar(), SOURCE));
    assertEmpty(graph.getDependenciesTargeting(foo()));
    assertContentsAnyOrder(graph.getDependenciesOf(bar()),
        new Dependency(bar(), baz(), true, false, SOURCE));
    assertContentsAnyOrder(graph.getDependenciesTargeting(bar()),
        new Dependency(foo(), bar(), SOURCE));
    control.verify();
  }
}
