package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COMPLEX;

@Named
@Singleton
public class CaseDataValidator {
    private static final String EMPTY_STRING = "";
    private static final String FIELD_SEPARATOR = ".";

    private List<BaseTypeValidator> validators;

    @Inject
    public CaseDataValidator(final List<BaseTypeValidator> validators) {
        this.validators = validators;
    }

    public List<ValidationResult> validate(final Map<String, JsonNode> data,
                                           final List<CaseField> caseFieldDefinitions) {
        return validate(data, caseFieldDefinitions, CaseDataValidator.EMPTY_STRING);
    }

    public List<ValidationResult> validate(final Map<String, JsonNode> data,
                                           final List<CaseField> caseFieldDefinitions,
                                           final String fieldIdPrefix) {
        return (data == null) ? new ArrayList<>() :
            data.entrySet().stream()
                .map(caseDataPair -> caseFieldDefinitions.stream()
                    .filter(caseField -> caseField.getId().equalsIgnoreCase(caseDataPair.getKey()))
                    .findAny()
                    .map(caseField -> validateField(caseDataPair.getKey(), caseDataPair.getValue(), caseField, fieldIdPrefix))
                    .orElseGet(() -> Collections.singletonList(
                        new ValidationResult("Field is not recognised", fieldIdPrefix + caseDataPair.getKey()))))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<ValidationResult> validateField(final String dataFieldId,
                                                 final JsonNode dataValue,
                                                 final CaseField caseFieldDefinition,
                                                 final String fieldIdPrefix) {
        final String caseFieldType = caseFieldDefinition.getFieldType().getType();

        if (!BaseType.contains(caseFieldType)) {
            return Collections.singletonList(new ValidationResult("Unknown Type:" + caseFieldType, dataFieldId));
        }

        final BaseType fieldType = BaseType.get(caseFieldType);

        if (BaseType.get(COMPLEX) == fieldType) {
            return validate(
                JacksonUtils.convertValue(dataValue),
                caseFieldDefinition.getFieldType().getComplexFields(),
                fieldIdPrefix + dataFieldId + FIELD_SEPARATOR);
        } else if (BaseType.get(COLLECTION) == fieldType) {
            final List<ValidationResult> validationResults = validateSimpleField(dataFieldId, dataValue, caseFieldDefinition, fieldIdPrefix, fieldType);
            final Iterator<JsonNode> collectionIterator = dataValue.iterator();

            Integer index = 0;
            while (collectionIterator.hasNext()) {
                final JsonNode itemValue = collectionIterator.next();

                validationResults.addAll(validateCollectionItem(caseFieldDefinition.getFieldType().getCollectionFieldType(),
                    itemValue,
                    fieldIdPrefix + dataFieldId + FIELD_SEPARATOR,
                    index.toString())
                );

                index++;
            }
            return validationResults;
        } else {
            return validateSimpleField(dataFieldId, dataValue, caseFieldDefinition, fieldIdPrefix, fieldType);
        }
    }

    private List<ValidationResult> validateSimpleField(final String dataFieldId,
                                                       final JsonNode dataValue,
                                                       final CaseField caseFieldDefinition,
                                                       final String fieldIdPrefix,
                                                       final BaseType fieldType) {
        return validators.stream()
            .filter(validator -> validator.getType() == fieldType)
            .findAny()
            .map(baseTypeValidator -> baseTypeValidator
                .validate(dataFieldId, dataValue, caseFieldDefinition)
                .stream()
                .map(result -> new ValidationResult(result.getErrorMessage(), fieldIdPrefix + result.getFieldId()))
                .collect(Collectors.toList()))
            .orElseThrow(() -> new RuntimeException("System error: No validator found for " + fieldType.getType()));
    }

    private List<ValidationResult> validateCollectionItem(FieldType fieldType, JsonNode item, String fieldIdPrefix, String index) {
        final String itemFieldId = fieldIdPrefix + index;

        final JsonNode itemValue = item.get(CollectionValidator.VALUE);

        if (null == itemValue) {
            return Collections.emptyList();
        }

        if (shouldTreatAsValueNode(fieldType, itemValue)) {
            if (!BaseType.contains(fieldType.getType())) {
                return Collections.singletonList(new ValidationResult("Unknown Type:" + fieldType.getType(), itemFieldId));
            }

            final BaseType baseType = BaseType.get(fieldType.getType());

            final CaseField caseField = new CaseField();
            caseField.setFieldType(fieldType);
            caseField.setId(index);
            return validateSimpleField(index, itemValue, caseField, fieldIdPrefix, baseType);
        } else if (itemValue.isObject()) {
            return validate(
                JacksonUtils.convertValue(itemValue),
                fieldType.getComplexFields(),
                itemFieldId + FIELD_SEPARATOR);
        }

        return Collections.singletonList(new ValidationResult("Unsupported collection item:" + itemValue.toString(), itemFieldId));
    }

    private boolean shouldTreatAsValueNode(FieldType fieldType, JsonNode itemValue) {
        return itemValue.isValueNode() || fieldType.getType().equalsIgnoreCase(DocumentValidator.TYPE_ID);
    }
}

