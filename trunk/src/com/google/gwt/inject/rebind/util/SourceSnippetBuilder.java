/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.inject.rebind.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder that composes source snippets from other source snippets.
 */
public class SourceSnippetBuilder {

  List<SourceSnippet> snippets = new ArrayList<SourceSnippet>();

  /**
   * Convenience routine to append a constant string to this snippet.
   */
  public SourceSnippetBuilder append(String snippet) {
    snippets.add(SourceSnippets.forText(snippet));
    return this;
  }

  public SourceSnippetBuilder append(SourceSnippet snippet) {
    snippets.add(snippet);
    return this;
  }

  public SourceSnippet build() {
    final List<SourceSnippet> snippetsCopy = new ArrayList<SourceSnippet>(snippets);
    return new SourceSnippet() {
      public String getSource(InjectorWriteContext writeContext) {
        StringBuilder resultBuilder = new StringBuilder();
        for (SourceSnippet snippet : snippetsCopy) {
          resultBuilder.append(snippet.getSource(writeContext));
        }

        return resultBuilder.toString();
      }
    };
  }
}
