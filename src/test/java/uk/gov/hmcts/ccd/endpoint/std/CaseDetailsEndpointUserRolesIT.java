package uk.gov.hmcts.ccd.endpoint.std;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;

import javax.inject.Inject;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PRIVATE;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

public class CaseDetailsEndpointUserRolesIT extends WireMockBaseTest {
    private static final String JURISDICTION = "PROBATE";
    private static final String TEST_EVENT_ID = "TEST_EVENT";
    private static final String UID = "123";
    private static final String CASE_TYPE_CREATOR_ROLE = "TestAddressBookCreatorCase";
    private static final String CASE_TYPE_CREATOR_ROLE_NO_CREATE_ACCESS = "TestAddressBookCreatorNoCreateAccessCase";

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
    public void shouldReturn201WhenPostCreateCaseWithCreatorRoleWithNoDataForCaseworker() throws Exception {
        final String description = "A very long comment.......";
        final String summary = "Short comment";
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE_CREATOR_ROLE
            + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(description);
        triggeringEvent.setSummary(summary);
        caseDetailsToSave.setEvent(triggeringEvent);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE_CREATOR_ROLE, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Expected empty case data", "", mapper.readTree(mvcResult.getResponse().getContentAsString())
            .get("case_data").asText());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertTrue("Incorrect Case Reference", uidService.validateUID(String.valueOf(savedCaseDetails
            .getReference())));
        assertEquals("Incorrect Case Type", CASE_TYPE_CREATOR_ROLE, savedCaseDetails.getCaseTypeId());
        assertEquals("Incorrect Data content", "{}", savedCaseDetails.getData().toString());
        assertEquals("state3", savedCaseDetails.getState());

        final List<AuditEvent> caseAuditEventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("Incorrect number of case events", 1, caseAuditEventList.size());

        final AuditEvent caseAuditEvent = caseAuditEventList.get(0);
        assertEquals("123", caseAuditEvent.getUserId());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals("Case in state 3", caseAuditEvent.getStateName());
        assertEquals(savedCaseDetails.getCreatedDate(), caseAuditEvent.getCreatedDate());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertEquals("Event ID", TEST_EVENT_ID, caseAuditEvent.getEventId());
        assertEquals("Description", description, caseAuditEvent.getDescription());
        assertEquals("Summary", summary, caseAuditEvent.getSummary());
        assertTrue(caseAuditEvent.getDataClassification().isEmpty());
        assertThat(caseAuditEvent.getSecurityClassification(), equalTo(PRIVATE));
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseWithCreatorRoleWithNoDataForCitizen() throws Exception {
        final String description = "A very long comment.......";
        final String summary = "Short comment";

        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE_CREATOR_ROLE
            + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(description);
        triggeringEvent.setSummary(summary);
        caseDetailsToSave.setEvent(triggeringEvent);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE_CREATOR_ROLE, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Expected empty case data", "", mapper.readTree(mvcResult.getResponse().getContentAsString())
            .get("case_data").asText());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertTrue("Incorrect Case Reference", uidService.validateUID(String.valueOf(savedCaseDetails
            .getReference())));
        assertEquals("Incorrect Case Type", CASE_TYPE_CREATOR_ROLE, savedCaseDetails.getCaseTypeId());
        assertEquals("Incorrect Data content", "{}", savedCaseDetails.getData().toString());
        assertEquals("state3", savedCaseDetails.getState());

        final List<AuditEvent> caseAuditEventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("Incorrect number of case events", 1, caseAuditEventList.size());

        final AuditEvent caseAuditEvent = caseAuditEventList.get(0);
        assertEquals("123", caseAuditEvent.getUserId());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals("Case in state 3", caseAuditEvent.getStateName());
        assertEquals(savedCaseDetails.getCreatedDate(), caseAuditEvent.getCreatedDate());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertEquals("Event ID", TEST_EVENT_ID, caseAuditEvent.getEventId());
        assertEquals("Description", description, caseAuditEvent.getDescription());
        assertEquals("Summary", summary, caseAuditEvent.getSummary());
        assertTrue(caseAuditEvent.getDataClassification().isEmpty());
        assertThat(caseAuditEvent.getSecurityClassification(), equalTo(PRIVATE));
    }

    @Test
    public void shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccessOnCreatorRoleForCaseworker() throws Exception {
        shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccess("caseworkers",
            CASE_TYPE_CREATOR_ROLE_NO_CREATE_ACCESS);
    }

    @Test
    public void shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccessOnCreatorRoleForCitizen() throws Exception {
        shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccess("citizens",
            CASE_TYPE_CREATOR_ROLE_NO_CREATE_ACCESS);
    }

    private void shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccess(String role, String caseType)
                                                                                                    throws Exception {
        final String URL = "/" + role + "/0/jurisdictions/" + JURISDICTION + "/case-types/" + caseType + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();
    }

}
