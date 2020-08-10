package uk.gov.hmcts.ccd.domain.model.search.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ElasticsearchRequestTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldReturnTrueWhenSourceIsProvided() throws JsonProcessingException {
        JsonNode queryNode = queryAsJsonNode("{\"_source\":[\"data.Field\"],\"query\":{\"match_all\": {}}}");
        ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(queryNode);

        assertTrue(elasticsearchRequest.hasSource());
    }

    @Test
    void shouldReturnFalseWhenSourceIsEmpty() throws JsonProcessingException {
        JsonNode queryNode = queryAsJsonNode("{\"_source\":[],\"query\":{\"match_all\": {}}}");
        ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(queryNode);

        assertFalse(elasticsearchRequest.hasSource());
    }

    @Test
    void shouldReturnFalseWhenSourceOnlyHasWildcard() throws JsonProcessingException {
        JsonNode queryNode = queryAsJsonNode("{\"_source\":[\"*\"],\"query\":{\"match_all\": {}}}");
        ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(queryNode);

        assertFalse(elasticsearchRequest.hasSource());
    }

    @Test
    void shouldReturnRequestedFieldsAsCaseFieldIds() throws JsonProcessingException {
        JsonNode queryNode = queryAsJsonNode("{\"_source\":[\"data.CaseDataField\",\"reference\",\"state\","
                                             + "\"data.OtherCaseDataField\"],\"query\":{\"match_all\": {}}}");
        ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(queryNode);

        List<String> requestedFields = elasticsearchRequest.getRequestedFields();

        assertAll(
            () -> assertThat(requestedFields.size(), is(4)),
            () -> assertThat(requestedFields.get(0), is("CaseDataField")),
            () -> assertThat(requestedFields.get(1), is("[CASE_REFERENCE]")),
            () -> assertThat(requestedFields.get(2), is("[STATE]")),
            () -> assertThat(requestedFields.get(3), is("OtherCaseDataField"))
        );
    }

    @Test
    void shouldOnlyReturnRequestedMetadataFieldsThatExist() throws JsonProcessingException {
        JsonNode queryNode = queryAsJsonNode("{\"_source\":[\"reference\",\"INVALID\"],\"query\":{\"match_all\": {}}}");
        ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(queryNode);

        List<String> requestedFields = elasticsearchRequest.getRequestedFields();

        assertAll(
            () -> assertThat(requestedFields.size(), is(1)),
            () -> assertThat(requestedFields.get(0), is("[CASE_REFERENCE]"))
        );
    }

    @Test
    void shouldReturnNoRequestedFieldsWhenOnlyWildcardIsRequested() throws JsonProcessingException {
        JsonNode queryNode = queryAsJsonNode("{\"_source\":[\"*\"],\"query\":{\"match_all\": {}}}");
        ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(queryNode);

        List<String> requestedFields = elasticsearchRequest.getRequestedFields();

        assertTrue(requestedFields.isEmpty());
    }

    private JsonNode queryAsJsonNode(String query) throws JsonProcessingException {
        return mapper.readTree(query);
    }
}
