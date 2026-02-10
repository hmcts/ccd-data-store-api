package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.MsearchRequest;
import co.elastic.clients.elasticsearch.core.MsearchResponse;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.SearchRequestBody;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.QUERY;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.createFailureItem;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.createHit;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.createMsearchResponse;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.createSuccessItem;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.createSuccessItemWithNoHits;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.createTotalHits;


class ElasticsearchCaseSearchOperationTest {

    private static final String INDEX_NAME_FORMAT = "%s_cases";
    private static final String CASE_TYPE_ID_1 = "casetypeid1";
    private static final String CASE_TYPE_ID_2 = "casetypeid2";
    private static final String INDEX_TYPE = "case";
    private final String caseDetailsElastic = "{some case details}";
    private final String caseDetailsElasticComplex = """
        {
          "hits": {
            "total": 4,
            "max_score": null,
            "hits": [
              {
                "_index": "casetypeid1_cases-000001"
              }
            ]
          }
        }
        """;

    private ElasticsearchCaseSearchOperation searchOperation;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CaseDetailsMapper mapper;

    @Mock
    private ElasticSearchCaseDetailsDTO caseDetailsDTO;

    @Mock
    private CaseSearchRequestSecurity caseSearchRequestSecurity;

    private final ObjectNode searchRequestJsonNode = JsonNodeFactory.instance.objectNode();

    private final ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(searchRequestJsonNode);

    private final String json = """
        {
          "took": 96,
          "timed_out": false,
          "_shards": {
            "total": 2,
            "successful": 2,
            "skipped": 0,
            "failed": 0
          },
          "hits": {
            "total": 2,
            "max_score": 0.18232156,
            "hits": [
              {
                "_index": "casetypeid1_cases-000001",
                "_type": "_doc",
                "_id": "355",
                "_score": 0.18232156,
                "_source": {
                  "jurisdiction": "AUTOTEST1",
                  "case_type_id": "AAT",
                  "data": {
                    "FixedRadioListField": null,
                    "TextAreaField": null,
                    "ComplexField": {
                      "ComplexTextField": null,
                      "ComplexFixedListField": null,
                      "ComplexNestedField": {
                        "NestedNumberField": null,
                        "NestedCollectionTextField": []
                      }
                    },
                    "EmailField": null,
                    "TextField": null,
                    "AddressUKField": {
                      "Country": null,
                      "AddressLine3": null,
                      "County": null,
                      "AddressLine1": null,
                      "PostCode": null,
                      "AddressLine2": null,
                      "PostTown": null
                    },
                    "CollectionField": [],
                    "MoneyGBPField": null,
                    "DateField": null,
                    "MultiSelectListField": [],
                    "PhoneUKField": null,
                    "YesOrNoField": null,
                    "NumberField": null,
                    "DateTimeField": null,
                    "FixedListField": null
                  },
                  "created_date": "2020-06-30T14:47:26.061Z",
                  "id": 355,
                  "last_modified": "2020-06-30T14:52:36.335Z",
                  "@timestamp": "2020-07-16T22:58:33.430Z",
                  "index_id": "aat_cases",
                  "@version": "1",
                  "data_classification": {
                    "FixedRadioListField": "PUBLIC",
                    "TextAreaField": "PUBLIC",
                    "ComplexField": {
                      "classification": "PUBLIC",
                      "value": {
                        "ComplexTextField": "PUBLIC",
                        "ComplexFixedListField": "PUBLIC",
                        "ComplexNestedField": {
                          "classification": "PUBLIC",
                          "value": {
                            "NestedNumberField": "PUBLIC",
                            "NestedCollectionTextField": {
                              "classification": "PUBLIC",
                              "value": []
                            }
                          }
                        }
                      }
                    },
                    "EmailField": "PUBLIC",
                    "TextField": "PUBLIC",
                    "AddressUKField": {
                      "classification": "PUBLIC",
                      "value": {
                        "Country": "PUBLIC",
                        "AddressLine3": "PUBLIC",
                        "County": "PUBLIC",
                        "AddressLine1": "PUBLIC",
                        "PostCode": "PUBLIC",
                        "AddressLine2": "PUBLIC",
                        "PostTown": "PUBLIC"
                      }
                    },
                    "CollectionField": {
                      "classification": "PUBLIC",
                      "value": []
                    },
                    "MoneyGBPField": "PUBLIC",
                    "DateField": "PUBLIC",
                    "MultiSelectListField": "PUBLIC",
                    "PhoneUKField": "PUBLIC",
                    "YesOrNoField": "PUBLIC",
                    "NumberField": "PUBLIC",
                    "DateTimeField": "PUBLIC",
                    "FixedListField": "PUBLIC"
                  },
                  "security_classification": "PUBLIC",
                  "state": "TODO",
                  "reference": 1593528446017551
                }
              },
              {
                "_index": "casetypeid1_cases-000001",
                "_type": "_doc",
                "_id": "357",
                "_score": 0.18232156,
                "_source": {
                  "jurisdiction": "AUTOTEST1",
                  "case_type_id": "AAT",
                  "data": {
                    "FixedRadioListField": null,
                    "TextAreaField": null,
                    "ComplexField": {
                      "ComplexTextField": null,
                      "ComplexFixedListField": null,
                      "ComplexNestedField": {
                        "NestedNumberField": null,
                        "NestedCollectionTextField": []
                      }
                    },
                    "EmailField": "email1@gmail.com",
                    "TextField": "Text Field 1",
                    "AddressUKField": {
                      "Country": null,
                      "AddressLine3": null,
                      "County": null,
                      "AddressLine1": null,
                      "PostCode": null,
                      "AddressLine2": null,
                      "PostTown": null
                    },
                    "CollectionField": [],
                    "MoneyGBPField": null,
                    "DateField": null,
                    "MultiSelectListField": [],
                    "PhoneUKField": null,
                    "YesOrNoField": null,
                    "NumberField": null,
                    "DateTimeField": null,
                    "FixedListField": null
                  },
                  "created_date": "2020-07-07T15:12:53.258Z",
                  "id": 357,
                  "last_modified": "2020-07-07T15:14:04.635Z",
                  "@timestamp": "2020-07-16T22:58:33.435Z",
                  "index_id": "aat_cases",
                  "@version": "1",
                  "data_classification": {
                    "FixedRadioListField": "PUBLIC",
                    "TextAreaField": "PUBLIC",
                    "ComplexField": {
                      "classification": "PUBLIC",
                      "value": {
                        "ComplexTextField": "PUBLIC",
                        "ComplexFixedListField": "PUBLIC",
                        "ComplexNestedField": {
                          "classification": "PUBLIC",
                          "value": {
                            "NestedNumberField": "PUBLIC",
                            "NestedCollectionTextField": {
                              "classification": "PUBLIC",
                              "value": []
                            }
                          }
                        }
                      }
                    },
                    "EmailField": "PUBLIC",
                    "TextField": "PUBLIC",
                    "AddressUKField": {
                      "classification": "PUBLIC",
                      "value": {
                        "Country": "PUBLIC",
                        "AddressLine3": "PUBLIC",
                        "County": "PUBLIC",
                        "AddressLine1": "PUBLIC",
                        "PostCode": "PUBLIC",
                        "AddressLine2": "PUBLIC",
                        "PostTown": "PUBLIC"
                      }
                    },
                    "CollectionField": {
                      "classification": "PUBLIC",
                      "value": []
                    },
                    "MoneyGBPField": "PUBLIC",
                    "DateField": "PUBLIC",
                    "MultiSelectListField": "PUBLIC",
                    "PhoneUKField": "PUBLIC",
                    "YesOrNoField": "PUBLIC",
                    "NumberField": "PUBLIC",
                    "DateTimeField": "PUBLIC",
                    "FixedListField": "PUBLIC"
                  },
                  "security_classification": "PUBLIC",
                  "state": "TODO",
                  "reference": 1594134773278525
                }
              }
            ]
          }
        }
        """;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(applicationParams.getCasesIndexNameFormat()).thenReturn(INDEX_NAME_FORMAT);
        when(applicationParams.getCasesIndexNameCaseTypeIdGroup()).thenReturn("(.+)(_cases.*)");
        when(applicationParams.getCasesIndexNameCaseTypeIdGroupPosition()).thenReturn(1);
        when(applicationParams.getCasesIndexType()).thenReturn(INDEX_TYPE);
        ObjectNode matchAllQuery = JsonNodeFactory.instance.objectNode();
        matchAllQuery.set("match_all", JsonNodeFactory.instance.objectNode());
        searchRequestJsonNode.set(QUERY, matchAllQuery);

        searchOperation = new ElasticsearchCaseSearchOperation(
            elasticsearchClient,
            objectMapper,
            mapper,
            applicationParams,
            caseSearchRequestSecurity
        );
    }

    @Nested
    @DisplayName("Named index search")
    class NamedIndexSearch {

        @Test
        @DisplayName("should execute search on Elasticsearch for a named index and return results")
        void testSearchNamedIndex() throws IOException {
            // GIVEN
            when(applicationParams.getCasesIndexNameFormat()).thenReturn("%s_cases");
            when(applicationParams.getCasesIndexNameCaseTypeIdGroup()).thenReturn("(.+)(_cases.*)");
            when(applicationParams.getCasesIndexNameCaseTypeIdGroupPosition()).thenReturn(1);

            final String indexName = "global_search";
            final CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(List.of(CASE_TYPE_ID_1, CASE_TYPE_ID_2))
                .withSearchRequest(elasticsearchRequest)
                .withSearchIndex(new SearchIndex(indexName, "_doc"))
                .build();
            final CaseSearchRequest caseSearchRequest = new CaseSearchRequest(CASE_TYPE_ID_1, elasticsearchRequest);

            // Secure the original request
            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CrossCaseTypeSearchRequest.class)))
                .thenReturn(crossCaseTypeSearchRequest);
            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class)))
                .thenReturn(new CaseSearchRequest(indexName, elasticsearchRequest));
            // Simulate a named-index response with no hits
            Hit<ElasticSearchCaseDetailsDTO> hit = new Hit.Builder<ElasticSearchCaseDetailsDTO>()
                .source(new ElasticSearchCaseDetailsDTO())
                .index(indexName)
                .build();
            MultiSearchResponseItem<ElasticSearchCaseDetailsDTO> responseItem = createSuccessItemWithNoHits(indexName);
            MsearchResponse<ElasticSearchCaseDetailsDTO> msearchResponse = createMsearchResponse(List.of(responseItem));

            when(elasticsearchClient.msearch(any(MsearchRequest.class),
                eq(ElasticSearchCaseDetailsDTO.class)))
                .thenReturn(msearchResponse);

            // WHEN
            CaseSearchResult caseSearchResult = searchOperation.execute(crossCaseTypeSearchRequest,true);

            // THEN
            assertAll(
                () -> assertThat(caseSearchResult.getCases()).isEmpty(),
                () -> assertThat(caseSearchResult.getTotal()).isEqualTo(0L),
                () -> verify(elasticsearchClient).msearch(any(MsearchRequest.class),
                    eq(ElasticSearchCaseDetailsDTO.class)),
                () -> verify(caseSearchRequestSecurity)
                    .createSecuredSearchRequest(any(CrossCaseTypeSearchRequest.class))
            );
        }

        @Test
        @DisplayName("should preserve search_after in msearch request body")
        void shouldPreserveSearchAfterInMsearchRequestBody() throws Exception {
            final String indexName = "global_search";

            ObjectNode searchRequestNode = JsonNodeFactory.instance.objectNode();
            searchRequestNode.set(QUERY, JsonNodeFactory.instance.objectNode()
                .set("match_all", JsonNodeFactory.instance.objectNode()));

            ArrayNode sort = JsonNodeFactory.instance.arrayNode();
            sort.add(JsonNodeFactory.instance.objectNode()
                .set("id", JsonNodeFactory.instance.objectNode().put("order", "asc")));
            searchRequestNode.set("sort", sort);

            ArrayNode searchAfter = JsonNodeFactory.instance.arrayNode();
            searchAfter.add(123456789L);
            searchAfter.add("case-2");
            searchRequestNode.set("search_after", searchAfter);

            ElasticsearchRequest requestWithSearchAfter = new ElasticsearchRequest(searchRequestNode);
            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(List.of(CASE_TYPE_ID_1, CASE_TYPE_ID_2))
                .withSearchRequest(requestWithSearchAfter)
                .withSearchIndex(new SearchIndex(indexName, "_doc"))
                .build();

            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CrossCaseTypeSearchRequest.class)))
                .thenReturn(crossCaseTypeSearchRequest);

            MultiSearchResponseItem<ElasticSearchCaseDetailsDTO> responseItem = createSuccessItemWithNoHits(indexName);
            MsearchResponse<ElasticSearchCaseDetailsDTO> msearchResponse = createMsearchResponse(List.of(responseItem));
            when(elasticsearchClient.msearch(any(MsearchRequest.class), eq(ElasticSearchCaseDetailsDTO.class)))
                .thenReturn(msearchResponse);

            searchOperation.execute(crossCaseTypeSearchRequest, true);

            ArgumentCaptor<MsearchRequest> requestCaptor = ArgumentCaptor.forClass(MsearchRequest.class);
            verify(elasticsearchClient).msearch(requestCaptor.capture(), eq(ElasticSearchCaseDetailsDTO.class));

            JsonNode requestBody = toJson(requestCaptor.getValue().searches().getFirst().body());

            assertAll(
                () -> assertThat(requestBody.has("search_after")).isTrue(),
                () -> assertThat(requestBody.get("search_after").get(0).asLong()).isEqualTo(123456789L),
                () -> assertThat(requestBody.get("search_after").get(1).asText()).isEqualTo("case-2")
            );
        }
    }

    @Nested
    @DisplayName("Single case type search")
    class SingleCaseTypeDefinitionSearch {

        @Test
        @DisplayName("should execute search on Elasticsearch for a single case type and return results")
        void searchShouldMapElasticSearchResultToSearchResult() throws IOException {
            ElasticSearchCaseDetailsDTO dto = new ElasticSearchCaseDetailsDTO();
            CaseDetails mappedCaseDetails = mock(CaseDetails.class);

            when(mapper.dtosToCaseDetailsList(List.of(dto))).thenReturn(List.of(mappedCaseDetails));

            Hit<ElasticSearchCaseDetailsDTO> hit = new Hit.Builder<ElasticSearchCaseDetailsDTO>()
                .source(dto)
                .index("casetypeid1_cases-000001")
                .build();

            var responseItem = new MultiSearchResponseItem.Builder<ElasticSearchCaseDetailsDTO>()
                .result(r -> r.hits(h ->
                        h.hits(List.of(hit)).total(t -> t.value(2L).relation(TotalHitsRelation.Eq)))
                    .took(1)
                    .timedOut(false)
                    .shards(s -> s.total(1).successful(1).skipped(0).failed(0))
                )
                .build();

            var msearchResponse = new MsearchResponse.Builder<ElasticSearchCaseDetailsDTO>()
                .responses(List.of(responseItem))
                .took(1)
                .build();

            when(elasticsearchClient.msearch(any(MsearchRequest.class),
                eq(ElasticSearchCaseDetailsDTO.class))).thenReturn(msearchResponse);

            CaseSearchRequest securedRequest = new CaseSearchRequest(CASE_TYPE_ID_1, elasticsearchRequest);

            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class)))
                .thenReturn(securedRequest);

            //when(applicationParams.getCasesIndexNameFormat()).thenReturn("%s_cases");
            when(applicationParams.getCasesIndexNameCaseTypeIdGroup()).thenReturn("(.+)(_cases.*)");
            when(applicationParams.getCasesIndexNameCaseTypeIdGroupPosition()).thenReturn(1);
            //when(applicationParams.getCasesIndexType()).thenReturn("_doc");

            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(List.of(CASE_TYPE_ID_1))
                .withSearchRequest(elasticsearchRequest)
                .build();

            // WHEN
            CaseSearchResult caseSearchResult = searchOperation.execute(crossCaseTypeSearchRequest,
                true);

            // THEN
            assertAll(
                () -> assertThat(caseSearchResult.getCases()).isEqualTo(List.of(mappedCaseDetails)),
                () -> assertThat(caseSearchResult.getTotal()).isEqualTo(2L),
                () -> assertThat(caseSearchResult.getCaseTypesResults()).hasSize(1),
                () -> verify(elasticsearchClient).msearch(any(MsearchRequest.class),
                    eq(ElasticSearchCaseDetailsDTO.class)),
                //() -> verify(applicationParams).getCasesIndexNameFormat(),
                //() -> verify(applicationParams).getCasesIndexType(),
                () -> verify(caseSearchRequestSecurity).createSecuredSearchRequest(any(CaseSearchRequest.class)),
                () -> verify(mapper).dtosToCaseDetailsList(any())
            );
        }

        @Test
        @DisplayName("should return ServiceError when ES index case type id capturing group is "
            + "not matching the ES index")
        void searchShouldReturnServiceErrorWhenESIndexCaseTypeIdCapturingGroupNotMatching() throws IOException {
            // Given
            String invalidRegex = "a_regex_with_not_matching_(capturing)_group";
            when(applicationParams.getCasesIndexNameCaseTypeIdGroup()).thenReturn(invalidRegex);
            when(applicationParams.getCasesIndexNameCaseTypeIdGroupPosition()).thenReturn(1);
            when(applicationParams.getCasesIndexNameFormat()).thenReturn("%s_cases");

            Hit<ElasticSearchCaseDetailsDTO> hit = new Hit.Builder<ElasticSearchCaseDetailsDTO>()
                .source(new ElasticSearchCaseDetailsDTO())
                .index("some_index_that_wont_match") // Will not match the regex
                .build();

            var responseItem = createSuccessItem("some_index_that_wont_match");

            var msearchResponse = new MsearchResponse.Builder<ElasticSearchCaseDetailsDTO>()
                .responses(List.of(responseItem))
                .took(1)
                .build();

            when(elasticsearchClient.msearch(any(MsearchRequest.class),
                eq(ElasticSearchCaseDetailsDTO.class))).thenReturn(msearchResponse);

            CaseSearchRequest securedRequest = new CaseSearchRequest(CASE_TYPE_ID_1, elasticsearchRequest);
            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class)))
                .thenReturn(securedRequest);

            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(List.of(CASE_TYPE_ID_1))
                .withSearchRequest(elasticsearchRequest)
                .build();

            assertThrows(ServiceException.class,
                () -> searchOperation.execute(crossCaseTypeSearchRequest, true));
        }

        @Test
        @DisplayName("should return ServiceError when ES index case type id capturing group is not specified")
        void searchShouldReturnServiceErrorWhenESIndexCaseTypeIdCapturingGroupNotSpecified() throws IOException {

            String regexWithNoCapturingGroup = "a_regex_with_no_capturing_group";
            when(applicationParams.getCasesIndexNameCaseTypeIdGroup()).thenReturn(regexWithNoCapturingGroup);
            when(applicationParams.getCasesIndexNameCaseTypeIdGroupPosition()).thenReturn(1); // invalid since no group
            when(applicationParams.getCasesIndexNameFormat()).thenReturn("%s_cases");

            String indexName = "unknown_cases-000001";
            MultiSearchResponseItem<ElasticSearchCaseDetailsDTO> responseItem = createSuccessItem(indexName);;

            MsearchResponse<ElasticSearchCaseDetailsDTO> msearchResponse =
                createMsearchResponse(List.of(responseItem));

            when(elasticsearchClient.msearch(any(MsearchRequest.class),
                eq(ElasticSearchCaseDetailsDTO.class))).thenReturn(msearchResponse);

            ElasticsearchClient elasticsearchClient = mock(ElasticsearchClient.class);
            ElasticsearchTransport transport = mock(ElasticsearchTransport.class);
            JsonpMapper jsonpMapper = mock(JsonpMapper.class);
            when(transport.jsonpMapper()).thenReturn(jsonpMapper);
            when(elasticsearchClient._transport()).thenReturn(transport);

            CaseSearchRequest securedRequest = new CaseSearchRequest(CASE_TYPE_ID_1, elasticsearchRequest);
            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class)))
                .thenReturn(securedRequest);

            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(List.of(CASE_TYPE_ID_1))
                .withSearchRequest(elasticsearchRequest)
                .build();

            assertThrows(ServiceException.class,
                () -> searchOperation.execute(crossCaseTypeSearchRequest, true));
        }

        @Test
        @DisplayName("should return ServiceError when ES index case type id capturing group "
            + "is not associated to any case type")
        void searchShouldReturnServiceErrorWhenESIndexCapturingGroupValueNotInCaseTypeList() throws IOException {
            // GIVEN
            String regex = "(.+)(_cases.*)";
            when(applicationParams.getCasesIndexNameCaseTypeIdGroup()).thenReturn(regex);
            when(applicationParams.getCasesIndexNameCaseTypeIdGroupPosition()).thenReturn(1);
            when(applicationParams.getCasesIndexNameFormat()).thenReturn("%s_cases");

            String indexName = "unknown_cases-000001";

            Hit<ElasticSearchCaseDetailsDTO> hit = new Hit.Builder<ElasticSearchCaseDetailsDTO>()
                .source(new ElasticSearchCaseDetailsDTO())
                .index(indexName)
                .build();

            MultiSearchResponseItem<ElasticSearchCaseDetailsDTO> responseItem =
                createSuccessItem(indexName);

            MsearchResponse<ElasticSearchCaseDetailsDTO> msearchResponse =
                createMsearchResponse(List.of(responseItem));

            when(elasticsearchClient.msearch(any(MsearchRequest.class),
                eq(ElasticSearchCaseDetailsDTO.class))).thenReturn(msearchResponse);

            CaseSearchRequest securedRequest = new CaseSearchRequest("aaa", elasticsearchRequest);
            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class)))
                .thenReturn(securedRequest);

            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(List.of("aaa")) // Doesn't match "unknown" extracted from index name
                .withSearchRequest(elasticsearchRequest)
                .build();

            assertThrows(ServiceException.class, () ->
                searchOperation.execute(crossCaseTypeSearchRequest, true)
            );
        }

        @Test
        @DisplayName("test complex ES index case type id capturing group scenario")
        void testComplexESIndexCapturingGroupScenario() throws IOException {

            // TODO: compare with old test - json block!
            when(applicationParams.getCasesIndexNameCaseTypeIdGroup()).thenReturn("(.+)(_cases.*)");
            when(applicationParams.getCasesIndexNameCaseTypeIdGroupPosition()).thenReturn(1);
            when(applicationParams.getCasesIndexNameFormat()).thenReturn("%s_cases");

            String complexIndexName = "casetypeid1_cases_cases-000001";

            List<String> caseTypeIds = List.of("casetypeid1_cases");

            Hit<ElasticSearchCaseDetailsDTO> hit = createHit(complexIndexName);

            MultiSearchResponseItem<ElasticSearchCaseDetailsDTO> responseItem =
                createSuccessItem(complexIndexName);

            MsearchResponse<ElasticSearchCaseDetailsDTO> msearchResponse =
                new MsearchResponse.Builder<ElasticSearchCaseDetailsDTO>()
                    .responses(List.of(responseItem))
                    .took(1)
                    .build();

            when(elasticsearchClient.msearch(any(MsearchRequest.class),
                eq(ElasticSearchCaseDetailsDTO.class))).thenReturn(msearchResponse);

            CaseSearchRequest securedRequest = new CaseSearchRequest("casetypeid1_cases",
                elasticsearchRequest);
            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class)))
                .thenReturn(securedRequest);

            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(caseTypeIds)
                .withSearchRequest(elasticsearchRequest)
                .build();

            CaseSearchResult result = searchOperation.execute(crossCaseTypeSearchRequest, true);

            assertAll(
                () -> assertThat(result.getCaseTypesResults()).hasSize(1),
                () -> assertThat(result.getCaseTypesResults().getFirst().getCaseTypeId())
                    .isEqualTo("casetypeid1_cases"),
                () -> assertThat(result.getTotal()).isEqualTo(1L)
            );
        }
    }

    @Nested
    @DisplayName("Multi case type search")
    class MultiCaseTypeSearch {

        @Test
        @DisplayName("should execute search on Elasticsearch for multiple case types and return results")
        void searchShouldMapElasticSearchResultToSearchResult() throws IOException {

            when(applicationParams.getCasesIndexNameFormat()).thenReturn("%s_cases");
            when(applicationParams.getCasesIndexNameCaseTypeIdGroup()).thenReturn("(.+)(_cases.*)");
            when(applicationParams.getCasesIndexNameCaseTypeIdGroupPosition()).thenReturn(1);
            when(applicationParams.getCasesIndexType()).thenReturn("_doc");

            CaseSearchRequest request1 = new CaseSearchRequest(CASE_TYPE_ID_1, elasticsearchRequest);
            CaseSearchRequest request2 = new CaseSearchRequest(CASE_TYPE_ID_2, elasticsearchRequest);

            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class)))
                .thenReturn(request1, request2);

            ElasticSearchCaseDetailsDTO dto = new ElasticSearchCaseDetailsDTO();
            CaseDetails caseDetails1 = mock(CaseDetails.class);
            CaseDetails caseDetails2 = mock(CaseDetails.class);

            //when(mapper.dtosToCaseDetailsList(any()))
            //    .thenReturn(List.of(caseDetails1))
            //    .thenReturn(List.of(caseDetails2));

            Hit<ElasticSearchCaseDetailsDTO> hit1 = createHit("casetypeid1_cases-000001");
            MultiSearchResponseItem<ElasticSearchCaseDetailsDTO> response1 =
                createSuccessItem(hit1, createTotalHits(1L));

            Hit<ElasticSearchCaseDetailsDTO> hit2 = createHit("casetypeid2_cases-000001");
            MultiSearchResponseItem<ElasticSearchCaseDetailsDTO> response2 =
                createSuccessItem(hit2, createTotalHits(3L));

            MsearchResponse<ElasticSearchCaseDetailsDTO> msearchResponse = createMsearchResponse(
                List.of(response1, response2));

            when(elasticsearchClient.msearch(any(MsearchRequest.class),
                eq(ElasticSearchCaseDetailsDTO.class))).thenReturn(msearchResponse);

            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(List.of(CASE_TYPE_ID_1, CASE_TYPE_ID_2))
                .withSearchRequest(elasticsearchRequest)
                .build();

            CaseSearchResult caseSearchResult = searchOperation.execute(crossCaseTypeSearchRequest,true);

            assertAll(
                //() -> assertThat(caseSearchResult.getCases()).containsExactly(caseDetails1, caseDetails2),
                () -> assertThat(caseSearchResult.getTotal()).isEqualTo(4L),
                () -> assertThat(caseSearchResult.getCaseTypesResults()).hasSize(2),
                () -> assertThat(caseSearchResult.getCaseTypesResults())
                    .extracting("caseTypeId")
                    .containsExactlyInAnyOrder(CASE_TYPE_ID_1, CASE_TYPE_ID_2),
                () -> verify(elasticsearchClient).msearch(any(MsearchRequest.class),
                    eq(ElasticSearchCaseDetailsDTO.class)),
                //() -> verify(applicationParams, times(2)).getCasesIndexType(),
                () -> verify(caseSearchRequestSecurity, times(2))
                    .createSecuredSearchRequest(any(CaseSearchRequest.class)),
                () -> verify(mapper, times(2)).dtosToCaseDetailsList(any())
            );
        }

    }

    @Nested
    @DisplayName("Multi case type search")
    class MultiCaseTypeSearchConsolidated {

        @Test
        @DisplayName("should execute search on Elasticsearch for multiple case types and return results")
        void searchShouldMapElasticSearchResultToSearchResult() throws IOException {

            when(applicationParams.getCasesIndexNameFormat()).thenReturn("%s_cases");
            when(applicationParams.getCasesIndexNameCaseTypeIdGroup()).thenReturn("(.+)(_cases.*)");
            when(applicationParams.getCasesIndexNameCaseTypeIdGroupPosition()).thenReturn(1);
            when(applicationParams.getCasesIndexType()).thenReturn("_doc");

            CaseSearchRequest request1 = new CaseSearchRequest(CASE_TYPE_ID_1, elasticsearchRequest);
            CaseSearchRequest request2 = new CaseSearchRequest(CASE_TYPE_ID_2, elasticsearchRequest);

            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(List.of(CASE_TYPE_ID_1, CASE_TYPE_ID_2))
                .withSearchRequest(elasticsearchRequest)
                .build();

            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class)))
                .thenReturn(request1, request2);

            ElasticSearchCaseDetailsDTO dto = new ElasticSearchCaseDetailsDTO();
            CaseDetails mappedCaseDetails = mock(CaseDetails.class);
            when(mapper.dtosToCaseDetailsList(any())).thenReturn(List.of(mappedCaseDetails));

            Hit<ElasticSearchCaseDetailsDTO> hit1 = new Hit.Builder<ElasticSearchCaseDetailsDTO>()
                .index("casetypeid1_cases-000001")
                .source(dto)
                .build();
            TotalHits totalHits1 = createTotalHits(2L);
            MultiSearchResponseItem<ElasticSearchCaseDetailsDTO> item1 =
                createSuccessItem(hit1, totalHits1);

            Hit<ElasticSearchCaseDetailsDTO> hit2 = new Hit.Builder<ElasticSearchCaseDetailsDTO>()
                .index("casetypeid2_cases-000001")
                .source(dto)
                .build();
            TotalHits totalHits2 = createTotalHits(4L);
            MultiSearchResponseItem<ElasticSearchCaseDetailsDTO> item2 =
                createSuccessItem(hit2, totalHits2);

            MsearchResponse<ElasticSearchCaseDetailsDTO> msearchResponse =
                createMsearchResponse(List.of(item1, item2));

            when(elasticsearchClient.msearch(any(MsearchRequest.class),
                eq(ElasticSearchCaseDetailsDTO.class))).thenReturn(msearchResponse);

            CaseSearchResult caseSearchResult = searchOperation.execute(crossCaseTypeSearchRequest,
                true);

            assertAll(
                () -> assertThat(caseSearchResult.getCases()).containsExactly(mappedCaseDetails, mappedCaseDetails),
                () -> assertThat(caseSearchResult.getTotal()).isEqualTo(6L),
                () -> assertThat(caseSearchResult.getCaseTypesResults()).hasSize(2),
                () -> assertThat(caseSearchResult.getCaseTypesResults())
                    .extracting("caseTypeId")
                    .containsExactlyInAnyOrder(CASE_TYPE_ID_1, CASE_TYPE_ID_2),
                () -> verify(elasticsearchClient).msearch(any(MsearchRequest.class),
                    eq(ElasticSearchCaseDetailsDTO.class)),
                //() -> verify(applicationParams, times(2)).getCasesIndexType(),
                () -> verify(caseSearchRequestSecurity, times(2))
                    .createSecuredSearchRequest(any(CaseSearchRequest.class)),
                () -> verify(mapper, times(2)).dtosToCaseDetailsList(any())
            );
        }

    }

    @Nested
    @DisplayName("Elasticsearch search failure")
    class SearchFailure {

        @Test
        @DisplayName("should throw exception when Elasticsearch search does not succeed")
        void searchShouldReturnBadSearchRequestOnFailure() throws IOException {

            when(applicationParams.getCasesIndexNameFormat()).thenReturn("%s_cases");
            when(applicationParams.getCasesIndexNameCaseTypeIdGroup()).thenReturn("(.+)(_cases.*)");
            when(applicationParams.getCasesIndexNameCaseTypeIdGroupPosition()).thenReturn(1);

            Map<String, Object> errorMap = Map.of(
                "error", "Failed to search",
                "status", 500);

            MultiSearchResponseItem<ElasticSearchCaseDetailsDTO> errorItem = createFailureItem(errorMap);

            MsearchResponse<ElasticSearchCaseDetailsDTO> msearchResponse =
                createMsearchResponse(List.of(errorItem));

            when(elasticsearchClient.msearch(any(MsearchRequest.class),
                eq(ElasticSearchCaseDetailsDTO.class))).thenReturn(msearchResponse);

            CaseSearchRequest securedRequest = new CaseSearchRequest(CASE_TYPE_ID_1, elasticsearchRequest);
            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class)))
                .thenReturn(securedRequest);

            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(List.of(CASE_TYPE_ID_1))
                .withSearchRequest(elasticsearchRequest)
                .build();

            assertThrows(BadSearchRequest.class, () ->
                searchOperation.execute(crossCaseTypeSearchRequest, true)
            );
        }

        @Test
        @DisplayName("should throw exception when Elasticsearch multi-search response returns error")
        void searchShouldReturnBadSearchRequestOnResponseError() throws IOException {

            when(applicationParams.getCasesIndexNameFormat()).thenReturn("%s_cases");
            when(applicationParams.getCasesIndexNameCaseTypeIdGroup()).thenReturn("(.+)(_cases.*)");
            when(applicationParams.getCasesIndexNameCaseTypeIdGroupPosition()).thenReturn(1);

            Map<String, Object> errorMap = Map.of(
                "error", "Simulated error",
                "status", 400);

            MultiSearchResponseItem<ElasticSearchCaseDetailsDTO> failedItem = createFailureItem(errorMap);

            MsearchResponse<ElasticSearchCaseDetailsDTO> msearchResponse =
                createMsearchResponse(List.of(failedItem));

            when(elasticsearchClient.msearch(any(MsearchRequest.class),
                eq(ElasticSearchCaseDetailsDTO.class))).thenReturn(msearchResponse);

            CaseSearchRequest securedRequest = new CaseSearchRequest(CASE_TYPE_ID_1, elasticsearchRequest);
            when(caseSearchRequestSecurity.createSecuredSearchRequest(any(CaseSearchRequest.class)))
                .thenReturn(securedRequest);

            CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(List.of(CASE_TYPE_ID_1, CASE_TYPE_ID_2))
                .withSearchRequest(elasticsearchRequest)
                .build();

            assertThrows(BadSearchRequest.class, () ->
                searchOperation.execute(crossCaseTypeSearchRequest, true)
            );
        }

    }

    private JsonNode toJson(SearchRequestBody body) throws Exception {
        StringWriter writer = new StringWriter();
        var mapper = new JacksonJsonpMapper();
        var generator = mapper.jsonProvider().createGenerator(writer);
        body.serialize(generator, mapper);
        generator.close();
        return new ObjectMapper().readTree(writer.toString());
    }

}
