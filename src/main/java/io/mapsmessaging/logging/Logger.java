/*
 *
 *  Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *  Copyright [ 2024 - 2025  ] [Maps Messaging B.V.]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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


  private final org.slf4j.Logger localLogger;

  Logger(String loggerName) {
    localLogger = org.slf4j.LoggerFactory.getLogger(loggerName);
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
    switch (logMessage.getLevel()) {
      case TRACE:
        return isTraceEnabled();

      case DEBUG:
        return isDebugEnabled();

      case INFO:
        return isInfoEnabled();

      case WARN:
        return isWarnEnabled();

      case ERROR:
        return isErrorEnabled();

      case FATAL:
        return isFatalEnabled();

      case AUTH:
        return isAuthEnabled();

      case AUDIT:
        return isAuditEnabled();

      default:
        return false;
    }
  }

  public String getName() {
    return localLogger.getName();
  }

  public boolean isTraceEnabled() {
    return localLogger.isTraceEnabled();
  }

  public boolean isDebugEnabled() {
    return localLogger.isDebugEnabled();
  }

  public boolean isInfoEnabled() {
    return localLogger.isInfoEnabled();
  }

  public boolean isWarnEnabled() {
    return localLogger.isWarnEnabled();
  }

  public boolean isErrorEnabled() {
    return localLogger.isErrorEnabled();
  }

  public boolean isFatalEnabled() {
    return isErrorEnabled();
  }

  public boolean isAuthEnabled() {
    return  isErrorEnabled();
  }

  public boolean isAuditEnabled() {
    return  isErrorEnabled();
  }

}
