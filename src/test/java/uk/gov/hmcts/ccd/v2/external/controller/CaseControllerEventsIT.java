package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mock;

import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;

import org.springframework.http.MediaType;

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

import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;

import uk.gov.hmcts.ccd.domain.service.getcasedocument.GetCaseDocumentsOperationTest;
import uk.gov.hmcts.ccd.v2.V2;

import uk.gov.hmcts.ccd.v2.external.resource.CaseEventsResource;

import uk.gov.hmcts.ccd.v3.V3;

import java.io.IOException;

import java.io.InputStream;

import java.util.HashMap;

import java.util.Map;

import static org.junit.Assert.assertEquals;

import static org.junit.Assert.assertNotNull;

import static org.mockito.Mockito.doReturn;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;


public class CaseControllerEventsIT extends WireMockBaseTest {
    private static final String GET_CASE_EVENTS = "/cases/1504259907353529/events";
    private static final String UID_NO_EVENT_ACCESS = "1234";
    private static final String UID_WITH_EVENT_ACCESS = "2345";


    private static final String JURISDICTION = "PROBATE";
    private static final String TEST_EVENT_ID = "HAS_PRE_STATES_EVENT";
    private static final String UID = "123";
    private static final String CASE_TYPE_CREATOR_ROLE = "TestAddressBookCreatorCase";
    private  CaseDataContent caseDetailsToUpdate;


    private static final int NUMBER_OF_CASES = 1;

    @Inject
    private WebApplicationContext wac;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;


    private MockMvc mockMvc;

    private JdbcTemplate template;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);
        MockUtils.setSecurityAuthorities(UID_NO_EVENT_ACCESS, authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        template = new JdbcTemplate(db);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_event_access_case_roles.sql"})
    public void shouldNotReturnEventHistoryDataForCitizenWhoHasNoAccessToEvents() throws Exception {

        assertCaseDataResultSetSize();

        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, "Bearer " + UID_NO_EVENT_ACCESS);
        headers.add(V2.EXPERIMENTAL_HEADER, "true");

        final MvcResult result = mockMvc.perform(get(GET_CASE_EVENTS)
            .contentType(JSON_CONTENT_TYPE)
            .headers(headers))
            .andExpect(status().is(200))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseEventsResource saveCaseEventsResource = mapper.readValue(content, CaseEventsResource.class);
        assertNotNull("Saved Case Details should not be null", saveCaseEventsResource);
        assertEquals("Should not contain events with case role access", 1, saveCaseEventsResource.getAuditEvents().size());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_event_access_case_roles.sql"})
    public void shouldReturnEventHistoryDataForCitizenWhoHasCaseRoleAccess() throws Exception {

        assertCaseDataResultSetSize();
        MockUtils.setSecurityAuthorities(UID_WITH_EVENT_ACCESS, authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, "Bearer " + UID_WITH_EVENT_ACCESS);
        headers.add(V2.EXPERIMENTAL_HEADER, "true");

        final MvcResult result = mockMvc.perform(get(GET_CASE_EVENTS)
            .contentType(JSON_CONTENT_TYPE)
            .headers(headers))
            .andExpect(status().is(200))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseEventsResource savedCaseEventsResource = mapper.readValue(content, CaseEventsResource.class);
        assertNotNull("Saved Case Details should not be null", savedCaseEventsResource);
        assertEquals("Should contain events with case role access", 2, savedCaseEventsResource.getAuditEvents().size());

    }

    private void assertCaseDataResultSetSize() {
        final int count = template.queryForObject("SELECT count(1) as n FROM case_data", Integer.class);
        assertEquals("Incorrect case data size", NUMBER_OF_CASES, count);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_event_access_case_roles.sql"})
    public void shouldReturn201WhenPostCreateEventV3() throws Exception {
        prepareInputForV3();

        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, "Bearer " + UID_WITH_EVENT_ACCESS);
        headers.add(V3.EXPERIMENTAL_HEADER, "true");
        final Map<String, String> parameterMap = new HashMap<>(4);
        parameterMap.put("charset", "UTF-8");

        MediaType JSON_CONTENT_V3_CREATE_EVENT = new MediaType(
            "application",
            "vnd.uk.gov.hmcts.ccd-data-store-api.create-event.v3+json",
            parameterMap);
        final MvcResult mvcResult = mockMvc.perform(post(GET_CASE_EVENTS)
            .headers(headers)
            .contentType(JSON_CONTENT_V3_CREATE_EVENT)
            .content(mapper.writeValueAsString(caseDetailsToUpdate))
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 201, mvcResult.getResponse().getStatus());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseEventsResource savedCaseEventsResource = mapper.readValue(content, CaseEventsResource.class);
        assertNotNull("Update Case Details should not be null", savedCaseEventsResource);
    }

    private void prepareInputForV3() throws IOException {
        final String DESCRIPTION = "A very long comment.......";
        final String SUMMARY = "Short comment";
        Map<String, JsonNode> data = buildCaseDetailData("case-update-payload-it.json");
        caseDetailsToUpdate = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(DESCRIPTION);
        triggeringEvent.setSummary(SUMMARY);
        caseDetailsToUpdate.setEvent(triggeringEvent);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE_CREATOR_ROLE, TEST_EVENT_ID);
        caseDetailsToUpdate.setToken(token);
        caseDetailsToUpdate.setData(data);
    }

    static HashMap<String, JsonNode> buildCaseDetailData(String fileName) throws IOException {
        InputStream inputStream =
            GetCaseDocumentsOperationTest.class.getClassLoader().getResourceAsStream("tests/".concat(fileName));
        return
            new ObjectMapper().readValue(inputStream, new TypeReference<HashMap<String, JsonNode>>() {
            });
    }
}
