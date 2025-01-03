package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.Arrays;
import javax.inject.Inject;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.CaseEventsResource;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.createRoleAssignmentRecord;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.createRoleAssignmentResponse;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.enhanceGetCaseTypeStubWithAccessProfiles;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.roleToAccessProfileDefinition;

public class CaseControllerEventsIT extends WireMockBaseTest {
    private static final String GET_CASE_EVENTS = "/cases/1504259907353529/events";
    private static final String UID_WITH_EVENT_ACCESS = "123";
    private static final String UID_WITH_NO_EVENT_ACCESS = "1234";
    private static final String ASSIGNMENT_1 = "assignment1";
    private static final String ASSIGNMENT_2 = "assignment2";
    private static final String ASSIGNMENT_3 = "assignment3";
    private static final String CASE_ID_1 = "1504259907353529";
    private static final String CASE_TYPE = "TestAddressBookNoEventAccessToCaseRole";
    private static final String CASE_ROLE_1 = "[02-TEST-EVENT-ACCESS-ROLE]";
    private static final String CASE_ROLE_2 = "citizen";
    private static final String CASE_ROLE_3 = "caseworker-probate-public";

    private static final int NUMBER_OF_CASES = 1;
    public static final String JURISDICTION = "PROBATE";

    @Inject
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    private JdbcTemplate template;

    @Before
    public void setUp() throws Exception {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            MockUtils.setSecurityAuthorities(RandomStringUtils.randomAlphanumeric(10), authentication,
                MockUtils.ROLE_CASEWORKER_PUBLIC, MockUtils.ROLE_CITIZEN);
        } else {
            MockUtils.setSecurityAuthorities(RandomStringUtils.randomAlphanumeric(10), authentication,
                MockUtils.ROLE_CASEWORKER_PUBLIC);
        }
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        template = new JdbcTemplate(db);

        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            CaseTypeDefinition caseTypeDefinition = enhanceGetCaseTypeStubWithAccessProfiles(
                "bookcase-no-event-access-to-caserole-definition.json",
                roleToAccessProfileDefinition(CASE_ROLE_1),
                roleToAccessProfileDefinition(CASE_ROLE_2),
                roleToAccessProfileDefinition(CASE_ROLE_3));

            stubFor(WireMock.get(urlMatching("/api/data/case-type/" + CASE_TYPE))
                .willReturn(okJson(defaultObjectMapper.writeValueAsString(caseTypeDefinition))
                    .withStatus(200)));
        }
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases_event_access_case_roles.sql"})
    public void shouldNotReturnEventHistoryDataForCitizenWhoHasNoAccessToEvents() throws Exception {

        assertCaseDataResultSetSize();

        stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + UID_WITH_NO_EVENT_ACCESS))
            .willReturn(okJson(defaultObjectMapper.writeValueAsString(
                createRoleAssignmentResponse(
                    Arrays.asList(
                        createRoleAssignmentRecord(ASSIGNMENT_1, CASE_ID_1, CASE_TYPE, JURISDICTION,
                            CASE_ROLE_2, UID_WITH_EVENT_ACCESS, false),
                        createRoleAssignmentRecord(ASSIGNMENT_2, CASE_ID_1, CASE_TYPE, JURISDICTION,
                            CASE_ROLE_3, UID_WITH_EVENT_ACCESS, false)
                    )
                )))
                .withStatus(200)));

        stubUserInfo("1234");

        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, "Bearer " + UID_WITH_NO_EVENT_ACCESS);
        headers.add(V2.EXPERIMENTAL_HEADER, "true");

        final MvcResult result = mockMvc.perform(get(GET_CASE_EVENTS)
            .contentType(JSON_CONTENT_TYPE)
            .headers(headers))
            .andExpect(status().is(200))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseEventsResource saveCaseEventsResource = mapper.readValue(content, CaseEventsResource.class);
        assertNotNull("Saved Case Details should not be null", saveCaseEventsResource);
        assertEquals("Should not contain events with case role access", 1,
            saveCaseEventsResource.getAuditEvents().size());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases_event_access_case_roles.sql"})
    public void shouldReturnEventHistoryDataForCitizenWhoHasCaseRoleAccess() throws Exception {
        // we need to add AccessProfiles to the caseType and include prefix when pseudo generation enabled
        String caseRole1 = CASE_ROLE_1;
        String caseRole2 = CASE_ROLE_2;

        stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + UID_WITH_EVENT_ACCESS))
            .willReturn(okJson(defaultObjectMapper.writeValueAsString(
                createRoleAssignmentResponse(
                    Arrays.asList(
                        createRoleAssignmentRecord(ASSIGNMENT_1, CASE_ID_1, CASE_TYPE, JURISDICTION,
                            caseRole1, UID_WITH_EVENT_ACCESS, false),
                        createRoleAssignmentRecord(ASSIGNMENT_2, CASE_ID_1, CASE_TYPE, JURISDICTION,
                            caseRole2, UID_WITH_EVENT_ACCESS, false),
                        createRoleAssignmentRecord(ASSIGNMENT_3, CASE_ID_1, CASE_TYPE, JURISDICTION,
                            CASE_ROLE_3, UID_WITH_EVENT_ACCESS, false)
                    )
                )))
                .withStatus(200)));

        assertCaseDataResultSetSize();

        stubUserInfo(UID_WITH_EVENT_ACCESS);

        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, "Bearer " + UID_WITH_EVENT_ACCESS);
        headers.add(V2.EXPERIMENTAL_HEADER, "true");

        final MvcResult result = mockMvc.perform(get(GET_CASE_EVENTS)
            .contentType(JSON_CONTENT_TYPE)
            .headers(headers))
            .andExpect(status().is(200))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseEventsResource savedCaseEventsResource = mapper.readValue(content, CaseEventsResource.class);
        assertNotNull("Saved Case Details should not be null", savedCaseEventsResource);
        assertEquals("Should contain events with case role access", 2,
            savedCaseEventsResource.getAuditEvents().size());

    }

    private void assertCaseDataResultSetSize() {
        final int count = template.queryForObject("SELECT count(1) as n FROM case_data", Integer.class);
        assertEquals("Incorrect case data size", NUMBER_OF_CASES, count);
    }
}
