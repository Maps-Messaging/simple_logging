/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package io.mapsmessaging.logging;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Provides a consistent logging API that hides the logging implementation so it can be changed in the future
 *
 */
public class Logger {

  private static final String DIVISION = "division";
  private static final String CATEGORY = "category";

  private static final Marker fatal = MarkerFactory.getMarker("Fatal");
  private static final Marker authentication = MarkerFactory.getMarker("Auth");
  private static final Marker audit = MarkerFactory.getMarker("Audit");
  private final boolean[] levelEnabled = new boolean[LEVEL.values().length];


  private final org.slf4j.Logger localLogger;

  Logger(String loggerName) {
    localLogger = org.slf4j.LoggerFactory.getLogger(loggerName);
    levelEnabled[LEVEL.TRACE.ordinal()] = localLogger.isTraceEnabled();
    levelEnabled[LEVEL.DEBUG.ordinal()] = localLogger.isDebugEnabled();
    levelEnabled[LEVEL.INFO.ordinal()] = localLogger.isInfoEnabled();
    levelEnabled[LEVEL.WARN.ordinal()] = localLogger.isWarnEnabled();
    levelEnabled[LEVEL.ERROR.ordinal()] = localLogger.isErrorEnabled();
    levelEnabled[LEVEL.FATAL.ordinal()] = levelEnabled[LEVEL.ERROR.ordinal()];
    levelEnabled[LEVEL.AUTH.ordinal()] = levelEnabled[LEVEL.ERROR.ordinal()];
    levelEnabled[LEVEL.AUDIT.ordinal()] = levelEnabled[LEVEL.ERROR.ordinal()];
  }

  /**
   * This function logs the predefined message with the attached args
   *
   * @param logMessage The predefined log message
   * @param args Variable list of arguments that will be added to the log message
   */
  public void log(LogMessage logMessage, Object... args) {
    if (logAt(logMessage)) {
      if (logMessage.getParameterCount() != args.length) {
        localLogger.warn("Invalid number of arguments for the log messages, expected {} received {}", logMessage.getParameterCount(), args.length);
      }

      ThreadContext.put(DIVISION, logMessage.getCategory().getDivision());
      ThreadContext.put(CATEGORY, logMessage.getCategory().getDescription());

      switch (logMessage.getLevel()) {
        case TRACE:
          localLogger.trace(logMessage.getMessage(), args);
          break;

        case DEBUG:
          localLogger.debug(logMessage.getMessage(), args);
          break;

        case INFO:
          localLogger.info(logMessage.getMessage(), args);
          break;

        case WARN:
          localLogger.warn(logMessage.getMessage(), args);
          break;

        case ERROR:
          localLogger.error(logMessage.getMessage(), args);
          break;

        case FATAL:
          localLogger.error(fatal, logMessage.getMessage(), args);
          break;

        case AUTH:
          localLogger.error(authentication, logMessage.getMessage(), args);
          break;

        case AUDIT:
          localLogger.error(audit, logMessage.getMessage(), args);
          break;

        default:
      }
      ThreadContext.remove(CATEGORY);
      ThreadContext.remove(DIVISION);
    }
  }

  /**
   * This function logs the predefined message with the attached args and the exception
   *
   * @param logMessage The predefined log message
   * @param throwable An exception that needs to be logged
   * @param args A list of variable arguments to be logged
   */
  public void log(LogMessage logMessage, Throwable throwable, Object... args) {
    if(logAt(logMessage)) {
      log(logMessage, args);
      ThreadContext.put(DIVISION, logMessage.getCategory().getDivision());
      ThreadContext.put(CATEGORY, logMessage.getCategory().getDescription());
      switch (logMessage.getLevel()) {
        case TRACE:
          localLogger.trace(logMessage.getMessage(), throwable);
          break;

        case DEBUG:
          localLogger.debug(logMessage.getMessage(), throwable);
          break;

        case INFO:
          localLogger.info(logMessage.getMessage(), throwable);
          break;

        case WARN:
          localLogger.warn(logMessage.getMessage(), throwable);
          break;

        case ERROR:
          localLogger.error(logMessage.getMessage(), throwable);
          break;

        case FATAL:
          localLogger.error(fatal, logMessage.getMessage(), throwable);
          break;

        case AUTH:
          localLogger.error(authentication, logMessage.getMessage(), throwable);
          break;

        case AUDIT:
          localLogger.error(audit, logMessage.getMessage(), throwable);
          break;

        default:
      }
      ThreadContext.remove(CATEGORY);
      ThreadContext.remove(DIVISION);
    }
  }

  private boolean logAt(LogMessage logMessage) {
    return levelEnabled[logMessage.getLevel().ordinal()];
  }

  public String getName() {
    return localLogger.getName();
  }

  public boolean isTraceEnabled() {
    return levelEnabled[LEVEL.TRACE.ordinal()];
  }

  public boolean isDebugEnabled() {
    return levelEnabled[LEVEL.DEBUG.ordinal()];
  }

  public boolean isInfoEnabled() {
    return levelEnabled[LEVEL.INFO.ordinal()];
  }

  public boolean isWarnEnabled() {
    return levelEnabled[LEVEL.WARN.ordinal()];
  }

  public boolean isErrorEnabled() {
    return levelEnabled[LEVEL.ERROR.ordinal()];
  }

  public boolean isFatalEnabled() {
    return levelEnabled[LEVEL.FATAL.ordinal()];
  }

  public boolean isAuthEnabled() {
    return levelEnabled[LEVEL.AUTH.ordinal()];
  }

  public boolean isAuditEnabled() {
    return levelEnabled[LEVEL.AUDIT.ordinal()];
  }

}
