package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewFieldBuilder;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.types.BaseType;

import java.util.*;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.*;

@Component
public class DateTimeFormatProcessor implements FieldProcessor {

    private static final String DATETIMEDISPLAY_PREFIX = "#DATETIMEDISPLAY(";
    private static final String DATETIMEENTRY_PREFIX = "#DATETIMEENTRY(";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<HashMap<String, JsonNode>> STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {};

    private final CaseViewFieldBuilder caseViewFieldBuilder;
    private final DateTimeFormatParser dateTimeFormatParser;

    @Autowired
    public DateTimeFormatProcessor(CaseViewFieldBuilder caseViewFieldBuilder,
                                   DateTimeFormatParser dateTimeFormatParser) {
        this.caseViewFieldBuilder = caseViewFieldBuilder;
        this.dateTimeFormatParser = dateTimeFormatParser;
    }

    @Override
    public JsonNode execute(JsonNode node, CaseField caseField, CaseEventField caseEventField) {
        CaseViewField caseViewField = caseViewFieldBuilder.build(caseField, caseEventField);

        if (!BaseType.contains(caseViewField.getFieldType().getType())) {
            // TODO: Error
        }

        final BaseType fieldType = BaseType.get(caseViewField.getFieldType().getType());

        if (BaseType.get(COMPLEX) == fieldType) {
            return executeComplex(node, caseField.getFieldType().getComplexFields(), caseEventField);
        } else if (BaseType.get(COLLECTION) == fieldType) {
            return executeCollection(node, caseField, caseEventField);
        } else {
            return executeSimple(node, caseViewField, fieldType);
        }
    }

    @SneakyThrows
    private JsonNode executeSimple(JsonNode node, CaseViewField caseViewField, BaseType baseType) {
        if (!Strings.isNullOrEmpty(node.asText())
            && isDateTimeDisplayContextParameter(caseViewField.getDisplayContextParameter())
            && baseType == BaseType.get(DATETIME)) {
            final Optional<DisplayContextParameter> param = DisplayContextParameterType.getDisplayContextParameterFor(caseViewField.getDisplayContextParameter());
            dateTimeFormatParser.parseDateTimeFormat(param.get().getValue(), node.textValue());
            return new TextNode(dateTimeFormatParser.convertDateTimeToIso8601(param.get().getValue(), node.asText()));
        }
        return node;
    }

    // TODO
    private JsonNode executeCollection(JsonNode node, CaseField caseField, CaseEventField caseEventField) {
        ArrayNode arrayNode = MAPPER.createArrayNode();

        final Iterator<JsonNode> collectionIterator = node.iterator();
        int index = 0;
        while (collectionIterator.hasNext()) {
            final JsonNode itemValue = collectionIterator.next();

            arrayNode.add(itemValue);

            index++;
        }

        return node;
    }

    // TODO
    private JsonNode executeComplex(JsonNode node, List<CaseField> caseFields, CaseEventField caseEventField) {
        Map<String, JsonNode> nodeMap = MAPPER.convertValue(node, STRING_JSON_MAP);

        ObjectNode objectNode = MAPPER.createObjectNode();
        nodeMap.entrySet().stream().forEach(entry -> {
            objectNode.set(entry.getKey(), entry.getValue());
        });

        return objectNode;
    }

    private boolean isDateTimeDisplayContextParameter(String displayContextParameter) {
        return !Strings.isNullOrEmpty(displayContextParameter) && (
            displayContextParameter.startsWith(DATETIMEDISPLAY_PREFIX) ||
                displayContextParameter.startsWith(DATETIMEENTRY_PREFIX));
    }
}
