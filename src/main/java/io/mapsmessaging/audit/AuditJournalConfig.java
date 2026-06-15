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

import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.interfaces.EdECPublicKey;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditJournalConfig {

  private static final long DEFAULT_MAX_JOURNAL_SIZE_BYTES = 64L * 1024L * 1024L;

  private final Path journalRoot;
  private final PrivateKey signingKey;
  private final EdECPublicKey verificationKey;

  @Builder.Default
  private final long maxJournalSizeBytes = DEFAULT_MAX_JOURNAL_SIZE_BYTES;

  @Builder.Default
  private final boolean rotateDaily = true;

  @Builder.Default
  private final boolean failOnInvalidExistingJournal = true;
}