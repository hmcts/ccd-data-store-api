package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewFieldBuilder;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageField;
import uk.gov.hmcts.ccd.domain.types.BaseType;
import uk.gov.hmcts.ccd.domain.types.CollectionValidator;

import java.util.*;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.*;

@Component
public class DateTimeFormatProcessor extends FieldProcessor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DateTimeFormatParser dateTimeFormatParser;

    @Autowired
    public DateTimeFormatProcessor(CaseViewFieldBuilder caseViewFieldBuilder,
                                   DateTimeFormatParser dateTimeFormatParser) {
        super(caseViewFieldBuilder);
        this.dateTimeFormatParser = dateTimeFormatParser;
    }

    @Override
    protected JsonNode executeSimple(JsonNode node, CaseViewField caseViewField, BaseType baseType) {
        if (!Strings.isNullOrEmpty(node.asText())
            && hasDateTimeEntryDCP(caseViewField.getDisplayContextParameter())
            && baseType == BaseType.get(DATETIME)) {
            return createTextNode(caseViewField.getDisplayContextParameter(), node.asText());
        }
        return node;
    }

    @Override
    protected JsonNode executeCollection(JsonNode collectionNode, CommonField caseViewField) {
        final BaseType collectionFieldType = BaseType.get(caseViewField.getFieldType().getCollectionFieldType().getType());

        if (hasDateTimeEntryDCP(caseViewField.getDisplayContextParameter())
            && collectionFieldType == BaseType.get(DATETIME)) {
            ArrayNode newNode = MAPPER.createArrayNode();
            collectionNode.forEach(item -> {
                JsonNode newItem = item.deepCopy();
                ((ObjectNode)newItem).replace(CollectionValidator.VALUE,
                    createTextNode(caseViewField.getDisplayContextParameter(), item.get(CollectionValidator.VALUE).asText()));
                newNode.add(newItem);
            });

            return newNode;
        }

        return collectionNode;
    }

    @Override
    protected JsonNode executeComplex(JsonNode complexNode, List<CaseField> complexCaseFields, CaseEventField caseEventField, WizardPageField wizardPageField, String fieldPrefix) {
        ObjectNode newNode = MAPPER.createObjectNode();

        complexCaseFields.stream().forEach(complexCaseField -> {
            final BaseType complexFieldType = BaseType.get(complexCaseField.getFieldType().getType());
            final String fieldId = complexCaseField.getId();
            final JsonNode caseFieldNode = complexNode.get(fieldId);
            final String fieldPath = fieldPrefix + FIELD_SEPARATOR + fieldId;

            if (complexFieldType == BaseType.get(COLLECTION)) {
                newNode.set(fieldId, executeCollection(caseFieldNode, complexCaseField));
            } else if (complexFieldType == BaseType.get(COMPLEX)) {
                newNode.set(fieldId, executeComplex(caseFieldNode, complexCaseField.getFieldType().getComplexFields(), caseEventField, wizardPageField, fieldPath));
            } else {
                // TODO: Get override
                newNode.set(fieldId,
                    !isNullOrEmpty(caseFieldNode)
                        && hasDateTimeEntryDCP(complexCaseField.getDisplayContextParameter())
                        && complexFieldType == BaseType.get(DATETIME) ?
                        createTextNode(complexCaseField.getDisplayContextParameter(), complexNode.get(fieldId).asText()) :
                        caseFieldNode);
            }
        });

        return newNode;
    }

    private TextNode createTextNode(String displayContextParameter, String valueToConvert) {
        return new TextNode(dateTimeFormatParser.convertDateTimeToIso8601(
            DisplayContextParameter
                .getDisplayContextParameterOfType(displayContextParameter, DisplayContextParameterType.DATETIMEENTRY).get().getValue()
            , valueToConvert)
        );
    }

    private boolean hasDateTimeEntryDCP(String displayContextParameter) {
        return !Strings.isNullOrEmpty(displayContextParameter) &&
            DisplayContextParameter
                .getDisplayContextParameterOfType(displayContextParameter, DisplayContextParameterType.DATETIMEENTRY)
                .isPresent();
    }

    private boolean isNullOrEmpty(final JsonNode node) {
        return node == null
            || node.isNull()
            || (node.isTextual() && (null == node.asText() || node.asText().trim().length() == 0))
            || (node.isObject() && node.toString().equals("{}"));
    }
}
