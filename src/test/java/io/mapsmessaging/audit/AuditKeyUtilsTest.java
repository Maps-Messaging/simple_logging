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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuditKeyUtilsTest {

  @TempDir
  private Path temporaryDirectory;

  @Test
  void shouldRoundTripPrivateAndPublicKeys() throws Exception {
    AuditKeyUtils keyUtils = new AuditKeyUtils();
    KeyPair keyPair = keyUtils.generateEd25519KeyPair();

    Path privateKeyPath = temporaryDirectory.resolve("private.pem");
    Path publicKeyPath = temporaryDirectory.resolve("public.pem");

    keyUtils.writePrivateKey(privateKeyPath, keyPair.getPrivate());
    keyUtils.writePublicKey(publicKeyPath, keyPair.getPublic());

    assertArrayEquals(
        keyPair.getPrivate().getEncoded(),
        keyUtils.readPrivateKey(privateKeyPath).getEncoded()
    );
    assertArrayEquals(
        keyPair.getPublic().getEncoded(),
        keyUtils.readPublicKey(publicKeyPath).getEncoded()
    );
  }

  @Test
  void shouldRejectInvalidPrivateKeyData() throws Exception {
    AuditKeyUtils keyUtils = new AuditKeyUtils();
    Path privateKeyPath = temporaryDirectory.resolve("private.pem");

    Files.writeString(
        privateKeyPath,
        "-----BEGIN PRIVATE KEY-----\nAQID\n-----END PRIVATE KEY-----\n",
        StandardCharsets.UTF_8
    );

    assertThrows(IOException.class, () -> keyUtils.readPrivateKey(privateKeyPath));
  }

  @Test
  void shouldRejectInvalidPublicKeyData() throws Exception {
    AuditKeyUtils keyUtils = new AuditKeyUtils();
    Path publicKeyPath = temporaryDirectory.resolve("public.pem");

    Files.writeString(
        publicKeyPath,
        "-----BEGIN PUBLIC KEY-----\nAQID\n-----END PUBLIC KEY-----\n",
        StandardCharsets.UTF_8
    );

    assertThrows(IOException.class, () -> keyUtils.readPublicKey(publicKeyPath));
  }
}
