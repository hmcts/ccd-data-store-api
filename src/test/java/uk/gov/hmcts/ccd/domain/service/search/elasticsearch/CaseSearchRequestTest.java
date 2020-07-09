package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

class CaseSearchRequestTest {
    private static final String CASE_TYPE_ID = "caseTypeId";
    private static final String FULL_QUERY = "{\"query\":{\"field\":\"value\"},\"sort\":{}}";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CaseSearchRequest caseSearchRequest;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        caseSearchRequest = new CaseSearchRequest(CASE_TYPE_ID, new ElasticsearchRequest(objectMapper.readValue(FULL_QUERY, JsonNode.class)));
    }

    @Test
    @DisplayName("should extract query clause")
    void shouldExtractQueryClause() {
        assertThat(caseSearchRequest.getQueryValue(), is("{\"field\":\"value\"}"));
    }

    @Test
    @DisplayName("should throw exception when query node not found")
    void shouldThrowExceptionWhenQueryNodeNotFound() {
        String query = "{}";
        assertThrows(BadSearchRequest.class, () -> new CaseSearchRequest(CASE_TYPE_ID,
            new ElasticsearchRequest(objectMapper.readValue(query, JsonNode.class))));
    }
}
