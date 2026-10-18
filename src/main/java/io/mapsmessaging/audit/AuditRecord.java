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
import io.mapsmessaging.logging.LogMessage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
public class AuditRecord {

  private long sequenceNumber;
  private String auditId;
  private String correlationId;
  private String parentCorrelationId;
  private Instant timestamp;

  private String actor;
  private String actorType;
  private String source;
  private String destination;
  private String subject;
  private String action;
  private AuditOutcome outcome;

  private String messageCode;
  private String message;
  private String level;
  private String categoryDivision;
  private String categoryDescription;

  private List<String> parameters;
  private Map<String, String> attributes;
  private List<AuditPayloadReference> payloadReferences;

  private String previousRecordHash;
  private String recordHash;
  private String signature;

  public List<String> getParameters() {
    if (parameters == null) {
      parameters = new ArrayList<>();
    }
    return parameters;
  }

  public Map<String, String> getAttributes() {
    if (attributes == null) {
      attributes = new LinkedHashMap<>();
    }
    return attributes;
  }

  public List<AuditPayloadReference> getPayloadReferences() {
    if (payloadReferences == null) {
      payloadReferences = new ArrayList<>();
    }
    return payloadReferences;
  }

  public static AuditRecord from(
      LogMessage logMessage,
      AuditContext auditContext,
      List<AuditPayloadReference> payloadReferences,
      Object... parameters
  ) {
    AuditRecord auditRecord = new AuditRecord();

    if (auditContext != null) {
      auditRecord.setAuditId(auditContext.getAuditId());
      auditRecord.setCorrelationId(auditContext.getCorrelationId());
      auditRecord.setParentCorrelationId(auditContext.getParentCorrelationId());
      auditRecord.setTimestamp(auditContext.getTimestamp());
      auditRecord.setActor(auditContext.getActor());
      auditRecord.setActorType(auditContext.getActorType());
      auditRecord.setSource(auditContext.getSource());
      auditRecord.setDestination(auditContext.getDestination());
      auditRecord.setSubject(auditContext.getSubject());
      auditRecord.setAction(auditContext.getAction());
      auditRecord.setOutcome(auditContext.getOutcome());
      auditRecord.getAttributes().putAll(auditContext.getAttributes());
    }

    if (auditRecord.getTimestamp() == null) {
      auditRecord.setTimestamp(Instant.now());
    }

    if (auditRecord.getOutcome() == null) {
      auditRecord.setOutcome(AuditOutcome.UNKNOWN);
    }

    auditRecord.setMessageCode(resolveMessageCode(logMessage));
    auditRecord.setMessage(logMessage.getMessage());
    auditRecord.setLevel(logMessage.getLevel().name());
    auditRecord.setCategoryDivision(logMessage.getCategory().getDivision());
    auditRecord.setCategoryDescription(logMessage.getCategory().getDescription());

    if (parameters != null) {
      for (Object parameter : parameters) {
        auditRecord.getParameters().add(parameter == null ? "" : String.valueOf(parameter));
      }
    }

    if (payloadReferences != null) {
      auditRecord.getPayloadReferences().addAll(payloadReferences);
    }

    return auditRecord;
  }

  public JsonObject toCanonicalJsonObject() {
    JsonObject jsonObject = new JsonObject();

    jsonObject.addProperty("sequenceNumber", sequenceNumber);
    jsonObject.addProperty("auditId", emptyIfNull(auditId));
    jsonObject.addProperty("correlationId", emptyIfNull(correlationId));
    jsonObject.addProperty("parentCorrelationId", emptyIfNull(parentCorrelationId));
    jsonObject.addProperty("timestamp", timestamp == null ? "" : timestamp.toString());

    jsonObject.addProperty("actor", emptyIfNull(actor));
    jsonObject.addProperty("actorType", emptyIfNull(actorType));
    jsonObject.addProperty("source", emptyIfNull(source));
    jsonObject.addProperty("destination", emptyIfNull(destination));
    jsonObject.addProperty("subject", emptyIfNull(subject));
    jsonObject.addProperty("action", emptyIfNull(action));
    jsonObject.addProperty("outcome", outcome == null ? "" : outcome.name());

    jsonObject.addProperty("messageCode", emptyIfNull(messageCode));
    jsonObject.addProperty("message", emptyIfNull(message));
    jsonObject.addProperty("level", emptyIfNull(level));
    jsonObject.addProperty("categoryDivision", emptyIfNull(categoryDivision));
    jsonObject.addProperty("categoryDescription", emptyIfNull(categoryDescription));

    JsonArray parameterArray = new JsonArray();
    for (String parameter : getParameters()) {
      parameterArray.add(emptyIfNull(parameter));
    }
    jsonObject.add("parameters", parameterArray);

    JsonObject attributeObject = new JsonObject();
    for (Map.Entry<String, String> entry : getAttributes().entrySet()) {
      attributeObject.addProperty(entry.getKey(), emptyIfNull(entry.getValue()));
    }
    jsonObject.add("attributes", attributeObject);

    JsonArray payloadArray = new JsonArray();
    for (AuditPayloadReference payloadReference : getPayloadReferences()) {
      JsonObject payloadObject = new JsonObject();
      payloadObject.addProperty("name", emptyIfNull(payloadReference.getName()));
      payloadObject.addProperty("path", emptyIfNull(payloadReference.getPath()));
      payloadObject.addProperty("size", payloadReference.getSize());
      payloadObject.addProperty("sha256", emptyIfNull(payloadReference.getSha256()));
      payloadObject.addProperty("contentType", emptyIfNull(payloadReference.getContentType()));
      payloadArray.add(payloadObject);
    }
    jsonObject.add("payloadReferences", payloadArray);

    jsonObject.addProperty("previousRecordHash", emptyIfNull(previousRecordHash));

    return jsonObject;
  }

  public JsonObject toJournalJsonObject() {
    JsonObject jsonObject = toCanonicalJsonObject();
    jsonObject.addProperty("recordHash", emptyIfNull(recordHash));
    jsonObject.addProperty("signature", emptyIfNull(signature));
    return jsonObject;
  }

  private static String resolveMessageCode(LogMessage logMessage) {
    if (logMessage instanceof Enum<?> enumValue) {
      return enumValue.getClass().getSimpleName() + "." + enumValue.name();
    }

    return logMessage.getClass().getName();
  }

  private String emptyIfNull(String value) {
    if (value == null) {
      return "";
    }

    return value;
  }
}