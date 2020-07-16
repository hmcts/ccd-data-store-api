package uk.gov.hmcts.ccd.endpoint.ui;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;

import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CaseRolesIT extends WireMockBaseTest {
    private static final String GET_EVENT_TRIGGER_FOR_CASE_TYPE = "/aggregated/caseworkers/0/jurisdictions/PROBATE"
        + "/case-types/CaseRolesCase/event-triggers/CREATE-CASE";

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void getEventTriggerForCaseType_200_shouldAddFieldsWith_CreatorCaseRole() throws Exception {

        final MvcResult result = mockMvc.perform(get(GET_EVENT_TRIGGER_FOR_CASE_TYPE)
            .contentType(JSON_CONTENT_TYPE)
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        final CaseUpdateViewEvent caseUpdateViewEvent = mapper.readValue(result.getResponse().getContentAsString(),
            CaseUpdateViewEvent.class);
        assertNotNull("Event Trigger is null", caseUpdateViewEvent);

        assertThat("Unexpected Case ID", caseUpdateViewEvent.getCaseId(), is(nullValue()));
        assertEquals("Unexpected Event ID", "CREATE-CASE", caseUpdateViewEvent.getId());
        assertEquals("Unexpected Event Name", "CREATE-CASE", caseUpdateViewEvent.getName());
        assertEquals("Unexpected Event Show Event Notes", true, caseUpdateViewEvent.getShowEventNotes());
        assertEquals("Unexpected Event Description", "Creation event", caseUpdateViewEvent.getDescription());
        assertEquals("Unexpected Case Fields", 1, caseUpdateViewEvent.getCaseFields().size());

        final CaseViewField field1 = caseUpdateViewEvent.getCaseFields().get(0);
        assertThat(field1.getId(), equalTo("PersonFirstName"));

        assertThat(field1.getAccessControlLists().get(0).getRole(), equalTo("caseworker-probate-public"));
        assertThat(field1.getAccessControlLists().get(0).isCreate(), is(false));
        assertThat(field1.getAccessControlLists().get(0).isRead(), is(false));
        assertThat(field1.getAccessControlLists().get(0).isUpdate(), is(false));
        assertThat(field1.getAccessControlLists().get(0).isDelete(), is(false));

        assertThat(field1.getAccessControlLists().get(1).getRole(), equalTo("[CREATOR]"));
        assertThat(field1.getAccessControlLists().get(1).isCreate(), is(true));
        assertThat(field1.getAccessControlLists().get(1).isRead(), is(true));
        assertThat(field1.getAccessControlLists().get(1).isUpdate(), is(true));
        assertThat(field1.getAccessControlLists().get(1).isDelete(), is(false));
    }
}
