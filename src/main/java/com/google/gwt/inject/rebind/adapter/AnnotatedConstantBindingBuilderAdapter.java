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
package com.google.gwt.inject.rebind.adapter;

import com.google.gwt.inject.client.binder.GinAnnotatedConstantBindingBuilder;
import com.google.gwt.inject.client.binder.GinConstantBindingBuilder;
import com.google.inject.binder.AnnotatedConstantBindingBuilder;

import java.lang.annotation.Annotation;

class AnnotatedConstantBindingBuilderAdapter implements GinAnnotatedConstantBindingBuilder {
  private final AnnotatedConstantBindingBuilder guiceBuilder;

  public AnnotatedConstantBindingBuilderAdapter(
      AnnotatedConstantBindingBuilder guiceBuilder) {
    this.guiceBuilder = guiceBuilder;
  }

  public GinConstantBindingBuilder annotatedWith(Class<? extends Annotation> aClass) {
    return new ConstantBindingBuilderAdapter(guiceBuilder.annotatedWith(aClass));
  }

  public GinConstantBindingBuilder annotatedWith(Annotation annotation) {
    return new ConstantBindingBuilderAdapter(guiceBuilder.annotatedWith(annotation));
  }
}
