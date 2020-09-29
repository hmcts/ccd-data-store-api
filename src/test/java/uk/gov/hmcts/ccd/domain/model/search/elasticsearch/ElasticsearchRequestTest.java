package uk.gov.hmcts.ccd.domain.model.search.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.hasItem;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.DATA_CLASSIFICATION_COL;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.DATA_COL;
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
        uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest elasticsearchRequest =
                new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest(queryNode);

        assertFalse(elasticsearchRequest.hasSourceFields());
    }

    @Test
    void hasSourceFieldsShouldReturnTrueWhenSourceIsProvided() throws JsonProcessingException {
        JsonNode queryNode = queryAsJsonNode("{\"_source\":[\"data.Field\"],\"query\":{\"match_all\": {}}}");
        uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest elasticsearchRequest =
                new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest(queryNode);

        assertTrue(elasticsearchRequest.hasSourceFields());
    }

    @Test
    void hasSourceFieldsShouldReturnFalseWhenSourceIsEmpty() throws JsonProcessingException {
        JsonNode queryNode = queryAsJsonNode("{\"_source\":[],\"query\":{\"match_all\": {}}}");
        uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest elasticsearchRequest =
                new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest(queryNode);

        assertFalse(elasticsearchRequest.hasSourceFields());
    }

    @Test
    void hasSourceFieldsShouldReturnFalseWhenSourceOnlyHasWildcard() throws JsonProcessingException {
        JsonNode queryNode = queryAsJsonNode("{\"_source\":[\"*\"],\"query\":{\"match_all\": {}}}");
        uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest elasticsearchRequest =
                new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest(queryNode);

        assertFalse(elasticsearchRequest.hasSourceFields());
    }

    @Test
    void hasSourceFieldsShouldReturnFalseWhenSourceIsFalse() throws JsonProcessingException {
        JsonNode queryNode = queryAsJsonNode("{\"_source\": false,\"query\":{\"match_all\": {}}}");
        uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest elasticsearchRequest =
                new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest(queryNode);

        assertFalse(elasticsearchRequest.hasSourceFields());
    }

    @Test
    void hasSourceFieldsShouldReturnFalseWhenSourceIsTrue() throws JsonProcessingException {
        JsonNode queryNode = queryAsJsonNode("{\"_source\": true,\"query\":{\"match_all\": {}}}");
        uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest elasticsearchRequest =
                new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest(queryNode);

        assertFalse(elasticsearchRequest.hasSourceFields());
    }

    @Test
    void shouldReturnRequestedFieldsAsCaseFieldIds() throws JsonProcessingException {
        JsonNode queryNode = queryAsJsonNode("{\"_source\":[\"data.CaseDataField\",\"reference\",\"state\","
                                             + "\"data.OtherCaseDataField\"],\"query\":{\"match_all\": {}}}");
        uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest elasticsearchRequest =
                new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest(queryNode);

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
        JsonNode queryNode =
                queryAsJsonNode("{\"_source\":[\"reference\",\"INVALID\"],\"query\":{\"match_all\": {}}}");
        uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest elasticsearchRequest =
                new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest(queryNode);

        List<String> requestedFields = elasticsearchRequest.getRequestedFields();

        assertAll(
            () -> assertThat(requestedFields.size(), is(1)),
            () -> assertThat(requestedFields.get(0), is("[CASE_REFERENCE]"))
        );
    }

    @Test
    void shouldReturnNoRequestedFieldsWhenOnlyWildcardIsRequested() throws JsonProcessingException {
        JsonNode queryNode = queryAsJsonNode("{\"_source\":[\"*\"],\"query\":{\"match_all\": {}}}");
        uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest elasticsearchRequest =
                new uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest(queryNode);

        List<String> requestedFields = elasticsearchRequest.getRequestedFields();

        assertTrue(requestedFields.isEmpty());
    }

    @Test
    void shouldHandleWrappedQueryFormatWithSupplementaryData() throws JsonProcessingException {
        JsonNode queryNode = queryAsJsonNode("{\"native_es_query\":{\"_source\":[\"data.CaseDataField\","
              + "\"reference\",\"state\",\"data.OtherCaseDataField\"],\"query\":{\"match_all\": {}}},"
               + "\"supplementary_data\":[\"SupDataField\"]}");
        ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(queryNode);

        assertAll(
            () -> assertThat(elasticsearchRequest.getQuery().toString(), is("{\"match_all\":{}}")),
            () -> assertThat(elasticsearchRequest.getRequestedFields().size(), is(4)),
            () -> assertThat(elasticsearchRequest.hasRequestedSupplementaryData(), is(true)),
            () -> assertThat(elasticsearchRequest.getRequestedSupplementaryData().toString(), is("[\"SupDataField\"]"))
        );
    }

    @Test
    void shouldHandleWrappedQueryFormatWithoutSupplementaryData() throws Exception {
        JsonNode queryNode = queryAsJsonNode("{\"native_es_query\":{\"query\":{\"match_all\": {}}}}");
        ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(queryNode);

        assertAll(
            () -> assertThat(elasticsearchRequest.getQuery().toString(), is("{\"match_all\":{}}")),
            () -> assertThat(elasticsearchRequest.hasRequestedSupplementaryData(), is(false)),
            () -> assertThat(elasticsearchRequest.getRequestedSupplementaryData(), is(nullValue()))
        );
    }

    @Test
    void shouldSetNativeQueryWhenProvidedInWrapperObject() throws Exception {
        JsonNode queryNode = queryAsJsonNode("{\"native_es_query\":{\"_source\":[\"data.name\"], \"query\":{}}}");
        ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(queryNode);

        assertAll(
            () -> assertThat(elasticsearchRequest.getNativeSearchRequest().toString(),
                    is("{\"_source\":[\"data.name\"],\"query\":{}}")),
            () -> assertThat(elasticsearchRequest.hasRequestedSupplementaryData(), is(false))
        );
    }

    @Test
    void shouldSetSupplementaryData() throws Exception {
        JsonNode queryNode =
                queryAsJsonNode("{\"native_es_query\":{\"query\":{}},\"supplementary_data\":[\"Field1\",\"Field2\"]}");
        ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(queryNode);

        assertAll(
            () -> assertThat(elasticsearchRequest.hasRequestedSupplementaryData(), is(true)),
            () -> assertThat(elasticsearchRequest.getRequestedSupplementaryData().toString(),
                    is("[\"Field1\",\"Field2\"]"))
        );
    }

    @Test
    void shouldSetMetadataFieldsArrayNode() throws Exception {
        ArrayNode result = ElasticsearchRequest.METADATA_FIELDS;

        List<String> resultAsList =
                new ObjectMapper().readValue(result.traverse(), new TypeReference<ArrayList<String>>(){});
        assertAll(
            () -> assertThat(result.size(), is(9)),
            () -> assertThat(resultAsList, hasItem(CASE_REFERENCE.getDbColumnName())),
            () -> assertThat(resultAsList, hasItem(LAST_STATE_MODIFIED_DATE.getDbColumnName())),
            () -> assertThat(resultAsList, hasItem(CREATED_DATE.getDbColumnName())),
            () -> assertThat(resultAsList, hasItem(CASE_TYPE.getDbColumnName())),
            () -> assertThat(resultAsList, hasItem(JURISDICTION.getDbColumnName())),
            () -> assertThat(resultAsList, hasItem(SECURITY_CLASSIFICATION.getDbColumnName())),
            () -> assertThat(resultAsList, hasItem(LAST_MODIFIED_DATE.getDbColumnName())),
            () -> assertThat(resultAsList, hasItem(STATE.getDbColumnName())),
            () -> assertThat(resultAsList, hasItem(DATA_CLASSIFICATION_COL))
        );
    }

    @Nested
    class ToFinalRequestTest {

        @Test
        void shouldSetSourceFieldsWhenSourceIsProvidedInRequest() throws Exception {
            JsonNode queryNode = queryAsJsonNode("{\"_source\":[\"data.name\"], \"query\":{}}");
            ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(queryNode);

            String result = elasticsearchRequest.toFinalRequest();

            JsonNode jsonResult = mapper.readTree(result);
            JsonNode sourceNode = jsonResult.get("_source");
            List<String> sourceFields =
                    new ObjectMapper().readValue(sourceNode.traverse(), new TypeReference<ArrayList<String>>(){});

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
            JsonNode queryNode = queryAsJsonNode("{\"query\":{}}");
            ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(queryNode);

            String result = elasticsearchRequest.toFinalRequest();

            JsonNode jsonResult = mapper.readTree(result);
            JsonNode sourceNode = jsonResult.get("_source");
            List<String> sourceFields =
                    new ObjectMapper().readValue(sourceNode.traverse(), new TypeReference<ArrayList<String>>(){});

            assertAll(
                () -> assertThat(sourceFields.size(), is(10)),
                () -> assertThat(sourceFields, hasItem(DATA_COL)),
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
        void shouldSetSourceFieldsWhenSupplementaryDataIsProvidedInRequest() throws Exception {
            JsonNode queryNode = queryAsJsonNode("{\"native_es_query\":{\"query\":{}},\"supplementary_data\":"
                    + "[\"Field1\",\"Field2\"]}");
            ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(queryNode);

            String result = elasticsearchRequest.toFinalRequest();

            JsonNode jsonResult = mapper.readTree(result);
            JsonNode sourceNode = jsonResult.get("_source");
            List<String> sourceFields =
                    new ObjectMapper().readValue(sourceNode.traverse(), new TypeReference<ArrayList<String>>(){});

            assertAll(
                () -> assertThat(sourceFields.size(), is(12)),
                () -> assertThat(sourceFields, hasItem("data")),
                () -> assertThat(sourceFields, hasItem("supplementary_data.Field1")),
                () -> assertThat(sourceFields, hasItem("supplementary_data.Field2")),
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
