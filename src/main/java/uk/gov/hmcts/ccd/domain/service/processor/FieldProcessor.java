package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Optional;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewFieldBuilder;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.DisplayContext;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageComplexFieldOverride;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageField;
import uk.gov.hmcts.ccd.domain.types.BaseType;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;

public abstract class FieldProcessor {

    protected static final ObjectMapper MAPPER = new ObjectMapper();
    protected static final String FIELD_SEPARATOR = ".";

    private final CaseViewFieldBuilder caseViewFieldBuilder;

    public FieldProcessor(CaseViewFieldBuilder caseViewFieldBuilder) {
        this.caseViewFieldBuilder = caseViewFieldBuilder;
    }

    protected JsonNode execute(JsonNode node, CaseFieldDefinition caseField, CaseEventFieldDefinition caseEventField, WizardPageField wizardPageField) {
        CaseViewField caseViewField = caseViewFieldBuilder.build(caseField, caseEventField);

        if (!BaseType.contains(caseViewField.getFieldTypeDefinition().getType())) {
            return node;
        }

        final BaseType fieldType = BaseType.get(caseViewField.getFieldTypeDefinition().getType());

        if (BaseType.get(COMPLEX) == fieldType) {
            return executeComplex(node, caseField.getFieldTypeDefinition().getComplexFields(), wizardPageField, caseField.getId(), caseViewField);
        } else if (BaseType.get(COLLECTION) == fieldType) {
            return executeCollection(node, caseViewField, caseField.getId(), null, caseViewField);
        } else {
            return executeSimple(node, caseViewField, fieldType, caseField.getId(), null, caseViewField);
        }
    }

    protected JsonNode executeComplex(JsonNode complexNode,
                                      List<CaseFieldDefinition> complexCaseFields,
                                      WizardPageField wizardPageField,
                                      String fieldPrefix,
                                      CommonField topLevelField) {
        if (complexNode == null) {
            return null;
        }
        ObjectNode newNode = MAPPER.createObjectNode();
        complexCaseFields.stream().forEach(complexCaseField -> {
            final BaseType complexFieldType = BaseType.get(complexCaseField.getFieldTypeDefinition().getType());
            final String fieldId = complexCaseField.getId();
            final JsonNode caseFieldNode = complexNode.get(fieldId);
            final String fieldPath = fieldPrefix + FIELD_SEPARATOR + fieldId;

            if (complexFieldType == BaseType.get(COLLECTION)) {
                newNode.set(
                    fieldId,
                    executeCollection(
                        caseFieldNode,
                        complexCaseField,
                        fieldPath,
                        wizardPageComplexFieldOverride(wizardPageField, fieldPath).orElse(null),
                        topLevelField)
                );
            } else if (complexFieldType == BaseType.get(COMPLEX)) {
                Optional.ofNullable(
                    executeComplex(caseFieldNode, complexCaseField.getFieldTypeDefinition().getComplexFields(), wizardPageField, fieldPath, topLevelField))
                    .ifPresent(result -> newNode.set(fieldId, result));
            } else {
                newNode.set(
                    fieldId,
                    executeSimple(
                        caseFieldNode,
                        complexCaseField,
                        complexFieldType,
                        fieldPath,
                        wizardPageComplexFieldOverride(wizardPageField, fieldPath).orElse(null),
                        topLevelField)
                );
            }
        });

        return newNode;
    }

    protected abstract JsonNode executeSimple(JsonNode node,
                                              CommonField field,
                                              BaseType baseType,
                                              String fieldPath,
                                              WizardPageComplexFieldOverride override,
                                              CommonField topLevelField);

    protected abstract JsonNode executeCollection(JsonNode collectionNode,
                                                  CommonField field,
                                                  String fieldPath,
                                                  WizardPageComplexFieldOverride override,
                                                  CommonField topLevelField);

    public static boolean isNullOrEmpty(final JsonNode node) {
        return node == null
            || node.isNull()
            || (node.isTextual() && (null == node.asText() || node.asText().trim().length() == 0))
            || (node.isObject() && node.toString().equals("{}"));
    }

    protected boolean isSupportedBaseType(BaseType baseType, List<String> supportedTypes) {
        return supportedTypes.contains(baseType.getType());
    }

    protected DisplayContext displayContext(CommonField field, WizardPageComplexFieldOverride override) {
        return Optional.ofNullable(override)
            .map(WizardPageComplexFieldOverride::displayContextType)
            .orElse(Optional.ofNullable(field.displayContextType()).orElse(DisplayContext.READONLY));
    }

    private Optional<WizardPageComplexFieldOverride> wizardPageComplexFieldOverride(WizardPageField wizardPageField, String fieldPath) {
        return wizardPageField != null
            ? wizardPageField.getComplexFieldOverride(fieldPath)
            : Optional.empty();
    }
}
