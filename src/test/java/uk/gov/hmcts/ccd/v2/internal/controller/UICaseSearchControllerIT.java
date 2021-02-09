package uk.gov.hmcts.ccd.v2.internal.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.ElasticsearchBaseTest;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewHeaderGroup;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.ccd.test.ElasticsearchTestHelper;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseSearchResultViewResource;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.domain.types.CollectionValidator.VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.ADDRESS_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.AUTOTEST1_PUBLIC;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.AUTOTEST2_PUBLIC;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.AUTOTEST_1;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.CASEWORKER_CAA;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.CASE_TYPE_A;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COLLECTION_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COLLECTION_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COMPLEX_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COMPLEX_FIXED_LIST_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COMPLEX_NESTED_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COMPLEX_TEXT_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COMPLEX_TEXT_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COUNTRY_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.CREATED_DATE_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.DATE_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.DATE_TIME_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.DATE_TIME_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.DATE_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.DEFAULT_CASE_REFERENCE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.DOCUMENT_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.EMAIL_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.EMAIL_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.FIXED_LIST_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.FIXED_LIST_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.FIXED_RADIO_LIST_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.HISTORY_COMPONENT_FIELD;
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
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.PHONE_VALUE_WITH_SPACE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.PARTIAL_PHONE_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.STATE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.STATE_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.TEXT_AREA_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.TEXT_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.TEXT_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.VALUE_SUFFIX;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.YES_OR_NO_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.YES_OR_NO_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.caseData;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.createPostRequest;

class UICaseSearchControllerIT extends ElasticsearchBaseTest {

    private static final String POST_SEARCH_CASES = "/internal/searchCases";
    private static final String CASE_FIELD_ID = "caseFieldId";
    private static final String ERROR_MESSAGE = "message";

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Inject
    private ApplicationParams applicationParams;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, AUTOTEST1_PUBLIC, AUTOTEST2_PUBLIC);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    void shouldReturnAllCaseDetailsForDefaultUseCase() throws Exception {
        ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
            .query(boolQuery()
                .must(matchQuery(caseData(NUMBER_FIELD), NUMBER_VALUE)) // ES Double
                .must(matchQuery(caseData(YES_OR_NO_FIELD), YES_OR_NO_VALUE)) // ES Keyword
                .must(matchQuery(caseData(TEXT_FIELD), TEXT_VALUE)) // ES Text
                .must(matchQuery(caseData(DATE_FIELD), DATE_VALUE)) // ES Date
                .must(matchQuery(caseData(PHONE_FIELD), PHONE_VALUE)) // ES Phone
                .must(matchQuery(caseData(COUNTRY_FIELD), ElasticsearchTestHelper.COUNTRY_VALUE)) // Complex
                .must(matchQuery(caseData(COLLECTION_FIELD) + VALUE_SUFFIX, COLLECTION_VALUE)) // Collection
                .must(matchQuery(STATE, STATE_VALUE))) // Metadata
            .build();

        CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
            null);

        SearchResultViewItem caseDetails = caseSearchResultViewResource.getCases().get(0);
        assertAll(
            () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
            () -> assertThat(caseSearchResultViewResource.getCases().size(), is(1)),
            () -> assertExampleCaseData(caseDetails.getFields(), false),
            () -> assertExampleCaseMetadata(caseDetails.getFields(), false)
        );
    }

    @Test
    void shouldReturnAllCaseDetailsForPhoneValueWithSpace() throws Exception {
        ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
            .query(boolQuery()
                .must(matchQuery(caseData(NUMBER_FIELD), NUMBER_VALUE)) // ES Double
                .must(matchQuery(caseData(YES_OR_NO_FIELD), YES_OR_NO_VALUE)) // ES Keyword
                .must(matchQuery(caseData(TEXT_FIELD), TEXT_VALUE)) // ES Text
                .must(matchQuery(caseData(DATE_FIELD), DATE_VALUE)) // ES Date
                .must(matchQuery(caseData(PHONE_FIELD), PHONE_VALUE_WITH_SPACE)) // ES Phone
                .must(matchQuery(caseData(COUNTRY_FIELD), ElasticsearchTestHelper.COUNTRY_VALUE)) // Complex
                .must(matchQuery(caseData(COLLECTION_FIELD) + VALUE_SUFFIX, COLLECTION_VALUE)) // Collection
                .must(matchQuery(STATE, STATE_VALUE))) // Metadata
            .build();

        CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A, null);

        SearchResultViewItem caseDetails = caseSearchResultViewResource.getCases().get(0);
        assertAll(
            () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
            () -> assertThat(caseSearchResultViewResource.getCases().size(), is(1)),
            () -> assertExampleCaseData(caseDetails.getFields(), false),
            () -> assertExampleCaseMetadata(caseDetails.getFields(), false)
        );
    }

    @Test
    void shouldNotReturnCaseDetailsForPartialPhoneValue() throws Exception {
        ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
            .query(boolQuery()
                .must(matchQuery(caseData(NUMBER_FIELD), NUMBER_VALUE)) // ES Double
                .must(matchQuery(caseData(YES_OR_NO_FIELD), YES_OR_NO_VALUE)) // ES Keyword
                .must(matchQuery(caseData(TEXT_FIELD), TEXT_VALUE)) // ES Text
                .must(matchQuery(caseData(DATE_FIELD), DATE_VALUE)) // ES Date
                .must(matchQuery(caseData(PHONE_FIELD), PARTIAL_PHONE_VALUE)) // ES Phone
                .must(matchQuery(caseData(COUNTRY_FIELD), ElasticsearchTestHelper.COUNTRY_VALUE)) // Complex
                .must(matchQuery(caseData(COLLECTION_FIELD) + VALUE_SUFFIX, COLLECTION_VALUE)) // Collection
                .must(matchQuery(STATE, STATE_VALUE))) // Metadata
            .build();

        CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A, null);

        assertThat(caseSearchResultViewResource.getCases().size(),is(0));
    }

    @Test
    void shouldReturnAllCaseDetailsForDefaultUseCaseWithRoleCaseworkerCaa() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, CASEWORKER_CAA);
        ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
            .query(boolQuery()
                .must(matchQuery(caseData(NUMBER_FIELD), NUMBER_VALUE)) // ES Double
                .must(matchQuery(caseData(YES_OR_NO_FIELD), YES_OR_NO_VALUE)) // ES Keyword
                .must(matchQuery(caseData(TEXT_FIELD), TEXT_VALUE)) // ES Text
                .must(matchQuery(caseData(DATE_FIELD), DATE_VALUE)) // ES Date
                .must(matchQuery(caseData(PHONE_FIELD), PHONE_VALUE)) // ES Phone
                .must(matchQuery(caseData(COUNTRY_FIELD), ElasticsearchTestHelper.COUNTRY_VALUE)) // Complex
                .must(matchQuery(caseData(COLLECTION_FIELD) + VALUE_SUFFIX, COLLECTION_VALUE)) // Collection
                .must(matchQuery(STATE, STATE_VALUE))) // Metadata
            .build();

        CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
            null);

        SearchResultViewItem caseDetails = caseSearchResultViewResource.getCases().get(0);
        assertAll(
            () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
            () -> assertThat(caseSearchResultViewResource.getCases().size(), is(1)),
            () -> assertExampleCaseData(caseDetails.getFields(), false),
            () -> assertExampleCaseMetadata(caseDetails.getFields(), false)
        );
    }

    @Test
    void shouldReturnAllHeaderInfoForDefaultUseCase() throws Exception {
        ElasticsearchTestRequest searchRequest = caseReferenceRequest(DEFAULT_CASE_REFERENCE);

        CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
            null);

        assertAll(
            () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
            () -> assertDefaultUseCaseHeaders(caseSearchResultViewResource.getHeaders())
        );
    }

    @Test
    void shouldReturnAllHeaderInfoForDefaultUseCaseWhenUserRoleColumnIsPopulated() throws Exception {
        ElasticsearchTestRequest searchRequest = caseReferenceRequest(DEFAULT_CASE_REFERENCE);

        CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
            "TEST");
        SearchResultViewItem caseDetails = caseSearchResultViewResource.getCases().get(0);
        assertAll(
            () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
            () -> assertUseCaseHeadersUserRole(caseSearchResultViewResource.getHeaders()),
            () -> assertExampleCaseDataForUserRole(caseDetails.getFields(), false),
            () -> assertExampleCaseMetadata(caseDetails.getFields(), false)
        );
    }

    @Test
    void shouldReturnAllHeaderInfoForDefaultUseCaseWhenUserHasSomeAuthorisationOnCaseFields() throws Exception {
        ElasticsearchTestRequest searchRequest = caseReferenceRequest(DEFAULT_CASE_REFERENCE);

        CaseSearchResultViewResource caseSearchResultViewResource =
            executeRequest(searchRequest, CASE_TYPE_A, "RDM-8782");
        SearchResultViewItem caseDetails = caseSearchResultViewResource.getCases().get(0);

        List<String> expectedFields = Arrays.asList(EMAIL_FIELD);
        assertAll(
            () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().size(), is(1)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getMetadata().getJurisdiction(),
                is(AUTOTEST_1)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getMetadata().getCaseTypeId(),
                is(CASE_TYPE_A)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getCases().size(), is(1)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getCases().get(0),
                is(DEFAULT_CASE_REFERENCE)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().size(), is(1)),
            () -> expectedFields.forEach(f -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields(),
                hasItem(hasProperty(CASE_FIELD_ID, is(f))))),

            () -> assertThat(caseDetails.getFields().get(EMAIL_FIELD), is(EMAIL_VALUE)),
            () -> assertExampleCaseMetadata(caseDetails.getFields(), false)
        );
    }


    @Test
    void shouldReturnAllHeaderInfoForDefaultUseCaseWhenUseHaveNoAuthorisationOnCaseField() throws Exception {
        ElasticsearchTestRequest searchRequest = caseReferenceRequest(DEFAULT_CASE_REFERENCE);

        CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
            "RDM-8782NOACCESS");
        SearchResultViewItem caseDetails = caseSearchResultViewResource.getCases().get(0);

        assertAll(
            () -> assertThat(caseSearchResultViewResource.getHeaders().size(), is(1)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getMetadata().getJurisdiction(),
                is(AUTOTEST_1)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getMetadata().getCaseTypeId(),
                is(CASE_TYPE_A)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getCases().size(), is(1)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getCases().get(0),
                is(DEFAULT_CASE_REFERENCE)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().size(), is(0)),
            () -> assertThat(caseDetails.getFields().size(), is(8)),
            () -> assertExampleCaseMetadata(caseDetails.getFields(), false)
        );
    }

    @Test
    void shouldReturnAllHeaderInfoForSpecifiedUseCase() throws Exception {
        ElasticsearchTestRequest searchRequest = caseReferenceRequest(DEFAULT_CASE_REFERENCE);

        CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
            "orgcases");

        assertAll(
            () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
            () -> assertOrgCasesUseCaseHeaders(caseSearchResultViewResource.getHeaders())
        );
    }

    @Test
    void shouldReturnAllFormattedCaseDetails() throws Exception {
        ElasticsearchTestRequest searchRequest = caseReferenceRequest(DEFAULT_CASE_REFERENCE);

        CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
            "ORGCASES");

        SearchResultViewItem caseDetails = caseSearchResultViewResource.getCases().get(0);
        assertAll(
            () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
            () -> assertThat(caseSearchResultViewResource.getCases().size(), is(1)),
            () -> assertExampleCaseData(caseDetails.getFields(), false),
            () -> assertExampleCaseMetadata(caseDetails.getFields(), false),
            () -> assertExampleCaseData(caseDetails.getFieldsFormatted(), true),
            () -> assertExampleCaseMetadata(caseDetails.getFieldsFormatted(), true)
        );
    }

    @Test
    void shouldOnlyReturnSpecifiedFieldsInResponse() throws Exception {
        String nestedFieldId = COMPLEX_FIELD + "." + COMPLEX_NESTED_FIELD + "." + NESTED_NUMBER_FIELD;
        ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
            .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
            .source(caseData(TEXT_FIELD))
            .source(caseData(nestedFieldId))
            .source(MetaData.CaseField.CASE_REFERENCE.getDbColumnName())
            .source("INVALID")
            .build();

        CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
            null);

        SearchResultViewItem caseDetails = caseSearchResultViewResource.getCases().get(0);
        assertAll(
            () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().size(), is(3)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().get(0).getCaseFieldId(),
                is(TEXT_FIELD)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().get(1).getCaseFieldId(),
                is(nestedFieldId)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().get(2).getCaseFieldId(),
                is(MetaData.CaseField.CASE_REFERENCE.getReference())),
            () -> assertThat(caseDetails.getFields().size(), is(11)),
            () -> assertExampleCaseMetadata(caseDetails.getFields(), false),
            () -> assertThat(caseDetails.getFields().get(TEXT_FIELD), is(TEXT_VALUE)),
            () -> assertThat(caseDetails.getFields().get(nestedFieldId), is(NESTED_NUMBER_FIELD_VALUE)),
            () -> assertThat(caseDetails.getFields().containsKey(COMPLEX_FIELD), is(true)),
            () -> assertThat(caseDetails.getFieldsFormatted().size(), is(11)),
            () -> assertExampleCaseMetadata(caseDetails.getFieldsFormatted(), false),
            () -> assertThat(caseDetails.getFieldsFormatted().get(TEXT_FIELD), is(TEXT_VALUE)),
            () -> assertThat(caseDetails.getFieldsFormatted().get(nestedFieldId), is(NESTED_NUMBER_FIELD_VALUE)),
            () -> assertThat(caseDetails.getFieldsFormatted().containsKey(COMPLEX_FIELD), is(true))
        );
    }

    @Test
    void shouldTreatUseCaseRequestWithSourceAsStandardRequest() throws Exception {
        String nestedFieldId = COMPLEX_FIELD + "." + COMPLEX_NESTED_FIELD + "." + NESTED_NUMBER_FIELD;
        ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
            .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
            .source(caseData(TEXT_FIELD))
            .source(caseData(nestedFieldId))
            .source(MetaData.CaseField.CASE_REFERENCE.getDbColumnName())
            .source("INVALID")
            .build();

        CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
            "SEARCH");

        SearchResultViewItem caseDetails = caseSearchResultViewResource.getCases().get(0);
        assertAll(
            () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().size(), is(3)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().get(0).getCaseFieldId(),
                is(TEXT_FIELD)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().get(1).getCaseFieldId(),
                is(nestedFieldId)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().get(2).getCaseFieldId(),
                is(MetaData.CaseField.CASE_REFERENCE.getReference())),
            () -> assertThat(caseDetails.getFields().size(), is(11)),
            () -> assertExampleCaseMetadata(caseDetails.getFields(), false),
            () -> assertThat(caseDetails.getFields().get(TEXT_FIELD), is(TEXT_VALUE)),
            () -> assertThat(caseDetails.getFields().get(nestedFieldId), is(NESTED_NUMBER_FIELD_VALUE)),
            () -> assertThat(caseDetails.getFields().containsKey(COMPLEX_FIELD), is(true)),
            () -> assertThat(caseDetails.getFieldsFormatted().size(), is(11)),
            () -> assertExampleCaseMetadata(caseDetails.getFieldsFormatted(), false),
            () -> assertThat(caseDetails.getFieldsFormatted().get(TEXT_FIELD), is(TEXT_VALUE)),
            () -> assertThat(caseDetails.getFieldsFormatted().get(nestedFieldId), is(NESTED_NUMBER_FIELD_VALUE)),
            () -> assertThat(caseDetails.getFieldsFormatted().containsKey(COMPLEX_FIELD), is(true))
        );
    }

    @Test
    void shouldReturnRequestedSupplementaryDataForUseCaseRequest() throws Exception {
        ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
            .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
            .supplementaryData(Arrays.asList("SDField2", "SDField3"))
            .build();

        CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
                "orgcases");

        assertAll(
            () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().size(), is(10)),
            () -> assertThat(caseSearchResultViewResource.getCases().get(0).getFields().size(), is(16)),
            () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().size(), is(2)),
            () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().get("SDField2")
                    .asText(), is("SDField2Value")),
            () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().get("SDField3")
                    .asText(), is("SDField3Value"))
        );
    }

    @Test
    void shouldReturnAllSupplementaryDataWhenWildcardIsUsed() throws Exception {
        ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
            .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
            .supplementaryData(Collections.singletonList("*"))
            .build();

        CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
                "orgcases");

        assertAll(
            () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
            () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().size(), is(3)),
            () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().get("SDField1")
                    .asText(), is("SDField1Value")),
            () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().get("SDField2")
                    .asText(), is("SDField2Value")),
            () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().get("SDField3")
                    .asText(), is("SDField3Value"))
        );
    }

    @Test
    void shouldReturnNoSupplementaryDataWhenNotRequestedForUseCaseRequest() throws Exception {
        ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
            .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
            .build();

        CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
                "orgcases");

        assertAll(
            () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
            () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData(), is(nullValue()))
        );
    }

    @Test
    void shouldReturnAllSupplementaryDataByDefaultForStandardRequest() throws Exception {
        ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
            .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
            .build();

        CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
                null);

        assertAll(
            () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
            () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().size(), is(3)),
            () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().get("SDField1")
                    .asText(), is("SDField1Value")),
            () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().get("SDField2")
                    .asText(), is("SDField2Value")),
            () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().get("SDField3")
                    .asText(), is("SDField3Value"))
        );
    }

    @Test
    void shouldReturnErrorWithUnsupportedUseCase() throws Exception {
        ElasticsearchTestRequest searchRequest = matchAllRequest();

        JsonNode exceptionNode = executeErrorRequest(searchRequest, CASE_TYPE_A, "INVALID",
            400);

        assertAll(
            () -> assertThat(exceptionNode.get(ERROR_MESSAGE).asText(),
                is("The provided use case 'INVALID' is unsupported for case type 'AAT'."))
        );
    }

    @Test
    void shouldReturnErrorWithMissingQueryField() throws Exception {
        ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder().build();

        JsonNode exceptionNode = executeErrorRequest(searchRequest, CASE_TYPE_A, null, 400);

        assertAll(
            () -> assertThat(exceptionNode.get(ERROR_MESSAGE).asText(),
                is("missing required field 'query'"))
        );
    }

    @Test
    void shouldReturnErrorWhenElasticsearchErrors() throws Exception {
        ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
            .query(matchAllQuery())
            .sort("invalid.keyword")
            .build();

        JsonNode exceptionNode = executeErrorRequest(searchRequest, CASE_TYPE_A, null, 400);

        assertAll(
            () -> assertThat(exceptionNode.get(ERROR_MESSAGE).asText(),
                containsString("No mapping found for [invalid.keyword] in order to sort on"))
        );
    }

    private void assertDefaultUseCaseHeaders(List<SearchResultViewHeaderGroup> headers) {
        List<String> expectedFields = Arrays.asList(HISTORY_COMPONENT_FIELD, FIXED_RADIO_LIST_FIELD, DOCUMENT_FIELD,
            ADDRESS_FIELD, COMPLEX_FIELD,
            COLLECTION_FIELD, MULTI_SELECT_LIST_FIELD, FIXED_LIST_FIELD, TEXT_AREA_FIELD, DATE_TIME_FIELD, DATE_FIELD,
            EMAIL_FIELD, PHONE_FIELD, YES_OR_NO_FIELD, NUMBER_FIELD, TEXT_FIELD,
            MetaData.CaseField.LAST_STATE_MODIFIED_DATE.getReference(), MetaData.CaseField.LAST_MODIFIED_DATE
                .getReference(),
            MetaData.CaseField.CREATED_DATE.getReference(), MetaData.CaseField.JURISDICTION.getReference(),
            MetaData.CaseField.CASE_TYPE.getReference(), MetaData.CaseField.SECURITY_CLASSIFICATION.getReference(),
            MetaData.CaseField.CASE_REFERENCE.getReference(), MetaData.CaseField.STATE.getReference());

        assertAll(
            () -> assertThat(headers.size(), is(1)),
            () -> assertThat(headers.get(0).getMetadata().getJurisdiction(), is(AUTOTEST_1)),
            () -> assertThat(headers.get(0).getMetadata().getCaseTypeId(), is(CASE_TYPE_A)),
            () -> assertThat(headers.get(0).getCases().size(), is(1)),
            () -> assertThat(headers.get(0).getCases().get(0), is(DEFAULT_CASE_REFERENCE)),
            () -> assertThat(headers.get(0).getFields().size(), is(24)),
            () -> expectedFields.forEach(f -> assertThat(headers.get(0).getFields(), hasItem(hasProperty(CASE_FIELD_ID,
                is(f)))))
        );
    }

    private void assertUseCaseHeadersUserRole(List<SearchResultViewHeaderGroup> headers) {
        List<String> expectedFields = Arrays.asList(COLLECTION_FIELD, EMAIL_FIELD, FIXED_LIST_FIELD,
            TEXT_FIELD, MetaData.CaseField.STATE.getReference());

        assertAll(
            () -> assertThat(headers.size(), is(1)),
            () -> assertThat(headers.get(0).getMetadata().getJurisdiction(), is(AUTOTEST_1)),
            () -> assertThat(headers.get(0).getMetadata().getCaseTypeId(), is(CASE_TYPE_A)),
            () -> assertThat(headers.get(0).getCases().size(), is(1)),
            () -> assertThat(headers.get(0).getCases().get(0), is(DEFAULT_CASE_REFERENCE)),
            () -> assertThat(headers.get(0).getFields().size(), is(5)),
            () -> expectedFields.forEach(f -> assertThat(headers.get(0).getFields(), hasItem(hasProperty(CASE_FIELD_ID,
                is(f)))))
        );
    }

    private void assertOrgCasesUseCaseHeaders(List<SearchResultViewHeaderGroup> headers) {
        List<String> expectedFields = Arrays.asList(TEXT_FIELD, EMAIL_FIELD, FIXED_LIST_FIELD, COLLECTION_FIELD,
            COMPLEX_FIELD, DATE_FIELD,
            DATE_TIME_FIELD, COMPLEX_FIELD + ".ComplexTextField", MetaData.CaseField.CREATED_DATE.getReference(),
            MetaData.CaseField.STATE.getReference());

        assertAll(
            () -> assertThat(headers.size(), is(1)),
            () -> assertThat(headers.get(0).getMetadata().getJurisdiction(), is(AUTOTEST_1)),
            () -> assertThat(headers.get(0).getMetadata().getCaseTypeId(), is(CASE_TYPE_A)),
            () -> assertThat(headers.get(0).getCases().size(), is(1)),
            () -> assertThat(headers.get(0).getCases().get(0), is(DEFAULT_CASE_REFERENCE)),
            () -> assertThat(headers.get(0).getFields().size(), is(10)),
            () -> expectedFields.forEach(f -> assertThat(headers.get(0).getFields(), hasItem(hasProperty(CASE_FIELD_ID,
                is(f)))))
        );
    }

    private void assertExampleCaseData(Map<String, Object> data, boolean formatted) {
        assertAll(
            () -> assertThat(asCollection(data.get(COLLECTION_FIELD)).get(0).get(VALUE), is(COLLECTION_VALUE)),
            () -> assertThat(asCollection(data.get(COLLECTION_FIELD)).get(1).get(VALUE),
                    is("CollectionTextValue1")),
            () -> assertThat(asMap(data.get(COMPLEX_FIELD)).get(COMPLEX_FIXED_LIST_FIELD), is("VALUE3")),
            () -> assertThat(asMap(asMap(data.get(COMPLEX_FIELD)).get(COMPLEX_NESTED_FIELD)).get(NESTED_NUMBER_FIELD),
                is(NESTED_NUMBER_FIELD_VALUE)),
            () -> assertThat(asMap(data.get(COMPLEX_FIELD)).get(COMPLEX_TEXT_FIELD), is(COMPLEX_TEXT_VALUE)),
            () -> assertThat(asCollection(asMap(asMap(data.get(COMPLEX_FIELD)).get(COMPLEX_NESTED_FIELD))
                .get(NESTED_COLLECTION_TEXT_FIELD)).get(0).get(VALUE), is("NestedCollectionTextValue1")),
            () -> assertThat(asCollection(asMap(asMap(data.get(COMPLEX_FIELD)).get(COMPLEX_NESTED_FIELD))
                .get(NESTED_COLLECTION_TEXT_FIELD)).get(1).get(VALUE), is("NestedCollectionTextValue2")),
            () -> assertThat(data.get(DATE_FIELD), is(formatted ? "12/2007" : DATE_VALUE)),
            () -> assertThat(data.get(DATE_TIME_FIELD), is(formatted ? "Saturday, 1 February 2003" : DATE_TIME_VALUE)),
            () -> assertThat(data.get(EMAIL_FIELD), is(EMAIL_VALUE)),
            () -> assertThat(data.get(FIXED_LIST_FIELD), is(FIXED_LIST_VALUE)),
            () -> assertThat(data.get(FIXED_RADIO_LIST_FIELD), is(nullValue())),
            () -> assertThat(data.get(TEXT_FIELD), is(TEXT_VALUE))
        );
    }

    private void assertExampleCaseDataForUserRole(Map<String, Object> data, boolean formatted) {
        assertAll(
            () -> assertThat(data.get(EMAIL_FIELD), is(EMAIL_VALUE)),
            () -> assertThat(data.get(FIXED_LIST_FIELD), is(FIXED_LIST_VALUE)),
            () -> assertThat(data.get(TEXT_FIELD), is(TEXT_VALUE)),
            () -> assertThat(asCollection(data.get(COLLECTION_FIELD)).get(0).get(VALUE), is(COLLECTION_VALUE)),
            () -> assertThat(asCollection(data.get(COLLECTION_FIELD)).get(1).get(VALUE),
                    is("CollectionTextValue1"))
        );
    }

    private void assertExampleCaseMetadata(Map<String, Object> data, boolean formatted) {
        assertAll(
            () -> assertThat(data.get(MetaData.CaseField.JURISDICTION.getReference()), is(AUTOTEST_1)),
            () -> assertThat(data.get(MetaData.CaseField.CASE_TYPE.getReference()), is(CASE_TYPE_A)),
            () -> assertThat(data.get(MetaData.CaseField.CREATED_DATE.getReference()),
                is(formatted ? "07 05 2020" : CREATED_DATE_VALUE)),
            () -> assertThat(data.get(MetaData.CaseField.LAST_MODIFIED_DATE.getReference()),
                is(LAST_MODIFIED_DATE_VALUE)),
            () -> assertThat(data.get(MetaData.CaseField.LAST_STATE_MODIFIED_DATE.getReference()),
                is(LAST_STATE_MODIFIED_DATE_VALUE)),
            () -> assertThat(data.get(MetaData.CaseField.CASE_REFERENCE.getReference()),
                is(Long.parseLong(DEFAULT_CASE_REFERENCE))),
            () -> assertThat(data.get(MetaData.CaseField.STATE.getReference()), is(STATE_VALUE)),
            () -> assertThat(data.get(MetaData.CaseField.SECURITY_CLASSIFICATION.getReference()),
                is(SecurityClassification.PUBLIC.name()))
        );
    }

    private Map<String, Object> asMap(Object obj) {
        return (Map<String, Object>) obj;
    }

    private List<Map<String, Object>> asCollection(Object obj) {
        return (List<Map<String, Object>>) obj;
    }

    private CaseSearchResultViewResource executeRequest(ElasticsearchTestRequest searchRequest, String caseTypeParam,
                                                        String useCase) throws Exception {
        MockHttpServletRequestBuilder postRequest =
            createPostRequest(POST_SEARCH_CASES, searchRequest, caseTypeParam, useCase);

        return ElasticsearchTestHelper.executeRequest(postRequest, 200, mapper, mockMvc,
            CaseSearchResultViewResource.class);
    }

    private JsonNode executeErrorRequest(ElasticsearchTestRequest searchRequest,
                                         String caseTypeParam,
                                         String useCase,
                                         int expectedErrorCode) throws Exception {
        MockHttpServletRequestBuilder postRequest = createPostRequest(POST_SEARCH_CASES, searchRequest, caseTypeParam,
            useCase);

        return ElasticsearchTestHelper.executeRequest(postRequest, expectedErrorCode, mapper, mockMvc, JsonNode.class);
    }
}
