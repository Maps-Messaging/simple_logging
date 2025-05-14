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

import lombok.Getter;

/**
 * This is a simple implementation, to use the logging framework, simply take this
 * class, rename it and then add your message enums ( Remove the TRACE,DEBUG ones, since they are simply for testing)
 *
 * The enum is simply, the log level for the message, an arbitrary category that makes sense to your application and then the log test itself
 */
public enum LogMessages implements LogMessage {

  TRACE(LEVEL.TRACE, CATEGORY.TEST, "Trace Testing Only - {}"),
  DEBUG(LEVEL.DEBUG, CATEGORY.TEST,"Debug Testing Only - {}"),
  INFO(LEVEL.INFO,  CATEGORY.TEST,"Info Testing Only - {}"),
  WARN(LEVEL.WARN, CATEGORY.TEST, "Warn Testing Only - {}"),
  ERROR(LEVEL.ERROR, CATEGORY.TEST, "Error Testing Only - {}"),
  FATAL(LEVEL.FATAL, CATEGORY.TEST, "Fatal Testing Only - {}"),
  AUTH(LEVEL.AUTH, CATEGORY.TEST, "Authentication Testing Only - {}"),
  AUDIT(LEVEL.AUDIT, CATEGORY.TEST, "Audit Testing Only - {}"),

  //<editor-fold desc="System and Environment property access">
  CONFIG_PROPERTY_ACCESS(LEVEL.ERROR, CATEGORY.CONFIGURATION, "Getting property {} from system resulted in {}"),
  PROPERTY_MANAGER_START(LEVEL.DEBUG, CATEGORY.CONFIGURATION, "Starting Property Manager"),
  PROPERTY_MANAGER_FOUND(LEVEL.DEBUG, CATEGORY.CONFIGURATION, "Found and loaded property {}"),
  PROPERTY_MANAGER_LOOKUP(LEVEL.DEBUG, CATEGORY.CONFIGURATION, "Looking failed for {} config"),
  PROPERTY_MANAGER_LOOKUP_FAILED(LEVEL.DEBUG, CATEGORY.CONFIGURATION, "Looking for {} config, found in {}"),
  PROPERTY_MANAGER_SCANNING(LEVEL.DEBUG, CATEGORY.CONFIGURATION, "Scanning property with {} entries"),
  PROPERTY_MANAGER_INDEX_DETECTED(LEVEL.DEBUG, CATEGORY.CONFIGURATION, "Detected an indexed property file, parsing into different properties"),
  PROPERTY_MANAGER_COMPLETED_INDEX(LEVEL.DEBUG, CATEGORY.CONFIGURATION, "Completed indexed property with {} for index {}"),
  PROPERTY_MANAGER_SCAN_FAILED(LEVEL.WARN, CATEGORY.CONFIGURATION, "Failed to scan for property files"),
  PROPERTY_MANAGER_LOAD_FAILED(LEVEL.WARN, CATEGORY.CONFIGURATION, "Failed to load property {}"),
  PROPERTY_MANAGER_ENTRY_LOOKUP(LEVEL.DEBUG, CATEGORY.CONFIGURATION, "Lookup for {} found {} in {}"),
  PROPERTY_MANAGER_ENTRY_LOOKUP_FAILED(LEVEL.DEBUG, CATEGORY.CONFIGURATION, "Lookup for {} not found, returning default {}"),

  ;

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
    CONFIGURATION("Config"),
    TEST("Test");

    public final @Getter String description;

    public String getDivision(){
      return "Test";
    }

    CATEGORY(String description) {
      this.description = description;
    }
  }

}
