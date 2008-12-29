/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.inject.client.GinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.rebind.adapter.GinModuleAdapter;
import com.google.gwt.inject.rebind.binding.BindClassBinding;
import com.google.gwt.inject.rebind.binding.BindConstantBinding;
import com.google.gwt.inject.rebind.binding.BindProviderBinding;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.CallConstructorBinding;
import com.google.gwt.inject.rebind.binding.CallGwtDotCreateBinding;
import com.google.gwt.inject.rebind.binding.ImplicitProviderBinding;
import com.google.gwt.inject.rebind.util.KeyUtil;
import com.google.gwt.inject.rebind.util.MemberCollector;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.google.inject.spi.BindingScopingVisitor;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.google.inject.spi.Message;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds up the bindings and scopes for this {@code Ginjector}.
 */
@Singleton
class BindingsProcessor {
  private final TreeLogger logger;
  private final GeneratorContext ctx;

  /**
   * Generates names for code we produce to resolve injection requests.
   */
  private final NameGenerator nameGenerator;

  /**
   * Map from key to binding for all types we already have a binding for.
   */
  private final Map<Key<?>, Binding> bindings = new HashMap<Key<?>, Binding>();

  /**
   * Map from key to scope for all types we have a binding for.
   */
  private final Map<Key<?>, GinScope> scopes = new HashMap<Key<?>, GinScope>();

  /**
   * Set of keys for classes that we still need to resolve. Every time a
   * binding is added to {@code bindings}, the key is removed from this set.
   * When this set becomes empty, we know we've satisfied all dependencies.
   */
  private final Set<Key<?>> unresolved = new HashSet<Key<?>>();

  /**
   * Collector that gathers all methods from an injector.
   */
  private final MemberCollector completeCollector;

  /**
   * Provider for {@link com.google.gwt.inject.rebind.binding.CallGwtDotCreateBinding}s.
   */
  private final Provider<CallGwtDotCreateBinding> callGwtDotCreateBindingProvider;

  /**
   * Provider for {@link com.google.gwt.inject.rebind.binding.CallConstructorBinding}s.
   */
  private final Provider<CallConstructorBinding> callConstructorBinding;

  /**
   * Provider for {@link com.google.gwt.inject.rebind.binding.BindClassBinding}s.
   */
  private final Provider<BindClassBinding> bindClassBindingProvider;

  /**
   * Provider for {@link com.google.gwt.inject.rebind.binding.BindProviderBinding}s.
   */
  private final Provider<BindProviderBinding> bindProviderBindingProvider;

  /**
   * Provider for {@link com.google.gwt.inject.rebind.binding.ImplicitProviderBinding}s.
   */
  private final Provider<ImplicitProviderBinding> implicitProviderBindingProvider;

  private final KeyUtil keyUtil;

  /**
   * Interface of the injector that this class is implementing.
   */
  private final JClassType ginjectorInterface;

  /**
   * Keeps track of whether we've found an error so we can eventually throw
   * an {@link com.google.gwt.core.ext.UnableToCompleteException}. We do this instead of throwing
   * immediately so that we can find more than one error per compilation cycle.
   */
  private boolean foundError = false;

  @Inject
  BindingsProcessor(NameGenerator nameGenerator, TreeLogger logger,
      Provider<MemberCollector> collectorProvider,
      Provider<CallGwtDotCreateBinding> callGwtDotCreateBindingProvider,
      Provider<CallConstructorBinding> callConstructorBinding,
      KeyUtil keyUtil, GeneratorContext ctx,
      Provider<BindClassBinding> bindClassBindingProvider,
      Provider<BindProviderBinding> bindProviderBindingProvider,
      Provider<ImplicitProviderBinding> implicitProviderBindingProvider,
      @GinjectorInterfaceType JClassType ginjectorInterface) {
    this.nameGenerator = nameGenerator;
    this.logger = logger;
    this.callGwtDotCreateBindingProvider = callGwtDotCreateBindingProvider;
    this.callConstructorBinding = callConstructorBinding;
    this.bindClassBindingProvider = bindClassBindingProvider;
    this.implicitProviderBindingProvider = implicitProviderBindingProvider;
    this.bindProviderBindingProvider = bindProviderBindingProvider;
    this.keyUtil = keyUtil;
    this.ctx = ctx;
    this.ginjectorInterface = ginjectorInterface;

    completeCollector = collectorProvider.get();
    completeCollector.setMethodFilter(MemberCollector.ALL_METHOD_FILTER);
  }

  public void process() throws UnableToCompleteException {
    validateMethods();

    addUnresolvedEntriesForInjectorInterface();

    createBindingsForModules(ginjectorInterface);

    while (!unresolved.isEmpty()) {
      // Iterate through a copy because we will modify it during iteration
      for (Key<?> key : new ArrayList<Key<?>>(unresolved)) {
        createImplicitBinding(key);
      }

      if (foundError) {
        throw new UnableToCompleteException();
      }
    }
  }

  public Map<Key<?>, Binding> getBindings() {
    return bindings;
  }

  public Map<Key<?>, GinScope> getScopes() {
    return scopes;
  }

  public GinScope determineScope(Key<?> key) {
    GinScope scope = getScopes().get(key);
    if (scope == null) {
      scope = GinScope.NO_SCOPE;
    }

    if (scope == GinScope.NO_SCOPE) {
      // Look for scope annotation as a fallback
      Class<?> raw = keyUtil.getRawType(key);
      if (raw.getAnnotation(Singleton.class) != null) {
        scope = GinScope.SINGLETON;
      }
    }

    logger.log(TreeLogger.TRACE, "scope for " + key + ": " + scope);
    return scope;
  }

  private void validateMethods() throws UnableToCompleteException {
    for (JMethod method : completeCollector.getMethods(ginjectorInterface)) {
      if (method.getParameters().length > 1) {
        logger.log(TreeLogger.Type.ERROR, "Injector methods cannot have more than one parameter, "
            + " found: " + method.getReadableDeclaration());
        foundError = true;
      }

      // Member inject method.
      if (method.getParameters().length == 1) {
        if (method.getParameters()[0].getType().isClassOrInterface() == null) {
          logger.log(TreeLogger.Type.ERROR, "Injector method parameter types must be a class or "
              + "interface, found: " + method.getReadableDeclaration());
          foundError = true;
        }

        if (method.getReturnType() != JPrimitiveType.VOID) {
          logger.log(TreeLogger.Type.ERROR, "Injector methods with a parameter must have a void "
              + "return type, found: " + method.getReadableDeclaration());
          foundError = true;
        }

      // Constructor injection.
      } else if (method.getReturnType().isClassOrInterface() == null) {
        logger.log(TreeLogger.Type.ERROR, "Injector methods with no parameters must return a class "
            + "or interface, found: " + method.getReadableDeclaration());
        foundError = true;
      }
    }

    if (foundError) {
      throw new UnableToCompleteException();
    }
  }

  private void addUnresolvedEntriesForInjectorInterface() {
    for (JMethod method : completeCollector.getMethods(ginjectorInterface)) {
      Key<?> key = keyUtil.getKey(method);
      logger.log(TreeLogger.TRACE, "Add unresolved key from injector interface: " + key);

      nameGenerator.markAsUsed(method.getName());

      unresolved.add(key);
    }
  }

  private void createBindingsForModules(JClassType injectorInterface)
      throws UnableToCompleteException {
    List<Module> modules = new ArrayList<Module>();
    populateModulesFromInjectorInterface(injectorInterface, modules);

    if (!modules.isEmpty()) {
      // Validate module consistency using Guice.
      try {
        Guice.createInjector(Stage.TOOL, modules);
      } catch (Exception e) {
        logger.log(TreeLogger.ERROR, "Errors from Guice: " + e.getMessage(), e);
        throw new UnableToCompleteException();
      }

      List<Element> elements = Elements.getElements(modules);
      for (Element element : elements) {
        GuiceElementVisitor visitor = new GuiceElementVisitor();
        element.acceptVisitor(visitor);

        // Capture any binding errors, any of which we treat as fatal
        List<Message> messages = visitor.getMessages();
        if (!messages.isEmpty()) {
          foundError = true;

          for (Message message : messages) {
            // tostring has both source and message so use that
            logger.log(TreeLogger.ERROR, message.toString(), message.getCause());
          }
        }
      }
    }
  }

  private void populateModulesFromInjectorInterface(JClassType iface, List<Module> modules) {
    GinModules gmodules = iface.getAnnotation(GinModules.class);
    if (gmodules != null) {
      for (Class<? extends GinModule> moduleClass : gmodules.value()) {
        Module module = instantiateGModuleClass(moduleClass);
        if (module != null) {
          modules.add(module);
        }
      }
    }

    for (JClassType superIface : iface.getImplementedInterfaces()) {
      populateModulesFromInjectorInterface(superIface, modules);
    }
  }

  private Module instantiateGModuleClass(Class<? extends GinModule> moduleClassName) {
    try {
      Constructor<? extends GinModule> constructor = moduleClassName.getDeclaredConstructor();
      try {
        constructor.setAccessible(true);
        return new GinModuleAdapter(constructor.newInstance());
      } finally {
        constructor.setAccessible(false);
      }
    } catch (IllegalAccessException e) {
      logger.log(TreeLogger.ERROR, "Error creating module: " + moduleClassName, e);
    } catch (InstantiationException e) {
      logger.log(TreeLogger.ERROR, "Error creating module: " + moduleClassName, e);
    } catch (NoSuchMethodException e) {
      logger.log(TreeLogger.ERROR, "Error creating module: " + moduleClassName, e);
    } catch (InvocationTargetException e) {
      logger.log(TreeLogger.ERROR, "Error creating module: " + moduleClassName, e);
    }

    foundError = true;
    return null;
  }

  private void createImplicitBinding(Key<?> key) {
    Binding binding = null;
    Type keyType = key.getTypeLiteral().getType();

    if (keyType instanceof ParameterizedType) {
      ParameterizedType keyParamType = (ParameterizedType) keyType;

      // Create implicit Provider<T> binding
      if (keyParamType.getRawType() == Provider.class) {
        binding = implicitProviderBindingProvider.get();
        ((ImplicitProviderBinding) binding).setProviderKey(key);

        // TODO(bstoler): Scope the provider binding like the thing being provided?
      }
    }

    if (binding == null) {
      JClassType classType = ctx.getTypeOracle().findType(
          nameGenerator.binaryNameToSourceName(keyUtil.getRawType(key).getName()));
      if (classType != null) {
        binding = createImplicitBindingForClass(classType);
      } else {
        logger.log(TreeLogger.ERROR, "Class not found: " + key);
        foundError = true;
      }
    }

    logger.log(TreeLogger.TRACE, "Implicit binding for " + key + ": " + binding);

    if (binding != null) {
      addBinding(key, binding);
    }
  }

  private Binding createImplicitBindingForClass(JClassType classType) {
    // Either call the @Inject constructor or use GWT.create
    JConstructor[] constructors = classType.getConstructors();
    if (constructors.length == 0 ||
        (constructors.length == 1 && constructors[0].getParameters().length == 0)) {
      CallGwtDotCreateBinding binding = callGwtDotCreateBindingProvider.get();
      binding.setClassType(classType);
      return binding;
    } else {
      JConstructor constructor = getInjectConstructor(classType);
      if (constructor != null) {
        CallConstructorBinding binding = callConstructorBinding.get();
        binding.setConstructor(constructor);
        return binding;
      }
    }

    logger.log(TreeLogger.ERROR, "No @Inject or default constructor found for " + classType);
    foundError = true;
    return null;
  }

  private void addBinding(Key<?> key, Binding binding) {
    if (bindings.containsKey(key)) {
      logger.log(TreeLogger.ERROR, "Double-bound: " + key + ". "
          + bindings.get(key) + ", " + binding);
      foundError = true;
      return;
    }

    bindings.put(key, binding);
    unresolved.remove(key);

    // Clone the returned set so we can safely mutate it
    Set<Key<?>> nowUnresolved = new HashSet<Key<?>>(binding.getRequiredKeys());
    nowUnresolved.removeAll(bindings.keySet());

    if (!nowUnresolved.isEmpty()) {
      logger.log(TreeLogger.TRACE, "Add unresolved as dep from binding to "
          + key + ": " + nowUnresolved);
      unresolved.addAll(nowUnresolved);
    }

    logger.log(TreeLogger.TRACE, "bound " + key + " to " + binding);
  }

  private JConstructor getInjectConstructor(JClassType classType) {
    JConstructor[] constructors = classType.getConstructors();

    JConstructor injectConstructor = null;
    for (JConstructor constructor : constructors) {
      if (constructor.getAnnotation(Inject.class) != null) {
        if (injectConstructor == null) {
          injectConstructor = constructor;
        } else {
          logger.log(TreeLogger.ERROR, "More than one @Inject constructor found for "
              + classType + "; " + injectConstructor + ", " + constructor);
          foundError = true;
        }
      }
    }

    return injectConstructor;
  }

  private class GuiceElementVisitor extends DefaultElementVisitor<Void> {
    private final List<Message> messages = new ArrayList<Message>();

    @Override
    public <T> Void visitBinding(com.google.inject.Binding<T> command) {
      GuiceBindingVisitor<T> bindingVisitor = new GuiceBindingVisitor<T>(command.getKey());
      command.acceptTargetVisitor(bindingVisitor);
      command.acceptScopingVisitor(bindingVisitor);
      return null;
    }

    @Override
    public Void visitMessage(Message message) {
      messages.add(message);
      return null;
    }

    @Override
    protected Void visitElement(Element element) {
      visitMessage(new Message(element.getSource(),
          "Ignoring unsupported Module element: " + element));
      return null;
    }

    public List<Message> getMessages() {
      return messages;
    }
  }

  private class GuiceBindingVisitor<T> extends DefaultBindingTargetVisitor<T, Void>
      implements BindingScopingVisitor<Void> {
    private final Key<T> targetKey;

    public GuiceBindingVisitor(Key<T> targetKey) {
      this.targetKey = targetKey;
    }

    @Override
    public Void visitProviderKey(Key<? extends Provider<? extends T>> providerKey) {
      BindProviderBinding binding = bindProviderBindingProvider.get();
      binding.setProviderKey(providerKey);
      addBinding(targetKey, binding);
      return null;
    }

    @Override
    public Void visitKey(Key<? extends T> key) {
      BindClassBinding binding = bindClassBindingProvider.get();
      binding.setBoundClassKey(key);
      addBinding(targetKey, binding);
      return null;
    }

    @Override
    public Void visitInstance(T instance, Set<com.google.inject.spi.InjectionPoint> injectionPoints) {
      return visitInstance(instance);
    }

    public Void visitInstance(T instance) {
      Binding binding = BindConstantBinding.create(targetKey, instance);
      if (binding != null) {
        addBinding(targetKey, binding);
      } else {
        logger.log(TreeLogger.ERROR, "Instance binding not supported; key="
            + targetKey + " inst=" + instance);
        foundError = true;
      }

      return null;
    }

    @Override
    public Void visitUntargetted() {
      // Do nothing -- this just means to use our normal implicit strategies
      // We need this override to avoid giving an error due to visitOther.
      return null;
    }

    @Override
    protected Void visitOther() {
      logger.log(TreeLogger.ERROR, "Unsupported binding provided for key: " + targetKey);
      foundError = true;
      return null;
    }

    public Void visitEagerSingleton() {
      scopes.put(targetKey, GinScope.EAGER_SINGLETON);
      return null;
    }

    public Void visitScope(Scope scope) {
      logger.log(TreeLogger.ERROR, "Explicit scope unsupported: key=" + targetKey
          + " scope=" + scope);
      foundError = true;
      return null;
    }

    public Void visitScopeAnnotation(Class<? extends Annotation> scopeAnnotation) {
      if (scopeAnnotation == Singleton.class) {
        scopes.put(targetKey, GinScope.SINGLETON);
      } else {
        logger.log(TreeLogger.ERROR, "Unsupported scope annoation: key=" + targetKey
            + " scope=" + scopeAnnotation);
        foundError = true;
      }
      return null;
    }

    public Void visitNoScoping() {
      scopes.put(targetKey, GinScope.NO_SCOPE);
      return null;
    }
  }
}