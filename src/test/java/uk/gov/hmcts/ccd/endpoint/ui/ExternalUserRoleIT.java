package uk.gov.hmcts.ccd.endpoint.ui;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
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

import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ExternalUserRoleIT extends WireMockBaseTest {

    private static final String GET_CASE_HISTORY_FOR_EVENT_EXTERNAL =
        "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCaseExternal/cases/1504259907353529/"
            + "events/%d/case-history";


    @Inject
    private WebApplicationContext wac;

    @MockBean
    private AuditRepository auditRepository;
    private MockMvc mockMvc;
    private JdbcTemplate template;

    @Before
    public void setUp() {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        template = new JdbcTemplate(db);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_case_event_history_external.sql"})
    public void shouldReturnForbiddenWhenEventUserRoleIsExternal() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_EXTERNAL_USER);

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
