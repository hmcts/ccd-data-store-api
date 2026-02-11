package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import co.elastic.clients.elasticsearch.core.msearch.RequestItem;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElasticsearchMsearchRequestBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createRequestItem_shouldConvertSingleSearchBody() {
        RequestItem item = ElasticsearchMsearchRequestBuilder.createRequestItem(
            "cases",
            "{\"query\":{\"match_all\":{}},\"size\":10}"
        );

        assertNotNull(item);
        assertEquals("cases", item.header().index().getFirst());
        assertNotNull(item.body());
        assertEquals(10, item.body().size());
        assertNotNull(item.body().query());
    }

    @Test
    void createRequestItem_shouldThrowOnEmptyBody() {
        assertThrows(IllegalArgumentException.class, () ->
            ElasticsearchMsearchRequestBuilder.createRequestItem("cases", "")
        );
    }

    @Test
    void createRequestItem_shouldPreserveSearchAfterInConvertedBody() throws Exception {
        RequestItem item = ElasticsearchMsearchRequestBuilder.createRequestItem(
            "cases",
            """
                {
                  "query": {"match_all": {}},
                  "sort": [{"id": {"order": "asc"}}],
                  "search_after": [123456789, "case-2"],
                  "size": 25
                }
                """
        );

        JsonNode bodyJson = toBodyJson(item);

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
    void createRequestItem_shouldPreserveSearchAfterWhenSourceIsArray() throws Exception {
        RequestItem item = ElasticsearchMsearchRequestBuilder.createRequestItem(
            "cases",
            """
                {
                  "_source": ["data", "reference"],
                  "query": {"match_all": {}},
                  "sort": [{"id": {"order": "asc"}}],
                  "search_after": [987654321, "case-3"],
                  "size": 25
                }
                """
        );

        JsonNode bodyJson = toBodyJson(item);

        assertAll(
            () -> assertTrue(bodyJson.has("_source")),
            () -> assertTrue(bodyJson.has("search_after")),
            () -> assertEquals(987654321L, bodyJson.get("search_after").get(0).asLong()),
            () -> assertEquals("case-3", bodyJson.get("search_after").get(1).asText())
        );
    }

    private JsonNode toBodyJson(RequestItem item) throws Exception {
        StringWriter writer = new StringWriter();
        var mapper = new JacksonJsonpMapper();
        var generator = mapper.jsonProvider().createGenerator(writer);
        item.body().serialize(generator, mapper);
        generator.close();
        return objectMapper.readTree(writer.toString());
    }
}
