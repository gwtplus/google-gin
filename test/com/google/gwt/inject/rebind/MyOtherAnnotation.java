// Copyright 2008 Google Inc. All Rights Reserved.

package com.google.gwt.inject.rebind;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Simple custom non-binding annotation for tests.
 * 
 * @author bstoler@google.com (Brian Stoler)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE })
@interface MyOtherAnnotation {}
