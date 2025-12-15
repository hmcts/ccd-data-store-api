package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import co.elastic.clients.elasticsearch.core.MsearchRequest;
import co.elastic.clients.elasticsearch.core.msearch.RequestItem;
import io.searchbox.core.Search;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JestToESConverterTest {

    @Test
    void fromJest_shouldConvertSingleSearch() {
        Search jestSearch = new Search.Builder("{\"query\":{\"match_all\":{}},\"size\":10}")
            .addIndex("cases")
            .build();

        MsearchRequest request = JestToESConverter.fromJest(List.of(jestSearch));

        assertNotNull(request);
        assertEquals(1, request.searches().size());

        RequestItem item = request.searches().get(0);
        assertEquals("cases", item.header().index());
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
}
