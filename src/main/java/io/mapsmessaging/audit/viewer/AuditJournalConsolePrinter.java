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

package io.mapsmessaging.audit.viewer;

import java.util.List;

public class AuditJournalConsolePrinter {

  public void print(List<AuditRecordView> records) {
    printHeader();

    for (AuditRecordView record : records) {
      printRecord(record);
    }

    printSummary(records);
  }

  private void printHeader() {
    System.out.printf(
        "%-8s %-8s %-8s %-28s %-24s %-18s %-18s %-20s %-12s %-40s %-30s%n",
        "Line",
        "Seq",
        "Status",
        "Timestamp",
        "Correlation",
        "Actor",
        "Source",
        "Action",
        "Outcome",
        "Message",
        "Validation"
    );

    System.out.println(
        "------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------"
    );
  }

  private void printRecord(AuditRecordView record) {
    System.out.printf(
        "%-8d %-8d %-8s %-28s %-24s %-18s %-18s %-20s %-12s %-40s %-30s%n",
        record.getLineNumber(),
        record.getSequenceNumber(),
        record.getStatus(),
        truncate(record.getTimestamp(), 28),
        truncate(record.getCorrelationId(), 24),
        truncate(record.getActor(), 18),
        truncate(record.getSource(), 18),
        truncate(record.getAction(), 20),
        truncate(record.getOutcome(), 12),
        truncate(record.getMessageCode(), 40),
        truncate(record.getValidationMessage(), 30)
    );
  }

  private void printSummary(List<AuditRecordView> records) {
    long validCount = records.stream()
        .filter(record -> record.getStatus() == AuditRecordVerificationStatus.VALID)
        .count();

    long invalidCount = records.size() - validCount;

    System.out.println();
    System.out.println("Records: " + records.size());
    System.out.println("Valid:   " + validCount);
    System.out.println("Invalid: " + invalidCount);
  }

  private String truncate(String value, int length) {
    if (value == null) {
      return "";
    }

    if (value.length() <= length) {
      return value;
    }

    if (length <= 3) {
      return value.substring(0, length);
    }

    return value.substring(0, length - 3) + "...";
  }
}