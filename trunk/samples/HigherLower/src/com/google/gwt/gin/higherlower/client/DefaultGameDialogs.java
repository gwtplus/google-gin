package com.google.gwt.gin.higherlower.client;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class DefaultGameDialogs implements GameDialogs {
  // we use a provider to break up a circular dependency,
  // GIN does not support them (yet)!
  private final Provider<GameHost> gameHost;

  @Inject
  public DefaultGameDialogs(Provider<GameHost> gameHost) {
    this.gameHost = gameHost;
  }

  public void show(String title) {
    final DialogBox box = new DialogBox();

    box.setAnimationEnabled(true);
    box.setText(title);
    box.setWidth("200px"); // It's an higherlower, folks!

    VerticalPanel verticalPanel = new VerticalPanel();

    Button higher = new Button("Higher, higher!");
    higher.addStyleName("centered");
    higher.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        box.hide();
        gameHost.get().playerGuess(RelationshipToPreviousCard.HIGHER);
      }
    });
    verticalPanel.add(higher);

    Button lower = new Button("Down, boy!");
    lower.addStyleName("centered");
    lower.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        box.hide();
        gameHost.get().playerGuess(RelationshipToPreviousCard.LOWER);
      }
    });
    verticalPanel.add(lower);

    HorizontalPanel hp = new HorizontalPanel();
    hp.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
    hp.add(verticalPanel);
    box.setWidget(hp);
    box.center();
    box.show();
  }

  public void showEndGame(final Runnable runnable) {
    final DialogBox box = new DialogBox();
    box.setAnimationEnabled(true);
    box.setText("Thanks for playing Higher or Lower! *ding*ding*ding*ding*");
    Button b = new Button("Thanks for having me!");
    b.addStyleName("centered");
    b.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        runnable.run();
        box.hide();
      }
    });
    box.setWidget(b);
    box.center();
    box.show();
  }
}
