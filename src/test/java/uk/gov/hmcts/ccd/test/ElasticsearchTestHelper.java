package uk.gov.hmcts.ccd.test;

import co.elastic.clients.elasticsearch._types.ErrorCause;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.elasticsearch.core.MsearchResponse;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.ccd.ElasticsearchBaseTest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.ElasticSearchCaseDetailsDTO;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ElasticsearchTestHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchTestHelper.class);

    public static final String DATA_PREFIX = "data.";
    public static final String ALIAS_PREFIX = "alias.";
    public static final String VALUE_SUFFIX = ".value";
    public static final String CASE_TYPE_A = "AAT";
    public static final String CASE_TYPE_B = "MAPPER";
    public static final String CASE_TYPE_C = "SECURITY";
    public static final String CASE_TYPE_D = "RESTRICTED_SECURITY";

    public static final String NUMBER_FIELD = "NumberField";
    public static final String YES_OR_NO_FIELD = "YesOrNoField";
    public static final String TEXT_FIELD = "TextField";
    public static final String DATE_FIELD = "DateField";
    public static final String PHONE_FIELD = "PhoneUKField";
    public static final String COUNTRY_FIELD = "AddressUKField.Country";
    public static final String COLLECTION_FIELD = "CollectionField";
    public static final String DATE_TIME_FIELD = "DateTimeField";
    public static final String EMAIL_FIELD = "EmailField";
    public static final String FIXED_LIST_FIELD = "FixedListField";
    public static final String FIXED_RADIO_LIST_FIELD = "FixedRadioListField";
    public static final String MONEY_FIELD = "MoneyGBPField";
    public static final String MULTI_SELECT_LIST_FIELD = "MultiSelectListField";
    public static final String TEXT_AREA_FIELD = "TextAreaField";
    public static final String ADDRESS_FIELD = "AddressUKField";
    public static final String COMPLEX_FIELD = "ComplexField";
    public static final String POST_CODE_FIELD = "PostCode";
    public static final String COUNTY_FIELD = "County";
    public static final String COUNTRY_NESTED_FIELD = "Country";
    public static final String NESTED_COLLECTION_TEXT_FIELD = "NestedCollectionTextField";
    public static final String COMPLEX_NESTED_FIELD = "ComplexNestedField";
    public static final String COMPLEX_TEXT_FIELD = "ComplexTextField";
    public static final String NESTED_NUMBER_FIELD = "NestedNumberField";
    public static final String COMPLEX_FIXED_LIST_FIELD = "ComplexFixedListField";
    public static final String HISTORY_COMPONENT_FIELD = "HistoryComponentField";
    public static final String DOCUMENT_FIELD = "DocumentField";
    public static final String ADDRESS_LINE_1 = "AddressLine1";
    public static final String ADDRESS_LINE_2 = "AddressLine2";
    public static final String ADDRESS_LINE_3 = "AddressLine3";
    public static final String ADDRESS_LINE_3_VALUE = "AddressLine3Value";
    public static final String TOWN_FIELD = "PostTown";
    public static final String STATE = "state";
    public static final String CREATED_DATE = "created_date";
    public static final String TEXT_ALIAS = "TextAlias";
    public static final String FIXED_LIST_ALIAS = "FixedListAlias";

    public static final String AUTOTEST_1 = "AUTOTEST1";
    public static final String AUTOTEST_2 = "AUTOTEST2";
    public static final String DEFAULT_CASE_REFERENCE = "1588866820969121";
    public static final String YES_OR_NO_VALUE = "No";
    public static final String NUMBER_VALUE = "12345";
    public static final String TEXT_VALUE = "AAA TextValue";
    public static final String DATE_VALUE = "2007-12-17";
    public static final String PHONE_VALUE = "01234567890";
    public static final String PHONE_VALUE_WITH_SPACE = "01234 567890";
    public static final String PARTIAL_PHONE_VALUE = "01234";
    public static final String COUNTRY_VALUE = "CountryValue";
    public static final String COLLECTION_VALUE = "CollectionTextValue2";
    public static final String STATE_VALUE = "TODO";
    public static final String DATE_TIME_VALUE = "2003-02-01T12:30:00.000";
    public static final String EMAIL_VALUE = "email1@gmail.com";
    public static final String FIXED_LIST_VALUE = "VALUE2";
    public static final String MONEY_VALUE = "98700";
    public static final String TEXT_AREA_VALUE = "TextArea\nValue";
    public static final String STREET_VALUE = "StreetValue";
    public static final String COMPLEX_TEXT_VALUE = "ComplexTextValue";
    public static final String COUNTY_VALUE = "CountyValue";
    public static final String POST_CODE_VALUE = "PST CDE";
    public static final String ADDRESS_LINE_2_VALUE = "AddressLine2Value";
    public static final String TOWN_VALUE = "TownValue";
    public static final String IN_PROGRESS_STATE = "IN_PROGRESS";
    public static final String CREATED_DATE_VALUE = "2020-05-07T15:53:40.974";
    public static final String LAST_STATE_MODIFIED_DATE_VALUE = "2020-05-07T17:42:00.527";
    public static final String LAST_MODIFIED_DATE_VALUE = "2020-06-09T13:17:06.542";
    public static final String NESTED_NUMBER_FIELD_VALUE = "567";
    public static final String COMPLEX_FIXED_LIST_VALUE = "VALUE3";

    public static final String AUTOTEST1_PUBLIC = "caseworker-autotest1";
    public static final String AUTOTEST2_PUBLIC = "caseworker-autotest2";
    public static final String AUTOTEST1_RESTRICTED = "caseworker-autotest1-restricted";
    public static final String AUTOTEST1_PRIVATE = "caseworker-autotest1-private";
    public static final String AUTOTEST1_SOLICITOR = "caseworker-autotest1-solicitor";
    public static final String CASEWORKER_CAA = "caseworker-caa";

    private static final String CASE_TYPE_ID_PARAM = "ctid";
    private static final String USE_CASE_PARAM = "use_case";

    private ElasticsearchTestHelper() {
    }

    public static String caseData(String fieldPath) {
        return DATA_PREFIX + fieldPath;
    }

    public static String alias(String fieldPath) {
        return ALIAS_PREFIX + fieldPath;
    }

    public static String caseTypesParam(String... caseTypeIds) {
        return String.join(",", caseTypeIds);
    }

    public static MockHttpServletRequestBuilder createPostRequest(String url,
                                                                  ElasticsearchBaseTest.ElasticsearchTestRequest
                                                                          searchRequest,
                                                                  String caseTypeParam,
                                                                  String useCase) throws Exception {
        MockHttpServletRequestBuilder postRequest = post(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(searchRequest.toJsonString())
            .param(CASE_TYPE_ID_PARAM, caseTypeParam);
        LOG.info("Executing request: {} with param: {}={} and body: {}", url, CASE_TYPE_ID_PARAM, caseTypeParam,
            searchRequest.toJsonString());

        if (!Strings.isNullOrEmpty(useCase)) {
            postRequest.param(USE_CASE_PARAM, useCase);
        }

        return postRequest;
    }

    public static <T> T executeRequest(MockHttpServletRequestBuilder postRequest,
                                       int expectedResponse,
                                       ObjectMapper mapper,
                                       MockMvc mockMvc,
                                       Class<T> returnType) throws Exception {
        MvcResult result = mockMvc.perform(postRequest)
            .andExpect(status().is(expectedResponse))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        return mapper.readValue(responseAsString, returnType);
    }

    public static MsearchResponse<ElasticSearchCaseDetailsDTO> createMsearchResponse(
        List<MultiSearchResponseItem<ElasticSearchCaseDetailsDTO>> responseItems) {
        LOG.info("createMsearchResponse with responseItems: {}", responseItems);
        return new MsearchResponse.Builder<ElasticSearchCaseDetailsDTO>()
            .responses(responseItems)
            .took(1)
            .build();
    }

    public static MultiSearchResponseItem<ElasticSearchCaseDetailsDTO> createFailureItem(
        Map<String, Object> errorMap) {
        LOG.info("createFailureItem with errorMap: {}", errorMap);
        String reason = (String) errorMap.getOrDefault("reason", "Unknown failure");
        String type = (String) errorMap.getOrDefault("type", "search_phase_execution_exception");
        int status = (int) errorMap.getOrDefault("status", 500);

        ErrorCause errorCause = new ErrorCause.Builder()
            .type(type)
            .reason(reason)
            .build();

        ErrorResponse errorResponse = new ErrorResponse.Builder()
            .error(errorCause)
            .status(status)
            .build();

        return MultiSearchResponseItem.of(item -> item.failure(errorResponse));
    }

    public static MultiSearchResponseItem<ElasticSearchCaseDetailsDTO> createSuccessItemWithNoHits(String indexName) {
        LOG.info("createSuccessItemWithNoHits with indexName: {}", indexName);
        Hit<ElasticSearchCaseDetailsDTO> hit = new Hit.Builder<ElasticSearchCaseDetailsDTO>()
            .source(new ElasticSearchCaseDetailsDTO())
            .index(indexName)
            .build();

        TotalHits totalHits = createTotalHits(0L);

        return MultiSearchResponseItem.of(item -> item.result(r -> r
            .hits(h -> h
                .hits(List.of(hit))
                .total(totalHits)
            )
            .took(1)
            .timedOut(false)
            .shards(s -> s.total(1).successful(1).skipped(0).failed(0))
        ));
    }

    public static MultiSearchResponseItem<ElasticSearchCaseDetailsDTO> createSuccessItem(
        Hit<ElasticSearchCaseDetailsDTO> hit, TotalHits totalHits) {
        LOG.info("createSuccessItem with hit: {} and totalHits: {}", hit, totalHits);
        return MultiSearchResponseItem.of(item ->
            item.result(r -> r
                .hits(h -> h
                    .hits(List.of(hit))
                    .total(totalHits)
                )
                .took(1)
                .timedOut(false)
                .shards(s -> s.total(1).successful(1).skipped(0).failed(0))
            ));
    }

    public static MultiSearchResponseItem<ElasticSearchCaseDetailsDTO> createSuccessItem(String indexName) {
        LOG.info("createSuccessItem with indexName: {}", indexName);
        Hit<ElasticSearchCaseDetailsDTO> hit = createHit(indexName);

        TotalHits totalHits = createTotalHits(1L);

        return MultiSearchResponseItem.of(item ->
            item.result(r -> r
            .hits(h -> h
                .hits(List.of(hit))
                .total(totalHits)
            )
            .took(1)
            .timedOut(false)
            .shards(s -> s.total(1).successful(1).skipped(0).failed(0))
        ));
    }

    public static Hit<ElasticSearchCaseDetailsDTO> createHit(String indexName) {
        LOG.info("createHit with indexName: {}", indexName);
        return new Hit.Builder<ElasticSearchCaseDetailsDTO>()
            .source(new ElasticSearchCaseDetailsDTO())
            .index(indexName)
            .build();
    }

    public static HitsMetadata<ElasticSearchCaseDetailsDTO> createHitsMetadata(String indexName) {
        LOG.info("createHitsMetadata with indexName: {}", indexName);
        return new HitsMetadata.Builder<ElasticSearchCaseDetailsDTO>()
            .hits(List.of(createHit(indexName)))
            .total(createTotalHits())
            .build();
    }

    public static TotalHits createTotalHits() {
        return createTotalHits(1L);
    }

    public static TotalHits createTotalHits(Long value) {
        LOG.info("createTotalHits with value: {}", value);
        return new TotalHits.Builder()
            .value(value)
            .relation(TotalHitsRelation.Eq)
            .build();
    }
}
