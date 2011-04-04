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
import com.google.gwt.inject.rebind.util.NameGenerator;
import com.google.gwt.user.rebind.SourceWriter;

import java.util.Collection;

/**
 * Interface used by {@code InjectorGeneratorImpl} to represent different kinds
 * of bindings.
 */
public interface Binding {

  /**
   * Writes the method necessary to create the binding's type to the writer.
   * A method with the {@code creatorMethodSignature} <b>must</b> be written,
   * other methods are optional.
   *
   * @param writer writer that methods are written to
   * @param creatorMethodSignature signature of method that needs to be created
   * @param nameGenerator {@link NameGenerator} to use
   * @throws NoSourceNameException if source name is not available for type
   */
  void writeCreatorMethods(SourceWriter writer, String creatorMethodSignature, 
      NameGenerator nameGenerator) throws NoSourceNameException;

  /**
   * Returns the set of dependencies that this binding produces.  This contains edges coming into
   * the key that this type binds (from {@link Dependency#GINJECTOR}) as well as dependencies that
   * this binding needs.
   */
  Collection<Dependency> getDependencies();
}
