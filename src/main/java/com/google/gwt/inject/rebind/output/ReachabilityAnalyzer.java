 /*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.inject.rebind.output;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.inject.rebind.GinScope;
import com.google.gwt.inject.rebind.GinjectorBindings;
import com.google.gwt.inject.rebind.RootBindings;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.Dependency;
import com.google.gwt.inject.rebind.binding.ExposedChildBinding;
import com.google.gwt.inject.rebind.binding.ParentBinding;
import com.google.gwt.inject.rebind.reflect.FieldLiteral;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.util.GuiceUtil;
import com.google.gwt.inject.rebind.util.MemberCollector;
import com.google.gwt.inject.rebind.util.PrettyPrinter;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionPoint;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility class that determines which bindings from a {@link GinjectorBindings}
 * hierarchy should actually be written in the generated code.
 *
 * <p>This eliminates a significant amount of code that we know will never be
 * invoked, based on our high-level knowledge of the generated code.  This is
 * preferable to relying entirely on GWT's reachability analysis, which is
 * (understandably) imperfect and sometimes makes different judgements as we
 * restructure the generated code files.
 */
final class ReachabilityAnalyzer {

  private Set<Binding> reachable = null;
  private Map<GinjectorBindings, Set<TypeLiteral<?>>> reachableMemberInjects = null;

  private final GuiceUtil guiceUtil;
  private final TreeLogger logger;
  private final MemberCollector memberCollector;
  private final GinjectorBindings rootBindings;

  @Inject
  public ReachabilityAnalyzer(
      GuiceUtil guiceUtil,
      Provider<MemberCollector> memberCollectorProvider,
      @RootBindings GinjectorBindings rootBindings,
      TreeLogger logger) {

    this.guiceUtil = guiceUtil;
    this.logger = logger;
    this.memberCollector = memberCollectorProvider.get();
    this.rootBindings = rootBindings;

    this.memberCollector.setMethodFilter(MemberCollector.ALL_METHOD_FILTER);
  }

  /**
   * Tests whether the given binding is reachable from a true root.
   *
   * <p>A true root is a key that the user can actually create.  In contrast,
   * the dependency tree we generate earlier in the process is rooted at keys
   * that must exist semantically, even if they are never created.
   *
   * <p>More specifically, the following keys are true roots:
   *
   * <ul>
   * <li>Every non-Void key that is the return value of an injector method.
   * <li>Every key that is a parameter to an injector method.
   * <li>Every key that is injected as an eager singleton.
   * <li>Every key that is a parameter to a static injection method on a class
   *     for which static injection was requested.
   * <li>Every key that is the type of a statically injected field of a class
   *     for which static injection was requested.
   * </ul>
   */
  boolean isReachable(Binding binding) {
    if (reachable == null) {
      computeReachable();
    }

    return reachable.contains(binding);
  }

  boolean isReachableMemberInject(GinjectorBindings bindings, TypeLiteral<?> type) {
    if (reachableMemberInjects == null) {
      computeReachable();
    }

    return getReachableMemberInjects(bindings).contains(type);
  }

  private void computeReachable() {
    reachable = new LinkedHashSet<Binding>();
    reachableMemberInjects = new LinkedHashMap<GinjectorBindings, Set<TypeLiteral<?>>>();

    logger.log(TreeLogger.DEBUG, "Begin reachability analysis");

    // Note on implementation: for simplicity, we use a Binding as the node of
    // the graph that we run reachability on.  This would be incoherent before
    // binding resolution (when we didn't know which bindings existed), but now
    // that all the bindings are created, every key that we can inject has a
    // unique Binding object.  Since the caller of this routine is interested in
    // determining which bindings to output, it's more convenient to just work
    // at the binding level.
    traceGinjectorMethods();
    traceEagerSingletons();
    traceStaticInjections();

    logger.log(TreeLogger.DEBUG, "End reachability analysis");
  }

  /** Traces out bindings that are reachable from a GInjector method. */
  private void traceGinjectorMethods() {
    TypeLiteral<?> ginjectorInterface = rootBindings.getGinjectorInterface();
    for (MethodLiteral<?, Method> method
         : memberCollector.getMethods(ginjectorInterface)) {

      if (!guiceUtil.isMemberInject(method)) {
        // It's a constructor method, so just trace to the key that's
        // constructed.
        Key<?> key = guiceUtil.getKey(method);
        PrettyPrinter.log(logger, TreeLogger.DEBUG,
            "ROOT -> %s:%s [%s]", rootBindings, key, method);
        traceKey(key, rootBindings);
      } else {
        Key<?> sourceKey = guiceUtil.getKey(method);
        getReachableMemberInjects(rootBindings).add(sourceKey.getTypeLiteral());
        for (Dependency dependency
             : guiceUtil.getMemberInjectionDependencies(sourceKey, sourceKey.getTypeLiteral())) {

          Key<?> targetKey = dependency.getTarget();
          PrettyPrinter.log(
              logger, TreeLogger.DEBUG, "ROOT -> %s:%s [%s]", rootBindings, targetKey, method);
          traceKey(targetKey, rootBindings);
        }
      }
    }
  }

  /** Traces out bindings that are reachable from an eager singleton. */
  private void traceEagerSingletons() {
    doTraceEagerSingletons(rootBindings);
  }

  private void doTraceEagerSingletons(GinjectorBindings bindings) {
    for (Map.Entry<Key<?>, Binding> entry : bindings.getBindings()) {
      Key<?> key = entry.getKey();
      Binding binding = entry.getValue();
      GinScope scope = bindings.determineScope(key);

      if (scope == GinScope.EAGER_SINGLETON) {
        PrettyPrinter.log(logger, TreeLogger.DEBUG,
            "ROOT -> %s:%s [eager singleton: %s]", bindings, key, binding);

        traceKey(key, bindings);
      }
    }

    for (GinjectorBindings child : bindings.getChildren()) {
      doTraceEagerSingletons(child);
    }
  }

  /**
   * Traces out bindings that are reachable from statically injected fields and
   * methods.
   */
  private void traceStaticInjections() {
    doTraceStaticInjections(rootBindings);
  }

  private void doTraceStaticInjections(GinjectorBindings bindings) {
    for (Class<?> klass : bindings.getStaticInjectionRequests()) {
      traceStaticInjectionsFor(klass, bindings);
    }

    for (GinjectorBindings child : bindings.getChildren()) {
      doTraceStaticInjections(child);
    }
  }

  private void traceStaticInjectionsFor(Class<?> klass, GinjectorBindings bindings) {
    TypeLiteral<?> type = TypeLiteral.get(klass);
    for (InjectionPoint injectionPoint : InjectionPoint.forStaticMethodsAndFields(klass)) {
      Member member = injectionPoint.getMember();

      if (member instanceof Method) {
        Method methodRaw = (Method) member;
        TypeLiteral<?> declaringClass = TypeLiteral.get(methodRaw.getDeclaringClass());

        MethodLiteral<?, ?> method = MethodLiteral.get(methodRaw, declaringClass);
        for (Key<?> key : method.getParameterKeys()) {
          PrettyPrinter.log(logger, TreeLogger.DEBUG, "ROOT -> %s:%s [static injection: %s]",
              bindings, key, method);

          traceKey(key, bindings);
        }
      } else if (member instanceof Field) {
        Field fieldRaw = (Field) member;
        TypeLiteral<?> declaringClass = TypeLiteral.get(fieldRaw.getDeclaringClass());

        FieldLiteral<?> field = FieldLiteral.get(fieldRaw, declaringClass);
        Key<?> key = guiceUtil.getKey(field);
        PrettyPrinter.log(logger, TreeLogger.DEBUG, "ROOT -> %s:%s [static injection: %s]",
            bindings, key, field);

        traceKey(key, bindings);
      }
    }
  }

  /**
   * Marks the binding of the given key in the given {@link GinjectorBindings}
   * as reachable, and traces out its dependencies.
   */
  private void traceKey(Key<?> key, GinjectorBindings bindings) {
    Binding binding = bindings.getBinding(key);
    // Make sure the binding is present: optional bindings might be missing.
    if (binding != null) {
      if (!reachable.add(binding)) {
        // The binding was already marked as reachable.
        return;
      }

      getReachableMemberInjects(bindings).addAll(binding.getMemberInjectRequests());

      for (Dependency dependency : binding.getDependencies()) {
        if (dependency.getSource().equals(key)) {
          Key<?> target = dependency.getTarget();

          PrettyPrinter.log(logger, TreeLogger.DEBUG, "%s:%s -> %s:%s [%s]",
              bindings, key, bindings, dependency.getTarget(), binding);
          traceKey(target, bindings);
        }
      }

      // Special cases: parent / child bindings induce dependencies between
      // GinjectorBindings objects, which can't be represented in the standard
      // dependency graph.
      if (binding instanceof ParentBinding) {
        ParentBinding parentBinding = (ParentBinding) binding;
        PrettyPrinter.log(logger, TreeLogger.DEBUG, "%s:%s -> %s:%s [inherited]",
            bindings, key, parentBinding.getParentBindings(), key);
        traceKey(key, parentBinding.getParentBindings());
      } else if (binding instanceof ExposedChildBinding) {
        ExposedChildBinding exposedChildBinding = (ExposedChildBinding) binding;
        PrettyPrinter.log(logger, TreeLogger.DEBUG, "%s:%s -> %s:%s [exposed]",
            bindings, key, exposedChildBinding.getChildBindings(), key);
        traceKey(key, exposedChildBinding.getChildBindings());
      }
    }
  }

  private Set<TypeLiteral<?>> getReachableMemberInjects(GinjectorBindings bindings) {
    Set<TypeLiteral<?>> result = reachableMemberInjects.get(bindings);
    if (result == null) {
      result = new LinkedHashSet<TypeLiteral<?>>();
      reachableMemberInjects.put(bindings, result);
    }

    return result;
  }
}
