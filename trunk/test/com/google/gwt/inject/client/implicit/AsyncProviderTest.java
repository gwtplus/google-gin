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

package com.google.gwt.inject.client.implicit;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AsyncProvider;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The tests here use the following approach for testing asynchronous code:
 * 
 * http://code.google.com/webtoolkit/doc/latest/DevGuideTesting.html#DevGuideAsynchronousTesting
 *
 */
public class AsyncProviderTest extends GWTTestCase {

  private AsyncProviderGinjector injector;
  
  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    injector = GWT.create(AsyncProviderGinjector.class);
  }
  
  public void testAsyncProviderInGinjector() {
    AsyncProvider<Foo> fooAsyncProvider = injector.getFooAsyncProvider();
    assertNotNull(fooAsyncProvider);
    delayTestFinish(500);
    fooAsyncProvider.get(new AbstractAsyncCallback<Foo>() {

      public void onSuccess(Foo result) {
        assertNotNull(result);
        finishTest();
      }
      
    });
  }

  public void testAsyncProviderInjection() {
    FooAsync fooAsync = injector.getFooAsync();
    AsyncProvider<Foo> fooAsyncProvider = fooAsync.getFooAsyncProvider();
    assertNotNull(fooAsyncProvider);
    delayTestFinish(500);
    fooAsyncProvider.get(new AbstractAsyncCallback<Foo>() {

      public void onSuccess(Foo result) {
        assertNotNull(result);
        finishTest();
      }
      
    });
  }
  
  public void testAsyncProviderWorksWithSingletons() {
    final AsyncProvider<Foo> fooAsyncProvider = injector.getFooAsyncProvider();
    // Foo is singleton
    assertNotNull(fooAsyncProvider);
    delayTestFinish(500);
    fooAsyncProvider.get(new AbstractAsyncCallback<Foo>() {

      public void onSuccess(final Foo foo1) {
        assertNotNull(foo1);
        delayTestFinish(500);
        fooAsyncProvider.get(new AbstractAsyncCallback<Foo>() {

          public void onSuccess(Foo foo2) {
            assertNotNull(foo2);
            assertSame(foo1, foo2);
            finishTest();
          }

        });
      }
    });
  }
  
  public void testAsyncProviderCreatesNewInstances() {
    final AsyncProvider<FooAsync> fooAsyncAsyncProvider = injector.getFooAsyncAsyncProvider();
    // FooAsync is not a singleton, calling AsyncProvider<FooAsync>.get()
    // should create new instances
    assertNotNull(fooAsyncAsyncProvider);
    delayTestFinish(500);
    fooAsyncAsyncProvider.get(new AbstractAsyncCallback<FooAsync>() {
      
      public void onSuccess(final FooAsync fooAsync1) {
        assertNotNull(fooAsync1);
        delayTestFinish(500);
        fooAsyncAsyncProvider.get(new AbstractAsyncCallback<FooAsync>() {
          
          public void onSuccess(final FooAsync fooAsync2) {
            assertNotNull(fooAsync2);
            assertNotSame(fooAsync1, fooAsync2);
            finishTest();
          }
        });
      }
    });
  }
  
  public void testAsyncProviderWithSupertype() {
    AsyncProvider<Foo> fooAsyncProvider = injector.getFooAsyncProvider();
    assertNotNull(fooAsyncProvider);
    delayTestFinish(500);
    fooAsyncProvider.get(new AbstractAsyncCallback<Object>() {

      public void onSuccess(Object result) {
        assertNotNull(result);
        finishTest();
      }
      
    });
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }

  private abstract class AbstractAsyncCallback<T> implements AsyncCallback<T> {

    public void onFailure(Throwable caught) {
      fail("Should not happen");
    }
  }
}
