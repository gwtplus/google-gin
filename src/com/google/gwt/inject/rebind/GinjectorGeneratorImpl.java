/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.inject.client.GinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.rebind.binding.BindClassBinding;
import com.google.gwt.inject.rebind.binding.BindConstantBinding;
import com.google.gwt.inject.rebind.binding.BindProviderBinding;
import com.google.gwt.inject.rebind.binding.Binding;
import com.google.gwt.inject.rebind.binding.CallConstructorBinding;
import com.google.gwt.inject.rebind.binding.CallGwtDotCreateBinding;
import com.google.gwt.inject.rebind.binding.ImplicitProviderBinding;
import com.google.gwt.inject.rebind.adapter.GinModuleAdapter;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Singleton;
import com.google.inject.spi.BindingScopingVisitor;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.google.inject.spi.Message;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Does the heavy lifting involved in generating implementations of
 * {@link com.google.gwt.inject.client.Ginjector}. This class is instantiated
 * once per class to generate, so it can keep useful state around in its fields.
 */
class GinjectorGeneratorImpl {
  /**
   * Suffix that is appended to the name of a GWT-RPC service interface to build
   * the name of the asynchronous proxy interface.
   */
  private static final String ASYNC_SERVICE_PROXY_SUFFIX = "Async";

  private final TreeLogger logger;
  private final GeneratorContext ctx;
  private final JClassType injectorInterface;
  private final NameGenerator nameGenerator = new NameGenerator();

  /**
   * Map from key to binding for all types we already have a binding for.
   */
  private final Map<Key<?>, Binding> bindings = new HashMap<Key<?>, Binding>();

  /**
   * Map from key to scope for all types we have a binding for.
   */
  private final Map<Key<?>, GinScope> scopes = new HashMap<Key<?>, GinScope>();

  /**
   * Set of keys for classes that we still need to resolve. Every time a binding
   * is added to {@code bindings}, the key is removed from this set. When this
   * set becomes empty, we know we've satisfied all dependencies.
   */
  private final Set<Key<?>> unresolved = new HashSet<Key<?>>();

  /**
   * A name to method map for the methods on the injector interface, and its
   * superinterfaces that we need to output a concrete method for. These also
   * form the basis for the unresolved classes. Methods redefined (with
   * different annotations) in the base interface take precedence over the
   * superinterfaces.
   */
  private final Map<String, JMethod> injectorMethods = new HashMap<String, JMethod>();

  /**
   * Keeps track of whether we've found an error so we can eventually throw
   * an {@link UnableToCompleteException}. We do this instead of throwing
   * immediately so that we can find more than one error per compilation cycle.
   */
  private boolean foundError = false;

  /**
   * Writer to append Java code for our implementation class.
   */
  private SourceWriter writer;

  public GinjectorGeneratorImpl(TreeLogger logger, GeneratorContext ctx,
      JClassType injectorInterface) {
    this.logger = logger;
    this.ctx = ctx;
    this.injectorInterface = injectorInterface;
  }

  public String generate() throws UnableToCompleteException {
    JPackage interfacePackage = injectorInterface.getPackage();
    String packageName = interfacePackage == null ? "" : interfacePackage.getName();

    String implClassName = injectorInterface.getSimpleSourceName() + "Impl";

    final PrintWriter printWriter = ctx.tryCreate(logger, packageName, implClassName);
    if (printWriter == null) {
      // We've already created it, so just return the class name
      return packageName + "." + implClassName;
    }

    ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(
        packageName, implClassName);

    composerFactory.addImplementedInterface(
        injectorInterface.getParameterizedQualifiedSourceName());
    composerFactory.addImport(GWT.class.getCanonicalName());

    determineInjectorMethods(injectorInterface);
    addUnresolvedEntriesForInjectorInterface();

    createBindingsForModules();

    while (!unresolved.isEmpty()) {
      // Iterate through a copy because we will modify it during iteration
      for (Key<?> key : new ArrayList<Key<?>>(unresolved)) {
        createImplicitBinding(key);
      }

      if (foundError) {
        throw new UnableToCompleteException();
      }
    }

    writer = composerFactory.createSourceWriter(ctx, printWriter);
    outputInterfaceMethods();
    outputBindings();

    // Hackery to see the generated Java output
    /*
    try {
      Field field = PrintWriter.class.getDeclaredField("out");
      field.setAccessible(true);
      System.out.println(field.get(printWriter));
    } catch (Exception e) {
      e.printStackTrace();
    }
    */

    writer.commit(logger);

    return composerFactory.getCreatedClassName();
  }

  private void determineInjectorMethods(JClassType iface) {
    for (JMethod method : iface.getMethods()) {
      if (validateMethod(method)) {
        if (!injectorMethods.containsKey(method.getName())) {
          logger.log(TreeLogger.TRACE, "Found injector method: " + iface.getName() + "#"
              + method.getReadableDeclaration());
          injectorMethods.put(method.getName(), method);
        } else {
          logger.log(TreeLogger.DEBUG, "Ignoring injector method: " + iface.getName() + "#"
              + method.getReadableDeclaration());
        }
      }
    }

    for (JClassType superIface : iface.getImplementedInterfaces()) {
      determineInjectorMethods(superIface);
    }
  }

  private void addUnresolvedEntriesForInjectorInterface() {
    for (JMethod method : injectorMethods.values()) {
      Key<?> key = Util.getKey(method);
      logger.log(TreeLogger.TRACE, "Add unresolved key from injector interface: " + key);
      unresolved.add(key);
    }
  }

  private boolean validateMethod(JMethod method) {
    if (method.getParameters().length > 0) {
      logger.log(TreeLogger.ERROR, String.format(
          "Invalid injector method %s; it has parameters", method));
      foundError = true;
      return false;
    }

    return true;
  }

  private void createBindingsForModules() {
    List<Module> modules = new ArrayList<Module>();
    populateModulesFromInjectorInterface(injectorInterface, modules);

    if (!modules.isEmpty()) {
      List<Element> elements = Elements.getElements(modules);
      for (Element element : elements) {
        GuiceElementVisitor visitor = new GuiceElementVisitor();
        element.acceptVisitor(visitor);

        // Capture any binding errors, any of which we treat as fatal
        List<Message> messages = visitor.getMessages();
        if (!messages.isEmpty()) {
          foundError = true;

          for (Message message : messages) {
            // tostring has both source and message so use that
            logger.log(TreeLogger.ERROR, message.toString(), message.getCause());
          }
        }
      }
    }
  }

  private void populateModulesFromInjectorInterface(JClassType iface, List<Module> modules) {
    GinModules gmodules = iface.getAnnotation(GinModules.class);
    if (gmodules != null) {
      for (Class<? extends GinModule> moduleClass : gmodules.value()) {
        Module module = instantiateGModuleClass(moduleClass);
        if (module != null) {
          modules.add(module);
        }
      }
    }

    for (JClassType superIface : iface.getImplementedInterfaces()) {
      populateModulesFromInjectorInterface(superIface, modules);
    }
  }

  private Module instantiateGModuleClass(Class<? extends GinModule> moduleClassName) {
    try {
      Constructor<? extends GinModule> constructor = moduleClassName.getDeclaredConstructor();
      try {
        constructor.setAccessible(true);
        return new GinModuleAdapter(constructor.newInstance());
      } finally {
        constructor.setAccessible(false);
      }
    } catch (IllegalAccessException e) {
      logger.log(TreeLogger.ERROR, "Error creating module: " + moduleClassName, e);
    } catch (InstantiationException e) {
      logger.log(TreeLogger.ERROR, "Error creating module: " + moduleClassName, e);
    } catch (NoSuchMethodException e) {
      logger.log(TreeLogger.ERROR, "Error creating module: " + moduleClassName, e);
    } catch (InvocationTargetException e) {
      logger.log(TreeLogger.ERROR, "Error creating module: " + moduleClassName, e);
    }

    foundError = true;
    return null;
  }

  private void outputBindings() {
    // Write out each binding
    for (Map.Entry<Key<?>, Binding> entry : bindings.entrySet()) {
      Key<?> key = entry.getKey();
      
      // toString on TypeLiteral outputs the binary name, not the source name
      String typeName = nameGenerator.binaryNameToSourceName(key.getTypeLiteral().toString());
      Binding binding = entry.getValue();

      String getter = nameGenerator.getGetterMethodName(key);
      String creator = nameGenerator.getCreatorMethodName(key);

      // Regardless of the scope, we have a creator method
      writeMethod("private " + typeName + " " + creator + "()",
          binding.getCreatorMethodBody(nameGenerator));

      // Name of the field that we might need
      String field = nameGenerator.getSingletonFieldName(key);

      GinScope scope = determineScope(key);
      switch (scope) {
        case SINGLETON:
          writer.println("private " + typeName + " " + field + " = null;");
          writer.println();
          writer.println("private " + typeName + " " + getter + "()" + " {");
          writer.indent();
          writer.println("if (" + field + " == null) {");
          writer.indent();
          writer.println(field + " = " + creator + "();");
          writer.outdent();
          writer.println("}");
          writer.println("return " + field + ";");
          writer.outdent();
          writer.println("}");
          break;

        case EAGER_SINGLETON:
          // Just call the creator on field init
          writer.println("private final " + typeName + " " + field + " = " + creator + "();");

          writeMethod("private " + typeName + " " + getter + "()", "return " + field + ";");
          break;

        case NO_SCOPE:
          // For none, getter just returns creator
          writeMethod("private " + typeName + " " + getter + "()", "return " + creator + "();");
          break;

        default:
          throw new IllegalStateException();
      }

      writer.println();
    }
  }

  private GinScope determineScope(Key<?> key) {
    GinScope scope = scopes.get(key);
    if (scope == null) {
      scope = GinScope.NO_SCOPE;
    }

    if (scope == GinScope.NO_SCOPE) {
      // Look for scope annotation as a fallback
      Class<?> raw = Util.getRawType(key);
      if (raw.getAnnotation(Singleton.class) != null) {
        scope = GinScope.SINGLETON;
      }
    }

    logger.log(TreeLogger.TRACE, "scope for " + key + ": " + scope);
    return scope;
  }

  private void outputInterfaceMethods() {
    // Add a forwarding method for each method in our injector interface
    for (JMethod method : injectorMethods.values()) {
      writeMethod(method.getReadableDeclaration(false, false, false, false, true),
          "return " + nameGenerator.getGetterMethodName(Util.getKey(method)) + "();");
    }
  }

  private void writeMethod(String methodDecl, String body) {
    writer.println(methodDecl + " {");
    writer.indent();
    writer.println(body);
    writer.outdent();
    writer.println("}");
    writer.println();
  }

  private void createImplicitBinding(Key<?> key) {
    Binding binding = null;
    Type keyType = key.getTypeLiteral().getType();

    if (keyType instanceof ParameterizedType) {
      ParameterizedType keyParamType = (ParameterizedType) keyType;

      // Create implicit Provider<T> binding
      if (keyParamType.getRawType() == Provider.class) {
        binding = new ImplicitProviderBinding(key);

        // TODO(bstoler): Scope the provider binding like the thing being provided?
      }
    }

    if (binding == null) {
      JClassType classType = ctx.getTypeOracle().findType(nameGenerator.binaryNameToSourceName(Util.getRawType(key).getName()));
      if (classType != null) {
        binding = createImplicitBindingForClass(classType);
      } else {
        logger.log(TreeLogger.ERROR, "Class not found: " + key);
        foundError = true;
      }
    }

    logger.log(TreeLogger.TRACE, "Implicit binding for " + key + ": " + binding);

    if (binding != null) {
      addBinding(key, binding);
    }
  }

  private Binding createImplicitBindingForClass(JClassType classType) {
    // Special case: When injecting a remote service proxy call GWT.create on
    // the synchronous service interface
    String name = classType.getQualifiedSourceName();
    if (classType.isInterface() != null && name.endsWith(ASYNC_SERVICE_PROXY_SUFFIX)) {
      String serviceInterfaceName =
          name.substring(0, name.length() - ASYNC_SERVICE_PROXY_SUFFIX.length());
      TypeOracle typeOracle = ctx.getTypeOracle();
      JClassType serviceInterface = typeOracle.findType(serviceInterfaceName);
      JClassType marker = typeOracle.findType(RemoteService.class.getName());
      if (serviceInterface != null && marker != null && serviceInterface.isAssignableTo(marker)) {
        return new CallGwtDotCreateBinding(serviceInterface);
      }
    }

    // Either call the @Inject constructor or use GWT.create
    JConstructor[] constructors = classType.getConstructors();
    if (constructors.length == 0 ||
        (constructors.length == 1 && constructors[0].getParameters().length == 0)) {
      return new CallGwtDotCreateBinding(classType);
    } else {
      JConstructor constructor = getInjectConstructor(classType);
      if (constructor != null) {
        return new CallConstructorBinding(constructor);
      }
    }

    logger.log(TreeLogger.ERROR, "No @Inject or default constructor found for " + classType);
    foundError = true;
    return null;
  }

  private void addBinding(Key<?> key, Binding binding) {
    if (bindings.containsKey(key)) {
      logger.log(TreeLogger.ERROR, "Double-bound: " + key + ". "
          + bindings.get(key) + ", " + binding);
      foundError = true;
      return;
    }

    bindings.put(key, binding);
    unresolved.remove(key);

    // Clone the returned set so we can safely mutate it
    Set<Key<?>> nowUnresolved = new HashSet<Key<?>>(binding.getRequiredKeys());
    nowUnresolved.removeAll(bindings.keySet());

    if (!nowUnresolved.isEmpty()) {
      logger.log(TreeLogger.TRACE, "Add unresolved as dep from binding to "
          + key + ": " + nowUnresolved);
      unresolved.addAll(nowUnresolved);
    }

    logger.log(TreeLogger.TRACE, "bound " + key + " to " + binding);
  }

  private JConstructor getInjectConstructor(JClassType classType) {
    JConstructor[] constructors = classType.getConstructors();

    JConstructor injectConstructor = null;
    for (JConstructor constructor : constructors) {
      if (constructor.getAnnotation(Inject.class) != null) {
        if (injectConstructor == null) {
          injectConstructor = constructor;
        } else {
          logger.log(TreeLogger.ERROR, "More than one @Inject constructor found for "
              + classType + "; " + injectConstructor + ", " + constructor);
          foundError = true;
        }
      }
    }

    return injectConstructor;
  }

  private class GuiceElementVisitor extends DefaultElementVisitor<Void> {
    private final List<Message> messages = new ArrayList<Message>();

    @Override
    public <T> Void visitBinding(com.google.inject.Binding<T> command) {
      GuiceBindingVisitor<T> bindingVisitor = new GuiceBindingVisitor<T>(command.getKey());
      command.acceptTargetVisitor(bindingVisitor);
      command.acceptScopingVisitor(bindingVisitor);
      return null;
    }

    @Override
    public Void visitMessage(Message message) {
      messages.add(message);
      return null;
    }

    @Override
    protected Void visitElement(Element element) {
      visitMessage(new Message(element.getSource(),
          "Ignoring unsupported Module element: " + element));
      return null;
    }

    public List<Message> getMessages() {
      return messages;
    }
  }

  private class GuiceBindingVisitor<T> extends DefaultBindingTargetVisitor<T, Void>
      implements BindingScopingVisitor<Void> {
    private final Key<T> targetKey;

    public GuiceBindingVisitor(Key<T> targetKey) {
      this.targetKey = targetKey;
    }

    @Override
    public Void visitProviderKey(Key<? extends Provider<? extends T>> providerKey) {
      addBinding(targetKey, new BindProviderBinding(providerKey));
      return null;
    }

    @Override
    public Void visitKey(Key<? extends T> key) {
      addBinding(targetKey, new BindClassBinding(key));
      return null;
    }

    @Override
    public Void visitInstance(T instance) {
      Binding binding = BindConstantBinding.create(targetKey, instance);
      if (binding != null) {
        addBinding(targetKey, binding);
      } else {
        logger.log(TreeLogger.ERROR, "Instance binding not supported; key="
            + targetKey + " inst=" + instance);
        foundError = true;
      }

      return null;
    }

    @Override
    public Void visitUntargetted() {
      // Do nothing -- this just means to use our normal implicit strategies
      // We need this override to avoid giving an error due to visitOther.
      return null;
    }

    @Override
    protected Void visitOther() {
      logger.log(TreeLogger.ERROR, "Unsupported binding provided for key: " + targetKey);
      foundError = true;
      return null;
    }

    public Void visitEagerSingleton() {
      scopes.put(targetKey, GinScope.EAGER_SINGLETON);
      return null;
    }

    public Void visitScope(Scope scope) {
      logger.log(TreeLogger.ERROR, "Explicit scope unsupported: key=" + targetKey
          + " scope=" + scope);
      foundError = true;
      return null;
    }

    public Void visitScopeAnnotation(Class<? extends Annotation> scopeAnnotation) {
      if (scopeAnnotation == Singleton.class) {
        scopes.put(targetKey, GinScope.SINGLETON);
      } else {
        logger.log(TreeLogger.ERROR, "Unsupported scope annoation: key=" + targetKey
            + " scope=" + scopeAnnotation);
        foundError = true;
      }
      return null;
    }

    public Void visitNoScoping() {
      scopes.put(targetKey, GinScope.NO_SCOPE);
      return null;
    }
  }
}
