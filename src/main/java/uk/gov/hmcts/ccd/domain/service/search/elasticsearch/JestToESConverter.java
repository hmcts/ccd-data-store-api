package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.MsearchRequest;
import co.elastic.clients.elasticsearch.core.msearch.RequestItem;
import co.elastic.clients.elasticsearch.core.search.SearchRequestBody;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
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
    private static final Gson GSON = new Gson();

    public static MsearchRequest fromJest(List<Search> jestSearches) {
        List<RequestItem> items = jestSearches.stream()
            .map(JestToESConverter::toRequestItem)
            .collect(Collectors.toList());

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
        String queryJson = normalizeRequest(json);
        SourceConfig sourceConfig = parseSourceJson(extractSource(json));
        try {
            JsonReader reader = Json.createReader(new StringReader(queryJson));
            JsonObject jsonObject = reader.readObject();
            SearchRequestBody requestBody = SearchRequestBody._DESERIALIZER.deserialize(
                mapper.jsonProvider().createParser(new StringReader(jsonObject.toString())),
                mapper
            );
            return SearchRequestBody.of(b -> b
                .query(requestBody.query())
                .sort(requestBody.sort())
                .from(requestBody.from())
                .size(requestBody.size())
                .source(sourceConfig));
        } catch (Exception e) {
            Query query = Query.of(b -> b
                .withJson(new StringReader(queryJson))
            );

            return SearchRequestBody.of(b -> b
                .query(query)
                .source(sourceConfig));
        }
    }

    private static String normalizeRequest(String json) {
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

    private static String extractSource(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject obj = reader.readObject();
            if (obj.containsKey("_source")) {
                return obj.get("_source").toString();
            }
        }
        return "true";
    }

    private static SourceConfig parseSourceJson(String src) {
        if (src.equals("true") || src.equals("false")) {
            return SourceConfig.of(s -> s.fetch(Boolean.valueOf(src)));
        }
        if (src.startsWith("[")) {
            return SourceConfig.of(b -> b.filter(f -> f.includes(parseArray(src))));
        }
        return SourceConfig.of(s -> s.fetch(Boolean.TRUE));
    }

    private static List<String> parseArray(String src) {
        try (JsonReader reader = Json.createReader(new StringReader(src))) {
            return reader.readArray().getValuesAs(v -> v.toString().replace("\"", ""));
        }
    }
}
