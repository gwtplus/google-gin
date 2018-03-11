package com.google.gwt.gin.higherlower.client.gin;

import com.google.gwt.gin.higherlower.client.DeckOfCardsImageBundle;
import com.google.gwt.user.client.ui.Image;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Gives out the back of a playing card.
 */
public class BackOfCardProvider implements Provider<Image> {
  private final DeckOfCardsImageBundle imageBundle;

  @Inject
  public BackOfCardProvider(DeckOfCardsImageBundle imageBundle) {
    this.imageBundle = imageBundle;
  }

  public Image get() {
    return imageBundle.back().createImage();
  }
}
