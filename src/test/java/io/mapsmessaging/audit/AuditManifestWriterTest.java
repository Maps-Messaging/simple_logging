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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuditManifestWriterTest {

  @TempDir
  private Path temporaryDirectory;

  @Test
  void shouldWriteSignedManifestAndSignatureFile() throws Exception {
    AuditKeyUtils keyUtils = new AuditKeyUtils();
    KeyPair keyPair = keyUtils.generateEd25519KeyPair();
    AuditManifestWriter writer = new AuditManifestWriter(temporaryDirectory, keyPair.getPrivate());

    AuditPayloadReference journal = AuditPayloadReference.builder()
        .name("journal")
        .path("journal.jsonl")
        .size(100)
        .sha256("journal-hash")
        .build();

    AuditManifest manifest = writer.writeManifest(
        1,
        2,
        "first-hash",
        "last-hash",
        2,
        "maps-build",
        "translator-build",
        List.of(journal),
        List.of()
    );

    assertNotNull(manifest.getSignature());

    Path manifestPath = temporaryDirectory.resolve(manifest.getManifestId() + ".manifest.json");
    Path signaturePath = temporaryDirectory.resolve(manifest.getManifestId() + ".manifest.sig");

    assertTrue(Files.exists(manifestPath));
    assertTrue(Files.exists(signaturePath));
    assertEquals(manifest.getSignature(), Files.readString(signaturePath, StandardCharsets.UTF_8));

    JsonObject json = new Gson().fromJson(
        Files.readString(manifestPath, StandardCharsets.UTF_8),
        JsonObject.class
    );

    assertEquals(1, json.get("firstSequenceNumber").getAsLong());
    assertEquals(2, json.get("lastSequenceNumber").getAsLong());
    assertEquals("maps-build", json.get("mapsBuild").getAsString());
    assertEquals("translator-build", json.get("translatorBuild").getAsString());
    assertEquals(manifest.getSignature(), json.get("signature").getAsString());
  }

  @Test
  void shouldWriteUnsignedManifestWithoutSignatureFile() throws Exception {
    AuditManifestWriter writer = new AuditManifestWriter(temporaryDirectory, null);

    AuditManifestWriter.ManifestRequest request = new AuditManifestWriter.ManifestRequest(
        10,
        20,
        "first",
        "last",
        11,
        "maps",
        "translator",
        List.of(),
        List.of()
    );

    AuditManifest manifest = writer.writeManifest(request);

    Path manifestPath = temporaryDirectory.resolve(manifest.getManifestId() + ".manifest.json");
    Path signaturePath = temporaryDirectory.resolve(manifest.getManifestId() + ".manifest.sig");

    assertTrue(Files.exists(manifestPath));
    assertFalse(Files.exists(signaturePath));
    assertEquals(null, manifest.getSignature());
  }
}
