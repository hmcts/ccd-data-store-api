package uk.gov.hmcts.ccd.domain.service.common;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.util.Map;

/**
 * Common Object mapper service for serialising/de-serialising objects.
 */
@Service
public class DefaultObjectMapperService implements ObjectMapperService {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory();
    private final ObjectMapper objectMapper;

    @Autowired
    public DefaultObjectMapperService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T convertStringToObject(String string, Class<T> classType) {
        try {
            return objectMapper.readValue(string, classType);
        } catch (Exception e) {
            throw new ServiceException("Unable to map JSON string to object", e);
        }
    }

    @Override
    public String convertObjectToString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JacksonException e) {
            throw new ServiceException("Unable map object to JSON string", e);
        }
    }

    @Override
    public JsonNode convertObjectToJsonNode(Object object) {
        return objectMapper.valueToTree(object);
    }

    @Override
    public Map<String, JsonNode> convertJsonNodeToMap(JsonNode node) {
        try {
            return objectMapper.convertValue(node, JacksonUtils.getHashMapTypeReference());
        } catch (IllegalArgumentException | JacksonException e) {
            throw new ServiceException("Unable to convert JSON node to map", e);
        }
    }

    @Override
    public JsonNode createEmptyJsonNode() {
        return JSON_NODE_FACTORY.objectNode();
    }
}
