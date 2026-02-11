package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import co.elastic.clients.elasticsearch.core.MsearchRequest;
import co.elastic.clients.elasticsearch.core.msearch.RequestItem;
import co.elastic.clients.elasticsearch.core.search.SearchRequestBody;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.google.gson.Gson;
import io.searchbox.core.Search;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import java.io.StringReader;
import java.util.List;

public class JestToESConverter {

    private static final JsonpMapper mapper = new JacksonJsonpMapper();
    private static final Gson GSON = new Gson();
    private static final String INCLUDES = "includes";
    private static final String SOURCE = "_source";

    public static MsearchRequest fromJest(List<Search> jestSearches) {
        List<RequestItem> items = jestSearches.stream()
            .map(JestToESConverter::toRequestItem)
            .toList();

        return new MsearchRequest.Builder()
            .searches(items)
            .build();
    }

    private static RequestItem toRequestItem(Search jestSearch) {
        String bodyJson = jestSearch.getData(GSON);
        if (bodyJson == null || bodyJson.isBlank()) {
            throw new IllegalArgumentException("Jest Search body is empty");
        }

        SearchRequestBody body = parseBody(bodyJson);

        return new RequestItem.Builder()
            .header(h -> {
                if (jestSearch.getIndex() != null) {
                    h.index(jestSearch.getIndex());
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
                mapper.jsonProvider().createParser(new StringReader(normalizedBody)),
                mapper
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Jest Search body", e);
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
