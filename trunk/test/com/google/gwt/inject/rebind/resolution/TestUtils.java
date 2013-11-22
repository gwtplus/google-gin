package com.google.gwt.inject.rebind.resolution;

import com.google.gwt.inject.client.AsyncProvider;
import com.google.gwt.inject.rebind.binding.Dependency;
import com.google.inject.Key;
import com.google.inject.Provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for creating test input/output for the unit tests for pieces of resolution.
 */
class TestUtils {
  
  private TestUtils() {}
  
  static Map<Key<?>, Set<Dependency>> originDependencies(Dependency... dependencies) {
    Map<Key<?>, Set<Dependency>> rootDependencies = new HashMap<Key<?>, Set<Dependency>>();
    for (Dependency dependency : dependencies) {
      Set<Dependency> dependencySet = rootDependencies.get(dependency.getTarget());
      if (dependencySet == null) {
        dependencySet = new LinkedHashSet<Dependency>();
        rootDependencies.put(dependency.getTarget(), dependencySet);
      }
      dependencySet.add(dependency);
    }
    return rootDependencies;
  }
  
  static List<Dependency> dependencyList(Dependency... dependencies) {
    ArrayList<Dependency> list = new ArrayList<Dependency>();
    Collections.addAll(list, dependencies);
    return list;
  }
  
  private static class Foo {}
  private static class FooImpl {}
  private static class Bar {}
  private static class Baz {}
 
  public static Key<Foo> foo() {
    return Key.get(Foo.class);
  }
  
  public static Key<FooImpl> fooImpl() {
    return Key.get(FooImpl.class);
  }
  
  public static Key<Bar> bar() {
    return Key.get(Bar.class);
  }
  
  public static Key<Baz> baz() {
    return Key.get(Baz.class);
  }
  
  public static Key<AsyncProvider<Foo>> asyncProviderFoo() {
    return new Key<AsyncProvider<Foo>>() {};
  }
  
  public static Key<Provider<Foo>> providerFoo() {
    return new Key<Provider<Foo>>() {};
  }
  
  public static Key<Provider<Bar>> providerBar() {
    return new Key<Provider<Bar>>() {};
  }
  
  public static Key<Provider<Baz>> providerBaz() {
    return new Key<Provider<Baz>>() {};
  }
}
