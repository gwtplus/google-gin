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

import static com.google.gwt.inject.client.multibindings.GinMapBinder.newMapBinder;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.multibindings.TestTypes.AboutPlaceProvider;
import com.google.gwt.inject.client.multibindings.TestTypes.HomePlaceProvider;
import com.google.gwt.inject.client.multibindings.TestTypes.Place;
import com.google.gwt.inject.client.multibindings.TestTypes.ProviderForXImpl2;
import com.google.gwt.inject.client.multibindings.TestTypes.X;
import com.google.gwt.inject.client.multibindings.TestTypes.XImpl1;
import com.google.gwt.inject.client.multibindings.TestTypes.XImpl2;
import com.google.gwt.inject.client.multibindings.TestTypes.XWithGenerics;
import com.google.gwt.inject.client.multibindings.TestTypes.Y;
import com.google.gwt.inject.client.multibindings.TestTypes.YImpl;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class GinMapBinderTest extends GWTTestCase {

  public static class GinModuleWithNoBinding extends AbstractGinModule {
    @Override
    protected void configure() {
      newMapBinder(binder(), String.class, X.class);
    }
  }

  @GinModules(GinModuleWithNoBinding.class)
  public interface NoBindingGinjector extends Ginjector {
    Map<String, X> getMap();
  }

  public void testInject_empty() throws Exception {
    NoBindingGinjector injector = GWT.create(NoBindingGinjector.class);
    Map<String, X> map = injector.getMap();
    assertTrue(map.isEmpty());
  }

  public static class GinModuleWithXY extends AbstractGinModule {
    @Override
    protected void configure() {
      newMapBinder(binder(), String.class, X.class).addBinding("1").to(XImpl1.class);
      newMapBinder(binder(), String.class, Y.class).addBinding("1").to(YImpl.class);
    }
  }

  public static class GinModuleWithMoreX extends AbstractGinModule {
    @Override
    protected void configure() {
      GinMapBinder<String, X> mapBinder = newMapBinder(binder(), String.class, X.class);
      mapBinder.addBinding("2").to(XImpl2.class);
      mapBinder.addBinding("3").to(XImpl1.class);
      TypeLiteral<XWithGenerics<YImpl>> x3 = new TypeLiteral<XWithGenerics<YImpl>>() {};
      mapBinder.addBinding("4").to(x3);
    }
  }

  @GinModules({GinModuleWithXY.class, GinModuleWithMoreX.class})
  public interface MapGinjector extends Ginjector {
    Map<String, X> getMapX();
    Map<String, Y> getMapY();
    Map<String, Provider<X>> getProviderMapX();
  }

  public void testInject() throws Exception {
    MapGinjector injector = GWT.create(MapGinjector.class);

    Map<String, X> mapX = injector.getMapX();
    assertEquals(4, mapX.size());
    assertTrue(mapX.get("1") instanceof XImpl1);
    assertTrue(mapX.get("2") instanceof XImpl2);
    assertTrue(mapX.get("3") instanceof XImpl1);
    assertTrue(mapX.get("4") instanceof XWithGenerics<?>);
    assertTrue(((XWithGenerics<?>) mapX.get("4")).object instanceof YImpl);

    Map<String, Y> mapY = injector.getMapY();
    assertEquals(1, mapY.size());
    assertTrue(mapY.get("1") instanceof YImpl);
  }

  public void testInject_mapProvider() throws Exception {
    MapGinjector injector = GWT.create(MapGinjector.class);

    Map<String, Provider<X>> mapX = injector.getProviderMapX();
    assertEquals(4, mapX.size());
    assertTrue(mapX.get("1").get() instanceof XImpl1);
    assertTrue(mapX.get("2").get() instanceof XImpl2);
    assertTrue(mapX.get("3").get() instanceof XImpl1);
    assertTrue(mapX.get("4").get() instanceof XWithGenerics<?>);
    assertTrue(((XWithGenerics<?>) mapX.get("4").get()).object instanceof YImpl);
  }

  public void testInject_sameMapTwice() throws Exception {
    MapGinjector injector = GWT.create(MapGinjector.class);

    Map<String, X> map1 = injector.getMapX();
    Map<String, X> map2 = injector.getMapX();

    assertEquals(map1.size(), map2.size());

    assertTrue(map2.get("1") instanceof XImpl1);
    assertNotSame(map1.get("1"), map2.get("1"));

    assertTrue(map2.get("2") instanceof XImpl2);
    assertNotSame(map1.get("2"), map2.get("2"));

    // Provider maps are safe to reuse
    assertSame(injector.getProviderMapX(), injector.getProviderMapX());
  }

  public static class GinModuleWithScopedX extends AbstractGinModule {
    @Override
    protected void configure() {
      newMapBinder(binder(), String.class, X.class)
          .addBinding("2").to(XImpl1.class).in(Singleton.class);
    }
  }

  @GinModules({GinModuleWithXY.class, GinModuleWithScopedX.class})
  public interface MapGinjectorWithScope extends Ginjector {
    Map<String, X> getMap();
  }

  public void testInject_sameMapTwiceWithScope() throws Exception {
    MapGinjectorWithScope injector = GWT.create(MapGinjectorWithScope.class);

    Map<String, X> map1 = injector.getMap();
    Map<String, X> map2 = injector.getMap();

    assertEquals(map1.size(), map2.size());
    assertNotSame(map1.get("1"), map2.get("1"));
    assertSame(map1.get("2"), map2.get("2"));
  }

  public static class GinModuleWithKeyProviders extends AbstractGinModule {
    @Override
    protected void configure() {
      GinMapBinder.newMapBinder(binder(), Place.class, X.class)
          .addBinding(HomePlaceProvider.class).to(XImpl1.class);
      GinMapBinder.newMapBinder(binder(), Place.class, X.class)
          .addBinding(AboutPlaceProvider.class).to(XImpl2.class);
    }
  }

  @GinModules(GinModuleWithKeyProviders.class)
  public interface PlacesGinjector extends Ginjector {
    Map<Place, X> getMap();
  }

  public void testInject_keyProvider() throws Exception {
    PlacesGinjector injector = GWT.create(PlacesGinjector.class);

    Map<Place, X> map = injector.getMap();
    assertEquals(2, map.size());
    assertTrue(map.get(new Place("home")) instanceof XImpl1);
    assertTrue(map.get(new Place("about")) instanceof XImpl2);
  }

  public static class GinModuleWithDuplicateBinding extends AbstractGinModule {
    @Override
    protected void configure() {
      install(new GinModuleWithXY());
      newMapBinder(binder(), String.class, X.class).addBinding("1").to(XImpl2.class);
    }
  }

  @GinModules(GinModuleWithDuplicateBinding.class)
  public interface DoubleBindingGinjector extends Ginjector {
    Map<String, X> getMapX();
    Map<String, Provider<X>> getProviderMapX();
  }

  public void testInject_dontPermitDuplicates() {
    DoubleBindingGinjector injector = GWT.create(DoubleBindingGinjector.class);
    try {
      injector.getMapX();
      fail("should have thrown exception");
    } catch (IllegalStateException e) {
      assertTrue(e.toString().contains("duplicate"));
    }
    try {
      injector.getProviderMapX();
      fail("should have thrown exception");
    } catch (IllegalStateException e) {
      assertTrue(e.toString().contains("duplicate"));
    }
  }

  public static class GinModuleForPermittingDuplicate extends AbstractGinModule {
    @Override
    protected void configure() {
      newMapBinder(binder(), String.class, X.class).permitDuplicates();
      newMapBinder(binder(), String.class, X.class).permitDuplicates(); // multiple calls are OK
    }
  }

  @GinModules(GinModuleForPermittingDuplicate.class)
  public interface MultimapGinjector extends DoubleBindingGinjector {
    Map<String, Set<X>> getMultimapX();
    Map<String, Set<Provider<X>>> getProviderMultimapX();
  }

  public void testInject_multimap() throws Exception {
    MultimapGinjector injector = GWT.create(MultimapGinjector.class);
    Map<String, Set<X>> mapX = injector.getMultimapX();
    assertEquals(1, mapX.size());
    Iterator<X> iterator = mapX.get("1").iterator();
    assertTrue(iterator.next() instanceof XImpl1);
    assertTrue(iterator.next() instanceof XImpl2);
  }

  public void testInject_providerMultimap() throws Exception {
    MultimapGinjector injector = GWT.create(MultimapGinjector.class);
    Map<String, Set<Provider<X>>> mapX = injector.getProviderMultimapX();
    assertEquals(1, mapX.size());
    Iterator<Provider<X>> iterator = mapX.get("1").iterator();
    assertTrue(iterator.next().get() instanceof XImpl1);
    assertTrue(iterator.next().get() instanceof XImpl2);
  }

  public void testInject_sameMultimapTwice() throws Exception {
    MultimapGinjector injector = GWT.create(MultimapGinjector.class);
    Map<String, Set<X>> map1 = injector.getMultimapX();
    Map<String, Set<X>> map2 = injector.getMultimapX();
    assertEquals(map1.size(), map2.size());

    Iterator<X> iterator1 = map1.get("1").iterator();
    Iterator<X> iterator2 = map2.get("1").iterator();

    X next1 = iterator1.next();
    X next2 = iterator2.next();
    assertTrue(next1 instanceof XImpl1);
    assertTrue(next2 instanceof XImpl1);
    assertNotSame(next1, next2);

    next1 = iterator1.next();
    next2 = iterator2.next();
    assertTrue(next1 instanceof XImpl2);
    assertTrue(next2 instanceof XImpl2);
    assertNotSame(next1, next2);

    // Provider maps are safe to reuse
    assertSame(injector.getProviderMultimapX(), injector.getProviderMultimapX());
  }

  public static class GinModuleWithRegularBind extends AbstractGinModule {
    @Override
    protected void configure() {
      newMapBinder(binder(), String.class, X.class).addBinding("1").to(XImpl1.class);
      bind(X.class).to(XImpl2.class);
    }
  }

  @GinModules(GinModuleWithRegularBind.class)
  public interface GinjectorWithRegularBind extends Ginjector {
    Map<String, X> getMapX();
    X getX();
  }

  public void testInject_sameInterfaceBoundWithBothRegularAndMapBind() throws Exception {
    GinjectorWithRegularBind injector = GWT.create(GinjectorWithRegularBind.class);
    assertTrue(injector.getMapX().get("1") instanceof XImpl1);
    assertTrue(injector.getX() instanceof XImpl2);
  }

  public static class GinModuleWithProvider extends AbstractGinModule {
    @Override
    protected void configure() {
      newMapBinder(binder(), String.class, X.class)
          .addBinding("P2").toProvider(ProviderForXImpl2.class);
    }
  }

  @GinModules({GinModuleWithXY.class, GinModuleWithProvider.class})
  public interface GinjectorWithProvider extends Ginjector {
    Map<String, X> getMapX();
  }

  public void testInject_valueBoundWithProvider() throws Exception {
    GinjectorWithProvider injector = GWT.create(GinjectorWithProvider.class);
    Map<String, X> mapX = injector.getMapX();
    assertEquals(2, mapX.size());
    assertTrue(mapX.get("1") instanceof XImpl1);
    assertTrue(mapX.get("P2") instanceof XImpl2);
  }

  public static class GinModuleWithAnnotations extends AbstractGinModule {
    @Override
    protected void configure() {
      GinMapBinder<String, X> mapBinder = newMapBinder(binder(), String.class, X.class);
      mapBinder.addBinding("1").to(XImpl1.class);
      mapBinder.addBinding("2").to(XImpl2.class);

      GinMapBinder<String, X> mapBinderA =
          newMapBinder(binder(), String.class, X.class, Names.named("a"));
      mapBinderA.addBinding("1").to(XImpl1.class);
      mapBinderA.addBinding("2").to(XImpl1.class);

      GinMapBinder<String, X> mapBinderB =
          newMapBinder(binder(), String.class, X.class, Names.named("b"));
      mapBinderB.addBinding("1").to(XImpl2.class);
      mapBinderB.addBinding("2").to(XImpl2.class);
    }
  }

  @GinModules(GinModuleWithAnnotations.class)
  public interface GinjectorWithAnnotations extends Ginjector {
    Map<String, X> getMapX();
    @Named("a") Map<String, X> getMapXa();
    @Named("b") Map<String, X> getMapXb();
  }

  public void testInject_annotated() throws Exception {
    GinjectorWithAnnotations injector = GWT.create(GinjectorWithAnnotations.class);

    Map<String, X> mapX = injector.getMapX();
    assertEquals(2, mapX.size());
    assertTrue(mapX.get("1") instanceof XImpl1);
    assertTrue(mapX.get("2") instanceof XImpl2);

    Map<String, X> mapXa = injector.getMapXa();
    assertEquals(2, mapXa.size());
    assertTrue(mapXa.get("1") instanceof XImpl1);
    assertTrue(mapXa.get("2") instanceof XImpl1);

    Map<String, X> mapXb = injector.getMapXb();
    assertEquals(2, mapXb.size());
    assertTrue(mapXb.get("1") instanceof XImpl2);
    assertTrue(mapXb.get("2") instanceof XImpl2);
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
}
