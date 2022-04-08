package uk.gov.hmcts.ccd.endpoint.std;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.AuditRepository;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLink;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationParameters;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationResult;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.MockUtils.ROLE_CASEWORKER;
import static uk.gov.hmcts.ccd.MockUtils.ROLE_CASEWORKER_CAA;
import static uk.gov.hmcts.ccd.MockUtils.ROLE_CASEWORKER_PROBATE;
import static uk.gov.hmcts.ccd.MockUtils.ROLE_CASEWORKER_SSCS;
import static uk.gov.hmcts.ccd.data.caselinking.CaseLinkEntity.NON_STANDARD_LINK;
import static uk.gov.hmcts.ccd.domain.model.caselinking.CaseLink.builder;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.GET_ROLE_ASSIGNMENTS_PREFIX;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.emptyRoleAssignmentResponseJson;

@TestPropertySource(locations = "classpath:test.properties", properties = {"migrations.endpoint.enabled=true"})
class MigrationEndpointIT extends WireMockBaseTest {

    private static final String CASE_TYPE_ID = "TestAddressBookCaseCaseLinks";
    private static final String PROBATE_JURISDICTION_ID = "PROBATE";
    private static final String SSCS_JURISDICTION_ID = "SSCS";

    // data values as per:
    //                      classpath:sql/insert_cases_missing_case_link.sql
    //                      classpath:sql/insert_cases_multiple_missing_case_link.sql
    private static final Long CASE_LINKS_CASE_01_ID = 1L; // 3393027116986763
    private static final Long CASE_LINKS_CASE_02_ID = 2L; // 1504259907353537
    private static final Long CASE_LINKS_CASE_03_ID = 3L; // 1504259907353545
    private static final Long CASE_LINKS_CASE_04_ID = 4L; // 1504259907353552
    private static final Long CASE_LINKS_CASE_05_ID = 5L; // 1557845948403939
    private static final Long CASE_LINKS_CASE_06_ID = 6L; // 1504254784737847
    private static final String CASE_LINKS_CASE_01_TYPE = CASE_TYPE_ID;
    private static final String CASE_LINKS_CASE_02_TYPE = "TestAddressBookCase";
    private static final String CASE_LINKS_CASE_03_TYPE = CASE_TYPE_ID;
    private static final String CASE_LINKS_CASE_04_TYPE = "TestAddressBookCase";
    private static final String CASE_LINKS_CASE_05_TYPE = "TestAddressBookCaseCaseLinks";
    private static final String CASE_LINKS_CASE_06_TYPE = "TestAddressBookCase";

    @Inject
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @SpyBean
    private AuditRepository auditRepository;

    private JdbcTemplate template;

    @BeforeEach
    public void setUp() {
        setUpMvc(wac);
    }

    @Nested
    @DisplayName("POST " + PopulateCaseLinks.URL)
    class PopulateCaseLinks {

        protected static final String URL = "/migration/populateCaseLinks";

        @ParameterizedTest(name = "Should populate CaseLinks where case has single missing CaseLink - #{index} - `{0}`")
        @CsvSource({
            ROLE_CASEWORKER_PROBATE,
            ROLE_CASEWORKER_CAA
        })
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_missing_case_link.sql"})
        void shouldPopulateCaseLinksWhereCaseHasSingleMissingCaseLink(String caseworkerRole) throws Exception {
            MigrationParameters migrationParameters =
                new MigrationParameters(CASE_LINKS_CASE_01_TYPE, PROBATE_JURISDICTION_ID, CASE_LINKS_CASE_01_ID, 1);

            stubIdamAndRasRequests(caseworkerRole);

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(migrationParameters)))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn();

            assertCaseLinksMatch_missing_case_links(CASE_LINKS_CASE_01_ID);
        }

        @ParameterizedTest(
            name = "Should populate CaseLinks where case has multiple missing CaseLinks - #{index} - `{0}`"
        )
        @CsvSource({
            ROLE_CASEWORKER_PROBATE,
            ROLE_CASEWORKER_CAA
        })
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_multiple_missing_case_link.sql"})
        void shouldPopulateCaseLinksWhereCaseHasMultipleMissingCaseLinks(String caseworkerRole) throws Exception {
            MigrationParameters migrationParameters =
                new MigrationParameters(CASE_LINKS_CASE_01_TYPE, PROBATE_JURISDICTION_ID, CASE_LINKS_CASE_01_ID, 1);

            stubIdamAndRasRequests(caseworkerRole);

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(migrationParameters)))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn();

            assertCaseLinksMatch_multiple_missing_case_links(CASE_LINKS_CASE_01_ID);
        }

        @ParameterizedTest(
            name = "Should populate CaseLinks where multiple cases has multiple missing CaseLinks - #{index} - `{0}`"
        )
        @CsvSource({
            ROLE_CASEWORKER_PROBATE,
            ROLE_CASEWORKER_CAA
        })
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_multiple_missing_case_link.sql"})
        void shouldPopulateCaseLinksWhereMultipleCasesHaveMultipleMissingCaseLinks(String caseworkerRole)
            throws Exception {

            MigrationParameters migrationParameters =
                new MigrationParameters(CASE_TYPE_ID, PROBATE_JURISDICTION_ID, CASE_LINKS_CASE_01_ID, 5);

            stubIdamAndRasRequests(caseworkerRole);

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(migrationParameters)))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn();

            assertCaseLinksMatch_multiple_missing_case_links(CASE_LINKS_CASE_01_ID);
            assertCaseLinksMatch_multiple_missing_case_links(CASE_LINKS_CASE_03_ID);
        }

        @ParameterizedTest(name = "Should populate CaseLinks where case has existing CaseLink - #{index} - `{0}`")
        @CsvSource({
            ROLE_CASEWORKER_PROBATE,
            ROLE_CASEWORKER_CAA
        })
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_missing_case_link.sql"})
        void shouldPopulateCaseLinksWhereCaseHasExistingCaseLink(String caseworkerRole) throws Exception {
            MigrationParameters migrationParameters =
                new MigrationParameters(CASE_LINKS_CASE_03_TYPE, PROBATE_JURISDICTION_ID, CASE_LINKS_CASE_03_ID, 1);

            stubIdamAndRasRequests(caseworkerRole);

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(migrationParameters)))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn();

            assertCaseLinksMatch_missing_case_links(CASE_LINKS_CASE_03_ID);
        }

        @ParameterizedTest(name = "Should NOT populate CaseLink due to starting ID - #{index} - `{0}`")
        @CsvSource({
            ROLE_CASEWORKER_PROBATE,
            ROLE_CASEWORKER_CAA
        })
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_missing_case_link.sql"})
        void shouldNotPopulateMissingCaseLinksDueToStartingID(String caseworkerRole) throws Exception {
            MigrationParameters migrationParameters =
                new MigrationParameters(CASE_LINKS_CASE_01_TYPE, PROBATE_JURISDICTION_ID, CASE_LINKS_CASE_01_ID + 1, 5);

            stubIdamAndRasRequests(caseworkerRole);

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(migrationParameters)))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn();

            assertCaseLinks(CASE_LINKS_CASE_01_ID, Collections.emptyList());
        }

        @ParameterizedTest(name = "Should NOT populate CaseLink with incorrect Jurisdiction - #{index} - `{0}`")
        @CsvSource({
            ROLE_CASEWORKER_PROBATE,
            ROLE_CASEWORKER_CAA
        })
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_missing_case_link.sql"})
        void shouldNotPopulateCaseLinksWithIncorrectJurisdiction(String caseworkerRole) throws Exception {

            // NB: case type and jurisdiction mismatch
            MigrationParameters migrationParameters =
                new MigrationParameters(CASE_LINKS_CASE_01_TYPE, SSCS_JURISDICTION_ID, CASE_LINKS_CASE_01_ID, 5);

            stubIdamAndRasRequests(caseworkerRole);

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(migrationParameters)))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn();

            assertCaseLinks(CASE_LINKS_CASE_01_ID, Collections.emptyList());
        }

        @Test
        @DisplayName("Should return FORBIDDEN response when case type not available to user")
        void shouldReturnForbiddenWhenCaseTypeNotAvailableToUser() throws Exception {
            MigrationParameters migrationParameters =
                new MigrationParameters(CASE_TYPE_ID, SSCS_JURISDICTION_ID, 1L, 5);

            // NB: IDAM and migration jurisdiction is SSCS but migration case type is for PROBATE
            //     therefore error as case type not permitted.

            stubIdamAndRasRequests(ROLE_CASEWORKER_SSCS);

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(migrationParameters)))
                .andExpect(status().is(HttpStatus.FORBIDDEN.value()))
                .andReturn();

        }

        @Test
        @DisplayName("Should verify audit log entry created")
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_missing_case_link.sql"})
        void shouldVerifyLogs() throws Exception {

            Long caseDataId = CASE_LINKS_CASE_01_ID;

            MigrationParameters migrationParameters =
                new MigrationParameters(CASE_LINKS_CASE_01_TYPE, PROBATE_JURISDICTION_ID, caseDataId, 5);

            stubIdamAndRasRequests(ROLE_CASEWORKER_PROBATE);

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(migrationParameters)))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn();

            ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
            verify(auditRepository).save(captor.capture());

            assertEquals(AuditOperationType.MIGRATION.getLabel(), captor.getValue().getOperationType());
            assertEquals(caseDataId.toString(), captor.getValue().getCaseId());
            assertEquals(CASE_TYPE_ID, captor.getValue().getCaseType());
            assertEquals(PROBATE_JURISDICTION_ID, captor.getValue().getJurisdiction());
        }

        @Test
        @DisplayName("Should verify migration result after CaseLink population")
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_missing_case_link.sql"})
        void shouldVerifyMigrationResultAfterCaseLinkPopulation() throws Exception {
            MigrationParameters migrationParameters =
                new MigrationParameters(CASE_TYPE_ID, PROBATE_JURISDICTION_ID, CASE_LINKS_CASE_01_ID, 100);

            stubIdamAndRasRequests(ROLE_CASEWORKER_PROBATE);

            MvcResult mvcResult = mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(migrationParameters)))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn();

            String content = mvcResult.getResponse().getContentAsString();
            MigrationResult migrationResult = mapper.readValue(content, MigrationResult.class);

            assertEquals(2, migrationResult.getRecordCount());
            assertEquals(CASE_LINKS_CASE_03_ID, migrationResult.getFinalRecordId());
        }
    }

    @Nested
    @DisplayName("Migrations Endpoint Disabled")
    @TestPropertySource(locations = "classpath:test.properties", properties = {"migrations.endpoint.enabled=false"})
    class MigrationsEndpointDisabled {

        @Inject // NB: using a fresh WAC instance, so it loads using new test.properties
        private WebApplicationContext wac;

        @BeforeEach
        public void setUp() {
            setUpMvc(wac);
        }

        @Test
        @DisplayName("Should return NOT_FOUND if migrations endpoint is disabled")
        void shouldReturnForbiddenWhenCaseTypeNotAvailableToUser() throws Exception {

            MigrationParameters migrationParameters =
                new MigrationParameters(CASE_TYPE_ID, PROBATE_JURISDICTION_ID, 1L, 5);

            mockMvc.perform(post(PopulateCaseLinks.URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(migrationParameters)))
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andReturn();
        }

    }

    private void assertCaseLinksMatch_missing_case_links(Long expectedCaseId) {
        List<CaseLink> expectedCaseLinks = new ArrayList<>();

        // data values as per:   classpath:sql/insert_cases_missing_case_link.sql
        if (CASE_LINKS_CASE_01_ID.equals(expectedCaseId)) {
            expectedCaseLinks.add(builder()
                .caseId(expectedCaseId)
                .linkedCaseId(CASE_LINKS_CASE_02_ID)
                .caseTypeId(CASE_LINKS_CASE_02_TYPE)
                .standardLink(NON_STANDARD_LINK)
                .build());
        } else if (CASE_LINKS_CASE_03_ID.equals(expectedCaseId)) {
            expectedCaseLinks.add(builder()
                .caseId(expectedCaseId)
                .linkedCaseId(CASE_LINKS_CASE_04_ID)
                .caseTypeId(CASE_LINKS_CASE_04_TYPE)
                .standardLink(NON_STANDARD_LINK)
                .build());
        }

        assertCaseLinks(expectedCaseId, expectedCaseLinks);
    }

    private void assertCaseLinksMatch_multiple_missing_case_links(Long expectedCaseId) {
        List<CaseLink> expectedCaseLinks = new ArrayList<>();

        // data values as per:   classpath:sql/insert_cases_multiple_missing_case_link.sql
        if (CASE_LINKS_CASE_01_ID.equals(expectedCaseId)) {
            expectedCaseLinks.add(builder()
                .caseId(expectedCaseId)
                .linkedCaseId(CASE_LINKS_CASE_02_ID)
                .caseTypeId(CASE_LINKS_CASE_02_TYPE)
                .standardLink(NON_STANDARD_LINK)
                .build());
            expectedCaseLinks.add(builder()
                .caseId(expectedCaseId)
                .linkedCaseId(CASE_LINKS_CASE_05_ID)
                .caseTypeId(CASE_LINKS_CASE_05_TYPE)
                .standardLink(NON_STANDARD_LINK)
                .build());
        } else if (CASE_LINKS_CASE_03_ID.equals(expectedCaseId)) {
            expectedCaseLinks.add(builder()
                .caseId(expectedCaseId)
                .linkedCaseId(CASE_LINKS_CASE_04_ID)
                .caseTypeId(CASE_LINKS_CASE_04_TYPE)
                .standardLink(NON_STANDARD_LINK)
                .build());
            expectedCaseLinks.add(builder()
                .caseId(expectedCaseId)
                .linkedCaseId(CASE_LINKS_CASE_06_ID)
                .caseTypeId(CASE_LINKS_CASE_06_TYPE)
                .standardLink(NON_STANDARD_LINK)
                .build());
        }

        assertCaseLinks(expectedCaseId, expectedCaseLinks);
    }

    private void assertCaseLinks(Long expectedCaseId, List<CaseLink> expectedCaseLinks) {
        List<CaseLink> caseLinks = template.query(
            String.format("SELECT * FROM case_link where case_id=%s", expectedCaseId),
            new BeanPropertyRowMapper<>(CaseLink.class));

        assertEquals(expectedCaseLinks, caseLinks);
    }

    private void setUpMvc(WebApplicationContext wac) {
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        template = new JdbcTemplate(db);
    }

    private void stubIdamAndRasRequests(String caseworkerRole) {
        String userId = UUID.randomUUID().toString();

        MockUtils.setSecurityAuthorities(authentication, ROLE_CASEWORKER, caseworkerRole);

        stubUserInfo(userId, ROLE_CASEWORKER, caseworkerRole);

        stubFor(WireMock.get(urlMatching(GET_ROLE_ASSIGNMENTS_PREFIX + userId))
            .willReturn(okJson(emptyRoleAssignmentResponseJson()).withStatus(200)));
    }

}
