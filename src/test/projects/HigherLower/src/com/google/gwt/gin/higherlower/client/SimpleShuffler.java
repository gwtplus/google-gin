package com.google.gwt.gin.higherlower.client;

import com.google.gwt.gin.higherlower.client.model.Card;
import com.google.gwt.user.client.Random;

import java.util.List;

/**
 * Custom card shuffler.
 */
public class SimpleShuffler implements Shuffler {
  private void swap(List<Card> cards, int firstIndex, int secondIndex) {
    Card o1 = cards.get(firstIndex);
    cards.set(firstIndex, cards.get(secondIndex));
    cards.set(secondIndex, o1);

  }

  public void shuffle(List<Card> cards) {
    for (int i = cards.size(); i > 1; i--) {
      swap(cards, i - 1, Random.nextInt(i));
    }
  }
}
