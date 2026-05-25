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

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.mapsmessaging.audit.AppendOnlyAuditJournal;
import io.mapsmessaging.audit.AuditContext;
import io.mapsmessaging.audit.AuditKeyUtils;
import io.mapsmessaging.audit.AuditLogger;
import io.mapsmessaging.audit.AuditOutcome;
import io.mapsmessaging.audit.TestAuditMessages;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.interfaces.EdECPublicKey;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuditJournalViewerTest {

  @TempDir
  private Path temporaryDirectory;

  @Test
  void shouldMarkAllRecordsAsValidWhenJournalIsUntouched() throws Exception {
    Path journalPath = createJournalWithThreeRecords();

    AuditJournalViewer auditJournalViewer = new AuditJournalViewer(publicKey);
    List<AuditRecordView> records = auditJournalViewer.readAndVerify(journalPath);

    assertEquals(3, records.size());

    assertEquals(AuditRecordVerificationStatus.VALID, records.get(0).getStatus());
    assertEquals(AuditRecordVerificationStatus.VALID, records.get(1).getStatus());
    assertEquals(AuditRecordVerificationStatus.VALID, records.get(2).getStatus());

    assertEquals("Valid", records.get(0).getValidationMessage());
    assertEquals("Valid", records.get(1).getValidationMessage());
    assertEquals("Valid", records.get(2).getValidationMessage());
  }

  @Test
  void shouldMarkTamperedRecordAndFollowingRecordsAsInvalid() throws Exception {
    Path journalPath = createJournalWithThreeRecords();

    String journalContent = Files.readString(journalPath, StandardCharsets.UTF_8);
    String modifiedJournalContent = journalContent.replace("value-2", "tampered-value");

    Files.writeString(journalPath, modifiedJournalContent, StandardCharsets.UTF_8);

    AuditJournalViewer auditJournalViewer = new AuditJournalViewer(publicKey);
    List<AuditRecordView> records = auditJournalViewer.readAndVerify(journalPath);

    assertEquals(3, records.size());

    assertEquals(AuditRecordVerificationStatus.VALID, records.get(0).getStatus());
    assertEquals(AuditRecordVerificationStatus.INVALID, records.get(1).getStatus());
    assertEquals(AuditRecordVerificationStatus.INVALID, records.get(2).getStatus());

    assertEquals("Valid", records.get(0).getValidationMessage());
    assertEquals("Record hash mismatch", records.get(1).getValidationMessage());
    assertEquals("Previous record in chain was invalid", records.get(2).getValidationMessage());
  }

  @Test
  void shouldMarkDeletedMiddleRecordAsInvalidSequence() throws Exception {
    Path journalPath = createJournalWithThreeRecords();

    List<String> journalLines = Files.readAllLines(journalPath, StandardCharsets.UTF_8);
    journalLines.remove(1);

    Files.write(journalPath, journalLines, StandardCharsets.UTF_8);

    AuditJournalViewer auditJournalViewer = new AuditJournalViewer(publicKey);
    List<AuditRecordView> records = auditJournalViewer.readAndVerify(journalPath);

    assertEquals(2, records.size());

    assertEquals(AuditRecordVerificationStatus.VALID, records.get(0).getStatus());
    assertEquals(AuditRecordVerificationStatus.INVALID, records.get(1).getStatus());

    assertEquals("Valid", records.get(0).getValidationMessage());
    assertEquals("Expected sequence 2 but found 3", records.get(1).getValidationMessage());
  }

  private EdECPublicKey publicKey;

  private Path createJournalWithThreeRecords() throws Exception {
    Path journalRoot = temporaryDirectory.resolve("journal");

    AuditKeyUtils auditKeyUtils = new AuditKeyUtils();
    KeyPair keyPair = auditKeyUtils.generateEd25519KeyPair();
    publicKey = (EdECPublicKey) keyPair.getPublic();

    Path journalPath;

    try (AppendOnlyAuditJournal auditJournal = new AppendOnlyAuditJournal(
        journalRoot,
        keyPair.getPrivate()
    )) {
      AuditLogger auditLogger = new AuditLogger(auditJournal);

      for (int recordIndex = 1; recordIndex <= 3; recordIndex++) {
        AuditContext auditContext = AuditContext.builder()
            .auditId("audit-" + recordIndex)
            .correlationId("correlation-" + recordIndex)
            .actor("unit-test")
            .actorType("test")
            .source("source-system")
            .destination("destination-system")
            .subject("test-subject-" + recordIndex)
            .action("test-action")
            .outcome(AuditOutcome.SUCCESS)
            .timestamp(Instant.parse("2026-05-25T00:00:0" + recordIndex + "Z"))
            .build();

        auditLogger.audit(
            TestAuditMessages.TEST_AUDIT_EVENT,
            auditContext,
            "value-" + recordIndex,
            "record-" + recordIndex
        );
      }

      journalPath = auditJournal.getActiveJournalPath();
    }

    return journalPath;
  }
}