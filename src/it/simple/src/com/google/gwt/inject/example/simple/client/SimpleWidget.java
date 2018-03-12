/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.inject.example.simple.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.inject.client.AsyncProvider;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

/**
 * A widget that displays a message or an error and allows the user to show more
 * messages by providing "Show Error" and "Show Message" buttons.
 */
public class SimpleWidget extends Composite {

  private final SimpleMessages messages;
  private final Label text;
  private final SimpleService service;
  private final AsyncProvider<SimpleAsyncWidget> asyncWidgetProvider;
  
  /**
   * Constructs a new simple widget.
   *
   * @param messages a message interface providing message and error templates
   * @param service a service that returns fresh messages and errors
   * @param constants constants to label the buttons
   */
  @Inject
  public SimpleWidget(SimpleMessages messages, SimpleService service, 
      SimpleConstants constants, AsyncProvider<SimpleAsyncWidget> asyncWidgetProvider) {
    this.messages = messages;
    this.service = service;
    this.asyncWidgetProvider = asyncWidgetProvider;
    
    text = new Label();
    text.addStyleName("message");

    Button showMessage = new Button(constants.showMessage(), new ClickHandler() {
      public void onClick(ClickEvent event) {
        showMessage();
      }
    });

    Button showError = new Button(constants.showError(), new ClickHandler() {
      public void onClick(ClickEvent event) {
        showError();
      }
    });

    Button showAsync = new Button(constants.showMessageForAsync(), new ClickHandler() {
      public void onClick(ClickEvent event) {
        showAsync();
      }
    });
    
    HorizontalPanel buttons = new HorizontalPanel();
    buttons.add(showMessage);
    buttons.add(showError);
    buttons.add(showAsync);
    
    VerticalPanel root = new VerticalPanel();
    root.add(text);
    root.add(buttons);
    root.addStyleName("simple");
    
    initWidget(root);
  }

  /**
   * Shows a message on the screen.
   */
  public void showMessage() {
    text.setText(messages.messageTemplate(service.getRandomMessage()));
    text.removeStyleName("error");
  }

  /**
   * Shows an error on the screen.
   */
  public void showError() {
    text.setText(messages.errorTemplate(service.getRandomError()));
    text.addStyleName("error");
  }
  
  public void showAsync() {
    asyncWidgetProvider.get(new AsyncCallback<SimpleAsyncWidget>() {
      @Override
      public void onSuccess(SimpleAsyncWidget widget) {
        widget.showMessage();
      }
      
      @Override
      public void onFailure(Throwable caught) {
        text.setText(messages.errorTemplate(caught.toString()));
      }
    });
  }
}
