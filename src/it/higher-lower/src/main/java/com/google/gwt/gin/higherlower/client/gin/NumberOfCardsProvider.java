package com.google.gwt.gin.higherlower.client.gin;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class NumberOfCardsProvider implements Provider<Integer> {
  private final int numberOfCards;

  @Inject
  public NumberOfCardsProvider(@Rows int rows, @Columns int columns) {
    this.numberOfCards = rows*columns;
  }

  public Integer get() {
    return numberOfCards;
  }
}
