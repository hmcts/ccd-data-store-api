package uk.gov.hmcts.ccd.endpoint.std;

import io.searchbox.client.JestClient;
import io.searchbox.core.MultiSearchResult;
import io.searchbox.core.SearchResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.AuditRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CaseSearchEndpointIT extends WireMockBaseTest {

    private static final String POST_SEARCH_CASES = "/searchCases";

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @MockBean
    private JestClient jestClient;

    @SpyBean
    private AuditRepository auditRepository;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void testSearchCaseDetails() throws Exception {

        String caseDetailElastic = createCaseDetailsElastic("1535450291607660");

        stubElasticSearchSearchRequestWillReturn(caseDetailElastic);

        String searchRequest = "{\"query\": {\"match_all\": {}}}";
        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
                .contentType(JSON_CONTENT_TYPE)
                .param("ctid", "TestAddressBookCase")
                .content(searchRequest))
                .andExpect(status().is(200))
                .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResult caseSearchResults = mapper.readValue(responseAsString,
                                                              CaseSearchResult.class);

        assertThat(caseSearchResults.getTotal(), is(30L));
        List<CaseDetails> caseDetails = caseSearchResults.getCases();
        assertThat(caseDetails, hasSize(1));
        assertThat(caseDetails, hasItem(hasProperty("reference", equalTo(1535450291607660L))));
        assertThat(caseDetails, hasItem(hasProperty("jurisdiction", equalTo("AUTOTEST1"))));
        assertThat(caseDetails, hasItem(hasProperty("caseTypeId", equalTo("AAT"))));
        assertThat(caseDetails, hasItem(hasProperty("lastModified",
                                                    equalTo(LocalDateTime.parse("2018-08-28T09:58:11.643")))));
        assertThat(caseDetails, hasItem(hasProperty("createdDate",
                equalTo(LocalDateTime.parse("2018-08-28T09:58:11.627")))));
        assertThat(caseDetails, hasItem(hasProperty("state", equalTo("TODO"))));
        assertThat(caseDetails, hasItem(hasProperty("securityClassification",
                equalTo(SecurityClassification.PUBLIC))));
    }

    @Test
    public void shouldAuditLogSearchCases() throws Exception {

        String reference1 = "1535450291607660";
        String reference2 = "1535450291607670";
        String caseDetailElastic1 = createCaseDetailsElastic(reference1);
        String caseDetailElastic2 = createCaseDetailsElastic(reference2);

        stubElasticSearchSearchRequestWillReturn(caseDetailElastic1, caseDetailElastic2);

        String searchRequest = "{\"query\": {\"match_all\": {}}}";
        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
            .contentType(JSON_CONTENT_TYPE)
            .param("ctid", "TestAddressBookCase", "TestAddressBookCase4")
            .content(searchRequest))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResult caseSearchResults = mapper.readValue(responseAsString,
            CaseSearchResult.class);

        List<CaseDetails> caseDetails = caseSearchResults.getCases();
        assertThat(caseDetails, hasSize(2));
        assertThat(caseDetails, hasItem(hasProperty("reference", equalTo(1535450291607660L))));
        assertThat(caseDetails, hasItem(hasProperty("reference", equalTo(1535450291607670L))));

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.SEARCH_CASE.getLabel()));
        assertThat(captor.getValue().getCaseId(), is("1535450291607660,1535450291607670"));
        assertThat(captor.getValue().getIdamId(), is("Cloud.Strife@test.com"));
        assertThat(captor.getValue().getInvokingService(), is("ccd-data"));
        assertThat(captor.getValue().getHttpStatus(), is(200));
        assertThat(captor.getValue().getListOfCaseTypes(), is("TestAddressBookCase,TestAddressBookCase4"));
    }

    private String createCaseDetailsElastic(String reference) {
        return "{\n"
            + "\"reference\": " + reference + ",\n"
            + "\"last_modified\": \"2018-08-28T09:58:11.643Z\",\n"
            + "\"state\": \"TODO\",\n"
            + "\"@version\": \"1\",\n"
            + "\"data_classification\": {},\n"
            + "\"id\": 18,\n"
            + "\"security_classification\": \"PUBLIC\",\n"
            + "\"jurisdiction\": \"AUTOTEST1\",\n"
            + "\"@timestamp\": \"2018-08-28T09:58:13.044Z\",\n"
            + "\"data\": {},\n"
            + "\"created_date\": \"2018-08-28T09:58:11.627Z\",\n"
            + "\"index_id\": \"autotest1_aat_cases\",\n"
            + "\"case_type_id\": \"AAT\"\n"
            + "}";
    }

    private void stubElasticSearchSearchRequestWillReturn(String... caseDetailElastic) throws java.io.IOException {
        MultiSearchResult multiSearchResult = mock(MultiSearchResult.class);
        when(multiSearchResult.isSucceeded()).thenReturn(true);

        SearchResult searchResult = mock(SearchResult.class);
        when(searchResult.getTotal()).thenReturn(30L);
        when(searchResult.getSourceAsStringList()).thenReturn(newArrayList(caseDetailElastic));

        MultiSearchResult.MultiSearchResponse response = mock(MultiSearchResult.MultiSearchResponse.class);
        when(multiSearchResult.getResponses()).thenReturn(Collections.singletonList(response));
        Whitebox.setInternalState(response, "searchResult", searchResult);

        given(jestClient.execute(any())).willReturn(multiSearchResult);
    }
}
