package io.mapsmessaging.logging;

import org.junit.jupiter.api.Test;

public class SimpleLogTest {

  @Test
  void simpleTest(){
    Logger logger = LoggerFactory.getLogger(SimpleLogTest.class);
    if(logger.isTraceEnabled())logger.log(LogMessages.TRACE, "Trace Message");
    if(logger.isDebugEnabled())logger.log(LogMessages.DEBUG, "Debug Message");
    if(logger.isInfoEnabled())logger.log(LogMessages.INFO, "Info Message");
    if(logger.isWarnEnabled())logger.log(LogMessages.WARN, "Warn Message");
    if(logger.isErrorEnabled())logger.log(LogMessages.ERROR, "Error Message");

  }
}
