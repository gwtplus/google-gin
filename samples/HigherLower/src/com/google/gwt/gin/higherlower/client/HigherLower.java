package com.google.gwt.gin.higherlower.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.gin.higherlower.client.gin.HigherOrLowerGinjector;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * HigherLower application.
 */
public class HigherLower implements EntryPoint {
  public void onModuleLoad() {
    // Create a Ginjector
    HigherOrLowerGinjector ginjector = GWT.create(HigherOrLowerGinjector.class);
    // Add the application homepage to the RootPanel
    RootPanel.get().add(ginjector.getHomepage());
  }
}
