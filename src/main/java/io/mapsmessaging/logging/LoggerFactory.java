package io.mapsmessaging.logging;

/**
 * This class encapsulates the Logger Factory implementation so it can be replaced in the future
 */
public class LoggerFactory {

  private LoggerFactory() { }

  public static Logger getLogger(Class<?> clazz) {
    return getLogger(clazz.getName());
  }

  public static Logger getLogger(String loggerName) {
    return new Logger(loggerName);
  }
}
