package uk.gov.hmcts.ccd.domain.model.search.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.DATA_CLASSIFICATION_COL;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.CASE_REFERENCE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.CASE_TYPE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.CREATED_DATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.JURISDICTION;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.LAST_MODIFIED_DATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.LAST_STATE_MODIFIED_DATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.SECURITY_CLASSIFICATION;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.STATE;

class ElasticsearchRequestTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    void hasSourceFieldsShouldReturnFalseWhenSourceIsMissing() throws JsonProcessingException {
        JsonNode queryNode = queryAsJsonNode("{\"query\":{\"match_all\": {}}}");
        ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(queryNode);

        assertFalse(elasticsearchRequest.hasSourceFields());
    }

    @Test
    void hasSourceFieldsShouldReturnTrueWhenSourceIsProvided() throws JsonProcessingException {
        JsonNode queryNode = queryAsJsonNode("{\"_source\":[\"data.Field\"],\"query\":{\"match_all\": {}}}");
        ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(queryNode);

        assertTrue(elasticsearchRequest.hasSourceFields());
    }

    @Test
    void hasSourceFieldsShouldReturnFalseWhenSourceIsEmpty() throws JsonProcessingException {
        JsonNode queryNode = queryAsJsonNode("{\"_source\":[],\"query\":{\"match_all\": {}}}");
        ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(queryNode);

        assertFalse(elasticsearchRequest.hasSourceFields());
    }

    @Test
    void hasSourceFieldsShouldReturnFalseWhenSourceOnlyHasWildcard() throws JsonProcessingException {
        JsonNode queryNode = queryAsJsonNode("{\"_source\":[\"*\"],\"query\":{\"match_all\": {}}}");
        ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(queryNode);

        assertFalse(elasticsearchRequest.hasSourceFields());
    }

    @Test
    void hasSourceFieldsShouldReturnFalseWhenSourceIsFalse() throws JsonProcessingException {
        JsonNode queryNode = queryAsJsonNode("{\"_source\": false,\"query\":{\"match_all\": {}}}");
        ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(queryNode);

        assertFalse(elasticsearchRequest.hasSourceFields());
    }

    @Test
    void hasSourceFieldsShouldReturnFalseWhenSourceIsTrue() throws JsonProcessingException {
        JsonNode queryNode = queryAsJsonNode("{\"_source\": true,\"query\":{\"match_all\": {}}}");
        ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(queryNode);

        assertFalse(elasticsearchRequest.hasSourceFields());
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

    @Test
    void shouldHandleWrappedQueryFormatWithSupplementaryData() throws JsonProcessingException {
        JsonNode queryNode = queryAsJsonNode("{\"native_es_query\":{\"_source\":[\"data.CaseDataField\",\"reference\",\"state\","
            + "\"data.OtherCaseDataField\"],\"query\":{\"match_all\": {}}},\"supplementary_data\":[\"SupDataField\"]}");
        ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(queryNode);

        assertAll(
            () -> assertThat(elasticsearchRequest.getQuery().toString(), is("{\"match_all\":{}}")),
            () -> assertThat(elasticsearchRequest.getRequestedFields().size(), is(4)),
            () -> assertThat(elasticsearchRequest.hasSupplementaryData(), is(true)),
            () -> assertThat(elasticsearchRequest.getSupplementaryData().toString(), is("[\"SupDataField\"]"))
        );
    }

    @Test
    void shouldSetNativeQueryWhenProvidedInWrapperObject() throws Exception {
        String query = "{\"native_es_query\":{\"_source\":[\"data.name\"], \"query\":{}}}";
        ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(mapper.readTree(query));

        assertAll(
            () -> assertThat(elasticsearchRequest.getNativeSearchRequest().toString(), is("{\"_source\":[\"data.name\"],\"query\":{}}")),
            () -> assertThat(elasticsearchRequest.hasSupplementaryData(), is(false))
        );
    }

    @Test
    void shouldSetSupplementaryData() throws Exception {
        String query = "{\"native_es_query\":{\"query\":{}},\"supplementary_data\":[\"Field1\",\"Field2\"]}";
        ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(mapper.readTree(query));

        assertAll(
            () -> assertThat(elasticsearchRequest.hasSupplementaryData(), is(true)),
            () -> assertThat(elasticsearchRequest.getSupplementaryData().toString(), is("[\"Field1\",\"Field2\"]"))
        );
    }

    @Test
    void shouldIgnoreSupplementaryDataInIncorrectFormat() throws Exception {
        String query = "{\"native_es_query\":{\"query\":{}},\"supplementary_data\":{\"unknown\":\"object\"}}}";
        ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(mapper.readTree(query));

        assertAll(
            () -> assertThat(elasticsearchRequest.hasSupplementaryData(), is(false))
        );
    }

    @Nested
    class ToJsonTest {

        @Test
        void shouldSetSourceFieldsWhenSourceIsProvidedInRequest() throws Exception {
            String query = "{\"_source\":[\"data.name\"], \"query\":{}}";
            ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(mapper.readTree(query));

            String result = elasticsearchRequest.toJson();

            JsonNode jsonResult = mapper.readTree(result);
            JsonNode sourceNode = jsonResult.get("_source");
            List<String> sourceFields = new ObjectMapper().readValue(sourceNode.traverse(), new TypeReference<ArrayList<String>>(){});

            assertAll(
                () -> assertThat(sourceFields.size(), is(10)),
                () -> assertThat(sourceFields, hasItem("data.name")),
                () -> assertThat(sourceFields, hasItem(CASE_REFERENCE.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(LAST_STATE_MODIFIED_DATE.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(CREATED_DATE.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(CASE_TYPE.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(JURISDICTION.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(SECURITY_CLASSIFICATION.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(LAST_MODIFIED_DATE.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(STATE.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(DATA_CLASSIFICATION_COL))
            );
        }

        @Test
        void shouldSetSourceFieldsWhenSourceIsNotProvidedInRequest() throws Exception {
            String query = "{\"query\":{}}";
            ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(mapper.readTree(query));

            String result = elasticsearchRequest.toJson();

            JsonNode jsonResult = mapper.readTree(result);
            JsonNode sourceNode = jsonResult.get("_source");
            List<String> sourceFields = new ObjectMapper().readValue(sourceNode.traverse(), new TypeReference<ArrayList<String>>(){});

            assertAll(
                () -> assertThat(sourceFields.size(), is(10)),
                () -> assertThat(sourceFields, hasItem("data")),
                () -> assertThat(sourceFields, hasItem(CASE_REFERENCE.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(LAST_STATE_MODIFIED_DATE.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(CREATED_DATE.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(CASE_TYPE.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(JURISDICTION.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(SECURITY_CLASSIFICATION.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(LAST_MODIFIED_DATE.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(STATE.getDbColumnName())),
                () -> assertThat(sourceFields, hasItem(DATA_CLASSIFICATION_COL))
            );
        }
    }

    private JsonNode queryAsJsonNode(String query) throws JsonProcessingException {
        return mapper.readTree(query);
    }
}
