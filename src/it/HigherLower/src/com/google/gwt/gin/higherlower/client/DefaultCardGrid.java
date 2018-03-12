package com.google.gwt.gin.higherlower.client;

import com.google.gwt.gin.higherlower.client.gin.BackOfCard;
import com.google.gwt.gin.higherlower.client.gin.Columns;
import com.google.gwt.gin.higherlower.client.gin.Rows;
import com.google.gwt.gin.higherlower.client.model.Card;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Image;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class DefaultCardGrid extends CardGrid {
  private final Provider<Image> backOfCard;
  private final Grid grid;
  private final int rows;
  private final int columns;

  @Inject
  public DefaultCardGrid(@BackOfCard Provider<Image> backOfCard,
      @Rows int rows, @Columns int columns) {
    // we use a Provider to avoid weird results with duplicate images in Grid
    this.backOfCard = backOfCard;
    this.grid = new Grid(rows, columns);

    this.rows = rows;
    this.columns = columns;
    
    grid.addStyleName("cardGrid");

    reset();

    initWidget(grid);
  }

  public void reset() {
    for (int row = 0; row < this.rows; row++) {
      for (int column = 0; column < this.columns; column++) {
        grid.setWidget(row, column, new Card(null,null,this.backOfCard.get()));
      }
    }
  }

  public void nextCard(Card card) {
    // TODO perf will suck for large grids
    for (int row = 0; row < this.rows; row++) {
      for (int column = 0; column < this.columns; column++) {
        Card currentCard = (Card)grid.getWidget(row, column);
        if (currentCard.getSuit() == null) {
          grid.setWidget(row, column, card);
          return;
        }
      }
    }
  }
}
