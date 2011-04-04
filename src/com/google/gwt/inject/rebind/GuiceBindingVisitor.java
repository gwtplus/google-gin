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

import com.google.gwt.inject.rebind.adapter.GwtDotCreateProvider;
import com.google.gwt.inject.rebind.binding.BindClassBinding;
import com.google.gwt.inject.rebind.binding.BindConstantBinding;
import com.google.gwt.inject.rebind.binding.BindProviderBinding;
import com.google.gwt.inject.rebind.binding.BindingContext;
import com.google.gwt.inject.rebind.binding.Dependency;
import com.google.gwt.inject.rebind.binding.ProviderMethodBinding;
import com.google.inject.Key;
import com.google.inject.Scope;
import com.google.inject.Singleton;
import com.google.inject.internal.ProviderMethod;
import com.google.inject.spi.BindingScopingVisitor;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.Element;
import com.google.inject.spi.InstanceBinding;
import com.google.inject.spi.LinkedKeyBinding;
import com.google.inject.spi.Message;
import com.google.inject.spi.ProviderInstanceBinding;
import com.google.inject.spi.ProviderKeyBinding;
import com.google.inject.spi.UntargettedBinding;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.inject.Provider;

/**
 * Gathers information about Guice Bindings and adds the information to a {@link GinjectorBindings}.
 */
public class GuiceBindingVisitor<T> extends DefaultBindingTargetVisitor<T, Void>
    implements BindingScopingVisitor<Void> {

  private final Key<T> targetKey;
  private final List<Message> messages;
  private final Provider<BindProviderBinding> bindProviderBindingProvider;
  private final Provider<ProviderMethodBinding> providerMethodBindingProvider;
  private final Provider<BindClassBinding> bindClassBindingProvider;
  private final Provider<BindConstantBinding> bindConstantBindingProvider;
  private final GinjectorBindings bindingsCollection;
  private final LieToGuiceModule lieToGuiceModule;

  public GuiceBindingVisitor(
      Provider<BindProviderBinding> bindProviderBindingProvider,
      Provider<ProviderMethodBinding> providerMethodBindingProvider,
      Provider<BindClassBinding> bindClassBindingProvider,
      Provider<BindConstantBinding> bindConstantBindingProvider,
      LieToGuiceModule lieToGuiceModule,
      Key<T> targetKey, List<Message> messages,
      GinjectorBindings bindingsCollection) {
    this.bindProviderBindingProvider = bindProviderBindingProvider;
    this.providerMethodBindingProvider = providerMethodBindingProvider;
    this.bindClassBindingProvider = bindClassBindingProvider;
    this.bindConstantBindingProvider = bindConstantBindingProvider;
    this.lieToGuiceModule = lieToGuiceModule;
    this.targetKey = targetKey;
    this.messages = messages;
    this.bindingsCollection = bindingsCollection;
  }

  public Void visit(ProviderKeyBinding<? extends T> providerKeyBinding) {
    BindProviderBinding binding = bindProviderBindingProvider.get();
    binding.setProviderKey(providerKeyBinding.getProviderKey());
    binding.setSourceKey(providerKeyBinding.getKey());
    bindingsCollection.addBinding(targetKey, binding, 
        BindingContext.forElement(providerKeyBinding));

    return null;
  }

  public Void visit(ProviderInstanceBinding<? extends T> providerInstanceBinding) {
    // Detect provider methods and handle them
    // TODO(bstoler): Update this when the SPI explicitly has a case for
    // provider methods
    Provider<? extends T> provider = providerInstanceBinding.getProviderInstance();
    if (provider instanceof ProviderMethod) {
      ProviderMethodBinding binding = providerMethodBindingProvider.get();
      binding.setProviderMethod((ProviderMethod<?>) provider);
      bindingsCollection.addBinding(targetKey, binding, 
          BindingContext.forElement(providerInstanceBinding));
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
    binding.setSourceClassKey(targetKey);
    binding.setBoundClassKey(linkedKeyBinding.getLinkedKey());
    bindingsCollection.addBinding(targetKey, 
        binding, BindingContext.forElement(linkedKeyBinding));
    return null;
  }

  public Void visit(InstanceBinding<? extends T> instanceBinding) {
    T instance = instanceBinding.getInstance();
    if (BindConstantBinding.isConstantKey(targetKey)) {
      BindConstantBinding binding = bindConstantBindingProvider.get();
      binding.setKeyAndInstance(targetKey, instance);
      bindingsCollection.addBinding(targetKey, binding, BindingContext.forElement(instanceBinding));
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
    // Register the target key with the {@link GuiceLiesModule}'s blacklist to avoid
    // adding a binding that Guice will think is double bound.
    lieToGuiceModule.blacklist(targetKey);
    bindingsCollection.addDependency(new Dependency(Dependency.GINJECTOR, targetKey));
  }

  protected Void visitOther(com.google.inject.Binding<? extends T> binding) {
    messages.add(new Message(binding.getSource(), "Unsupported binding provided for key: "
        + targetKey + ": " + binding));
    return null;
  }

  public Void visitEagerSingleton() {
    bindingsCollection.putScope(targetKey, GinScope.EAGER_SINGLETON);
    return null;
  }

  // TODO(schmitt): We don't support this right now in any case, but it's
  // strange to be using the Guice Scope instead of javax.inject.Scope
  public Void visitScope(Scope scope) {
    messages.add(new Message("Explicit scope unsupported: key=" + targetKey + " scope=" + scope));
    return null;
  }

  public Void visitScopeAnnotation(Class<? extends Annotation> scopeAnnotation) {
    if (scopeAnnotation == Singleton.class || scopeAnnotation == javax.inject.Singleton.class) {
      bindingsCollection.putScope(targetKey, GinScope.SINGLETON);
    } else {
      messages.add(new Message("Unsupported scope annoation: key=" + targetKey + " scope="
          + scopeAnnotation));
    }
    return null;
  }

  public Void visitNoScoping() {
    bindingsCollection.putScope(targetKey, GinScope.NO_SCOPE);
    return null;
  }
}