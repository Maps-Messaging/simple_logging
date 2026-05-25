package io.mapsmessaging.audit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.PrivateKey;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

public class AppendOnlyAuditJournal implements AuditJournal {

  private static final String GENESIS_HASH =
      "0000000000000000000000000000000000000000000000000000000000000000";

  private final Path journalRoot;
  private final AuditCrypto auditCrypto;
  private final Gson gson;
  private final PrivateKey signingKey;
  private final AtomicLong sequenceNumber;

  private FileChannel fileChannel;
  private Path activeJournalPath;
  private String previousRecordHash;

  public AppendOnlyAuditJournal(Path journalRoot, PrivateKey signingKey) throws IOException {
    this.journalRoot = journalRoot;
    this.signingKey = signingKey;
    this.auditCrypto = new AuditCrypto();
    this.gson = new GsonBuilder()
        .disableHtmlEscaping()
        .create();
    this.sequenceNumber = new AtomicLong(0);
    this.previousRecordHash = GENESIS_HASH;

    openJournalFile();
  }

  @Override
  public synchronized AuditRecord append(AuditRecord auditRecord) throws IOException {
    auditRecord.setSequenceNumber(sequenceNumber.incrementAndGet());
    auditRecord.setPreviousRecordHash(previousRecordHash);

    String canonicalJson = gson.toJson(auditRecord.toCanonicalJsonObject());
    String recordHash = auditCrypto.sha256Hex(canonicalJson);

    auditRecord.setRecordHash(recordHash);

    if (signingKey != null) {
      auditRecord.setSignature(auditCrypto.signBase64(signingKey, recordHash));
    }

    String journalJson = gson.toJson(auditRecord.toJournalJsonObject());
    byte[] journalBytes = (journalJson + "\n").getBytes(StandardCharsets.UTF_8);

    fileChannel.write(ByteBuffer.wrap(journalBytes));
    fileChannel.force(true);

    previousRecordHash = recordHash;

    return auditRecord;
  }

  @Override
  public synchronized String getCurrentRecordHash() {
    return previousRecordHash;
  }

  @Override
  public long getCurrentSequenceNumber() {
    return sequenceNumber.get();
  }

  public synchronized Path getActiveJournalPath() {
    return activeJournalPath;
  }

  private void openJournalFile() throws IOException {
    LocalDate localDate = LocalDate.now();
    Path journalDirectory = journalRoot.resolve(localDate.toString());

    Files.createDirectories(journalDirectory);

    activeJournalPath = journalDirectory.resolve("audit-" + localDate + "-000001.jsonl");

    fileChannel = FileChannel.open(
        activeJournalPath,
        StandardOpenOption.CREATE,
        StandardOpenOption.WRITE,
        StandardOpenOption.APPEND,
        StandardOpenOption.DSYNC
    );
  }

  @Override
  public synchronized void close() throws IOException {
    if (fileChannel != null) {
      fileChannel.force(true);
      fileChannel.close();
    }
  }
}