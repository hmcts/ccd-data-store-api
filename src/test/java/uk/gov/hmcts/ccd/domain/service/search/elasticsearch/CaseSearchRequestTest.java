package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

class CaseSearchRequestTest {
    private static final String CASE_TYPE_ID = "caseTypeId";
    private static final String FULL_QUERY = "{\"query\":{\"field\":\"value\"},\"sort\":{}}";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CaseSearchRequest caseSearchRequest;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        caseSearchRequest = new CaseSearchRequest(CASE_TYPE_ID, objectMapper.readValue(FULL_QUERY, JsonNode.class));
    }

    @Test
    @DisplayName("should extract query clause")
    void shouldExtractQueryClause() {
        assertThat(caseSearchRequest.getQueryValue(), is("{\"field\":\"value\"}"));
    }

}
