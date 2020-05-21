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
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.AuditRepository;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.v2.external.resource.CaseResource;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private static final String REQUEST_ID = "request-id";
    private static final String REQUEST_ID_VALUE = "1234567898765432";

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
            .header(REQUEST_ID, REQUEST_ID_VALUE)
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

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.CASE_ACCESSED.getLabel()));
        assertThat(captor.getValue().getCaseId(), is(savedCaseResource.getReference()));
        assertThat(captor.getValue().getIdamId(), is("Cloud.Strife@test.com"));
        assertThat(captor.getValue().getInvokingService(), is("ccd-data"));
        assertThat(captor.getValue().getHttpStatus(), is(200));
        assertThat(captor.getValue().getCaseType(), is(CASE_TYPE));
        assertThat(captor.getValue().getJurisdiction(), is(JURISDICTION));
        assertThat(captor.getValue().getRequestId(), is(REQUEST_ID_VALUE));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldLogAuditInfoForCreateEventByCaseId() throws Exception {
        String caseId = "1504259907353529";
        final String URL =  "/cases/" + caseId + "/events";

        final CaseDataContent caseDetailsToSave = newCaseDataContent()
            .withCaseReference(caseId)
            .withEvent(anEvent()
                .withEventId("HAS_PRE_STATES_EVENT")
                .withSummary("Short comment")
                .build())
            .withToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, "HAS_PRE_STATES_EVENT"))
            .build();

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .header(REQUEST_ID, REQUEST_ID_VALUE)
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

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.UPDATE_CASE.getLabel()));
        assertThat(captor.getValue().getCaseId(), is(savedCaseResource.getReference()));
        assertThat(captor.getValue().getIdamId(), is("Cloud.Strife@test.com"));
        assertThat(captor.getValue().getInvokingService(), is("ccd-data"));
        assertThat(captor.getValue().getHttpStatus(), is(201));
        assertThat(captor.getValue().getCaseType(), is(CASE_TYPE));
        assertThat(captor.getValue().getJurisdiction(), is(JURISDICTION));
        assertThat(captor.getValue().getEventSelected(), is("HAS_PRE_STATES_EVENT"));
        assertThat(captor.getValue().getRequestId(), is(REQUEST_ID_VALUE));
    }

    @Test
    public void shouldReturn201WhenPostCreateCase() throws Exception {
        final String URL =  "/case-types/" + CASE_TYPE + "/cases";
        final String description = "A very long comment.......";
        final String summary = "Short comment";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(description);
        triggeringEvent.setSummary(summary);
        caseDetailsToSave.setEvent(triggeringEvent);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .header(REQUEST_ID, REQUEST_ID_VALUE)
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

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.CREATE_CASE.getLabel()));
        assertThat(captor.getValue().getCaseId(), is(savedCaseResource.getReference()));
        assertThat(captor.getValue().getIdamId(), is("Cloud.Strife@test.com"));
        assertThat(captor.getValue().getInvokingService(), is("ccd-data"));
        assertThat(captor.getValue().getHttpStatus(), is(201));
        assertThat(captor.getValue().getCaseType(), is(CASE_TYPE));
        assertThat(captor.getValue().getJurisdiction(), is(JURISDICTION));
        assertThat(captor.getValue().getEventSelected(), is(TEST_EVENT_ID));
        assertThat(captor.getValue().getRequestId(), is(REQUEST_ID_VALUE));
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseWithCreatorRoleWithNoDataForCaseworker() throws Exception {
        final String description = "A very long comment.......";
        final String summary = "Short comment";

        final String URL =  "/case-types/" + CASE_TYPE_CREATOR_ROLE + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(description);
        triggeringEvent.setSummary(summary);
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

}
