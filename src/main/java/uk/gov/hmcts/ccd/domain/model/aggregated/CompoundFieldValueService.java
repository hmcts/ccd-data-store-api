package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

/**
 * This service sorts value of compound field based on given order of compound fields
 */
@Named
@Singleton
public class CompoundFieldValueService {

    public static JsonNode getSortedValue(final CommonField caseField, JsonNode data) {
        if (data != null) {
            if (caseField.isCompound()) {
                List<CaseField> children = caseField.getFieldType().getChildren();
                children.forEach(childField -> {
                    if (childField.isCollectionFieldType() || caseField.isCollectionFieldType()) {
                        for (int index = 0; index < data.size(); index++) {
                            JsonNode newOrderData = getSortedValue(childField, data.get(index).get("value").get(childField.getId()));
                            if (newOrderData != null) {
                                ((ObjectNode) data.get(index).get("value")).set(childField.getId(), newOrderData);
                            }
                        }
                    } else if (childField.isComplexFieldType()) {
                        JsonNode newOrderData = getSortedValue(childField, data.get(childField.getId()));
                        if (newOrderData != null) {
                            ((ObjectNode) data).set(childField.getId(), newOrderData);
                        }
                    }
                });
                return getOrderedData(caseField, data, children);
            }
        }
        return null;
    }

    private static JsonNode getOrderedData(final CommonField caseField, final JsonNode data, final List<CaseField> children) {
        if (caseField.isCollectionFieldType()) {
            ArrayNode orderedData = instance.arrayNode();
            for (int index = 0; index < data.size(); index++) {
                JsonNode elem = data.get(index);
                ObjectNode elemNode = getElemNode(children, elem);
                orderedData.add(elemNode);
            }
            return orderedData;
        } else {
            ObjectNode newOrderData = getValue(children, data);
            return newOrderData;
        }
    }

    private static ObjectNode getElemNode(final List<CaseField> children, final JsonNode elem) {
        ObjectNode elemNode = instance.objectNode();
        JsonNode colData = elem.get("value");
        ObjectNode valueNode = getValue(children, colData);
        elemNode.set("id", elem.get("id"));
        elemNode.set("value", valueNode);
        return elemNode;
    }

    private static ObjectNode getValue(final List<CaseField> children, final JsonNode colData) {
        ObjectNode valueNode = instance.objectNode();
        for (CaseField field : children) {
            if (colData.get(field.getId()) != null) {
                valueNode.set(field.getId(), colData.get(field.getId()));
            }
        }
        return valueNode;
    }
}
