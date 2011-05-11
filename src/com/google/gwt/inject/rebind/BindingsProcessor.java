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
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.assistedinject.FactoryModule;
import com.google.gwt.inject.rebind.binding.BindingFactory;
import com.google.gwt.inject.rebind.binding.Context;
import com.google.gwt.inject.rebind.binding.FactoryBinding;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.inject.rebind.util.MemberCollector;
import com.google.inject.ConfigurationException;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.Elements;

import java.lang.reflect.Method;
import java.util.List;

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

  private final BindingFactory bindingFactory;

  /**
   * Interface of the injector that this class is implementing.
   */
  private final TypeLiteral<? extends Ginjector> ginjectorInterface;

  private final ErrorManager errorManager;
  private final GinjectorBindings rootGinjectorBindings;
  private final GuiceElementVisitor.GuiceElementVisitorFactory guiceElementVisitorFactory;

  private final ModuleInstantiator instantiator;

  private final GuiceValidator validator;
  
  @Inject
  BindingsProcessor(Provider<MemberCollector> collectorProvider,
      @GinjectorInterfaceType Class<? extends Ginjector> ginjectorInterface,
      ErrorManager errorManager,
      @RootBindings GinjectorBindings rootGinjectorBindings,
      GuiceElementVisitor.GuiceElementVisitorFactory guiceElementVisitorFactory,
      BindingFactory bindingFactory,
      ModuleInstantiator instantiator, GuiceValidator validator) {
    this.bindingFactory = bindingFactory;
    this.instantiator = instantiator;
    this.validator = validator;
    this.ginjectorInterface = TypeLiteral.get(ginjectorInterface);
    this.errorManager = errorManager;
    this.rootGinjectorBindings = rootGinjectorBindings;
    this.guiceElementVisitorFactory = guiceElementVisitorFactory;

    completeCollector = collectorProvider.get();
    completeCollector.setMethodFilter(MemberCollector.ALL_METHOD_FILTER);
  }
  
  public void process() throws UnableToCompleteException {
    validateMethods();
    rootGinjectorBindings.setModule(ginjectorInterface.getClass());
    rootGinjectorBindings.addUnresolvedEntriesForInjectorInterface();
    registerGinjectorBinding();

    createBindingsForModules(
        instantiator.instantiateModulesForGinjector(rootGinjectorBindings, false));
    errorManager.checkForError();
    
    resolveAllUnresolvedBindings(rootGinjectorBindings);
    errorManager.checkForError();
  }
  
  /**
   * Create an explicit binding for the Ginjector.
   */
  private void registerGinjectorBinding() {
    Key<? extends Ginjector> ginjectorKey = Key.get(ginjectorInterface);
    rootGinjectorBindings.addBinding(ginjectorKey, bindingFactory.getGinjectorBinding());
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
   * @throws UnableToCompleteException if binding failed
   */
  private void resolveAllUnresolvedBindings(GinjectorBindings collection) 
      throws UnableToCompleteException {
    for (GinjectorBindings child : collection.getChildren()) {
      resolveAllUnresolvedBindings(child);
    }
    createBindingsForFactories(collection);
    collection.resolveBindings();
    validator.validate(collection);
  }

  private void createBindingsForFactories(GinjectorBindings bindings) {
    for (final FactoryModule<?> factoryModule : bindings.getFactoryModules()) {
      FactoryBinding binding;
      try {
        binding = bindingFactory.getFactoryBinding(
            factoryModule.getBindings(),
            factoryModule.getFactoryType(),
            Context.forText(factoryModule.getSource()));
      } catch (ConfigurationException e) {
        errorManager.logError("Factory %s could not be created", factoryModule.getFactoryType(), e);
        continue;
      }

      bindings.addBinding(factoryModule.getFactoryType(), binding);

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
        errorManager.logError("Injector methods cannot have more than one parameter, found: %s",
            method);
      }

      if (parameters.size() == 1) {

        // Member inject method.
        Class<?> paramType = parameters.get(0).getRawType();
        if (!ReflectUtil.isClassOrInterface(paramType)) {
          errorManager.logError(
              "Injector method parameter types must be a class or interface, found: %s",
              method);
        }

        if (!method.getReturnType().getRawType().equals(Void.TYPE)) {
          errorManager.logError(
              "Injector methods with a parameter must have a void return type, found: %s",
              method);
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
}
