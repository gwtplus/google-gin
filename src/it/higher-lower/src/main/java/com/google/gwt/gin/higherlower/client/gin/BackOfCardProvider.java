package com.google.gwt.gin.higherlower.client.gin;

import com.google.gwt.gin.higherlower.client.bundle.DeckOfCardsClientBundle;
import com.google.gwt.user.client.ui.Image;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Gives out the back of a playing card.
 */
public class BackOfCardProvider implements Provider<Image> {
  private final DeckOfCardsClientBundle clientBundle;

  @Inject
  public BackOfCardProvider(DeckOfCardsClientBundle clientBundle) {
    this.clientBundle = clientBundle;
  }

  public Image get() {
    return new Image(clientBundle.back());
  }
}
