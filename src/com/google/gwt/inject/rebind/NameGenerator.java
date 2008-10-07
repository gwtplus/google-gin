package com.google.gwt.inject.rebind;

import com.google.inject.Key;

public interface NameGenerator {

  String sourceNameToBinaryName(ClassType type, String fullyQualifiedClassName);

  String binaryNameToSourceName(String fullyQualifiedClassName);

  String getGetterMethodName(Key<?> key);

  String getCreatorMethodName(Key<?> key);

  String getSingletonFieldName(Key<?> key);
}
