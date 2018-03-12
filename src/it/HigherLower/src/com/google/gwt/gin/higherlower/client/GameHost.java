package com.google.gwt.gin.higherlower.client;

import com.google.gwt.user.client.ui.ClickListener;

/**
 * The game host that drives a HigherLower game.
 */
public interface GameHost extends ClickListener {
  void playerGuess(RelationshipToPreviousCard guess);
}
