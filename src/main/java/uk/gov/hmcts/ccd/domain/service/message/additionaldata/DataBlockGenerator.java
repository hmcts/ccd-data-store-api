package uk.gov.hmcts.ccd.domain.service.message.additionaldata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.DisplayContext;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Maps.newHashMap;
import static uk.gov.hmcts.ccd.domain.model.common.CaseFieldPathUtils.getNestedCaseFieldByPath;
import static uk.gov.hmcts.ccd.domain.service.message.additionaldata.PublishableField.FIELD_SEPARATOR;

@Component
public class DataBlockGenerator {

    ObjectMapper mapper = new ObjectMapper();

    public Map<String, JsonNode> generateData(AdditionalDataContext context) {
        Map<String, JsonNode> dataBlock = newHashMap();

        context.getTopLevelPublishables().forEach(publishableField -> buildTopLevelDataBlock(publishableField,
            dataBlock, context.getNestedPublishables(), context.getCaseDetails()));

        return dataBlock;
    }

    private Map<String, JsonNode> buildTopLevelDataBlock(PublishableField publishableField,
                                                       Map<String, JsonNode> dataBlock,
                                                       List<PublishableField> nestedPublishable,
                                                       CaseDetails caseDetails) {
        if (publishableField.getDisplayContext().equals(DisplayContext.COMPLEX)) {
            buildNestedLevelDataBlock(publishableField, dataBlock, nestedPublishable, caseDetails);
        } else {
            JsonNode node = getNestedCaseFieldByPath(mapper.valueToTree(caseDetails.getData()), publishableField.getOriginalId());
            switch (publishableField.getCaseField().getFieldTypeDefinition().getType()) {
                case FieldTypeDefinition.YES_OR_NO:
                    dataBlock.put(publishableField.getKey(), booleanNodeOf(node));
                    break;
                case FieldTypeDefinition.NUMBER:
                case FieldTypeDefinition.MONEY_GBP:
                    dataBlock.put(publishableField.getKey(), intNodeOf(node));
                    break;
                case FieldTypeDefinition.COMPLEX:
                case FieldTypeDefinition.COLLECTION:
                    dataBlock.put(publishableField.getKey(), node);
                    break;
                default:
                    dataBlock.put(publishableField.getKey(), textNodeOf(node));
            }
        }
        return dataBlock;

    }

    private JsonNode booleanNodeOf(JsonNode node) {
        if (node == null || node.isNull() || isNullOrEmpty(node.textValue())) {
            return mapper.nullNode();
        }

        return node.textValue().equalsIgnoreCase("Yes") ? BooleanNode.TRUE : BooleanNode.FALSE;
    }

    private JsonNode intNodeOf(JsonNode node) {
        if (node == null || node.isNull()) {
            return mapper.nullNode();
        }

        return IntNode.valueOf(node.intValue());
    }

    private JsonNode textNodeOf(JsonNode node) {
        if (node == null || node.isNull()) {
            return mapper.nullNode();
        }

        return TextNode.valueOf(node.asText());
    }

    private Map<String, JsonNode> buildNestedLevelDataBlock(PublishableField publishableField,
                                                          Map<String, JsonNode> dataBlock,
                                                          List<PublishableField> nestedPublishable,
                                                          CaseDetails caseDetails) {
        Map<String, Object> nestedDataBlock = newHashMap();
        List<PublishableField> fields = publishableField.filterDirectChildrenFrom(nestedPublishable);
        fields.forEach(field -> {
            JsonNode node = getNestedCaseFieldByPath(caseDetails.getData().get(field.splitPath()[0]),
                StringUtils.substringAfter(field.getPath(), FIELD_SEPARATOR));
            if (!field.getOriginalId().equals(field.getKey())) {
                populateDataBlockForComplex(field, node);
//                buildNestedDataBlock(field, nestedDataBlock, caseDetails);
            } else {
                dataBlock.put(publishableField.getKey(),
                    populateDataBlockForComplex(field, node));
            }
        });

       // dataBlock.put(publishableField.getKey(), nestedDataBlock);
        return dataBlock;
    }

    private JsonNode populateDataBlockForComplex(PublishableField field,
                                                            JsonNode node) {
        switch (field.getCaseField().getFieldTypeDefinition().getType()) {
            case FieldTypeDefinition.YES_OR_NO:
                return booleanNodeOf(node);
            case FieldTypeDefinition.NUMBER:
            case FieldTypeDefinition.MONEY_GBP:
                return intNodeOf(node);
            case FieldTypeDefinition.COMPLEX:
            case FieldTypeDefinition.COLLECTION:
                return node;
            default:
                return TextNode.valueOf(node.asText());
        }

    }

    private boolean isBoolean(String fieldType) {
        return fieldType.equals(FieldTypeDefinition.YES_OR_NO);
    }

    private boolean isNumber(String fieldType) {
        return fieldType.equals(FieldTypeDefinition.NUMBER) || fieldType.equals(FieldTypeDefinition.MONEY_GBP);
    }

    private boolean isComplex(String fieldType) {
        return fieldType.equals(FieldTypeDefinition.COMPLEX)
            || fieldType.equals(FieldTypeDefinition.COLLECTION)
            || fieldType.equals(FieldTypeDefinition.MULTI_SELECT_LIST)
            || fieldType.equals(FieldTypeDefinition.PREDEFINED_COMPLEX_ADDRESS_GLOBAL_UK)
            || fieldType.equals(FieldTypeDefinition.PREDEFINED_COMPLEX_ADDRESS_GLOBAL)
            || fieldType.equals(FieldTypeDefinition.PREDEFINED_COMPLEX_ADDRESS_UK);
    }
}
