// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.gwt.inject.client.constructor;

public class Giraffe {

  public static final int DEFAULT_NECK_LENGTH = 300;

  private final int neckLength;

  public Giraffe(boolean moreNeck) {
    neckLength = DEFAULT_NECK_LENGTH + 42;
  }

  public Giraffe() {
    neckLength = DEFAULT_NECK_LENGTH;
  }

  public int getNeckLength() {
    return neckLength;
  }
}
