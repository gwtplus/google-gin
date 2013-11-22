/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.inject.client.binder.GinAnnotatedElementBuilder;
import com.google.inject.binder.AnnotatedElementBuilder;

import java.lang.annotation.Annotation;

public class AnnotatedElementBuilderAdapter
    implements GinAnnotatedElementBuilder {

  private final AnnotatedElementBuilder guiceBuilder;

  AnnotatedElementBuilderAdapter(AnnotatedElementBuilder builder) {
    this.guiceBuilder = builder;
  }

  public void annotatedWith(Class<? extends Annotation> annotationType) {
    guiceBuilder.annotatedWith(annotationType);
  }

  public void annotatedWith(Annotation annotation) {
    guiceBuilder.annotatedWith(annotation);
  }
}
