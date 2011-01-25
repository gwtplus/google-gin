/*
 * Copyright 2010 Google Inc.
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
import com.google.gwt.inject.client.AsyncProvider;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.rebind.binding.AsyncProviderBinding;
import com.google.gwt.inject.rebind.binding.BindClassBinding;
import com.google.gwt.inject.rebind.binding.BindConstantBinding;
import com.google.gwt.inject.rebind.binding.BindProviderBinding;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.BindingContext;
import com.google.gwt.inject.rebind.binding.BindingIndex;
import com.google.gwt.inject.rebind.binding.CallConstructorBinding;
import com.google.gwt.inject.rebind.binding.CallGwtDotCreateBinding;
import com.google.gwt.inject.rebind.binding.GinjectorBinding;
import com.google.gwt.inject.rebind.binding.ImplicitProviderBinding;
import com.google.gwt.inject.rebind.binding.RemoteServiceProxyBinding;
import com.google.gwt.inject.rebind.binding.RequiredKeys;
import com.google.gwt.inject.rebind.reflect.FieldLiteral;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.inject.rebind.util.GuiceUtil;
import com.google.gwt.inject.rebind.util.MemberCollector;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.inject.ImplementedBy;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.ProvidedBy;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionPoint;
import com.google.inject.spi.Message;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

/**
 * Stores all the information about the injector.  Most importantly, which bindings
 * are present, and the related scopes.  Also contains information about unresolved
 * dependencies that those bindings have, and provides methods for filling in the
 * necessary dependencies.
 */
@Singleton
public class BindingCollection implements BindingIndex {

  private final TreeLogger logger;

  /**
   * Generates names for code we produce to resolve injection requests.
   */
  private final NameGenerator nameGenerator;

  /**
   * Map from key to binding for all types we already have a binding for.
   */
  private final Map<Key<?>, BindingEntry> bindings = new HashMap<Key<?>, BindingEntry>();

  /**
   * Map from key to scope for all types we have a binding for.
   */
  private final Map<Key<?>, GinScope> scopes = new HashMap<Key<?>, GinScope>();

  /**
   * Set of keys for classes that we still need to resolve. Every time a
   * binding is added to {@code bindings}, the key is removed from this set.
   * When this set and {@code unresolvedOptional} becomes empty, we know we've
   * satisfied all dependencies.
   */
  private final Set<Key<?>> unresolved = new HashSet<Key<?>>();

  /**
   * Set of keys for classes that we still need to resolve but that are
   * optionally bound. Every time a binding is added to {@code bindings},
   * the key is removed from this set. When this set and {@code unresolved}
   * becomes empty, we know we've satisfied all dependencies.
   */
  private final Set<Key<?>> unresolvedOptional = new HashSet<Key<?>>();

  /**
   * Collection of keys for which the ginjector interface provides member
   * inject methods. If a regular binding is defined for the same key, no
   * special member inject handling is required - a member inject method will
   * be created as part of a regular binding.
   */
  private final Set<Key<?>> memberInjectRequests = new HashSet<Key<?>>();

  /**
   * All types for which static injection has been requested.
   */
  private final Set<Class<?>> staticInjectionRequests = new HashSet<Class<?>>();

  /**
   * Collector that gathers all methods from an injector.
   */
  private final MemberCollector completeCollector;

  private final Provider<CallGwtDotCreateBinding> callGwtDotCreateBindingProvider;
  private final Provider<RemoteServiceProxyBinding> remoteServiceProxyBindingProvider;
  private final Provider<CallConstructorBinding> callConstructorBinding;
  private final Provider<BindClassBinding> bindClassBindingProvider;
  private final Provider<BindProviderBinding> bindProviderBindingProvider;
  private final Provider<ImplicitProviderBinding> implicitProviderBindingProvider;
  private final Provider<AsyncProviderBinding> asyncProviderBindingProvider;
  private final Provider<GinjectorBinding> ginjectorBindingProvider;

  private final GuiceUtil guiceUtil;

  /**
   * Interface of the injector that this class is implementing.
   */
  private final TypeLiteral<? extends Ginjector> ginjectorInterface;

  /**
   * Module used to pretend to Guice about the source of all generated binding
   * targets.
   */
  private final LieToGuiceModule lieToGuiceModule;

  private final ErrorManager errorManager;

  @Inject
  public BindingCollection(NameGenerator nameGenerator, TreeLogger logger,
      Provider<CallGwtDotCreateBinding> callGwtDotCreateBindingProvider,
      Provider<CallConstructorBinding> callConstructorBinding,
      GuiceUtil guiceUtil,
      Provider<BindClassBinding> bindClassBindingProvider,
      Provider<BindProviderBinding> bindProviderBindingProvider,
      Provider<ImplicitProviderBinding> implicitProviderBindingProvider,
      Provider<AsyncProviderBinding> asyncProviderBindingProvider,
      @GinjectorInterfaceType Class<? extends Ginjector> ginjectorInterface,
      LieToGuiceModule lieToGuiceModule,
      Provider<RemoteServiceProxyBinding> remoteServiceProxyBindingProvider,
      Provider<GinjectorBinding> ginjectorBindingProvider,
      MemberCollector collector,
      ErrorManager errorManager) {
    this.nameGenerator = nameGenerator;
    this.logger = logger;
    this.callGwtDotCreateBindingProvider = callGwtDotCreateBindingProvider;
    this.callConstructorBinding = callConstructorBinding;
    this.bindClassBindingProvider = bindClassBindingProvider;
    this.implicitProviderBindingProvider = implicitProviderBindingProvider;
    this.asyncProviderBindingProvider = asyncProviderBindingProvider;
    this.bindProviderBindingProvider = bindProviderBindingProvider;
    this.guiceUtil = guiceUtil;
    this.ginjectorInterface = TypeLiteral.get(ginjectorInterface);
    this.lieToGuiceModule = lieToGuiceModule;
    this.remoteServiceProxyBindingProvider = remoteServiceProxyBindingProvider;
    this.ginjectorBindingProvider = ginjectorBindingProvider;
    this.errorManager = errorManager;

    completeCollector = collector;
    completeCollector.setMethodFilter(MemberCollector.ALL_METHOD_FILTER);
  }

  void createImplicitBindingsForUnresolved() throws UnableToCompleteException {
    while (!unresolved.isEmpty() || !unresolvedOptional.isEmpty()) {
      // Iterate through copies because we will modify sets during iteration
      for (Key<?> key : new ArrayList<Key<?>>(unresolved)) {
        createImplicitBindingForUnresolved(key, false);
      }

      for (Key<?> key : new ArrayList<Key<?>>(unresolvedOptional)) {
        createImplicitBindingForUnresolved(key, true);
      }

      errorManager.checkForError();
    }
  }

  private void createImplicitBindingForUnresolved(final Key<?> key, boolean optional) {
    Binding binding = createImplicitBinding(key, optional);

    if (binding != null) {
      logger.log(TreeLogger.TRACE, "Implicit binding for " + key + ": " + binding);
      if (binding instanceof CallGwtDotCreateBinding || binding instanceof GinjectorBinding
          || binding instanceof AsyncProviderBinding){
        // Need to lie to Guice about any implicit GWT.create bindings and
        // ginjector bindings we install that Guice would otherwise not see.
        // http://code.google.com/p/google-gin/issues/detail?id=13
        lieToGuiceModule.registerImplicitBinding(key);
      }

      // TODO(dburrows): provide a summary of why the unresolved binding was
      // created in the first place in its context.
      addBinding(key, new BindingEntry(binding,
          BindingContext.forText("Implicit binding for " + key)));
    } else if (optional) {
      unresolvedOptional.remove(key);
    }
  }

  public Map<Key<?>, BindingEntry> getBindings() {
    return bindings;
  }

  public Map<Key<?>, GinScope> getScopes() {
    return scopes;
  }

  public Set<Class<?>> getStaticInjectionRequests() {
    return staticInjectionRequests;
  }

  public void addMemberInjectRequests(Set<Key<?>> implementations) {
    memberInjectRequests.addAll(implementations);
  }

  public Set<Key<?>> getMemberInjectRequests() {
    return memberInjectRequests;
  }

  void putScope(Key<?> key, GinScope scope) {
    scopes.put(key, scope);
  }

  public GinScope determineScope(Key<?> key) {
    GinScope scope = getScopes().get(key);
    if (scope == null) {
      Class<?> raw = key.getTypeLiteral().getRawType();
      if (raw.getAnnotation(Singleton.class) != null
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

  void addUnresolvedEntriesForInjectorInterface() {
    for (MethodLiteral<?, Method> method : completeCollector.getMethods(ginjectorInterface)) {
      nameGenerator.markAsUsed(method.getName());
      Key<?> key = guiceUtil.getKey(method);
      logger.log(TreeLogger.TRACE, "Add unresolved key from injector interface: " + key);

      // Member inject types do not need to be gin-creatable themselves but we
      // need to provide all dependencies.
      if (guiceUtil.isMemberInject(method)) {
        if (!unresolved.contains(key)) {
          memberInjectRequests.add(key);
          RequiredKeys requiredKeys =
              guiceUtil.getMemberInjectionRequiredKeys(key.getTypeLiteral());
          unresolved.addAll(requiredKeys.getRequiredKeys());
          unresolvedOptional.addAll(requiredKeys.getOptionalKeys());
        }
      } else {
        unresolved.add(key);
      }
    }
  }

  Binding createImplicitBinding(Key<?> key, boolean optional) {
    TypeLiteral<?> type = key.getTypeLiteral();

    // All steps per:
    // http://code.google.com/p/google-guice/wiki/BindingResolution

    // 1. Explicit binding - already finished at this point.

    // This is really an explicit binding, we add it here.
    // TODO(schmitt): Can we just add a binding to the module?
    if (type.equals(ginjectorInterface)) {
      return ginjectorBindingProvider.get();
    }

    // 2. Ask parent injector.
    // TODO(schmitt): Implement parent/child injectors.

    // 3. Ask child injector.
    // TODO(schmitt): Implement parent/child injectors.

    // 4. Provider injections.
    if (isProviderKey(key)) {
      ImplicitProviderBinding binding = implicitProviderBindingProvider.get();
      binding.setProviderKey(key);

      if (optional) {
        // We have to take special measures for optional implicit providers
        // since they are only created/injected if their provided type can be
        // bound.
        return checkOptionalBindingAvailability(binding);
      }

      return binding;

      // TODO(bstoler): Scope the provider binding like the thing being provided?
    }

    // 4b. AsyncProvider injections.
    if (isAsyncProviderKey(key)) {
      AsyncProviderBinding binding = asyncProviderBindingProvider.get();
      binding.setProviderKey(key);

      if (optional) {
        // We have to take special measures for optional implicit providers
        // since they are only created/injected if their provided type can be
        // bound.
        return checkOptionalBindingAvailability(binding);
      }

      return binding;
    }

    // 5. Convert constants.
    // Already covered by resolving explicit bindings.
    if (BindConstantBinding.isConstantKey(key)) {
      if (!optional) {
        errorManager.logError("Binding requested for constant key " + key
            + " but no explicit binding was found.");
      }

      return null;
    }

    // 6. If the dependency has a binding annotation, give up.
    if (key.getAnnotation() != null || key.getAnnotationType() != null) {
      if (!optional) {
        errorManager.logError("No implementation bound for \"" + key
            + "\" and an implicit binding cannot be created because the type is annotated.");
      }

      return null;
    }

    // 7. If the dependency is an array or enum, give up.
    // Covered by step 5 (enum) and 11 (array).

    // 8. Handle TypeLiteral injections.
    // TODO(schmitt): Implement TypeLiteral injections.

    // 9. Use resolution annotations (@ImplementedBy, @ProvidedBy)
    ImplementedBy implementedBy = type.getRawType().getAnnotation(ImplementedBy.class);
    if (implementedBy != null) {
      return createImplementedByBinding(key, implementedBy, optional);
    }

    ProvidedBy providedBy = type.getRawType().getAnnotation(ProvidedBy.class);
    if (providedBy != null) {
      return createProvidedByBinding(key, providedBy, optional);
    }

    // 10. If the dependency is abstract or a non-static inner class, give up.
    // Abstract classes are handled by GWT.create.
    // TODO(schmitt): Introduce check.

    // 11. Use a single @Inject or public no-arguments constructor.
    return createImplicitBindingForClass(type, optional);
  }

  private Binding createImplicitBindingForClass(TypeLiteral<?> type, boolean optional) {
    // Either call the @Inject constructor or use GWT.create
    MethodLiteral<?, Constructor<?>> injectConstructor = getInjectConstructor(type);

    if (injectConstructor != null) {
      CallConstructorBinding binding = callConstructorBinding.get();
      binding.setConstructor(injectConstructor);
      return binding;
    }

    if (hasAccessibleZeroArgConstructor(type)) {
      if (RemoteServiceProxyBinding.isRemoteServiceProxy(type)) {
        RemoteServiceProxyBinding binding = remoteServiceProxyBindingProvider.get();
        binding.setType(type);
        return binding;
      } else {
        CallGwtDotCreateBinding binding = callGwtDotCreateBindingProvider.get();
        binding.setType(type);
        return binding;
      }
    }

    if (!optional) {
      errorManager.logError("No @Inject or default constructor found for " + type);
    }

    return null;
  }

  /**
   * Returns true iff the passed type has a constructor with zero arguments
   * (default constructors included) and that constructor is non-private,
   * excepting constructors for private classes where the constructor may be of
   * any visibility.
   *
   * @param typeLiteral type to be checked for matching constructor
   * @return true if a matching constructor is present on the passed type
   */
  private boolean hasAccessibleZeroArgConstructor(TypeLiteral<?> typeLiteral) {
    Class<?> rawType = typeLiteral.getRawType();
    if (rawType.isInterface()) {
      return true;
    }

    Constructor<?> constructor;
    try {
      constructor = rawType.getDeclaredConstructor();
    } catch (NoSuchMethodException e) {
      return rawType.getDeclaredConstructors().length == 0;
    }

    return !ReflectUtil.isPrivate(constructor) || ReflectUtil.isPrivate(typeLiteral);
  }

  void addBinding(Key<?> key, BindingEntry bindingEntry) {
    if (bindings.containsKey(key)) {
      BindingEntry keyEntry = bindings.get(key);
      errorManager.logError("Double-bound: " + key + ". " + keyEntry.getBindingContext() + ", "
          + bindingEntry.getBindingContext());
      return;
    }

    if (!isClassAccessibleFromGinjector(key.getTypeLiteral())) {
      errorManager.logError("Can not inject an instance of an inaccessible class. Key=" + key);
      return;
    }

    bindings.put(key, bindingEntry);
    unresolved.remove(key);
    unresolvedOptional.remove(key);
    memberInjectRequests.remove(key);

    addRequiredKeys(key, bindingEntry.getBinding().getRequiredKeys());

    logger.log(TreeLogger.TRACE, "bound " + key + " to " + bindingEntry);
  }

  private void addRequiredKeys(Key<?> key, RequiredKeys requiredKeys) {
    // Resolve optional keys.
    // Clone the returned set so we can safely mutate it
    Set<Key<?>> optionalKeys = new HashSet<Key<?>>(requiredKeys.getOptionalKeys());
    optionalKeys.removeAll(bindings.keySet());
    if (!optionalKeys.isEmpty()) {
      logger.log(TreeLogger.TRACE, "Add optional unresolved as dep from binding to "
          + key + ": " + optionalKeys);
      unresolvedOptional.addAll(optionalKeys);
    }

    // Resolve required keys.
    // Clone the returned set so we can safely mutate it
    Set<Key<?>> nowUnresolved = new HashSet<Key<?>>(requiredKeys.getRequiredKeys());
    nowUnresolved.removeAll(bindings.keySet());
    if (!nowUnresolved.isEmpty()) {
      logger.log(TreeLogger.TRACE, "Add unresolved as dep from binding to "
          + key + ": " + nowUnresolved);
      unresolved.addAll(nowUnresolved);
    }
  }

  private <T extends Binding> T checkOptionalBindingAvailability(T binding) {
    RequiredKeys requiredKeys = binding.getRequiredKeys();

    assert(requiredKeys.getOptionalKeys().isEmpty());

    // Find out whether all requirements of this provider can be satisfied.
    Set<Key<?>> unresolved = new HashSet<Key<?>>(requiredKeys.getRequiredKeys());
    unresolved.removeAll(bindings.keySet());
    for (Key<?> requiredKey : unresolved) {

      // Note: This call doesn't cause a binding to be registered.
      if (createImplicitBinding(requiredKey, true) == null) {

        // A dependency cannot be constructed, this binding is not available.
        return null;
      }
    }

    return binding;
  }

  private BindClassBinding createImplementedByBinding(Key<?> key, ImplementedBy implementedBy,
      boolean optional) {
    Class<?> rawType = key.getTypeLiteral().getRawType();
    Class<?> implementationType = implementedBy.value();

    if (implementationType == rawType) {
      errorManager.logError("@ImplementedBy points to the same class it annotates: " + rawType);
      return null;
    }

    if (!rawType.isAssignableFrom(implementationType)) {
      errorManager.logError(implementationType + " doesn't extend " + rawType
          + " (while resolving @ImplementedBy)");
      return null;
    }

    BindClassBinding implementedByBinding = bindClassBindingProvider.get();
    implementedByBinding.setBoundClassKey(Key.get(implementationType));

    if (optional) {
      return checkOptionalBindingAvailability(implementedByBinding);
    }

    return implementedByBinding;
  }

  private BindProviderBinding createProvidedByBinding(Key<?> key, ProvidedBy providedBy,
      boolean optional) {
    Class<?> rawType = key.getTypeLiteral().getRawType();
    Class<? extends Provider<?>> providerType = providedBy.value();

    if (providerType == rawType) {
      errorManager.logError("@ProvidedBy points to the same class it annotates: " + rawType);
      return null;
    }

    BindProviderBinding implementedByBinding = bindProviderBindingProvider.get();
    implementedByBinding.setProviderKey(Key.get(providerType));

    if (optional) {
      return checkOptionalBindingAvailability(implementedByBinding);
    }

    return implementedByBinding;
  }

  private boolean isProviderKey(Key<?> key) {
    Type keyType = key.getTypeLiteral().getType();
    return keyType instanceof ParameterizedType &&
        (((ParameterizedType) keyType).getRawType() == Provider.class
            || ((ParameterizedType) keyType).getRawType() == com.google.inject.Provider.class);
  }

  private boolean isAsyncProviderKey(Key<?> key) {
      Type keyType = key.getTypeLiteral().getType();
      return keyType instanceof ParameterizedType &&
          ((ParameterizedType) keyType).getRawType() == AsyncProvider.class;
  }

  private boolean isClassAccessibleFromGinjector(TypeLiteral<?> type) {
    if (ReflectUtil.isPublic(type)) {
      return true;
    }

    Package classPackage = type.getRawType().getPackage();
    Package ginjectorPackage = ginjectorInterface.getRawType().getPackage();
    return classPackage == ginjectorPackage;
  }

  private MethodLiteral<?, Constructor<?>> getInjectConstructor(TypeLiteral<?> type) {
    Constructor<?>[] constructors = type.getRawType().getDeclaredConstructors();
    MethodLiteral<?, Constructor<?>> injectConstructor = null;
    for (Constructor<?> constructor : constructors) {
      MethodLiteral<?, Constructor<?>> constructorLiteral = MethodLiteral.get(constructor, type);
      if (GuiceUtil.hasInject(constructorLiteral)) {
        if (injectConstructor != null) {
          errorManager.logError(String.format("More than one @Inject constructor found for %s; %s, %s", type,
              injectConstructor, constructor));
          return null;
        }
        injectConstructor = constructorLiteral;
      }
    }
    return injectConstructor;
  }

  void addStaticInjectionRequest(Class<?> type, List<Message> messages) {
    staticInjectionRequests.add(type);

    // Calculate required bindings and add to unresolved.
    Set<Key<?>> unresolved = new HashSet<Key<?>>();
    Set<Key<?>> unresolvedOptional = new HashSet<Key<?>>();
    for (InjectionPoint injectionPoint : InjectionPoint.forStaticMethodsAndFields(type)) {
      Member member = injectionPoint.getMember();
      if (member instanceof Method) {
        RequiredKeys keys = guiceUtil.getRequiredKeys(
            MethodLiteral.get((Method) member, TypeLiteral.get(type)));
        unresolved.addAll(keys.getRequiredKeys());
        unresolvedOptional.addAll(keys.getOptionalKeys());
      } else if (member instanceof Field) {
        FieldLiteral<?> field = FieldLiteral.get((Field) member, TypeLiteral.get(type));
        Key<?> key = guiceUtil.getKey(field);
        if (guiceUtil.isOptional(field)) {
          unresolvedOptional.add(key);
        } else {
          unresolved.add(key);
        }
      }
    }
    addRequiredKeys(Key.get(type), new RequiredKeys(unresolved, unresolvedOptional));
  }
}
