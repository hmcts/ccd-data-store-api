package uk.gov.hmcts.ccd.v2.internal.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
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
import java.util.List;
import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.domain.types.CollectionValidator.VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.*;

class UICaseSearchControllerIT extends ElasticsearchBaseTest {

    private static final String POST_SEARCH_CASES = "/internal/searchCases";
    private static final String CASE_TYPE_ID_PARAM = "ctid";
    private static final String USE_CASE_PARAM = "usecase";
    private static final String CASE_FIELD_ID = "caseFieldId";

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

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    void shouldReturnAllCaseDetailsForDefaultUseCase() throws Exception {
        String searchRequest = ElasticsearchTestRequest.builder()
            .query(boolQuery()
                .must(matchQuery(caseData(NUMBER_FIELD), NUMBER_VALUE)) // ES Double
                .must(matchQuery(caseData(YES_OR_NO_FIELD), YES_OR_NO_VALUE)) // ES Keyword
                .must(matchQuery(caseData(TEXT_FIELD), TEXT_VALUE)) // ES Text
                .must(matchQuery(caseData(DATE_FIELD), DATE_VALUE)) // ES Date
                .must(matchQuery(caseData(PHONE_FIELD), PHONE_VALUE)) // ES Phone
                .must(matchQuery(caseData(COUNTRY_FIELD), ElasticsearchTestHelper.COUNTRY_VALUE)) // Complex
                .must(matchQuery(caseData(COLLECTION_FIELD) + VALUE_SUFFIX, COLLECTION_VALUE)) // Collection
                .must(matchQuery(STATE, STATE_VALUE)))
            .build().toJsonString();

        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
            .contentType(MediaType.APPLICATION_JSON)
            .param(CASE_TYPE_ID_PARAM, CASE_TYPE_A)
            .content(searchRequest))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResultViewResource caseSearchResultViewResource = mapper.readValue(responseAsString, CaseSearchResultViewResource.class);

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
        String searchRequest = ElasticsearchTestRequest.builder()
            .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
            .build().toJsonString();

        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
            .contentType(MediaType.APPLICATION_JSON)
            .param(CASE_TYPE_ID_PARAM, CASE_TYPE_A)
            .content(searchRequest))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResultViewResource caseSearchResultViewResource = mapper.readValue(responseAsString, CaseSearchResultViewResource.class);

        assertAll(
            () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
            () -> assertDefaultUseCaseHeaders(caseSearchResultViewResource.getHeaders())
        );
    }

    @Test
    void shouldReturnAllHeaderInfoForSpecifiedUseCase() throws Exception {
        String searchRequest = ElasticsearchTestRequest.builder()
            .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
            .build().toJsonString();

        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
            .contentType(MediaType.APPLICATION_JSON)
            .param(CASE_TYPE_ID_PARAM, CASE_TYPE_A)
            .param(USE_CASE_PARAM, "orgcases")
            .content(searchRequest))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResultViewResource caseSearchResultViewResource = mapper.readValue(responseAsString, CaseSearchResultViewResource.class);

        assertAll(
            () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
            () -> assertOrgCasesUseCaseHeaders(caseSearchResultViewResource.getHeaders())
        );
    }

    @Test
    void shouldReturnAllFormattedCaseDetails() throws Exception {
        String searchRequest = ElasticsearchTestRequest.builder()
            .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
            .build().toJsonString();

        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
            .contentType(MediaType.APPLICATION_JSON)
            .param(CASE_TYPE_ID_PARAM, CASE_TYPE_A)
            .param(USE_CASE_PARAM, "ORGCASES")
            .content(searchRequest))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResultViewResource caseSearchResultViewResource = mapper.readValue(responseAsString, CaseSearchResultViewResource.class);

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
        String searchRequest = ElasticsearchTestRequest.builder()
            .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
            .source(caseData(TEXT_FIELD))
            .source(caseData(nestedFieldId))
            .source(MetaData.CaseField.CASE_REFERENCE.getDbColumnName())
            .source("INVALID")
            .build().toJsonString();

        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
            .contentType(MediaType.APPLICATION_JSON)
            .param(CASE_TYPE_ID_PARAM, CASE_TYPE_A)
            .content(searchRequest))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResultViewResource caseSearchResultViewResource = mapper.readValue(responseAsString, CaseSearchResultViewResource.class);

        SearchResultViewItem caseDetails = caseSearchResultViewResource.getCases().get(0);
        assertAll(
            () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().size(), is(3)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().get(0).getCaseFieldId(), is(TEXT_FIELD)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().get(1).getCaseFieldId(), is(nestedFieldId)),
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
        String searchRequest = ElasticsearchTestRequest.builder()
            .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
            .source(caseData(TEXT_FIELD))
            .source(caseData(nestedFieldId))
            .source(MetaData.CaseField.CASE_REFERENCE.getDbColumnName())
            .source("INVALID")
            .build().toJsonString();

        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
            .contentType(MediaType.APPLICATION_JSON)
            .param(CASE_TYPE_ID_PARAM, CASE_TYPE_A)
            .param(USE_CASE_PARAM, "SEARCH")
            .content(searchRequest))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResultViewResource caseSearchResultViewResource = mapper.readValue(responseAsString, CaseSearchResultViewResource.class);

        SearchResultViewItem caseDetails = caseSearchResultViewResource.getCases().get(0);
        assertAll(
            () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().size(), is(3)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().get(0).getCaseFieldId(), is(TEXT_FIELD)),
            () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().get(1).getCaseFieldId(), is(nestedFieldId)),
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

    private void assertDefaultUseCaseHeaders(List<SearchResultViewHeaderGroup> headers) {
        List<String> expectedFields = Arrays.asList(HISTORY_COMPONENT_FIELD, FIXED_RADIO_LIST_FIELD, DOCUMENT_FIELD, ADDRESS_FIELD, COMPLEX_FIELD,
            COLLECTION_FIELD, MULTI_SELECT_LIST_FIELD, FIXED_LIST_FIELD, TEXT_AREA_FIELD, DATE_TIME_FIELD, DATE_FIELD,
            MONEY_FIELD, EMAIL_FIELD, PHONE_FIELD, YES_OR_NO_FIELD, NUMBER_FIELD, TEXT_FIELD,
            MetaData.CaseField.LAST_STATE_MODIFIED_DATE.getReference(), MetaData.CaseField.LAST_MODIFIED_DATE.getReference(),
            MetaData.CaseField.CREATED_DATE.getReference(), MetaData.CaseField.JURISDICTION.getReference(),
            MetaData.CaseField.CASE_TYPE.getReference(), MetaData.CaseField.SECURITY_CLASSIFICATION.getReference(),
            MetaData.CaseField.CASE_REFERENCE.getReference(), MetaData.CaseField.STATE.getReference());

        assertAll(
            () -> assertThat(headers.size(), is(1)),
            () -> assertThat(headers.get(0).getMetadata().getJurisdiction(), is(AUTOTEST_1)),
            () -> assertThat(headers.get(0).getMetadata().getCaseTypeId(), is(CASE_TYPE_A)),
            () -> assertThat(headers.get(0).getCases().size(), is(1)),
            () -> assertThat(headers.get(0).getCases().get(0), is(DEFAULT_CASE_REFERENCE)),
            () -> assertThat(headers.get(0).getFields().size(), is(25)),
            () -> expectedFields.forEach(f -> assertThat(headers.get(0).getFields(), hasItem(hasProperty(CASE_FIELD_ID, is(f)))))
        );
    }

    private void assertOrgCasesUseCaseHeaders(List<SearchResultViewHeaderGroup> headers) {
        List<String> expectedFields = Arrays.asList(TEXT_FIELD, EMAIL_FIELD, FIXED_LIST_FIELD, COLLECTION_FIELD, COMPLEX_FIELD, DATE_FIELD,
            DATE_TIME_FIELD, COMPLEX_FIELD + ".ComplexTextField", MetaData.CaseField.CREATED_DATE.getReference(),
            MetaData.CaseField.STATE.getReference());

        assertAll(
            () -> assertThat(headers.size(), is(1)),
            () -> assertThat(headers.get(0).getMetadata().getJurisdiction(), is(AUTOTEST_1)),
            () -> assertThat(headers.get(0).getMetadata().getCaseTypeId(), is(CASE_TYPE_A)),
            () -> assertThat(headers.get(0).getCases().size(), is(1)),
            () -> assertThat(headers.get(0).getCases().get(0), is(DEFAULT_CASE_REFERENCE)),
            () -> assertThat(headers.get(0).getFields().size(), is(10)),
            () -> expectedFields.forEach(f -> assertThat(headers.get(0).getFields(), hasItem(hasProperty(CASE_FIELD_ID, is(f)))))
        );
    }

    private void assertExampleCaseData(Map<String, Object> data, boolean formatted) {
        // TODO: After ACA-17, remove case data that should no longer be returned
        assertAll(
            () -> assertThat(asMap(data.get(ADDRESS_FIELD)).get(ADDRESS_LINE_1), is(STREET_VALUE)),
            () -> assertThat(asMap(data.get(ADDRESS_FIELD)).get(ADDRESS_LINE_2), is(ADDRESS_LINE_2_VALUE)),
            () -> assertThat(asMap(data.get(ADDRESS_FIELD)).get(ADDRESS_LINE_3), is(ADDRESS_LINE_3_VALUE)),
            () -> assertThat(asMap(data.get(ADDRESS_FIELD)).get(COUNTRY_NESTED_FIELD), is(COUNTRY_VALUE)),
            () -> assertThat(asMap(data.get(ADDRESS_FIELD)).get(COUNTY_FIELD), is(COUNTY_VALUE)),
            () -> assertThat(asMap(data.get(ADDRESS_FIELD)).get(POST_CODE_FIELD), is(POST_CODE_VALUE)),
            () -> assertThat(asMap(data.get(ADDRESS_FIELD)).get(TOWN_FIELD), is(TOWN_VALUE)),
            () -> assertThat(asCollection(data.get(COLLECTION_FIELD)).get(0).get(VALUE), is(COLLECTION_VALUE)),
            () -> assertThat(asCollection(data.get(COLLECTION_FIELD)).get(1).get(VALUE), is("CollectionTextValue1")),
            () -> assertThat(asMap(data.get(COMPLEX_FIELD)).get(COMPLEX_FIXED_LIST_FIELD), is("VALUE3")),
            () -> assertThat(asMap(asMap(data.get(COMPLEX_FIELD)).get(COMPLEX_NESTED_FIELD)).get(NESTED_NUMBER_FIELD), is(NESTED_NUMBER_FIELD_VALUE)),
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
            () -> assertThat(data.get(MONEY_FIELD), is(MONEY_VALUE)),
            () -> assertThat(((List<String>)data.get(MULTI_SELECT_LIST_FIELD)).size(), is(2)),
            () -> assertThat(((List<String>)data.get(MULTI_SELECT_LIST_FIELD)).get(0), is("OPTION2")),
            () -> assertThat(((List<String>)data.get(MULTI_SELECT_LIST_FIELD)).get(1), is("OPTION4")),
            () -> assertThat(data.get(NUMBER_FIELD), is(NUMBER_VALUE)),
            () -> assertThat(data.get(PHONE_FIELD), is(PHONE_VALUE)),
            () -> assertThat(data.get(TEXT_AREA_FIELD), is(TEXT_AREA_VALUE)),
            () -> assertThat(data.get(TEXT_FIELD), is(TEXT_VALUE)),
            () -> assertThat(data.get(YES_OR_NO_FIELD), is(YES_OR_NO_VALUE))
        );
    }

    public static void assertExampleCaseMetadata(Map<String, Object> data, boolean formatted) {
        assertAll(
            () -> assertThat(data.get(MetaData.CaseField.JURISDICTION.getReference()), is(AUTOTEST_1)),
            () -> assertThat(data.get(MetaData.CaseField.CASE_TYPE.getReference()), is(CASE_TYPE_A)),
            () -> assertThat(data.get(MetaData.CaseField.CREATED_DATE.getReference()), is(formatted ? "07 05 2020" : CREATED_DATE_VALUE)),
            () -> assertThat(data.get(MetaData.CaseField.LAST_MODIFIED_DATE.getReference()), is(LAST_MODIFIED_DATE_VALUE)),
            () -> assertThat(data.get(MetaData.CaseField.LAST_STATE_MODIFIED_DATE.getReference()), is(LAST_STATE_MODIFIED_DATE_VALUE)),
            () -> assertThat(data.get(MetaData.CaseField.CASE_REFERENCE.getReference()), is(Long.parseLong(DEFAULT_CASE_REFERENCE))),
            () -> assertThat(data.get(MetaData.CaseField.STATE.getReference()), is(STATE_VALUE)),
            () -> assertThat(data.get(MetaData.CaseField.SECURITY_CLASSIFICATION.getReference()), is(SecurityClassification.PUBLIC.name()))
        );
    }

    private Map<String, Object> asMap(Object obj) {
        return (Map<String, Object>)obj;
    }

    private List<Map<String, Object>> asCollection(Object obj) {
        return (List<Map<String, Object>>)obj;
    }
}
