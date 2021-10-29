package io.mapsmessaging.logging;

import org.apache.logging.log4j.Level;

/**
 * Simple enum that maps over the log levels provided by log4j2
 */
public enum LEVEL {
  TRACE,
  DEBUG,
  INFO,
  WARN,
  ERROR,
  FATAL,
  AUTH;

  // This creates the "VERBOSE" level if it does not exist yet.
  public static final Level AUTHENTICATION = Level.forName("AUTH", 50);

}