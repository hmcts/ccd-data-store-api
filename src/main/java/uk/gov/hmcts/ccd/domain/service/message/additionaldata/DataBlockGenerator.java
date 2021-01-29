package uk.gov.hmcts.ccd.domain.service.message.additionaldata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
        if (publishableField.getDisplayContext() != null &&
            publishableField.getDisplayContext().equals(DisplayContext.COMPLEX)) {
            JsonNode subNode = buildNestedLevelDataBlock(publishableField, nestedPublishable, dataBlock, caseDetails);
            dataBlock.put(publishableField.getKey(), subNode);
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
                    JsonNode subNode = buildNestedLevelDataBlock(publishableField, nestedPublishable, dataBlock, caseDetails);
                    dataBlock.put(publishableField.getKey(), subNode);
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

    private JsonNode buildNestedLevelDataBlock(PublishableField publishableField,
                                               List<PublishableField> nestedPublishable,
                                               Map<String, JsonNode> dataBlock,
                                               CaseDetails caseDetails) {

        ObjectNode array = mapper.createObjectNode();

        List<PublishableField> fields = publishableField.filterDirectChildrenFrom(nestedPublishable);
        fields.forEach(field -> {
            JsonNode node = getNestedCaseFieldByPath(caseDetails.getData().get(field.splitPath()[0]),
                StringUtils.substringAfter(field.getPath(), FIELD_SEPARATOR));
            array.put(field.getCaseField().getId(), populateDataBlockForComplex(field, node, nestedPublishable, caseDetails, dataBlock));
        });

        return array;
    }

    private JsonNode populateDataBlockForComplex(PublishableField field,
                                                 JsonNode node,
                                                 List<PublishableField> nestedPublishable,
                                                 CaseDetails caseDetails,
                                                 Map<String, JsonNode> dataBlock) {
        switch (field.getCaseField().getFieldTypeDefinition().getType()) {
            case FieldTypeDefinition.YES_OR_NO:
                return booleanNodeOf(node);
            case FieldTypeDefinition.NUMBER:
            case FieldTypeDefinition.MONEY_GBP:
                return intNodeOf(node);
            case FieldTypeDefinition.COMPLEX:
            case FieldTypeDefinition.COLLECTION:
                return buildNestedLevelDataBlock(field, nestedPublishable, dataBlock, caseDetails);
            default:
                return TextNode.valueOf(node.asText());
        }

    }
}
