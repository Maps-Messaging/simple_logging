package io.mapsmessaging.logging;

/**
 * All Log message instances must implement this interface
 */
public interface LogMessage {

  String getMessage();
  LEVEL getLevel();
  Category getCategory();
  int getParameterCount();

}
