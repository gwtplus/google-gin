package com.google.gwt.gin.higherlower.client;

import com.google.gwt.event.dom.client.ClickHandler;

/**
 * The game host that drives a HigherLower game.
 */
public interface GameHost extends ClickHandler {
  void playerGuess(RelationshipToPreviousCard guess);
}
