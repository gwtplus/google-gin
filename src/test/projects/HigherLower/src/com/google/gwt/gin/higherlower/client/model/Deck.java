package com.google.gwt.gin.higherlower.client.model;

import com.google.gwt.gin.higherlower.client.Shuffler;

import java.util.LinkedList;

/**
 * Card deck.
 */
public class Deck implements HasCards {
  private final LinkedList<Card> cards;
  private Shuffler shuffler;

  public Deck(LinkedList<Card> cards, Shuffler shuffler) {
    this.cards = cards;
    this.shuffler = shuffler;
  }
  
  public void shuffle() {
    shuffler.shuffle(cards);
  }

  public Card turnCard() {
    return cards.poll();
  }
}
