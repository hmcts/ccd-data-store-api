package uk.gov.hmcts.ccd.endpoint.std;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

public class EventsEndpointIT extends WireMockBaseTest {
    private static final String GET_EVENTS_AS_CASEWORKER = "/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/cases/1504259907353529/events";

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;
    private JdbcTemplate template;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        template = new JdbcTemplate(db);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200_whenSearchWithParamsAsCaseWorker() throws Exception {

        assertCaseDataResultSetSize();

        final MvcResult result = mockMvc.perform(get(GET_EVENTS_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("case.PersonFirstName", "Janet ")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        List<AuditEvent> events = Arrays.asList(mapper.readValue(responseAsString, AuditEvent[].class));

        assertAll(
            () -> assertThat(events, hasSize(2)),
            () -> assertThat(events.get(1).getEventName(), equalTo("TEST TRIGGER_EVENT NAME")),
            () -> assertThat(events.get(0).getEventName(), containsString("GRACIOUS")),
            () -> assertThat(events.get(0).getSummary(), containsString("The summary 2")),
            () -> assertThat(events.get(0).getDescription(), containsString("Some comment 2")),
            () -> assertThat(events.get(0).getEventName(), containsString("GRACIOUS")),
            () -> assertThat(events.get(0).getCreatedDate().toString(), containsString(
                LocalDateTime.of(2017, 05, 9, 15, 31, 43).toString())),
            () -> assertThat(events.get(0).getCaseTypeId(), containsString("TestAddressBookCase")),
            () -> assertThat(events.get(0).getStateId(), containsString("state4"))
        );
    }

    /**
     * Checks that we have the expected test data set size, this is to ensure
     * that state filtering is correct.
     */
    private void assertCaseDataResultSetSize() {
        final int count = template.queryForObject("SELECT count(1) as n FROM case_data", Integer.class);
        assertEquals("Incorrect case data size", 16, count);
    }
}
