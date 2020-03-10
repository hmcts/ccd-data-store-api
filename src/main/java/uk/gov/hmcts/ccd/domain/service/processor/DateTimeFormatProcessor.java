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
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplex;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.types.BaseType;
import uk.gov.hmcts.ccd.domain.types.CollectionValidator;

import java.util.*;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.*;

@Component
public class DateTimeFormatProcessor extends AbstractFieldProcessor {

    private static final String DATETIMEDISPLAY_PREFIX = "#DATETIMEDISPLAY(";
    private static final String DATETIMEENTRY_PREFIX = "#DATETIMEENTRY(";
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
            && isDateTimeDCP(caseViewField.getDisplayContextParameter())
            && baseType == BaseType.get(DATETIME)) {
            return createTextNode(caseViewField.getDisplayContextParameter(), node.asText());
        }
        return node;
    }

    @Override
    protected JsonNode executeCollection(JsonNode collectionNode, CommonField caseViewField) {
        final BaseType collectionFieldType = BaseType.get(caseViewField.getFieldType().getCollectionFieldType().getType());

        if (isDateTimeDCP(caseViewField.getDisplayContextParameter())
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
    protected JsonNode executeComplex(JsonNode complexNode, List<CaseField> complexCaseFields, CaseEventField caseEventField) {
        ObjectNode newNode = MAPPER.createObjectNode();

        complexCaseFields.stream().forEach(complexCaseField -> {
            final String displayContextParameter = getComplexDisplayContextParameter(caseEventField, complexCaseField);
            final BaseType complexFieldType = BaseType.get(complexCaseField.getFieldType().getType());
            final String fieldId = complexCaseField.getId();
            final JsonNode caseFieldNode = complexNode.get(fieldId);

            if (complexFieldType == BaseType.get(COLLECTION)) {
                newNode.set(fieldId, executeCollection(caseFieldNode, complexCaseField));
            } else if (complexFieldType == BaseType.get(COMPLEX)) {
                newNode.set(fieldId, executeComplex(caseFieldNode, complexCaseField.getFieldType().getComplexFields(), caseEventField));
            } else {
                newNode.set(fieldId,
                    isDateTimeDCP(displayContextParameter)
                        && complexFieldType == BaseType.get(DATETIME) ?
                        createTextNode(displayContextParameter, complexNode.get(fieldId).asText()) :
                        caseFieldNode);
            }
        });

        return newNode;
    }

    private TextNode createTextNode(String displayContextParameter, String valueToConvert) {
        return new TextNode(dateTimeFormatParser
            .convertDateTimeToIso8601(DisplayContextParameterType
            .getDisplayContextParameterFor(displayContextParameter).get().getValue(), valueToConvert));
    }

    private String getComplexDisplayContextParameter(CaseEventField caseEventField, CaseField complexCaseField) {
        return caseEventField.getCaseEventFieldComplex().stream()
            .filter(complexFieldOverride -> complexCaseField.getId().equals(complexFieldOverride.getReference()))
            .findAny()
            .map(CaseEventFieldComplex::getDisplayContextParameter)
            .orElseGet(complexCaseField::getDisplayContextParameter);
    }

    private boolean isDateTimeDCP(String displayContextParameter) {
        return !Strings.isNullOrEmpty(displayContextParameter) && (
            displayContextParameter.startsWith(DATETIMEDISPLAY_PREFIX) ||
                displayContextParameter.startsWith(DATETIMEENTRY_PREFIX));
    }
}
