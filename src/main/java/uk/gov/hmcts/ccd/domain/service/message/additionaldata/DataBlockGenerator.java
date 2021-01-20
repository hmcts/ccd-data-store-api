package uk.gov.hmcts.ccd.domain.service.message.additionaldata;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.DisplayContext;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static uk.gov.hmcts.ccd.domain.model.common.CaseFieldPathUtils.getNestedCaseFieldByPath;
import static uk.gov.hmcts.ccd.domain.service.message.additionaldata.PublishableField.FIELD_SEPARATOR;

@Component
public class DataBlockGenerator {

    public Map<String, Object> generateData(AdditionalDataContext context) {
        Map<String, Object> dataBlock = newHashMap();

        context.getTopLevelPublishables().forEach(publishableField -> buildTopLevelDataBlock(publishableField,
            dataBlock, context.getNestedPublishables(), context.getCaseDetails()));

        return dataBlock;
    }

    private Map<String, Object> buildTopLevelDataBlock(PublishableField publishableField,
                                                       Map<String, Object> dataBlock,
                                                       List<PublishableField> nestedPublishable,
                                                       CaseDetails caseDetails) {
        if (isBoolean(publishableField.getFieldType().getType())) {
            dataBlock.put(publishableField.getKey(), Boolean.valueOf(publishableField.getValue()));
        } else if (isNumber(publishableField.getFieldType().getType())) {
            dataBlock.put(publishableField.getKey(),
                (publishableField.getValue() != null) ? Double.parseDouble(publishableField.getValue()) : null);
        } else if (publishableField.getDisplayContext() != null) {
            if (publishableField.getDisplayContext().equals(DisplayContext.COMPLEX)) {
                buildNestedLevelDataBlock(publishableField, dataBlock, nestedPublishable, caseDetails);
            } else {
                dataBlock.put(publishableField.getKey(), (publishableField.getComplexValue()));
            }
        } else {
            dataBlock.put(publishableField.getKey(), (publishableField.getComplexValue()));
        }
        return dataBlock;
    }

    private Map<String, Object> buildNestedLevelDataBlock(PublishableField publishableField,
                                                          Map<String, Object> dataBlock,
                                                          List<PublishableField> nestedPublishable,
                                                          CaseDetails caseDetails) {
        Map<String, Object> nestedDataBlock = newHashMap();
        List<PublishableField> fields = publishableField.filterDirectChildrenFrom(nestedPublishable);
        fields.forEach(field -> {
            JsonNode node = getNestedCaseFieldByPath(caseDetails.getData().get(field.splitPath()[0]),
                StringUtils.substringAfter(field.getPath(), FIELD_SEPARATOR));
            if (!field.getOriginalId().equals(field.getKey())) {
                populateDataBlockForComplex(dataBlock, field, node, field.getKey(), node.asBoolean());
                buildNestedDataBlock(field, nestedDataBlock, caseDetails);
            } else {
                dataBlock.put(publishableField.getKey(),
                    populateDataBlockForComplex(nestedDataBlock, field, node, field.getKey(), node.asBoolean()));
            }
        });

        dataBlock.put(publishableField.getKey(), nestedDataBlock);
        return dataBlock;
    }

    private Map<String, Object> populateDataBlockForComplex(Map<String, Object> dataBlock,
                                                            PublishableField field,
                                                            JsonNode node,
                                                            String key,
                                                            boolean b) {
        if (isBoolean(field.getFieldType().getType())) {
            dataBlock.put(key, b);
        } else if (isNumber(field.getFieldType().getType())) {
            dataBlock.put(key, node.asDouble());
        } else {
            dataBlock.put(key, node);
        }
        return dataBlock;
    }

    private Map<String, Object> buildNestedDataBlock(PublishableField publishableField,
                                                     Map<String, Object> nestedDataBlock, CaseDetails caseDetails) {

        JsonNode node = getNestedCaseFieldByPath(caseDetails.getData().get(publishableField.splitPath()[0]),
            StringUtils.substringAfter(publishableField.getPath(), FIELD_SEPARATOR));

        populateDataBlockForComplex(nestedDataBlock,
            publishableField, node,
            publishableField.getOriginalId(),
            node.booleanValue());
        return nestedDataBlock;
    }

    private boolean isBoolean(String fieldType) {
        return fieldType.equals(FieldTypeDefinition.YES_OR_NO);
    }

    private boolean isNumber(String fieldType) {
        return fieldType.equals(FieldTypeDefinition.NUMBER) || fieldType.equals(FieldTypeDefinition.MONEY_GBP);
    }
}
