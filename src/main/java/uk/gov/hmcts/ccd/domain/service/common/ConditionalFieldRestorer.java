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
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.extractAccessProfileNames;

/**
 * The ConditionalFieldRestorer is responsible for evaluating and handling fields
 * that are missing from sanitized data based on field-level permissions.
 * <p>
 * It processes the data as follows:
 * - Identifies fields missing from the sanitized client request by comparing
 *   it with existing data.
 * - Checks whether the missing fields are excluded due to restricted field
 *   permissions (e.g., no Read permission but Create permission is allowed).
 * - Restores the missing fields to the sanitized data if the field permissions
 *   allow it (e.g., Create permission is granted even when Read is restricted).
 * - Ensures compliance with access control rules defined at the field level
 *   to maintain data integrity.
 * <p>
 * This class operates as an intermediate step in the data sanitization and
 * merge process, addressing incomplete field data while adhering to field-level
 * permission constraints.
 */
@Slf4j
@Service
public class ConditionalFieldRestorer {

    private static final String VALUE = "value";
    private static final String ID = "id";

    private final CaseAccessService caseAccessService;

    public ConditionalFieldRestorer(CaseAccessService caseAccessService) {
        this.caseAccessService = caseAccessService;
    }

    public Map<String, JsonNode> restoreConditionalFields(final CaseTypeDefinition caseTypeDefinition,
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

            if (existingValue == null) {
                return;
            }

            Optional<CaseFieldDefinition> optionalRootFieldDefinition = caseTypeDefinition.getCaseField(key);

            optionalRootFieldDefinition.ifPresent(rootFieldDefinition -> {
                if (rootFieldDefinition.isCompoundFieldType()) {
                    JsonNode updatedValue = processSubFieldsRecursively(
                        rootFieldDefinition,
                        sanitizedValue,
                        existingValue,
                        accessProfileNames
                    );

                    mergedData.put(key, updatedValue);
                }
            });
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

        ObjectNode sanitizedObjectNode = initializeSanitizedObjectNode(sanitizedNode);
        ObjectNode existingObjectNode = (ObjectNode) existingNode;

        existingObjectNode.fieldNames().forEachRemaining(fieldName -> {
            JsonNode existingSubField = existingObjectNode.get(fieldName);
            JsonNode sanitizedSubField = sanitizedObjectNode.get(fieldName);

            CaseFieldDefinition subFieldDefinition = parentFieldDefinition.getSubfieldDefinition(fieldName)
                .orElse(parentFieldDefinition);

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

    private static ObjectNode initializeSanitizedObjectNode(JsonNode sanitizedNode) {
        return sanitizedNode != null && sanitizedNode.isObject()
            ? (ObjectNode) sanitizedNode.deepCopy()
            : JsonNodeFactory.instance.objectNode();
    }

    private JsonNode processCollectionFields(CaseFieldDefinition subFieldDefinition,
                                             JsonNode sanitizedArrayNode,
                                             JsonNode existingArrayNode,
                                             Set<String> accessProfileNames) {
        ArrayNode sanitizedArray = initializeSanitizedArrayNode(sanitizedArrayNode);
        ArrayNode existingArray = (ArrayNode) existingArrayNode;

        for (JsonNode existingItem : existingArray) {
            processExistingItem(subFieldDefinition, sanitizedArray, existingItem, accessProfileNames);
        }

        return sanitizedArray;
    }

    private ArrayNode initializeSanitizedArrayNode(JsonNode sanitizedArrayNode) {
        return sanitizedArrayNode != null && sanitizedArrayNode.isArray()
            ? (ArrayNode) sanitizedArrayNode.deepCopy()
            : JsonNodeFactory.instance.arrayNode();
    }

    private void processExistingItem(CaseFieldDefinition subFieldDefinition,
                                     ArrayNode sanitizedArray,
                                     JsonNode existingItem,
                                     Set<String> accessProfileNames) {
        JsonNode existingItemId = existingItem.get(ID);

        Optional<JsonNode> matchingNewItem = findMatchingNewItem(sanitizedArray, existingItemId);

        if (matchingNewItem.isEmpty()) {
            handleMissingItem(subFieldDefinition, sanitizedArray, existingItem, accessProfileNames, existingItemId);
        } else {
            processMatchingItem(subFieldDefinition, matchingNewItem.get(), existingItem, accessProfileNames);
        }
    }

    private Optional<JsonNode> findMatchingNewItem(ArrayNode sanitizedArray, JsonNode existingItemId) {
        return StreamSupport.stream(sanitizedArray.spliterator(), false)
            .filter(newItem -> !isNullId(newItem) && newItem.get(ID).equals(existingItemId))
            .findFirst();
    }

    private void handleMissingItem(CaseFieldDefinition subFieldDefinition,
                                   ArrayNode sanitizedArray,
                                   JsonNode existingItem,
                                   Set<String> accessProfileNames,
                                   JsonNode existingItemId) {
        log.debug("Missing collection item with ID '{}' under '{}'.", existingItemId,
            subFieldDefinition.getId());

        if (isCreateWithoutReadAllowed(subFieldDefinition.getAccessControlLists(), accessProfileNames)) {
            log.info("Adding missing collection item with ID '{}' under '{}'.", existingItemId,
                subFieldDefinition.getId());
            sanitizedArray.add(existingItem);
        }
    }

    private void processMatchingItem(CaseFieldDefinition subFieldDefinition,
                                     JsonNode matchingNewItem,
                                     JsonNode existingItem,
                                     Set<String> accessProfileNames) {
        JsonNode newValueField = matchingNewItem.get(VALUE);
        JsonNode existingValueField = existingItem.get(VALUE);

        if (existingValueField != null) {
            JsonNode processedValueField = processValueField(subFieldDefinition, newValueField, existingValueField,
                accessProfileNames);
            ((ObjectNode) matchingNewItem).set(VALUE, processedValueField);
        }
    }

    private JsonNode processValueField(CaseFieldDefinition subFieldDefinition,
                                       JsonNode newValueField,
                                       JsonNode existingValueField,
                                       Set<String> accessProfileNames) {
        if (existingValueField.isObject()) {
            return processSubFieldsRecursively(subFieldDefinition,
                newValueField,
                existingValueField,
                accessProfileNames);
        } else {
            return processSimpleValueField(subFieldDefinition,
                newValueField,
                existingValueField,
                accessProfileNames);
        }
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
        return newItem.get(ID) == null
            || newItem.get(ID).equals(NullNode.getInstance())
            || "null".equalsIgnoreCase(newItem.get(ID).asText());
    }

    private boolean isCreateWithoutReadAllowed(List<AccessControlList> fieldAccessControlLists,
                                               Set<String> accessProfileNames) {
        boolean hasReadPermission = fieldAccessControlLists
            .stream()
            .anyMatch(acl -> accessProfileNames.contains(acl.getAccessProfile()) && acl.isRead());

        boolean hasCreatePermission = fieldAccessControlLists
            .stream()
            .anyMatch(acl -> accessProfileNames.contains(acl.getAccessProfile()) && acl.isCreate());

        return !hasReadPermission && hasCreatePermission;
    }
}
