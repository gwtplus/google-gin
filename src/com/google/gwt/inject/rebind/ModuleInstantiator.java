package com.google.gwt.inject.rebind;

import com.google.gwt.inject.client.GinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.PrivateGinModule;
import com.google.gwt.inject.rebind.adapter.GinModuleAdapter;
import com.google.gwt.inject.rebind.adapter.PrivateGinModuleAdapter;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Instantiate Guice modules given classes.  This is used in {@link BindingsProcessor} to inspect
 * the modules by visiting the bindings and by {@link GuiceValidator} to ensure that the results of
 * binding resolution are valid.
 * 
 * <p>In the latter case we intentionally avoid installing child modules to ensure that only one
 * injector is tested at a time.
 */
public class ModuleInstantiator {
  
  private final ErrorManager errorManager;
  
  /**
   * Modules specified via a GWT configuration property
   */
  private final Set<Class<? extends GinModule>> configurationModules;

  /**
   * Interface of the injector that this class is implementing.
   */
  private TypeLiteral<? extends Ginjector> ginjectorInterface;

  @Inject
  public ModuleInstantiator(ErrorManager errorManager,
      @GinjectorInterfaceType Class<? extends Ginjector> ginjectorInterface,
      @ConfigurationModuleTypes Set<Class<? extends GinModule>> configurationModules) {
    this.errorManager = errorManager;
    this.configurationModules = configurationModules;
    this.ginjectorInterface = TypeLiteral.get(ginjectorInterface);
  }
  
  /**
   * Instantiate the modules for the Ginjector type.
   * 
   * @param ginjectorBindings Ginjector bindings to use as the "root" ginjector.
   * @param hideChildModules if {@code true} avoids instantiating/installing PrivateModules
   */
  public List<Module> instantiateModulesForGinjector(
      GinjectorBindings ginjectorBindings, boolean hideChildModules) {
    Set<Class<? extends GinModule>> moduleClasses = new HashSet<Class<? extends GinModule>>();
    moduleClasses.addAll(configurationModules);
    getModulesFromInjectorInterface(ginjectorInterface, moduleClasses);
    return instantiateModulesForClasses(ginjectorBindings, hideChildModules, moduleClasses);
  }
  
  /**
   * Instantiate the module the given module class.
   * 
   * @param hideChildModules should PrivateModules be hidden/suppressed?
   */
  public List<Module> instantiateModulesForClass(
      GinjectorBindings ginjectorBindings, boolean hideChildModules,
      Class<? extends GinModule> clazz) {
    List<Class<? extends GinModule>> moduleClasses = 
        Collections.<Class<? extends GinModule>>singletonList(clazz);
    return instantiateModulesForClasses(ginjectorBindings, hideChildModules, moduleClasses);
  }
  
  private List<Module> instantiateModulesForClasses(
      GinjectorBindings ginjectorBindings, boolean hideChildModules,
      Iterable<Class<? extends GinModule>> classes) {
    List<Module> modules = new ArrayList<Module>();
    for (Class<? extends GinModule> clazz : classes) {
      Module module = instantiateModuleClass(clazz, ginjectorBindings, hideChildModules);
      if (module != null) {
        modules.add(module);
      }
    }
    return modules;
  }
  
  private Module instantiateModuleClass(
      Class<? extends GinModule> moduleClass, GinjectorBindings rootGinjectorBindings,
      boolean hideChildModules) {
    try {
      Constructor<? extends GinModule> constructor = moduleClass.getDeclaredConstructor();
      try {
        constructor.setAccessible(true);
        if (PrivateGinModule.class.isAssignableFrom(moduleClass)) {
          return hideChildModules ? null : new PrivateGinModuleAdapter(
              (PrivateGinModule) constructor.newInstance(), rootGinjectorBindings);
        } else {
          return new GinModuleAdapter(constructor.newInstance(), rootGinjectorBindings, 
              hideChildModules);
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

  private void getModulesFromInjectorInterface(TypeLiteral<?> ginjectorType,
      Set<Class<? extends GinModule>> moduleClasses) {
    GinModules ginModulesAnnotation = ginjectorType.getRawType().getAnnotation(GinModules.class);
    if (ginModulesAnnotation != null) {
      moduleClasses.addAll(Arrays.asList(ginModulesAnnotation.value()));
    }

    for (Class<?> ancestor : ginjectorType.getRawType().getInterfaces()) {
      getModulesFromInjectorInterface(ginjectorType.getSupertype(ancestor), moduleClasses);
    }
  }
}
