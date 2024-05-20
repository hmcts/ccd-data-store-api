package uk.gov.hmcts.ccd.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ClientContextUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ClientContextUtil.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private ClientContextUtil() {}

    public static String mergeClientContexts(String originalJson, String toBeMergedJson) {
        ObjectNode originalJsonNode = null;
        ObjectNode toBeMergedJsonNode = null;

        try {
            originalJsonNode = (ObjectNode) objectMapper.readTree(originalJson);
        } catch (IOException e) {
            LOG.error("Problem deserialising original JSON: {}", originalJson, e);
        } catch (Exception e) {
            LOG.error("Problem with original JSON: {}", originalJson, e);
        }
        try {
            toBeMergedJsonNode = (ObjectNode) objectMapper.readTree(toBeMergedJson);
        } catch (IOException e) {
            LOG.error("Problem deserialising to-Be-Merged JSON: {}", toBeMergedJson, e);
        } catch (Exception e) {
            LOG.error("Problem with to-Be-Merged JSON: {}", toBeMergedJson, e);
        }

        if (null == originalJsonNode && null == toBeMergedJsonNode) {
            return originalJson;
        } else if (null == toBeMergedJsonNode) {
            return originalJson;
        } else if (null == originalJsonNode) {
            return toBeMergedJson;
        }

        mergeObjectNodes(originalJsonNode, toBeMergedJsonNode);
        return originalJsonNode.toString();    }

    private static void mergeObjectNodes(ObjectNode originalJsonNode, ObjectNode toBeMergedJsonNode) {
        toBeMergedJsonNode.fields().forEachRemaining(entry ->
            originalJsonNode.set(entry.getKey(), entry.getValue())
        );
    }

}
