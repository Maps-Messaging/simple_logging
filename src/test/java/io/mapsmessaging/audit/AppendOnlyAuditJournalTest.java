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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class AppendOnlyAuditJournalTest {

  @TempDir
  private Path temporaryDirectory;

  @Test
  void shouldAppendAuditRecordAndVerifyJournal() throws Exception {
    Path journalRoot = temporaryDirectory.resolve("journal");
    Path payloadRoot = temporaryDirectory.resolve("payloads");

    AuditKeyUtils auditKeyUtils = new AuditKeyUtils();
    KeyPair keyPair = auditKeyUtils.generateEd25519KeyPair();

    AuditPayloadStore payloadStore = new AuditPayloadStore(payloadRoot);

    AuditPayloadReference payloadReference = payloadStore.writePayload(
        "correlation-1",
        "input-message",
        "input.json",
        "{\"message\":\"test\"}".getBytes(StandardCharsets.UTF_8)
    );

    try (AppendOnlyAuditJournal auditJournal = new AppendOnlyAuditJournal(
        journalRoot,
        keyPair.getPrivate()
    )) {
      AuditLogger auditLogger = new AuditLogger(auditJournal);

      AuditContext auditContext = AuditContext.builder()
          .auditId("audit-1")
          .correlationId("correlation-1")
          .actor("unit-test")
          .actorType("test")
          .source("source-system")
          .destination("destination-system")
          .subject("test-subject")
          .action("test-action")
          .outcome(AuditOutcome.SUCCESS)
          .timestamp(Instant.parse("2026-05-25T00:00:00Z"))
          .build();

      AuditRecord auditRecord = auditLogger.audit(
          TestAuditMessages.TEST_AUDIT_EVENT,
          auditContext,
          List.of(payloadReference),
          "value-1",
          "value-2"
      );

      assertEquals(1, auditRecord.getSequenceNumber());
      assertEquals("audit-1", auditRecord.getAuditId());
      assertEquals("correlation-1", auditRecord.getCorrelationId());
      assertEquals("TEST_AUDIT_EVENT", auditRecord.getMessageCode().substring(
          auditRecord.getMessageCode().lastIndexOf('.') + 1
      ));
      assertEquals("AUDIT", auditRecord.getLevel());
      assertEquals("Audit", auditRecord.getCategoryDivision());
      assertEquals("Test Audit", auditRecord.getCategoryDescription());
      assertEquals(AuditOutcome.SUCCESS, auditRecord.getOutcome());
      assertEquals(2, auditRecord.getParameters().size());
      assertEquals(1, auditRecord.getPayloadReferences().size());
      assertNotNull(auditRecord.getPreviousRecordHash());
      assertNotNull(auditRecord.getRecordHash());
      assertNotNull(auditRecord.getSignature());

      AuditVerifier auditVerifier = new AuditVerifier(
          (java.security.interfaces.EdECPublicKey) keyPair.getPublic()
      );

      AuditVerifier.VerificationResult verificationResult = auditVerifier.verifyJournal(
          auditJournal.getActiveJournalPath()
      );

      assertTrue(verificationResult.valid());
      assertEquals(1, verificationResult.verifiedRecords());
      assertEquals(auditRecord.getRecordHash(), verificationResult.lastHash());
    }
  }

  @Test
  void shouldAppendMultipleRecordsAndMaintainHashChain() throws Exception {
    Path journalRoot = temporaryDirectory.resolve("journal");

    AuditKeyUtils auditKeyUtils = new AuditKeyUtils();
    KeyPair keyPair = auditKeyUtils.generateEd25519KeyPair();

    try (AppendOnlyAuditJournal auditJournal = new AppendOnlyAuditJournal(
        journalRoot,
        keyPair.getPrivate()
    )) {
      AuditLogger auditLogger = new AuditLogger(auditJournal);

      AuditContext firstAuditContext = AuditContext.builder()
          .auditId("audit-1")
          .correlationId("correlation-1")
          .actor("unit-test")
          .actorType("test")
          .source("source-system")
          .destination("destination-system")
          .subject("first-subject")
          .action("first-action")
          .outcome(AuditOutcome.SUCCESS)
          .timestamp(Instant.parse("2026-05-25T00:00:00Z"))
          .build();

      AuditContext secondAuditContext = AuditContext.builder()
          .auditId("audit-2")
          .correlationId("correlation-2")
          .actor("unit-test")
          .actorType("test")
          .source("source-system")
          .destination("destination-system")
          .subject("second-subject")
          .action("second-action")
          .outcome(AuditOutcome.SUCCESS)
          .timestamp(Instant.parse("2026-05-25T00:00:01Z"))
          .build();

      AuditRecord firstAuditRecord = auditLogger.audit(
          TestAuditMessages.TEST_AUDIT_EVENT,
          firstAuditContext,
          "first",
          "record"
      );

      AuditRecord secondAuditRecord = auditLogger.audit(
          TestAuditMessages.TEST_AUDIT_EVENT,
          secondAuditContext,
          "second",
          "record"
      );

      assertEquals(1, firstAuditRecord.getSequenceNumber());
      assertEquals(2, secondAuditRecord.getSequenceNumber());
      assertEquals(firstAuditRecord.getRecordHash(), secondAuditRecord.getPreviousRecordHash());
      assertEquals(secondAuditRecord.getRecordHash(), auditJournal.getCurrentRecordHash());
      assertEquals(2, auditJournal.getCurrentSequenceNumber());

      AuditVerifier auditVerifier = new AuditVerifier(
          (java.security.interfaces.EdECPublicKey) keyPair.getPublic()
      );

      AuditVerifier.VerificationResult verificationResult = auditVerifier.verifyJournal(
          auditJournal.getActiveJournalPath()
      );

      assertTrue(verificationResult.valid());
      assertEquals(2, verificationResult.verifiedRecords());
      assertEquals(secondAuditRecord.getRecordHash(), verificationResult.lastHash());
    }
  }

  @Test
  void shouldDetectModifiedMiddleJournalRecordInHashChain() throws Exception {
    Path journalRoot = temporaryDirectory.resolve("journal");

    AuditKeyUtils auditKeyUtils = new AuditKeyUtils();
    KeyPair keyPair = auditKeyUtils.generateEd25519KeyPair();

    Path journalPath;

    try (AppendOnlyAuditJournal auditJournal = new AppendOnlyAuditJournal(
        journalRoot,
        keyPair.getPrivate()
    )) {
      AuditLogger auditLogger = new AuditLogger(auditJournal);

      for (int eventIndex = 1; eventIndex <= 3; eventIndex++) {
        AuditContext auditContext = AuditContext.builder()
            .auditId("audit-" + eventIndex)
            .correlationId("correlation-" + eventIndex)
            .actor("unit-test")
            .actorType("test")
            .source("source-system")
            .destination("destination-system")
            .subject("test-subject-" + eventIndex)
            .action("test-action")
            .outcome(AuditOutcome.SUCCESS)
            .timestamp(Instant.parse("2026-05-25T00:00:0" + eventIndex + "Z"))
            .build();

        auditLogger.audit(
            TestAuditMessages.TEST_AUDIT_EVENT,
            auditContext,
            "value-" + eventIndex,
            "record-" + eventIndex
        );
      }

      journalPath = auditJournal.getActiveJournalPath();
    }

    String journalContent = Files.readString(journalPath, StandardCharsets.UTF_8);
    String modifiedJournalContent = journalContent.replace("value-2", "tampered-value");

    Files.writeString(journalPath, modifiedJournalContent, StandardCharsets.UTF_8);

    AuditVerifier auditVerifier = new AuditVerifier(
        (java.security.interfaces.EdECPublicKey) keyPair.getPublic()
    );

    AuditVerifier.VerificationResult verificationResult = auditVerifier.verifyJournal(journalPath);

    assertFalse(verificationResult.valid());
    assertEquals(
        "Record hash mismatch at sequence 2",
        verificationResult.error()
    );
  }

  @Test
  void shouldRejectIncorrectParameterCount() throws Exception {
    Path journalRoot = temporaryDirectory.resolve("journal");

    AuditKeyUtils auditKeyUtils = new AuditKeyUtils();
    KeyPair keyPair = auditKeyUtils.generateEd25519KeyPair();

    try (AppendOnlyAuditJournal auditJournal = new AppendOnlyAuditJournal(
        journalRoot,
        keyPair.getPrivate()
    )) {
      AuditLogger auditLogger = new AuditLogger(auditJournal);

      AuditContext auditContext = AuditContext.builder()
          .auditId("audit-1")
          .correlationId("correlation-1")
          .actor("unit-test")
          .actorType("test")
          .source("source-system")
          .destination("destination-system")
          .subject("test-subject")
          .action("test-action")
          .outcome(AuditOutcome.SUCCESS)
          .build();

      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          () -> auditLogger.audit(
              TestAuditMessages.TEST_AUDIT_EVENT,
              auditContext,
              "only-one-parameter"
          )
      );

      assertTrue(exception.getMessage().contains("expects 2 parameters"));
    }
  }

  @Test
  void shouldDetectModifiedJournalRecord() throws Exception {
    Path journalRoot = temporaryDirectory.resolve("journal");

    AuditKeyUtils auditKeyUtils = new AuditKeyUtils();
    KeyPair keyPair = auditKeyUtils.generateEd25519KeyPair();

    Path journalPath;

    try (AppendOnlyAuditJournal auditJournal = new AppendOnlyAuditJournal(
        journalRoot,
        keyPair.getPrivate()
    )) {
      AuditLogger auditLogger = new AuditLogger(auditJournal);

      AuditContext auditContext = AuditContext.builder()
          .auditId("audit-1")
          .correlationId("correlation-1")
          .actor("unit-test")
          .actorType("test")
          .source("source-system")
          .destination("destination-system")
          .subject("test-subject")
          .action("test-action")
          .outcome(AuditOutcome.SUCCESS)
          .timestamp(Instant.parse("2026-05-25T00:00:00Z"))
          .build();

      auditLogger.audit(
          TestAuditMessages.TEST_AUDIT_EVENT,
          auditContext,
          "value-1",
          "value-2"
      );

      journalPath = auditJournal.getActiveJournalPath();
    }

    String journalContent = Files.readString(journalPath, StandardCharsets.UTF_8);
    String modifiedJournalContent = journalContent.replace("value-1", "tampered-value");

    Files.writeString(journalPath, modifiedJournalContent, StandardCharsets.UTF_8);

    AuditVerifier auditVerifier = new AuditVerifier(
        (java.security.interfaces.EdECPublicKey) keyPair.getPublic()
    );

    AuditVerifier.VerificationResult verificationResult = auditVerifier.verifyJournal(journalPath);

    assertFalse(verificationResult.valid());
    assertTrue(verificationResult.error().contains("Record hash mismatch"));
  }

  private enum TestAuditMessages implements LogMessage {

    TEST_AUDIT_EVENT(
        LEVEL.AUDIT,
        CATEGORY.AUDIT,
        "Test audit message {} {}"
    );

    private final @Getter LEVEL level;
    private final @Getter Category category;
    private final @Getter String message;
    private final @Getter int parameterCount;

    TestAuditMessages(LEVEL level, Category category, String message) {
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

    private enum CATEGORY implements Category {

      AUDIT("Test Audit");

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
}