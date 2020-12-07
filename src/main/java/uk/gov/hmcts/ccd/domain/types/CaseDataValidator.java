package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;

@Named
@Singleton
public class CaseDataValidator {
    private static final String EMPTY_STRING = "";
    private static final String FIELD_SEPARATOR = ".";

    private List<FieldValidator> validators;

    @Inject
    public CaseDataValidator(final List<FieldValidator> validators) {
        this.validators = validators;
    }

    public List<ValidationResult> validate(final ValidationContext validationContext) {
        return validate(
            validationContext.getData(),
            validationContext.getCaseTypeDefinition().getCaseFieldDefinitions(),
            CaseDataValidator.EMPTY_STRING, validationContext)
            ;
    }

    public List<ValidationResult> validate(final Map<String, JsonNode> data,
                                           final List<CaseFieldDefinition> caseFieldDefinitions,
                                           final String fieldIdPrefix,
                                           final ValidationContext validationContext) {
        return (data == null)
            ? new ArrayList<>()
            : data.entrySet().stream()
                .map(caseDataPair -> caseFieldDefinitions.stream()
                    .filter(caseField -> caseField.getId().equalsIgnoreCase(caseDataPair.getKey()))
                    .findAny()
                    .map(caseField -> validateField(
                        caseDataPair.getKey(),
                        caseDataPair.getValue(),
                        caseField,
                        fieldIdPrefix,
                        validationContext
                    ))
                    .orElseGet(() -> Collections.singletonList(
                        new ValidationResult("Field is not recognised", fieldIdPrefix + caseDataPair.getKey()))))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<ValidationResult> validateField(final String dataFieldId,
                                                 final JsonNode dataValue,
                                                 final CaseFieldDefinition caseFieldDefinition,
                                                 final String fieldIdPrefix,
                                                 final ValidationContext validationContext) {
        final String caseFieldType = caseFieldDefinition.getFieldTypeDefinition().getType();

        if (!BaseType.contains(caseFieldType)) {
            return Collections.singletonList(new ValidationResult("Unknown Type:" + caseFieldType, dataFieldId));
        }

        final BaseType fieldType = BaseType.get(caseFieldType);

        if (BaseType.get(COMPLEX) == fieldType) {
            return validate(
                JacksonUtils.convertValue(dataValue),
                caseFieldDefinition.getFieldTypeDefinition().getComplexFields(),
                fieldIdPrefix + dataFieldId + FIELD_SEPARATOR, validationContext
            );
        } else if (BaseType.get(COLLECTION) == fieldType) {
            final List<ValidationResult> validationResults =
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
                validationResults.addAll(
                    validateCollectionItem(
                        caseFieldDefinition.getFieldTypeDefinition().getCollectionFieldTypeDefinition(),
                        itemValue,
                        fieldIdPrefix + dataFieldId + FIELD_SEPARATOR, index.toString(),
                        validationContext
                    )
                );
                index++;
            }
            return validationResults;
        } else {
            return validateSimpleField(
                                        dataFieldId,
                                        dataValue,
                                        caseFieldDefinition,
                                        fieldIdPrefix,
                                        fieldType,
                                        validationContext
            );
        }
    }

    private List<ValidationResult> validateSimpleField(final String fieldId,
                                                       final JsonNode dataValue,
                                                       final CaseFieldDefinition caseFieldDefinition,
                                                       final String fieldIdPrefix,
                                                       final BaseType fieldType,
                                                       final ValidationContext validationContext) {
        validationContext.setPath(fieldIdPrefix);
        validationContext.setFieldValue(dataValue);
        validationContext.setCaseFieldDefinition(caseFieldDefinition);
        validationContext.setFieldId(fieldId);
        Optional<FieldValidator> fieldIdBasedValidator = validators.stream().filter(
            validator -> isFieldIdBasedValidator(validator, fieldId)
        ).findAny();

        Optional<FieldValidator> customTypeValidator = validators.stream().filter(
            validator -> isCustomTypeValidator(validator, caseFieldDefinition.getFieldTypeDefinition().getId())
        ).findAny();

        Optional<FieldValidator> baseTypeValidator = validators.stream().filter(validator ->
            isBaseTypeValidator(validator, fieldType)
        ).findAny();

        Optional<FieldValidator> validatorToExecute =
            fieldIdBasedValidator.or(() -> customTypeValidator).or(() -> baseTypeValidator);

        return validatorToExecute.map(validator -> validator.validate(validationContext)
                .stream()
                .map(result ->
                    new ValidationResult(result.getErrorMessage(), fieldIdPrefix + result.getFieldId()))
                .collect(Collectors.toList()))
                .orElseThrow(() ->
                    new RuntimeException("System error: No validator found for " + fieldType.getType()));
    }

    private boolean isCustomTypeValidator(FieldValidator validator, String fieldTypeId) {
        if (validator instanceof CustomTypeValidator) {
            String customTypeId = ((CustomTypeValidator) validator).getCustomTypeId();
            return customTypeId.equals(fieldTypeId);
        }
        return false;
    }

    private boolean isFieldIdBasedValidator(FieldValidator validator, String fieldId) {
        if (validator instanceof FieldIdBasedValidator) {
            String validatorFieldId = ((FieldIdBasedValidator) validator).getFieldId();
            return validatorFieldId.equals(fieldId);
        }
        return false;
    }

    private boolean isBaseTypeValidator(FieldValidator validator, BaseType fieldType) {
        if (validator instanceof BaseTypeValidator) {
            return ((BaseTypeValidator) validator).getType() == fieldType;
        }
        return false;
    }

    private List<ValidationResult> validateCollectionItem(
        FieldTypeDefinition fieldTypeDefinition,
        JsonNode item,
        String fieldIdPrefix,
        String index,
        ValidationContext validationContext
    ) {
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
            return validateSimpleField(
                                        index,
                                        itemValue,
                                        caseFieldDefinition,
                                        fieldIdPrefix,
                                        baseType,
                                        validationContext
            );
        } else if (itemValue.isObject()) {
            return validate(
                JacksonUtils.convertValue(itemValue),
                fieldTypeDefinition.getComplexFields(),
                itemFieldId + FIELD_SEPARATOR, validationContext);
        }

        return Collections.singletonList(new ValidationResult("Unsupported collection item:" + itemValue
            .toString(), itemFieldId));
    }

    private boolean shouldTreatAsValueNode(FieldTypeDefinition fieldTypeDefinition, JsonNode itemValue) {
        return itemValue.isValueNode() || fieldTypeDefinition.getType().equalsIgnoreCase(DocumentValidator.TYPE_ID);
    }
}

