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
package com.google.gwt.inject.client.field;

import com.google.inject.Inject;

public class Basket {

  private final Fruit fruit;
  private final Pear pear;
  private final Tree tree;
  private final Fruit.Worm worm;

  @Inject
  public Basket(Fruit fruit, Pear pear, Tree tree, Fruit.Worm worm) {
    this.fruit = fruit;
    this.pear = pear;
    this.tree = tree;
    this.worm = worm;
  }

  public Fruit getFruit() {
    return fruit;
  }

  public Pear getPear() {
    return pear;
  }

  public Tree getTree() {
    return tree;
  }

  public Fruit.Worm getWorm() {
    return worm;
  }
}
