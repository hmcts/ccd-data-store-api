package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

class CrossCaseTypeSearchRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("should throw exception when query node not found")
    void shouldThrowExceptionWhenQueryNodeNotFound() {
        String query = "{}";
        assertThrows(BadSearchRequest.class, () -> new CrossCaseTypeSearchRequest(Collections.singletonList("caseType"),
                                                                                  objectMapper.readValue(query, JsonNode.class)));
    }

}
