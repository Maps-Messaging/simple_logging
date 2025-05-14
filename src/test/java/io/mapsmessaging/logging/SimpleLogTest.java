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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

class SimpleLogTest {

  @BeforeEach
  public void setUp() {
    InMemoryAppender.clearLogEvents();
  }

  @AfterEach
  public void tearDown() {
    InMemoryAppender.clearLogEvents();
  }

  @Test
  void simpleTest(){
    Logger logger = LoggerFactory.getLogger(SimpleLogTest.class);
    Assertions.assertEquals(SimpleLogTest.class.getName(), logger.getName());
    if(logger.isTraceEnabled()){
      logger.log(LogMessages.TRACE, "Trace Message");
      Assertions.assertEquals("Trace Testing Only - "+"Trace Message", InMemoryAppender.logEvents.get(0).getFormattedMessage());
      InMemoryAppender.clearLogEvents();
    }
    if(logger.isDebugEnabled()){
      logger.log(LogMessages.DEBUG, "Debug Message");
      Assertions.assertEquals("Debug Testing Only - "+"Debug Message", InMemoryAppender.logEvents.get(0).getFormattedMessage());
      InMemoryAppender.clearLogEvents();
    }
    if(logger.isInfoEnabled()){
      logger.log(LogMessages.INFO, "Info Message");
      Assertions.assertEquals("Info Testing Only - "+"Info Message", InMemoryAppender.logEvents.get(0).getFormattedMessage());
      InMemoryAppender.clearLogEvents();
    }
    if(logger.isWarnEnabled()){
      logger.log(LogMessages.WARN, "Warn Message");
      Assertions.assertEquals("Warn Testing Only - "+"Warn Message", InMemoryAppender.logEvents.get(0).getFormattedMessage());
      InMemoryAppender.clearLogEvents();
    }
    if(logger.isErrorEnabled()) {
      logger.log(LogMessages.ERROR, "Error Message");
      Assertions.assertEquals("Error Testing Only - " + "Error Message", InMemoryAppender.logEvents.get(0).getFormattedMessage());
      InMemoryAppender.clearLogEvents();
    }
    if(logger.isFatalEnabled()){
      logger.log(LogMessages.FATAL, "Fatal Message");
      Assertions.assertEquals("Fatal Testing Only - "+"Fatal Message", InMemoryAppender.logEvents.get(0).getFormattedMessage());
      InMemoryAppender.clearLogEvents();
    }
    if(logger.isAuthEnabled()){
      logger.log(LogMessages.AUTH, "Auth Message");
      Assertions.assertEquals("Authentication Testing Only - "+"Auth Message", InMemoryAppender.logEvents.get(0).getFormattedMessage());
      InMemoryAppender.clearLogEvents();
    }
    if(logger.isAuditEnabled()){
      logger.log(LogMessages.AUDIT, "Audit Message");
      Assertions.assertEquals("Audit Testing Only - "+"Audit Message", InMemoryAppender.logEvents.get(0).getFormattedMessage());
      InMemoryAppender.clearLogEvents();
    }
  }

  @Test
  void simpleExcessArgTest(){
    Exception ex = new Exception("Just a test");
    ex.fillInStackTrace();

    Logger logger = LoggerFactory.getLogger(SimpleLogTest.class);
    Assertions.assertEquals(1, LogMessages.TRACE.getParameterCount());
    if(logger.isTraceEnabled()){
      logger.log(LogMessages.TRACE, "Trace Message", "extra");
      Assertions.assertEquals("Invalid number of arguments for the log messages, expected 1 received 2", InMemoryAppender.logEvents.get(0).getFormattedMessage());
      Assertions.assertEquals("Trace Testing Only - "+"Trace Message", InMemoryAppender.logEvents.get(1).getFormattedMessage());
      InMemoryAppender.clearLogEvents();
    }
    if(logger.isDebugEnabled()){
      logger.log(LogMessages.DEBUG,  ex, "Debug Message", "extra");
      Assertions.assertEquals("Invalid number of arguments for the log messages, expected 1 received 2", InMemoryAppender.logEvents.get(0).getFormattedMessage());
      Assertions.assertEquals("Debug Testing Only - "+"Debug Message", InMemoryAppender.logEvents.get(1).getFormattedMessage());
      InMemoryAppender.clearLogEvents();
    }
  }

  @Test
  void simpleThreadContextTest(){
    ThreadContext.put("test", "value");
    ThreadContext.put("test1", "value1");
    Map<String, String> map = ThreadContext.getContext();
    Assertions.assertNotNull(map);
    Assertions.assertEquals("value", map.get("test"));
    Assertions.assertEquals("value1", map.get("test1"));
    ThreadContext.clearAll();
    Assertions.assertNull(ThreadContext.getContext());

    ThreadContext.put("test", "value");
    ThreadContext.put("test1", "value1");
    map = ThreadContext.getContext();
    Assertions.assertNotNull(map);
    Assertions.assertEquals("value", map.get("test"));
    Assertions.assertEquals("value1", map.get("test1"));
    ThreadContext.clear();
    Assertions.assertNull(ThreadContext.getContext());

    ThreadContext.put("test", "value");
    ThreadContext.put("test1", "value1");
    map = ThreadContext.getContext();
    Assertions.assertNotNull(map);
    Assertions.assertEquals("value", map.get("test"));
    Assertions.assertEquals("value1", map.get("test1"));
    ThreadContext.clearMap();
    Assertions.assertNull(ThreadContext.getContext());

    ThreadContext.putAll(map);
    map = ThreadContext.getContext();
    Assertions.assertNotNull(map);
    Assertions.assertEquals("value", map.get("test"));
    Assertions.assertEquals("value1", map.get("test1"));

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
    if(logger.isFatalEnabled())logger.log(LogMessages.FATAL, ex,"Fatal Message");
    if(logger.isAuthEnabled())logger.log(LogMessages.AUTH, ex,"Auth Message");
    if(logger.isAuditEnabled())logger.log(LogMessages.AUDIT, ex,"Audit Message");
  }
}
