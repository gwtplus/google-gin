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

import com.google.gwt.inject.client.AsyncProvider;
import com.google.gwt.inject.rebind.binding.AsyncProviderBinding;
import com.google.gwt.inject.rebind.binding.BindClassBinding;
import com.google.gwt.inject.rebind.binding.BindConstantBinding;
import com.google.gwt.inject.rebind.binding.BindProviderBinding;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.CallConstructorBinding;
import com.google.gwt.inject.rebind.binding.CallGwtDotCreateBinding;
import com.google.gwt.inject.rebind.binding.ImplicitProviderBinding;
import com.google.gwt.inject.rebind.binding.RemoteServiceProxyBinding;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.inject.rebind.util.GuiceUtil;
import com.google.inject.ImplementedBy;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.ProvidedBy;
import com.google.inject.TypeLiteral;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.inject.Provider;

/**
 * Class responsible for creating implicit bindings.  This returns the binding entry
 * that should be used for the given type.  It does not concern itself with satisfying
 * the dependencies or with figuring out where the binding should appear.  For that,
 * see {@link BindingResolver}.
 */
public class ImplicitBindingCreator {
  
  private final Provider<CallGwtDotCreateBinding> callGwtDotCreateBindingProvider;
  private final Provider<RemoteServiceProxyBinding> remoteServiceProxyBindingProvider;
  private final Provider<CallConstructorBinding> callConstructorBinding;
  private final Provider<BindClassBinding> bindClassBindingProvider;
  private final Provider<BindProviderBinding> bindProviderBindingProvider;
  private final Provider<ImplicitProviderBinding> implicitProviderBindingProvider;
  private final Provider<AsyncProviderBinding> asyncProviderBindingProvider;

  private final ErrorManager errorManager;
  private final LieToGuiceModule lieToGuiceModule;
  
  @Inject
  public ImplicitBindingCreator(
      Provider<CallGwtDotCreateBinding> callGwtDotCreateBindingProvider,
      Provider<CallConstructorBinding> callConstructorBinding,
      GuiceUtil guiceUtil,
      Provider<BindClassBinding> bindClassBindingProvider,
      Provider<BindProviderBinding> bindProviderBindingProvider,
      Provider<ImplicitProviderBinding> implicitProviderBindingProvider,
      Provider<AsyncProviderBinding> asyncProviderBindingProvider,
      LieToGuiceModule lieToGuiceModule,
      Provider<RemoteServiceProxyBinding> remoteServiceProxyBindingProvider,
      ErrorManager errorManager) {
    this.callGwtDotCreateBindingProvider = callGwtDotCreateBindingProvider;
    this.callConstructorBinding = callConstructorBinding;
    this.bindClassBindingProvider = bindClassBindingProvider;
    this.implicitProviderBindingProvider = implicitProviderBindingProvider;
    this.asyncProviderBindingProvider = asyncProviderBindingProvider;
    this.bindProviderBindingProvider = bindProviderBindingProvider;
    this.lieToGuiceModule = lieToGuiceModule;
    this.remoteServiceProxyBindingProvider = remoteServiceProxyBindingProvider;
    this.errorManager = errorManager;
  }
  
  /**
   * Creates the implicit binding and registers with the {@link LieToGuiceModule} if
   * necessary (and appropriate)
   */
  public Binding create(Key<?> key, boolean optional) {
    Binding binding = internalCreate(key, optional);
    if (binding != null 
        && (binding instanceof CallGwtDotCreateBinding 
            || binding instanceof AsyncProviderBinding)) {
        // Need to lie to Guice about any implicit GWT.create bindings and
        // async provider bindings we install that Guice would otherwise not see.
        // http://code.google.com/p/google-gin/issues/detail?id=13
        lieToGuiceModule.registerImplicitBinding(key);
    }
    return binding;
  }
  
  private Binding internalCreate(Key<?> key, boolean optional) {
    TypeLiteral<?> type = key.getTypeLiteral();
    
    // All steps per:
    // http://code.google.com/p/google-guice/wiki/BindingResolution

    // 1. Explicit binding - already finished at this point.

    // 2. Ask parent injector.
    // 3. Ask child injector.
    // These bindings are created in BindingResolver and are not necessary here.
    
    // 4. Provider injections.
    if (isProviderKey(key)) {
      ImplicitProviderBinding binding = implicitProviderBindingProvider.get();
      binding.setProviderKey(key);
      return binding;
      // TODO(bstoler): Scope the provider binding like the thing being provided?
    }
    
    // 4b. AsyncProvider injections.
    if (isAsyncProviderKey(key)) {
      AsyncProviderBinding binding = asyncProviderBindingProvider.get();
      binding.setProviderKey(key);
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
  
  private MethodLiteral<?, Constructor<?>> getInjectConstructor(TypeLiteral<?> type) {
    Constructor<?>[] constructors = type.getRawType().getDeclaredConstructors();
    MethodLiteral<?, Constructor<?>> injectConstructor = null;
    for (Constructor<?> constructor : constructors) {
      MethodLiteral<?, Constructor<?>> constructorLiteral = MethodLiteral.get(constructor, type);
      if (GuiceUtil.hasInject(constructorLiteral)) {
        if (injectConstructor != null) {
          errorManager.logError(String.format(
              "More than one @Inject constructor found for %s; %s, %s", type,
              injectConstructor, constructor));
          return null;
        }
        injectConstructor = constructorLiteral;
      }
    }
    return injectConstructor;
  }
}
