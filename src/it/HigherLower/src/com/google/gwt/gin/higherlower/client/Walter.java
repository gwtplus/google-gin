package com.google.gwt.gin.higherlower.client;

import com.google.gwt.gin.higherlower.client.gin.StartGame;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Meet Walter Capiau, our game host!
 * http://images.google.com/images?q=walter+capiau
 */
public class Walter implements GameHost {

  private final ScoreBoard scoreBoard;
  private final GameDialogs gameDialogs;
  private final Button button;
  private final Provider<DefaultHigherLowerGame> higherLowerGameProvider;

  private HigherLowerGame higherLowerGame;

  @Inject
  public Walter(Provider<DefaultHigherLowerGame> higherLowerGameProvider, ScoreBoard scoreBoard,
      GameDialogs gameDialogs, @StartGame Button button) {
    this.higherLowerGameProvider = higherLowerGameProvider;
    this.scoreBoard = scoreBoard;
    this.gameDialogs = gameDialogs;
    this.button = button;
    newGame();
  }

  public void onClick(Widget sender) {
    startGame();
  }

  private void endGame() {
    gameDialogs.showEndGame(new Runnable() {
      public void run() {
        scoreBoard.clear();
        button.setEnabled(true);
        newGame();
      }
    });
  }

  private void newGame() {
    this.higherLowerGame = higherLowerGameProvider.get();
  }

  private void startGame() {
    button.setEnabled(false);
    scoreBoard.setScore(0);
    this.higherLowerGame.displayNextCard(null /* no guess yet! */);
    gameDialogs.show("Tell me your guess: higher or lower?");
  }

  public void playerGuess(RelationshipToPreviousCard guess) {
    PlayerGuessResult playerGuessResult = this.higherLowerGame.displayNextCard(guess);
    if (PlayerGuessResult.RIGHT.equals(playerGuessResult)) {
      scoreBoard.incrementScore();
    }
    if (this.higherLowerGame.isOver()) {
      endGame();
    } else {
      gameDialogs.show(PlayerGuessResult.RIGHT.equals(playerGuessResult) ? "Congratulations! How about the next card?" :
          "Ouch.. let's have a look at the next card!");
    }
  }
}
