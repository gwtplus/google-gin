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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.inject.rebind.binding.BindingContext;
import com.google.gwt.inject.rebind.binding.ExposedChildBinding;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.Element;
import com.google.inject.spi.Message;
import com.google.inject.spi.PrivateElements;
import com.google.inject.spi.ProviderLookup;
import com.google.inject.spi.StaticInjectionRequest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Provider;

/**
 * Gathers elements and adds them to a {@link GinjectorBindings}.
 */
public class GuiceElementVisitor extends DefaultElementVisitor<Void> {
  
  /**
   * Interface for use with Assisted Injection for creating {@link GuiceElementVisitor}
   */
  public interface GuiceElementVisitorFactory {
    GuiceElementVisitor create(GinjectorBindings bindingsCollection);
  }

  private final List<Message> messages = new ArrayList<Message>();
  private final TreeLogger logger;
  private GuiceElementVisitor.GuiceElementVisitorFactory guiceElementVisitorFactory;
  private GuiceBindingVisitorFactory bindingVisitorFactory;
  private Provider<ExposedChildBinding> exposedChildBindingProvider;
  private GinjectorBindings bindings;
  private Iterator<GinjectorBindings> children;
  private final ErrorManager errorManager;

  @Inject
  public GuiceElementVisitor(TreeLogger logger,
      GuiceElementVisitor.GuiceElementVisitorFactory guiceElementVisitorFactory,
      GuiceBindingVisitorFactory bindingVisitorFactory,
      Provider<ExposedChildBinding> exposedChildBindingProvider,
      ErrorManager errorManager,
      @Assisted GinjectorBindings bindings) {
    this.logger = logger;
    this.guiceElementVisitorFactory = guiceElementVisitorFactory;
    this.bindingVisitorFactory = bindingVisitorFactory;
    this.exposedChildBindingProvider = exposedChildBindingProvider;
    this.errorManager = errorManager;
    this.bindings = bindings;
  }
  
  public void visitElementsAndReportErrors(List<Element> elements) {
    visitElements(elements);
    
    // Capture any binding errors, any of which we treat as fatal.
    if (!messages.isEmpty()) {
      for (Message message : messages) {
        // tostring has both source and message so use that
        errorManager.logError(message.toString(), message.getCause());
      }
    }
  }
  
  private void visitElements(List<Element> elements) {
    // We take advantage of the fact that iterating over the PrivateElements should
    // happen in the same order that the modules were installed.  We match each PrivateElements
    // up with the {@link GinjectorBindings} that were created in the adapter.
    children = bindings.getChildren().iterator();
    for (Element element : elements) {
      element.acceptVisitor(this);
    }
  }

  public <T> Void visit(com.google.inject.Binding<T> command) {
    GuiceBindingVisitor<T> bindingVisitor = bindingVisitorFactory.create(
        command.getKey(), messages, bindings);
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
    visit(new Message(element.getSource(), "Ignoring unsupported Module element: " + element));
    return null;
  }

  public Void visit(StaticInjectionRequest staticInjectionRequest) {
    bindings.addStaticInjectionRequest(staticInjectionRequest.getType(), messages);
    return null;
  }
  
  public Void visit(PrivateElements privateElements) {
    GinjectorBindings childCollection = children.next();
    
    // Add information about the elements in the child ginjector to the child bindings
    // TODO(bchambers): Use the tree loggers more intelligently -- when visiting
    // a child bindings collection, we should create a new branch.  This is slightly
    // complicated because we process  in two stages -- once here where we
    // add explicit bindings (and record implicit dependencies), and again later
    // to resolve the implicit dependencies.
    GuiceElementVisitor childVisitor = guiceElementVisitorFactory.create(childCollection);
    childVisitor.visitElements(privateElements.getElements());
    messages.addAll(childVisitor.getMessages());
    
    // Add information about the exposed elements in child to the current binding collection
    for (Key<?> key : privateElements.getExposedKeys()) {
      ExposedChildBinding childBinding = exposedChildBindingProvider.get();
      childBinding.setChild(childCollection);
      childBinding.setKey(key);
      logger.log(TreeLogger.TRACE, "Child binding for " + key + ": " + childBinding);
      BindingEntry entry =
          new BindingEntry(childBinding, BindingContext.forElement(privateElements));
      bindings.addBinding(key, entry);
    }
    return null;
  }

  public List<Message> getMessages() {
    return messages;
  }
}