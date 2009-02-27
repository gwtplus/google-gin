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
package com.google.gwt.inject.client;

import com.google.gwt.inject.client.binder.GinBinder;

/**
 * GIN counterpart of Guice's {@code Module}. A {@code GinModule}
 * implementation is compiled both by the regular Java compiler and
 * by the GWT compiler.
 *
 * <p>The regular-compiled version is used at GWT compile time as part
 * of the implementation of the {@code Generator} that outputs the generated
 * {@code Ginjector} class. Notably, the {@code Generator} calls the
 * {@code configure} method on the module class.
 *
 * <p>The GWT-compiled version is used client-side at runtime. This is
 * necessary to support provider methods. Important, the GWT client
 * code never actually calls the {@code configure} method on the module class
 * and it is eliminated as dead code by the compiler. So {@code configure}
 * needs to be GWT compilable, but the resulting code will never execute.
 *
 * <p>Because {@code GinModule} classes need to compile as GWT code,
 * all types that modules reference must be GWT translatable. This
 * is a problem for types like {@link com.google.inject.Key Key} and
 * {@link com.google.inject.TypeLiteral TypeLiteral}, since they normally
 * reference {@link java.lang.reflect.Type} instances and other untranslatable
 * things. But since the {@code configure} method that uses them won't actually
 * be executed, all we need is to satisfy the compiler. Thus, we can get away
 * with using {@code super-source} to provide dummy stubs of {@code Key} and
 * {@code TypeLiteral}.
 */
public interface GinModule {
  void configure(GinBinder binder);
}
