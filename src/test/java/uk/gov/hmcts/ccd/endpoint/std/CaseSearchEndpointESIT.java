package uk.gov.hmcts.ccd.endpoint.std;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.ElasticsearchBaseTest;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.test.ElasticsearchTestHelper;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.ADDRESS_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.ADDRESS_LINE_1;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.ADDRESS_LINE_2;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.ADDRESS_LINE_2_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.ADDRESS_LINE_3;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.ADDRESS_LINE_3_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.AUTOTEST1_PUBLIC;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.AUTOTEST2_PUBLIC;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.CASE_TYPE_A;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.CASE_TYPE_B;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COLLECTION_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COLLECTION_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COMPLEX_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COMPLEX_FIXED_LIST_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COMPLEX_FIXED_LIST_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COMPLEX_NESTED_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COMPLEX_TEXT_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COMPLEX_TEXT_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COUNTRY_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COUNTRY_NESTED_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COUNTRY_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COUNTY_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COUNTY_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.CREATED_DATE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.CREATED_DATE_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.DATE_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.DATE_TIME_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.DATE_TIME_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.DATE_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.DEFAULT_CASE_REFERENCE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.EMAIL_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.EMAIL_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.FIXED_LIST_ALIAS;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.FIXED_LIST_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.FIXED_LIST_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.FIXED_RADIO_LIST_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.LAST_MODIFIED_DATE_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.LAST_STATE_MODIFIED_DATE_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.MULTI_SELECT_LIST_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.NESTED_COLLECTION_TEXT_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.NESTED_NUMBER_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.NESTED_NUMBER_FIELD_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.NUMBER_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.NUMBER_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.PHONE_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.PHONE_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.POST_CODE_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.POST_CODE_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.STATE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.STATE_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.STREET_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.TEXT_ALIAS;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.TEXT_AREA_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.TEXT_AREA_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.TEXT_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.TEXT_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.TOWN_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.TOWN_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.VALUE_SUFFIX;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.YES_OR_NO_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.YES_OR_NO_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.alias;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.caseData;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.caseTypesParam;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.createPostRequest;

class CaseSearchEndpointESIT extends ElasticsearchBaseTest {

    private static final String POST_SEARCH_CASES = "/searchCases";

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, AUTOTEST1_PUBLIC, AUTOTEST2_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Nested
    class CrossCaseTypeSearch {

        @Test // Note that cross case type searches do NOT return case data
        void shouldReturnAllCasesForAllSpecifiedCaseTypes() throws Exception {
            ElasticsearchTestRequest searchRequest = matchAllRequest();

            CaseSearchResult caseSearchResult = executeRequest(searchRequest, caseTypesParam(CASE_TYPE_A, CASE_TYPE_B));

            assertAll(
                () -> assertThat(caseSearchResult.getTotal(), is(3L)),
                () -> assertThat(caseSearchResult.getCaseReferences(CASE_TYPE_A).size(), is(2)),
                () -> assertThat(caseSearchResult.getCaseReferences(CASE_TYPE_B).size(), is(1)),
                () -> assertThat(caseSearchResult.getCases().get(0).getData().size(), is(0)),
                () -> assertThat(caseSearchResult.getCases().get(1).getData().size(), is(0)),
                () -> assertThat(caseSearchResult.getCases().get(2).getData().size(), is(0))
            );
        }

        @Test
        void shouldReturnRequestedAliasSource() throws Exception {
            ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                .query(matchAllQuery())
                .source(alias(TEXT_ALIAS))
                .sort(CREATED_DATE)
                .build();

            CaseSearchResult caseSearchResult = executeRequest(searchRequest, caseTypesParam(CASE_TYPE_A, CASE_TYPE_B));

            assertAll(
                () -> assertThat(caseSearchResult.getTotal(), is(3L)),
                () -> assertThat(caseSearchResult.getCases().get(0).getData().get(TEXT_ALIAS).asText(), is(TEXT_VALUE)),
                () -> assertThat(caseSearchResult.getCases().get(1).getData().get(TEXT_ALIAS).asText(), is("CCC TextValue")),
                () -> assertThat(caseSearchResult.getCases().get(2).getData().get(TEXT_ALIAS).asText(), is("BBB TextValue")),
                () -> assertThat(caseSearchResult.getCases().get(0).getData().size(), is(1)),
                () -> assertThat(caseSearchResult.getCases().get(1).getData().size(), is(1)),
                () -> assertThat(caseSearchResult.getCases().get(2).getData().size(), is(1))
            );
        }

        @Test // Note that the size and sort is applied to each separate case type then results combined
        void shouldReturnPaginatedResults() throws Exception {
            ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                .query(matchAllQuery())
                .sort(CREATED_DATE)
                .size(1)
                .build();

            CaseSearchResult caseSearchResult = executeRequest(searchRequest, caseTypesParam(CASE_TYPE_A, CASE_TYPE_B));

            assertAll(
                () -> assertThat(caseSearchResult.getTotal(), is(3L)),
                () -> assertThat(caseSearchResult.getCases().size(), is(2)) // = Size * Number of case types
            );
        }

        @Test
        void shouldQueryOnAliasField() throws Exception {
            ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                .query(matchQuery(alias(FIXED_LIST_ALIAS), FIXED_LIST_VALUE))
                .build();

            CaseSearchResult caseSearchResult = executeRequest(searchRequest, caseTypesParam(CASE_TYPE_A, CASE_TYPE_B));

            assertAll(
                () -> assertThat(caseSearchResult.getTotal(), is(2L)),
                () -> assertThat(caseSearchResult.getCases().get(0).getReference(), is(1588866820969121L)),
                () -> assertThat(caseSearchResult.getCases().get(1).getReference(), is(1588870615652827L))
            );
        }
    }

    @Nested
    class SingleCaseTypeSearch {

        @Test
        void shouldReturnAllCaseDetails() throws Exception {
            ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                .query(boolQuery()
                    .must(matchQuery(caseData(NUMBER_FIELD), NUMBER_VALUE)) // ES Double
                    .must(matchQuery(caseData(YES_OR_NO_FIELD), YES_OR_NO_VALUE)) // ES Keyword
                    .must(matchQuery(caseData(TEXT_FIELD), TEXT_VALUE)) // ES Text
                    .must(matchQuery(caseData(DATE_FIELD), DATE_VALUE)) // ES Date
                    .must(matchQuery(caseData(PHONE_FIELD), PHONE_VALUE)) // ES Phone
                    .must(matchQuery(caseData(COUNTRY_FIELD), COUNTRY_VALUE)) // Complex
                    .must(matchQuery(caseData(COLLECTION_FIELD) + VALUE_SUFFIX, COLLECTION_VALUE)) // Collection
                    .must(matchQuery(STATE, STATE_VALUE))) // Metadata
                .build();

            CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_A);

            CaseDetails caseDetails = caseSearchResult.getCases().get(0);
            assertAll(
                () -> assertThat(caseSearchResult.getTotal(), is(1L)),
                () -> assertExampleCaseMetadata(caseDetails),
                () -> assertExampleCaseData(caseDetails),
                () -> assertThat(caseDetails.getSupplementaryData().size(), is(3)),
                () -> assertThat(caseDetails.getSupplementaryData().get("SDField1").asText(), is("SDField1Value")),
                () -> assertThat(caseDetails.getSupplementaryData().get("SDField2").asText(), is("SDField2Value")),
                () -> assertThat(caseDetails.getSupplementaryData().get("SDField3").asText(), is("SDField3Value"))
            );
        }

        @Test
        void shouldReturnRequestedSupplementaryData() throws Exception {
            ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
                .supplementaryData(Arrays.asList("SDField2", "SDField3"))
                .build();

            CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_A);

            CaseDetails caseDetails = caseSearchResult.getCases().get(0);
            assertAll(
                () -> assertThat(caseSearchResult.getTotal(), is(1L)),
                () -> assertExampleCaseMetadata(caseDetails),
                () -> assertExampleCaseData(caseDetails),
                () -> assertThat(caseDetails.getSupplementaryData().size(), is(2)),
                () -> assertThat(caseDetails.getSupplementaryData().get("SDField2").asText(), is("SDField2Value")),
                () -> assertThat(caseDetails.getSupplementaryData().get("SDField3").asText(), is("SDField3Value"))
            );
        }

        @Test
        void shouldReturnAllSupplementaryDataWhenWildcardIsUsed() throws Exception {
            ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
                .supplementaryData(Collections.singletonList("*"))
                .build();

            CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_A);

            CaseDetails caseDetails = caseSearchResult.getCases().get(0);
            assertAll(
                () -> assertThat(caseSearchResult.getTotal(), is(1L)),
                () -> assertExampleCaseMetadata(caseDetails),
                () -> assertExampleCaseData(caseDetails),
                () -> assertThat(caseDetails.getSupplementaryData().size(), is(3)),
                () -> assertThat(caseDetails.getSupplementaryData().get("SDField1").asText(), is("SDField1Value")),
                () -> assertThat(caseDetails.getSupplementaryData().get("SDField2").asText(), is("SDField2Value")),
                () -> assertThat(caseDetails.getSupplementaryData().get("SDField3").asText(), is("SDField3Value"))
            );
        }

        @Test
        void shouldErrorWhenInvalidCaseTypeIsProvided() throws Exception {
            ElasticsearchTestRequest searchRequest = matchAllRequest();

            JsonNode exceptionNode = executeErrorRequest(searchRequest, "INVALID", 404);

            assertAll(
                () -> assertThat(exceptionNode.get("message").asText(),
                    startsWith("Resource not found when getting case type definition for INVALID"))
            );
        }

        public void assertExampleCaseMetadata(CaseDetails caseDetails) {
            assertAll(
                () -> assertThat(caseDetails.getJurisdiction(), is("AUTOTEST1")),
                () -> assertThat(caseDetails.getCaseTypeId(), is(CASE_TYPE_A)),
                () -> assertThat(caseDetails.getCreatedDate().toString(), is(CREATED_DATE_VALUE)),
                () -> assertThat(caseDetails.getLastModified().toString(), is(LAST_MODIFIED_DATE_VALUE)),
                () -> assertThat(caseDetails.getLastStateModifiedDate().toString(), is(LAST_STATE_MODIFIED_DATE_VALUE)),
                () -> assertThat(caseDetails.getReference(), is(1588866820969121L)),
                () -> assertThat(caseDetails.getState(), is(STATE_VALUE)),
                () -> assertThat(caseDetails.getSecurityClassification(), is(SecurityClassification.PUBLIC))
            );
        }

        public void assertExampleCaseData(CaseDetails caseDetails) {
            Map<String, JsonNode> data = caseDetails.getData();
            assertAll(
                () -> assertThat(data.get(ADDRESS_FIELD).get(ADDRESS_LINE_1).asText(), is(STREET_VALUE)),
                () -> assertThat(data.get(ADDRESS_FIELD).get(ADDRESS_LINE_2).asText(), is(ADDRESS_LINE_2_VALUE)),
                () -> assertThat(data.get(ADDRESS_FIELD).get(ADDRESS_LINE_3).asText(), is(ADDRESS_LINE_3_VALUE)),
                () -> assertThat(data.get(ADDRESS_FIELD).get(COUNTRY_NESTED_FIELD).asText(), is(COUNTRY_VALUE)),
                () -> assertThat(data.get(ADDRESS_FIELD).get(COUNTY_FIELD).asText(), is(COUNTY_VALUE)),
                () -> assertThat(data.get(ADDRESS_FIELD).get(POST_CODE_FIELD).asText(), is(POST_CODE_VALUE)),
                () -> assertThat(data.get(ADDRESS_FIELD).get(TOWN_FIELD).asText(), is(TOWN_VALUE)),
                () -> assertThat(data.get(COLLECTION_FIELD).toString(),
                    is("[{\"id\":\"2c6da07c-1dfb-4765-88f6-96cd5d5f33b1\",\"value\":\"CollectionTextValue2\"},"
                       + "{\"id\":\"f7d67f03-172d-4adb-85e5-ca958ad442ce\",\"value\":\"CollectionTextValue1\"}]")),
                () -> assertThat(data.get(COMPLEX_FIELD).get(COMPLEX_TEXT_FIELD).asText(), is(COMPLEX_TEXT_VALUE)),
                () -> assertThat(data.get(COMPLEX_FIELD).get(COMPLEX_FIXED_LIST_FIELD).asText(), is(COMPLEX_FIXED_LIST_VALUE)),
                () -> assertThat(data.get(COMPLEX_FIELD).get(COMPLEX_NESTED_FIELD).get(NESTED_NUMBER_FIELD).asText(), is(NESTED_NUMBER_FIELD_VALUE)),
                () -> assertThat(data.get(COMPLEX_FIELD).get(COMPLEX_NESTED_FIELD).get(NESTED_COLLECTION_TEXT_FIELD).toString(),
                    is("[{\"id\":\"8e19ccb3-2d8c-42f0-abe1-fa585cc2d8c8\",\"value\":\"NestedCollectionTextValue1\"},"
                        + "{\"id\":\"95f337e8-5f17-4b25-a795-b7f84f4b2855\",\"value\":\"NestedCollectionTextValue2\"}]")),
                () -> assertThat(data.get(DATE_FIELD).asText(), is(DATE_VALUE)),
                () -> assertThat(data.get(DATE_TIME_FIELD).asText(), is(DATE_TIME_VALUE)),
                () -> assertThat(data.get(EMAIL_FIELD).asText(), is(EMAIL_VALUE)),
                () -> assertThat(data.get(FIXED_LIST_FIELD).asText(), is(FIXED_LIST_VALUE)),
                () -> assertThat(data.get(FIXED_RADIO_LIST_FIELD).isNull(), is(true)),
                () -> assertThat(data.get(MULTI_SELECT_LIST_FIELD).toString(), is("[\"OPTION2\",\"OPTION4\"]")),
                () -> assertThat(data.get(NUMBER_FIELD).asText(), is(NUMBER_VALUE)),
                () -> assertThat(data.get(PHONE_FIELD).asText(), is(PHONE_VALUE)),
                () -> assertThat(data.get(TEXT_AREA_FIELD).asText(), is(TEXT_AREA_VALUE)),
                () -> assertThat(data.get(TEXT_FIELD).asText(), is(TEXT_VALUE)),
                () -> assertThat(data.get(YES_OR_NO_FIELD).asText(), is(YES_OR_NO_VALUE))
            );
        }
    }

    private CaseSearchResult executeRequest(ElasticsearchTestRequest searchRequest, String caseTypeParam) throws Exception {
        MockHttpServletRequestBuilder postRequest = createPostRequest(POST_SEARCH_CASES, searchRequest, caseTypeParam, null);

        return ElasticsearchTestHelper.executeRequest(postRequest, 200, mapper, mockMvc, CaseSearchResult.class);
    }

    private JsonNode executeErrorRequest(ElasticsearchTestRequest searchRequest,
                                         String caseTypeParam,
                                         int expectedErrorCode) throws Exception {
        MockHttpServletRequestBuilder postRequest = createPostRequest(POST_SEARCH_CASES, searchRequest, caseTypeParam, null);

        return ElasticsearchTestHelper.executeRequest(postRequest, expectedErrorCode, mapper, mockMvc, JsonNode.class);
    }
}
