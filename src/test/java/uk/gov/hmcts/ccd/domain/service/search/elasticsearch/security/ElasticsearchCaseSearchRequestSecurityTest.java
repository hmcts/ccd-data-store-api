package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.common.DefaultObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElasticsearchCaseSearchRequestSecurityTest {

    private final String SEARCH_REQUEST = "{\"query\" : {\"match\" : {\"reference\" : 1630596267899527}}}";

    private static final String CASE_TYPE_ID = "caseType";
    private static final String CASE_TYPE_ID_2 = "caseType2";

    private static final String FILTER_VALUE_1 = "filterType1";
    private static final String FILTER_VALUE_2 = "filterType2";

    private List<CaseSearchFilter> filterList = new ArrayList<>();

    @Mock
    private CaseSearchFilter caseSearchFilter;

    @Mock
    private CaseAccessService caseAccessService;

    private ObjectMapperService objectMapperService;

    @Mock
    private ObjectNode searchRequestJsonNode;

    private ElasticsearchCaseSearchRequestSecurity querySecurity;

    @BeforeEach
    void setUp() {
        objectMapperService = new DefaultObjectMapperService(new ObjectMapper());
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @DisplayName("should parse and secure request with filters and single case type")
    void shouldSecureRequest() {
        createTestEnvironment();

        JsonNode searchRequestNode = objectMapperService.convertStringToObject(SEARCH_REQUEST, JsonNode.class);

        CaseSearchRequest request = new CaseSearchRequest(CASE_TYPE_ID, new ElasticsearchRequest(searchRequestNode));

        CaseSearchRequest securedSearchRequest = querySecurity.createSecuredSearchRequest(request);

        JsonNode jsonNode = objectMapperService.convertStringToObject(
            securedSearchRequest.toJsonString(),
            JsonNode.class);
        String decodedQuery = getDecodedQuery(jsonNode);

        assertEquals(request.getCaseTypeId(), securedSearchRequest.getCaseTypeId());
        assertEquals(request.getQueryValue(), decodedQuery);
        assertEquals(FILTER_VALUE_1, jsonNode.at("/query/bool/filter").get(0).at("/term/filterTermValue/value")
            .asText());
    }

    @Test
    @DisplayName("should parse and secure request with NO filters and only base query and multiple case types")
    void shouldSecureCrossCaseTypeRequest() {
        createTestEnvironment();

        List<String> caseTypeIds = new ArrayList<>();
        caseTypeIds.add(CASE_TYPE_ID);
        caseTypeIds.add(CASE_TYPE_ID_2);

        JsonNode searchRequestNode = objectMapperService.convertStringToObject(SEARCH_REQUEST, JsonNode.class);

        CrossCaseTypeSearchRequest request = new CrossCaseTypeSearchRequest.Builder()
            .withSearchRequest(new ElasticsearchRequest(searchRequestNode))
            .withCaseTypes(caseTypeIds)
            .build();

        CrossCaseTypeSearchRequest securedSearchRequest = querySecurity.createSecuredSearchRequest(request);

        String decodedQuery = getDecodedQuery(securedSearchRequest.getSearchRequestJsonNode());

        assertEquals(request.getCaseTypeIds(), securedSearchRequest.getCaseTypeIds());
        assertEquals(request.getElasticSearchRequest().getQuery().toString(), decodedQuery);
    }

    @Test
    @DisplayName("should parse and secure request with filters and multiple case types")
    void shouldSecureCrossCaseTypeRequestWithFilters() {
        List<String> caseTypeIds = new ArrayList<>();
        caseTypeIds.add(CASE_TYPE_ID);
        caseTypeIds.add(CASE_TYPE_ID_2);

        when(caseSearchFilter.getFilter(CASE_TYPE_ID_2)).thenReturn(Optional.of(newQueryBuilder(FILTER_VALUE_2)));
        createTestEnvironment();

        JsonNode searchRequestNode = objectMapperService.convertStringToObject(SEARCH_REQUEST, JsonNode.class);

        CrossCaseTypeSearchRequest request = new CrossCaseTypeSearchRequest.Builder()
            .withSearchRequest(new ElasticsearchRequest(searchRequestNode))
            .withCaseTypes(caseTypeIds)
            .build();

        CrossCaseTypeSearchRequest securedSearchRequest = querySecurity.createSecuredSearchRequest(request);
        String decodedQuery = getDecodedQuery(securedSearchRequest.getSearchRequestJsonNode());

        Map<String, JsonNode> mapOfFilterValues = createMapOfFilterValues(securedSearchRequest.getSearchRequestJsonNode());

        assertEquals(request.getCaseTypeIds(), securedSearchRequest.getCaseTypeIds());
        assertEquals(request.getElasticSearchRequest().getQuery().toString(), decodedQuery);
        assertEquals(FILTER_VALUE_1, mapOfFilterValues.get(CASE_TYPE_ID.toLowerCase()).at("/bool/must").get(0)
            .at("/term/filterTermValue/value").asText());
        assertEquals(FILTER_VALUE_2, mapOfFilterValues.get(CASE_TYPE_ID_2.toLowerCase()).at("/bool/must").get(0)
            .at("/term/filterTermValue/value").asText());
        assertEquals(CASE_TYPE_ID.toLowerCase(), mapOfFilterValues.get(CASE_TYPE_ID.toLowerCase()).at("/bool/must")
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

    // decode base 64
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
                if(!mustNode.at("/term/case_type_id").isEmpty()) {
                    values.put(
                        mustNode.at("/term/case_type_id/value").asText(),
                        shouldNode
                    );
                }
            }
        }

        return values;
    }

    private void createTestEnvironment() {
        when(caseSearchFilter.getFilter(CASE_TYPE_ID)).thenReturn(Optional.of(newQueryBuilder(FILTER_VALUE_1)));

        filterList.add(caseSearchFilter);
        querySecurity = new ElasticsearchCaseSearchRequestSecurity(filterList,
            objectMapperService);
    }

}
