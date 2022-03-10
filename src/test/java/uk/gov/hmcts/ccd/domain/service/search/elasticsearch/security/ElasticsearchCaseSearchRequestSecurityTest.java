package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.service.common.DefaultObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.SearchIndex;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder.AccessControlGrantTypeESQueryBuilder;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.NATIVE_ES_QUERY;
import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.SOURCE;
import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.SUPPLEMENTARY_DATA;

@ExtendWith(MockitoExtension.class)
class ElasticsearchCaseSearchRequestSecurityTest {

    public static final String FROM = "from";
    public static final String SIZE = "size";

    private static final String EXPECTED_SEARCH_TERM = "{\"match\":{\"reference\":1630596267899527}}";
    private static final String SEARCH_QUERY = "{\"query\" : {\"match\" : {\"reference\" : 1630596267899527}}}";

    private static final String CASE_TYPE_ID_1 = "caseType";
    private static final String CASE_TYPE_ID_2 = "caseType2";
    private static final List<String> CASE_TYPE_IDS = List.of(CASE_TYPE_ID_1, CASE_TYPE_ID_2);

    private static final String FILTER_VALUE_1 = "filterType1";
    private static final String FILTER_VALUE_2 = "filterType2";

    private final CaseSearchFilter caseSearchFilter = mock(CaseSearchFilter.class);
    private final List<CaseSearchFilter> filterList = List.of(caseSearchFilter);

    @Mock
    private AccessControlGrantTypeESQueryBuilder grantTypeESQueryBuilder;

    private final ObjectMapperService objectMapperService = new DefaultObjectMapperService(new ObjectMapper());
    private JsonNode searchRequestNode;
    private ElasticsearchRequest elasticsearchRequest;

    private ElasticsearchCaseSearchRequestSecurity underTest = null;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        searchRequestNode = objectMapperService.convertStringToObject(SEARCH_QUERY, JsonNode.class);
        elasticsearchRequest = new ElasticsearchRequest(searchRequestNode);
        underTest =
            new ElasticsearchCaseSearchRequestSecurity(filterList, objectMapperService,grantTypeESQueryBuilder);
        when(caseSearchFilter.getFilter(CASE_TYPE_ID_1)).thenReturn(Optional.of(newQueryBuilder(FILTER_VALUE_1)));
    }

    @Test
    @DisplayName("should parse and secure request with filters and single case type")
    void shouldSecureRequest() {
        // GIVEN
        CaseSearchRequest request = new CaseSearchRequest(CASE_TYPE_ID_1, elasticsearchRequest);

        // WHEN
        CaseSearchRequest securedSearchRequest = underTest.createSecuredSearchRequest(request);

        // THEN
        JsonNode jsonNode = objectMapperService.convertStringToObject(
            securedSearchRequest.toJsonString(),
            JsonNode.class);
        String decodedQuery = getDecodedQuery(jsonNode);

        assertEquals(CASE_TYPE_ID_1, securedSearchRequest.getCaseTypeId());
        assertEquals(EXPECTED_SEARCH_TERM, decodedQuery);
        assertEquals(FILTER_VALUE_1, jsonNode.at("/query/bool/filter").get(0).at("/term/filterTermValue/value")
            .asText());
    }

    @Test
    @DisplayName("should parse and secure request with NO filters and only base query and multiple case types")
    void shouldSecureCrossCaseTypeRequest() {
        // GIVEN
        CrossCaseTypeSearchRequest request = new CrossCaseTypeSearchRequest.Builder()
            .withSearchRequest(elasticsearchRequest)
            .withCaseTypes(CASE_TYPE_IDS)
            .build();

        // WHEN
        CrossCaseTypeSearchRequest securedSearchRequest = underTest.createSecuredSearchRequest(request);

        // THEN
        String decodedQuery = getDecodedQuery(securedSearchRequest.getSearchRequestJsonNode());

        assertEquals(CASE_TYPE_IDS, securedSearchRequest.getCaseTypeIds());
        assertEquals(EXPECTED_SEARCH_TERM, decodedQuery);
    }

    @Test
    @DisplayName("should parse and secure request with filters and multiple case types and verify complete query")
    void shouldSecureCrossCaseTypeRequestWithFiltersVerifyQuery() {
        // GIVEN
        when(caseSearchFilter.getFilter(CASE_TYPE_ID_2)).thenReturn(Optional.of(newQueryBuilder(FILTER_VALUE_2)));

        CrossCaseTypeSearchRequest request = new CrossCaseTypeSearchRequest.Builder()
            .withSearchRequest(elasticsearchRequest)
            .withCaseTypes(CASE_TYPE_IDS)
            .build();

        // WHEN
        CrossCaseTypeSearchRequest securedSearchRequest = underTest.createSecuredSearchRequest(request);

        // THEN
        String expectedFinalQueryBody = "{\"query\":{\"bool\":{\"must\":[{\"wrapper\":"
            + "{\"query\":\"eyJtYXRjaCI6eyJyZWZlcmVuY2UiOjE2MzA1OTYyNjc4OTk1Mjd9fQ==\"}}],\"should\":["
            + "{\"bool\":{\"must\":[{\"term\":{\"filterTermValue\":{\"value\":\"filterType1\",\"boost\":1.0}}},"
            + "{\"term\":{\"case_type_id\":{\"value\":\"casetype\",\"boost\":1.0}}}],\"adjust_pure_negative\":true,"
            + "\"boost\":1.0}},{\"bool\":{\"must\":[{\"term\":{\"filterTermValue\":{\"value\":\"filterType2\","
            + "\"boost\":1.0}}},{\"term\":{\"case_type_id\":{\"value\":\"casetype2\",\"boost\":1.0}}}],"
            + "\"adjust_pure_negative\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,"
            + "\"minimum_should_match\":\"1\",\"boost\":1.0}}}";

        JsonNode returnedFinalQueryBody = securedSearchRequest.getSearchRequestJsonNode();
        String decodedQuery = getDecodedQuery(returnedFinalQueryBody);

        assertEquals(CASE_TYPE_IDS, securedSearchRequest.getCaseTypeIds());
        assertEquals(EXPECTED_SEARCH_TERM, decodedQuery);
        assertEquals(expectedFinalQueryBody, returnedFinalQueryBody.toString());
    }

    @Test
    @DisplayName("should parse and secure request with filters and multiple case types and verify correct filters")
    void shouldSecureCrossCaseTypeRequestWithFiltersVerifyFilters() {
        // GIVEN
        when(caseSearchFilter.getFilter(CASE_TYPE_ID_2)).thenReturn(Optional.of(newQueryBuilder(FILTER_VALUE_2)));

        CrossCaseTypeSearchRequest request = new CrossCaseTypeSearchRequest.Builder()
            .withSearchRequest(elasticsearchRequest)
            .withCaseTypes(CASE_TYPE_IDS)
            .build();

        // WHEN
        CrossCaseTypeSearchRequest securedSearchRequest = underTest.createSecuredSearchRequest(request);

        // THEN
        Map<String, JsonNode> mapOfFilterValues =
            createMapOfFilterValues(securedSearchRequest.getSearchRequestJsonNode());
        assertEquals(FILTER_VALUE_1, mapOfFilterValues.get(CASE_TYPE_ID_1.toLowerCase()).at("/bool/must").get(0)
            .at("/term/filterTermValue/value").asText());
        assertEquals(FILTER_VALUE_2, mapOfFilterValues.get(CASE_TYPE_ID_2.toLowerCase()).at("/bool/must").get(0)
            .at("/term/filterTermValue/value").asText());
        assertEquals(CASE_TYPE_ID_1.toLowerCase(), mapOfFilterValues.get(CASE_TYPE_ID_1.toLowerCase()).at("/bool/must")
            .get(1).at("/term/case_type_id/value").asText());
        assertEquals(CASE_TYPE_ID_2.toLowerCase(), mapOfFilterValues.get(CASE_TYPE_ID_2.toLowerCase()).at("/bool/must")
            .get(1).at("/term/case_type_id/value").asText());
    }

    @Test
    @DisplayName("should preserve SearchIndex, pagination, source fields and SupplementaryData fields if supplied")
    void shouldPreserveSearchIndexPaginationSourceAndSupplementaryDataValues() throws JSONException {
        // GIVEN
        when(caseSearchFilter.getFilter(CASE_TYPE_ID_2)).thenReturn(Optional.of(newQueryBuilder(FILTER_VALUE_2)));

        int from = 100;
        int size = 999;

        String sourceField1 = "my.source.field.one";
        String sourceField2 = "my.source.field.two";
        ArrayNode sourceFields = JacksonUtils.MAPPER.createArrayNode()
            .add(sourceField1)
            .add(sourceField2);

        ObjectNode searchRequestObjectNode = (ObjectNode)searchRequestNode;
        searchRequestObjectNode.put(FROM, from);
        searchRequestObjectNode.put(SIZE, size);
        searchRequestObjectNode.set(SOURCE, sourceFields);

        String supplementaryDataField1 = "my.supplementary-data.field.one";
        String supplementaryDataField2 = "my.supplementary-data.field.two";
        ArrayNode supplementaryDataFields = JacksonUtils.MAPPER.createArrayNode()
            .add(supplementaryDataField1)
            .add(supplementaryDataField2);

        ObjectNode combinedSearchRequest = (ObjectNode)objectMapperService.createEmptyJsonNode();
        combinedSearchRequest.set(NATIVE_ES_QUERY, searchRequestObjectNode);
        combinedSearchRequest.set(SUPPLEMENTARY_DATA, supplementaryDataFields);

        SearchIndex searchIndex = new SearchIndex(
            "my_index_name",
            "my_index_type"
        );

        CrossCaseTypeSearchRequest request = new CrossCaseTypeSearchRequest.Builder()
            .withSearchRequest(new ElasticsearchRequest(combinedSearchRequest))
            .withCaseTypes(CASE_TYPE_IDS)
            .withSearchIndex(searchIndex)
            .build();

        // WHEN
        CrossCaseTypeSearchRequest securedSearchRequest = underTest.createSecuredSearchRequest(request);

        // THEN
        // :: SearchIndex
        assertTrue(securedSearchRequest.getSearchIndex().isPresent());
        assertEquals(searchIndex, securedSearchRequest.getSearchIndex().get());
        // :: pagination
        assertEquals(from, securedSearchRequest.getSearchRequestJsonNode().get(FROM).asInt());
        assertEquals(size, securedSearchRequest.getSearchRequestJsonNode().get(SIZE).asInt());
        // :: source fields
        JSONAssert.assertEquals(
            "[\"" + sourceField1 + "\",\"" + sourceField2 + "\"]",
            securedSearchRequest.getSearchRequestJsonNode().get(SOURCE).toString(),
            JSONCompareMode.LENIENT
        );
        // :: SupplementaryData fields
        JSONAssert.assertEquals(
            "[\"" + supplementaryDataField1 + "\",\"" + supplementaryDataField2 + "\"]",
            securedSearchRequest.getElasticSearchRequest().getRequestedSupplementaryData().toString(),
            JSONCompareMode.LENIENT
        );
    }

    @Test
    @DisplayName("should preserve SearchIndex, pagination and source fields when SupplementaryData fields not supplied")
    void shouldPreserveSearchIndexPaginationAndSourceWithoutSupplementaryDataValues() throws JSONException {
        // GIVEN
        when(caseSearchFilter.getFilter(CASE_TYPE_ID_2)).thenReturn(Optional.of(newQueryBuilder(FILTER_VALUE_2)));

        int from = 100;
        int size = 999;

        String sourceField1 = "my.source.field.one";
        String sourceField2 = "my.source.field.two";
        ArrayNode sourceFields = JacksonUtils.MAPPER.createArrayNode()
            .add(sourceField1)
            .add(sourceField2);

        ObjectNode searchRequestObjectNode = (ObjectNode)searchRequestNode;
        searchRequestObjectNode.put(FROM, from);
        searchRequestObjectNode.put(SIZE, size);
        searchRequestObjectNode.set(SOURCE, sourceFields);

        SearchIndex searchIndex = new SearchIndex(
            "my_index_name",
            "my_index_type"
        );

        CrossCaseTypeSearchRequest request = new CrossCaseTypeSearchRequest.Builder()
            .withSearchRequest(new ElasticsearchRequest(searchRequestObjectNode))
            .withCaseTypes(CASE_TYPE_IDS)
            .withSearchIndex(searchIndex)
            .build();

        // WHEN
        CrossCaseTypeSearchRequest securedSearchRequest = underTest.createSecuredSearchRequest(request);

        // THEN
        // :: SearchIndex
        assertTrue(securedSearchRequest.getSearchIndex().isPresent());
        assertEquals(searchIndex, securedSearchRequest.getSearchIndex().get());
        // :: pagination
        assertEquals(from, securedSearchRequest.getSearchRequestJsonNode().get(FROM).asInt());
        assertEquals(size, securedSearchRequest.getSearchRequestJsonNode().get(SIZE).asInt());
        // :: source fields
        JSONAssert.assertEquals(
            "[\"" + sourceField1 + "\",\"" + sourceField2 + "\"]",
            securedSearchRequest.getSearchRequestJsonNode().get(SOURCE).toString(),
            JSONCompareMode.LENIENT
        );
    }

    private String getDecodedQuery(JsonNode node) {
        // get wrapped query
        JsonNode queryJsonNode = node
            .at("/query/bool/must").get(0).at("/wrapper/query");
        return decodedQuery(queryJsonNode);
    }

    private String decodedQuery(JsonNode queryJsonNode) {
        return new String(Base64.getDecoder().decode(queryJsonNode.asText()));
    }

    private QueryBuilder newQueryBuilder(String value) {
        return QueryBuilders.termQuery("filterTermValue", value);
    }

    private Map<String, JsonNode> createMapOfFilterValues(JsonNode jsonNode) {
        Map<String, JsonNode> values = new HashMap<>();
        JsonNode filterJsonNodes = jsonNode.at("/query/bool/should");

        for (JsonNode shouldNode : filterJsonNodes) {
            for (JsonNode mustNode : shouldNode.at("/bool/must")) {
                if (!mustNode.at("/term/case_type_id").isEmpty()) {
                    values.put(
                        mustNode.at("/term/case_type_id/value").asText(),
                        shouldNode
                    );
                }
            }
        }

        return values;
    }
}
