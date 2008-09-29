package com.google.gwt.gin.higherlower.client;

import com.google.gwt.gin.higherlower.client.gin.StartGame;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

/**
 * Application home page.
 */
public class DefaultHomepage extends Homepage {
  @Inject
  public DefaultHomepage(CardGrid cardGrid,
                  @StartGame Button button,
                  GameHost startGame,
                  ScoreBoard scoreBoard) {
    
    button.addClickListener(startGame);

    VerticalPanel gamePanel = new VerticalPanel();
    gamePanel.addStyleName("widePanel");

    Label title = new Label("Welcome to... Higher or Lower!");
    title.addStyleName("title");
    gamePanel.add(title);

    gamePanel.add(button);
    gamePanel.add(cardGrid);
    gamePanel.add(scoreBoard);
    initWidget(gamePanel);
  }
}
