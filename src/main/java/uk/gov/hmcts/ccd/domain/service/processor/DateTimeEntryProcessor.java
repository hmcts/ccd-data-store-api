package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewFieldBuilder;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameter;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageComplexFieldOverride;
import uk.gov.hmcts.ccd.domain.types.BaseType;
import uk.gov.hmcts.ccd.domain.types.CollectionValidator;
import uk.gov.hmcts.ccd.endpoint.exceptions.DataProcessingException;

import java.util.Arrays;
import java.util.List;

import static uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterType.DATETIMEENTRY;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.*;

@Component
public class DateTimeEntryProcessor extends CaseDataFieldProcessor {

    private static final List<String> SUPPORTED_TYPES = Arrays.asList(DATETIME, DATE);

    private final DateTimeFormatParser dateTimeFormatParser;

    @Autowired
    public DateTimeEntryProcessor(CaseViewFieldBuilder caseViewFieldBuilder,
                                  DateTimeFormatParser dateTimeFormatParser) {
        super(caseViewFieldBuilder);
        this.dateTimeFormatParser = dateTimeFormatParser;
    }

    @Override
    protected JsonNode executeSimple(JsonNode node,
                                     CommonField field,
                                     BaseType baseType,
                                     String fieldPath,
                                     WizardPageComplexFieldOverride override,
                                     CommonField topLevelField) {
        return !isNullOrEmpty(node)
            && field.hasDisplayContextParameter(DATETIMEENTRY)
            && isSupportedBaseType(baseType, SUPPORTED_TYPES) ?
            createNode(field, node.asText(), baseType, fieldPath) :
            node;
    }

    @Override
    protected JsonNode executeCollection(JsonNode collectionNode,
                                         CommonField field,
                                         String fieldPath,
                                         WizardPageComplexFieldOverride override,
                                         CommonField topLevelField) {
        final BaseType collectionFieldType = BaseType.get(field.getFieldType().getCollectionFieldType().getType());

        if ((field.hasDisplayContextParameter(DATETIMEENTRY)
            && isSupportedBaseType(collectionFieldType, SUPPORTED_TYPES))
            || BaseType.get(COMPLEX) == collectionFieldType) {
            ArrayNode newNode = MAPPER.createArrayNode();
            collectionNode.forEach(item -> {
                JsonNode newItem = item.deepCopy();
                ((ObjectNode)newItem).replace(CollectionValidator.VALUE,
                    isSupportedBaseType(collectionFieldType, SUPPORTED_TYPES) ?
                        createNode(field, item.get(CollectionValidator.VALUE).asText(), collectionFieldType, fieldPath) :
                        executeComplex(item.get(CollectionValidator.VALUE), field.getFieldType().getChildren(), null, fieldPath, topLevelField));
                newNode.add(newItem);
            });

            return newNode;
        }

        return collectionNode;
    }

    private TextNode createNode(CommonField caseViewField, String valueToConvert, BaseType baseType, String fieldPath) {
        String format = caseViewField.getDisplayContextParameter(DATETIMEENTRY)
            .map(DisplayContextParameter::getValue)
            .orElseThrow(() -> new DataProcessingException().withDetails(
                String.format("Unable to obtain datetime format for field %s with display context parameter %s",
                    fieldPath,
                    caseViewField.getDisplayContextParameter())
            ));
        if (Strings.isNullOrEmpty(valueToConvert)) {
            return new TextNode(valueToConvert);
        }
        try {
            if (baseType == BaseType.get(DATETIME)) {
                return new TextNode(dateTimeFormatParser.convertDateTimeToIso8601(format, valueToConvert));
            } else {
                return new TextNode(dateTimeFormatParser.convertDateToIso8601(format, valueToConvert));
            }
        } catch (Exception e) {
            throw new DataProcessingException().withDetails(
                String.format("Unable to process field %s with value %s. Expected format to be either %s or %s",
                    fieldPath,
                    valueToConvert,
                    format,
                    baseType == BaseType.get(DATETIME) ? DateTimeFormatParser.DATE_TIME_FORMAT : DateTimeFormatParser.DATE_FORMAT)
            );
        }
    }
}
