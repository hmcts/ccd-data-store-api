package uk.gov.hmcts.ccd.endpoint.ui;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;

import javax.inject.Inject;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.MockUtils.ROLE_CASEWORKER_PRIVATE;
import static uk.gov.hmcts.ccd.MockUtils.ROLE_CASEWORKER_PUBLIC;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation.WORKBASKET;

public class SearchWithSortIT extends WireMockBaseTest {

    private static final String GET_CASES =
        "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/cases";

    private static final String TEST_CASE_TYPE = "TestAddressBookCase";
    private static final String TEST_JURISDICTION = "PROBATE";
    private static final String CASE_CREATED = "CaseCreated";

    @Inject
    private WebApplicationContext wac;
    @Inject
    private ApplicationParams applicationParams;
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        MockUtils.setSecurityAuthorities(authentication, ROLE_CASEWORKER_PUBLIC, ROLE_CASEWORKER_PRIVATE);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        ReflectionTestUtils.setField(applicationParams, "paginationPageSize", 3);

        stubFor(WireMock.get(urlMatching("/api/data/case-type/TestAddressBookCase/version"))
            .willReturn(okJson("{\"version\": \"34\"}")));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_search_sort_cases.sql"})
    public void expectBadRequestErrorForMalformattedDateParam() throws Exception {
        MvcResult result = mockMvc.perform(get(GET_CASES)
            .contentType(JSON_CONTENT_TYPE)
            .param("view", WORKBASKET)
            .param("case_type", TEST_CASE_TYPE)
            .param("jurisdiction", TEST_JURISDICTION)
            .param("state", CASE_CREATED)
            .param("page", "1")
            .param("created_date", "2016-06-22T::.000")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(400))
            .andReturn();

    }

}
