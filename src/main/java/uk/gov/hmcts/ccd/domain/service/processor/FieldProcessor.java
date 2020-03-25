package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewFieldBuilder;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageField;
import uk.gov.hmcts.ccd.domain.types.BaseType;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.*;

public abstract class FieldProcessor {

    protected static final ObjectMapper MAPPER = new ObjectMapper();
    protected static final String FIELD_SEPARATOR = ".";

    private final CaseViewFieldBuilder caseViewFieldBuilder;

    public FieldProcessor(CaseViewFieldBuilder caseViewFieldBuilder) {
        this.caseViewFieldBuilder = caseViewFieldBuilder;
    }

    public JsonNode execute(JsonNode node, CaseField caseField, CaseEventField caseEventField, WizardPageField wizardPageField) {
        CaseViewField caseViewField = caseViewFieldBuilder.build(caseField, caseEventField);

        if (!BaseType.contains(caseViewField.getFieldType().getType())) {
            return node;
        }

        final BaseType fieldType = BaseType.get(caseViewField.getFieldType().getType());

        if (BaseType.get(COMPLEX) == fieldType) {
            return executeComplex(node, caseField.getFieldType().getComplexFields(), caseEventField, wizardPageField, caseField.getId());
        } else if (BaseType.get(COLLECTION) == fieldType) {
            return executeCollection(node, caseViewField, caseField.getId());
        } else {
            return executeSimple(node, caseViewField, fieldType, caseField.getId());
        }
    }

    private JsonNode executeComplex(JsonNode complexNode,
                                      List<CaseField> complexCaseFields,
                                      CaseEventField caseEventField,
                                      WizardPageField wizardPageField,
                                      String fieldPrefix) {
        if (complexNode == null) {
            return null;
        }
        ObjectNode newNode = MAPPER.createObjectNode();
        complexCaseFields.stream().forEach(complexCaseField -> {
            final BaseType complexFieldType = BaseType.get(complexCaseField.getFieldType().getType());
            final String fieldId = complexCaseField.getId();
            final JsonNode caseFieldNode = complexNode.get(fieldId);
            final String fieldPath = fieldPrefix + FIELD_SEPARATOR + fieldId;

            if (complexFieldType == BaseType.get(COLLECTION)) {
                newNode.set(fieldId, executeCollection(caseFieldNode, complexCaseField, fieldPath));
            } else if (complexFieldType == BaseType.get(COMPLEX)) {
                Optional.ofNullable(
                    executeComplex(caseFieldNode, complexCaseField.getFieldType().getComplexFields(), caseEventField, wizardPageField, fieldPath))
                    .ifPresent(result -> newNode.set(fieldId, result));
            } else {
                newNode.set(fieldId, executeSimple(caseFieldNode, complexCaseField, complexFieldType, fieldPath));
            }
        });

        return newNode;
    }

    protected abstract JsonNode executeSimple(JsonNode node, CommonField field, BaseType baseType, String fieldPath);

    protected abstract JsonNode executeCollection(JsonNode collectionNode, CommonField field, String fieldPath);

    protected boolean isNullOrEmpty(final JsonNode node) {
        return node == null
            || node.isNull()
            || (node.isTextual() && (null == node.asText() || node.asText().trim().length() == 0))
            || (node.isObject() && node.toString().equals("{}"));
    }

    protected boolean isSupportedBaseType(BaseType baseType, List<String> supportedTypes) {
        return supportedTypes.contains(baseType.getType());
    }
}
