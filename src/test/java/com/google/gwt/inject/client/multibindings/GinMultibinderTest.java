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

import static com.google.gwt.inject.client.multibindings.GinMultibinder.newSetBinder;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.multibindings.TestTypes.ProviderForXImpl2;
import com.google.gwt.inject.client.multibindings.TestTypes.X;
import com.google.gwt.inject.client.multibindings.TestTypes.XImpl1;
import com.google.gwt.inject.client.multibindings.TestTypes.XImpl2;
import com.google.gwt.inject.client.multibindings.TestTypes.XWithEquals;
import com.google.gwt.inject.client.multibindings.TestTypes.Y;
import com.google.gwt.inject.client.multibindings.TestTypes.YImpl;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import java.util.Iterator;
import java.util.Set;

public class GinMultibinderTest extends GWTTestCase {

  public static class GinModuleWithNoBinding extends AbstractGinModule {
    @Override
    protected void configure() {
      newSetBinder(binder(), X.class);
    }
  }

  @GinModules(GinModuleWithNoBinding.class)
  public interface NoBindingGinjector extends Ginjector {
    Set<X> getSet();
  }

  public void testInject_empty() throws Exception {
    NoBindingGinjector injector = GWT.create(NoBindingGinjector.class);
    Set<X> set = injector.getSet();
    assertTrue(set.isEmpty());
  }

  public static class GinModuleWithXY extends AbstractGinModule {
    @Override
    protected void configure() {
      newSetBinder(binder(), X.class).addBinding().to(XImpl1.class);
      newSetBinder(binder(), Y.class).addBinding().to(YImpl.class);
    }
  }

  public static class GinModuleWithMoreX extends AbstractGinModule {
    @Override
    protected void configure() {
      GinMultibinder<X> setBinder = newSetBinder(binder(), X.class);
      setBinder.addBinding().to(XImpl2.class);
      setBinder.addBinding().to(XImpl1.class);
    }
  }

  @GinModules({GinModuleWithXY.class, GinModuleWithMoreX.class})
  public interface SetGinjector extends Ginjector {
    Set<X> getSetX();
    Set<Y> getSetY();
  }

  public void testInject() throws Exception {
    SetGinjector injector = GWT.create(SetGinjector.class);

    Set<X> setX = injector.getSetX();
    assertEquals(3, setX.size());

    Iterator<X> iterator = setX.iterator();
    assertTrue(iterator.next() instanceof XImpl1);
    assertTrue(iterator.next() instanceof XImpl2);
    assertTrue(iterator.next() instanceof XImpl1);

    Set<Y> setY = injector.getSetY();
    assertEquals(1, setY.size());
    assertTrue(setY.iterator().next() instanceof YImpl);
  }

  public void testInject_sameSetTwice() throws Exception {
    SetGinjector injector = GWT.create(SetGinjector.class);
    Set<X> set1 = injector.getSetX();
    Set<X> set2 = injector.getSetX();

    assertEquals(set1.size(), set2.size());

    Iterator<X> iterator1 = set1.iterator();
    Iterator<X> iterator2 = set2.iterator();

    X element = iterator2.next();
    assertTrue(element instanceof XImpl1);
    assertNotSame(iterator1.next(), element);

    element = iterator2.next();
    assertTrue(element instanceof XImpl2);
    assertNotSame(iterator1.next(), element);

    element = iterator2.next();
    assertTrue(element instanceof XImpl1);
    assertNotSame(iterator1.next(), element);
  }

  public static class GinModuleWithScopedX extends AbstractGinModule {
    @Override
    protected void configure() {
      newSetBinder(binder(), X.class).addBinding().to(XImpl1.class).in(Singleton.class);
    }
  }

  @GinModules({GinModuleWithXY.class, GinModuleWithScopedX.class})
  public interface SetGinjectorWithScope extends Ginjector {
    Set<X> getSet();
  }

  public void testInject_sameSetTwiceWithScope() throws Exception {
    SetGinjectorWithScope injector = GWT.create(SetGinjectorWithScope.class);
    Set<X> set1 = injector.getSet();
    Set<X> set2 = injector.getSet();

    assertEquals(set1.size(), set2.size());

    Iterator<X> iterator1 = set1.iterator();
    Iterator<X> iterator2 = set2.iterator();
    assertNotSame(iterator1.next(), iterator2.next());
    assertSame(iterator1.next(), iterator2.next());
  }

  public static class GinModuleWithRegularBind extends AbstractGinModule {
    @Override
    protected void configure() {
      newSetBinder(binder(), X.class).addBinding().to(XImpl1.class);
      bind(X.class).to(XImpl2.class);
    }
  }

  @GinModules(GinModuleWithRegularBind.class)
  public interface GinjectorWithRegularBind extends Ginjector {
    Set<X> getSet();
    X getX();
  }

  public void testInject_sameInterfaceBoundWithBothRegularAndMultiBind() throws Exception {
    GinjectorWithRegularBind injector = GWT.create(GinjectorWithRegularBind.class);
    assertEquals(1, injector.getSet().size());
    assertTrue(injector.getSet().iterator().next() instanceof XImpl1);
    assertTrue(injector.getX() instanceof XImpl2);
  }

  public static class GinModuleWithProvider extends AbstractGinModule {
    @Override
    protected void configure() {
      newSetBinder(binder(), X.class).addBinding().toProvider(ProviderForXImpl2.class);
    }
  }

  @GinModules({GinModuleWithXY.class, GinModuleWithProvider.class})
  public interface GinjectorWithProvider extends Ginjector {
    Set<X> getSetX();
  }

  public void testInject_valueBoundWithProvider() throws Exception {
    GinjectorWithProvider injector = GWT.create(GinjectorWithProvider.class);
    Set<X> setX = injector.getSetX();
    assertEquals(2, setX.size());
    Iterator<X> iterator = setX.iterator();
    assertTrue(iterator.next() instanceof XImpl1);
    assertTrue(iterator.next() instanceof XImpl2);
  }

  public static class GinModuleWithAnnotations extends AbstractGinModule {
    @Override
    protected void configure() {
      GinMultibinder<X> mapBinder = newSetBinder(binder(), X.class);
      mapBinder.addBinding().to(XImpl1.class);
      mapBinder.addBinding().to(XImpl2.class);

      GinMultibinder<X> mapBinderA = newSetBinder(binder(), X.class, Names.named("a"));
      mapBinderA.addBinding().to(XImpl1.class);
      mapBinderA.addBinding().to(XImpl1.class);

      GinMultibinder<X> mapBinderB = newSetBinder(binder(), X.class, Names.named("b"));
      mapBinderB.addBinding().to(XImpl2.class);
      mapBinderB.addBinding().to(XImpl2.class);
    }
  }

  @GinModules(GinModuleWithAnnotations.class)
  public interface GinjectorWithAnnotations extends Ginjector {
    Set<X> getSetX();
    @Named("a") Set<X> getSetXa();
    @Named("b") Set<X> getSetXb();
  }

  public void testInject_annotated() throws Exception {
    GinjectorWithAnnotations injector = GWT.create(GinjectorWithAnnotations.class);

    Set<X> setX = injector.getSetX();
    assertEquals(2, setX.size());
    Iterator<X> iterator = setX.iterator();
    assertTrue(iterator.next() instanceof XImpl1);
    assertTrue(iterator.next() instanceof XImpl2);

    Set<X> setXa = injector.getSetXa();
    assertEquals(2, setXa.size());
    iterator = setXa.iterator();
    assertTrue(iterator.next() instanceof XImpl1);
    assertTrue(iterator.next() instanceof XImpl1);

    Set<X> setXb = injector.getSetXb();
    assertEquals(2, setXb.size());
    iterator = setXb.iterator();
    assertTrue(iterator.next() instanceof XImpl2);
    assertTrue(iterator.next() instanceof XImpl2);
  }

  public static class GinModuleWithDuplicateBinding extends AbstractGinModule {
    @Override
    protected void configure() {
      newSetBinder(binder(), X.class).addBinding().to(XWithEquals.class);
      newSetBinder(binder(), X.class).addBinding().to(XWithEquals.class);
    }
  }

  @GinModules(GinModuleWithDuplicateBinding.class)
  public interface DoubleBindingGinjector extends Ginjector {
    Set<X> getSetX();
  }

  public void testInject_dontPermitDuplicates() {
    DoubleBindingGinjector injector = GWT.create(DoubleBindingGinjector.class);
    try {
      injector.getSetX();
      fail("should have thrown exception");
    } catch (IllegalStateException e) {
      assertTrue(e.toString().contains("duplicate"));
    }
  }

  public static class GinModuleForPermittingDuplicate extends AbstractGinModule {
    @Override
    protected void configure() {
      newSetBinder(binder(), X.class).permitDuplicates();
      newSetBinder(binder(), X.class).permitDuplicates(); // shows multiple calls is OK
    }
  }

  @GinModules(GinModuleForPermittingDuplicate.class)
  public interface GinjectorWithPermitDuplicates extends DoubleBindingGinjector {}

  public void testInject_permitDuplicates() {
    GinjectorWithPermitDuplicates injector = GWT.create(GinjectorWithPermitDuplicates.class);
    assertEquals(1, injector.getSetX().size());
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}
