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

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.inject.client.GinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.PrivateGinModule;
import com.google.gwt.inject.client.assistedinject.FactoryModule;
import com.google.gwt.inject.rebind.adapter.GinModuleAdapter;
import com.google.gwt.inject.rebind.adapter.PrivateGinModuleAdapter;
import com.google.gwt.inject.rebind.binding.BindingContext;
import com.google.gwt.inject.rebind.binding.FactoryBinding;
import com.google.gwt.inject.rebind.binding.GinjectorBinding;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.inject.rebind.util.MemberCollector;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.Elements;
import com.google.inject.util.Modules;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;

/**
 * Builds up the bindings and scopes for this {@code Ginjector}.  This uses
 * Guice SPI to inspect the modules and build up details about the necessary
 * bindings in the {@link GinjectorBindings}. 
 */
@Singleton
class BindingsProcessor {
  /**
   * Collector that gathers all methods from an injector.
   */
  private final MemberCollector completeCollector;

  private final Provider<FactoryBinding> factoryBindingProvider;
  private final Provider<GinjectorBinding> ginjectorBindingProvider;

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

  private final GinjectorBindings rootGinjectorBindings;

  private final GuiceElementVisitor.GuiceElementVisitorFactory guiceElementVisitorFactory;
  /**
   * Modules specified via a GWT configuration property
   */
  private final Set<Class<? extends GinModule>> configurationModules;
  
  @Inject
  BindingsProcessor(Provider<MemberCollector> collectorProvider,
      @GinjectorInterfaceType Class<? extends Ginjector> ginjectorInterface,
      LieToGuiceModule lieToGuiceModule,
      Provider<FactoryBinding> factoryBindingProvider,
      Provider<GinjectorBinding> ginjectorBindingProvider,
      ErrorManager errorManager,
      @RootBindings GinjectorBindings rootGinjectorBindings,
      GuiceElementVisitor.GuiceElementVisitorFactory guiceElementVisitorFactory,
      @ConfigurationModuleTypes Set<Class<? extends GinModule>> configurationModules) {
    this.ginjectorBindingProvider = ginjectorBindingProvider;
    this.ginjectorInterface = TypeLiteral.get(ginjectorInterface);
    this.lieToGuiceModule = lieToGuiceModule;
    this.factoryBindingProvider = factoryBindingProvider;
    this.errorManager = errorManager;
    this.rootGinjectorBindings = rootGinjectorBindings;
    this.guiceElementVisitorFactory = guiceElementVisitorFactory;
    this.configurationModules = configurationModules;

    completeCollector = collectorProvider.get();
    completeCollector.setMethodFilter(MemberCollector.ALL_METHOD_FILTER);
  }
  
  public void process() throws UnableToCompleteException {
    validateMethods();
    rootGinjectorBindings.setModule(ginjectorInterface.getClass());
    rootGinjectorBindings.addUnresolvedEntriesForInjectorInterface();
    registerGinjectorBinding();

    createBindingsForModules(createModules(rootGinjectorBindings));
    errorManager.checkForError();
    
    resolveAllUnresolvedBindings(rootGinjectorBindings);
    errorManager.checkForError();
    
    validateModulesUsingGuice(createModules(null));
    errorManager.checkForError();
  }
  
  /**
   * Create an explicit binding for the Ginjector.
   */
  private void registerGinjectorBinding() {
    GinjectorBinding ginjectorBinding = ginjectorBindingProvider.get();
    rootGinjectorBindings.addBinding(Key.get(ginjectorInterface), 
        new BindingEntry(ginjectorBinding, BindingContext.forText("Binding for ginjector")));
    lieToGuiceModule.registerImplicitBinding(Key.get(ginjectorInterface));
  }
  
  /**
   * Create bindings for factories and resolve all implicit bindings for all 
   * unresolved bindings in the each injector.
   * 
   * <p> This performs a depth-first iteration over all the nodes, and fills in the
   * bindings on the way up the tree.  This order is important because creating
   * implicit bindings in a child {@link GinjectorBindings} may add dependencies to the
   * parent.  By processing on the way up, we ensure that we only need to 
   * process each set once.
   * 
   * @param collection {@link GinjectorBindings} to resolve bindings for
   */
  private void resolveAllUnresolvedBindings(GinjectorBindings collection) {
    for (GinjectorBindings child : collection.getChildren()) {
      resolveAllUnresolvedBindings(child);
    }
    createBindingsForFactories(collection);
    collection.resolveBindings();
  }

  private void createBindingsForFactories(GinjectorBindings bindings) {
    for (final FactoryModule<?> factoryModule : bindings.getFactoryModules()) {
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
      bindings.addBinding(factoryModule.getFactoryType(), new BindingEntry(binding,
          BindingContext.forText("Bound using factory in " + bindings.getModule().getName())));

      // All implementations that are created by the factory are also member-
      // injected. To ensure that implementations created by multiple factories
      // result only in one member inject method they are added to this central
      // list.
      bindings.addMemberInjectRequests(binding.getImplementations());
    }
  }

  private void validateMethods() throws UnableToCompleteException {
    for (MethodLiteral<?, Method> method : completeCollector.getMethods(ginjectorInterface)) {
      List<TypeLiteral<?>> parameters = method.getParameterTypes();
      if (parameters.size() > 1) {
        errorManager.logError("Injector methods cannot have more than one parameter, found: " 
            + method);
      }

      if (parameters.size() == 1) {

        // Member inject method.
        Class<?> paramType = parameters.get(0).getRawType();
        if (!ReflectUtil.isClassOrInterface(paramType)) {
          errorManager.logError(
              "Injector method parameter types must be a class or interface, found: " + method);
        }

        if (!method.getReturnType().getRawType().equals(Void.TYPE)) {
          errorManager.logError(
              "Injector methods with a parameter must have a void return type, found: " + method);
        }
      } else if (method.getReturnType().getRawType().equals(Void.TYPE)) {

        // Constructor injection.
        errorManager.logError("Injector methods with no parameters cannot return void");
      }
    }

    errorManager.checkForError();
  }

  private void createBindingsForModules(List<Module> modules) {
    GuiceElementVisitor visitor = guiceElementVisitorFactory.create(rootGinjectorBindings);
    visitor.visitElementsAndReportErrors(Elements.getElements(modules));
  }

  private List<Module> createModules(GinjectorBindings rootGinjectorBindings) {
    Set<Class<? extends GinModule>> moduleClasses = 
        new HashSet<Class<? extends GinModule>>(configurationModules);
    getModulesFromInjectorInterface(ginjectorInterface, moduleClasses);
    
    List<Module> modules = new ArrayList<Module>();
    for (Class<? extends GinModule> moduleClass : moduleClasses) {
      Module module = instantiateGModuleClass(moduleClass, rootGinjectorBindings);
      if (module != null) {
        modules.add(module);
      }
    }
    
    return modules;
  }

  private void validateModulesUsingGuice(List<Module> modules) {
    // Validate module consistency using Guice.
    try {
      // Use Modules.override so that the guice lies can override (and correct) implicit bindings
      // that Guice emitted earlier in the stack.
      Guice.createInjector(Stage.TOOL, Modules.override(modules).with(lieToGuiceModule));
    } catch (Exception e) {
      errorManager.logError("Errors from Guice: " + e.getMessage(), e);
    }
  }

  private void getModulesFromInjectorInterface(TypeLiteral<?> ginjectorType,
      Set<Class<? extends GinModule>> moduleClasses) {
    GinModules ginModulesAnnotation = ginjectorType.getRawType().getAnnotation(GinModules.class);
    if (ginModulesAnnotation != null) {
      moduleClasses.addAll(Arrays.asList(ginModulesAnnotation.value()));
    }

    for (Class<?> ancestor : ginjectorType.getRawType().getInterfaces()) {
      // TODO(schmitt): Only look at ancestors extending Ginjector?
      getModulesFromInjectorInterface(ginjectorType.getSupertype(ancestor), moduleClasses);
    }
  }

  private Module instantiateGModuleClass(
      Class<? extends GinModule> moduleClass, GinjectorBindings rootGinjectorBindings) {
    try {
      Constructor<? extends GinModule> constructor = moduleClass.getDeclaredConstructor();
      try {
        constructor.setAccessible(true);
        if (PrivateGinModule.class.isAssignableFrom(moduleClass)) {
          return new PrivateGinModuleAdapter(
              (PrivateGinModule) constructor.newInstance(), rootGinjectorBindings);
        } else {
          return new GinModuleAdapter(constructor.newInstance(), rootGinjectorBindings);
        }
      } finally {
        constructor.setAccessible(false);
      }
    } catch (IllegalAccessException e) {
      errorManager.logError("Error creating module: " + moduleClass, e);
    } catch (InstantiationException e) {
      errorManager.logError("Error creating module: " + moduleClass, e);
    } catch (NoSuchMethodException e) {
      errorManager.logError("Error creating module: " + moduleClass, e);
    } catch (InvocationTargetException e) {
      errorManager.logError("Error creating module: " + moduleClass, e);
    }

    return null;
  }
}
