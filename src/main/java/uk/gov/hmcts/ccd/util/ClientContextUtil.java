package uk.gov.hmcts.ccd.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Base64;
import java.util.regex.Pattern;

public class ClientContextUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ClientContextUtil.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9+/]*={0,2}$");

    private ClientContextUtil() {

    }

    public static String mergeClientContexts(String originalJsonEncoded, String toBeMergedJsonEncoded) {
        String originalJson = decodeAndRemoveBrackets(originalJsonEncoded);
        LOG.info("originalJson: {}", originalJson);

        String toBeMergedJson = decodeAndRemoveBrackets(toBeMergedJsonEncoded);
        LOG.info("toBeMergedJson: {}", toBeMergedJson);

        ObjectNode originalJsonNode = convertToObjectNode(originalJson);
        ObjectNode toBeMergedJsonNode = convertToObjectNode(toBeMergedJson);

        if (originalJsonNode == null && toBeMergedJsonNode == null) {
            LOG.info("originalJsonNode == null && toBeMergedJsonNode == null - returning originalJson");
            return originalJsonEncoded;
        } else if (toBeMergedJsonNode == null) {
            LOG.info("toBeMergedJsonNode == null - returning originalJson");
            return originalJsonEncoded;
        } else if (originalJsonNode == null) {
            LOG.info("originalJsonNode == null - returning toBeMergedJson");
            return toBeMergedJsonEncoded;
        }

        mergeObjectNodes(originalJsonNode, toBeMergedJsonNode);
        LOG.info("merged into originalJson now: {}",originalJsonNode);
        return encodeToBase64(originalJsonNode.toString());
    }

    public static String decodeFromBase64(String encodedString) {
        return new String(Base64.getDecoder().decode(encodedString));
    }

    public static String encodeToBase64(String decodedString) {
        return Base64.getEncoder().encodeToString(decodedString.getBytes());
    }

    public static boolean isBase64(String input) {
        if (input == null || input.length() % 4 != 0) {
            return false;
        }
        return BASE64_PATTERN.matcher(input).matches();
    }

    public static String removeEnclosingSquareBrackets(String input) {
        if (input.startsWith("[") && input.endsWith("]")) {
            return input.substring(1, input.length() - 1);
        }
        return input;
    }

    private static String decodeAndRemoveBrackets(String encodedJson) {
        if (encodedJson == null) {
            return null;
        }

        String json = removeEnclosingSquareBrackets(encodedJson);
        return isBase64(json) ? decodeFromBase64(json) : json;
    }

    private static ObjectNode convertToObjectNode(String json) {
        if (json == null) {
            return null;
        }

        try {
            return (ObjectNode) objectMapper.readTree(json);
        } catch (IOException e) {
            LOG.error("Problem deserialising JSON: {}", json, e);
        } catch (Exception e) {
            LOG.error("Problem with JSON: {}", json, e);
        }

        return null;
    }

    private static void mergeObjectNodes(ObjectNode originalJsonNode, ObjectNode toBeMergedJsonNode) {
        toBeMergedJsonNode.fields().forEachRemaining(entry ->
            originalJsonNode.set(entry.getKey(), entry.getValue())
        );
    }

}