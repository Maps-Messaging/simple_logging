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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.EdECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class AuditKeyUtils {

  public KeyPair generateEd25519KeyPair() {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("Ed25519");
      return keyPairGenerator.generateKeyPair();
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to generate Ed25519 key pair", exception);
    }
  }

  public void writePrivateKey(Path path, PrivateKey privateKey) throws IOException {
    writePem(path, "PRIVATE KEY", privateKey.getEncoded());
  }

  public void writePublicKey(Path path, PublicKey publicKey) throws IOException {
    writePem(path, "PUBLIC KEY", publicKey.getEncoded());
  }

  public PrivateKey readPrivateKey(Path path) throws IOException {
    try {
      byte[] keyBytes = readPem(path, "PRIVATE KEY");
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
      KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
      return keyFactory.generatePrivate(keySpec);
    } catch (Exception exception) {
      throw new IOException("Unable to read Ed25519 private key", exception);
    }
  }

  public EdECPublicKey readPublicKey(Path path) throws IOException {
    try {
      byte[] keyBytes = readPem(path, "PUBLIC KEY");
      X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
      KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
      return (EdECPublicKey) keyFactory.generatePublic(keySpec);
    } catch (Exception exception) {
      throw new IOException("Unable to read Ed25519 public key", exception);
    }
  }

  private void writePem(Path path, String type, byte[] encodedKey) throws IOException {
    String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
        .encodeToString(encodedKey);

    String pem = "-----BEGIN " + type + "-----\n"
        + base64
        + "\n-----END " + type + "-----\n";

    Files.writeString(path, pem, StandardCharsets.UTF_8);
  }

  private byte[] readPem(Path path, String type) throws IOException {
    String pem = Files.readString(path, StandardCharsets.UTF_8);

    String start = "-----BEGIN " + type + "-----";
    String end = "-----END " + type + "-----";

    String base64 = pem
        .replace(start, "")
        .replace(end, "")
        .replaceAll("\\s", "");

    return Base64.getDecoder().decode(base64);
  }
}