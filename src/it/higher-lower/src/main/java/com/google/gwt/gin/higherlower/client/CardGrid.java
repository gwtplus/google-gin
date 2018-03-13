package com.google.gwt.gin.higherlower.client;

import com.google.gwt.gin.higherlower.client.model.Card;
import com.google.gwt.user.client.ui.Composite;

/**
 * The card grid visible on the screen.
 */
public abstract class CardGrid extends Composite {

  public abstract void reset();
  
  public abstract void nextCard(Card card);
}
