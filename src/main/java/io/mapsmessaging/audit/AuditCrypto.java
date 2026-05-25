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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.EdECPublicKey;
import java.util.Base64;

public class AuditCrypto {

  public String sha256Hex(byte[] data) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      byte[] digest = messageDigest.digest(data);
      return toHex(digest);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to calculate SHA-256", exception);
    }
  }

  public String sha256Hex(String value) {
    return sha256Hex(value.getBytes(StandardCharsets.UTF_8));
  }

  public String signBase64(PrivateKey privateKey, String value) {
    try {
      Signature signature = Signature.getInstance("Ed25519");
      signature.initSign(privateKey);
      signature.update(value.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(signature.sign());
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to sign audit data", exception);
    }
  }

  public boolean verifySignature(EdECPublicKey publicKey, String value, String signatureBase64) {
    try {
      Signature signature = Signature.getInstance("Ed25519");
      signature.initVerify(publicKey);
      signature.update(value.getBytes(StandardCharsets.UTF_8));
      byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
      return signature.verify(signatureBytes);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to verify audit signature", exception);
    }
  }

  private String toHex(byte[] bytes) {
    StringBuilder stringBuilder = new StringBuilder(bytes.length * 2);

    for (byte value : bytes) {
      stringBuilder.append(Character.forDigit((value >> 4) & 0x0f, 16));
      stringBuilder.append(Character.forDigit(value & 0x0f, 16));
    }

    return stringBuilder.toString();
  }
}