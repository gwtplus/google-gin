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
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.BindingContext;
import com.google.gwt.inject.rebind.binding.BindingFactory;
import com.google.gwt.inject.rebind.binding.Dependency;
import com.google.gwt.inject.rebind.binding.ParentBinding;
import com.google.gwt.inject.rebind.resolution.DependencyExplorer.DependencyExplorerOutput;
import com.google.inject.Key;
import junit.framework.TestCase;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link BindingInstaller}.
 */
public class BindingInstallerTest extends TestCase {
   
  private IMocksControl control;
  private BindingPositioner positions;
  private DependencyGraph graph;
  private DependencyExplorerOutput output;
  
  private GinjectorBindings root;
  private GinjectorBindings child;
  
  private BindingFactory bindingFactory;
  private BindingInstaller installer;
  
  @SuppressWarnings("unchecked")
  @Override
  protected void setUp() throws Exception {
    control = EasyMock.createControl();
    positions = control.createMock(BindingPositioner.class);
    graph = control.createMock(DependencyGraph.class);
    output = control.createMock(DependencyExplorerOutput.class);
    
    root = control.createMock("root", GinjectorBindings.class);
    child = control.createMock("child", GinjectorBindings.class);
    
    bindingFactory = control.createMock(BindingFactory.class);

    expect(output.getGraph()).andStubReturn(graph);
    positions.position(output);
    installer = new BindingInstaller(positions, bindingFactory);
    expect(graph.getOrigin()).andStubReturn(child);
  }
  
  public void testNoDependencies() throws Exception {
    Map<Key<?>, Binding> implicitBindingMap = new HashMap<Key<?>, Binding>();    
    expect(output.getImplicitBindings()).andReturn(implicitBindingMap.entrySet());
    expect(child.getDependencies()).andReturn(TestUtils.dependencyList());
    
    control.replay();
    installer.installBindings(output);
    control.verify();
  }
  
  public void testInstallImplicitBindings() throws Exception {
    // Tests that implicit bindings that are not already available in the origin are made accessible
    // foo and bar both had implicit bindings created (with no dependencies).  Foo is installed in
    // the child, and bar is installed in root.  We should add a binding to make bar accessible in
    // the child.
    expect(positions.getPosition(foo())).andStubReturn(child);
    expect(positions.getPosition(bar())).andStubReturn(root);
    
    Map<Key<?>, Binding> implicitBindingMap = new HashMap<Key<?>, Binding>();
    
    // Parent Binding to make bar available to child
    ParentBinding barBinding = control.createMock("barBinding", ParentBinding.class);
    expect(child.isBound(bar())).andReturn(false);
    expect(bindingFactory.getParentBinding(bar(), root)).andReturn(barBinding);
    
    // Implicit binding for Foo
    Binding fooBinding = control.createMock("fooBinding", Binding.class);
    expect(graph.getDependenciesOf(foo())).andReturn(TestUtils.dependencyList());
    implicitBindingMap.put(foo(), fooBinding);
    
    expect(output.getImplicitBindings()).andReturn(implicitBindingMap.entrySet());
    expect(child.getDependencies()).andReturn(TestUtils.dependencyList(
        new Dependency(Dependency.GINJECTOR, foo()),
        new Dependency(Dependency.GINJECTOR, bar())));
    
    child.addBinding(eq(bar()), eq(barBinding), isA(BindingContext.class));
    child.addBinding(eq(foo()), eq(fooBinding), isA(BindingContext.class));
    control.replay();
    installer.installBindings(output);
    control.verify();
  }
  
  public void testInheritDependencies() throws Exception {
    // Tests that when we install an implicit binding (for foo), we install bindings to "inherit"
    // the dependencies (bar and baz) from the appropriate injectors.  In this case, bar must be
    // inherited from the root, but we don't need to do anything with baz, since it is already
    // available.
    expect(positions.getPosition(foo())).andStubReturn(child);
    expect(positions.getPosition(bar())).andStubReturn(root);
    expect(positions.getPosition(baz())).andStubReturn(child);
    
    Map<Key<?>, Binding> implicitBindingMap = new HashMap<Key<?>, Binding>();
    
    // Parent Binding to make bar available to child
    ParentBinding barBinding = control.createMock("barBinding", ParentBinding.class);
    expect(child.isBound(bar())).andReturn(false);
    expect(bindingFactory.getParentBinding(bar(), root)).andReturn(barBinding);

    // Implicit binding for Bar
    Binding bazBinding = control.createMock("bazBinding", Binding.class);
    expect(graph.getDependenciesOf(baz())).andReturn(TestUtils.dependencyList());
    implicitBindingMap.put(baz(), bazBinding);
    
    // Implicit binding for Foo
    Binding fooBinding = control.createMock("fooBinding", Binding.class);
    expect(graph.getDependenciesOf(foo())).andReturn(TestUtils.dependencyList(
        new Dependency(foo(), bar()),
        new Dependency(foo(), baz())));
    implicitBindingMap.put(foo(), fooBinding);
    
    expect(output.getImplicitBindings()).andReturn(implicitBindingMap.entrySet());
    expect(child.getDependencies()).andReturn(TestUtils.dependencyList(
        new Dependency(Dependency.GINJECTOR, foo())));
    
    child.addBinding(eq(baz()), eq(bazBinding), isA(BindingContext.class));
    child.addBinding(eq(bar()), eq(barBinding), isA(BindingContext.class));
    child.addBinding(eq(foo()), eq(fooBinding), isA(BindingContext.class));
    control.replay();
    installer.installBindings(output);
    control.verify();
  }
}
