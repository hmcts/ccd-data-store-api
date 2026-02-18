package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.elastic.clients.elasticsearch.core.MsearchRequest;
import co.elastic.clients.elasticsearch.core.msearch.RequestItem;
import io.searchbox.core.Search;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JestToESConverterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fromJest_shouldConvertSingleSearch() {
        Search jestSearch = new Search.Builder("{\"query\":{\"match_all\":{}},\"size\":10}")
            .addIndex("cases")
            .build();

        MsearchRequest request = JestToESConverter.fromJest(List.of(jestSearch));

        assertNotNull(request);
        assertEquals(1, request.searches().size());

        RequestItem item = request.searches().get(0);
        assertEquals("cases", item.header().index().getFirst());
        assertNotNull(item.body());
        assertEquals(10, item.body().size());
        assertNotNull(item.body().query());
    }

    @Test
    void toRequestItem_shouldThrowOnEmptyBody() {
        Search jestSearch = new Search.Builder("")
            .build();

        assertThrows(IllegalArgumentException.class, () ->
            JestToESConverter.fromJest(List.of(jestSearch))
        );
    }

    @Test
    void toRequestItem_shouldThrowOnNullBody() {
        Search jestSearch = new Search.Builder(null)
            .build();

        assertThrows(IllegalArgumentException.class, () ->
            JestToESConverter.fromJest(List.of(jestSearch))
        );
    }

    @Test
    void fromJest_shouldPreserveSearchAfterInConvertedBody() throws Exception {
        Search jestSearch = new Search.Builder("""
            {
              "query": {"match_all": {}},
              "sort": [{"id": {"order": "asc"}}],
              "search_after": [123456789, "case-2"],
              "size": 25
            }
            """)
            .addIndex("cases")
            .build();

        MsearchRequest request = JestToESConverter.fromJest(List.of(jestSearch));
        RequestItem item = request.searches().get(0);

        StringWriter writer = new StringWriter();
        var mapper = new JacksonJsonpMapper();
        var generator = mapper.jsonProvider().createGenerator(writer);
        item.body().serialize(generator, mapper);
        generator.close();

        JsonNode bodyJson = objectMapper.readTree(writer.toString());

        assertAll(
            () -> assertTrue(bodyJson.has("query")),
            () -> assertTrue(bodyJson.has("sort")),
            () -> assertTrue(bodyJson.has("size")),
            () -> assertTrue(bodyJson.has("search_after")),
            () -> assertEquals(123456789L, bodyJson.get("search_after").get(0).asLong()),
            () -> assertEquals("case-2", bodyJson.get("search_after").get(1).asText())
        );
    }

    @Test
    void fromJest_shouldPreserveSearchAfterWhenSourceIsArray() throws Exception {
        Search jestSearch = new Search.Builder("""
            {
              "_source": ["data", "reference"],
              "query": {"match_all": {}},
              "sort": [{"id": {"order": "asc"}}],
              "search_after": [987654321, "case-3"],
              "size": 25
            }
            """)
            .addIndex("cases")
            .build();

        MsearchRequest request = JestToESConverter.fromJest(List.of(jestSearch));
        RequestItem item = request.searches().get(0);

        StringWriter writer = new StringWriter();
        var mapper = new JacksonJsonpMapper();
        var generator = mapper.jsonProvider().createGenerator(writer);
        item.body().serialize(generator, mapper);
        generator.close();

        JsonNode bodyJson = objectMapper.readTree(writer.toString());

        assertAll(
            () -> assertTrue(bodyJson.has("_source")),
            () -> assertTrue(bodyJson.has("search_after")),
            () -> assertEquals(987654321L, bodyJson.get("search_after").get(0).asLong()),
            () -> assertEquals("case-3", bodyJson.get("search_after").get(1).asText())
        );
    }
}
