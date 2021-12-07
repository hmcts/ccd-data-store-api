package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;

@Named
@Singleton
public class CaseLinkValidator {
    private static final String EMPTY_STRING = "";
    private static final String FIELD_SEPARATOR = ".";
    private static final String CASE_LINK = "CaseLink";

    private List<FieldValidator> validators;

    public void validate(final ValidationContext validationContext) {
        validate(
            validationContext.getData(),
            validationContext.getCaseTypeDefinition().getCaseFieldDefinitions(),
            CaseLinkValidator.EMPTY_STRING, validationContext);
    }

    public void validate(final Map<String, JsonNode> data,
                                           final List<CaseFieldDefinition> caseFieldDefinitions,
                                           final String fieldIdPrefix,
                                           final ValidationContext validationContext) {
        data.entrySet().stream()
            .forEach(caseDataPair -> caseFieldDefinitions.stream()
                .filter(caseField -> caseField.getId().equalsIgnoreCase(caseDataPair.getKey()))
                .forEach(caseField -> validateField(
                    caseDataPair.getKey(),
                    caseDataPair.getValue(),
                    caseField,
                    fieldIdPrefix,
                    validationContext
                )));
    }

    private void validateField(final String dataFieldId,
                               final JsonNode dataValue,
                               final CaseFieldDefinition caseFieldDefinition,
                               final String fieldIdPrefix,
                               final ValidationContext validationContext) {
        final String caseFieldType = caseFieldDefinition.getFieldTypeDefinition().getType();

        if (!BaseType.contains(caseFieldType)) {
            //TODO throw error
            // return Collections.singletonList(new ValidationResult("Unknown Type:" + caseFieldType, dataFieldId));
        }

        final BaseType fieldType = BaseType.get(caseFieldType);

        if (BaseType.get(COMPLEX) == fieldType) {
            validate(
                JacksonUtils.convertValue(dataValue),
                caseFieldDefinition.getFieldTypeDefinition().getComplexFields(),
                fieldIdPrefix + dataFieldId + FIELD_SEPARATOR, validationContext
            );
        } else if (BaseType.get(COLLECTION) == fieldType) {
            validateSimpleField(
                dataFieldId, dataValue,
                caseFieldDefinition,
                fieldIdPrefix,
                fieldType,
                validationContext
            );
            final Iterator<JsonNode> collectionIterator = dataValue.iterator();

            Integer index = 0;
            while (collectionIterator.hasNext()) {
                final JsonNode itemValue = collectionIterator.next();
                validateCollectionItem(
                    caseFieldDefinition.getFieldTypeDefinition().getCollectionFieldTypeDefinition(),
                    itemValue,
                    fieldIdPrefix + dataFieldId + FIELD_SEPARATOR, index.toString(),
                    validationContext
                );
                index++;
            }
        } else {
            validateSimpleField(
                dataFieldId,
                dataValue,
                caseFieldDefinition,
                fieldIdPrefix,
                fieldType,
                validationContext
            );
        }
    }

    private void validateSimpleField(final String fieldId,
                                     final JsonNode dataValue,
                                     final CaseFieldDefinition caseFieldDefinition,
                                     final String fieldIdPrefix,
                                     final BaseType fieldType,
                                     final ValidationContext validationContext) {
        //TODO

        if (caseFieldDefinition.getFieldTypeDefinition().getType().equals(CASE_LINK)) {
            //Does it exist in db if yes delete and insert else insert

        }
    }

    private List<ValidationResult> validateCollectionItem(
        FieldTypeDefinition fieldTypeDefinition,
        JsonNode item,
        String fieldIdPrefix,
        String index,
        ValidationContext validationContext
    ) {

        //TODO
        final String itemFieldId = fieldIdPrefix + index;

        final JsonNode itemValue = item.get(CollectionValidator.VALUE);

        if (null == itemValue) {
            return Collections.emptyList();
        }
        if (shouldTreatAsValueNode(fieldTypeDefinition, itemValue)) {
            if (!BaseType.contains(fieldTypeDefinition.getType())) {
                return Collections.singletonList(
                    new ValidationResult("Unknown Type:" + fieldTypeDefinition.getType(), itemFieldId)
                );
            }
            final BaseType baseType = BaseType.get(fieldTypeDefinition.getType());
            final CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
            caseFieldDefinition.setFieldTypeDefinition(fieldTypeDefinition);
            caseFieldDefinition.setId(index);
            validateSimpleField(
                index,
                itemValue,
                caseFieldDefinition,
                fieldIdPrefix,
                baseType,
                validationContext
            );
        } else if (itemValue.isObject()) {
            validate(
                JacksonUtils.convertValue(itemValue),
                fieldTypeDefinition.getComplexFields(),
                itemFieldId + FIELD_SEPARATOR, validationContext);
        }

        return Collections.singletonList(new ValidationResult("Unsupported collection item:" + itemValue
            .toString(), itemFieldId));
    }

    private boolean shouldTreatAsValueNode(FieldTypeDefinition fieldTypeDefinition, JsonNode itemValue) {
        return itemValue.isValueNode()
            || fieldTypeDefinition.getType().equalsIgnoreCase(DocumentValidator.TYPE_ID)
            || isDynamicListNode(fieldTypeDefinition);
    }

    private boolean isDynamicListNode(FieldTypeDefinition fieldTypeDefinition) {
        return fieldTypeDefinition.getType().equalsIgnoreCase(DynamicListValidator.TYPE_ID)
            || fieldTypeDefinition.getType().equalsIgnoreCase(DynamicMultiSelectListValidator.TYPE_ID)
            || fieldTypeDefinition.getType().equalsIgnoreCase(DynamicRadioListValidator.TYPE_ID);
    }
}

