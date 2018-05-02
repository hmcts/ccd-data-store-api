package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class SecurityClassificationUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SecurityClassificationUtils.class);

    private static final String ID = "id";
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

    public static JsonNode getDataClassificationForData(JsonNode data, Iterator<JsonNode> dataIterator) {
        Boolean found = false;
        JsonNode dataClassificationValue = null;
        while (dataIterator.hasNext() && !found) {
            dataClassificationValue = dataIterator.next();
            if (null != dataClassificationValue.get(ID)
                && dataClassificationValue.get(ID).equals(data.get(ID))) {
                found = true;
            }
        }
        return found ? dataClassificationValue : JSON_NODE_FACTORY.nullNode();
    }
}
