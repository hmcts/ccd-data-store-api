package uk.gov.hmcts.ccd.endpoint.std;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.casedeletion.CaseLink;
import uk.gov.hmcts.ccd.domain.model.caselinks.MigrationParameters;

import java.util.List;
import javax.inject.Inject;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.domain.model.casedeletion.CaseLink.builder;

public class MigrationEndpointIT extends WireMockBaseTest {
    private static final String URL = "/migration/populateCaseLinks";

    private final String CASE_TYPE_ID = "TestAddressBookCaseCaseLinks";
    private final String JURISDICTION_ID = "PROBATE";

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Inject
    private ApplicationParams applicationParams;

    private JdbcTemplate template;

    @Before
    public void setUp() {

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);
        //MockUtils.setSecurityAuthorities(authentication, AUTOTEST1_PUBLIC, AUTOTEST2_PUBLIC);
        //MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);
        //MockUtils.setSecurityAuthorities(authentication, CASEWORKER_CAA);
        MockUtils.setSecurityAuthorities(authentication, "caseworker-probate-public");

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        template = new JdbcTemplate(db);

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_missing_case_link.sql"})
    public void shouldPopulateCaseLinksWhereCaseHasSingleMissingCaseLink() throws Exception {
        MigrationParameters migrationParameters = new MigrationParameters(CASE_TYPE_ID, JURISDICTION_ID, 1L, 5);

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
        MigrationParameters migrationParameters = new MigrationParameters(CASE_TYPE_ID, JURISDICTION_ID, 1L, 5);

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
                .build()
        );

        assertCaseLinks(expectedCaseId1, expectedCaseLinksCaseId1);
        assertCaseLinks(expectedCaseId2, expectedCaseLinksCaseId2);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_missing_case_link.sql"})
    public void shouldPopulateCaseLinksWhereMultipleCasesHaveMultipleMissingCaseLinks() throws Exception {
        MigrationParameters migrationParameters = new MigrationParameters(CASE_TYPE_ID, JURISDICTION_ID, 1L, 5);

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
                .build()
        );

        Long expectedCaseId2 = 2L;
        List<CaseLink> expectedCaseLinksCaseId2 = List.of(
            builder()
                .caseId(expectedCaseId1)
                .linkedCaseId(4L)
                .caseTypeId(CASE_TYPE_ID)
                .build()
        );

        assertCaseLinks(expectedCaseId1, expectedCaseLinksCaseId1);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_missing_case_link.sql"})
    public void shouldPopulateCaseLinksWhereCaseHasExistingCaseLink() throws Exception {
        MigrationParameters migrationParameters = new MigrationParameters("TestAddressBookCaseCaseLinks", JURISDICTION_ID, 1L, 5);

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
        MigrationParameters migrationParameters = new MigrationParameters(CASE_TYPE_ID, JURISDICTION_ID, 2L, 5);

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

        mockMvc.perform(post(URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(migrationParameters)))
            .andExpect(status().is(200))
            .andReturn();

    }

    private void assertCaseLinks(Long expectedCaseId, List<CaseLink> expectedCaseLinks) {
        List<CaseLink> caseLinks = template.query(
            String.format("SELECT * FROM case_link where case_id=%s", expectedCaseId),
            new BeanPropertyRowMapper<>(CaseLink.class));

        assertTrue(expectedCaseLinks.equals(caseLinks));
    }

}
