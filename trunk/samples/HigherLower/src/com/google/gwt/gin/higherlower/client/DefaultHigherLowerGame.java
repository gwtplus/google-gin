package com.google.gwt.gin.higherlower.client;

import com.google.gwt.gin.higherlower.client.model.Card;
import com.google.gwt.gin.higherlower.client.model.Deck;
import com.google.gwt.gin.higherlower.client.gin.NumberOfCards;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class DefaultHigherLowerGame implements HigherLowerGame {
  private final Deck deck;
  private final CardGrid grid;
  private final int numberOfCards;

  private int cardsTurnedPlusOne = 1;
  private Card previous = null;

  @Inject
  public DefaultHigherLowerGame(Deck deck, CardGrid grid, @NumberOfCards int numberOfCards) {
    this.deck = deck;
    this.grid = grid;
    this.numberOfCards = numberOfCards;
    setTheStage();
  }

  /**
   * Turn the next card.
   * @param guess the player's guess
   * @return whether the player was right or wrong
   */
  public PlayerGuessResult displayNextCard(RelationshipToPreviousCard guess) {
    Card card = deck.turnCard();
    grid.nextCard(card);
    cardsTurnedPlusOne++;

    RelationshipToPreviousCard actualRelationshipToPrevious = getRelationshipToPreviousCard(card);
    previous = card;
    if (actualRelationshipToPrevious == null) {
      return null;
    }

    return actualRelationshipToPrevious.equals(guess) ? PlayerGuessResult.RIGHT : PlayerGuessResult.WRONG;
  }

  /**
   * @return if the last card has been shown and the game is over
   */
  public boolean isOver() {
    return cardsTurnedPlusOne > numberOfCards;
  }

  /**
   * @param card the current card
   * @return the relationship to the previous card, or @{code null} if the given card is the first one
   */
  private RelationshipToPreviousCard getRelationshipToPreviousCard(Card card) {
    RelationshipToPreviousCard compare = null;
    if (previous != null) {
      if (card.compareTo(previous) < 0) {
        compare = RelationshipToPreviousCard.LOWER;
      } else if (card.compareTo(previous) > 0) {
        compare = RelationshipToPreviousCard.HIGHER;
      } else {
        compare = RelationshipToPreviousCard.EQUAL;
      }
    }
    return compare;
  }

  private void setTheStage() {
    this.deck.shuffle();
    this.grid.reset();
  }
}
