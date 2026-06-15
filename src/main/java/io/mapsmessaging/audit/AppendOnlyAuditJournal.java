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
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicLong;

public class AppendOnlyAuditJournal implements AuditJournal {

  private static final String GENESIS_HASH =
      "0000000000000000000000000000000000000000000000000000000000000000";

  private static final Pattern JOURNAL_FILE_PATTERN =
      Pattern.compile("audit-(\\d{4}-\\d{2}-\\d{2})-(\\d{6})\\.jsonl");

  private final Path journalRoot;
  private final AuditCrypto auditCrypto;
  private final Gson gson;
  private final PrivateKey signingKey;
  private final AtomicLong sequenceNumber;
  private final long maxJournalSizeBytes;
  private final boolean rotateDaily;
  private final boolean failOnInvalidExistingJournal;

  private FileChannel fileChannel;
  private Path activeJournalPath;
  private String previousRecordHash;
  private LocalDate activeJournalDate;
  private int activeJournalIndex;

  public AppendOnlyAuditJournal(Path journalRoot, PrivateKey signingKey) throws IOException {
    this(AuditJournalConfig.builder()
        .journalRoot(journalRoot)
        .signingKey(signingKey)
        .build());
  }

  public AppendOnlyAuditJournal(AuditJournalConfig auditJournalConfig) throws IOException {
    this.journalRoot = auditJournalConfig.getJournalRoot();
    this.signingKey = auditJournalConfig.getSigningKey();
    this.maxJournalSizeBytes = auditJournalConfig.getMaxJournalSizeBytes();
    this.rotateDaily = auditJournalConfig.isRotateDaily();
    this.failOnInvalidExistingJournal = auditJournalConfig.isFailOnInvalidExistingJournal();

    this.auditCrypto = new AuditCrypto();
    this.gson = new GsonBuilder()
        .disableHtmlEscaping()
        .create();

    this.sequenceNumber = new AtomicLong(0);
    this.previousRecordHash = GENESIS_HASH;

    recoverExistingJournalState(auditJournalConfig);
    openWritableJournalFile();
  }

  @Override
  public synchronized AuditRecord append(AuditRecord auditRecord) throws IOException {
    rotateForDateIfRequired();

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

    rotateForSizeIfRequired(journalBytes.length);
    writeFully(journalBytes);

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

  @Override
  public synchronized void close() throws IOException {
    if (fileChannel != null) {
      fileChannel.force(true);
      fileChannel.close();
      fileChannel = null;
    }
  }

  private void recoverExistingJournalState(AuditJournalConfig auditJournalConfig) throws IOException {
    List<JournalFile> journalFiles = discoverJournalFiles();

    if (journalFiles.isEmpty()) {
      LocalDate localDate = LocalDate.now();
      activeJournalDate = localDate;
      activeJournalIndex = 1;
      activeJournalPath = buildJournalPath(activeJournalDate, activeJournalIndex);
      return;
    }

    List<Path> journalPaths = journalFiles.stream()
        .map(JournalFile::path)
        .toList();

    AuditVerifier auditVerifier = new AuditVerifier(auditJournalConfig.getVerificationKey());
    AuditVerifier.VerificationResult verificationResult = auditVerifier.verifyJournals(journalPaths);

    if (!verificationResult.valid()) {
      if (failOnInvalidExistingJournal) {
        throw new IOException("Existing audit journal verification failed: " + verificationResult.error());
      }

      LocalDate localDate = LocalDate.now();
      activeJournalDate = localDate;
      activeJournalIndex = nextJournalIndexForDate(localDate, journalFiles);
      activeJournalPath = buildJournalPath(activeJournalDate, activeJournalIndex);
      return;
    }

    sequenceNumber.set(verificationResult.verifiedRecords());
    previousRecordHash = verificationResult.lastHash();

    JournalFile latestJournalFile = journalFiles.get(journalFiles.size() - 1);

    activeJournalDate = latestJournalFile.localDate();
    activeJournalIndex = latestJournalFile.index();
    activeJournalPath = latestJournalFile.path();

    if (shouldRotateForRecoveredJournal()) {
      rotateJournalFile();
    }
  }

  private List<JournalFile> discoverJournalFiles() throws IOException {
    if (!Files.exists(journalRoot)) {
      return List.of();
    }

    try (Stream<Path> pathStream = Files.walk(journalRoot)) {
      return pathStream
          .filter(Files::isRegularFile)
          .map(this::toJournalFile)
          .filter(journalFile -> journalFile != null)
          .sorted(Comparator
              .comparing(JournalFile::localDate)
              .thenComparingInt(JournalFile::index))
          .toList();
    }
  }

  private JournalFile toJournalFile(Path path) {
    String fileName = path.getFileName().toString();
    Matcher matcher = JOURNAL_FILE_PATTERN.matcher(fileName);

    if (!matcher.matches()) {
      return null;
    }

    LocalDate localDate = LocalDate.parse(matcher.group(1));
    int index = Integer.parseInt(matcher.group(2));

    return new JournalFile(path, localDate, index);
  }

  private boolean shouldRotateForRecoveredJournal() throws IOException {
    if (rotateDaily && !activeJournalDate.equals(LocalDate.now())) {
      return true;
    }

    return Files.exists(activeJournalPath)
        && Files.size(activeJournalPath) >= maxJournalSizeBytes;
  }

  private void openWritableJournalFile() throws IOException {
    Files.createDirectories(activeJournalPath.getParent());

    fileChannel = FileChannel.open(
        activeJournalPath,
        StandardOpenOption.CREATE,
        StandardOpenOption.WRITE,
        StandardOpenOption.APPEND,
        StandardOpenOption.DSYNC
    );
  }

  private void rotateForDateIfRequired() throws IOException {
    if (!rotateDaily) {
      return;
    }

    LocalDate localDate = LocalDate.now();

    if (!activeJournalDate.equals(localDate)) {
      rotateJournalFile();
    }
  }

  private void rotateForSizeIfRequired(int pendingBytes) throws IOException {
    long currentSize = fileChannel.size();
    long nextSize = currentSize + pendingBytes;

    if (nextSize > maxJournalSizeBytes) {
      rotateJournalFile();
    }
  }

  private void rotateJournalFile() throws IOException {
    close();

    LocalDate localDate = LocalDate.now();

    if (!localDate.equals(activeJournalDate)) {
      activeJournalDate = localDate;
      activeJournalIndex = 1;
    } else {
      activeJournalIndex++;
    }

    activeJournalPath = buildJournalPath(activeJournalDate, activeJournalIndex);

    while (Files.exists(activeJournalPath)) {
      activeJournalIndex++;
      activeJournalPath = buildJournalPath(activeJournalDate, activeJournalIndex);
    }

    openWritableJournalFile();
  }

  private int nextJournalIndexForDate(LocalDate localDate, List<JournalFile> journalFiles) {
    return journalFiles.stream()
        .filter(journalFile -> journalFile.localDate().equals(localDate))
        .mapToInt(JournalFile::index)
        .max()
        .orElse(0) + 1;
  }

  private Path buildJournalPath(LocalDate localDate, int index) {
    String fileName = String.format("audit-%s-%06d.jsonl", localDate, index);
    return journalRoot
        .resolve(localDate.toString())
        .resolve(fileName);
  }

  private void writeFully(byte[] journalBytes) throws IOException {
    ByteBuffer byteBuffer = ByteBuffer.wrap(journalBytes);

    while (byteBuffer.hasRemaining()) {
      fileChannel.write(byteBuffer);
    }

    fileChannel.force(true);
  }

  private record JournalFile(
      Path path,
      LocalDate localDate,
      int index
  ) {
  }
}