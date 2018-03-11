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
package com.google.gwt.inject.rebind.binding;

import com.google.gwt.inject.rebind.reflect.NoSourceNameException;
import com.google.gwt.inject.rebind.util.InjectorMethod;
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.inject.rebind.util.SourceSnippet;
import com.google.inject.TypeLiteral;

import java.util.Collection;
import java.util.List;

/**
 * Interface used by {@code InjectorGeneratorImpl} to represent different kinds
 * of bindings.
 */
public interface Binding {

  /**
   * Gets one or more fully formed Java statements that create the bound value
   * and store it in a new local variable named {@code result}.  If additional
   * methods are to be created, the given {@link NameGenerator} should be used
   * to choose their names, and they should be added to {@code methodsOutput}.
   *
   * @throws NoSourceNameException if source name is not available for type
   */
  SourceSnippet getCreationStatements(NameGenerator nameGenerator,
      List<InjectorMethod> methodsOutput) throws NoSourceNameException;

  /**
   * Returns the context in which this binding was created.
   */
  Context getContext();

  /**
   * Returns the package in which the getter for the bound key is created.  This
   * is used by other bindings and by the top-level interface to determine which
   * package this binding's method was written to.  Implementors may assume that
   * all other bindings have been created and placed in their respective
   * {@link GinjectorBindings} objects.
   *
   * <p>Typically this will be the package containing the type that is returned
   * by the getter, but the only requirement is that it has to be a package
   * where the return type is visible.
   */
  String getGetterMethodPackage();

  /**
   * Returns the set of dependencies that this binding produces.  This contains edges coming into
   * the key that this type binds (from {@link Dependency#GINJECTOR}) as well as dependencies that
   * this binding needs.
   */
  Collection<Dependency> getDependencies();

  /**
   * Returns the set of types for which this binding requires member injection methods to be
   * written.
   */
  Collection<TypeLiteral<?>> getMemberInjectRequests();
}
