/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.inject.client.optional;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class OptionalMembersClass {

  @Inject(optional=true) @Named("unavailable") public String unavailableField = null;
  @Inject(optional=true) @Named("available") public String availableField = null;

  public String unavailableMethod = null;
  public String availableMethod = null;

  public Bar availableObject = null;
  public Foo unavailableObject = null;

  public Provider<Foo> unavailableProvider = null;
  public Provider<Bar> availableProvider = null;

  @Inject(optional=true)
  public void setUnavailable(@Named("unavailable") String foo) {
    this.unavailableMethod = foo;
  }

  @Inject(optional=true)
  public void setAvailable(@Named("available") String bar) {
    this.availableMethod = bar;
  }

  @Inject(optional=true)
  public void setAvailableObject(Bar availableObject) {
    this.availableObject = availableObject;
  }

  @Inject(optional=true)
  public void setUnavailableObject(Foo unavailableObject) {
    this.unavailableObject = unavailableObject;
  }

  @Inject(optional=true)
  public void setUnavailableProvider(Provider<Foo> unavailableProvider) {
    this.unavailableProvider = unavailableProvider;
  }

  @Inject(optional=true)
  public void setAvailableProvider(Provider<Bar> availableProvider) {
    this.availableProvider = availableProvider;
  }
}
