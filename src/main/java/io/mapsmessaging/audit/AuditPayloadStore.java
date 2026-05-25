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
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.UUID;

public class AuditPayloadStore {

  private final Path payloadRoot;
  private final AuditCrypto auditCrypto;

  public AuditPayloadStore(Path payloadRoot) {
    this.payloadRoot = payloadRoot;
    this.auditCrypto = new AuditCrypto();
  }

  public AuditPayloadReference writePayload(
      String translationId,
      String name,
      String fileName,
      byte[] payload
  ) throws IOException {
    LocalDate localDate = LocalDate.now();
    String safeTranslationId = safeTranslationId(translationId);

    Path payloadDirectory = payloadRoot
        .resolve(localDate.toString())
        .resolve(safeTranslationId);

    Files.createDirectories(payloadDirectory);

    Path payloadPath = payloadDirectory.resolve(fileName);

    try (FileChannel fileChannel = FileChannel.open(
        payloadPath,
        StandardOpenOption.CREATE_NEW,
        StandardOpenOption.WRITE
    )) {
      fileChannel.write(java.nio.ByteBuffer.wrap(payload));
      fileChannel.force(true);
    }

    String payloadHash = auditCrypto.sha256Hex(payload);
    long payloadSize = payload.length;

    return AuditPayloadReference.builder()
        .name(name)
        .path(payloadRoot.relativize(payloadPath).toString())
        .size(payloadSize)
        .sha256(payloadHash)
        .build();
  }

  private String safeTranslationId(String translationId) {
    if (translationId == null || translationId.isBlank()) {
      return UUID.randomUUID().toString();
    }

    return translationId.replaceAll("[^a-zA-Z0-9._-]", "_");
  }
}