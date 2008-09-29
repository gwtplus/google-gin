package com.google.gwt.gin.higherlower.client;

/**
 * A game of "Higher or Lower?".
 */
public interface HigherLowerGame {

  PlayerGuessResult displayNextCard(RelationshipToPreviousCard guess);

  boolean isOver();
}
