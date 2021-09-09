package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.service.common.DefaultObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ElasticsearchCaseSearchRequestSecurityTest {

    private static final String EXPECTED_SEARCH_TERM = "{\"match\":{\"reference\":1630596267899527}}";
    private static final String SEARCH_QUERY = "{\"query\" : {\"match\" : {\"reference\" : 1630596267899527}}}";

    private static final String CASE_TYPE_ID_1 = "caseType";
    private static final String CASE_TYPE_ID_2 = "caseType2";
    private static final List<String> CASE_TYPE_IDS = List.of(CASE_TYPE_ID_1, CASE_TYPE_ID_2);

    private static final String FILTER_VALUE_1 = "filterType1";
    private static final String FILTER_VALUE_2 = "filterType2";

    private final CaseSearchFilter caseSearchFilter = mock(CaseSearchFilter.class);
    private final List<CaseSearchFilter> filterList = List.of(caseSearchFilter);


    private final ObjectMapperService objectMapperService = new DefaultObjectMapperService(new ObjectMapper());
    private final JsonNode searchRequestNode = objectMapperService.convertStringToObject(SEARCH_QUERY, JsonNode.class);
    private final ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(searchRequestNode);

    private final ElasticsearchCaseSearchRequestSecurity underTest =
        new ElasticsearchCaseSearchRequestSecurity(filterList, objectMapperService);

    @BeforeEach
    void setUp() {
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
    @DisplayName("should parse and secure request with filters and multiple case types")
    void shouldSecureCrossCaseTypeRequestWithFilters() {
        // GIVEN
        when(caseSearchFilter.getFilter(CASE_TYPE_ID_2)).thenReturn(Optional.of(newQueryBuilder(FILTER_VALUE_2)));

        CrossCaseTypeSearchRequest request = new CrossCaseTypeSearchRequest.Builder()
            .withSearchRequest(elasticsearchRequest)
            .withCaseTypes(CASE_TYPE_IDS)
            .build();

        // WHEN
        CrossCaseTypeSearchRequest securedSearchRequest = underTest.createSecuredSearchRequest(request);

        // THEN
        String decodedQuery = getDecodedQuery(securedSearchRequest.getSearchRequestJsonNode());

        Map<String, JsonNode> mapOfFilterValues =
            createMapOfFilterValues(securedSearchRequest.getSearchRequestJsonNode());

        assertEquals(CASE_TYPE_IDS, securedSearchRequest.getCaseTypeIds());
        assertEquals(EXPECTED_SEARCH_TERM, decodedQuery);
        assertEquals(FILTER_VALUE_1, mapOfFilterValues.get(CASE_TYPE_ID_1.toLowerCase()).at("/bool/must").get(0)
            .at("/term/filterTermValue/value").asText());
        assertEquals(FILTER_VALUE_2, mapOfFilterValues.get(CASE_TYPE_ID_2.toLowerCase()).at("/bool/must").get(0)
            .at("/term/filterTermValue/value").asText());
        assertEquals(CASE_TYPE_ID_1.toLowerCase(), mapOfFilterValues.get(CASE_TYPE_ID_1.toLowerCase()).at("/bool/must")
            .get(1).at("/term/case_type_id/value").asText());
        assertEquals(CASE_TYPE_ID_2.toLowerCase(), mapOfFilterValues.get(CASE_TYPE_ID_2.toLowerCase()).at("/bool/must")
            .get(1).at("/term/case_type_id/value").asText());
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
