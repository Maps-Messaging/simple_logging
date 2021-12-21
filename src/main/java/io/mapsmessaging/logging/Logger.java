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
    if (logMessage.getParameterCount() != args.length) {
      localLogger.warn("Invalid number of arguments for the log messages, expected {} received {}",
          logMessage.getParameterCount(),
          args.length);
    }

    ThreadContext.put(CATEGORY, logMessage.getCategory().getDescription());
    switch (logMessage.getLevel()) {
      case TRACE:
        if (isTraceEnabled()) {
          localLogger.trace(logMessage.getMessage(), args);
        }
        break;

      case DEBUG:
        if (isDebugEnabled()) {
          localLogger.debug(logMessage.getMessage(), args);
        }
        break;

      case INFO:
        if (isInfoEnabled()) {
          localLogger.info(logMessage.getMessage(), args);
        }
        break;

      case WARN:
        if (isWarnEnabled()) {
          localLogger.warn(logMessage.getMessage(), args);
        }
        break;

      case ERROR:
        if (isErrorEnabled()) {
          localLogger.error(logMessage.getMessage(), args);
        }
        break;

      case FATAL:
        if (isFatalEnabled()) {
          localLogger.error(fatal, logMessage.getMessage(), args);
        }
        break;

      case AUTH:
        if (isAuthEnabled()) {
          localLogger.error(authentication, logMessage.getMessage(), args);
        }
        break;

      case AUDIT:
        if (isAuditEnabled()) {
          localLogger.error(audit, logMessage.getMessage(), args);
        }
        break;

      default:
    }
    ThreadContext.remove(CATEGORY);
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
