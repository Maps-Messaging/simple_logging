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

  @Test
  void simpleExceptionTest(){
    Exception ex = new Exception("Just a test");
    ex.fillInStackTrace();
    Logger logger = LoggerFactory.getLogger(SimpleLogTest.class);
    if(logger.isTraceEnabled())logger.log(LogMessages.TRACE,ex, "Trace Message");
    if(logger.isDebugEnabled())logger.log(LogMessages.DEBUG,ex, "Debug Message");
    if(logger.isInfoEnabled())logger.log(LogMessages.INFO, ex,"Info Message");
    if(logger.isWarnEnabled())logger.log(LogMessages.WARN, ex,"Warn Message");
    if(logger.isErrorEnabled())logger.log(LogMessages.ERROR, ex,"Error Message");

  }
}
