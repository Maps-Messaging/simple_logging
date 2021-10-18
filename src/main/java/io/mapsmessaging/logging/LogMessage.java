package io.mapsmessaging.logging;

public interface LogMessage {

  String getMessage();
  LEVEL getLevel();
  Category getCategory();
  int getParameterCount();

}
