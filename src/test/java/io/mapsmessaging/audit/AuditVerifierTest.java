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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.interfaces.EdECPublicKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuditVerifierTest {

  @TempDir
  private Path temporaryDirectory;

  @Test
  void shouldRejectUnexpectedSequenceNumber() throws Exception {
    JournalFixture fixture = writeJournal();
    mutateJournal(fixture.path(), "sequenceNumber", 2);

    AuditVerifier.VerificationResult result = verifier(fixture.keyPair()).verifyJournal(fixture.path());

    assertFalse(result.valid());
    assertEquals("Expected sequence 1 but found 2", result.error());
  }

  @Test
  void shouldRejectPreviousHashMismatch() throws Exception {
    JournalFixture fixture = writeJournal();
    mutateJournal(
        fixture.path(),
        "previousRecordHash",
        "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
    );

    AuditVerifier.VerificationResult result = verifier(fixture.keyPair()).verifyJournal(fixture.path());

    assertFalse(result.valid());
    assertEquals("Previous hash mismatch at sequence 1", result.error());
  }

  @Test
  void shouldRejectSignatureMismatch() throws Exception {
    JournalFixture fixture = writeJournal();
    mutateJournal(fixture.path(), "signature", "AAAA");

    AuditVerifier.VerificationResult result = verifier(fixture.keyPair()).verifyJournal(fixture.path());

    assertFalse(result.valid());
    assertEquals("Signature mismatch at sequence 1", result.error());
  }

  @Test
  void shouldPreserveFileReadFailureFromKeyUtility() {
    AuditKeyUtils keyUtils = new AuditKeyUtils();
    Path missingKey = temporaryDirectory.resolve("missing.pem");

    IOException exception = assertThrows(IOException.class, () -> keyUtils.readPublicKey(missingKey));

    assertTrue(exception.getCause() == null);
  }

  private JournalFixture writeJournal() throws Exception {
    AuditKeyUtils keyUtils = new AuditKeyUtils();
    KeyPair keyPair = keyUtils.generateEd25519KeyPair();
    Path journalRoot = temporaryDirectory.resolve("journal");

    Path journalPath;
    try (AppendOnlyAuditJournal journal = new AppendOnlyAuditJournal(journalRoot, keyPair.getPrivate())) {
      AuditRecord record = AuditRecord.builder()
          .auditId("audit-1")
          .correlationId("correlation-1")
          .messageCode("TEST")
          .level("AUDIT")
          .categoryDivision("Audit")
          .categoryDescription("Test")
          .build();
      journal.append(record);
      journalPath = journal.getActiveJournalPath();
    }

    return new JournalFixture(journalPath, keyPair);
  }

  private AuditVerifier verifier(KeyPair keyPair) {
    return new AuditVerifier((EdECPublicKey) keyPair.getPublic());
  }

  private void mutateJournal(Path path, String property, Object value) throws IOException {
    Gson gson = new Gson();
    String line = Files.readString(path, StandardCharsets.UTF_8).trim();
    var jsonObject = gson.fromJson(line, com.google.gson.JsonObject.class);

    if (value instanceof Number number) {
      jsonObject.addProperty(property, number);
    } else {
      jsonObject.addProperty(property, value.toString());
    }

    Files.writeString(path, gson.toJson(jsonObject) + System.lineSeparator(), StandardCharsets.UTF_8);
  }

  private record JournalFixture(Path path, KeyPair keyPair) {
  }
}
