package uk.gov.hmcts.ccd.config;

import tools.jackson.core.JacksonException;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.POJONode;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CollectionSanitiser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.domain.model.common.CaseFieldPathUtils.getNestedCaseFieldByPath;

public final class JacksonUtils {

    public static final String DATA = "data";

    private JacksonUtils() {
    }

    public static final JsonFactory jsonFactory = JsonFactory.builderWithJackson2Defaults()
        // Change per-factory setting to prevent use of `String.intern()` on symbols
        .disable(TokenStreamFactory.Feature.INTERN_PROPERTY_NAMES)
        .build();

    public static final ObjectMapper MAPPER = JsonMapper.builder(jsonFactory)
        .configureForJackson2()
        .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, true)
        .configure(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES, true)
        .configure(JsonReadFeature.ALLOW_SINGLE_QUOTES, true)
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

    public static Map<String, JsonNode> convertJsonNode(String from) throws JacksonException {
        return MAPPER.readValue(from, new TypeReference<HashMap<String, JsonNode>>() {
        });
    }

    public static String writeValueAsString(Map<String, JsonNode> caseData) throws JacksonException {
        return JacksonUtils.MAPPER.writeValueAsString(caseData);
    }

    /**
     * Coerces a node to text using Jackson 2's {@code JsonNode.asText()} semantics.
     *
     * <p>Jackson 3's replacement, {@code asString()}, rejects container and missing nodes,
     * while CCD previously relied on Jackson 2 returning an empty string for those nodes.
     */
    public static String asText(@NonNull JsonNode jsonNode) {
        if (jsonNode.isContainer() || jsonNode.isMissingNode()) {
            return "";
        }
        if (jsonNode.isNull()) {
            return "null";
        }
        if (jsonNode.isPojo()) {
            Object value = ((POJONode) jsonNode).getPojo();
            return value == null ? "null" : value.toString();
        }
        return jsonNode.asString(null);
    }

    /**
     * Coerces a node to text using Jackson 2's {@code JsonNode.asText(defaultValue)} semantics.
     */
    public static String asText(@NonNull JsonNode jsonNode, String defaultValue) {
        if (jsonNode.isNull() || jsonNode.isMissingNode()) {
            return defaultValue;
        }
        if (jsonNode.isPojo()) {
            Object value = ((POJONode) jsonNode).getPojo();
            return value == null ? defaultValue : value.toString();
        }
        if (jsonNode.isContainer()) {
            return "";
        }
        return jsonNode.asString(defaultValue);
    }

    /**
     * Creates a Jackson 3 string node while preserving Jackson 2's effective JSON-null
     * representation for the formerly permitted {@code new TextNode(null)}.
     */
    public static JsonNode stringOrNullNode(String value) {
        return value == null ? MAPPER.getNodeFactory().nullNode() : MAPPER.getNodeFactory().stringNode(value);
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
            return MAPPER.getNodeFactory().stringNode(value);
        } else {
            String first = pathElements.remove(0);
            return MAPPER.getNodeFactory().objectNode().set(first, addNode(pathElements, value));
        }
    }

    public static void merge(Map<String, JsonNode> mergeFrom, Map<String, JsonNode> mergeInto) {

        for (String key : mergeFrom.keySet()) {
            JsonNode value = mergeFrom.get(key);
            if (!mergeInto.containsKey(key) || mergeInto.get(key).isNull()) {
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

        Iterator<String> fieldNames = updateNode.propertyNames().iterator();
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

    public static String getValueFromPath(final String path, final Map<String, JsonNode> dataMap) {
        return getValueFromPath(path, convertValueJsonNode(dataMap));
    }

    public static String getValueFromPath(final String path, final JsonNode jsonNode) {
        String returnValue = null;
        if (path.contains(".")) {
            String pathStart = path.substring(0, path.indexOf("."));
            String truncatedPath = path.substring(path.indexOf(".") + 1);

            JsonNode foundNodeValue = getNestedCaseFieldByPath(jsonNode, pathStart);
            if (foundNodeValue != null) {
                if (foundNodeValue.isArray()) {
                    returnValue = getValueFromArray(truncatedPath, foundNodeValue);
                } else {
                    returnValue = getValueFromPath(truncatedPath, foundNodeValue);
                }
            }
        } else {
            JsonNode foundNodeValue = getNestedCaseFieldByPath(jsonNode, path);
            if (foundNodeValue != null) {
                returnValue = getValue(foundNodeValue);
            }
        }

        return returnValue;
    }


    private static String getValueFromArray(String path, JsonNode jsonNode) {
        ArrayNode arrayNode = (ArrayNode)jsonNode;
        final var arrayIndex = path.contains(".") ? path.substring(0, path.indexOf(".")) : path;
        final var truncatedPath = path.contains(".") ? path.substring(path.indexOf(".") + 1) : "";
        if (StringUtils.isNumeric(arrayIndex)) {
            JsonNode foundJsonNode = arrayNode.get(Integer.parseInt(arrayIndex));
            if (foundJsonNode != null) {
                // check if need to auto-apply collection processing
                if (!truncatedPath.startsWith(CollectionSanitiser.VALUE)
                    && !truncatedPath.startsWith(CollectionSanitiser.ID)
                    && foundJsonNode.has(CollectionSanitiser.VALUE)
                ) {
                    // adjust node to accommodate for use of value property
                    foundJsonNode = foundJsonNode.get(CollectionSanitiser.VALUE);
                }

                if (StringUtils.isNotBlank(truncatedPath)) {
                    return getValueFromPath(truncatedPath, foundJsonNode);
                } else {
                    return getValue(foundJsonNode);
                }
            }
        }
        return null;
    }

    private static String getValue(@NonNull JsonNode jsonNode) {
        Iterator<JsonNode> values = jsonNode.iterator();
        JsonNode valueNode = values.hasNext() ? values.next() : jsonNode;
        if (valueNode instanceof IntNode) {
            return valueNode.toString();
        }
        return valueNode.isString() ? valueNode.stringValue() : null;
    }

    public static Map<String, JsonNode> convertValueInDataField(Map<String, JsonNode> from) {
        Map<String, JsonNode> dataNode = new HashMap<>();
        dataNode.put(DATA, MAPPER.valueToTree(from));
        return dataNode;
    }
}
