/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.inject.client.privatebasic;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.inject.client.PrivateGinModule;
import com.google.gwt.inject.client.privatebasic.PrivateBasicTest.GameModule.Suits;
import com.google.gwt.inject.client.privatebasic.PrivateBasicTest.GameModule.Values;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.inject.BindingAnnotation;
import com.google.inject.Exposed;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

public class PrivateBasicTest extends GWTTestCase {

  @GinModules(GameModule.class)
  public interface GameGinjector extends Ginjector {
    Card getCard();
  }
  
  public void testNonGuiceConstructorInjection() {
    GameGinjector ginjector = GWT.create(GameGinjector.class);

    Card card = ginjector.getCard();
    assertNotNull(card);
    assertEquals("Spade", card.suit);
    assertEquals("2", card.value);
  }

  public void testEagerInjection() {
    GWT.create(Injector.class);
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.inject.InjectTest";
  }
  
  static class GameModule extends AbstractGinModule {

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    public @interface Suits {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    public @interface Values {
    }

    @Override
    protected void configure() {
      install(new DeckModule());
    }

    @Provides @Singleton @Suits
    public List<String> provideSuits() {
      ArrayList<String> suits = new ArrayList<String>();
      suits.add("Spade");
      suits.add("Heart");
      suits.add("Club");
      suits.add("Diamond");
      return suits;
    }

    @Provides @Singleton @Values
    public List<String> provideValues() {
      ArrayList<String> values = new ArrayList<String>();
      for (int i = 2; i <= 10; i++) {
        values.add("" + i);
      }
      values.add("Jack");
      values.add("Queen");
      values.add("King");
      values.add("Ace");
      return values;
    }
  }

  static class DeckModule extends PrivateGinModule {
    @Override
    protected void configure() {
    }

    @Provides @Exposed
    public Card provideCard(List<Card> cards) {
      if (cards.isEmpty()) {
        return null;
      }
      return cards.remove(0);
    }

    @Provides @Singleton
    public List<Card> provideCards(
        @Suits List<String> suits, @Values List<String> values) {
      List<Card> cards = new ArrayList<Card>();
      for (String suit : suits) {
        for (String value : values) {
          cards.add(new Card(value, suit));
        }
      }
      return cards;
    }
  }

  @GinModules({TestGinModule.class})
  public interface Injector extends Ginjector {
  }

  public static class TestGinModule extends PrivateGinModule {

    @Override
    protected void configure() {
      bind(Implementation.class).asEagerSingleton();
      bind(SubInterface.class).to(SubImplementation.class);
    }
  }

  public static class Implementation {
    @Inject Implementation(SubInterface subInterface) {}
  }

  public interface SubInterface {}

  public static class SubImplementation implements SubInterface {}
}
