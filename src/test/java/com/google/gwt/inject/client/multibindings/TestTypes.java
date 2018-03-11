/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.inject.client.multibindings;

import com.google.inject.Inject;
import com.google.inject.Provider;

class TestTypes {

  interface X {}

  static class XImpl1 implements X {}

  static class XImpl2 implements X {}

  static class XWithGenerics<T> implements X {
    final T object;
    @Inject public XWithGenerics(T object) {
      this.object = object;
    }
  }

  static class XWithEquals implements X {
    @Override public int hashCode() {
      return getClass().hashCode();
    }
    @Override public boolean equals(Object obj) {
      return getClass().equals(obj.getClass());
    }
  }

  static class ProviderForXImpl2 implements Provider<XImpl2> {
    @Override public XImpl2 get() {
      return new XImpl2();
    }
  }

  interface Y {}

  static class YImpl implements Y {}

  static class Place {
    private String key;

    public Place(String key) {
      this.key = key;
    }

    @Override
    public int hashCode() {
      return key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Place && ((Place) obj).key.equals(key);
    }
  }

  static class HomePlaceProvider implements Provider<Place> {
    @Override
    public Place get() {
      return new Place("home");
    }
  }

  static class AboutPlaceProvider implements Provider<Place> {
    @Override
    public Place get() {
      return new Place("about");
    }
  }
}
