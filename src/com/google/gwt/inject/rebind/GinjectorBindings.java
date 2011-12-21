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
package com.google.gwt.inject.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.Preconditions;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.assistedinject.FactoryModule;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.BindingIndex;
import com.google.gwt.inject.rebind.binding.Dependency;
import com.google.gwt.inject.rebind.binding.ExposedChildBinding;
import com.google.gwt.inject.rebind.binding.ParentBinding;
import com.google.gwt.inject.rebind.binding.RemoteServiceProxyBinding;
import com.google.gwt.inject.rebind.reflect.FieldLiteral;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.inject.rebind.resolution.BindingResolver;
import com.google.gwt.inject.rebind.util.GuiceUtil;
import com.google.gwt.inject.rebind.util.MemberCollector;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionPoint;

import javax.inject.Provider;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores information that describes the bindings present in a given injector,
 * and the relationship to the other injectors in the hierarchy. This class is
 * used in two stages:
 * 
 * <ul>
 * <li>During bindings processing {@link BindingsProcessor} this
 * gathers up explicit bindings and unresolved dependencies. After all bindings
 * have been gathered from the modules, {@link #resolveBindings} is called in
 * the last stage of processing and it is finalized. After that point, no new
 * unresolved bindings should be added.
 * </li>
 * <li>At this point, it is ready to be used by {@link GinjectorOutputter} for
 * generating the Ginjector implementation. In this stage several additional
 * methods are available for getting information about the code that has been
 * generated to represent this ginjector.
 * </li>
 * </ul>
 *
 * <p>Each {@link GinjectorBindings} can have a parent ginjector, and any number of
 * child ginjectors.
 */
public class GinjectorBindings implements BindingIndex {

  private final TreeLogger logger;

  /**
   * Generates names for code we produce to resolve injection requests.
   */
  private final NameGenerator nameGenerator;

  /**
   * Map from key to binding for all types we already have a binding for.  We use a LinkedHashMap
   * so that error reporting (and tests) will be deterministic.
   */
  private final Map<Key<?>, Binding> bindings = new LinkedHashMap<Key<?>, Binding>(); 
  
  /**
   * Set of all Dependency edges that this Ginjector is aware of.  This includes dependecies that
   * have been satisfied by bindings that are already available.  Some examples:
   * bind(Foo.class); adds the dependency GINJECTOR -> Foo
   * bind(Foo.class).to(FooImpl.class); adds the dependencies GINJECTOR -> Foo and Foo -> FooImpl
   * 
   * <p>We use a LinkedHashSet so that error reporting (and tests) will be deterministic.
   */
  private final Set<Dependency> dependencies = new LinkedHashSet<Dependency>();

  /**
   * Map from key to scope for all types we have a binding for.
   */
  private final Map<Key<?>, GinScope> scopes = new HashMap<Key<?>, GinScope>();

  /**
   * Collection of keys for which the ginjector interface provides member inject
   * methods. If a regular binding is defined for the same key, no special
   * member inject handling is required - a member inject method will be created
   * as part of a regular binding.
   */
  private final Set<TypeLiteral<?>> memberInjectRequests = new HashSet<TypeLiteral<?>>();

  /**
   * Collection of all factory modules configured for this ginjector.
   */
  private final Set<FactoryModule<?>> factoryModules = new HashSet<FactoryModule<?>>();

  /**
   * All types for which static injection has been requested.
   */
  private final Set<Class<?>> staticInjectionRequests = new HashSet<Class<?>>();
  
  /**
   * The map of all keys that are bound locally in children of this ginjector to
   * the child binding it. "Locally" here means that they aren't inherited from
   * this ginjector or from one of its parents (speaking more pragmatically:
   * they aren't ParentBindings).  This is used when creating implicit
   * bindings. Specifically, we can't create an implicit binding here if any of
   * the children already bind it (even if its not exposed) because it would
   * lead to a double binding error.
   */
  private final Map<Key<?>, GinjectorBindings> boundLocallyInChildren =
      new HashMap<Key<?>, GinjectorBindings>();
  
  /**
   * Set of key's that *must* be bound here.  This corresponds to things that are explicitly bound
   * here. 
   */
  private final Set<Key<?>> pinned = new HashSet<Key<?>>();

  /**
   * Collector that gathers all methods from an injector.
   */
  private final MemberCollector completeCollector;

  private final GuiceUtil guiceUtil;

  /**
   * Interface of the injector that this class is implementing.
   */
  private final TypeLiteral<? extends Ginjector> ginjectorInterface;

  private final ErrorManager errorManager;

  /**
   * The class (either Ginjector or Module) that this {@link GinjectorBindings}
   * is created for.
   */
  private Class<?> module;

  // Parent/Child information used for creating hierarchical injectors
  private GinjectorBindings parent = null;
  private final List<GinjectorBindings> children = new ArrayList<GinjectorBindings>();

  private final Provider<GinjectorBindings> ginjectorBindingsProvider;

  private final BindingResolver bindingResolver;

  /**
   * The {@link GinjectorBindings} are used in two "stages" -- during binding processing 
   * (in {@link BindingsProcessor}) it is used to gather information, and during ginjector
   * generation where (in {@link GinjectorOutputter}) that information is read. To help
   * catch accidental mistakes this tracks which stage it is currently being used in.
   * TODO(bchambers): Split this class into two parts, and refactor the stages so that
   * they use the appropriate part.
   */
  private boolean finalized = false;

  @Inject
  public GinjectorBindings(NameGenerator nameGenerator,
      TreeLogger logger,
      GuiceUtil guiceUtil,
      @GinjectorInterfaceType Class<? extends Ginjector> ginjectorInterface,
      Provider<GinjectorBindings> ginjectorBindingsProvider,
      MemberCollector collector,
      ErrorManager errorManager,
      BindingResolver bindingResolver) {
    this.nameGenerator = nameGenerator;
    this.logger = logger;
    this.guiceUtil = guiceUtil;
    this.bindingResolver = bindingResolver;
    this.ginjectorInterface = TypeLiteral.get(ginjectorInterface);
    this.ginjectorBindingsProvider = ginjectorBindingsProvider;
    this.errorManager = errorManager;

    completeCollector = collector;
    completeCollector.setMethodFilter(MemberCollector.ALL_METHOD_FILTER);
  }

  void assertFinalized() {
    Preconditions.checkState(finalized, 
        "Can only use this method after finalizing the ginjector bindings!");
  }

  void assertNotFinalized() {
    Preconditions.checkState(!finalized,
        "Can only use this method before finalizing the ginjector bindings!");
  }

  /**
   * Create a new {@link GinjectorBindings} that collects bindings for an injector that is the
   * child of this {@link GinjectorBindings}.
   *
   * @param module the module the ginjector is being created for
   * @return the child {@link GinjectorBindings}
   */
  public GinjectorBindings createChildGinjectorBindings(Class<?> module) {
    assertNotFinalized();
    GinjectorBindings child = ginjectorBindingsProvider.get();
    child.setParent(this);
    child.setModule(module);
    children.add(child);
    return child;
  }

  public void resolveBindings() throws UnableToCompleteException {
    assertNotFinalized();
    
    bindingResolver.resolveBindings(this);
    errorManager.checkForError();

    // Mark this collection as finalized, so that no new bindings or unresolved
    // dependencies
    // can be added.
    finalized = true;
  }

  public Iterable<Dependency> getDependencies() {
    assertNotFinalized();
    return Collections.unmodifiableCollection(dependencies);
  }

  public Iterable<Key<?>> getBoundKeys() {
    return Collections.unmodifiableCollection(bindings.keySet());
  }

  public Iterable<Map.Entry<Key<?>, Binding>> getBindings() {
    return Collections.unmodifiableCollection(bindings.entrySet());
  }

  public Iterable<Class<?>> getStaticInjectionRequests() {
    return Collections.unmodifiableCollection(staticInjectionRequests);
  }

  public Iterable<TypeLiteral<?>> getMemberInjectRequests() {
    return Collections.unmodifiableCollection(memberInjectRequests);
  }

  void putScope(Key<?> key, GinScope scope) {
    scopes.put(key, scope);
  }

  public GinjectorBindings getParent() {
    return parent;
  }

  public void setParent(GinjectorBindings parent) {
    assertNotFinalized();
    this.parent = parent;
  }

  public Class<?> getModule() {
    return module;
  }
  
  public String getModuleName() {
    return getModule().getSimpleName();
  }

  public void setModule(Class<?> module) {
    this.module = module;
  }

  public Iterable<GinjectorBindings> getChildren() {
    return Collections.unmodifiableCollection(children);
  }
  
  public Iterable<FactoryModule<?>> getFactoryModules() {
    return Collections.unmodifiableCollection(factoryModules);
  }

  public NameGenerator getNameGenerator() {
    assertFinalized();
    return nameGenerator;
  }

  public GinScope determineScope(Key<?> key) {
    assertFinalized();
    GinScope scope = scopes.get(key);
    if (scope == null) {
      Class<?> raw = key.getTypeLiteral().getRawType();
      Binding binding = bindings.get(key);
      if (binding != null 
          && (binding instanceof ExposedChildBinding
              || binding instanceof ParentBinding)) {
        // If this is just a "copy" of a binding higher/lower in the injector
        // tree, we prefer to treat the binding like it's unscoped, and refer to
        // the "real" binding every time we need the value.
        scope = GinScope.NO_SCOPE; 
      } else if (raw.getAnnotation(Singleton.class) != null
          || raw.getAnnotation(javax.inject.Singleton.class) != null) {
        // Look for scope annotation as a fallback
        scope = GinScope.SINGLETON;
      } else if (RemoteServiceProxyBinding.isRemoteServiceProxy(key.getTypeLiteral())) {
        // Special case for remote services
        scope = GinScope.SINGLETON;
      } else {
        scope = GinScope.NO_SCOPE;
      }
    }

    logger.log(TreeLogger.TRACE, "scope for " + key + ": " + scope);
    return scope;
  }

  public boolean isBound(Key<?> key) {
    return bindings.containsKey(key);
  }

  public Binding getBinding(Key<?> key) {
    return bindings.get(key);
  }

  public void addDependency(Dependency dependency) {
    assertNotFinalized();
    dependencies.add(dependency);
  }
  
  public void addDependencies(Collection<Dependency> dependencies) {
    assertNotFinalized();
    this.dependencies.addAll(dependencies);
  }

  void addUnresolvedEntriesForInjectorInterface() {
    assertNotFinalized();
    for (MethodLiteral<?, Method> method : completeCollector.getMethods(ginjectorInterface)) {
      nameGenerator.markAsUsed(method.getName());
      Key<?> key = guiceUtil.getKey(method);
      logger.log(TreeLogger.TRACE, "Add unresolved key from injector interface: " + key);

      // Member inject types do not need to be gin-creatable themselves but we
      // need to provide all dependencies.
      if (guiceUtil.isMemberInject(method)) {
        memberInjectRequests.add(key.getTypeLiteral());
        addDependencies(guiceUtil.getMemberInjectionDependencies(
            Dependency.GINJECTOR, key.getTypeLiteral()));
      } else {
        addDependency(new Dependency(Dependency.GINJECTOR, key, method.toString()));
      }
    }
  }
  
  public void addBinding(Key<?> key, Binding binding) {
    assertNotFinalized();
    if (bindings.containsKey(key)) {
      errorManager.logDoubleBind(key, bindings.get(key), this, binding, this);
      return;
    }

    if (!isClassAccessibleFromGinjector(key.getTypeLiteral())) {
      errorManager.logError("Can not inject an instance of an inaccessible class: %s", key);
      return;
    }

    bindings.put(key, binding);
    if (parent != null && !(binding instanceof ParentBinding)) {
      parent.registerLocalChildBinding(key, this);
    }

    logger.log(TreeLogger.TRACE, "bound " + key + " to " + binding);
    dependencies.addAll(binding.getDependencies());
    memberInjectRequests.addAll(binding.getMemberInjectRequests());
  }
  
  public void addPin(Key<?> key) {
    pinned.add(key);
  }
  
  public boolean isPinned(Key<?> key) {
    return pinned.contains(key);
  }
  
  /**
   * Register the key in the "boundLocallyInChildren" set for this injector, and
   * recursively register it with all of the ancestors.  The caller is
   * responsible for ensuring that the binding being registered is actually
   * local (i.e., not a ParentBinding).
   */
  private void registerLocalChildBinding(Key<?> key, GinjectorBindings binding) {
    boundLocallyInChildren.put(key, binding);
    if (parent != null) {
      parent.registerLocalChildBinding(key, binding);
    }
  }
 
  public boolean isBoundLocallyInChild(Key<?> key) {
    return boundLocallyInChildren.containsKey(key);
  }

  /**
   * Returns the child injector which binds the given key. If no child binds the key, returns
   * {@code null}.
   */
  public GinjectorBindings getChildWhichBindsLocally(Key<?> key) {
    return boundLocallyInChildren.get(key);
  }

  private boolean isClassAccessibleFromGinjector(TypeLiteral<?> type) {
    if (ReflectUtil.isPublic(type)) {
      return true;
    }

    Package classPackage = type.getRawType().getPackage();
    Package ginjectorPackage = ginjectorInterface.getRawType().getPackage();
    return classPackage == ginjectorPackage;
  }

  void addStaticInjectionRequest(Class<?> type, Object source) {
    assertNotFinalized();
    staticInjectionRequests.add(type);

    // Calculate required bindings and add to dependencies
    for (InjectionPoint injectionPoint : InjectionPoint.forStaticMethodsAndFields(type)) {
      Member member = injectionPoint.getMember();
      if (member instanceof Method) {
        addDependencies(guiceUtil.getDependencies(Dependency.GINJECTOR,
            MethodLiteral.get((Method) member, TypeLiteral.get(member.getDeclaringClass()))));
      } else if (member instanceof Field) {
        FieldLiteral<?> field =
            FieldLiteral.get((Field) member, TypeLiteral.get(member.getDeclaringClass()));
        Key<?> key = guiceUtil.getKey(field);
        addDependency(new Dependency(
            Dependency.GINJECTOR, key, guiceUtil.isOptional(field), false,
            source.toString()));
      }
    }
  }

  public void addFactoryModule(FactoryModule<?> install) {
    factoryModules.add(install);
  }

  @Override
  public String toString() {
    if (parent == null) {
      return ginjectorInterface.toString();
    } else {
      return module.getCanonicalName();
    }
  }
}
