package uk.gov.hmcts.ccd.endpoint.ui;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultView;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultViewItem;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.MockUtils.*;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation.WORKBASKET;

public class SearchWithSortIT extends WireMockBaseTest {

    private static final String GET_CASES = "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/cases";

    private static final String TEST_CASE_TYPE = "TestAddressBookCase";
    private static final String TEST_JURISDICTION = "PROBATE";
    private static final String CASE_CREATED = "CaseCreated";

    @Inject
    private WebApplicationContext wac;
    @Inject
    private ApplicationParams applicationParams;
    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;

    private MockMvc mockMvc;
    private StubMapping stubMapping;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, ROLE_CASEWORKER_PUBLIC, ROLE_CASEWORKER_PRIVATE);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        ReflectionTestUtils.setField(applicationParams, "paginationPageSize", 3);

        stubMapping  = stubFor(WireMock.get(urlMatching("/api/data/case-type/TestAddressBookCase/version")).willReturn(okJson("{\"version\": \"34\"}")));
    }

    @After
    public void tearDown() {
        removeStub(stubMapping);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_search_sort_cases.sql"})
    public void workbasketSearchWithSortOrder() throws Exception {
        MvcResult result = mockMvc.perform(get(GET_CASES)
            .contentType(JSON_CONTENT_TYPE)
            .param("view", WORKBASKET)
            .param("case_type", TEST_CASE_TYPE)
            .param("jurisdiction", TEST_JURISDICTION)
            .param("state", CASE_CREATED)
            .param("page", "1")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        String contentAsString = result.getResponse().getContentAsString();
        final SearchResultView searchResultView = mapper.readValue(contentAsString,
            SearchResultView.class);
        final List<SearchResultViewItem> searchResultViewItems = searchResultView.getSearchResultViewItems();

        assertEquals("Incorrect view items count", 4, searchResultViewItems.size());

        assertEquals("John", searchResultViewItems.get(0).getCaseFields().get("PersonFirstName"));
        assertEquals(null, searchResultViewItems.get(0).getCaseFields().get("PersonAddress"));

        assertEquals("Angel", searchResultViewItems.get(1).getCaseFields().get("PersonFirstName"));
        assertEquals("George", searchResultViewItems.get(2).getCaseFields().get("PersonFirstName"));
        assertEquals("1504259907353545", searchResultViewItems.get(2).getCaseId());
        assertEquals("George", searchResultViewItems.get(3).getCaseFields().get("PersonFirstName"));
        assertEquals("1504259907353548", searchResultViewItems.get(3).getCaseId());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_search_sort_cases.sql"})
    public void workbasketSearchWithRoleSpecificSortOrder() throws Exception {
        String roleWithSortOrder = ROLE_TEST_PUBLIC;
        MockUtils.setSecurityAuthorities(authentication, roleWithSortOrder, ROLE_CASEWORKER_PUBLIC, ROLE_CASEWORKER_PRIVATE);
        MvcResult result = mockMvc.perform(get(GET_CASES)
            .contentType(JSON_CONTENT_TYPE)
            .param("view", WORKBASKET)
            .param("case_type", TEST_CASE_TYPE)
            .param("jurisdiction", TEST_JURISDICTION)
            .param("state", CASE_CREATED)
            .param("page", "1")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        String contentAsString = result.getResponse().getContentAsString();
        final SearchResultView searchResultView = mapper.readValue(contentAsString,
            SearchResultView.class);
        final List<SearchResultViewItem> searchResultViewItems = searchResultView.getSearchResultViewItems();

        assertEquals("Incorrect view items count", 4, searchResultViewItems.size());

        assertEquals("John", searchResultViewItems.get(0).getCaseFields().get("PersonFirstName"));
        assertEquals(null, searchResultViewItems.get(0).getCaseFields().get("PersonAddress"));

        assertEquals("Angel", searchResultViewItems.get(1).getCaseFields().get("PersonFirstName"));
        assertEquals("SE1 4EE", ((Map) searchResultViewItems.get(1).getCaseFields().get("PersonAddress"))
            .get("Postcode"));

        assertEquals("George", searchResultViewItems.get(2).getCaseFields().get("PersonFirstName"));
        assertEquals("W11 5CF", ((Map) searchResultViewItems.get(2).getCaseFields().get("PersonAddress"))
            .get("Postcode"));
        assertEquals("George", searchResultViewItems.get(3).getCaseFields().get("PersonFirstName"));
        assertEquals("W11 5DF", ((Map) searchResultViewItems.get(3).getCaseFields().get("PersonAddress"))
            .get("Postcode"));
    }
}
