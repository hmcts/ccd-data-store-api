package uk.gov.hmcts.ccd.v2.internal.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.collect.Lists;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
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
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseHistoryViewResource;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseViewResource;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import javax.inject.Inject;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UICaseControllerCaseRolesIT extends WireMockBaseTest {
    private static final String GET_CASE = "/internal/cases/1504259907353529";
    private static final String GET_EVENT = "/internal/cases/1504259907353529/events/1";
    private static final String UID_NO_EVENT_ACCESS = "1234";
    private static final String UID_WITH_EVENT_ACCESS = "123";

    private static final int NUMBER_OF_CASES = 1;

    @Inject
    private WebApplicationContext wac;

    @SpyBean
    private AuditRepository auditRepository;

    private MockMvc mockMvc;

    private JdbcTemplate template;

    @Before
    public void setUp() {
        MockUtils.setSecurityAuthorities(RandomStringUtils.randomAlphanumeric(10), authentication,
            MockUtils.ROLE_CASEWORKER_PUBLIC);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        template = new JdbcTemplate(db);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = { "classpath:sql/insert_cases_event_access_case_roles.sql" })
    public void shouldNotReturnEventHistoryDataForCitizenWhoHasNoAccessToEvents() throws Exception {

        UserInfo userInfo = UserInfo.builder()
            .uid(UID_NO_EVENT_ACCESS)
            .roles(Lists.newArrayList(MockUtils.ROLE_CASEWORKER_PUBLIC))
            .build();
        stubFor(WireMock.get(urlMatching("/o/userinfo"))
            .willReturn(okJson(mapper.writeValueAsString(userInfo)).withStatus(200)));


        assertCaseDataResultSetSize();

        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, "Bearer " + UID_NO_EVENT_ACCESS);
        headers.add(V2.EXPERIMENTAL_HEADER, "true");

        final MvcResult result = mockMvc.perform(get(GET_CASE)
            .contentType(JSON_CONTENT_TYPE)
            .headers(headers))
            .andExpect(status().is(200))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseViewResource savedCaseResource = mapper.readValue(content, CaseViewResource.class);
        assertNotNull("Saved Case Details should not be null", savedCaseResource);
        assertEquals("Should not contain events with case role access", 1,
                savedCaseResource.getCaseViewEvents().length);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = { "classpath:sql/insert_cases_event_access_case_roles.sql" })
    public void shouldReturnEventHistoryDataForCitizenWhoHasCaseRoleAccess() throws Exception {

        assertCaseDataResultSetSize();

        UserInfo userInfo = UserInfo.builder()
            .uid(UID_WITH_EVENT_ACCESS)
            .roles(Lists.newArrayList(MockUtils.ROLE_CASEWORKER_PUBLIC))
            .build();
        stubFor(WireMock.get(urlMatching("/o/userinfo"))
            .willReturn(okJson(mapper.writeValueAsString(userInfo)).withStatus(200)));

        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, "Bearer " + UID_WITH_EVENT_ACCESS);
        headers.add(V2.EXPERIMENTAL_HEADER, "true");

        final MvcResult result = mockMvc.perform(get(GET_CASE)
            .contentType(JSON_CONTENT_TYPE)
            .headers(headers))
            .andExpect(status().is(200))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseViewResource savedCaseResource = mapper.readValue(content, CaseViewResource.class);
        assertNotNull("Saved Case Details should not be null", savedCaseResource);
        assertEquals("Should contain events with case role access", 2, savedCaseResource.getCaseViewEvents().length);

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.CASE_ACCESSED.getLabel()));
        assertThat(captor.getValue().getCaseId(), is("1504259907353529"));
        assertThat(captor.getValue().getCaseType(), is("TestAddressBookNoEventAccessToCaseRole"));
        assertThat(captor.getValue().getJurisdiction(), is("PROBATE"));

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = { "classpath:sql/insert_cases_event_access_case_roles.sql" })
    public void shouldGetEventById() throws Exception {

        UserInfo userInfo = UserInfo.builder()
            .uid(UID_WITH_EVENT_ACCESS)
            .roles(Lists.newArrayList(MockUtils.ROLE_CASEWORKER_PUBLIC))
            .build();
        stubFor(WireMock.get(urlMatching("/o/userinfo"))
            .willReturn(okJson(mapper.writeValueAsString(userInfo)).withStatus(200)));

        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, "Bearer " + UID_WITH_EVENT_ACCESS);
        headers.add(V2.EXPERIMENTAL_HEADER, "true");

        final MvcResult result = mockMvc.perform(get(GET_EVENT)
            .contentType(JSON_CONTENT_TYPE)
            .headers(headers))
            .andExpect(status().is(200))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = result.getResponse().getContentAsString();
        CaseHistoryViewResource response = mapper.readValue(content, CaseHistoryViewResource.class);
        assertThat(response.getEvent().getId(), is(1L));

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.VIEW_CASE_HISTORY.getLabel()));
        assertThat(captor.getValue().getCaseId(), is("1504259907353529"));
        assertThat(captor.getValue().getEventSelected(), is("TEST_EVENT_ACCESS"));
        assertThat(captor.getValue().getCaseType(), is("TestAddressBookNoEventAccessToCaseRole"));
        assertThat(captor.getValue().getJurisdiction(), is("PROBATE"));

    }

    private void assertCaseDataResultSetSize() {
        final int count = template.queryForObject("SELECT count(1) as n FROM case_data",Integer.class);
        assertEquals("Incorrect case data size", NUMBER_OF_CASES, count);
    }
}
