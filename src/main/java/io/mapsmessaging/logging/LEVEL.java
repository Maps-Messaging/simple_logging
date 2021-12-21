package io.mapsmessaging.logging;

import org.slf4j.event.Level;

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
  AUTH,
  AUDIT;
}