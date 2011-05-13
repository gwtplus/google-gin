package com.google.gwt.inject.rebind;

import com.google.gwt.inject.client.GinModule;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.rebind.binding.AsyncProviderBinding;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.CallGwtDotCreateBinding;
import com.google.gwt.inject.rebind.binding.ExposedChildBinding;
import com.google.gwt.inject.rebind.binding.ParentBinding;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Validates each GinjectorBindings to make sure that all bindings have been satisfied using Guice.
 * The corresponding Gin module (or modules) are installed into a test injector as the root
 * module(s), using {@link ModuleInstantiator} and suppressing the installation of any private child
 * modules.
 * 
 * <p>Lies are also generated for bindings that will be missing in the Guice module but available to
 * Gin, specifically:
 * <ul>
 *   <li>Bindings that are Gin-specific (e.g., GWT.create and Async Provider) 
 *   since Guice won't know that these exist.
 *   </li>
 *   <li>The binding for the Ginjector, since Guice won't know it exists.
 *   </li>
 *   <li>Bindings that come from another injector (parent or child bindings) because we 
 *   intentionally didn't install any child modules (or parent modules) to avoid problems in 
 *   Modules.override that arise from its inability to override a binding in the presence of
 *   multiple child injectors.
 *   </li>
 * </ul>
 * 
 * <p>The inspection in isolation finds most errors that can arise during resolution, although it
 * does miss certain cases, such as "cycles" in in the GinjectorImpl.  These cases shouldn't occur
 * during resolution, and care needs to be taken to avoid introducing such bugs.
 */
public class GuiceValidator {
  
  private final ErrorManager errorManager;
  private final ModuleInstantiator instantiator;
  private final Class<? extends Ginjector> ginjectorInterface;
  
  @Inject
  public GuiceValidator(ErrorManager errorManager, ModuleInstantiator instantiator,
      @GinjectorInterfaceType Class<? extends Ginjector> ginjectorInterface) {
    this.errorManager = errorManager;
    this.instantiator = instantiator;
    this.ginjectorInterface = ginjectorInterface;    
  }
  
  /**
   * Validate the given Ginjector using Guice, according to the class doc for 
   * {@link GuiceValidator}.
   */
  public void validate(GinjectorBindings ginjector) {
    // Validate module consistency using Guice.
    try {
      // Use Modules.override so that the guice lies can override (and correct) implicit bindings
      // that Guice emitted earlier in the stack.
      Module lies = new LieToGuiceModule(getNecessaryLies(ginjector));
      Guice.createInjector(Stage.TOOL, Modules.override(getModules(ginjector)).with(lies));
    } catch (Exception e) {
      errorManager.logError("Errors from Guice: " + e.getMessage(), e);
    }
  }

  /**
   * Determine which Guice modules to use for validation.
   */
  private List<Module> getModules(GinjectorBindings ginjector) {
    List<Module> modules = new ArrayList<Module>();
    if (GinModule.class.isAssignableFrom(ginjector.getModule())) {
      @SuppressWarnings("unchecked")
      Class<? extends GinModule> clazz = (Class<? extends GinModule>) ginjector.getModule();
      modules.addAll(instantiator.instantiateModulesForClass(null, true, clazz));
    } else {
      modules.addAll(instantiator.instantiateModulesForGinjector(null, true));
    }
    return modules;
  }
  
  /**
   * We need to lie about any bindings that come from GWT (eg, GWT.create bindings)
   * and any bindings that come from another injector (parent/child bindings), in addition to the
   * binding for the Ginjector.  See {@link GuiceValidator above} for more details.
   */
  private Set<Key<?>> getNecessaryLies(GinjectorBindings ginjector) {
    Set<Key<?>> lies = new LinkedHashSet<Key<?>>();
    lies.add(Key.get(ginjectorInterface));
    for (Entry<Key<?>, Binding> bindingPair : ginjector.getBindings()) {
      if (shouldLieAbout(bindingPair.getValue())) {
        Key<?> key = bindingPair.getKey();
        // The lie module can't install a lie for Provider<X> because Guice doesn't allow you to
        // create bindings for Providers.  In this case, we should lie about X instead and let
        // Guice create its own Provider<X>.
        Class<?> clazz = key.getTypeLiteral().getRawType();
        if (Provider.class.equals(clazz) || javax.inject.Provider.class.equals(clazz)) {
          key = ReflectUtil.getProvidedKey(key);
        }
        lies.add(key);
      }
    }
    return lies;
  }

  private boolean shouldLieAbout(Binding binding) {
    return binding instanceof CallGwtDotCreateBinding
        || binding instanceof AsyncProviderBinding
        || binding instanceof ParentBinding
        || binding instanceof ExposedChildBinding;
  }
}
