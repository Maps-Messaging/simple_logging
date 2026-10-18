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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditManifest {

  private String manifestId;
  private Instant createdAt;
  private long firstSequenceNumber;
  private long lastSequenceNumber;
  private String firstEventHash;
  private String lastEventHash;
  private long eventCount;
  private String mapsBuild;
  private String translatorBuild;
  private List<AuditPayloadReference> journalFiles;
  private List<AuditPayloadReference> payloadFiles;
  private String signature;

  public List<AuditPayloadReference> getJournalFiles() {
    if (journalFiles == null) {
      journalFiles = new ArrayList<>();
    }
    return journalFiles;
  }

  public List<AuditPayloadReference> getPayloadFiles() {
    if (payloadFiles == null) {
      payloadFiles = new ArrayList<>();
    }
    return payloadFiles;
  }

  public JsonObject toCanonicalJsonObject() {
    JsonObject jsonObject = new JsonObject();

    jsonObject.addProperty("manifestId", emptyIfNull(manifestId));
    jsonObject.addProperty("createdAt", createdAt == null ? "" : createdAt.toString());
    jsonObject.addProperty("firstSequenceNumber", firstSequenceNumber);
    jsonObject.addProperty("lastSequenceNumber", lastSequenceNumber);
    jsonObject.addProperty("firstEventHash", emptyIfNull(firstEventHash));
    jsonObject.addProperty("lastEventHash", emptyIfNull(lastEventHash));
    jsonObject.addProperty("eventCount", eventCount);
    jsonObject.addProperty("mapsBuild", emptyIfNull(mapsBuild));
    jsonObject.addProperty("translatorBuild", emptyIfNull(translatorBuild));

    jsonObject.add("journalFiles", toPayloadArray(getJournalFiles()));
    jsonObject.add("payloadFiles", toPayloadArray(getPayloadFiles()));

    return jsonObject;
  }

  public JsonObject toSignedJsonObject() {
    JsonObject jsonObject = toCanonicalJsonObject();
    jsonObject.addProperty("signature", emptyIfNull(signature));
    return jsonObject;
  }

  private JsonArray toPayloadArray(List<AuditPayloadReference> payloadReferences) {
    JsonArray jsonArray = new JsonArray();

    for (AuditPayloadReference payloadReference : payloadReferences) {
      JsonObject payloadObject = new JsonObject();
      payloadObject.addProperty("name", emptyIfNull(payloadReference.getName()));
      payloadObject.addProperty("path", emptyIfNull(payloadReference.getPath()));
      payloadObject.addProperty("size", payloadReference.getSize());
      payloadObject.addProperty("sha256", emptyIfNull(payloadReference.getSha256()));
      jsonArray.add(payloadObject);
    }

    return jsonArray;
  }

  private String emptyIfNull(String value) {
    if (value == null) {
      return "";
    }
    return value;
  }
}