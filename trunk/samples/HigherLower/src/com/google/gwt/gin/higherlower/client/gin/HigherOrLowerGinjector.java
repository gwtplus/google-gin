package com.google.gwt.gin.higherlower.client.gin;

import com.google.gwt.gin.higherlower.client.Homepage;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

/**
 * The "Higher or Lower?" game's injector.
 */
@GinModules(HigherOrLowerModule.class)
public interface HigherOrLowerGinjector extends Ginjector {
  Homepage getHomepage();
}
