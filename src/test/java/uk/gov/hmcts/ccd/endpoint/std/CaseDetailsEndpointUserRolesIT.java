package uk.gov.hmcts.ccd.endpoint.std;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PRIVATE;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

class CaseDetailsEndpointUserRolesIT extends WireMockBaseTest {
    private static final String JURISDICTION = "PROBATE";
    private static final String TEST_EVENT_ID = "TEST_EVENT";
    private static final String UID = "123";
    private static final String CASE_TYPE_CREATOR_ROLE = "TestAddressBookCreatorCase";
    private static final String CASE_TYPE_CREATOR_ROLE_NO_CREATE_ACCESS = "TestAddressBookCreatorNoCreateAccessCase";

    @Inject
    private WebApplicationContext wac;

    private MockMvc mockMvc;
    private JdbcTemplate template;

    @BeforeEach
    void setUp() {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        template = new JdbcTemplate(db);
    }

    @Test
    void shouldReturn201WhenPostCreateCaseWithCreatorRoleWithNoDataForCaseworker() throws Exception {
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

        assertEquals("", mapper.readTree(mvcResult.getResponse().getContentAsString())
            .get("case_data").asText(), "Expected empty case data");

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals(1, caseDetailsList.size(), "Incorrect Case Reference");

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertTrue(uidService.validateUID(String.valueOf(savedCaseDetails
            .getReference())));
        assertEquals(CASE_TYPE_CREATOR_ROLE, savedCaseDetails.getCaseTypeId(), "Incorrect Case Type");
        assertEquals("{}", savedCaseDetails.getData().toString(), "Incorrect Data content");
        assertEquals("state3", savedCaseDetails.getState());

        final List<AuditEvent> caseAuditEventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals(1, caseAuditEventList.size(), "Incorrect number of case events");

        final AuditEvent caseAuditEvent = caseAuditEventList.get(0);
        assertEquals("123", caseAuditEvent.getUserId());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals("Case in state 3", caseAuditEvent.getStateName());
        assertEquals(savedCaseDetails.getCreatedDate(), caseAuditEvent.getCreatedDate());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertThat(caseAuditEvent.getEventId()).isEqualTo(TEST_EVENT_ID);
        assertThat(caseAuditEvent.getDescription()).isEqualTo(description);
        assertThat(caseAuditEvent.getSummary()).isEqualTo(summary);
        assertThat(caseAuditEvent.getDataClassification()).isEmpty();
        assertThat(caseAuditEvent.getSecurityClassification()).isEqualTo(PRIVATE);
    }

    @Test
    void shouldReturn201WhenPostCreateCaseWithCreatorRoleWithNoDataForCitizen() throws Exception {
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

        assertThat(mapper.readTree(mvcResult.getResponse().getContentAsString())
            .get("case_data").asText()).isEqualTo("");

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals(1, caseDetailsList.size(), "Incorrect number of cases");

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertTrue(uidService.validateUID(String.valueOf(savedCaseDetails
            .getReference())), "Incorrect Case Reference");
        assertEquals(CASE_TYPE_CREATOR_ROLE, savedCaseDetails.getCaseTypeId(), "Incorrect Case Type");
        assertEquals("{}", savedCaseDetails.getData().toString(), "Incorrect Data content");
        assertEquals("state3", savedCaseDetails.getState());

        final List<AuditEvent> caseAuditEventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals(1, caseAuditEventList.size(), "Incorrect number of case events");

        final AuditEvent caseAuditEvent = caseAuditEventList.get(0);
        assertEquals("123", caseAuditEvent.getUserId());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals("Case in state 3", caseAuditEvent.getStateName());
        assertEquals(savedCaseDetails.getCreatedDate(), caseAuditEvent.getCreatedDate());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertThat(caseAuditEvent.getEventId()).isEqualTo(TEST_EVENT_ID);
        assertThat(caseAuditEvent.getDescription()).isEqualTo(description);
        assertThat(caseAuditEvent.getSummary()).isEqualTo(summary);
        assertThat(caseAuditEvent.getDataClassification().isEmpty());
        assertThat(caseAuditEvent.getSecurityClassification()).isEqualTo(PRIVATE);
    }

    @Test
    void shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccessOnCreatorRoleForCaseworker() throws Exception {
        shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccess("caseworkers",
            CASE_TYPE_CREATOR_ROLE_NO_CREATE_ACCESS);
    }

    @Test
    void shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccessOnCreatorRoleForCitizen() throws Exception {
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
