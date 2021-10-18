package io.mapsmessaging.logging;

import lombok.Getter;

public enum LogMessages implements LogMessage {
  DEBUG(LEVEL.DEBUG, CATEGORY.TEST,"Debug Testing Only - {}"),
  INFO(LEVEL.INFO,  CATEGORY.TEST,"Info Testing Only - {}"),
  WARN(LEVEL.WARN, CATEGORY.TEST, "Warn Testing Only - {}");

  private final @Getter String message;
  private final @Getter LEVEL level;
  private final @Getter Category category;
  private final @Getter int parameterCount;

  LogMessages(LEVEL level, Category category, String message) {
    this.message = message;
    this.level = level;
    this.category = category;
    int location = message.indexOf("{}");
    int count = 0;
    while (location != -1) {
      count++;
      location = message.indexOf("{}", location + 2);
    }
    this.parameterCount = count;
  }

  public enum CATEGORY implements Category {
    TEST("Test");

    public final @Getter String description;

    CATEGORY(String description) {
      this.description = description;
    }
  }

}
