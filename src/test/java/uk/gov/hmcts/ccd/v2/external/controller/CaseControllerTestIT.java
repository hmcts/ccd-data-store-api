package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.SpyBean;
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
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.AuditRepository;
import uk.gov.hmcts.ccd.auditlog.OperationType;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.v2.external.resource.CaseResource;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;
import static uk.gov.hmcts.ccd.v2.V2.EXPERIMENTAL_HEADER;

public class CaseControllerTestIT extends WireMockBaseTest {

    private static final String CASE_TYPE = "TestAddressBookCase";
    private static final String JURISDICTION = "PROBATE";
    private static final String TEST_EVENT_ID = "TEST_EVENT";
    private static final String UID = "123";
    private static final String CASE_TYPE_CREATOR_ROLE = "TestAddressBookCreatorCase";
    private static final String CASE_TYPE_CREATOR_ROLE_NO_CREATE_ACCESS = "TestAddressBookCreatorNoCreateAccessCase";

    @Inject
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @SpyBean
    private AuditRepository auditRepository;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldLogAuditInfoForGetCaseById() throws Exception {
        String caseId = "1504259907353529";
        final String URL =  "/cases/" + caseId;

        final MvcResult mvcResult = mockMvc.perform(get(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .contentType(JSON_CONTENT_TYPE)
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 200, mvcResult.getResponse().getStatus());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);

        assertThat(savedCaseResource.getReference(), is(caseId));

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(OperationType.VIEW_CASE.getLabel()));
        assertThat(captor.getValue().getCaseId(), is("1504259907353529"));
        assertThat(captor.getValue().getCaseType(), is("TestAddressBookCase"));
        assertThat(captor.getValue().getJurisdiction(), is("PROBATE"));
    }

    @Test
    public void shouldReturn201WhenPostCreateCase() throws Exception {
        final String URL =  "/case-types/" + CASE_TYPE + "/cases";
        final String DESCRIPTION = "A very long comment.......";
        final String SUMMARY = "Short comment";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(DESCRIPTION);
        triggeringEvent.setSummary(SUMMARY);
        caseDetailsToSave.setEvent(triggeringEvent);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 201, mvcResult.getResponse().getStatus());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull("Saved Case Details should not be null", savedCaseResource);

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(OperationType.CREATE_CASE.getLabel()));
        assertThat(captor.getValue().getCaseId(), is(savedCaseResource.getReference()));
        assertThat(captor.getValue().getCaseType(), is(CASE_TYPE));
        assertThat(captor.getValue().getJurisdiction(), is(JURISDICTION));
        assertThat(captor.getValue().getEventSelected(), is(TEST_EVENT_ID));
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseWithCreatorRoleWithNoDataForCaseworker() throws Exception {
        final String DESCRIPTION = "A very long comment.......";
        final String SUMMARY = "Short comment";

        final String URL =  "/case-types/" + CASE_TYPE_CREATOR_ROLE + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(DESCRIPTION);
        triggeringEvent.setSummary(SUMMARY);
        caseDetailsToSave.setEvent(triggeringEvent);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE_CREATOR_ROLE, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);


        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 201, mvcResult.getResponse().getStatus());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull("Saved Case Details should not be null", savedCaseResource);
    }

    @Test
    public void shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccessOnCreatorRole() throws Exception {
        final String URL =  "/case-types/" + CASE_TYPE_CREATOR_ROLE_NO_CREATE_ACCESS + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();

        mockMvc.perform(post(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldLogAuditInfoForUpdateCaseById() throws Exception {
        String caseId = "1504259907353529";
        final String URL =  "/cases/" + caseId + "/events";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        String eventId = "Goodness";
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(eventId);
        triggeringEvent.setSummary("Short comment");
        caseDetailsToSave.setEvent(triggeringEvent);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, eventId);
        caseDetailsToSave.setToken(token);

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 201, mvcResult.getResponse().getStatus());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull("Saved Case Details should not be null", savedCaseResource);

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(OperationType.CREATE_CASE.getLabel()));
        assertThat(captor.getValue().getCaseId(), is(savedCaseResource.getReference()));
        assertThat(captor.getValue().getCaseType(), is(CASE_TYPE));
        assertThat(captor.getValue().getJurisdiction(), is(JURISDICTION));
        assertThat(captor.getValue().getEventSelected(), is(eventId));
    }

}
