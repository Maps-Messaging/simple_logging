package io.mapsmessaging.audit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.EdECPublicKey;

public class AuditVerifier {

  private static final String GENESIS_HASH =
      "0000000000000000000000000000000000000000000000000000000000000000";

  private final Gson gson;
  private final AuditCrypto auditCrypto;
  private final EdECPublicKey publicKey;

  public AuditVerifier(EdECPublicKey publicKey) {
    this.publicKey = publicKey;
    this.gson = new GsonBuilder()
        .disableHtmlEscaping()
        .create();
    this.auditCrypto = new AuditCrypto();
  }

  public VerificationResult verifyJournal(Path journalPath) throws IOException {
    long expectedSequenceNumber = 1;
    long verifiedRecords = 0;
    String previousRecordHash = GENESIS_HASH;

    try (BufferedReader bufferedReader = Files.newBufferedReader(journalPath, StandardCharsets.UTF_8)) {
      String line = bufferedReader.readLine();

      while (line != null) {
        if (!line.isBlank()) {
          JsonObject journalObject = gson.fromJson(line, JsonObject.class);

          long sequenceNumber = journalObject.get("sequenceNumber").getAsLong();
          String storedPreviousRecordHash = journalObject.get("previousRecordHash").getAsString();
          String storedRecordHash = journalObject.get("recordHash").getAsString();
          String storedSignature = journalObject.has("signature")
              ? journalObject.get("signature").getAsString()
              : "";

          if (sequenceNumber != expectedSequenceNumber) {
            return VerificationResult.failed(
                verifiedRecords,
                "Expected sequence " + expectedSequenceNumber + " but found " + sequenceNumber
            );
          }

          if (!previousRecordHash.equals(storedPreviousRecordHash)) {
            return VerificationResult.failed(
                verifiedRecords,
                "Previous hash mismatch at sequence " + sequenceNumber
            );
          }

          journalObject.remove("recordHash");
          journalObject.remove("signature");

          String canonicalJson = gson.toJson(journalObject);
          String calculatedHash = auditCrypto.sha256Hex(canonicalJson);

          if (!calculatedHash.equals(storedRecordHash)) {
            return VerificationResult.failed(
                verifiedRecords,
                "Record hash mismatch at sequence " + sequenceNumber
            );
          }

          if (publicKey != null && storedSignature != null && !storedSignature.isBlank()) {
            boolean signatureValid = auditCrypto.verifySignature(
                publicKey,
                storedRecordHash,
                storedSignature
            );

            if (!signatureValid) {
              return VerificationResult.failed(
                  verifiedRecords,
                  "Signature mismatch at sequence " + sequenceNumber
              );
            }
          }

          previousRecordHash = storedRecordHash;
          expectedSequenceNumber++;
          verifiedRecords++;
        }

        line = bufferedReader.readLine();
      }
    }

    return VerificationResult.successful(verifiedRecords, previousRecordHash);
  }

  public record VerificationResult(
      boolean valid,
      long verifiedRecords,
      String lastHash,
      String error
  ) {

    public static VerificationResult successful(long verifiedRecords, String lastHash) {
      return new VerificationResult(true, verifiedRecords, lastHash, "");
    }

    public static VerificationResult failed(long verifiedRecords, String error) {
      return new VerificationResult(false, verifiedRecords, "", error);
    }
  }
}