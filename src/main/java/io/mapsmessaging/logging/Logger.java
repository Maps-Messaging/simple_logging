package io.mapsmessaging.logging;
import org.apache.logging.log4j.ThreadContext;

/**
 * Provides a consistent logging API that hides the logging implementation so it can be changed in the future
 *
 */
public class Logger {

  private static final String CATEGORY = "category";

  private final org.apache.logging.log4j.Logger localLogger;

  Logger(String loggerName) {
    localLogger = org.apache.logging.log4j.LogManager.getLogger(loggerName);
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
          localLogger
              .atTrace()
              .withThrowable(throwable)
              .withLocation()
              .log(logMessage.getMessage(), args);
        }
        break;

      case DEBUG:
        if (isDebugEnabled()) {
          localLogger
              .atDebug()
              .withThrowable(throwable)
              .withLocation()
              .log(logMessage.getMessage(), args);
        }
        break;

      case INFO:
        if (isInfoEnabled()) {
          localLogger
              .atInfo()
              .withThrowable(throwable)
              .withLocation()
              .log(logMessage.getMessage(), args);
        }
        break;

      case WARN:
        if (isWarnEnabled()) {
          localLogger
              .atDebug()
              .withThrowable(throwable)
              .withLocation()
              .log(logMessage.getMessage(), args);
        }
        break;

      case ERROR:
        if (isErrorEnabled()) {
          localLogger
              .atError()
              .withThrowable(throwable)
              .withLocation()
              .log(logMessage.getMessage(), args);
        }
        break;

      default:
    }
    ThreadContext.remove(CATEGORY);
  }

  private boolean logAt(LogMessage logMessage) {
    switch (logMessage.getLevel()) {
      case TRACE:
        return localLogger.isTraceEnabled();

      case DEBUG:
        return localLogger.isDebugEnabled();

      case INFO:
        return localLogger.isInfoEnabled();

      case WARN:
        return localLogger.isWarnEnabled();

      case ERROR:
        return localLogger.isErrorEnabled();

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
}
