package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_DELETE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.VALUE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.hasAccessControlList;

@Slf4j
@Service
public class DeleteAccessControlService {

    public static final String NO_DELETE_ACL_LOG = "A child '{}' of '{}' has been deleted but has no Delete ACL";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean canDeleteCaseFields(final JsonNode newData,
                                       final JsonNode existingData,
                                       final CaseFieldDefinition caseFieldDefinition,
                                       final Set<AccessProfile> accessProfiles) {
        return checkDeleteAccessForField(existingData, newData, caseFieldDefinition, accessProfiles);
    }

    private boolean checkDeleteAccessForField(JsonNode existingData,
                                              JsonNode newData,
                                              CaseFieldDefinition caseFieldDefinition,
                                              Set<AccessProfile> accessProfiles) {
        String fieldDefinitionId = caseFieldDefinition.getId();

        JsonNode existingDataFiltered = filterData(existingData, caseFieldDefinition, fieldDefinitionId);
        JsonNode newDataFiltered = filterData(newData, caseFieldDefinition, fieldDefinitionId);

        if (hasDataChanged(existingDataFiltered, newDataFiltered)) {
            return verifyDeleteAccessForDeletedField(caseFieldDefinition, accessProfiles, existingDataFiltered,
                newDataFiltered);
        }

        return true;
    }

    private JsonNode filterData(JsonNode data, CaseFieldDefinition caseFieldDefinition, String fieldDefinitionId) {
        return caseFieldDefinition.isCollectionFieldType() ? wrapInRootNode(data, fieldDefinitionId) :
            data.get(fieldDefinitionId);
    }

    private JsonNode wrapInRootNode(JsonNode data, String fieldId) {
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.set(fieldId, data.get(fieldId));
        return rootNode;
    }

    private boolean hasDataChanged(JsonNode existingData, JsonNode newData) {
        return existingData != null && !existingData.isEmpty() && newData != null && !newData.isEmpty();
    }

    private boolean verifyDeleteAccessForDeletedField(CaseFieldDefinition caseFieldDefinition,
                                                      Set<AccessProfile> accessProfiles,
                                                      JsonNode existingDataFiltered,
                                                      JsonNode newDataFiltered) {
        Optional<String> deletedFieldPath = findDeletedFieldPath("", existingDataFiltered, newDataFiltered);

        if (deletedFieldPath.isPresent()) {
            CaseFieldDefinition caseField = getFieldDefinitionByPath(caseFieldDefinition, deletedFieldPath.get())
                .orElseThrow(() -> new ResourceNotFoundException("Field definition not found for path: '"
                    + deletedFieldPath.get() + "' within the case field definition."));

            if (!hasAccessControlList(accessProfiles, caseField.getAccessControlLists(), CAN_DELETE)) {
                log.info(NO_DELETE_ACL_LOG, caseField.getId(), caseFieldDefinition.getId());
                return false;
            }
        }

        return true;
    }

    private Optional<String> findDeletedFieldPath(final String rootFieldName, final JsonNode existingData,
                                                  final JsonNode newData) {
        if (existingData == null || newData == null || newData.isNull()) {
            log.debug("Missing nodes. ExistingData: {}, NewData: {}", existingData, newData);
            return Optional.of(rootFieldName);
        }

        if (existingData.equals(newData)) {
            return Optional.empty();
        }

        for (Iterator<String> fieldNames = existingData.fieldNames(); fieldNames.hasNext(); ) {
            String fieldName = fieldNames.next();
            JsonNode existingFieldNode = existingData.get(fieldName);
            JsonNode newFieldNode = newData.get(fieldName);

            if (newFieldNode == null || newFieldNode.isNull()) {
                log.debug("Field '{}' is missing or null in new data but exists in existing data.", fieldName);
                return Optional.of(rootFieldName + "." + fieldName);
            }

            if (existingFieldNode.isObject() && newFieldNode.isObject()) {
                Optional<String> nestedFieldPath = findDeletedFieldPath(rootFieldName + "." + fieldName,
                    existingFieldNode, newFieldNode);
                if (nestedFieldPath.isPresent()) {
                    return nestedFieldPath;
                }
            } else if (existingFieldNode.isArray() && newFieldNode.isArray()) {
                Optional<String> deletedPath = findDeletedCollectionItemPath(rootFieldName, fieldName,
                    existingFieldNode, newFieldNode);
                if (deletedPath.isPresent()) {
                    return deletedPath;
                }
            }
        }

        return Optional.empty();
    }

    private Optional<String> findDeletedCollectionItemPath(String rootFieldName, String fieldName,
                                                           JsonNode existingFieldNode, JsonNode newFieldNode) {
        for (JsonNode existingItem : existingFieldNode) {
            JsonNode existingItemId = existingItem.get("id");

            Optional<JsonNode> matchingNewItem = StreamSupport.stream(newFieldNode.spliterator(), false)
                .filter(newItem -> !isNullId(newItem) && newItem.get("id").equals(existingItemId))
                .findFirst();

            if (matchingNewItem.isEmpty()) {
                log.debug("Deleted collection item with ID '{}' under '{}'.", existingItemId,
                    rootFieldName + "." + fieldName);
                return Optional.of(rootFieldName + "." + fieldName);
            }

            Optional<String> nestedPath = findDeletedFieldPath(rootFieldName + "." + fieldName,
                existingItem.get(VALUE), matchingNewItem.get().get(VALUE));
            if (nestedPath.isPresent()) {
                return nestedPath;
            }
        }
        return Optional.empty();
    }

    private Optional<CaseFieldDefinition> getFieldDefinitionByPath(final CaseFieldDefinition rootField,
                                                                   final String fieldPath) {
        List<String> fieldIds = Arrays.stream(fieldPath.split("\\."))
            .filter(s -> !s.isEmpty())
            .toList();

        // If the root field is a collection with a single item matching the root field type, return rootFieldType
        // If the root field is a collection with a multiple item matching the root field type, drop the first item
        if (rootField.isCollectionFieldType() && !fieldIds.isEmpty() && fieldIds.getFirst().equals(rootField.getId())) {
            if (fieldIds.size() == 1) {
                return Optional.of(rootField);
            } else {
                fieldIds = fieldIds.subList(1, fieldIds.size());
            }
        }

        CaseFieldDefinition currentField = rootField;
        for (String fieldId : fieldIds) {

            //return the current field as the target field is one of the based types and any sub-fields inherits ACls
            if (!currentField.isCompoundFieldType() || isFieldWithoutSubfields(currentField)) {
                log.debug("Returning current field '{}', as target field {} is a type which does not contain "
                    + "sub-fields", currentField.getId(), fieldId);
                return Optional.of(currentField);
            }

            currentField = findFieldInDefinition(currentField, fieldId);
            if (currentField == null) {
                return Optional.empty();
            }
        }

        return Optional.of(currentField);
    }

    private boolean isFieldWithoutSubfields(CaseFieldDefinition currentField) {
        return (currentField.getFieldTypeDefinition().getComplexFields() == null
            || currentField.getFieldTypeDefinition().getComplexFields().isEmpty())
            && (currentField.getFieldTypeDefinition().getCollectionFieldTypeDefinition() == null
            || currentField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getComplexFields().isEmpty());
    }

    private CaseFieldDefinition findFieldInDefinition(CaseFieldDefinition parentField, String fieldId) {
        List<CaseFieldDefinition> fieldDefinitions = getFieldDefinitions(parentField);
        if (fieldDefinitions == null || fieldDefinitions.isEmpty()) {
            return null;
        }

        return fieldDefinitions.stream()
            .filter(field -> field.getId().equals(fieldId))
            .findFirst()
            .orElse(null);
    }

    private List<CaseFieldDefinition> getFieldDefinitions(CaseFieldDefinition currentFieldType) {
        List<CaseFieldDefinition> fieldDefinitions = null;

        if (currentFieldType.isComplexFieldType()) {
            fieldDefinitions = currentFieldType.getFieldTypeDefinition().getComplexFields();
        } else if (currentFieldType.isCollectionFieldType()) {
            fieldDefinitions =
                currentFieldType.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getComplexFields();
        }

        return fieldDefinitions;
    }

    private boolean isNullId(JsonNode newItem) {
        return newItem.get("id") == null
            || newItem.get("id").equals(NullNode.getInstance())
            || "null".equalsIgnoreCase(newItem.get("id").asText());
    }
}
