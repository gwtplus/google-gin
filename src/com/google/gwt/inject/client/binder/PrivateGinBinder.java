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

package com.google.gwt.inject.client.binder;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * Extension of GinBinder that allows for exposing keys.  This is used when
 * creating private modules, where bindings would not be exposed by default.
 */
public interface PrivateGinBinder extends GinBinder {
  /**
   * Expose the given key.
   */
  void expose(Key<?> key);
  
  /**
   * Expose the given class.  Returns a {@link GinAnnotatedElementBuilder} which
   * can be used for adding an annotation.
   */
  GinAnnotatedElementBuilder expose(Class<?> type);
  
  /**
   * Expose the given type.  Returns a {@link GinAnnotatedElementBuilder} which
   * can be used for adding an annotation.
   */
  GinAnnotatedElementBuilder expose(TypeLiteral<?> type);
}
