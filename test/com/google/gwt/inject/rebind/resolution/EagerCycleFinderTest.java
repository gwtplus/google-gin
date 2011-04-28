/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.inject.rebind.resolution;

import static com.google.gwt.inject.rebind.resolution.TestUtils.bar;
import static com.google.gwt.inject.rebind.resolution.TestUtils.baz;
import static com.google.gwt.inject.rebind.resolution.TestUtils.foo;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import com.google.gwt.inject.rebind.ErrorManager;
import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.binding.Dependency;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

public class EagerCycleFinderTest extends TestCase { 

  private static final String SOURCE = "dummy";

  private IMocksControl control;
  private GinjectorBindings origin;
  private ErrorManager errorManager;
  private EagerCycleFinder eagerCycleFinder;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    control = EasyMock.createControl();
    origin = control.createMock(GinjectorBindings.class);
    errorManager = control.createMock(ErrorManager.class);
    eagerCycleFinder = new EagerCycleFinder(errorManager);
  }
  
  public void testShortCycleDetection() throws Exception {
    eagerCycleFinder.reportError(
        TestUtils.dependencyList(new Dependency(Dependency.GINJECTOR, foo(), SOURCE)),
        TestUtils.dependencyList(new Dependency(foo(), foo(), SOURCE)));
    expect(origin.getDependencies()).andStubReturn(
        TestUtils.dependencyList(new Dependency(Dependency.GINJECTOR, foo(), SOURCE)));
    control.replay();
    DependencyGraph graph = new DependencyGraph.Builder(origin)
        .addEdge(new Dependency(Dependency.GINJECTOR, foo(), SOURCE))
        .addEdge(new Dependency(foo(), foo(), SOURCE)).build();
    assertTrue(eagerCycleFinder.findAndReportCycles(graph));
    control.verify();
  }
  
  public void testBasicCycleDetection() throws Exception {
    eagerCycleFinder.reportError(
        TestUtils.dependencyList(new Dependency(Dependency.GINJECTOR, foo(), SOURCE)),
        TestUtils.dependencyList(
            new Dependency(foo(), bar(), SOURCE),
            new Dependency(bar(), foo(), SOURCE)));
    expect(origin.getDependencies()).andStubReturn(
        TestUtils.dependencyList(new Dependency(Dependency.GINJECTOR, foo(), SOURCE)));
    control.replay();
    DependencyGraph graph = new DependencyGraph.Builder(origin)
        .addEdge(new Dependency(Dependency.GINJECTOR, foo(), SOURCE))
        .addEdge(new Dependency(foo(), bar(), SOURCE))
        .addEdge(new Dependency(bar(), foo(), SOURCE)).build();
    assertTrue(eagerCycleFinder.findAndReportCycles(graph));
    control.verify();
  }
  
  public void testEagerCycleInUnresolved() throws Exception {
    eagerCycleFinder.reportError(
        TestUtils.dependencyList(new Dependency(Dependency.GINJECTOR, foo(), SOURCE)),
        TestUtils.dependencyList(
            new Dependency(foo(), bar(), SOURCE),
            new Dependency(bar(), foo(), SOURCE)));
    expect(origin.getDependencies()).andStubReturn(TestUtils.dependencyList(
        new Dependency(Dependency.GINJECTOR, foo(), SOURCE),
        new Dependency(foo(), bar(), SOURCE)));
    control.replay();
    DependencyGraph graph = new DependencyGraph.Builder(origin)
        .addEdge(new Dependency(Dependency.GINJECTOR, foo(), SOURCE))
        .addEdge(new Dependency(foo(), bar(), SOURCE))
        .addEdge(new Dependency(bar(), foo(), SOURCE)).build();
    assertTrue(eagerCycleFinder.findAndReportCycles(graph));
    control.verify();
  }
  
  public void testEagerCycleSecondInUnresolved() throws Exception {
    eagerCycleFinder.reportError(
        TestUtils.dependencyList(new Dependency(Dependency.GINJECTOR, foo(), SOURCE)),
        TestUtils.dependencyList(
            new Dependency(foo(), baz(), SOURCE),
            new Dependency(baz(), foo(), SOURCE)));
    expect(origin.getDependencies()).andStubReturn(TestUtils.dependencyList(
        new Dependency(Dependency.GINJECTOR, foo(), SOURCE),
        new Dependency(foo(), bar(), SOURCE),
        new Dependency(foo(), baz(), SOURCE)));
    control.replay();
    DependencyGraph graph = new DependencyGraph.Builder(origin)
        .addEdge(new Dependency(Dependency.GINJECTOR, foo(), SOURCE))
        .addEdge(new Dependency(foo(), bar(), SOURCE))
        .addEdge(new Dependency(foo(), baz(), SOURCE))
        .addEdge(new Dependency(baz(), foo(), SOURCE)).build();
    assertTrue(eagerCycleFinder.findAndReportCycles(graph));
    control.verify();
  }
  
  public void testOnlyReportedOnce() throws Exception {
    errorManager.logError(isA(String.class));
    expect(origin.getDependencies()).andStubReturn(
        TestUtils.dependencyList(
            new Dependency(Dependency.GINJECTOR, foo(), SOURCE),
            new Dependency(Dependency.GINJECTOR, bar(), SOURCE)));
    control.replay();
    DependencyGraph graph = new DependencyGraph.Builder(origin)
        .addEdge(new Dependency(Dependency.GINJECTOR, foo(), SOURCE))
        .addEdge(new Dependency(foo(), bar(), SOURCE))
        .addEdge(new Dependency(bar(), foo(), SOURCE)).build();
    assertTrue(eagerCycleFinder.findAndReportCycles(graph));
    control.verify();
  }
  
  public void testNoCycle() throws Exception {
    expect(origin.getDependencies()).andStubReturn(
        TestUtils.dependencyList(new Dependency(foo(), bar(), SOURCE)));
    control.replay();
    DependencyGraph graph = new DependencyGraph.Builder(origin)
        .addEdge(new Dependency(foo(), bar(), SOURCE))
        .addEdge(new Dependency(bar(), baz(), SOURCE)).build();
    assertFalse(eagerCycleFinder.findAndReportCycles(graph));
    control.verify();
  }
  
  public void testNotEagerCycle() throws Exception {
    expect(origin.getDependencies()).andStubReturn(
        TestUtils.dependencyList(new Dependency(Dependency.GINJECTOR, foo(), SOURCE)));
    control.replay();
    DependencyGraph graph = new DependencyGraph.Builder(origin)
        .addEdge(new Dependency(Dependency.GINJECTOR, foo(), SOURCE))
        .addEdge(new Dependency(foo(), bar(), SOURCE))
        .addEdge(new Dependency(bar(), foo(), false, true, SOURCE)).build();
    assertFalse(eagerCycleFinder.findAndReportCycles(graph));
    control.verify();
  }
  
  public void testNotEagerShortCycle() throws Exception {
    expect(origin.getDependencies()).andStubReturn(
        TestUtils.dependencyList(new Dependency(Dependency.GINJECTOR, foo(), SOURCE)));
    control.replay();
    DependencyGraph graph = new DependencyGraph.Builder(origin)
        .addEdge(new Dependency(Dependency.GINJECTOR, foo(), SOURCE))
        .addEdge(new Dependency(foo(), foo(), false, true, SOURCE)).build();
    assertFalse(eagerCycleFinder.findAndReportCycles(graph));
    control.verify();
  }
  
  public void testLazyCycleInUnresolved() throws Exception {
    expect(origin.getDependencies()).andStubReturn(
        TestUtils.dependencyList(new Dependency(foo(), bar(), false, true, SOURCE)));
    control.replay();
    DependencyGraph graph = new DependencyGraph.Builder(origin)
        .addEdge(new Dependency(foo(), bar(), false, true, SOURCE))
        .addEdge(new Dependency(bar(), foo(), SOURCE)).build();
    assertFalse(eagerCycleFinder.findAndReportCycles(graph));
    control.verify();
  }
  
  public void testLazyProviderDoesNotHideCycle() throws Exception {
    eagerCycleFinder.reportError(
        TestUtils.dependencyList(new Dependency(Dependency.GINJECTOR, foo(), SOURCE)),
        TestUtils.dependencyList(
            new Dependency(foo(), baz(), SOURCE),
            new Dependency(baz(), foo(), SOURCE)));
    expect(origin.getDependencies()).andStubReturn(
        TestUtils.dependencyList(new Dependency(Dependency.GINJECTOR, foo(), SOURCE)));
    control.replay();
    DependencyGraph graph = new DependencyGraph.Builder(origin)
        .addEdge(new Dependency(Dependency.GINJECTOR, foo(), SOURCE))
        .addEdge(new Dependency(foo(), bar(), false, true, SOURCE))
        .addEdge(new Dependency(bar(), baz(), SOURCE))
        .addEdge(new Dependency(foo(), baz(), SOURCE))
        .addEdge(new Dependency(baz(), foo(), SOURCE)).build();
    assertTrue(eagerCycleFinder.findAndReportCycles(graph));
    control.verify();
  }
  
  public void testCycleAfterProvider() throws Exception {
    eagerCycleFinder.reportError(
        TestUtils.dependencyList(
            new Dependency(Dependency.GINJECTOR, foo(), SOURCE),
            new Dependency(foo(), bar(), false, true, SOURCE)),
        TestUtils.dependencyList(
            new Dependency(bar(), baz(), SOURCE),
            new Dependency(baz(), bar(), SOURCE)));
    expect(origin.getDependencies()).andStubReturn(
        TestUtils.dependencyList(new Dependency(Dependency.GINJECTOR, foo(), SOURCE)));
    control.replay();
    DependencyGraph graph = new DependencyGraph.Builder(origin)
        .addEdge(new Dependency(Dependency.GINJECTOR, foo(), SOURCE))
        .addEdge(new Dependency(foo(), bar(), false, true, SOURCE))
        .addEdge(new Dependency(bar(), baz(), SOURCE))
        .addEdge(new Dependency(baz(), bar(), SOURCE)).build();
    assertTrue(eagerCycleFinder.findAndReportCycles(graph));
    control.verify();
  }
}
