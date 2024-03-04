package io.mapsmessaging.logging;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.classic.spi.ILoggingEvent;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class InMemoryAppender extends AppenderBase<ILoggingEvent> {
  static final List<ILoggingEvent> logEvents = new ArrayList<>();

  @Override
  protected void append(ILoggingEvent eventObject) {
    logEvents.add(eventObject);
  }

  public static void clearLogEvents() {
    logEvents.clear();
  }
}
