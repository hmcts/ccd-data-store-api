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
        if (publishableField.getFieldType().getType().equals(FieldTypeDefinition.YES_OR_NO)) {
            dataBlock.put(publishableField.getKey(), Boolean.valueOf(publishableField.getValue()));
        } else if (publishableField.getFieldType().getType().equals(FieldTypeDefinition.NUMBER)
            || publishableField.getFieldType().getType().equals(FieldTypeDefinition.MONEY_GBP)) {
            dataBlock.put(publishableField.getKey(),
                (publishableField.getValue() != null) ? Double.parseDouble(publishableField.getValue()) : null);
        } else if (publishableField.getDisplayContext() != null) {
            if (publishableField.getDisplayContext().equals(DisplayContext.COMPLEX)) {
                buildNestedLevelDataBlock(publishableField, dataBlock, nestedPublishable, caseDetails);
            } else {
                dataBlock.put(publishableField.getKey(), (publishableField.getValue()));
            }
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
            buildNestedDataBlock(field, nestedDataBlock, caseDetails);
        });
        dataBlock.put(publishableField.getKey(), nestedDataBlock);
        return dataBlock;
    }

    private Map<String, Object> buildNestedDataBlock(PublishableField publishableField,
                                                     Map<String, Object> nestedDataBlock, CaseDetails caseDetails) {

        JsonNode node = getNestedCaseFieldByPath(caseDetails.getData().get(publishableField.splitPath()[0]),
            StringUtils.substringAfter(publishableField.getPath(), FIELD_SEPARATOR));

        if (publishableField.getFieldType().getType().equals(FieldTypeDefinition.YES_OR_NO)) {
            nestedDataBlock.put(publishableField.getKey(), node.booleanValue());
        } else if (publishableField.getFieldType().getType().equals(FieldTypeDefinition.NUMBER)
            || publishableField.getFieldType().getType().equals(FieldTypeDefinition.MONEY_GBP)) {
            nestedDataBlock.put(publishableField.getKey(), node.asDouble());
        } else {
            nestedDataBlock.put(publishableField.getKey(), node);
        }
        return nestedDataBlock;
    }
}
