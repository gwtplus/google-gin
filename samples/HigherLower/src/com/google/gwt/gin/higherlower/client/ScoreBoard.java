package com.google.gwt.gin.higherlower.client;

import com.google.gwt.user.client.ui.Composite;

/**
 * Displays the current game's score.
 */
public abstract class ScoreBoard extends Composite {

  public abstract void setScore(int score);

  public abstract void incrementScore();

  public abstract void clear();
}
