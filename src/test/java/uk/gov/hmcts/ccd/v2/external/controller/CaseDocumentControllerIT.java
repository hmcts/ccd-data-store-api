package uk.gov.hmcts.ccd.v2.external.controller;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.inject.Inject;
import javax.sql.DataSource;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.customheaders.CustomHeadersFilter;
import uk.gov.hmcts.ccd.v2.V2;

public class CaseDocumentControllerIT extends WireMockBaseTest {

    @Inject
    private WebApplicationContext wac;

    @Inject
    private CustomHeadersFilter customHeadersFilter;

    @Inject
    protected ApplicationParams applicationParams;

    @Inject
    protected DataSource db;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private MockMvc mockMvc;
    private static String CUSTOM_CONTEXT = "";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).addFilters(customHeadersFilter).build();
        CUSTOM_CONTEXT = applicationParams.getCallbackPassthruHeaderContexts().get(0);

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200WhenGetValidCaseDocuments() throws Exception {

        final MvcResult result = mockMvc
            .perform(get(String.format("http://localhost:%s/cases/1504259907353651/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1", super.wiremockPort))
                         .contentType(MediaType.APPLICATION_JSON)
                         .header("Accept", V2.MediaType.CASE_DOCUMENT)
                         .header(CUSTOM_CONTEXT, new JSONObject(responseJson1).toString())
                         .header("experimental", true))
            .andExpect(status().is(200))
            .andExpect(jsonPath("$._links.self.href").value(String
                .format("http://localhost:%s/cases/1504259907353651/documents/"
                    + "05e7cd7e-7041-4d8a-826a-7bb49dfd83d1", super.wiremockPort)))
            .andExpect(jsonPath("$.documentMetadata.documentPermissions.permissions", hasSize(1)))
            .andExpect(jsonPath("$.documentMetadata.documentPermissions.permissions[0]").value("READ"))
            .andReturn();

        assertTrue(result.getResponse().getHeaderNames().contains(CUSTOM_CONTEXT));
    }
}
