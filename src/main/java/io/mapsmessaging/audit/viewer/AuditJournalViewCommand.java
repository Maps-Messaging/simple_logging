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

package io.mapsmessaging.audit.viewer;

import io.mapsmessaging.audit.AuditKeyUtils;
import java.nio.file.Path;
import java.security.interfaces.EdECPublicKey;
import java.util.List;

public class AuditJournalViewCommand {

  public static void main(String[] args) throws Exception {
    if (args.length != 1 && args.length != 3) {
      printUsage();
      return;
    }

    Path journalPath = Path.of(args[0]);
    EdECPublicKey publicKey = null;

    if (args.length == 3) {
      if (!"--public-key".equals(args[1])) {
        printUsage();
        return;
      }

      AuditKeyUtils auditKeyUtils = new AuditKeyUtils();
      publicKey = auditKeyUtils.readPublicKey(Path.of(args[2]));
    }

    AuditJournalViewer auditJournalViewer = new AuditJournalViewer(publicKey);
    List<AuditRecordView> records = auditJournalViewer.readAndVerify(journalPath);

    AuditJournalConsolePrinter auditJournalConsolePrinter = new AuditJournalConsolePrinter();
    auditJournalConsolePrinter.print(records);
  }

  private static void printUsage() {
    System.out.println("Usage:");
    System.out.println("  maps-audit-view <journal.jsonl>");
    System.out.println("  maps-audit-view <journal.jsonl> --public-key <audit-public-key.pem>");
  }
}