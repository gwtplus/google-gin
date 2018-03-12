package com.google.gwt.gin.higherlower.client.model;

/**
 * Playing card rank.
 */
public enum Rank {
  ONE(1), TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7),
  EIGHT(8), NINE(9), TEN(10), JACK(11), QUEEN(12), KING(13);
  
  private final int position;

  private Rank(int position) {
    this.position = position;
  }

  public int getPosition() {
    return position;
  }
}
