package com.google.gwt.gin.higherlower.client;

import com.google.gwt.gin.higherlower.client.model.Card;

import java.util.List;

/**
 * Shuffles cards.
 */
public interface Shuffler {
  void shuffle(List<Card> cards);
}
