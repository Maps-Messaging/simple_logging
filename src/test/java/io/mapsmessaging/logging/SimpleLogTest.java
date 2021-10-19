package io.mapsmessaging.logging;

import org.junit.jupiter.api.Test;

public class SimpleLogTest {

  @Test
  void simpleTest(){
    Logger logger = LoggerFactory.getLogger(SimpleLogTest.class);
    if(logger.isTraceEnabled())logger.log(LogMessages.TRACE);
    if(logger.isDebugEnabled())logger.log(LogMessages.DEBUG);
    if(logger.isInfoEnabled())logger.log(LogMessages.INFO);
    if(logger.isWarnEnabled())logger.log(LogMessages.WARN);
    if(logger.isErrorEnabled())logger.log(LogMessages.ERROR);

  }
}
