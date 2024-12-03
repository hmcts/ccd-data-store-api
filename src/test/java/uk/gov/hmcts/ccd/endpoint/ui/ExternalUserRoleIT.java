package uk.gov.hmcts.ccd.endpoint.ui;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.auditlog.AuditRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

import javax.inject.Inject;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.userRoleAssignmentJson;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.GET_ROLE_ASSIGNMENTS_PREFIX;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.roleAssignmentResponseJson;

public class ExternalUserRoleIT extends WireMockBaseTest {

    private static final String EXTERNAL_CASETYPE = "TestAddressBookCaseExternal";
    private static final String GET_CASE_HISTORY_FOR_EVENT_EXTERNAL =
        "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/"
            + EXTERNAL_CASETYPE + "/cases/1504259907353529/"
            + "events/%d/case-history";


    @Inject
    private WebApplicationContext wac;

    @SpyBean
    private AuditRepository auditRepository;
    private MockMvc mockMvc;
    private JdbcTemplate template;

    @Before
    public void setUp() {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_EXTERNAL_USER);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        template = new JdbcTemplate(db);

        String userId = "124";
        stubUserInfo(userId, MockUtils.ROLE_EXTERNAL_USER);
        String roleAssignmentResponseJson = roleAssignmentResponseJson(
            userRoleAssignmentJson(userId, MockUtils.ROLE_EXTERNAL_USER, CASE_01_REFERENCE, EXTERNAL_CASETYPE)
        );
        stubFor(WireMock.get(urlMatching(GET_ROLE_ASSIGNMENTS_PREFIX + userId))
            .willReturn(okJson(roleAssignmentResponseJson).withStatus(200)));

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_case_event_history_external.sql"})
    public void shouldReturnForbiddenWhenEventUserRoleIsExternal() throws Exception {

        // Check that we have the expected test data set size
        List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", 1, resultList.size());

        List<AuditEvent> eventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("Incorrect data initiation", 3, eventList.size());

        mockMvc.perform(get(String.format(GET_CASE_HISTORY_FOR_EVENT_EXTERNAL,
                eventList.get(1).getId()))
                .contentType(JSON_CONTENT_TYPE)
                .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(HttpStatus.FORBIDDEN.value()))
            .andReturn();
    }

}
