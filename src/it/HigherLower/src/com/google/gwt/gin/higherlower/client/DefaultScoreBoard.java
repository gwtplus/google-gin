package com.google.gwt.gin.higherlower.client;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.gin.higherlower.client.gin.NumberOfCards;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class DefaultScoreBoard extends ScoreBoard {
  private final Label label;
  private final int numberOfPoints;
  private int score;

  @Inject
  public DefaultScoreBoard(@NumberOfCards int numberOfCards) {
    this.numberOfPoints = numberOfCards-1; // first card is not for guessing
    label = new Label();
    label.addStyleName("scoreBoard");
    clear();
    initWidget(label);
  }

  public void setScore(int score) {
    this.label.setText("Score: "+score+"/"+numberOfPoints);
    this.score = score;
  }

  public void incrementScore() {
    setScore(this.score+1);
  }

  public void clear() {
    this.label.setText("");
  }
}
