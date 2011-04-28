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
import com.google.gwt.inject.rebind.binding.BindConstantBinding;
import com.google.gwt.inject.rebind.binding.BindingContext;
import com.google.gwt.inject.rebind.binding.BindingFactory;
import com.google.gwt.inject.rebind.binding.Dependency;
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

import javax.inject.Provider;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Gathers information about Guice Bindings and adds the information to a {@link GinjectorBindings}.
 */
public class GuiceBindingVisitor<T> extends DefaultBindingTargetVisitor<T, Void>
    implements BindingScopingVisitor<Void> {

  private final Key<T> targetKey;
  private final List<Message> messages;
  private final GinjectorBindings bindingsCollection;
  private final LieToGuiceModule lieToGuiceModule;
  private final BindingFactory bindingFactory;

  public GuiceBindingVisitor(LieToGuiceModule lieToGuiceModule, Key<T> targetKey,
      List<Message> messages, GinjectorBindings bindingsCollection, BindingFactory bindingFactory) {
    this.lieToGuiceModule = lieToGuiceModule;
    this.targetKey = targetKey;
    this.messages = messages;
    this.bindingsCollection = bindingsCollection;
    this.bindingFactory = bindingFactory;
  }

  public Void visit(ProviderKeyBinding<? extends T> providerKeyBinding) {
    BindingContext context = BindingContext.forElement(providerKeyBinding);
    bindingsCollection.addBinding(
        targetKey,
        bindingFactory.getBindProviderBinding(
            providerKeyBinding.getProviderKey(), providerKeyBinding.getKey(), context));

    return null;
  }

  public Void visit(ProviderInstanceBinding<? extends T> providerInstanceBinding) {
    // Detect provider methods and handle them
    // TODO(bstoler): Update this when the SPI explicitly has a case for
    // provider methods
    Provider<? extends T> provider = providerInstanceBinding.getProviderInstance();
    if (provider instanceof ProviderMethod) {
      BindingContext context = BindingContext.forElement(providerInstanceBinding);
      bindingsCollection.addBinding(targetKey,
          bindingFactory.getProviderMethodBinding((ProviderMethod) provider, context));
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
    BindingContext context = BindingContext.forElement(linkedKeyBinding);
    bindingsCollection.addBinding(targetKey,
        bindingFactory.getBindClassBinding(linkedKeyBinding.getLinkedKey(), targetKey, context));
    return null;
  }

  public Void visit(InstanceBinding<? extends T> instanceBinding) {
    T instance = instanceBinding.getInstance();
    if (BindConstantBinding.isConstantKey(targetKey)) {
      BindingContext context = BindingContext.forElement(instanceBinding);
      bindingsCollection.addBinding(targetKey,
          bindingFactory.getBindConstantBinding(targetKey, instance, context));
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
    bindingsCollection.addDependency(new Dependency(Dependency.GINJECTOR, targetKey,
        BindingContext.forElement(sourceElement).toString()));
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
