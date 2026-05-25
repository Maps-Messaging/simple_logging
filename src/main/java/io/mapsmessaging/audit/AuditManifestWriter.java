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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.PrivateKey;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AuditManifestWriter {

  private final Path manifestRoot;
  private final AuditCrypto auditCrypto;
  private final Gson gson;
  private final PrivateKey signingKey;

  public AuditManifestWriter(Path manifestRoot, PrivateKey signingKey) {
    this.manifestRoot = manifestRoot;
    this.signingKey = signingKey;
    this.auditCrypto = new AuditCrypto();
    this.gson = new GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create();
  }

  public AuditManifest writeManifest(
      long firstSequenceNumber,
      long lastSequenceNumber,
      String firstEventHash,
      String lastEventHash,
      long eventCount,
      String mapsBuild,
      String translatorBuild,
      List<AuditPayloadReference> journalFiles,
      List<AuditPayloadReference> payloadFiles
  ) throws IOException {
    Files.createDirectories(manifestRoot);

    Instant createdAt = Instant.now();
    String manifestId = "audit-" + DateTimeFormatter.ISO_INSTANT.format(createdAt)
        .replace(":", "")
        .replace(".", "-");

    AuditManifest auditManifest = AuditManifest.builder()
        .manifestId(manifestId)
        .createdAt(createdAt)
        .firstSequenceNumber(firstSequenceNumber)
        .lastSequenceNumber(lastSequenceNumber)
        .firstEventHash(firstEventHash)
        .lastEventHash(lastEventHash)
        .eventCount(eventCount)
        .mapsBuild(mapsBuild)
        .translatorBuild(translatorBuild)
        .journalFiles(journalFiles)
        .payloadFiles(payloadFiles)
        .build();

    String canonicalJson = new GsonBuilder()
        .disableHtmlEscaping()
        .create()
        .toJson(auditManifest.toCanonicalJsonObject());

    if (signingKey != null) {
      auditManifest.setSignature(auditCrypto.signBase64(signingKey, canonicalJson));
    }

    Path manifestPath = manifestRoot.resolve(manifestId + ".manifest.json");
    Path signaturePath = manifestRoot.resolve(manifestId + ".manifest.sig");

    writeAndForce(manifestPath, gson.toJson(auditManifest.toSignedJsonObject()));

    if (auditManifest.getSignature() != null) {
      writeAndForce(signaturePath, auditManifest.getSignature());
    }

    return auditManifest;
  }

  private void writeAndForce(Path path, String data) throws IOException {
    byte[] bytes = data.getBytes(StandardCharsets.UTF_8);

    try (FileChannel fileChannel = FileChannel.open(
        path,
        StandardOpenOption.CREATE_NEW,
        StandardOpenOption.WRITE
    )) {
      fileChannel.write(java.nio.ByteBuffer.wrap(bytes));
      fileChannel.force(true);
    }
  }
}