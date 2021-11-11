package uk.gov.hmcts.ccd.domain.service.casedeletion;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.types.BaseType;
import uk.gov.hmcts.ccd.domain.types.CollectionValidator;
import uk.gov.hmcts.ccd.domain.types.DocumentValidator;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;

@Named
@Singleton
@Slf4j
public class CaseDataExtractor {
    private static final String EMPTY_STRING = "";
    private static final String FIELD_SEPARATOR = ".";

    public List<String> extractFieldTypePaths(final Map<String, JsonNode> data,
                                              final List<CaseFieldDefinition> caseFieldDefinitions,
                                              final String fieldType) {
        return extractFieldTypePaths(
            data,
            caseFieldDefinitions,
            CaseDataExtractor.EMPTY_STRING,
            new ArrayList<>(),
            fieldType);
    }

    public List<String> extractFieldTypePaths(final Map<String, JsonNode> data,
                                              final List<CaseFieldDefinition> caseFieldDefinitions,
                                              final String fieldIdPrefix,
                                              List<String> paths,
                                              final String fieldType) {
        return (data == null)
            ? new ArrayList<>()
            : data.entrySet().stream()
                .map(caseDataPair -> caseFieldDefinitions.stream()
                    .filter(caseField -> caseField.getId().equalsIgnoreCase(caseDataPair.getKey()))
                    .findAny()
                    .map(caseField -> extractField(
                        caseDataPair.getKey(),
                        caseDataPair.getValue(),
                        caseField,
                        fieldIdPrefix,
                        paths,fieldType
                    ))
                    .orElseGet(Collections::emptyList))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    private List<String> extractField(final String dataFieldId,
                                                final JsonNode dataValue,
                                                final CaseFieldDefinition caseFieldDefinition,
                                                final String fieldIdPrefix,
                                                List<String> paths,
                                                String fieldType) {
        final String caseFieldType = caseFieldDefinition.getFieldTypeDefinition().getType();

        if (!BaseType.contains(caseFieldType)) {
            log.debug("Ignoring Unknown Type: " + caseFieldType);
            return Collections.emptyList();
        }

        final BaseType baseFieldType = BaseType.get(caseFieldType);

        if (BaseType.get(COMPLEX) == baseFieldType) {
            return extractFieldTypePaths(
                JacksonUtils.convertValue(dataValue),
                caseFieldDefinition.getFieldTypeDefinition().getComplexFields(),
                fieldIdPrefix + dataFieldId + FIELD_SEPARATOR, paths, fieldType
            );
        } else if (BaseType.get(COLLECTION) == baseFieldType) {
            final List<String> extractionResults =
                extractSimpleField(
                    dataFieldId,
                    caseFieldDefinition,
                    fieldIdPrefix,
                    fieldType,
                    paths
                );
            final Iterator<JsonNode> collectionIterator = dataValue.iterator();

            int index = 0;
            while (collectionIterator.hasNext()) {
                final JsonNode itemValue = collectionIterator.next();
                extractionResults.addAll(
                    extractCollectionItem(
                        caseFieldDefinition.getFieldTypeDefinition().getCollectionFieldTypeDefinition(),
                        itemValue,
                        fieldIdPrefix + dataFieldId + FIELD_SEPARATOR, Integer.toString(index),
                        paths,
                        fieldType
                    )
                );
                index++;
            }
            return extractionResults;
        } else {
            return extractSimpleField(
                                        dataFieldId,
                                        caseFieldDefinition,
                                        fieldIdPrefix,
                                        fieldType,
                                        paths
            );
        }
    }

    private List<String> extractSimpleField(final String fieldId,
                                                      final CaseFieldDefinition caseFieldDefinition,
                                                      final String fieldIdPrefix,
                                                      final String fieldType,
                                                      List<String> paths) {
        List<String> returnValue = new ArrayList<>(paths);
        FieldTypeDefinition fieldTypeDefinition = caseFieldDefinition.getFieldTypeDefinition();

        if (fieldTypeDefinition.getId() != null && caseFieldDefinition.getFieldTypeDefinition().getId()
            .equals(fieldType)) {
            returnValue.add(fieldIdPrefix + fieldId);
        }

        return returnValue;
    }

    private List<String> extractCollectionItem(FieldTypeDefinition fieldTypeDefinition,
                                               JsonNode item,
                                               String fieldIdPrefix,
                                               String index,
                                               List<String> paths,
                                               String fieldType) {
        final String itemFieldId = fieldIdPrefix + index;

        final JsonNode itemValue = item.get(CollectionValidator.VALUE);

        if (null == itemValue) {
            return Collections.emptyList();
        }

        if (shouldTreatAsValueNode(fieldTypeDefinition, itemValue)) {
            if (!BaseType.contains(fieldTypeDefinition.getType())) {
                log.debug("Ignoring Unknown Type:" + fieldTypeDefinition.getType() + " " + itemFieldId);
                return Collections.emptyList();
            }
            final CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
            caseFieldDefinition.setFieldTypeDefinition(fieldTypeDefinition);
            caseFieldDefinition.setId(index);
            return extractSimpleField(index,
                                        caseFieldDefinition,
                                        fieldIdPrefix,
                                        fieldType,
                                        paths
            );
        } else if (itemValue.isObject()) {
            return extractFieldTypePaths(
                JacksonUtils.convertValue(itemValue),
                fieldTypeDefinition.getComplexFields(),
                itemFieldId + FIELD_SEPARATOR, paths, fieldType);
        }

        return Collections.emptyList();
    }

    private boolean shouldTreatAsValueNode(FieldTypeDefinition fieldTypeDefinition, JsonNode itemValue) {
        return itemValue.isValueNode()
            || fieldTypeDefinition.getType().equalsIgnoreCase(DocumentValidator.TYPE_ID)
            || isDynamicListNode(fieldTypeDefinition);
    }

    private boolean isDynamicListNode(FieldTypeDefinition fieldTypeDefinition) {
        return fieldTypeDefinition.getType().equalsIgnoreCase("DynamicList")
            || fieldTypeDefinition.getType().equalsIgnoreCase("DynamicMultiSelectList")
            || fieldTypeDefinition.getType().equalsIgnoreCase("DynamicRadioList");
    }
}

