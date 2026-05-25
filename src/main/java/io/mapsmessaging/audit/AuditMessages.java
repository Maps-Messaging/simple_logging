/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.audit;

import io.mapsmessaging.logging.Category;
import io.mapsmessaging.logging.LEVEL;
import io.mapsmessaging.logging.LogMessage;
import lombok.Getter;

public enum AuditMessages implements LogMessage {

  AUDIT_RECORD_WRITTEN(
      LEVEL.AUDIT,
      CATEGORY.AUDIT,
      "Audit record written for correlation id {}"
  ),

  AUDIT_RECORD_REJECTED(
      LEVEL.AUDIT,
      CATEGORY.AUDIT,
      "Audit record rejected for correlation id {} because {}"
  ),

  AUDIT_MANIFEST_CREATED(
      LEVEL.AUDIT,
      CATEGORY.AUDIT,
      "Audit manifest created for sequence range {} to {}"
  ),

  AUDIT_MANIFEST_VERIFIED(
      LEVEL.AUDIT,
      CATEGORY.AUDIT,
      "Audit manifest verified for manifest {}"
  ),

  AUDIT_MANIFEST_VERIFICATION_FAILED(
      LEVEL.AUDIT,
      CATEGORY.AUDIT,
      "Audit manifest verification failed for manifest {} because {}"
  ),

  AUDIT_CHAIN_VERIFIED(
      LEVEL.AUDIT,
      CATEGORY.AUDIT,
      "Audit chain verified from sequence {} to {}"
  ),

  AUDIT_CHAIN_VERIFICATION_FAILED(
      LEVEL.AUDIT,
      CATEGORY.AUDIT,
      "Audit chain verification failed at sequence {} because {}"
  ),

  AUDIT_PAYLOAD_STORED(
      LEVEL.AUDIT,
      CATEGORY.AUDIT,
      "Audit payload {} stored with hash {}"
  ),

  AUDIT_PAYLOAD_VERIFICATION_FAILED(
      LEVEL.AUDIT,
      CATEGORY.AUDIT,
      "Audit payload {} failed verification because {}"
  );

  private final @Getter LEVEL level;
  private final @Getter Category category;
  private final @Getter String message;
  private final @Getter int parameterCount;

  AuditMessages(LEVEL level, Category category, String message) {
    this.level = level;
    this.category = category;
    this.message = message;
    this.parameterCount = countParameters(message);
  }

  private int countParameters(String message) {
    int count = 0;
    int location = message.indexOf("{}");

    while (location != -1) {
      count++;
      location = message.indexOf("{}", location + 2);
    }

    return count;
  }

  public enum CATEGORY implements Category {

    AUDIT("Audit"),
    SECURITY("Security"),
    CONFIGURATION("Configuration"),
    SYSTEM("System");

    private final @Getter String description;

    CATEGORY(String description) {
      this.description = description;
    }

    @Override
    public String getDivision() {
      return "Audit";
    }
  }
}