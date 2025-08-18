package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import co.elastic.clients.elasticsearch.core.MsearchRequest;
import co.elastic.clients.elasticsearch.core.msearch.RequestItem;
import co.elastic.clients.elasticsearch.core.search.SearchRequestBody;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.google.gson.Gson;
import io.searchbox.core.Search;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;

import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

public class JestToESConverter {

    private static final JsonpMapper mapper = new JacksonJsonpMapper();

    public static MsearchRequest fromJest(List<Search> jestSearches) {
        List<RequestItem> items = jestSearches.stream()
            .map(JestToESConverter::toRequestItem)
            .collect(Collectors.toList());

        return new MsearchRequest.Builder()
            .searches(items)
            .build();
    }

    private static SearchRequestBody parseBody(String json) {
        JsonReader reader = Json.createReader(new StringReader(json));
        JsonObject jsonObject = reader.readObject();

        return SearchRequestBody._DESERIALIZER.deserialize(
            mapper.jsonProvider().createParser(new StringReader(jsonObject.toString())),
            mapper
        );
    }

    private static String normalizeSourceJakarta(String json) {
        try (jakarta.json.JsonReader reader = jakarta.json.Json.createReader(new StringReader(json))) {
            jakarta.json.JsonObject obj = reader.readObject();
            JsonObjectBuilder builder = Json.createObjectBuilder();
            obj.forEach((k, v) -> {
                if (!"_source".equals(k)) {
                    builder.add(k, v);
                }
            });
            return builder.build().toString();
        }
    }

    private static RequestItem toRequestItem(Search jestSearch) {
        String bodyJson = jestSearch.getData(new Gson());
        if (bodyJson == null || bodyJson.isBlank()) {
            throw new IllegalArgumentException("Jest Search body is empty");
        }
        SearchRequestBody body = parseBody(normalizeSourceJakarta(bodyJson));

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
}
