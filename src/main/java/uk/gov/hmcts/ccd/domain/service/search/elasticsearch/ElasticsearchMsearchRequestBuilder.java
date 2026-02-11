package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import co.elastic.clients.elasticsearch.core.msearch.RequestItem;
import co.elastic.clients.elasticsearch.core.search.SearchRequestBody;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import java.io.StringReader;

public final class ElasticsearchMsearchRequestBuilder {

    private static final String INCLUDES = "includes";
    private static final JsonpMapper MAPPER = new JacksonJsonpMapper();
    private static final String SOURCE = "_source";

    private ElasticsearchMsearchRequestBuilder() {
    }

    public static RequestItem createRequestItem(String indexName, String bodyJson) {
        if (bodyJson == null || bodyJson.isBlank()) {
            throw new IllegalArgumentException("Search request body is empty");
        }

        SearchRequestBody body = parseBody(bodyJson);
        return new RequestItem.Builder()
            .header(h -> {
                if (indexName != null && !indexName.isBlank()) {
                    h.index(indexName);
                }
                return h;
            })
            .body(body)
            .build();
    }

    private static SearchRequestBody parseBody(String json) {
        String normalizedBody = normalizeSourceSyntax(json);
        try {
            return SearchRequestBody._DESERIALIZER.deserialize(
                MAPPER.jsonProvider().createParser(new StringReader(normalizedBody)),
                MAPPER
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse search request body", e);
        }
    }

    private static String normalizeSourceSyntax(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject body = reader.readObject();
            if (!body.containsKey(SOURCE)) {
                return json;
            }

            JsonValue source = body.get(SOURCE);
            if (source.getValueType() != JsonValue.ValueType.ARRAY) {
                return json;
            }

            JsonArray sourceIncludes = source.asJsonArray();
            JsonObject normalizedSource = Json.createObjectBuilder()
                .add(INCLUDES, sourceIncludes)
                .build();

            JsonObjectBuilder normalizedBody = Json.createObjectBuilder();
            body.forEach((key, value) -> {
                if (SOURCE.equals(key)) {
                    normalizedBody.add(key, normalizedSource);
                } else {
                    normalizedBody.add(key, value);
                }
            });

            return normalizedBody.build().toString();
        }
    }
}
