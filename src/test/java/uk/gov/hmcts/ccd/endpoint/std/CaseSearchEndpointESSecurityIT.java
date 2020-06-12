package uk.gov.hmcts.ccd.endpoint.std;

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
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;

import javax.inject.Inject;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.*;

class CaseSearchEndpointESSecurityIT extends ElasticsearchBaseTest {

    private static final String POST_SEARCH_CASES = "/searchCases";
    private static final String CASE_TYPE_ID_PARAM = "ctid";

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

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void shouldOnlyReturnCasesFromCaseTypesWithJurisdictionRole() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, AUTOTEST2_PUBLIC);

        String searchRequest = ElasticsearchTestRequest.builder()
            .query(matchAllQuery())
            .build().toJsonString();

        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
            .contentType(MediaType.APPLICATION_JSON)
            .param(CASE_TYPE_ID_PARAM, "AAT,MAPPER,SECURITY")
            .content(searchRequest))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResult caseSearchResult = mapper.readValue(responseAsString, CaseSearchResult.class);

        assertAll(
            () -> assertThat(caseSearchResult.getTotal(), is(1L)),
            () -> assertThat(caseSearchResult.getCases().get(0).getCaseTypeId(), is(CASE_TYPE_B))
        );
    }

    @Test
    void shouldReturnCasesWithLowerCaseSecurityClassification() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, AUTOTEST1_RESTRICTED);

        String searchRequest = ElasticsearchTestRequest.builder()
            .query(matchQuery(STATE, STATE_VALUE))
            .sort(CREATED_DATE)
            .build().toJsonString();

        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
            .contentType(MediaType.APPLICATION_JSON)
            .param(CASE_TYPE_ID_PARAM, CASE_TYPE_C)
            .content(searchRequest))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResult caseSearchResult = mapper.readValue(responseAsString, CaseSearchResult.class);

        assertAll(
            () -> assertThat(caseSearchResult.getTotal(), is(2L)),
            () -> assertThat(caseSearchResult.getCases().get(0).getSecurityClassification(), is(SecurityClassification.PRIVATE)),
            () -> assertThat(caseSearchResult.getCases().get(1).getSecurityClassification(), is(SecurityClassification.PUBLIC))
        );
    }

    @Test
    void shouldNotReturnCasesWithHigherCaseSecurityClassification() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, AUTOTEST1_PUBLIC);

        String searchRequest = ElasticsearchTestRequest.builder()
            .query(matchAllQuery())
            .build().toJsonString();

        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
            .contentType(MediaType.APPLICATION_JSON)
            .param(CASE_TYPE_ID_PARAM, CASE_TYPE_C)
            .content(searchRequest))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResult caseSearchResult = mapper.readValue(responseAsString, CaseSearchResult.class);

        assertAll(
            () -> assertThat(caseSearchResult.getTotal(), is(1L)),
            () -> assertThat(caseSearchResult.getCases().get(0).getSecurityClassification(), is(SecurityClassification.PUBLIC))
        );
    }

    @Test
    void shouldReturnAuthorisedCaseFieldsWithPrivateRole() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, AUTOTEST1_PRIVATE);

        String searchRequest = ElasticsearchTestRequest.builder()
            .query(matchQuery(STATE, STATE_VALUE))
            .sort(CREATED_DATE)
            .build().toJsonString();

        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
            .contentType(MediaType.APPLICATION_JSON)
            .param(CASE_TYPE_ID_PARAM, CASE_TYPE_C)
            .content(searchRequest))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResult caseSearchResult = mapper.readValue(responseAsString, CaseSearchResult.class);

        assertAll(
            () -> assertThat(caseSearchResult.getTotal(), is(2L)),
            () -> assertThat(caseSearchResult.getCases().get(0).getData().size(), is(1)),
            () -> assertThat(caseSearchResult.getCases().get(0).getData().get(PHONE_FIELD), is(notNullValue()))
        );
    }

    @Test
    void shouldReturnAuthorisedCaseFieldsWithRestrictedRole() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, AUTOTEST1_RESTRICTED);

        String searchRequest = ElasticsearchTestRequest.builder()
            .query(matchQuery(STATE, STATE_VALUE))
            .build().toJsonString();

        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
            .contentType(MediaType.APPLICATION_JSON)
            .param(CASE_TYPE_ID_PARAM, CASE_TYPE_C)
            .content(searchRequest))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResult caseSearchResult = mapper.readValue(responseAsString, CaseSearchResult.class);

        assertAll(
            () -> assertThat(caseSearchResult.getTotal(), is(2L)),
            () -> assertThat(caseSearchResult.getCases().get(0).getData().size(), is(2)),
            () -> assertThat(caseSearchResult.getCases().get(0).getData().get(PHONE_FIELD), is(notNullValue())),
            () -> assertThat(caseSearchResult.getCases().get(0).getData().get("AddressUKField"), is(notNullValue()))
        );
    }

    @Test
    void shouldReturnAuthorisedCaseFieldsWithPublicRole() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, AUTOTEST1_PUBLIC);

        String searchRequest = ElasticsearchTestRequest.builder()
            .query(matchAllQuery())
            .build().toJsonString();

        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
            .contentType(MediaType.APPLICATION_JSON)
            .param(CASE_TYPE_ID_PARAM, CASE_TYPE_C)
            .content(searchRequest))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResult caseSearchResult = mapper.readValue(responseAsString, CaseSearchResult.class);

        assertAll(
            () -> assertThat(caseSearchResult.getTotal(), is(1L)),
            () -> assertThat(caseSearchResult.getCases().get(0).getData().size(), is(14)),
            () -> assertThat(caseSearchResult.getCases().get(0).getData().get("AddressUKField"), is(nullValue()))
        );
    }

    @Test
    void shouldReturnCasesWithAuthorisedState() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, AUTOTEST1_RESTRICTED);

        String searchRequest = ElasticsearchTestRequest.builder()
            .query(matchQuery(STATE, IN_PROGRESS_STATE))
            .build().toJsonString();

        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
            .contentType(MediaType.APPLICATION_JSON)
            .param(CASE_TYPE_ID_PARAM, CASE_TYPE_C)
            .content(searchRequest))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResult caseSearchResult = mapper.readValue(responseAsString, CaseSearchResult.class);

        assertAll(
            () -> assertThat(caseSearchResult.getTotal(), is(1L)),
            () -> assertThat(caseSearchResult.getCases().get(0).getReference(), is(1589460099608690L))
        );
    }

    @Test
    void shouldNotReturnCasesWithUnauthorisedState() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, AUTOTEST1_PRIVATE);

        String searchRequest = ElasticsearchTestRequest.builder()
            .query(matchQuery(STATE, IN_PROGRESS_STATE))
            .build().toJsonString();

        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
            .contentType(MediaType.APPLICATION_JSON)
            .param(CASE_TYPE_ID_PARAM, CASE_TYPE_C)
            .content(searchRequest))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResult caseSearchResult = mapper.readValue(responseAsString, CaseSearchResult.class);

        assertAll(
            () -> assertThat(caseSearchResult.getTotal(), is(0L))
        );
    }
}
