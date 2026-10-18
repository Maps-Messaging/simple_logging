# Simple Logging 2.1.3: usage examples

Simple Logging provides two complementary APIs:

- **Application logging** sends predefined, categorised messages to SLF4J.
- **Audit journaling** writes structured, append-only, hash-chained records to disk, optionally signed with Ed25519.

Use application logs for diagnostics and troubleshooting. Use the audit journal for events such as authentication decisions, administrative changes, and actions where preserving an attributable record matters. An `AUDIT`-level SLF4J message is **not** automatically written to the tamper-evident audit journal.

The examples below target Java 17 and version 2.1.3.

## Maven dependency

~~~xml
<dependency>
  <groupId>io.mapsmessaging</groupId>
  <artifactId>simple_logging</artifactId>
  <version>2.1.3</version>
</dependency>
~~~

For ordinary logging output, the application also needs an SLF4J provider such as Logback. Simple Logging depends on the SLF4J API but does not impose a production logging backend. The audit journal does not require a logging backend.

## 1. Define application messages in one place

A message implements `LogMessage`, including its severity, category, format string and number of `{}` placeholders. One enum can be shared by both APIs.

**ExampleMessages.java**

~~~java
package example;

import io.mapsmessaging.logging.Category;
import io.mapsmessaging.logging.LEVEL;
import io.mapsmessaging.logging.LogMessage;

public enum ExampleMessages implements LogMessage {

  CONNECTED(LEVEL.INFO, ExampleCategory.NETWORK, "Connected to {}", 1),
  CONNECTION_FAILED(LEVEL.ERROR, ExampleCategory.NETWORK, "Connection to {} failed", 1),
  CONFIG_CHANGED(LEVEL.AUDIT, ExampleCategory.SECURITY, "Configuration {} changed", 1);

  private final LEVEL level;
  private final Category category;
  private final String message;
  private final int parameterCount;

  ExampleMessages(LEVEL level, Category category, String message, int parameterCount) {
    this.level = level;
    this.category = category;
    this.message = message;
    this.parameterCount = parameterCount;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public LEVEL getLevel() {
    return level;
  }

  @Override
  public Category getCategory() {
    return category;
  }

  @Override
  public int getParameterCount() {
    return parameterCount;
  }

  private enum ExampleCategory implements Category {

    NETWORK("Messaging", "Network"),
    SECURITY("Messaging", "Security");

    private final String division;
    private final String description;

    ExampleCategory(String division, String description) {
      this.division = division;
      this.description = description;
    }

    @Override
    public String getDivision() {
      return division;
    }

    @Override
    public String getDescription() {
      return description;
    }
  }
}
~~~

## 2. Write normal application logs

**LoggingExample.java**

~~~java
package example;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import java.io.IOException;

public final class LoggingExample {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingExample.class);

  private LoggingExample() {
  }

  public static void main(String[] args) {
    LOGGER.log(ExampleMessages.CONNECTED, "broker-1");

    IOException failure = new IOException("Connection reset");
    LOGGER.log(ExampleMessages.CONNECTION_FAILED, failure, "broker-1");
  }
}
~~~

The second call retains the exception for the SLF4J provider, including its stack trace where the provider is configured to display it. You can also pass a `Throwable` as the final argument after the message parameters. Exception arguments do not count as format placeholders.

The message's category is placed in the library's thread context using the `division` and `category` keys for the logging call. Output formatting and destinations are controlled by your SLF4J provider.

## 3. Record and verify an audited change

The audit journal is independent of SLF4J output. `AuditLogger` takes the same `LogMessage` enum but additionally records context, outcome, hashes and optional signatures.

This self-contained example uses a temporary directory and creates a fresh signing key **for demonstration only**.

**AuditExample.java**

~~~java
package example;

import io.mapsmessaging.audit.AppendOnlyAuditJournal;
import io.mapsmessaging.audit.AuditContext;
import io.mapsmessaging.audit.AuditJournalConfig;
import io.mapsmessaging.audit.AuditKeyUtils;
import io.mapsmessaging.audit.AuditLogger;
import io.mapsmessaging.audit.AuditOutcome;
import io.mapsmessaging.audit.AuditPayloadReference;
import io.mapsmessaging.audit.AuditPayloadStore;
import io.mapsmessaging.audit.AuditRecord;
import io.mapsmessaging.audit.AuditVerifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.interfaces.EdECPublicKey;
import java.time.Instant;
import java.util.List;

public final class AuditExample {

  private AuditExample() {
  }

  public static void main(String[] args) throws Exception {
    Path root = Files.createTempDirectory("simple-logging-audit-");
    AuditKeyUtils keyUtils = new AuditKeyUtils();
    KeyPair keys = keyUtils.generateEd25519KeyPair();
    EdECPublicKey verificationKey = (EdECPublicKey) keys.getPublic();

    AuditJournalConfig config = AuditJournalConfig.builder()
        .journalRoot(root.resolve("journals"))
        .signingKey(keys.getPrivate())
        .verificationKey(verificationKey)
        .build();

    AuditContext context = AuditContext.builder()
        .auditId("audit-42")
        .correlationId("request-42")
        .actor("operator-17")
        .actorType("user")
        .source("admin-api")
        .subject("rate-limits")
        .action("update")
        .outcome(AuditOutcome.SUCCESS)
        .timestamp(Instant.now())
        .build();

    AuditPayloadStore payloadStore = new AuditPayloadStore(root.resolve("payloads"));
    AuditPayloadReference payload = payloadStore.writePayload(
        "request-42",
        "configuration-request",
        "request.json",
        "{\"enabled\":true}".getBytes(StandardCharsets.UTF_8)
    );

    Path journalPath;
    try (AppendOnlyAuditJournal journal = new AppendOnlyAuditJournal(config)) {
      AuditLogger auditLogger = new AuditLogger(journal);
      AuditRecord record = auditLogger.audit(
          ExampleMessages.CONFIG_CHANGED,
          context,
          List.of(payload),
          "rate-limits"
      );
      journalPath = journal.getActiveJournalPath();
      System.out.println("Recorded audit sequence " + record.getSequenceNumber());
    }

    AuditVerifier.VerificationResult result =
        new AuditVerifier(verificationKey).verifyJournal(journalPath);

    if (!result.valid()) {
      throw new IllegalStateException("Audit verification failed: " + result.error());
    }

    System.out.println("Verified " + result.verifiedRecords() + " audit record(s)");
  }
}
~~~

This example writes a journal entry describing a configuration change, plus a separately stored JSON payload referenced by name, relative path, size and SHA-256 digest. The journal entry contains a SHA-256 hash chain and, because a signing key was supplied, an Ed25519 signature.

## 4. Operational considerations

- **Persistent keys:** For real deployments, load the same signing key and verification key on each restart. Never generate a new key pair against an existing signed journal. Protect private keys using your organisation's secrets/key-management system.
- **Journal recovery:** `AppendOnlyAuditJournal` can recover sequence and hash state from existing journal files. Its configuration can reject invalid previous journals at startup.
- **Rotation:** Journals rotate daily by default and when the configured size threshold is exceeded (default 64 MiB). To verify a rotated history, call `AuditVerifier.verifyJournals(List<Path>)` with files ordered chronologically, not just `verifyJournal` on the latest file.
- **Signature policy:** The current verifier validates signatures **when present**. It does not reject a record merely because its signature is absent. If your application requires every record to be signed, independently enforce that requirement.
- **Audit failures:** `AuditLogger.audit` can throw `IOException`. Callers must decide whether an operation should be rejected when its audit record cannot be persisted.
- **Sensitive data:** Keep passwords, tokens and unnecessary personal information out of messages, attributes and stored payloads. A hash chain detects alteration; it does not encrypt data.
- **Retention:** The application remains responsible for file access permissions, retention, archival and backups.
