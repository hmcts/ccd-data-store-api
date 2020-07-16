package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
import uk.gov.hmcts.ccd.domain.types.CollectionValidator;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.CaseResource;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseUpdateViewEventResource;

import javax.inject.Inject;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.*;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class CaseControllerDCPIT extends WireMockBaseTest {
    private static final String CREATE_CASE = "/case-types/DCP/cases";
    private static final String SUBMIT_EVENT = "/cases/1587051668000989/events";
    private static final String CREATE_CASE_START_TRIGGER = "/internal/case-types/DCP/event-triggers/CREATE";
    private static final String UPDATE_EVENT_START_TRIGGER = "/internal/cases/1587051668000989/event-triggers/UPDATE";

    private static final int NUMBER_OF_CASES = 2;

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

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_DCP_CASEWORKER);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        template = new JdbcTemplate(db);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    public void shouldCreateCaseWithFormattedDCPValues() throws Exception {
        final MvcResult result = mockMvc.perform(post(CREATE_CASE)
            .content(createCaseRequestContent(eventToken(CREATE_CASE_START_TRIGGER)))
            .contentType(MediaType.APPLICATION_JSON)
            .header(AUTHORIZATION, "Bearer user1")
            .header(V2.EXPERIMENTAL_HEADER, "true"))
            .andExpect(status().is(201))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 201, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        CaseResource caseResource = mapper.readValue(content, CaseResource.class);

        Map<String, JsonNode> data = caseResource.getData();

        assertAll(
            () -> assertThat(data.get(TEXT_FIELD).asText(), is("Case 1 Text")),
            () -> assertThat(data.get(DATE_FIELD).asText(), is("2003-01-01")),
            () -> assertThat(data.get(DATE_TIME_FIELD).asText(), is("1970-01-01T07:08:00.000")),
            () -> assertThat(data.get(COLLECTION_FIELD).at(jsonPath("0", CollectionValidator.VALUE)).asText(),
                is("2017-12-16T18:19:20.000")),
            () -> assertThat(data.get(COLLECTION_FIELD).at(jsonPath("1", CollectionValidator.VALUE)).asText(),
                is("2020-12-21T22:23:24.000")),
            () -> assertThat(data.get(COMPLEX_FIELD).at(jsonPath(COMPLEX_DATE_TIME_FIELD)).asText(),
                is("2004-07-01T00:00:00.000")),
            () -> assertThat(data.get(COMPLEX_FIELD).at(jsonPath(COMPLEX_NESTED_FIELD, NESTED_NUMBER_FIELD)).asText(),
                is("1970-01-01T12:34:00.000"))
        );
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = { "classpath:sql/insert_case_dcp.sql" })
    public void shouldCreateEventWithFormattedDCPValues() throws Exception {
        assertCaseDataResultSetSize();

        final MvcResult result = mockMvc.perform(post(SUBMIT_EVENT)
            .content(updateEventRequestContent(eventToken(UPDATE_EVENT_START_TRIGGER)))
            .contentType(MediaType.APPLICATION_JSON)
            .header(AUTHORIZATION, "Bearer user1")
            .header(V2.EXPERIMENTAL_HEADER, "true"))
            .andExpect(status().is(201))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 201, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        CaseResource caseResource = mapper.readValue(content, CaseResource.class);

        Map<String, JsonNode> data = caseResource.getData();

        assertAll(
            () -> assertThat(data.get(TEXT_FIELD).asText(), is("Case 1 Text")),
            () -> assertThat(data.get(DATE_FIELD).asText(), is("2000-10-20")),
            () -> assertThat(data.get(DATE_TIME_FIELD).asText(), is("1987-01-15T00:00:00.000")),
            () -> assertThat(data.get(COLLECTION_FIELD).at(jsonPath("0", CollectionValidator.VALUE)).asText(),
                is("2004-01-01T05:06:00.000")),
            () -> assertThat(data.get(COLLECTION_FIELD).at(jsonPath("1", CollectionValidator.VALUE)).asText(),
                is("2010-01-01T11:12:00.000")),
            () -> assertThat(data.get(COMPLEX_FIELD).at(jsonPath(COMPLEX_DATE_TIME_FIELD)).asText(),
                is("2005-03-01T00:00:00.000")),
            () -> assertThat(data.get(COMPLEX_FIELD).at(jsonPath(COMPLEX_NESTED_FIELD, NESTED_NUMBER_FIELD)).asText(),
                is("1970-01-01T17:30:00.000")),
            () -> assertThat(data.get(COLLECTION_COMPLEX_DATE_TIME)
                    .at(jsonPath("0", CollectionValidator.VALUE, DATE_FIELD)).asText(),
                is("1963-05-07")),
            () -> assertThat(data.get(COLLECTION_COMPLEX_DATE_TIME)
                    .at(jsonPath("0", CollectionValidator.VALUE, DATE_TIME_FIELD)).asText(),
                is("2008-04-02T16:37:00.000")),
            () -> assertThat(data.get(COLLECTION_COMPLEX_DATE_TIME)
                    .at(jsonPath("0", CollectionValidator.VALUE, STANDARD_DATE)).asText(),
                is("1999-08-19")),
            () -> assertThat(data.get(COLLECTION_COMPLEX_DATE_TIME)
                    .at(jsonPath("0", CollectionValidator.VALUE, STANDARD_DATE_TIME)).asText(),
                is("2010-06-17T19:20:00.000")),
            () -> assertThat(data.get(COLLECTION_COMPLEX_DATE_TIME)
                    .at(jsonPath("0", CollectionValidator.VALUE, NESTED_COMPLEX, DATE_FIELD)).asText(),
                is("1981-02-01")),
            () -> assertThat(data.get(COLLECTION_COMPLEX_DATE_TIME)
                    .at(jsonPath("0", CollectionValidator.VALUE, NESTED_COMPLEX, DATE_TIME_FIELD)).asText(),
                is("2002-03-04T00:00:00.000")),
            () -> assertThat(data.get(COLLECTION_COMPLEX_DATE_TIME)
                    .at(jsonPath("0", CollectionValidator.VALUE, NESTED_COMPLEX, STANDARD_DATE)).asText(),
                is("2020-02-19")),
            () -> assertThat(data.get(COLLECTION_COMPLEX_DATE_TIME)
                    .at(jsonPath("0", CollectionValidator.VALUE, NESTED_COMPLEX, STANDARD_DATE_TIME)).asText(),
                is("2007-07-17T07:07:00.000"))
        );
    }

    private String eventToken(String triggerUrl) throws Exception {
        MvcResult startTriggerResult = mockMvc.perform(get(triggerUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .header(AUTHORIZATION, "Bearer user1")
            .header(V2.EXPERIMENTAL_HEADER, "true"))
            .andExpect(status().is(200))
            .andReturn();

        String startTriggerContent = startTriggerResult.getResponse().getContentAsString();
        CaseUpdateViewEventResource caseUpdateViewEventResource = mapper.readValue(startTriggerContent, CaseUpdateViewEventResource.class);
        return caseUpdateViewEventResource.getCaseUpdateViewEvent().getEventToken();
    }

    private void assertCaseDataResultSetSize() {
        final int count = template.queryForObject("SELECT count(1) as n FROM case_data", Integer.class);
        assertEquals("Incorrect case data size", NUMBER_OF_CASES, count);
    }
}
