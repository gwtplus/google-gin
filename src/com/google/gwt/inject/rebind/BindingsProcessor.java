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
import com.google.gwt.inject.client.GinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.assistedinject.FactoryModule;
import com.google.gwt.inject.rebind.adapter.GinModuleAdapter;
import com.google.gwt.inject.rebind.adapter.GwtDotCreateProvider;
import com.google.gwt.inject.rebind.binding.AsyncProviderBinding;
import com.google.gwt.inject.rebind.binding.BindClassBinding;
import com.google.gwt.inject.rebind.binding.BindConstantBinding;
import com.google.gwt.inject.rebind.binding.BindProviderBinding;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.BindingContext;
import com.google.gwt.inject.rebind.binding.CallConstructorBinding;
import com.google.gwt.inject.rebind.binding.CallGwtDotCreateBinding;
import com.google.gwt.inject.rebind.binding.FactoryBinding;
import com.google.gwt.inject.rebind.binding.ImplicitProviderBinding;
import com.google.gwt.inject.rebind.binding.ProviderMethodBinding;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.inject.rebind.util.MemberCollector;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Scope;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
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
import com.google.inject.spi.StaticInjectionRequest;
import com.google.inject.spi.UntargettedBinding;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;

/**
 * Builds up the bindings and scopes for this {@code Ginjector}.  This uses
 * Guice SPI to inspect the modules and build up details about the necessary
 * bindings in the {@link BindingCollection}. 
 */
@Singleton
class BindingsProcessor {
  private final TreeLogger logger;

  /**
   * Collector that gathers all methods from an injector.
   */
  private final MemberCollector completeCollector;

  private final Provider<BindClassBinding> bindClassBindingProvider;
  private final Provider<BindProviderBinding> bindProviderBindingProvider;
  private final Provider<ProviderMethodBinding> providerMethodBindingProvider;
  private final Provider<BindConstantBinding> bindConstantBindingProvider;
  private final Provider<FactoryBinding> factoryBindingProvider;

  /**
   * Collection of all factory modules configured in gin modules.
   */
  private final Set<FactoryModule<?>> factoryModules = new HashSet<FactoryModule<?>>();

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

  private final BindingCollection bindingsCollection;

  @Inject
  BindingsProcessor(TreeLogger logger,
      Provider<MemberCollector> collectorProvider,
      Provider<CallGwtDotCreateBinding> callGwtDotCreateBindingProvider,
      Provider<CallConstructorBinding> callConstructorBinding,
      Provider<BindClassBinding> bindClassBindingProvider,
      Provider<BindProviderBinding> bindProviderBindingProvider,
      Provider<ImplicitProviderBinding> implicitProviderBindingProvider,
      Provider<AsyncProviderBinding> asyncProviderBindingProvider,
      @GinjectorInterfaceType Class<? extends Ginjector> ginjectorInterface,
      LieToGuiceModule lieToGuiceModule,
      Provider<BindConstantBinding> bindConstantBindingProvider,
      Provider<ProviderMethodBinding> providerMethodBindingProvider,
      Provider<FactoryBinding> factoryBindingProvider,
      ErrorManager errorManager,
      BindingCollection bindingsCollection) {
    this.logger = logger;
    this.bindClassBindingProvider = bindClassBindingProvider;
    this.bindProviderBindingProvider = bindProviderBindingProvider;
    this.ginjectorInterface = TypeLiteral.get(ginjectorInterface);
    this.lieToGuiceModule = lieToGuiceModule;
    this.bindConstantBindingProvider = bindConstantBindingProvider;
    this.providerMethodBindingProvider = providerMethodBindingProvider;
    this.factoryBindingProvider = factoryBindingProvider;
    this.errorManager = errorManager;
    this.bindingsCollection = bindingsCollection;

    completeCollector = collectorProvider.get();
    completeCollector.setMethodFilter(MemberCollector.ALL_METHOD_FILTER);
  }

  public void process() throws UnableToCompleteException {
    validateMethods();
    bindingsCollection.addUnresolvedEntriesForInjectorInterface();

    List<Module> modules = createModules();

    createBindingsForModules(modules);
    createBindingsForFactories();
    bindingsCollection.createImplicitBindingsForUnresolved();
    validateModulesUsingGuice(modules);
  }

  private void createBindingsForFactories() throws UnableToCompleteException {
    for (final FactoryModule<?> factoryModule : factoryModules) {
      lieToGuiceModule.registerImplicitBinding(factoryModule.getFactoryType());

      FactoryBinding binding = factoryBindingProvider.get();
      try {
        binding.setKeyAndCollector(factoryModule.getFactoryType(), factoryModule.getBindings());
      } catch (ConfigurationException e) {
        errorManager.logError("Factory " + factoryModule.getFactoryType()
            + " could not be created: ", e);
        continue;
      }

      // TODO(dburrows): store appropriate contextual information for the
      // factory and use it here.
      bindingsCollection.addBinding(factoryModule.getFactoryType(), new BindingEntry(binding,
          BindingContext.forText("Bound using factory " + factoryModule.getFactoryType())));

      // All implementations that are created by the factory are also member-
      // injected. To ensure that implementations created by multiple factories
      // result only in one member inject method they are added to this central
      // list.
      bindingsCollection.addMemberInjectRequests(binding.getImplementations());
    }

    errorManager.checkForError();
  }

  private void validateMethods() throws UnableToCompleteException {
    for (MethodLiteral<?, Method> method : completeCollector.getMethods(ginjectorInterface)) {
      List<TypeLiteral<?>> parameters = method.getParameterTypes();
      if (parameters.size() > 1) {
        errorManager.logError("Injector methods cannot have more than one parameter, found: " + method);
      }

      if (parameters.size() == 1) {

        // Member inject method.
        Class<?> paramType = parameters.get(0).getRawType();
        if (!ReflectUtil.isClassOrInterface(paramType)) {
          errorManager.logError(
              "Injector method parameter types must be a class or interface, found: " + method);
        }

        if (!method.getReturnType().getRawType().equals(Void.TYPE)) {
          errorManager.logError("Injector methods with a parameter must have a void return type, found: "
              + method);
        }
      } else if (method.getReturnType().getRawType().equals(Void.TYPE)) {

        // Constructor injection.
        errorManager.logError("Injector methods with no parameters cannot return void");
      }
    }

    errorManager.checkForError();
  }

  private void createBindingsForModules(List<Module> modules) throws UnableToCompleteException {
    List<Element> elements = Elements.getElements(modules);
    for (Element element : elements) {
      GuiceElementVisitor visitor = new GuiceElementVisitor();
      element.acceptVisitor(visitor);

      // Capture any binding errors, any of which we treat as fatal.
      List<Message> messages = visitor.getMessages();
      if (!messages.isEmpty()) {
        for (Message message : messages) {
          // tostring has both source and message so use that
          errorManager.logError(message.toString(), message.getCause());
        }
      }
    }

    errorManager.checkForError();
  }

  private List<Module> createModules() {
    List<Module> modules = new ArrayList<Module>();
    populateModulesFromInjectorInterface(ginjectorInterface, modules,
        new HashSet<Class<? extends GinModule>>());
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
      errorManager.logError("Errors from Guice: " + e.getMessage(), e);
      throw new UnableToCompleteException();
    }
  }

  private void populateModulesFromInjectorInterface(TypeLiteral<?> ginjectorType,
      List<Module> modules, Set<Class<? extends GinModule>> added) {
    GinModules ginModules = ginjectorType.getRawType().getAnnotation(GinModules.class);
    if (ginModules != null) {
      for (Class<? extends GinModule> moduleClass : ginModules.value()) {
        if (added.contains(moduleClass)) {
          continue;
        }
        
        Module module = instantiateGModuleClass(moduleClass);
        if (module != null) {
          modules.add(module);
          added.add(moduleClass);
        }
      }
    }

    for (Class<?> ancestor : ginjectorType.getRawType().getInterfaces()) {

      // TODO(schmitt): Only look at ancestors extending Ginjector?
      populateModulesFromInjectorInterface(ginjectorType.getSupertype(ancestor), modules, added);
    }
  }

  private Module instantiateGModuleClass(Class<? extends GinModule> moduleClassName) {
    try {
      Constructor<? extends GinModule> constructor = moduleClassName.getDeclaredConstructor();
      try {
        constructor.setAccessible(true);
        return new GinModuleAdapter(constructor.newInstance(), factoryModules);
      } finally {
        constructor.setAccessible(false);
      }
    } catch (IllegalAccessException e) {
      errorManager.logError("Error creating module: " + moduleClassName, e);
    } catch (InstantiationException e) {
      errorManager.logError("Error creating module: " + moduleClassName, e);
    } catch (NoSuchMethodException e) {
      errorManager.logError("Error creating module: " + moduleClassName, e);
    } catch (InvocationTargetException e) {
      errorManager.logError("Error creating module: " + moduleClassName, e);
    }

    return null;
  }

  private class GuiceElementVisitor extends DefaultElementVisitor<Void> {
    private final List<Message> messages = new ArrayList<Message>();

    public <T> Void visit(com.google.inject.Binding<T> command) {
      GuiceBindingVisitor<T> bindingVisitor = new GuiceBindingVisitor<T>(command.getKey(),
          messages);
      command.acceptTargetVisitor(bindingVisitor);
      command.acceptScopingVisitor(bindingVisitor);
      return null;
    }

    public Void visit(Message message) {
      messages.add(message);
      return null;
    }

    public <T> Void visit(ProviderLookup<T> providerLookup) {
      // Ignore provider lookups for now
      // TODO(bstoler): I guess we should error if you try to lookup a provider
      // that is not bound?
      return null;
    }

    protected Void visitOther(Element element) {
      visit(new Message(element.getSource(),
          "Ignoring unsupported Module element: " + element));
      return null;
    }

    public Void visit(StaticInjectionRequest staticInjectionRequest) {
      bindingsCollection.addStaticInjectionRequest(
          staticInjectionRequest.getType(), messages);
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

    public Void visit(ProviderKeyBinding<? extends T> providerKeyBinding) {
      BindProviderBinding binding = bindProviderBindingProvider.get();
      binding.setProviderKey(providerKeyBinding.getProviderKey());
      bindingsCollection.addBinding(targetKey, 
          new BindingEntry(binding, BindingContext.forElement(providerKeyBinding)));

      return null;
    }

    public Void visit(ProviderInstanceBinding<? extends T> providerInstanceBinding) {
      // Detect provider methods and handle them
      // TODO(bstoler): Update this when the SPI explicitly has a case for provider methods
      Provider<? extends T> provider = providerInstanceBinding.getProviderInstance();
      if (provider instanceof ProviderMethod) {
        ProviderMethodBinding binding = providerMethodBindingProvider.get();
        binding.setProviderMethod((ProviderMethod<?>) provider);
        bindingsCollection.addBinding(targetKey, 
            new BindingEntry(binding, BindingContext.forElement(providerInstanceBinding)));
        return null;
      }

      if (provider instanceof GwtDotCreateProvider) {
        addImplicitBinding(providerInstanceBinding);
        return null;
      }

      // OTt, use the normal default handler (and error)
      return super.visit(providerInstanceBinding);
    }

    public Void visit(LinkedKeyBinding<? extends T> linkedKeyBinding) {
      BindClassBinding binding = bindClassBindingProvider.get();
      binding.setBoundClassKey(linkedKeyBinding.getLinkedKey());
      bindingsCollection.addBinding(targetKey, 
          new BindingEntry(binding, BindingContext.forElement(linkedKeyBinding)));
      return null;
    }

    public Void visit(InstanceBinding<? extends T> instanceBinding) {
      T instance = instanceBinding.getInstance();
      if (BindConstantBinding.isConstantKey(targetKey)) {
        BindConstantBinding binding = bindConstantBindingProvider.get();
        binding.setKeyAndInstance(targetKey, instance);
        bindingsCollection.addBinding(targetKey, 
            new BindingEntry(binding, BindingContext.forElement(instanceBinding)));
      } else {
        messages.add(new Message(instanceBinding.getSource(),
            "Instance binding not supported; key=" + targetKey + " inst=" + instance));
      }

      return null;
    }

    public Void visit(UntargettedBinding<? extends T> untargettedBinding) {
      addImplicitBinding(untargettedBinding);

      return null;
    }

    private void addImplicitBinding(Element sourceElement) {
      // Register a Gin binding for the default-case binding that
      // Guice saw. We need to register this to avoid later adding
      // this key to the Guice-lies module, which would make it
      // double bound. If binding was null, an error was already logged.
      Binding binding = bindingsCollection.createImplicitBinding(targetKey, false);
      if (binding != null) {
        logger.log(TreeLogger.TRACE, "Implicit binding for " + targetKey + ": " + binding);
        bindingsCollection.addBinding(targetKey, 
            new BindingEntry(binding, BindingContext.forElement(sourceElement)));
      }
    }

    protected Void visitOther(com.google.inject.Binding<? extends T> binding) {
      messages.add(new Message(binding.getSource(),
          "Unsupported binding provided for key: " + targetKey + ": " + binding));
      return null;
    }

    public Void visitEagerSingleton() {
      bindingsCollection.putScope(targetKey, GinScope.EAGER_SINGLETON);
      return null;
    }

    // TODO(schmitt): We don't support this right now in any case, but it's
    // strange to be using the Guice Scope instead of javax.inject.Scope
    public Void visitScope(Scope scope) {
      messages.add(new Message("Explicit scope unsupported: key=" + targetKey
          + " scope=" + scope));
      return null;
    }

    public Void visitScopeAnnotation(Class<? extends Annotation> scopeAnnotation) {
      if (scopeAnnotation == Singleton.class || scopeAnnotation == javax.inject.Singleton.class) {
        bindingsCollection.putScope(targetKey, GinScope.SINGLETON);
      } else {
        messages.add(new Message("Unsupported scope annoation: key=" + targetKey
            + " scope=" + scopeAnnotation));
      }
      return null;
    }

    public Void visitNoScoping() {
      bindingsCollection.putScope(targetKey, GinScope.NO_SCOPE);
      return null;
    }
  }
}
