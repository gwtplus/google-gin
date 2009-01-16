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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.inject.client.GinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.rebind.adapter.GinModuleAdapter;
import com.google.gwt.inject.rebind.adapter.GwtDotCreateProvider;
import com.google.gwt.inject.rebind.binding.BindClassBinding;
import com.google.gwt.inject.rebind.binding.BindConstantBinding;
import com.google.gwt.inject.rebind.binding.BindProviderBinding;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.CallConstructorBinding;
import com.google.gwt.inject.rebind.binding.CallGwtDotCreateBinding;
import com.google.gwt.inject.rebind.binding.ImplicitProviderBinding;
import com.google.gwt.inject.rebind.binding.ProviderMethodBinding;
import com.google.gwt.inject.rebind.binding.RemoteServiceProxyBinding;
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
import com.google.inject.internal.ProviderMethod;
import com.google.inject.spi.BindingScopingVisitor;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.google.inject.spi.InstanceBinding;
import com.google.inject.spi.LinkedKeyBinding;
import com.google.inject.spi.Message;
import com.google.inject.spi.ProviderInstanceBinding;
import com.google.inject.spi.ProviderKeyBinding;
import com.google.inject.spi.ProviderLookup;
import com.google.inject.spi.UntargettedBinding;

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

  private final Provider<CallGwtDotCreateBinding> callGwtDotCreateBindingProvider;
  private final Provider<RemoteServiceProxyBinding> remoteServiceProxyBindingProvider;
  private final Provider<CallConstructorBinding> callConstructorBinding;
  private final Provider<BindClassBinding> bindClassBindingProvider;
  private final Provider<BindProviderBinding> bindProviderBindingProvider;
  private final Provider<ImplicitProviderBinding> implicitProviderBindingProvider;
  private final Provider<ProviderMethodBinding> providerMethodBindingProvider;
  private final Provider<BindConstantBinding> bindConstantBindingProvider;

  private final KeyUtil keyUtil;

  /**
   * Interface of the injector that this class is implementing.
   */
  private final JClassType ginjectorInterface;

  private final LieToGuiceModule lieToGuiceModule;

  /**
   * Keeps track of whether we've found an error so we can eventually throw
   * an {@link UnableToCompleteException}. We do this instead of throwing
   * immediately so that we can find more than one error per compilation cycle.
   */
  private boolean foundError = false;

  @Inject
  BindingsProcessor(NameGenerator nameGenerator, TreeLogger logger,
      Provider<MemberCollector> collectorProvider,
      Provider<CallGwtDotCreateBinding> callGwtDotCreateBindingProvider,
      Provider<CallConstructorBinding> callConstructorBinding,
      KeyUtil keyUtil,
      Provider<BindClassBinding> bindClassBindingProvider,
      Provider<BindProviderBinding> bindProviderBindingProvider,
      Provider<ImplicitProviderBinding> implicitProviderBindingProvider,
      @GinjectorInterfaceType JClassType ginjectorInterface,
      LieToGuiceModule lieToGuiceModule,
      Provider<BindConstantBinding> bindConstantBindingProvider,
      Provider<RemoteServiceProxyBinding> remoteServiceProxyBindingProvider,
      Provider<ProviderMethodBinding> providerMethodBindingProvider) {
    this.nameGenerator = nameGenerator;
    this.logger = logger;
    this.callGwtDotCreateBindingProvider = callGwtDotCreateBindingProvider;
    this.callConstructorBinding = callConstructorBinding;
    this.bindClassBindingProvider = bindClassBindingProvider;
    this.implicitProviderBindingProvider = implicitProviderBindingProvider;
    this.bindProviderBindingProvider = bindProviderBindingProvider;
    this.keyUtil = keyUtil;
    this.ginjectorInterface = ginjectorInterface;
    this.lieToGuiceModule = lieToGuiceModule;
    this.remoteServiceProxyBindingProvider = remoteServiceProxyBindingProvider;
    this.bindConstantBindingProvider = bindConstantBindingProvider;
    this.providerMethodBindingProvider = providerMethodBindingProvider;

    completeCollector = collectorProvider.get();
    completeCollector.setMethodFilter(MemberCollector.ALL_METHOD_FILTER);
  }

  public void process() throws UnableToCompleteException {
    validateMethods();
    addUnresolvedEntriesForInjectorInterface();

    List<Module> modules = createModules();

    createBindingsForModules(modules);
    createImplicitBindingsForUnresolved();
    validateModulesUsingGuice(modules);
  }

  private void createImplicitBindingsForUnresolved() throws UnableToCompleteException {
    while (!unresolved.isEmpty()) {
      // Iterate through a copy because we will modify it during iteration
      for (Key<?> key : new ArrayList<Key<?>>(unresolved)) {
        Binding binding = createImplicitBinding(key);

        if (binding != null) {
          if (binding instanceof CallGwtDotCreateBinding) {
            // Need to lie to Guice about any implicit GWT.create bindings
            // we install that Guice would otherwise not see.
            // http://code.google.com/p/google-gin/issues/detail?id=13
            lieToGuiceModule.registerImplicitBinding(key);
          }

          addBinding(key, binding);
        }
      }

      checkForError();
    }
  }

  private void checkForError() throws UnableToCompleteException {
    if (foundError) {
      throw new UnableToCompleteException();
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
      Class<?> raw = keyUtil.getRawType(key);
      if (raw.getAnnotation(Singleton.class) != null) {
        // Look for scope annotation as a fallback
        scope = GinScope.SINGLETON;
      } else if (RemoteServiceProxyBinding.isRemoteServiceProxy(keyUtil.getRawClassType(key))) {
        // Special case for remote services
        scope = GinScope.SINGLETON;
      } else {
        scope = GinScope.NO_SCOPE;
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

      if (method.getParameters().length == 1) {
        // Member inject method.
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
      } else if (method.getReturnType().isClassOrInterface() == null) {
        // Constructor injection.
        logger.log(TreeLogger.Type.ERROR, "Injector methods with no parameters must return a class "
            + "or interface, found: " + method.getReadableDeclaration());
        foundError = true;
      }
    }

    checkForError();
  }

  private void addUnresolvedEntriesForInjectorInterface() {
    for (JMethod method : completeCollector.getMethods(ginjectorInterface)) {
      Key<?> key = keyUtil.getKey(method);
      logger.log(TreeLogger.TRACE, "Add unresolved key from injector interface: " + key);

      nameGenerator.markAsUsed(method.getName());

      unresolved.add(key);
    }
  }

  private void createBindingsForModules(List<Module> modules) throws UnableToCompleteException {
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

  private List<Module> createModules() {
    List<Module> modules = new ArrayList<Module>();
    populateModulesFromInjectorInterface(ginjectorInterface, modules);
    return modules;
  }

  private void validateModulesUsingGuice(List<Module> modules) throws UnableToCompleteException {
    // Validate module consistency using Guice.
    try {
      List<Module> modulesForGuice = new ArrayList<Module>(modules.size() + 1);
      modulesForGuice.add(lieToGuiceModule);
      modulesForGuice.addAll(modules);
      Guice.createInjector(Stage.TOOL, modulesForGuice);
    } catch (Exception e) {
      logger.log(TreeLogger.ERROR, "Errors from Guice: " + e.getMessage(), e);
      throw new UnableToCompleteException();
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

  private Binding createImplicitBinding(Key<?> key) {
    Binding binding = null;
    Type keyType = key.getTypeLiteral().getType();

    if (BindConstantBinding.isConstantKey(key)) {
      logger.log(TreeLogger.Type.ERROR, "Binding requested for constant key " + key
          + " but no explicit binding was found.");
      foundError = true;
      return null;
    }

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
      // Only use implicit GWT.create binding if there is no binding annotation.
      // This is compatible with Guice.
      if (key.getAnnotation() != null || key.getAnnotationType() != null) {
        logger.log(TreeLogger.Type.ERROR, "No implementation bound for key " + key);
        foundError = true;
      } else {
        JClassType classType = keyUtil.getRawClassType(key);
        if (classType != null) {
          binding = createImplicitBindingForClass(classType);
        } else {
          logger.log(TreeLogger.ERROR, "Class not found: " + key);
          foundError = true;
        }
      }
    }

    logger.log(TreeLogger.TRACE, "Implicit binding for " + key + ": " + binding);
    return binding;
  }

  private Binding createImplicitBindingForClass(JClassType classType) {
    // Either call the @Inject constructor or use GWT.create
    if (hasZeroArgConstructor(classType)) {
      if (RemoteServiceProxyBinding.isRemoteServiceProxy(classType)) {
        RemoteServiceProxyBinding binding = remoteServiceProxyBindingProvider.get();
        binding.setClassType(classType);
        return binding;
      } else {
        CallGwtDotCreateBinding binding = callGwtDotCreateBindingProvider.get();
        binding.setClassType(classType);
        return binding;
      }
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

  private boolean hasZeroArgConstructor(JClassType classType) {
    JConstructor[] constructors = classType.getConstructors();
    return constructors.length == 0
        || (constructors.length == 1 && constructors[0].getParameters().length == 0);
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
      GuiceBindingVisitor<T> bindingVisitor = new GuiceBindingVisitor<T>(command.getKey(),
          messages);
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
    public <T> Void visitProviderLookup(ProviderLookup<T> providerLookup) {
      // Ignore provider lookups for now
      // TODO(bstoler): I guess we should error if you try to lookup a provider
      // that is not bound?
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
    private final List<Message> messages;

    public GuiceBindingVisitor(Key<T> targetKey, List<Message> messages) {
      this.targetKey = targetKey;
      this.messages = messages;
    }

    @Override
    public Void visitProviderKey(ProviderKeyBinding<? extends T> providerKeyBinding) {
      BindProviderBinding binding = bindProviderBindingProvider.get();
      binding.setProviderKey(providerKeyBinding.getProviderKey());
      addBinding(targetKey, binding);
      return null;
    }

    @Override
    public Void visitProviderInstance(
        ProviderInstanceBinding<? extends T> providerInstanceBinding) {
      // Detect provider methods and handle them
      // TODO(bstoler): Update this when the SPI explicitly has a case for provider methods
      Provider<? extends T> provider = providerInstanceBinding.getProviderInstance();
      if (provider instanceof ProviderMethod) {
        ProviderMethodBinding binding = providerMethodBindingProvider.get();
        try {
          binding.setProviderMethod((ProviderMethod) provider);
          addBinding(targetKey, binding);
        } catch (UnableToCompleteException e) {
          messages.add(new Message(providerInstanceBinding.getSource(),
              "Error processing provider method"));
        }
        return null;
      }

      if (provider instanceof GwtDotCreateProvider) {
        addImplicitBinding();
        return null;
      }

      // OTt, use the normal default handler (and error)
      return super.visitProviderInstance(providerInstanceBinding);
    }

    @Override
    public Void visitLinkedKey(LinkedKeyBinding<? extends T> linkedKeyBinding) {
      BindClassBinding binding = bindClassBindingProvider.get();
      binding.setBoundClassKey(linkedKeyBinding.getLinkedKey());
      addBinding(targetKey, binding);
      return null;
    }

    @Override
    public Void visitInstance(InstanceBinding<? extends T> instanceBinding) {
      T instance = instanceBinding.getInstance();
      if (BindConstantBinding.isConstantKey(targetKey)) {
        BindConstantBinding binding = bindConstantBindingProvider.get();
        binding.setKeyAndInstance(targetKey, instance);
        addBinding(targetKey, binding);
      } else {
        messages.add(new Message(instanceBinding.getSource(),
            "Instance binding not supported; key=" + targetKey + " inst=" + instance));
      }

      return null;
    }

    @Override
    public Void visitUntargetted(UntargettedBinding<? extends T> untargettedBinding) {
      addImplicitBinding();

      return null;
    }

    private void addImplicitBinding() {
      // Register a Gin binding for the default-case binding that
      // Guice saw. We need to register this to avoid later adding
      // this key to the Guice-lies module, which would make it
      // double bound.
      addBinding(targetKey, createImplicitBinding(targetKey));
    }

    @Override
    protected Void visitOther(com.google.inject.Binding<? extends T> binding) {
      messages.add(new Message(binding.getSource(),
          "Unsupported binding provided for key: " + targetKey + ": " + binding));
      return null;
    }

    public Void visitEagerSingleton() {
      scopes.put(targetKey, GinScope.EAGER_SINGLETON);
      return null;
    }

    public Void visitScope(Scope scope) {
      messages.add(new Message("Explicit scope unsupported: key=" + targetKey
          + " scope=" + scope));
      return null;
    }

    public Void visitScopeAnnotation(Class<? extends Annotation> scopeAnnotation) {
      if (scopeAnnotation == Singleton.class) {
        scopes.put(targetKey, GinScope.SINGLETON);
      } else {
        messages.add(new Message("Unsupported scope annoation: key=" + targetKey
            + " scope=" + scopeAnnotation));
      }
      return null;
    }

    public Void visitNoScoping() {
      scopes.put(targetKey, GinScope.NO_SCOPE);
      return null;
    }
  }
}
