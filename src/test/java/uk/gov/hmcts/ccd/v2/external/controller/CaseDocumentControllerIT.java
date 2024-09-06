package uk.gov.hmcts.ccd.v2.external.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import javax.inject.Inject;
import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
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
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocumentMetadata;
import uk.gov.hmcts.ccd.v2.external.domain.Permission;
import uk.gov.hmcts.ccd.v2.external.resource.CaseDocumentResource;

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
    protected static final ObjectMapper mapper = new ObjectMapper();
    private static String CUSTOM_CONTEXT = "";
    public static final JSONObject responseJson1 = new JSONObject("""
        {
            "user_task": {
                "task_data": {
                    "task_id": "000001",
                    "task_name": "Task 1 name"
                },
                "complete_task": "false"
            }
        }
        """);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).addFilters(customHeadersFilter).build();
        CUSTOM_CONTEXT = applicationParams.getCallbackPassthruHeaderContexts().get(0);

        mapper.registerModule(new Jackson2HalModule());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200WhenGetValidCaseDocuments() throws Exception {

        final MvcResult result = mockMvc
            .perform(get(String.format("http://localhost:%s/cases/1504259907353651/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1", super.wiremockPort))
                         .contentType(MediaType.APPLICATION_JSON)
                         .header("Accept", V2.MediaType.CASE_DOCUMENT)
                         .header(CUSTOM_CONTEXT, responseJson1.toString())
                         .header("experimental", true))
            .andExpect(status().is(200))
            .andReturn();

        assertTrue(result.getResponse().getHeaderNames().contains(CUSTOM_CONTEXT));
        final CaseDocumentResource caseDocumentResource
            = mapper.readValue(result.getResponse().getContentAsString(), CaseDocumentResource.class);

        CaseDocumentMetadata caseDocumentMetadata = caseDocumentResource.getDocumentMetadata();
        Optional<Link> self = caseDocumentResource.getLink("self");

        assertAll(
            () -> assertThat(self.get().getHref(),
                             is(String
                                    .format("http://localhost:%s/cases/1504259907353651/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1", super.wiremockPort))),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions().getPermissions(), hasSize(1)),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions().getPermissions(),
                hasItems(Permission.READ))
        );
    }
}
