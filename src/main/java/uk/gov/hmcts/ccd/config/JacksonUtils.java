package uk.gov.hmcts.ccd.config;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;


public final class JacksonUtils {

    private JacksonUtils(){

    }

    public static final JsonFactory jsonFactory = JsonFactory.builder()
        // Change per-factory setting to prevent use of `String.intern()` on symbols
        .disable(JsonFactory.Feature.INTERN_FIELD_NAMES)
        .build();

    public static final ObjectMapper MAPPER = JsonMapper.builder(jsonFactory)
        .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, true)
        .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
        .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
        .build();


    public static Map<String, JsonNode> convertValue(Object from) {
        return MAPPER.convertValue(from, new TypeReference<HashMap<String, JsonNode>>() {
        });
    }

    public static JsonNode convertValueJsonNode(Object from) {
        return MAPPER.convertValue(from, JsonNode.class);
    }

    public static TypeReference<HashMap<String, JsonNode>> getHashMapTypeReference() {
        return new TypeReference<HashMap<String, JsonNode>>() {
        };
    }

    public static Map<String, Object> convertJsonNode(Object from) {
        return MAPPER.convertValue(from, new TypeReference<HashMap<String, Object>>() {
        });
    }

    /**
     * Builds a JsonNode from a path and puts the value to the last node.
     * @param path Eg. "OrganisationPolicyField.OrgPolicyCaseAssignedRole"
     * @param value eg. "[Claimant]"
     * @return JsonNode where it represents the following structure:
     *     {
     *         "OrganisationPolicyField": {
     *             "OrgPolicyCaseAssignedRole": "[Claimant]"
     *         }
     *     }
     */
    public static JsonNode buildFromDottedPath(String path, String value) {
        List<String> pathElements = Arrays.stream(path.split("\\.")).collect(toList());
        return addNode(pathElements, value);
    }

    private static JsonNode addNode(List<String> pathElements, String value) {
        if (pathElements.isEmpty()) {
            return MAPPER.getNodeFactory().textNode(value);
        } else {
            String first = pathElements.remove(0);
            return MAPPER.getNodeFactory().objectNode().set(first, addNode(pathElements, value));
        }
    }

    public static void merge(Map<String, JsonNode> mergeFrom, Map<String, JsonNode> mergeInto) {

        for (String key : mergeFrom.keySet()) {
            JsonNode value = mergeFrom.get(key);
            if (!mergeInto.containsKey(key)) {
                mergeInto.put(key, value);
            } else {
                mergeInto.put(key, merge(mergeInto.get(key), value));
            }
        }
    }

    private static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {
        // If the top level node is an @ArrayNode we do not update
        if (mainNode.isArray()) {
            return mainNode;
        }

        Iterator<String> fieldNames = updateNode.fieldNames();
        while (fieldNames.hasNext()) {
            String updatedFieldName = fieldNames.next();
            JsonNode valueToBeUpdated = mainNode.get(updatedFieldName);
            JsonNode updatedValue = updateNode.get(updatedFieldName);

            // If the node is an @ArrayNode we do not update
            if (valueToBeUpdated != null && valueToBeUpdated.isArray()) {
                return mainNode;
            } else if (valueToBeUpdated != null && valueToBeUpdated.isObject()) {
                merge(valueToBeUpdated, updatedValue);
            } else {
                if (mainNode instanceof ObjectNode) {
                    ((ObjectNode) mainNode).replace(updatedFieldName, updatedValue);
                }
            }
        }
        return mainNode;
    }
}
