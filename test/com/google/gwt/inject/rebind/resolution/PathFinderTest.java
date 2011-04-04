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

import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.binding.Dependency;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

public class PathFinderTest extends TestCase {
  private IMocksControl control;
  private GinjectorBindings origin;
  
  @Override
  protected void setUp() throws Exception {
    control = EasyMock.createControl();
    origin = control.createMock(GinjectorBindings.class);
  }
  
  public void testFindAnyPath() throws Exception {
    DependencyGraph graph = new DependencyGraph.Builder(origin)
        .addEdge(new Dependency(Dependency.GINJECTOR, foo()))
        .addEdge(new Dependency(foo(), bar()))
        .addEdge(new Dependency(bar(), baz()))
        .build();
    control.replay();
    assertEquals(TestUtils.dependencyList(
            new Dependency(Dependency.GINJECTOR, foo()),
            new Dependency(foo(), bar()), 
            new Dependency(bar(), baz())),
        new PathFinder().onGraph(graph)
            .addRoots(Dependency.GINJECTOR)
            .addDestinations(baz()).findShortestPath());
    control.verify();
  }
  
  public void testFindShortestPath() throws Exception {
    DependencyGraph graph = new DependencyGraph.Builder(origin)
        .addEdge(new Dependency(Dependency.GINJECTOR, foo()))
        .addEdge(new Dependency(foo(), bar()))
        .addEdge(new Dependency(bar(), baz()))
        .addEdge(new Dependency(foo(), baz())) // we should use the "shortcut" to baz
        .build();
    control.replay();
    assertEquals(TestUtils.dependencyList(
            new Dependency(Dependency.GINJECTOR, foo()),
            new Dependency(foo(), baz())),
        new PathFinder().onGraph(graph)
            .addRoots(Dependency.GINJECTOR)
            .addDestinations(baz()).findShortestPath());
    control.verify();
  }
  
  public void testFindShortestPath_MultipleDestinations() throws Exception {
    DependencyGraph graph = new DependencyGraph.Builder(origin)
        .addEdge(new Dependency(Dependency.GINJECTOR, foo()))
        .addEdge(new Dependency(foo(), bar()))
        .addEdge(new Dependency(bar(), baz()))
        .build();
    control.replay();
    assertEquals(TestUtils.dependencyList(
            new Dependency(Dependency.GINJECTOR, foo()),
            new Dependency(foo(), bar())),
        new PathFinder().onGraph(graph)
            .addRoots(Dependency.GINJECTOR)
            .addDestinations(baz(), bar()).findShortestPath());
    control.verify();
  }
  
  public void testPathInUnresolved() throws Exception {
    DependencyGraph graph = new DependencyGraph.Builder(origin)
        .addEdge(new Dependency(Dependency.GINJECTOR, foo()))
        .addEdge(new Dependency(Dependency.GINJECTOR, baz()))
        .addEdge(new Dependency(foo(), bar()))
        .addEdge(new Dependency(bar(), baz()))
        .addEdge(new Dependency(foo(), baz()))
        .build();
    control.replay();
    assertEquals(TestUtils.dependencyList(
            new Dependency(Dependency.GINJECTOR, baz())),
        new PathFinder().onGraph(graph)
            .addRoots(Dependency.GINJECTOR)
            .addDestinations(baz()).findShortestPath());
    control.verify();
  }
  
  public void testRequiredPath_OptionalInPath() throws Exception {
    DependencyGraph graph = new DependencyGraph.Builder(origin)
        .addEdge(new Dependency(Dependency.GINJECTOR, foo()))
        .addEdge(new Dependency(foo(), bar()))
        .addEdge(new Dependency(bar(), baz()))
        .addEdge(new Dependency(foo(), baz(), true, false)) // avoid optional short-cut
        .build();
    control.replay();
    assertEquals(TestUtils.dependencyList(
            new Dependency(Dependency.GINJECTOR, foo()),
            new Dependency(foo(), bar()),
            new Dependency(bar(), baz())),
        new PathFinder().onGraph(graph)
            .addRoots(Dependency.GINJECTOR)
            .addDestinations(baz())
            .withOnlyRequiredEdges(true).findShortestPath());
    control.verify();
  }
  
  public void testRequiredPath_OptionalInUnresolved() throws Exception {
    DependencyGraph graph = new DependencyGraph.Builder(origin)
        .addEdge(new Dependency(Dependency.GINJECTOR, foo()))
        .addEdge(new Dependency(Dependency.GINJECTOR, baz(), true, false))
        .addEdge(new Dependency(foo(), bar()))
        .addEdge(new Dependency(bar(), baz()))
        .build();
    control.replay();
    assertEquals(TestUtils.dependencyList(
            new Dependency(Dependency.GINJECTOR, foo()),
            new Dependency(foo(), bar()),
            new Dependency(bar(), baz())),
        new PathFinder().onGraph(graph)
            .addRoots(Dependency.GINJECTOR)
            .addDestinations(baz())
            .withOnlyRequiredEdges(true).findShortestPath());
    control.verify();
  }
}