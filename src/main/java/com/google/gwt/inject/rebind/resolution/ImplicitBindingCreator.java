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
package com.google.gwt.inject.rebind.resolution;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.inject.client.AsyncProvider;
import com.google.gwt.inject.rebind.binding.BindClassBinding;
import com.google.gwt.inject.rebind.binding.BindConstantBinding;
import com.google.gwt.inject.rebind.binding.BindProviderBinding;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.BindingFactory;
import com.google.gwt.inject.rebind.binding.Context;
import com.google.gwt.inject.rebind.binding.RemoteServiceProxyBinding;
import com.google.gwt.inject.rebind.reflect.MethodLiteral;
import com.google.gwt.inject.rebind.reflect.ReflectUtil;
import com.google.gwt.inject.rebind.util.GuiceUtil;
import com.google.gwt.inject.rebind.util.PrettyPrinter;
import com.google.inject.ImplementedBy;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.ProvidedBy;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.Assisted;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import jakarta.inject.Provider;

/**
 * Class responsible for creating implicit bindings.  This returns the binding entry
 * that should be used for the given type.  It does not concern itself with satisfying
 * the dependencies or with figuring out where the binding should appear.  For that,
 * see {@link BindingResolver}.
 */
public class ImplicitBindingCreator {

  /**
   * Exception thrown to indicate an error occurred during binding creation.
   */
  public static class BindingCreationException extends Exception {

    /**
     * Create a new BindingCreationException using the given format string and arguments.  Will
     * create an exception with a message constructed with {@code String.format(msgFmt, args)}.
     */
    public BindingCreationException(String msgFmt, Object... args) {
      super(PrettyPrinter.format(msgFmt, args));
    }
  }

  private final BindingFactory bindingFactory;
  private final GeneratorContext generatorContext;
  private final TreeLogger logger;

  @Inject
  public ImplicitBindingCreator(BindingFactory bindingFactory, GeneratorContext generatorContext,
      @Assisted TreeLogger logger) {
    this.bindingFactory = bindingFactory;
    this.generatorContext = generatorContext;
    this.logger = logger;
  }

  /**
   * Creates the implicit binding.
   */
  public Binding create(Key<?> key) throws BindingCreationException {
    TypeLiteral<?> type = key.getTypeLiteral();

    // All steps per:
    // http://code.google.com/p/google-guice/wiki/BindingResolution

    // 1. Explicit binding - already finished at this point.

    // 2. Ask parent injector.
    // 3. Ask child injector.
    // These bindings are created in BindingResolver and are not necessary here.

    // 4. Provider injections.
    if (isProviderKey(key)) {
      return bindingFactory.getImplicitProviderBinding(key);
      // TODO(bstoler): Scope the provider binding like the thing being provided?
    }

    // 4b. AsyncProvider injections.
    if (isAsyncProviderKey(key)) {
      return bindingFactory.getAsyncProviderBinding(key);
    }

    // 5. Convert constants.
    // Already covered by resolving explicit bindings.
    if (BindConstantBinding.isConstantKey(key)) {
      throw new BindingCreationException(
          "Binding requested for constant key '%s' but no explicit binding was found", key);
    }

    // 6. If the dependency has a binding annotation, give up.
    if (key.getAnnotation() != null || key.getAnnotationType() != null) {
      throw new BindingCreationException("No implementation bound for '%s' and an implicit binding"
          + " cannot be created because the type is annotated.", key);
    }

    // 7. If the dependency is an array or enum, give up.
    // Covered by step 5 (enum) and 11 (array).

    // 8. Handle TypeLiteral injections.
    // TODO(schmitt): Implement TypeLiteral injections.

    // 9. Use resolution annotations (@ImplementedBy, @ProvidedBy)
    ImplementedBy implementedBy = type.getRawType().getAnnotation(ImplementedBy.class);
    if (implementedBy != null) {
      return createImplementedByBinding(key, implementedBy);
    }

    ProvidedBy providedBy = type.getRawType().getAnnotation(ProvidedBy.class);
    if (providedBy != null) {
      return createProvidedByBinding(key, providedBy);
    }

    // 10. If the dependency is abstract or a non-static inner class, give up.
    // Abstract classes are handled by GWT.create.
    // TODO(schmitt): Introduce check.

    // 11. Use a single @Inject or public no-arguments constructor.
    return createImplicitBindingForClass(type);
  }

  private Binding createImplicitBindingForClass(TypeLiteral<?> type)
      throws BindingCreationException {
    // Either call the @Inject constructor or use GWT.create
    MethodLiteral<?, Constructor<?>> injectConstructor = getInjectConstructor(type);

    if (injectConstructor != null) {
      return bindingFactory.getCallConstructorBinding(injectConstructor);
    }

    if (shouldGwtDotCreate(type)) {
      if (RemoteServiceProxyBinding.isRemoteServiceProxy(type)) {
        return bindingFactory.getRemoteServiceProxyBinding(type);
      } else {
        return bindingFactory.getCallGwtDotCreateBinding(type);
      }
    }

    throw new BindingCreationException("No @Inject or default constructor found for %s", type);
  }

  /**
   * Returns {@code true} if the given type should be created automatically with
   * {@code GWT.create()}.
   *
   * <p>{@code GWT.create()} is used to create concrete classes with accessible
   * zero-argument constructors, and interfaces or classes with GWT rebind
   * rules.
   */
  private boolean shouldGwtDotCreate(TypeLiteral<?> typeLiteral) throws BindingCreationException {
    Class<?> rawType = typeLiteral.getRawType();
    if (rawType.isInterface()) {
      // Check whether we can GWT.create() the interface.

      // Remote service proxies don't have rebind rules; we handle them
      // specially by creating the corresponding synchronous interface (which
      // does have a rebind rule).
      if (RemoteServiceProxyBinding.isRemoteServiceProxy(typeLiteral)) {
        // We could check whether the synchronous interface has a rebind rule;
        // however, the user is probably expecting us to GWT.create() a service
        // interface for them.  If there isn't a rebind rule, a GWT rebind error
        // probably makes more sense than a Gin error.
        return true;
      }

      return hasRebindRule(rawType);
    } else {
      return hasAccessibleZeroArgConstructor(rawType) || hasRebindRule(rawType);
    }
  }

  private boolean hasRebindRule(Class<?> rawType) throws BindingCreationException {
    String canonicalName = rawType.getCanonicalName();
    if (canonicalName == null) {
      throw new BindingCreationException("Cannot inject a type with no canonical name: " + rawType);
    } else {
      return generatorContext.checkRebindRuleAvailable(canonicalName);
    }
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
  private boolean hasAccessibleZeroArgConstructor(Class<?> rawType) {
    Constructor<?> constructor;
    try {
      constructor = rawType.getDeclaredConstructor();
    } catch (NoSuchMethodException e) {
      return rawType.getDeclaredConstructors().length == 0;
    }

    return !ReflectUtil.isPrivate(constructor) || ReflectUtil.isPrivate(rawType);
  }

  private BindClassBinding createImplementedByBinding(Key<?> key, ImplementedBy implementedBy)
      throws BindingCreationException {
    Class<?> rawType = key.getTypeLiteral().getRawType();
    Class<?> implementationType = implementedBy.value();

    if (implementationType == rawType) {
      throw new BindingCreationException(
          "@ImplementedBy points to the same class it annotates: %s", rawType);
    }

    if (!rawType.isAssignableFrom(implementationType)) {
      throw new BindingCreationException("%s doesn't extend %s (while resolving @ImplementedBy)",
          implementationType, rawType);
    }

    return bindingFactory.getBindClassBinding(Key.get(implementationType), key,
        Context.forText("@ImplementedBy annotation"));
  }

  private BindProviderBinding createProvidedByBinding(Key<?> key, ProvidedBy providedBy)
      throws BindingCreationException {
    Class<?> rawType = key.getTypeLiteral().getRawType();
    Class<? extends Provider<?>> providerType = providedBy.value();

    if (providerType == rawType) {
      throw new BindingCreationException(
          "@ProvidedBy points to the same class it annotates: %s", rawType);
    }

    return bindingFactory.getBindProviderBinding(Key.get(providerType), key,
        Context.forText("@ProvidedBy annotation"));
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

  private MethodLiteral<?, Constructor<?>> getInjectConstructor(TypeLiteral<?> type)
      throws BindingCreationException {
    Constructor<?>[] constructors = type.getRawType().getDeclaredConstructors();
    MethodLiteral<?, Constructor<?>> injectConstructor = null;
    for (Constructor<?> constructor : constructors) {
      MethodLiteral<?, Constructor<?>> constructorLiteral = MethodLiteral.get(constructor, type);
      if (GuiceUtil.hasInject(constructorLiteral)) {
        if (injectConstructor != null) {
          throw new BindingCreationException(
              "More than one @Inject constructor found for %s; %s, %s",
              type, injectConstructor, constructorLiteral);
        }
        injectConstructor = constructorLiteral;
      }
    }
    return injectConstructor;
  }

  /**
   * 
   */
  public interface Factory {
    ImplicitBindingCreator create(TreeLogger logger);
  }
}
