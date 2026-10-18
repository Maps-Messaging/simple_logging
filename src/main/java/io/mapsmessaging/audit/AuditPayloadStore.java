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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Pattern;

public class AuditPayloadStore {

  private static final Pattern UNSAFE_TRANSLATION_ID = Pattern.compile("[^a-zA-Z0-9._-]");

  private final Path payloadRoot;
  private final AuditCrypto auditCrypto;
  private final Clock clock;

  public AuditPayloadStore(Path payloadRoot) {
    this(payloadRoot, Clock.systemDefaultZone());
  }

  AuditPayloadStore(Path payloadRoot, Clock clock) {
    this.payloadRoot = payloadRoot;
    this.auditCrypto = new AuditCrypto();
    this.clock = clock;
  }

  public AuditPayloadReference writePayload(
      String translationId,
      String name,
      String fileName,
      byte[] payload
  ) throws IOException {
    LocalDate localDate = LocalDate.now(clock);
    String safeTranslationId = safeTranslationId(translationId);

    Path payloadDirectory = payloadRoot
        .resolve(localDate.toString())
        .resolve(safeTranslationId);

    Files.createDirectories(payloadDirectory);

    Path payloadPath = payloadDirectory.resolve(fileName);

    writeAndForce(payloadPath, payload);

    String payloadHash = auditCrypto.sha256Hex(payload);

    return AuditPayloadReference.builder()
        .name(name)
        .path(payloadRoot.relativize(payloadPath).toString())
        .size(payload.length)
        .sha256(payloadHash)
        .build();
  }

  private String safeTranslationId(String translationId) {
    if (translationId == null || translationId.isBlank()) {
      return UUID.randomUUID().toString();
    }

    return UNSAFE_TRANSLATION_ID.matcher(translationId).replaceAll("_");
  }

  private void writeAndForce(Path path, byte[] payload) throws IOException {
    ByteBuffer byteBuffer = ByteBuffer.wrap(payload);

    try (FileChannel fileChannel = FileChannel.open(
        path,
        StandardOpenOption.CREATE_NEW,
        StandardOpenOption.WRITE
    )) {
      while (byteBuffer.hasRemaining()) {
        fileChannel.write(byteBuffer);
      }
      fileChannel.force(true);
    }
  }
}