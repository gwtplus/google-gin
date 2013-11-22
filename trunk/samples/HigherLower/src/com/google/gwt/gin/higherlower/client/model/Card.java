package com.google.gwt.gin.higherlower.client.model;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

/**
 * Playing card.
 */
public class Card extends Composite implements Comparable<Card> {
  private final Suit suit;
  private final Rank rank;

  public Card(Suit suit, Rank rank, Widget cardWidget) {
    this.suit = suit;
    this.rank = rank;
    cardWidget.addStyleName("card");
    initWidget(cardWidget);
  }

  public int compareTo(Card card) {
    return Integer.valueOf(getRank().getPosition()).compareTo(card.getRank().getPosition());
  }

  public Rank getRank() {
    return this.rank;
  }

  public Suit getSuit() {
    return this.suit;
  }
}
