package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.core.MultiSearch;
import io.searchbox.core.MultiSearchResult;
import io.searchbox.core.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.ElasticSearchCaseDetailsDTO;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.mapper.CaseDetailsMapper;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security.CaseSearchRequestSecurity;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.QUERY;
import static uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchCaseSearchOperation.MULTI_SEARCH_ERROR_MSG_ROOT_CAUSE;

class ElasticsearchCaseSearchOperationTest {

    private static final String INDEX_NAME_FORMAT = "%s_cases";
    private static final String CASE_TYPE_ID_1 = "casetypeid1";
    private static final String CASE_TYPE_ID_2 = "casetypeid2";
    private static final String INDEX_TYPE = "case";
    private final String caseDetailsElastic = "{some case details}";
    private final String caseDetailsElasticComplex = "{\n"
        + "   \"hits\":{\n"
        + "      \"total\":4,\n"
        + "      \"max_score\":null,\n"
        + "      \"hits\":[\n"
        + "         {\n"
        + "            \"_index\":\"casetypeid1_cases-000001\"\n"
        + "         }\n"
        + "      ]\n"
        + "   }\n"
        + "}";

    final JsonObject convertedObject = new Gson().fromJson(caseDetailsElasticComplex, JsonObject.class);

    @InjectMocks
    private ElasticsearchCaseSearchOperation searchOperation;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private JestClient jestClient;

    @Mock
    private CaseDetailsMapper mapper;

    @Mock
    private ElasticSearchCaseDetailsDTO caseDetailsDTO;

    @Mock
    private CaseSearchRequestSecurity caseSearchRequestSecurity;

    @Mock
    private CaseDetails caseDetails;

    @Mock
    private ObjectMapper objectMapper;

    private final ObjectNode searchRequestJsonNode = JsonNodeFactory.instance.objectNode();

    private final ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(searchRequestJsonNode);

    private final String json = "{\n"
        + "    \"took\": 96,\n"
        + "    \"timed_out\": false,\n"
        + "    \"_shards\": {\n"
        + "        \"total\": 2,\n"
        + "        \"successful\": 2,\n"
        + "        \"skipped\": 0,\n"
        + "        \"failed\": 0\n"
        + "    },\n"
        + "    \"hits\": {\n"
        + "        \"total\": 2,\n"
        + "        \"max_score\": 0.18232156,\n"
        + "        \"hits\": [\n"
        + "            {\n"
        + "                \"_index\": \"casetypeid1_cases-000001\",\n"
        + "                \"_type\": \"_doc\",\n"
        + "                \"_id\": \"355\",\n"
        + "                \"_score\": 0.18232156,\n"
        + "                \"_source\": {\n"
        + "                    \"jurisdiction\": \"AUTOTEST1\",\n"
        + "                    \"case_type_id\": \"AAT\",\n"
        + "                    \"data\": {\n"
        + "                        \"FixedRadioListField\": null,\n"
        + "                        \"TextAreaField\": null,\n"
        + "                        \"ComplexField\": {\n"
        + "                            \"ComplexTextField\": null,\n"
        + "                            \"ComplexFixedListField\": null,\n"
        + "                            \"ComplexNestedField\": {\n"
        + "                                \"NestedNumberField\": null,\n"
        + "                                \"NestedCollectionTextField\": [\n"
        + "                                    \n"
        + "                                ]\n"
        + "                            }\n"
        + "                        },\n"
        + "                        \"EmailField\": null,\n"
        + "                        \"TextField\": null,\n"
        + "                        \"AddressUKField\": {\n"
        + "                            \"Country\": null,\n"
        + "                            \"AddressLine3\": null,\n"
        + "                            \"County\": null,\n"
        + "                            \"AddressLine1\": null,\n"
        + "                            \"PostCode\": null,\n"
        + "                            \"AddressLine2\": null,\n"
        + "                            \"PostTown\": null\n"
        + "                        },\n"
        + "                        \"CollectionField\": [\n"
        + "                            \n"
        + "                        ],\n"
        + "                        \"MoneyGBPField\": null,\n"
        + "                        \"DateField\": null,\n"
        + "                        \"MultiSelectListField\": [\n"
        + "                            \n"
        + "                        ],\n"
        + "                        \"PhoneUKField\": null,\n"
        + "                        \"YesOrNoField\": null,\n"
        + "                        \"NumberField\": null,\n"
        + "                        \"DateTimeField\": null,\n"
        + "                        \"FixedListField\": null\n"
        + "                    },\n"
        + "                    \"created_date\": \"2020-06-30T14:47:26.061Z\",\n"
        + "                    \"id\": 355,\n"
        + "                    \"last_modified\": \"2020-06-30T14:52:36.335Z\",\n"
        + "                    \"@timestamp\": \"2020-07-16T22:58:33.430Z\",\n"
        + "                    \"index_id\": \"aat_cases\",\n"
        + "                    \"@version\": \"1\",\n"
        + "                    \"data_classification\": {\n"
        + "                        \"FixedRadioListField\": \"PUBLIC\",\n"
        + "                        \"TextAreaField\": \"PUBLIC\",\n"
        + "                        \"ComplexField\": {\n"
        + "                            \"classification\": \"PUBLIC\",\n"
        + "                            \"value\": {\n"
        + "                                \"ComplexTextField\": \"PUBLIC\",\n"
        + "                                \"ComplexFixedListField\": \"PUBLIC\",\n"
        + "                                \"ComplexNestedField\": {\n"
        + "                                    \"classification\": \"PUBLIC\",\n"
        + "                                    \"value\": {\n"
        + "                                        \"NestedNumberField\": \"PUBLIC\",\n"
        + "                                        \"NestedCollectionTextField\": {\n"
        + "                                            \"classification\": \"PUBLIC\",\n"
        + "                                            \"value\": [\n"
        + "                                                \n"
        + "                                            ]\n"
        + "                                        }\n"
        + "                                    }\n"
        + "                                }\n"
        + "                            }\n"
        + "                        },\n"
        + "                        \"EmailField\": \"PUBLIC\",\n"
        + "                        \"TextField\": \"PUBLIC\",\n"
        + "                        \"AddressUKField\": {\n"
        + "                            \"classification\": \"PUBLIC\",\n"
        + "                            \"value\": {\n"
        + "                                \"Country\": \"PUBLIC\",\n"
        + "                                \"AddressLine3\": \"PUBLIC\",\n"
        + "                                \"County\": \"PUBLIC\",\n"
        + "                                \"AddressLine1\": \"PUBLIC\",\n"
        + "                                \"PostCode\": \"PUBLIC\",\n"
        + "                                \"AddressLine2\": \"PUBLIC\",\n"
        + "                                \"PostTown\": \"PUBLIC\"\n"
        + "                            }\n"
        + "                        },\n"
        + "                        \"CollectionField\": {\n"
        + "                            \"classification\": \"PUBLIC\",\n"
        + "                            \"value\": [\n"
        + "                                \n"
        + "                            ]\n"
        + "                        },\n"
        + "                        \"MoneyGBPField\": \"PUBLIC\",\n"
        + "                        \"DateField\": \"PUBLIC\",\n"
        + "                        \"MultiSelectListField\": \"PUBLIC\",\n"
        + "                        \"PhoneUKField\": \"PUBLIC\",\n"
        + "                        \"YesOrNoField\": \"PUBLIC\",\n"
        + "                        \"NumberField\": \"PUBLIC\",\n"
        + "                        \"DateTimeField\": \"PUBLIC\",\n"
        + "                        \"FixedListField\": \"PUBLIC\"\n"
        + "                    },\n"
        + "                    \"security_classification\": \"PUBLIC\",\n"
        + "                    \"state\": \"TODO\",\n"
        + "                    \"reference\": 1593528446017551\n"
        + "                }\n"
        + "            },\n"
        + "            {\n"
        + "                \"_index\": \"casetypeid1_cases-000001\",\n"
        + "                \"_type\": \"_doc\",\n"
        + "                \"_id\": \"357\",\n"
        + "                \"_score\": 0.18232156,\n"
        + "                \"_source\": {\n"
        + "                    \"jurisdiction\": \"AUTOTEST1\",\n"
        + "                    \"case_type_id\": \"AAT\",\n"
        + "                    \"data\": {\n"
        + "                        \"FixedRadioListField\": null,\n"
        + "                        \"TextAreaField\": null,\n"
        + "                        \"ComplexField\": {\n"
        + "                            \"ComplexTextField\": null,\n"
        + "                            \"ComplexFixedListField\": null,\n"
        + "                            \"ComplexNestedField\": {\n"
        + "                                \"NestedNumberField\": null,\n"
        + "                                \"NestedCollectionTextField\": [\n"
        + "                                    \n"
        + "                                ]\n"
        + "                            }\n"
        + "                        },\n"
        + "                        \"EmailField\": \"email1@gmail.com\",\n"
        + "                        \"TextField\": \"Text Field 1\",\n"
        + "                        \"AddressUKField\": {\n"
        + "                            \"Country\": null,\n"
        + "                            \"AddressLine3\": null,\n"
        + "                            \"County\": null,\n"
        + "                            \"AddressLine1\": null,\n"
        + "                            \"PostCode\": null,\n"
        + "                            \"AddressLine2\": null,\n"
        + "                            \"PostTown\": null\n"
        + "                        },\n"
        + "                        \"CollectionField\": [\n"
        + "                            \n"
        + "                        ],\n"
        + "                        \"MoneyGBPField\": null,\n"
        + "                        \"DateField\": null,\n"
        + "                        \"MultiSelectListField\": [\n"
        + "                            \n"
        + "                        ],\n"
        + "                        \"PhoneUKField\": null,\n"
        + "                        \"YesOrNoField\": null,\n"
        + "                        \"NumberField\": null,\n"
        + "                        \"DateTimeField\": null,\n"
        + "                        \"FixedListField\": null\n"
        + "                    },\n"
        + "                    \"created_date\": \"2020-07-07T15:12:53.258Z\",\n"
        + "                    \"id\": 357,\n"
        + "                    \"last_modified\": \"2020-07-07T15:14:04.635Z\",\n"
        + "                    \"@timestamp\": \"2020-07-16T22:58:33.435Z\",\n"
        + "                    \"index_id\": \"aat_cases\",\n"
        + "                    \"@version\": \"1\",\n"
        + "                    \"data_classification\": {\n"
        + "                        \"FixedRadioListField\": \"PUBLIC\",\n"
        + "                        \"TextAreaField\": \"PUBLIC\",\n"
        + "                        \"ComplexField\": {\n"
        + "                            \"classification\": \"PUBLIC\",\n"
        + "                            \"value\": {\n"
        + "                                \"ComplexTextField\": \"PUBLIC\",\n"
        + "                                \"ComplexFixedListField\": \"PUBLIC\",\n"
        + "                                \"ComplexNestedField\": {\n"
        + "                                    \"classification\": \"PUBLIC\",\n"
        + "                                    \"value\": {\n"
        + "                                        \"NestedNumberField\": \"PUBLIC\",\n"
        + "                                        \"NestedCollectionTextField\": {\n"
        + "                                            \"classification\": \"PUBLIC\",\n"
        + "                                            \"value\": [\n"
        + "                                                \n"
        + "                                            ]\n"
        + "                                        }\n"
        + "                                    }\n"
        + "                                }\n"
        + "                            }\n"
        + "                        },\n"
        + "                        \"EmailField\": \"PUBLIC\",\n"
        + "                        \"TextField\": \"PUBLIC\",\n"
        + "                        \"AddressUKField\": {\n"
        + "                            \"classification\": \"PUBLIC\",\n"
        + "                            \"value\": {\n"
        + "                                \"Country\": \"PUBLIC\",\n"
        + "                                \"AddressLine3\": \"PUBLIC\",\n"
        + "                                \"County\": \"PUBLIC\",\n"
        + "                                \"AddressLine1\": \"PUBLIC\",\n"
        + "                                \"PostCode\": \"PUBLIC\",\n"
        + "                                \"AddressLine2\": \"PUBLIC\",\n"
        + "                                \"PostTown\": \"PUBLIC\"\n"
        + "                            }\n"
        + "                        },\n"
        + "                        \"CollectionField\": {\n"
        + "                            \"classification\": \"PUBLIC\",\n"
        + "                            \"value\": [\n"
        + "                                \n"
        + "                            ]\n"
        + "                        },\n"
        + "                        \"MoneyGBPField\": \"PUBLIC\",\n"
        + "                        \"DateField\": \"PUBLIC\",\n"
        + "                        \"MultiSelectListField\": \"PUBLIC\",\n"
        + "                        \"PhoneUKField\": \"PUBLIC\",\n"
        + "                        \"YesOrNoField\": \"PUBLIC\",\n"
        + "                        \"NumberField\": \"PUBLIC\",\n"
        + "                        \"DateTimeField\": \"PUBLIC\",\n"
        + "                        \"FixedListField\": \"PUBLIC\"\n"
        + "                    },\n"
        + "                    \"security_classification\": \"PUBLIC\",\n"
        + "                    \"state\": \"TODO\",\n"
        + "                    \"reference\": 1594134773278525\n"
        + "                }\n"
        + "            }\n"
        + "        ]\n"
        + "    }\n"
        + "}";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(applicationParams.getCasesIndexNameFormat()).thenReturn(INDEX_NAME_FORMAT);
        when(applicationParams.getCasesIndexNameCaseTypeIdGroup()).thenReturn("(.+)(_cases.*)");
        when(applicationParams.getCasesIndexNameCaseTypeIdGroupPosition()).thenReturn(1);
        when(applicationParams.getCasesIndexType()).thenReturn(INDEX_TYPE);
        searchRequestJsonNode.set(QUERY, new TextNode("queryVal"));
    }

    @Nested
    @DisplayName("Named index search")
    class NamedIndexSearch {

        @Test
        @DisplayName("should execute search on Elasticsearch for a named index and return results")
        void testSearchNamedIndex() throws IOException {
            // GIVEN
            final CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = buildCrossCaseTypeSearchRequest();
            final MultiSearchResult multiSearchResult = mockMultiSearchResult();
            doReturn(crossCaseTypeSearchRequest)
                .when(caseSearchRequestSecurity).createSecuredSearchRequest(any(CrossCaseTypeSearchRequest.class));
            doReturn(multiSearchResult).when(jestClient).execute(any(MultiSearch.class));

            // WHEN
            final CaseSearchResult caseSearchResult = searchOperation.execute(crossCaseTypeSearchRequest, true);

            // THEN
            assertAll(
                () -> assertThat(caseSearchResult.getCases(), equalTo(emptyList())),
                () -> assertThat(caseSearchResult.getTotal(), equalTo(0L)),
                () -> verify(jestClient).execute(any(MultiSearch.class)),
                () -> verify(caseSearchRequestSecurity)
                    .createSecuredSearchRequest(any(CrossCaseTypeSearchRequest.class)));
        }

        private CrossCaseTypeSearchRequest buildCrossCaseTypeSearchRequest() {
            return new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(List.of(CASE_TYPE_ID_1, CASE_TYPE_ID_2))
                .withSearchRequest(elasticsearchRequest)
                .withSearchIndex(new SearchIndex("global_search", "_doc"))
                .build();
        }

        private MultiSearchResult mockMultiSearchResult() {
            final MultiSearchResult multiSearchResult = mock(MultiSearchResult.class);
            final SearchResult searchResult = mock(SearchResult.class);
            doReturn(true).when(multiSearchResult).isSucceeded();
            doReturn(0L).when(searchResult).getTotal();
            doReturn(emptyList()).when(multiSearchResult).getResponses();

            return multiSearchResult;
        }
    }

    @Nested
    @DisplayName("Single case type search")
    class SingleCaseTypeDefinitionSearch {

        @Test
        @DisplayName("should execute search on Elasticsearch for a single case type and return results")
        void searchShouldMapElasticSearchResultToSearchResult() throws IOException {
            MultiSearchResult multiSearchResult = mock(MultiSearchResult.class);
            when(multiSearchResult.isSucceeded()).thenReturn(true);

            JsonObject convertedObject = new Gson().fromJson(json, JsonObject.class);
            SearchResult searchResult;
            Gson gson = new Gson();
            searchResult = new SearchResult(gson);
            searchResult.setSucceeded(true);
            searchResult.setJsonObject(convertedObject);
            searchResult.setJsonString(convertedObject.toString());
            searchResult.setPathToResult("hits/hits/_source");

            MultiSearchResult.MultiSearchResponse response = mock(MultiSearchResult.MultiSearchResponse.class);
            when(multiSearchResult.getResponses()).thenReturn(Collections.singletonList(response));
            Whitebox.setInternalState(response, "searchResult", searchResult);

            when(objectMapper.readValue(caseDetailsElastic, ElasticSearchCaseDetailsDTO.class))
                .thenReturn(caseDetailsDTO);
            when(mapper.dtosToCaseDetailsList(newArrayList(caseDetailsDTO))).thenReturn(newArrayList(caseDetails));
            when(jestClient.execute(any(MultiSearch.class))).thenReturn(multiSearchResult);

            CaseSearchRequest request = new CaseSearchRequest(CASE_TYPE_ID_1, elasticsearchRequest);
            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(Collections.singletonList(CASE_TYPE_ID_1))
                .withSearchRequest(elasticsearchRequest)
                .build();
            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class)))
                .thenReturn(request);

            CaseSearchResult caseSearchResult = searchOperation.execute(crossCaseTypeSearchRequest, true);

            assertAll(
                () -> assertThat(caseSearchResult.getCases(), equalTo(newArrayList())),
                () -> assertThat(caseSearchResult.getTotal(), equalTo(2L)),
                () -> verify(jestClient).execute(any(MultiSearch.class)),
                () -> verify(applicationParams).getCasesIndexNameFormat(),
                () -> verify(applicationParams).getCasesIndexType(),
                () -> verify(caseSearchRequestSecurity).createSecuredSearchRequest(any(CaseSearchRequest.class)));
        }

        @Test
        @DisplayName("should return ServiceError when ES index case type id capturing group is not matching the ES "
            + "index")
        void searchShouldReturnServiceErrorWhenESIndexCaseTypeIdCapturingGroudNotMatching() throws IOException {
            when(applicationParams.getCasesIndexNameCaseTypeIdGroup()).thenReturn("a_regex_with_not_matching_"
                + "(capturing)_group");

            MultiSearchResult multiSearchResult = mock(MultiSearchResult.class);
            when(multiSearchResult.isSucceeded()).thenReturn(true);
            SearchResult searchResult = mock(SearchResult.class);
            when(searchResult.getTotal()).thenReturn(1L);
            when(searchResult.getSourceAsStringList()).thenReturn(newArrayList(caseDetailsElastic));
            MultiSearchResult.MultiSearchResponse response = mock(MultiSearchResult.MultiSearchResponse.class);
            when(multiSearchResult.getResponses()).thenReturn(Collections.singletonList(response));
            when(searchResult.getJsonObject()).thenReturn(convertedObject);
            Whitebox.setInternalState(response, "searchResult", searchResult);

            when(objectMapper.readValue(caseDetailsElastic, ElasticSearchCaseDetailsDTO.class))
                .thenReturn(caseDetailsDTO);
            when(mapper.dtosToCaseDetailsList(newArrayList(caseDetailsDTO))).thenReturn(newArrayList(caseDetails));
            when(jestClient.execute(any(MultiSearch.class))).thenReturn(multiSearchResult);

            CaseSearchRequest request = new CaseSearchRequest(CASE_TYPE_ID_1, elasticsearchRequest);
            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(Collections.singletonList(CASE_TYPE_ID_1))
                .withSearchRequest(elasticsearchRequest)
                .build();
            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class)))
                .thenReturn(request);

            assertThrows(ServiceException.class, () -> searchOperation.execute(crossCaseTypeSearchRequest, true));
        }

        @Test
        @DisplayName("should return ServiceError when ES index case type id capturing group is not specified")
        void searchShouldReturnServiceErrorWhenESIndexCaseTypeIdCapturingGroudNotSpedified() throws IOException {
            when(applicationParams.getCasesIndexNameCaseTypeIdGroup()).thenReturn("a_regex_with_no_capturing_group");

            MultiSearchResult multiSearchResult = mock(MultiSearchResult.class);
            when(multiSearchResult.isSucceeded()).thenReturn(true);
            SearchResult searchResult = mock(SearchResult.class);
            when(searchResult.getTotal()).thenReturn(1L);
            when(searchResult.getSourceAsStringList()).thenReturn(newArrayList(caseDetailsElastic));
            MultiSearchResult.MultiSearchResponse response = mock(MultiSearchResult.MultiSearchResponse.class);
            when(multiSearchResult.getResponses()).thenReturn(Collections.singletonList(response));
            when(searchResult.getJsonObject()).thenReturn(convertedObject);
            Whitebox.setInternalState(response, "searchResult", searchResult);

            when(objectMapper.readValue(caseDetailsElastic, ElasticSearchCaseDetailsDTO.class))
                .thenReturn(caseDetailsDTO);
            when(mapper.dtosToCaseDetailsList(newArrayList(caseDetailsDTO))).thenReturn(newArrayList(caseDetails));
            when(jestClient.execute(any(MultiSearch.class))).thenReturn(multiSearchResult);

            CaseSearchRequest request = new CaseSearchRequest(CASE_TYPE_ID_1, elasticsearchRequest);
            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(Collections.singletonList(CASE_TYPE_ID_1))
                .withSearchRequest(elasticsearchRequest)
                .build();
            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class)))
                .thenReturn(request);

            assertThrows(ServiceException.class, () -> searchOperation.execute(crossCaseTypeSearchRequest, true));
        }

        @Test
        @DisplayName("should return ServiceError when ES index case type id capturing group is not associated to any "
            + "case type")
        void searchShouldReturnServiceErrorWhenESIndexCaseTypeIdCapturingGroudNotAssociatedToAnyCaseType()
                                                                                                throws IOException {
            when(applicationParams.getCasesIndexNameCaseTypeIdGroup()).thenReturn("(.+)(_cases.*)");

            MultiSearchResult multiSearchResult = mock(MultiSearchResult.class);
            when(multiSearchResult.isSucceeded()).thenReturn(true);
            SearchResult searchResult = mock(SearchResult.class);
            when(searchResult.getTotal()).thenReturn(1L);
            when(searchResult.getSourceAsStringList()).thenReturn(newArrayList(caseDetailsElastic));
            MultiSearchResult.MultiSearchResponse response = mock(MultiSearchResult.MultiSearchResponse.class);
            when(multiSearchResult.getResponses()).thenReturn(Collections.singletonList(response));
            when(searchResult.getJsonObject()).thenReturn(convertedObject);
            Whitebox.setInternalState(response, "searchResult", searchResult);

            when(objectMapper.readValue(caseDetailsElastic, ElasticSearchCaseDetailsDTO.class))
                .thenReturn(caseDetailsDTO);
            when(mapper.dtosToCaseDetailsList(newArrayList(caseDetailsDTO))).thenReturn(newArrayList(caseDetails));
            when(jestClient.execute(any(MultiSearch.class))).thenReturn(multiSearchResult);

            CaseSearchRequest request = new CaseSearchRequest("aaa", elasticsearchRequest);
            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(Collections.singletonList("aaa"))
                .withSearchRequest(elasticsearchRequest)
                .build();
            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class)))
                .thenReturn(request);

            assertThrows(ServiceException.class, () -> searchOperation.execute(crossCaseTypeSearchRequest, true));
        }

        @Test
        @DisplayName("test complex ES index case type id capturing group scenario")
        void testComplexESIndexCapturingGroupScenario() throws IOException {
            String caseDetailsElasticComplex = "{\n"
                + "   \"hits\":{\n"
                + "      \"total\":4,\n"
                + "      \"max_score\":null,\n"
                + "      \"hits\":[\n"
                + "         {\n"
                + "            \"_index\":\"casetypeid1_cases_cases-000001\"\n"
                + "         }\n"
                + "      ]\n"
                + "   }\n"
                + "}";

            when(applicationParams.getCasesIndexNameCaseTypeIdGroup()).thenReturn("(.+)(_cases.*)");
            JsonObject convertedObject = new Gson().fromJson(caseDetailsElasticComplex, JsonObject.class);
            SearchResult searchResult;
            Gson gson = new Gson();
            searchResult = new SearchResult(gson);
            searchResult.setSucceeded(true);
            searchResult.setJsonObject(convertedObject);
            searchResult.setJsonString(convertedObject.toString());
            searchResult.setPathToResult("hits/hits/_source");

            when(applicationParams.getCasesIndexNameCaseTypeIdGroup()).thenReturn("(.+)(_cases.*)");

            MultiSearchResult multiSearchResult = mock(MultiSearchResult.class);
            when(multiSearchResult.isSucceeded()).thenReturn(true);
            MultiSearchResult.MultiSearchResponse response = mock(MultiSearchResult.MultiSearchResponse.class);
            when(multiSearchResult.getResponses()).thenReturn(Collections.singletonList(response));
            Whitebox.setInternalState(response, "searchResult", searchResult);

            when(objectMapper.readValue(caseDetailsElastic, ElasticSearchCaseDetailsDTO.class))
                .thenReturn(caseDetailsDTO);
            when(mapper.dtosToCaseDetailsList(newArrayList(caseDetailsDTO))).thenReturn(newArrayList(caseDetails));
            when(jestClient.execute(any(MultiSearch.class))).thenReturn(multiSearchResult);

            CaseSearchRequest request = new CaseSearchRequest("casetypeid1_cases", elasticsearchRequest);
            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(Collections.singletonList("casetypeid1_cases"))
                .withSearchRequest(elasticsearchRequest)
                .build();
            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class)))
                .thenReturn(request);

            CaseSearchResult caseSearchResult = searchOperation.execute(crossCaseTypeSearchRequest, true);

            assertAll(
                () -> assertThat(caseSearchResult.getCaseTypesResults().get(0).getCaseTypeId(), 
                    equalTo("casetypeid1_cases"))
            );
        }
    }

    @Nested
    @DisplayName("Multi case type search")
    class MultiCaseTypeSearch {

        @Test
        @DisplayName("should execute search on Elasticsearch for multiple case types and return results")
        void searchShouldMapElasticSearchResultToSearchResult() throws IOException {
            MultiSearchResult multiSearchResult = mock(MultiSearchResult.class);
            when(multiSearchResult.isSucceeded()).thenReturn(true);

            JsonObject convertedObject = new Gson().fromJson(json, JsonObject.class);
            SearchResult searchResult;
            Gson gson = new Gson();
            searchResult = new SearchResult(gson);
            searchResult.setSucceeded(true);
            searchResult.setJsonObject(convertedObject);
            searchResult.setJsonString(convertedObject.toString());
            searchResult.setPathToResult("hits/hits/_source");


            MultiSearchResult.MultiSearchResponse response1 = mock(MultiSearchResult.MultiSearchResponse.class);
            Whitebox.setInternalState(response1, "searchResult", searchResult);
            MultiSearchResult.MultiSearchResponse response2 = mock(MultiSearchResult.MultiSearchResponse.class);
            Whitebox.setInternalState(response2, "searchResult", searchResult);
            when(multiSearchResult.getResponses()).thenReturn(asList(response1, response2));

            when(objectMapper.readValue(caseDetailsElastic, ElasticSearchCaseDetailsDTO.class))
                .thenReturn(caseDetailsDTO);
            when(mapper.dtosToCaseDetailsList(newArrayList(caseDetailsDTO))).thenReturn(newArrayList(caseDetails));
            when(jestClient.execute(any(MultiSearch.class))).thenReturn(multiSearchResult);

            CaseSearchRequest request1 = new CaseSearchRequest(CASE_TYPE_ID_1, elasticsearchRequest);
            CaseSearchRequest request2 = new CaseSearchRequest(CASE_TYPE_ID_2, elasticsearchRequest);
            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(asList(CASE_TYPE_ID_1, CASE_TYPE_ID_2))
                .withSearchRequest(elasticsearchRequest)
                .build();
            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class)))
                .thenReturn(request1, request2);

            CaseSearchResult caseSearchResult = searchOperation.execute(crossCaseTypeSearchRequest, true);

            assertAll(
                () -> assertThat(caseSearchResult.getCases(), equalTo(newArrayList())),
                () -> assertThat(caseSearchResult.getTotal(), equalTo(4L)),
                () -> verify(jestClient).execute(any(MultiSearch.class)),
                () -> verify(applicationParams, times(2)).getCasesIndexType(),
                () -> verify(caseSearchRequestSecurity, times(2))
                    .createSecuredSearchRequest(any(CaseSearchRequest.class)));
        }

    }

    @Nested
    @DisplayName("Multi case type search")
    class MultiCaseTypeSearchConsolidated {

        @Test
        @DisplayName("should execute search on Elasticsearch for multiple case types and return results")
        void searchShouldMapElasticSearchResultToSearchResult() throws IOException {
            MultiSearchResult multiSearchResult = mock(MultiSearchResult.class);
            when(multiSearchResult.isSucceeded()).thenReturn(true);

            JsonObject convertedObject = new Gson().fromJson(json, JsonObject.class);
            SearchResult searchResult;
            Gson gson = new Gson();
            searchResult = new SearchResult(gson);
            searchResult.setSucceeded(true);
            searchResult.setJsonObject(convertedObject);
            searchResult.setJsonString(convertedObject.toString());
            searchResult.setPathToResult("hits/hits/_source");


            MultiSearchResult.MultiSearchResponse response1 = mock(MultiSearchResult.MultiSearchResponse.class);
            Whitebox.setInternalState(response1, "searchResult", searchResult);
            MultiSearchResult.MultiSearchResponse response2 = mock(MultiSearchResult.MultiSearchResponse.class);
            Whitebox.setInternalState(response2, "searchResult", searchResult);
            when(multiSearchResult.getResponses()).thenReturn(asList(response1, response2));

            when(objectMapper.readValue(caseDetailsElastic, ElasticSearchCaseDetailsDTO.class))
                .thenReturn(caseDetailsDTO);
            when(mapper.dtosToCaseDetailsList(newArrayList(caseDetailsDTO))).thenReturn(newArrayList(caseDetails));
            when(jestClient.execute(any(MultiSearch.class))).thenReturn(multiSearchResult);

            CaseSearchRequest request1 = new CaseSearchRequest(CASE_TYPE_ID_1, elasticsearchRequest);
            CaseSearchRequest request2 = new CaseSearchRequest(CASE_TYPE_ID_2, elasticsearchRequest);
            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(asList(CASE_TYPE_ID_1, CASE_TYPE_ID_2))
                .withSearchRequest(elasticsearchRequest)
                .build();
            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class)))
                .thenReturn(request1, request2);

            CaseSearchResult caseSearchResult = searchOperation.execute(crossCaseTypeSearchRequest, true);

            assertAll(
                () -> assertThat(caseSearchResult.getCases(), equalTo(newArrayList())),
                () -> assertThat(caseSearchResult.getTotal(), equalTo(4L)),
                () -> verify(jestClient).execute(any(MultiSearch.class)),
                () -> verify(applicationParams, times(2)).getCasesIndexType(),
                () -> verify(caseSearchRequestSecurity, times(2))
                    .createSecuredSearchRequest(any(CaseSearchRequest.class)));
        }

    }

    @Nested
    @DisplayName("Elasticsearch search failure")
    class SearchFailure {

        @Test
        @DisplayName("should throw exception when Elasticsearch search does not succeed")
        void searchShouldReturnBadSearchRequestOnFailure() throws IOException {
            MultiSearchResult multiSearchResult = mock(MultiSearchResult.class);
            when(multiSearchResult.isSucceeded()).thenReturn(false);
            when(jestClient.execute(any(MultiSearch.class))).thenReturn(multiSearchResult);
            CaseSearchRequest request = new CaseSearchRequest(CASE_TYPE_ID_1, elasticsearchRequest);
            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(Collections.singletonList(CASE_TYPE_ID_1))
                .withSearchRequest(elasticsearchRequest)
                .build();
            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class)))
                .thenReturn(request);

            assertThrows(BadSearchRequest.class, () -> searchOperation.execute(crossCaseTypeSearchRequest, true));
        }

        @Test
        @DisplayName("should throw exception when Elasticsearch multi-search response returns error")
        void searchShouldReturnBadSearchRequestOnResponseError() throws IOException {
            MultiSearchResult.MultiSearchResponse response = mock(MultiSearchResult.MultiSearchResponse.class);
            JsonElement error = mock(JsonElement.class);
            Whitebox.setInternalState(response, "isError", true);
            Whitebox.setInternalState(response, "error", error);

            JsonObject errorObject = new JsonObject();
            errorObject.addProperty(MULTI_SEARCH_ERROR_MSG_ROOT_CAUSE, "error msg");
            when(response.error.getAsJsonObject()).thenReturn(errorObject);
            MultiSearchResult multiSearchResult = mock(MultiSearchResult.class);
            when(multiSearchResult.isSucceeded()).thenReturn(true);
            when(multiSearchResult.getResponses()).thenReturn(Collections.singletonList(response));
            when(jestClient.execute(any(MultiSearch.class))).thenReturn(multiSearchResult);
            CaseSearchRequest request = new CaseSearchRequest(CASE_TYPE_ID_1, elasticsearchRequest);
            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(asList(CASE_TYPE_ID_1, CASE_TYPE_ID_2))
                .withSearchRequest(elasticsearchRequest)
                .build();
            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class)))
                .thenReturn(request);

            assertThrows(BadSearchRequest.class, () -> searchOperation.execute(crossCaseTypeSearchRequest, true));
        }

    }

}
