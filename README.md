# Simple Logging

This very simple wrapper over Log4J2 is not meant to take away or add any logging features, instead, it simply provides a single class for all the log messages. 
The actual log code then is simply a call to the log message enum that needs to be logged. This becomes handy when looking for specific log messages or if you want to 
change the text of the message etc.

So instead of something like. where you hardwire in the log level and the text 
```java
    logger.debug("Debug Testing Only - {}", "Debug Message");
```

it simply becomes

```java
  logger.log(LogMessages.DEBUG, "Debug Message");
```

The text and level for the log is then configured in one place such as and can add in categories or any other additional context to the log message

```java
public enum LogMessages implements LogMessage {
  DEBUG(LEVEL.DEBUG, CATEGORY.TEST, "Debug Testing Only - {}");
}
```