package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.extractAccessProfileNames;

@Slf4j
@Service
public class RestrictedFieldProcessor {

    private final CaseAccessService caseAccessService;

    public RestrictedFieldProcessor(CaseAccessService caseAccessService) {
        this.caseAccessService = caseAccessService;
    }

    public Map<String, JsonNode> filterRestrictedFields(final CaseTypeDefinition caseTypeDefinition,
                                                         final Map<String, JsonNode> sanitisedData,
                                                         final Map<String, JsonNode> existingData,
                                                         final String caseReference) {
        Set<AccessProfile> accessProfiles = caseAccessService.getAccessProfilesByCaseReference(caseReference);
        if (accessProfiles == null || accessProfiles.isEmpty()) {
            throw new ValidationException("Cannot find user roles for the user");
        }

        final Set<String> accessProfileNames = extractAccessProfileNames(accessProfiles);
        final Map<String, JsonNode> mergedData = new HashMap<>(sanitisedData);

        sanitisedData.forEach((key, sanitizedValue) -> {
            JsonNode existingValue = existingData.get(key);

            if (existingValue != null) {
                CaseFieldDefinition rootFieldDefinition = caseTypeDefinition.getCaseFieldDefinitions()
                    .stream()
                    .filter(field -> field.getId().equals(key))
                    .findFirst()
                    .orElse(null);

                if (rootFieldDefinition != null && rootFieldDefinition.isCompoundFieldType()) {
                    JsonNode updatedValue = processSubFieldsRecursively(
                        rootFieldDefinition,
                        sanitizedValue,
                        existingValue,
                        accessProfileNames
                    );

                    mergedData.put(key, updatedValue);
                }
            }
        });

        return mergedData;
    }

    private JsonNode processSubFieldsRecursively(CaseFieldDefinition parentFieldDefinition,
                                                 JsonNode sanitizedNode,
                                                 JsonNode existingNode,
                                                 Set<String> accessProfileNames) {
        if (existingNode == null) {
            return sanitizedNode;
        }

        if (existingNode.isArray()) {
            return processCollectionFields(parentFieldDefinition, sanitizedNode, existingNode, accessProfileNames);
        }

        if (!existingNode.isObject()) {
            return sanitizedNode;
        }

        ObjectNode sanitizedObjectNode = sanitizedNode != null && sanitizedNode.isObject()
            ? (ObjectNode) sanitizedNode.deepCopy()
            : JsonNodeFactory.instance.objectNode();

        ObjectNode existingObjectNode = (ObjectNode) existingNode;

        existingObjectNode.fieldNames().forEachRemaining(fieldName -> {
            JsonNode existingSubField = existingObjectNode.get(fieldName);
            JsonNode sanitizedSubField = sanitizedObjectNode.get(fieldName);

            CaseFieldDefinition subFieldDefinition = getFieldDefinition(fieldName, parentFieldDefinition);

            if (sanitizedSubField == null) {
                log.debug("Missing field '{}' under '{}'.", fieldName, parentFieldDefinition.getId());

                if (isCreateWithoutReadAllowed(subFieldDefinition.getAccessControlLists(), accessProfileNames)) {
                    log.info("Adding missing field '{}' under '{}'.", fieldName, parentFieldDefinition.getId());
                    sanitizedObjectNode.set(fieldName, existingSubField);
                }
            } else {
                sanitizedObjectNode.set(fieldName, processSubFieldsRecursively(
                    subFieldDefinition,
                    sanitizedSubField,
                    existingSubField,
                    accessProfileNames));
            }
        });

        return sanitizedObjectNode;
    }

    private JsonNode processCollectionFields(CaseFieldDefinition subFieldDefinition,
                                             JsonNode sanitizedArrayNode,
                                             JsonNode existingArrayNode,
                                             Set<String> accessProfileNames) {

        ArrayNode sanitizedArray = sanitizedArrayNode != null && sanitizedArrayNode.isArray()
            ? (ArrayNode) sanitizedArrayNode.deepCopy()
            : JsonNodeFactory.instance.arrayNode();

        ArrayNode existingArray = (ArrayNode) existingArrayNode;

        for (JsonNode existingItem : existingArray) {
            JsonNode existingItemId = existingItem.get("id");

            Optional<JsonNode> matchingNewItem = StreamSupport.stream(sanitizedArray.spliterator(), false)
                .filter(newItem -> !isNullId(newItem) && newItem.get("id").equals(existingItemId))
                .findFirst();

            if (matchingNewItem.isEmpty()) {
                log.debug("Missing collection item with ID '{}' under '{}'.", existingItemId,
                    subFieldDefinition.getId());

                if (isCreateWithoutReadAllowed(subFieldDefinition.getAccessControlLists(), accessProfileNames)) {
                    log.info("Adding missing collection item with ID '{}' under '{}'.", existingItemId,
                        subFieldDefinition.getId());
                    sanitizedArray.add(existingItem);
                }
            } else {
                JsonNode newValueField = matchingNewItem.get().get("value");
                JsonNode existingValueField = existingItem.get("value");

                if (existingValueField != null) {
                    JsonNode processedValueField;

                    if (existingValueField.isObject()) {
                        processedValueField = processSubFieldsRecursively(subFieldDefinition,
                            newValueField,
                            existingValueField,
                            accessProfileNames);
                    } else {
                        processedValueField = processSimpleValueField(
                            subFieldDefinition, newValueField, existingValueField, accessProfileNames);
                    }

                    ((ObjectNode) matchingNewItem.get()).set("value", processedValueField);
                }
            }
        }

        return sanitizedArray;
    }

    private JsonNode processSimpleValueField(CaseFieldDefinition subFieldDefinition, JsonNode newValueField,
                                             JsonNode existingValueField,
                                             Set<String> accessProfileNames) {
        if (newValueField == null) {
            log.debug("Missing value field under '{}'.", subFieldDefinition.getId());

            if (isCreateWithoutReadAllowed(subFieldDefinition.getAccessControlLists(), accessProfileNames)) {
                log.info("Adding missing value field under '{}'.", subFieldDefinition.getId());
                return existingValueField;
            }
        }

        return newValueField != null ? newValueField : existingValueField;
    }

    private boolean isNullId(JsonNode newItem) {
        return newItem.get("id") == null
            || newItem.get("id").equals(NullNode.getInstance())
            || "null".equalsIgnoreCase(newItem.get("id").asText());
    }

    private boolean isCreateWithoutReadAllowed(List<AccessControlList> fieldAccessControlLists,
                                               Set<String> accessProfileNames) {
        boolean hasReadPermission = fieldAccessControlLists
            .stream()
            .anyMatch(acl -> accessProfileNames.contains(acl.getAccessProfile())
                && Boolean.TRUE.equals(acl.isRead()));

        boolean hasCreatePermission = fieldAccessControlLists
            .stream()
            .anyMatch(acl -> accessProfileNames.contains(acl.getAccessProfile())
                && Boolean.TRUE.equals(acl.isCreate()));

        return !hasReadPermission && hasCreatePermission;
    }

    private CaseFieldDefinition getFieldDefinition(String fieldName,
                                                   CaseFieldDefinition parentFieldDefinition) {
        // Check if the parent field's definition contains subfields
        FieldTypeDefinition parentFieldType = parentFieldDefinition.getFieldTypeDefinition();

        if (parentFieldType == null) {
            return parentFieldDefinition;
        }

        if (parentFieldType.getComplexFields() != null && !parentFieldType.getComplexFields().isEmpty()) {
            return parentFieldType.getComplexFields()
                .stream()
                .filter(subField -> subField.getId().equals(fieldName))
                .findFirst()
                .orElse(parentFieldDefinition);
        }

        if (parentFieldType.getCollectionFieldTypeDefinition() != null && !parentFieldType
            .getCollectionFieldTypeDefinition().getComplexFields().isEmpty()) {
            return parentFieldType.getCollectionFieldTypeDefinition().getComplexFields()
                .stream()
                .filter(subField -> subField.getId().equals(fieldName))
                .findFirst()
                .orElse(parentFieldDefinition);
        }

        return parentFieldDefinition;
    }
}
