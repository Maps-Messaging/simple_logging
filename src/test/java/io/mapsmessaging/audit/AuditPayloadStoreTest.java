/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.audit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuditPayloadStoreTest {

  @TempDir
  private Path temporaryDirectory;

  @Test
  void shouldUseInjectedClockAndSanitiseTranslationId() throws Exception {
    Clock clock = Clock.fixed(Instant.parse("2026-09-27T10:00:00Z"), ZoneOffset.UTC);
    AuditPayloadStore store = new AuditPayloadStore(temporaryDirectory, clock);
    byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);

    AuditPayloadReference reference = store.writePayload(
        "translation/id",
        "input",
        "input.bin",
        payload
    );

    assertEquals("2026-09-27/translation_id/input.bin", reference.getPath().replace('\\', '/'));
    assertEquals(payload.length, reference.getSize());
    assertArrayEquals(payload, Files.readAllBytes(temporaryDirectory.resolve(reference.getPath())));
    assertTrue(reference.getSha256().matches("[0-9a-f]{64}"));
  }
}
