package com.google.gwt.gin.higherlower.client;

/**
 * Utility for showing in-game dialogs.
 */
public interface GameDialogs {

  void show(String title);

  void showEndGame(Runnable runnable);
}
