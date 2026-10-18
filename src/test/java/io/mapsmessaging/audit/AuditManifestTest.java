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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuditManifestTest {

  @Test
  void shouldBuildCanonicalAndSignedJson() {
    AuditPayloadReference journal = AuditPayloadReference.builder()
        .name("journal")
        .path("2026-09-27/audit.jsonl")
        .size(123)
        .sha256("abc")
        .build();

    AuditPayloadReference payload = AuditPayloadReference.builder()
        .name("payload")
        .path("2026-09-27/payload.bin")
        .size(456)
        .sha256("def")
        .build();

    AuditManifest manifest = AuditManifest.builder()
        .manifestId("manifest-1")
        .createdAt(Instant.parse("2026-09-27T00:00:00Z"))
        .firstSequenceNumber(10)
        .lastSequenceNumber(20)
        .firstEventHash("first")
        .lastEventHash("last")
        .eventCount(11)
        .mapsBuild("maps-1")
        .translatorBuild("translator-1")
        .journalFiles(List.of(journal))
        .payloadFiles(List.of(payload))
        .signature("signature")
        .build();

    JsonObject canonical = manifest.toCanonicalJsonObject();

    assertEquals("manifest-1", canonical.get("manifestId").getAsString());
    assertEquals("2026-09-27T00:00:00Z", canonical.get("createdAt").getAsString());
    assertEquals(10, canonical.get("firstSequenceNumber").getAsLong());
    assertEquals(20, canonical.get("lastSequenceNumber").getAsLong());
    assertEquals("first", canonical.get("firstEventHash").getAsString());
    assertEquals("last", canonical.get("lastEventHash").getAsString());
    assertEquals(11, canonical.get("eventCount").getAsLong());

    JsonArray journalFiles = canonical.getAsJsonArray("journalFiles");
    assertEquals(1, journalFiles.size());
    assertEquals("journal", journalFiles.get(0).getAsJsonObject().get("name").getAsString());

    JsonArray payloadFiles = canonical.getAsJsonArray("payloadFiles");
    assertEquals(1, payloadFiles.size());
    assertEquals("payload", payloadFiles.get(0).getAsJsonObject().get("name").getAsString());

    JsonObject signed = manifest.toSignedJsonObject();
    assertEquals("signature", signed.get("signature").getAsString());
  }

  @Test
  void shouldNormaliseNullValuesAndLists() {
    AuditManifest manifest = new AuditManifest();

    JsonObject canonical = manifest.toCanonicalJsonObject();

    assertEquals("", canonical.get("manifestId").getAsString());
    assertEquals("", canonical.get("createdAt").getAsString());
    assertEquals("", canonical.get("firstEventHash").getAsString());
    assertEquals("", canonical.get("lastEventHash").getAsString());
    assertEquals("", canonical.get("mapsBuild").getAsString());
    assertEquals("", canonical.get("translatorBuild").getAsString());
    assertTrue(manifest.getJournalFiles().isEmpty());
    assertTrue(manifest.getPayloadFiles().isEmpty());
    assertTrue(canonical.getAsJsonArray("journalFiles").isEmpty());
    assertTrue(canonical.getAsJsonArray("payloadFiles").isEmpty());
    assertEquals("", manifest.toSignedJsonObject().get("signature").getAsString());
  }
}
