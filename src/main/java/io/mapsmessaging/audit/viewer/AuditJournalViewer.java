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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.mapsmessaging.audit.AuditCrypto;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.EdECPublicKey;
import java.util.ArrayList;
import java.util.List;

public class AuditJournalViewer {

  private static final String GENESIS_HASH =
      "0000000000000000000000000000000000000000000000000000000000000000";

  private final Gson gson;
  private final AuditCrypto auditCrypto;
  private final EdECPublicKey publicKey;

  public AuditJournalViewer(EdECPublicKey publicKey) {
    this.publicKey = publicKey;
    this.gson = new GsonBuilder()
        .disableHtmlEscaping()
        .create();
    this.auditCrypto = new AuditCrypto();
  }

  public List<AuditRecordView> readAndVerify(Path journalPath) throws IOException {
    List<AuditRecordView> records = new ArrayList<>();

    long expectedSequenceNumber = 1;
    long lineNumber = 0;
    String previousRecordHash = GENESIS_HASH;
    boolean chainAlreadyBroken = false;

    try (BufferedReader bufferedReader = Files.newBufferedReader(journalPath, StandardCharsets.UTF_8)) {
      String line = bufferedReader.readLine();

      while (line != null) {
        lineNumber++;

        if (!line.isBlank()) {
          AuditRecordView recordView = verifyLine(
              line,
              lineNumber,
              expectedSequenceNumber,
              previousRecordHash,
              chainAlreadyBroken
          );

          records.add(recordView);

          if (recordView.getStatus() == AuditRecordVerificationStatus.VALID) {
            previousRecordHash = recordView.getRecordHash();
            expectedSequenceNumber++;
          } else {
            chainAlreadyBroken = true;

            if (recordView.getSequenceNumber() == expectedSequenceNumber) {
              expectedSequenceNumber++;
            }

            if (recordView.getRecordHash() != null && !recordView.getRecordHash().isBlank()) {
              previousRecordHash = recordView.getRecordHash();
            }
          }
        }

        line = bufferedReader.readLine();
      }
    }

    return records;
  }

  private AuditRecordView verifyLine(
      String line,
      long lineNumber,
      long expectedSequenceNumber,
      String previousRecordHash,
      boolean chainAlreadyBroken
  ) {
    try {
      JsonObject journalObject = gson.fromJson(line, JsonObject.class);

      AuditRecordView recordView = createRecordView(lineNumber, journalObject);

      if (chainAlreadyBroken) {
        recordView.setStatus(AuditRecordVerificationStatus.INVALID);
        recordView.setValidationMessage("Previous record in chain was invalid");
        return recordView;
      }

      if (!journalObject.has("sequenceNumber")) {
        return invalid(recordView, "Missing sequenceNumber");
      }

      if (!journalObject.has("previousRecordHash")) {
        return invalid(recordView, "Missing previousRecordHash");
      }

      if (!journalObject.has("recordHash")) {
        return invalid(recordView, "Missing recordHash");
      }

      long sequenceNumber = journalObject.get("sequenceNumber").getAsLong();
      String storedPreviousRecordHash = journalObject.get("previousRecordHash").getAsString();
      String storedRecordHash = journalObject.get("recordHash").getAsString();

      if (sequenceNumber != expectedSequenceNumber) {
        return invalid(
            recordView,
            "Expected sequence " + expectedSequenceNumber + " but found " + sequenceNumber
        );
      }

      if (!previousRecordHash.equals(storedPreviousRecordHash)) {
        return invalid(
            recordView,
            "Previous hash mismatch"
        );
      }

      String storedSignature = journalObject.has("signature")
          ? journalObject.get("signature").getAsString()
          : "";

      journalObject.remove("recordHash");
      journalObject.remove("signature");

      String canonicalJson = gson.toJson(journalObject);
      String calculatedHash = auditCrypto.sha256Hex(canonicalJson);

      if (!calculatedHash.equals(storedRecordHash)) {
        return invalid(
            recordView,
            "Record hash mismatch"
        );
      }

      if (publicKey != null && storedSignature != null && !storedSignature.isBlank()) {
        boolean signatureValid = auditCrypto.verifySignature(
            publicKey,
            storedRecordHash,
            storedSignature
        );

        if (!signatureValid) {
          return invalid(
              recordView,
              "Signature mismatch"
          );
        }
      }

      recordView.setStatus(AuditRecordVerificationStatus.VALID);
      recordView.setValidationMessage("Valid");

      return recordView;
    } catch (Exception exception) {
      return AuditRecordView.builder()
          .lineNumber(lineNumber)
          .sequenceNumber(-1)
          .status(AuditRecordVerificationStatus.INVALID)
          .validationMessage("Invalid JSON or unreadable audit record: " + exception.getMessage())
          .build();
    }
  }

  private AuditRecordView createRecordView(long lineNumber, JsonObject journalObject) {
    return AuditRecordView.builder()
        .lineNumber(lineNumber)
        .sequenceNumber(getLong(journalObject, "sequenceNumber"))
        .timestamp(getString(journalObject, "timestamp"))
        .correlationId(getString(journalObject, "correlationId"))
        .parentCorrelationId(getString(journalObject, "parentCorrelationId"))
        .actor(getString(journalObject, "actor"))
        .source(getString(journalObject, "source"))
        .destination(getString(journalObject, "destination"))
        .action(getString(journalObject, "action"))
        .outcome(getString(journalObject, "outcome"))
        .categoryDivision(getString(journalObject, "categoryDivision"))
        .categoryDescription(getString(journalObject, "categoryDescription"))
        .messageCode(getString(journalObject, "messageCode"))
        .message(getString(journalObject, "message"))
        .recordHash(getString(journalObject, "recordHash"))
        .previousRecordHash(getString(journalObject, "previousRecordHash"))
        .status(AuditRecordVerificationStatus.INVALID)
        .validationMessage("Not verified")
        .build();
  }

  private AuditRecordView invalid(AuditRecordView recordView, String validationMessage) {
    recordView.setStatus(AuditRecordVerificationStatus.INVALID);
    recordView.setValidationMessage(validationMessage);
    return recordView;
  }

  private String getString(JsonObject jsonObject, String name) {
    if (!jsonObject.has(name) || jsonObject.get(name).isJsonNull()) {
      return "";
    }

    return jsonObject.get(name).getAsString();
  }

  private long getLong(JsonObject jsonObject, String name) {
    if (!jsonObject.has(name) || jsonObject.get(name).isJsonNull()) {
      return -1;
    }

    return jsonObject.get(name).getAsLong();
  }
}