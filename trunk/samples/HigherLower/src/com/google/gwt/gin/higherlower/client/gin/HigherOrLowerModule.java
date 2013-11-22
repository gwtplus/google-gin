package com.google.gwt.gin.higherlower.client.gin;

import com.google.gwt.gin.higherlower.client.GameHost;
import com.google.gwt.gin.higherlower.client.Shuffler;
import com.google.gwt.gin.higherlower.client.SimpleShuffler;
import com.google.gwt.gin.higherlower.client.Walter;
import com.google.gwt.gin.higherlower.client.GameDialogs;
import com.google.gwt.gin.higherlower.client.DefaultGameDialogs;
import com.google.gwt.gin.higherlower.client.DefaultScoreBoard;
import com.google.gwt.gin.higherlower.client.ScoreBoard;
import com.google.gwt.gin.higherlower.client.DefaultHigherLowerGame;
import com.google.gwt.gin.higherlower.client.HigherLowerGame;
import com.google.gwt.gin.higherlower.client.Homepage;
import com.google.gwt.gin.higherlower.client.DefaultHomepage;
import com.google.gwt.gin.higherlower.client.DefaultCardGrid;
import com.google.gwt.gin.higherlower.client.CardGrid;
import com.google.gwt.gin.higherlower.client.model.Deck;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Image;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

public class HigherOrLowerModule extends AbstractGinModule {
  @Override
  protected void configure() {
    bind(Homepage.class).to(DefaultHomepage.class);

    bind(Deck.class).toProvider(DeckProvider.class);
    bind(Shuffler.class).to(SimpleShuffler.class).in(Singleton.class);
    bind(Image.class).annotatedWith(BackOfCard.class).toProvider(BackOfCardProvider.class);
    bind(Button.class).annotatedWith(StartGame.class).toProvider(StartGameButtonProvider.class)
        .in(Singleton.class);

    bind(GameHost.class).to(Walter.class).in(Singleton.class);
    bind(GameDialogs.class).to(DefaultGameDialogs.class).in(Singleton.class);
    bind(HigherLowerGame.class).to(DefaultHigherLowerGame.class);
    bind(CardGrid.class).to(DefaultCardGrid.class).in(Singleton.class);
    bind(ScoreBoard.class).to(DefaultScoreBoard.class).in(Singleton.class);

    bindConstant().annotatedWith(Rows.class).to(2);
    bindConstant().annotatedWith(Columns.class).to(5);
    bind(Integer.class).annotatedWith(NumberOfCards.class)
        .toProvider(NumberOfCardsProvider.class);
  }
}
