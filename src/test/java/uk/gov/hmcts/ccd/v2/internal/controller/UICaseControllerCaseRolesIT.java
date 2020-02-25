package uk.gov.hmcts.ccd.v2.internal.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.collect.Lists;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.internal.resource.UICaseViewResource;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import javax.inject.Inject;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UICaseControllerCaseRolesIT extends WireMockBaseTest {
    private static final String GET_CASE = "/internal/cases/1504259907353529";
    private static final String UID_NO_EVENT_ACCESS = "1234";
    private static final String UID_WITH_EVENT_ACCESS = "2345";

    private static final int NUMBER_OF_CASES = 1;

    @Inject
    private WebApplicationContext wac;
    @Inject
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private JdbcTemplate template;

    @Before
    public void setUp() {
        MockUtils.setSecurityAuthorities(RandomStringUtils.randomAlphanumeric(10), authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        template = new JdbcTemplate(db);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = { "classpath:sql/insert_cases_event_access_case_roles.sql" })
    public void shouldNotReturnEventHistoryDataForCitizenWhoHasNoAccessToEvents() throws Exception {

        UserInfo userInfo = UserInfo.builder()
            .uid(UID_NO_EVENT_ACCESS)
            .roles(Lists.newArrayList(MockUtils.ROLE_CASEWORKER_PUBLIC))
            .build();
        stubFor(WireMock.get(urlMatching("/o/userinfo"))
            .willReturn(okJson(objectMapper.writeValueAsString(userInfo)).withStatus(200)));


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
        UICaseViewResource savedCaseResource = mapper.readValue(content, UICaseViewResource.class);
        assertNotNull("Saved Case Details should not be null", savedCaseResource);
        assertEquals("Should not contain events with case role access", 1, savedCaseResource.getEvents().length);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = { "classpath:sql/insert_cases_event_access_case_roles.sql" })
    public void shouldReturnEventHistoryDataForCitizenWhoHasCaseRoleAccess() throws Exception {

        assertCaseDataResultSetSize();
        MockUtils.setSecurityAuthorities(UID_WITH_EVENT_ACCESS, authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        UserInfo userInfo = UserInfo.builder()
            .uid(UID_WITH_EVENT_ACCESS)
            .roles(Lists.newArrayList(MockUtils.ROLE_CASEWORKER_PUBLIC))
            .build();
        stubFor(WireMock.get(urlMatching("/o/userinfo"))
            .willReturn(okJson(objectMapper.writeValueAsString(userInfo)).withStatus(200)));

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
        UICaseViewResource savedCaseResource = mapper.readValue(content, UICaseViewResource.class);
        assertNotNull("Saved Case Details should not be null", savedCaseResource);
        assertEquals("Should contain events with case role access", 2, savedCaseResource.getEvents().length);

    }

    private void assertCaseDataResultSetSize() {
        final int count = template.queryForObject("SELECT count(1) as n FROM case_data",Integer.class);
        assertEquals("Incorrect case data size", NUMBER_OF_CASES, count);
    }
}
