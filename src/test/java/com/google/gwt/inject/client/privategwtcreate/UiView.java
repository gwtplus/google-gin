package com.google.gwt.inject.client.privategwtcreate;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UiView {
  public interface MyBinder extends UiBinder<FlowPanel, UiView> {}
  private FlowPanel panel;

  @Inject
  public UiView(MyBinder binder) {
    panel = binder.createAndBindUi(this);
  }

  public FlowPanel getPanel() {
    return panel;
  }
}
