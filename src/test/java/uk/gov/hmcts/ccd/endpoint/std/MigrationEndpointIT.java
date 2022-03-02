package uk.gov.hmcts.ccd.endpoint.std;

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
import uk.gov.hmcts.ccd.domain.model.casedeletion.CaseLink;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationParameters;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationResult;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.MockUtils.VALID_IDAM_TOKEN_CASEWORKER_CAA;
import static uk.gov.hmcts.ccd.MockUtils.VALID_IDAM_TOKEN_CASEWORKER_PROBATE;
import static uk.gov.hmcts.ccd.MockUtils.VALID_IDAM_TOKEN_CASEWORKER_SSCS;
import static uk.gov.hmcts.ccd.domain.model.casedeletion.CaseLink.builder;

@TestPropertySource(locations = "classpath:test.properties", properties = {"migrations.endpoint.enabled=true"})
class MigrationEndpointIT extends WireMockBaseTest {

    private static final String CASE_TYPE_ID = "TestAddressBookCaseCaseLinks";
    private static final String PROBATE_JURISDICTION_ID = "PROBATE";
    private static final String SSCS_JURISDICTION_ID = "SSCS";

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
            VALID_IDAM_TOKEN_CASEWORKER_PROBATE,
            VALID_IDAM_TOKEN_CASEWORKER_CAA
        })
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_missing_case_link.sql"})
        void shouldPopulateCaseLinksWhereCaseHasSingleMissingCaseLink(String idamToken) throws Exception {
            MigrationParameters migrationParameters =
                new MigrationParameters(CASE_TYPE_ID, PROBATE_JURISDICTION_ID, 1L, 5);

            stubIdamRequest(idamToken);

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(migrationParameters)))
                .andExpect(status().is(HttpStatus.OK.value()))
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

        @ParameterizedTest(
            name = "Should populate CaseLinks where case has multiple missing CaseLinks - #{index} - `{0}`"
        )
        @CsvSource({
            VALID_IDAM_TOKEN_CASEWORKER_PROBATE,
            VALID_IDAM_TOKEN_CASEWORKER_CAA
        })
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_multiple_missing_case_link.sql"})
        void shouldPopulateCaseLinksWhereCaseHasMultipleMissingCaseLinks(String idamToken) throws Exception {
            MigrationParameters migrationParameters =
                new MigrationParameters(CASE_TYPE_ID, PROBATE_JURISDICTION_ID, 1L, 5);

            stubIdamRequest(idamToken);

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(migrationParameters)))
                .andExpect(status().is(HttpStatus.OK.value()))
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

        @ParameterizedTest(
            name = "Should populate CaseLinks where multiple cases has multiple missing CaseLinks - #{index} - `{0}`"
        )
        @CsvSource({
            VALID_IDAM_TOKEN_CASEWORKER_PROBATE,
            VALID_IDAM_TOKEN_CASEWORKER_CAA
        })
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_multiple_missing_case_link.sql"})
        void shouldPopulateCaseLinksWhereMultipleCasesHaveMultipleMissingCaseLinks(String idamToken) throws Exception {
            MigrationParameters migrationParameters =
                new MigrationParameters(CASE_TYPE_ID, PROBATE_JURISDICTION_ID, 1L, 5);

            stubIdamRequest(idamToken);

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(migrationParameters)))
                .andExpect(status().is(HttpStatus.OK.value()))
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

        @ParameterizedTest(name = "Should populate CaseLinks where case has existing CaseLink - #{index} - `{0}`")
        @CsvSource({
            VALID_IDAM_TOKEN_CASEWORKER_PROBATE,
            VALID_IDAM_TOKEN_CASEWORKER_CAA
        })
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_missing_case_link.sql"})
        void shouldPopulateCaseLinksWhereCaseHasExistingCaseLink(String idamToken) throws Exception {
            MigrationParameters migrationParameters =
                new MigrationParameters(CASE_TYPE_ID, PROBATE_JURISDICTION_ID, 1L, 5);

            stubIdamRequest(idamToken);

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(migrationParameters)))
                .andExpect(status().is(HttpStatus.OK.value()))
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

        @ParameterizedTest(name = "Should NOT populate CaseLink due to starting ID - #{index} - `{0}`")
        @CsvSource({
            VALID_IDAM_TOKEN_CASEWORKER_PROBATE,
            VALID_IDAM_TOKEN_CASEWORKER_CAA
        })
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_missing_case_link.sql"})
        void shouldNotPopulateMissingCaseLinksDueToStartingID(String idamToken) throws Exception {
            MigrationParameters migrationParameters =
                new MigrationParameters(CASE_TYPE_ID, PROBATE_JURISDICTION_ID, 2L, 5);

            stubIdamRequest(idamToken);

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(migrationParameters)))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn();

            assertCaseLinks(1L, Collections.emptyList());
        }

        @ParameterizedTest(name = "Should NOT populate CaseLink with incorrect Jurisdiction - #{index} - `{0}`")
        @CsvSource({
            VALID_IDAM_TOKEN_CASEWORKER_PROBATE,
            VALID_IDAM_TOKEN_CASEWORKER_CAA
        })
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_missing_case_link.sql"})
        void shouldNotPopulateCaseLinksWithIncorrectJurisdiction(String idamToken) throws Exception {

            // NB: case type and jurisdiction mismatch
            MigrationParameters migrationParameters =
                new MigrationParameters(CASE_TYPE_ID, SSCS_JURISDICTION_ID, 1L, 5);

            stubIdamRequest(idamToken);

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(migrationParameters)))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn();

            Long expectedCaseId = 1L;
            List<CaseLink> expectedCaseLinks = Collections.emptyList();
            assertCaseLinks(expectedCaseId, expectedCaseLinks);

        }

        @Test
        @DisplayName("Should return FORBIDDEN response when case type not available to user")
        void shouldReturnForbiddenWhenCaseTypeNotAvailableToUser() throws Exception {
            MigrationParameters migrationParameters =
                new MigrationParameters(CASE_TYPE_ID, SSCS_JURISDICTION_ID, 1L, 5);

            // NB: IDAM and migration jurisdiction is SSCS but migration case type is for PROBATE
            //     therefore error as case type not permitted.

            stubIdamRequest(VALID_IDAM_TOKEN_CASEWORKER_SSCS);

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

            Long caseDataId = 1L;

            MigrationParameters migrationParameters =
                new MigrationParameters(CASE_TYPE_ID, PROBATE_JURISDICTION_ID, caseDataId, 5);

            stubIdamRequest(VALID_IDAM_TOKEN_CASEWORKER_PROBATE);

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
                new MigrationParameters(CASE_TYPE_ID, PROBATE_JURISDICTION_ID, 1L, 5);

            stubIdamRequest(VALID_IDAM_TOKEN_CASEWORKER_PROBATE);

            MvcResult mvcResult = mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(migrationParameters)))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn();

            String content = mvcResult.getResponse().getContentAsString();
            MigrationResult migrationResult = mapper.readValue(content, MigrationResult.class);

            assertEquals(2, migrationResult.getRecordCount());
            assertEquals(3, migrationResult.getFinalRecordId());
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

    private void stubIdamRequest(String token) {
        MockUtils.setSecurityAuthorities(token, authentication);

        removeStub(IDAM_DEFAULT_DETAILS_STUB_ID);
    }

}
