package uk.gov.hmcts.ccd.endpoint.std;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
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
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.AuditRepository;
import uk.gov.hmcts.ccd.domain.model.casedeletion.CaseLink;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationParameters;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationResult;
import uk.gov.hmcts.ccd.v2.external.resource.CaseResource;

import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.domain.model.casedeletion.CaseLink.builder;

public class MigrationEndpointIT extends WireMockBaseTest {
    private static final String URL = "/migration/populateCaseLinks";

    private final String CASE_TYPE_ID = "TestAddressBookCaseCaseLinks";
    private final String PROBATE_JURISDICTION_ID = "PROBATE";
    private final String SSCS_JURISDICTION_ID = "SSCS";

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Inject
    private ApplicationParams applicationParams;

    @SpyBean
    private AuditRepository auditRepository;

    private JdbcTemplate template;

    @Before
    public void setUp() {

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);
        MockUtils.setSecurityAuthorities(authentication, "caseworker-probate-public");

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        template = new JdbcTemplate(db);

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_missing_case_link.sql"})
    public void shouldPopulateCaseLinksWhereCaseHasSingleMissingCaseLink() throws Exception {
        MigrationParameters migrationParameters = new MigrationParameters(CASE_TYPE_ID, PROBATE_JURISDICTION_ID, 1L, 5);

        stubIdamRequest();

        mockMvc.perform(post(URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(migrationParameters)))
            .andExpect(status().is(200))
            .andReturn();

        Long expectedCaseId = 1L;
        List<CaseLink> expectedCaseLinks = List.of(
            builder()
                .caseId(expectedCaseId)
                .linkedCaseId(2L)
                .caseTypeId(CASE_TYPE_ID)
                .build()
        );
        assertCaseLinks(expectedCaseId, expectedCaseLinks);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_multiple_missing_case_link.sql"})
    public void shouldPopulateCaseLinksWhereCaseHasMultipleMissingCaseLinks() throws Exception {
        MigrationParameters migrationParameters = new MigrationParameters(CASE_TYPE_ID, PROBATE_JURISDICTION_ID, 1L, 5);

        stubIdamRequest();

        mockMvc.perform(post(URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(migrationParameters)))
            .andExpect(status().is(200))
            .andReturn();

        Long expectedCaseId1 = 1L;
        List<CaseLink> expectedCaseLinksCaseId1 = List.of(
            builder()
                .caseId(expectedCaseId1)
                .linkedCaseId(2L)
                .caseTypeId(CASE_TYPE_ID)
                .build(),
            builder()
                .caseId(expectedCaseId1)
                .linkedCaseId(5L)
                .caseTypeId(CASE_TYPE_ID)
                .build()
        );

        assertCaseLinks(expectedCaseId1, expectedCaseLinksCaseId1);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_multiple_missing_case_link.sql"})
    public void shouldPopulateCaseLinksWhereMultipleCasesHaveMultipleMissingCaseLinks() throws Exception {
        MigrationParameters migrationParameters = new MigrationParameters(CASE_TYPE_ID, PROBATE_JURISDICTION_ID, 1L, 5);

        stubIdamRequest();

        mockMvc.perform(post(URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(migrationParameters)))
            .andExpect(status().is(200))
            .andReturn();

        Long expectedCaseId1 = 1L;
        List<CaseLink> expectedCaseLinksCaseId1 = List.of(
            builder()
                .caseId(expectedCaseId1)
                .linkedCaseId(2L)
                .caseTypeId(CASE_TYPE_ID)
                .build(),
            builder()
                .caseId(expectedCaseId1)
                .linkedCaseId(5L)
                .caseTypeId(CASE_TYPE_ID)
                .build()
        );

        Long expectedCaseId2 = 3L;
        List<CaseLink> expectedCaseLinksCaseId2 = List.of(
            builder()
                .caseId(expectedCaseId2)
                .linkedCaseId(4L)
                .caseTypeId(CASE_TYPE_ID)
                .build(),
            builder()
                .caseId(expectedCaseId2)
                .linkedCaseId(6L)
                .caseTypeId(CASE_TYPE_ID)
                .build()
        );

        assertCaseLinks(expectedCaseId1, expectedCaseLinksCaseId1);
        assertCaseLinks(expectedCaseId2, expectedCaseLinksCaseId2);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_missing_case_link.sql"})
    public void shouldPopulateCaseLinksWhereCaseHasExistingCaseLink() throws Exception {
        MigrationParameters migrationParameters = new MigrationParameters("TestAddressBookCaseCaseLinks", PROBATE_JURISDICTION_ID, 1L, 5);

        stubIdamRequest();

        mockMvc.perform(post(URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(migrationParameters)))
            .andExpect(status().is(200))
            .andReturn();

        Long expectedCaseId = 3L;
        List<CaseLink> expectedCaseLinksCaseId = List.of(
            builder()
                .caseId(expectedCaseId)
                .linkedCaseId(4L)
                .caseTypeId("TestAddressBookCaseCaseLinks")
                .build()
        );
        assertCaseLinks(expectedCaseId, expectedCaseLinksCaseId);

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_missing_case_link.sql"})
    public void shouldNotPopulateMissingCaseLinksDueToStartingID() throws Exception {
        MigrationParameters migrationParameters = new MigrationParameters(CASE_TYPE_ID, PROBATE_JURISDICTION_ID, 2L, 5);

        stubIdamRequest();

        mockMvc.perform(post(URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(migrationParameters)))
            .andExpect(status().is(200))
            .andReturn();

        assertCaseLinks(1L, Collections.emptyList());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_missing_case_link.sql"})
    public void shouldNotPopulateCaseLinksWithIncorrectJurisdiction() throws Exception {
        MigrationParameters migrationParameters = new MigrationParameters(CASE_TYPE_ID, SSCS_JURISDICTION_ID, 1L, 5);

        stubIdamRequest();

        mockMvc.perform(post(URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(migrationParameters)))
            .andExpect(status().is(200))
            .andReturn();

        Long expectedCaseId = 1L;
        List<CaseLink> expectedCaseLinks = Collections.emptyList();
        assertCaseLinks(expectedCaseId, expectedCaseLinks);

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_missing_case_link.sql"})
    public void shouldVerifyLogs() throws Exception {

        Long caseDataId = 1L;

        MigrationParameters migrationParameters =
            new MigrationParameters(CASE_TYPE_ID, PROBATE_JURISDICTION_ID, caseDataId, 5);

        stubIdamRequest();

        mockMvc.perform(post(URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(migrationParameters)))
            .andExpect(status().is(200))
            .andReturn();

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.MIGRATION.getLabel()));
        assertThat(captor.getValue().getCaseId(), is(caseDataId.toString()));
        assertThat(captor.getValue().getCaseType(), is(CASE_TYPE_ID));
        assertThat(captor.getValue().getJurisdiction(), is(PROBATE_JURISDICTION_ID));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_missing_case_link.sql"})
    public void shouldVerifyMigrationResultAfterCaseLinkPopulation() throws Exception {
        MigrationParameters migrationParameters = new MigrationParameters(CASE_TYPE_ID, PROBATE_JURISDICTION_ID, 1L, 5);

        stubIdamRequest();

        MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(migrationParameters)))
            .andExpect(status().is(200))
            .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        MigrationResult migrationResult = mapper.readValue(content, MigrationResult.class);

        assertEquals(2, migrationResult.getRecordCount());
        assertEquals(3, migrationResult.getFinalRecordId());
    }


    private void assertCaseLinks(Long expectedCaseId, List<CaseLink> expectedCaseLinks) {
        List<CaseLink> caseLinks = template.query(
            String.format("SELECT * FROM case_link where case_id=%s", expectedCaseId),
            new BeanPropertyRowMapper<>(CaseLink.class));

        assertTrue(expectedCaseLinks.equals(caseLinks));
    }

    private void stubIdamRequest(){
        String userJson = "{\n"
            + "          \"sub\": \"Cloud.Strife@test.com\",\n"
            + "          \"uid\": \"89000\",\n"
            + "          \"roles\": [\n"
            + "            \"caseworker\",\n"
            + "            \"caseworker-probate-public\"\n"
            + "          ],\n"
            + "          \"name\": \"Cloud Strife\"\n"
            + "        }";
        stubFor(WireMock.get(urlMatching("/o/userinfo"))
            .willReturn(okJson(userJson).withStatus(200)));
    }

}
