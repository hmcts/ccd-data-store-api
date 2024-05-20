package uk.gov.hmcts.ccd.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ClientContextUtil {
    public static Logger LOG = LoggerFactory.getLogger(ClientContextUtil.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String mergeClientContexts(String jsonA, String jsonB) {
        ObjectNode nodeA = null;
        ObjectNode nodeB = null;

        try {
            nodeA = (ObjectNode) objectMapper.readTree(jsonA);
        } catch (IOException e) {
            LOG.error("Problem deserialising jsonA: {}", jsonA, e);
        } catch (Exception e) {
            LOG.error("Problem with jsonA: {}", jsonA, e);
        }
        try {
            nodeB = (ObjectNode) objectMapper.readTree(jsonB);
        } catch (IOException e) {
            LOG.error("Problem deserialising jsonB: {}", jsonB, e);
        } catch (Exception e) {
            LOG.error("Problem with jsonB: {}", jsonB, e);
        }

        if (null == nodeA && null == nodeB) {
            return jsonA;
        } else if (null == nodeB) {
            return jsonA;
        } else if (null == nodeA) {
            return jsonB;
        }

        mergeObjectNodes(nodeA, nodeB);
        return nodeA.toString();
    }

    private static void mergeObjectNodes(ObjectNode targetNode, ObjectNode sourceNode) {
        sourceNode.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode value = entry.getValue();
            targetNode.set(fieldName, value);
        });
    }

}
