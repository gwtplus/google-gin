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
package com.google.gwt.inject.client.nonpublic.secret;

import com.google.inject.Inject;

/**
 * Class with a bunch of instances of a package-private injected.
 */
public class SecretMain {
  @Inject private PrivateClass privateViaField;
  private PrivateClass privateViaMethod;
  private final PrivateClass privateViaConstructor;

  @Inject private Inner innerViaField;
  private Inner innerViaMethod;
  private final Inner innerViaConstructor;

  @Inject
  SecretMain(PrivateClass privateViaConstructor, Inner innerViaConstructor) {
    this.privateViaConstructor = privateViaConstructor;
    this.innerViaConstructor = innerViaConstructor;
  }

  @Inject
  void setPrivateViaMethod(PrivateClass privateViaMethod) {
    this.privateViaMethod = privateViaMethod;
  }

  public void setInnerViaMethod(Inner innerViaMethod) {
    this.innerViaMethod = innerViaMethod;
  }

  public PrivateClass getPrivateViaField() {
    return privateViaField;
  }

  public PrivateClass getPrivateViaMethod() {
    return privateViaMethod;
  }

  public PrivateClass getPrivateViaConstructor() {
    return privateViaConstructor;
  }

  public Inner getInnerViaField() {
    return innerViaField;
  }

  public Inner getInnerViaMethod() {
    return innerViaMethod;
  }

  public Inner getInnerViaConstructor() {
    return innerViaConstructor;
  }

  private static class Inner {}
}
