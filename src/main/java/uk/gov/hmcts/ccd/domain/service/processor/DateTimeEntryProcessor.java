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
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageComplexFieldOverride;
import uk.gov.hmcts.ccd.domain.types.BaseType;
import uk.gov.hmcts.ccd.domain.types.CollectionValidator;
import uk.gov.hmcts.ccd.endpoint.exceptions.DataProcessingException;

import java.util.Arrays;
import java.util.List;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.*;
import static uk.gov.hmcts.ccd.domain.service.processor.DisplayContextParameter.*;

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
    protected JsonNode executeSimple(JsonNode node, CommonField field, BaseType baseType, String fieldPath, WizardPageComplexFieldOverride override, CommonField topLevelField) {
        return !isNullOrEmpty(node)
            && hasDisplayContextParameterType(field.getDisplayContextParameter(), DisplayContextParameterType.DATETIMEENTRY)
            && isSupportedBaseType(baseType, SUPPORTED_TYPES) ?
            createNode(field.getDisplayContextParameter(), node.asText(), baseType, fieldPath) :
            node;
    }

    @Override
    protected JsonNode executeCollection(JsonNode collectionNode, CommonField caseViewField, String fieldPath, WizardPageComplexFieldOverride override, CommonField topLevelField) {
        final BaseType collectionFieldType = BaseType.get(caseViewField.getFieldType().getCollectionFieldType().getType());

        if (shouldExecuteCollection(collectionNode, caseViewField,
            DisplayContextParameterType.DATETIMEENTRY, collectionFieldType, SUPPORTED_TYPES)) {
            ArrayNode newNode = MAPPER.createArrayNode();
            collectionNode.forEach(item -> {
                JsonNode newItem = item.deepCopy();
                ((ObjectNode)newItem).replace(CollectionValidator.VALUE,
                    createCollectionValueNode(item.get(CollectionValidator.VALUE),
                        collectionFieldType, caseViewField, fieldPath, topLevelField));
                newNode.add(newItem);
            });

            return newNode;
        }

        return collectionNode;
    }

    private JsonNode createCollectionValueNode(JsonNode valueNode, BaseType collectionFieldType, CommonField caseViewField, String fieldPath, CommonField topLevelField) {
        if (valueNode.isNull()) {
            return valueNode;
        }
        return isSupportedBaseType(collectionFieldType, SUPPORTED_TYPES) ?
            createNode(caseViewField.getDisplayContextParameter(), valueNode.asText(), collectionFieldType, fieldPath) :
            executeComplex(valueNode, caseViewField.getFieldType().getChildren(), null, fieldPath, topLevelField);
    }

    private TextNode createNode(String displayContextParameter, String valueToConvert, BaseType baseType, String fieldPath) {
        String format = getDisplayContextParameterOfType(displayContextParameter, DisplayContextParameterType.DATETIMEENTRY)
            .map(DisplayContextParameter::getValue)
            .orElseThrow(() -> new DataProcessingException().withDetails(
                String.format("Unable to obtain datetime format for field %s with display context parameter %s",
                    fieldPath,
                    displayContextParameter)
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
