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
        ObjectNode originalJsonNode = null;
        ObjectNode toBeMergedJsonNode = null;
        String originalJson = null;
        String toBeMergedJson = null;

        if (isBase64(originalJsonEncoded)) {
            try {
                originalJson = decodeFromBase64(originalJsonEncoded);
            } catch (Exception e) {
                LOG.error("Problem decoding original encoded JSON: {}", originalJsonEncoded, e);
            }
        } else {
            originalJson = originalJsonEncoded;
        }
        if (isBase64(toBeMergedJsonEncoded)) {
            try {
                toBeMergedJson = decodeFromBase64(toBeMergedJsonEncoded);
            } catch (Exception e) {
                LOG.error("Problem decoding toBeMerged encoded JSON: {}", toBeMergedJsonEncoded, e);
            }
        } else {
            toBeMergedJson = toBeMergedJsonEncoded;
        }

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
            return originalJsonEncoded;
        } else if (null == toBeMergedJsonNode) {
            return originalJsonEncoded;
        } else if (null == originalJsonNode) {
            return toBeMergedJsonEncoded;
        }

        mergeObjectNodes(originalJsonNode, toBeMergedJsonNode);
        return encodeToBase64(originalJsonNode.toString());
    }

    private static void mergeObjectNodes(ObjectNode originalJsonNode, ObjectNode toBeMergedJsonNode) {
        toBeMergedJsonNode.fields().forEachRemaining(entry ->
            originalJsonNode.set(entry.getKey(), entry.getValue())
        );
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
}
