package uk.gov.hmcts.ccd.endpoint.std;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

public class CaseDetailsEndpointPostStateIT extends WireMockBaseTest {
    private static final String JURISDICTION = "PROBATE";
    private static final String TEST_EVENT_ID = "TEST_EVENT";
    private static final String NO_PRE_STATES_EVENT = "NO_PRE_STATES_EVENT";
    private static final String HAS_PRE_STATES_EVENT = "HAS_PRE_STATES_EVENT";
    private static final String UID = "123";
    private static final String CASE_TYPE_POST_STATE = "TestAddressBookPostState";

    @Inject
    private WebApplicationContext wac;
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
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldChangeCaseStateWhenPostStateConditionMatches() throws Exception {
        final JsonNode data = createData();
        performRequestAndValidate(data, TEST_EVENT_ID, "state4");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldChangeCaseStateBasedOnPriorityWhenPostStateConditionMatches() throws Exception {
        final JsonNode data = createData();
        performRequestAndValidate(data, NO_PRE_STATES_EVENT, "CaseEnteredIntoLegacy");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldChangeToDefaultWhenPostStateConditionNotMatches() throws Exception {
        final JsonNode data = mapper.readTree("{"
            + "\"PersonLastName\":\"Test Last1\","
            + "\"PersonFirstName\":\"Test First\"}");
        performRequestAndValidate(data, NO_PRE_STATES_EVENT, "state4");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldStayInThePreviousStateWhenWildcardUsedInPostConditionState() throws Exception {
        final JsonNode data = createData();
        performRequestAndValidate(data, HAS_PRE_STATES_EVENT, "CaseCreated");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldIgnorePostStateConditionWhenFieldNotAssignedToEvent() throws Exception {
        final JsonNode data = createData();
        performRequestAndValidate(data, "Goodness", "CaseEnteredIntoLegacy");
    }

    private JsonNode createData() throws com.fasterxml.jackson.core.JsonProcessingException {
        return mapper.readTree("{"
            + "\"PersonLastName\":\"Test Last\","
            + "\"PersonFirstName\":\"Test First\"}");
    }

    private void performRequestAndValidate(JsonNode data, String eventId, String expectedCaseState) throws Exception {
        final String caseReference = "1601933818308168";
        final String URL = "/citizens/" + UID + "/jurisdictions/" + JURISDICTION + "/case-types/"
            + CASE_TYPE_POST_STATE + "/cases/" + caseReference + "/events";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(eventId);
        caseDetailsToSave.setEvent(triggeringEvent);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE_POST_STATE, eventId);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setData(JacksonUtils.convertValue(data));

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("Incorrect Case Type", CASE_TYPE_POST_STATE, savedCaseDetails.getCaseTypeId());
        assertEquals("State should have been updated", expectedCaseState, savedCaseDetails.getState());
    }

}
