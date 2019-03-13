package uk.gov.hmcts.ccd.domain.service.common;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Common Object mapper service for serialising/de-serialising objects.
 */
public interface ObjectMapperService {

    <T> T convertStringToObject(String string, Class<T> classType);

    String convertObjectToString(Object object);

    JsonNode convertObjectToJsonNode(Object object);

    Map<String, JsonNode> convertJsonNodeToMap(JsonNode node);

    JsonNode createEmptyJsonNode();
}
