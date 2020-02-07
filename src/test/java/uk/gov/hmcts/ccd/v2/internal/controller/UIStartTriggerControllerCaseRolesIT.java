package uk.gov.hmcts.ccd.v2.internal.controller;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.internal.resource.UIStartTriggerResource;

import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UIStartTriggerControllerCaseRolesIT extends WireMockBaseTest {
    private static final String GET_EVENT_TRIGGER_FOR_CASE_TYPE_INTERNAL = "/internal/case-types/CaseRolesCase" +
        "/event-triggers/CREATE-CASE";

    @Inject
    private WebApplicationContext wac;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void internalGetStartCaseTrigger_200_shouldAddFieldsWithCREATORCaseRole() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, "Bearer user1");
        headers.add(V2.EXPERIMENTAL_HEADER, "true");

        final MvcResult result = mockMvc.perform(get(GET_EVENT_TRIGGER_FOR_CASE_TYPE_INTERNAL)
            .contentType(JSON_CONTENT_TYPE)
            .accept(V2.MediaType.UI_START_CASE_TRIGGER)
            .headers(headers))
            .andExpect(status().is(200))
            .andReturn();

        final UIStartTriggerResource uiStartTriggerResource =
            mapper.readValue(result.getResponse().getContentAsString(), UIStartTriggerResource.class);
        assertNotNull("UI Start Trigger Resource is null", uiStartTriggerResource);

        assertThat("Unexpected Case ID", uiStartTriggerResource.getCaseEventTrigger().getCaseId(), is(nullValue()));
        assertEquals("Unexpected Event Name", "CREATE-CASE", uiStartTriggerResource.getCaseEventTrigger().getName());
        assertEquals("Unexpected Event Show Event Notes", true, uiStartTriggerResource.getCaseEventTrigger().getShowEventNotes());
        assertEquals("Unexpected Event Description", "Creation event", uiStartTriggerResource.getCaseEventTrigger().getDescription());
        assertEquals("Unexpected Case Fields", 1, uiStartTriggerResource.getCaseEventTrigger().getCaseFields().size());

        final CaseViewField field1 = uiStartTriggerResource.getCaseEventTrigger().getCaseFields().get(0);
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
