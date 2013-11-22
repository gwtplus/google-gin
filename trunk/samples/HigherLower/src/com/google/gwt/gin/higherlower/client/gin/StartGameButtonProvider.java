package com.google.gwt.gin.higherlower.client.gin;

import com.google.gwt.user.client.ui.Button;
import com.google.inject.Provider;

public class StartGameButtonProvider implements Provider<Button> {
  public Button get() {
    return new Button("Start Game");
  }
}
