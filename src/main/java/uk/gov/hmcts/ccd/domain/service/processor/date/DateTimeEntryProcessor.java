package uk.gov.hmcts.ccd.domain.service.processor.date;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewFieldBuilder;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageComplexFieldOverride;
import uk.gov.hmcts.ccd.domain.service.processor.CaseDataFieldProcessor;
import uk.gov.hmcts.ccd.domain.service.processor.date.DateTimeFormatParser;
import uk.gov.hmcts.ccd.domain.types.BaseType;
import uk.gov.hmcts.ccd.endpoint.exceptions.DataProcessingException;

import static uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterType.DATETIMEENTRY;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.DATE;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.DATETIME;
import static uk.gov.hmcts.ccd.domain.types.CollectionValidator.VALUE;

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
            && isSupportedBaseType(baseType, SUPPORTED_TYPES)
            ? createNode(field, node.asText(), baseType, fieldPath)
            : node;
    }

    @Override
    protected JsonNode executeCollection(JsonNode collectionNode,
                                         CommonField caseViewField,
                                         String fieldPath,
                                         WizardPageComplexFieldOverride override,
                                         CommonField topLevelField) {
        final BaseType collectionFieldType =
            BaseType.get(caseViewField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getType());

        if (shouldExecuteCollection(collectionNode, caseViewField,
            DATETIMEENTRY, collectionFieldType, SUPPORTED_TYPES)) {
            ArrayNode newNode = MAPPER.createArrayNode();
            collectionNode.forEach(item -> {
                JsonNode newItem = item.deepCopy();
                ((ObjectNode)newItem).replace(VALUE,
                    createCollectionValueNode(item.get(VALUE),
                        collectionFieldType, caseViewField, fieldPath, topLevelField));
                newNode.add(newItem);
            });

            return newNode;
        }

        return collectionNode;
    }

    private JsonNode createCollectionValueNode(JsonNode valueNode,
                                               BaseType collectionFieldType,
                                               CommonField caseViewField,
                                               String fieldPath,
                                               CommonField topLevelField) {
        if (valueNode.isNull()) {
            return valueNode;
        }
        return isSupportedBaseType(collectionFieldType, SUPPORTED_TYPES)
            ? createNode(caseViewField, valueNode.asText(), collectionFieldType, fieldPath)
            : executeComplex(valueNode,
                            caseViewField.getFieldTypeDefinition().getChildren(),
                            null,
                            fieldPath,
                            topLevelField);
    }

    private TextNode createNode(CommonField caseViewField, String valueToConvert, BaseType baseType, String fieldPath) {
        String format = caseViewField.getDisplayContextParameterValue(DATETIMEENTRY)
            .orElseThrow(() -> new DataProcessingException().withDetails(
                String.format("Unable to obtain datetime format for field %s with display context parameter %s",
                    fieldPath,
                    caseViewField.getDisplayContextParameter())
            ));

        return dateTimeFormatParser.valueToTextNode(valueToConvert, baseType, fieldPath, format, true);
    }
}
