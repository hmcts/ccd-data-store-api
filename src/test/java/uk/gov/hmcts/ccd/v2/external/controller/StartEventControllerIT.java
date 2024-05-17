package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.customheaders.CustomHeadersFilter;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.StartEventResource;

import javax.inject.Inject;

import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class StartEventControllerIT extends WireMockBaseTest {

    private static final JSONObject responseJson1 = new JSONObject("""
        {
            "user_task": {
                "task_data": {
                    "task_id": "000001",
                    "task_name": "Task 1 name"
                },
                "complete_task": "false"
            }
        }
        """);
    private static final JSONObject responseJson2 = new JSONObject("""
        {
            "user_task": {
                "task_data": {
                    "task_id": "000002",
                    "task_name": "Task 2 name"
                },
                "complete_task": "false"
            }
        }
        """);

    @Inject
    private WebApplicationContext wac;
    @Inject
    private CustomHeadersFilter customHeadersFilter;

    private MockMvc mockMvc;

    private final JSONObject jsonObject = new JSONObject("""
        {
            json:anyData
        }
        """);

    @Before
    public void setUp() {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).addFilters(customHeadersFilter).build();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    public void shouldReturnCustomHeader() throws Exception {
        final String customContext = applicationParams.getCallbackPassthruHeaderContexts().get(0);

        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, "Bearer user1");
        headers.add(V2.EXPERIMENTAL_HEADER, "true");
        headers.add(customContext, jsonObject.toString());

        mockMvc.perform(MockMvcRequestBuilders.get(
                    "/case-types/TestAddressBookCreatorCase/event-triggers/NO_PRE_STATES_EVENT")
                .headers(headers)
            )
            .andExpect(MockMvcResultMatchers.status().isOk())
             .andExpect(MockMvcResultMatchers.header().string(customContext, jsonObject.toString()));
    }

    @Test
    public void shouldReturnCustomHeaderFromAttribute() throws Exception {
        final String customContext = applicationParams.getCallbackPassthruHeaderContexts().get(0);

        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, "Bearer user1");
        headers.add(V2.EXPERIMENTAL_HEADER, "true");
        headers.add(customContext, responseJson1.toString());

        mockMvc.perform(MockMvcRequestBuilders.get(
            "/case-types/TestAddressBookCreatorCase/event-triggers/NO_PRE_STATES_EVENT")
                .requestAttr(customContext, responseJson2)
                .headers(headers)
            )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.header().string(customContext, responseJson2.toString()));
    }

    @Test
    public void getStartCaseEvent_should_return_200() throws Exception {
        final String URL =  "/case-types/TestAddressBookCreatorCase/event-triggers/NO_PRE_STATES_EVENT";

        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, "Bearer user1");
        headers.add(V2.EXPERIMENTAL_HEADER, "true");

        final MvcResult result = mockMvc.perform(get(URL)
            .contentType(JSON_CONTENT_TYPE)
            .accept(V2.MediaType.START_CASE_EVENT)
            .headers(headers))
            .andExpect(status().is(200))
            .andReturn();

        final StartEventResource startEventResource = mapper.readValue(result.getResponse().getContentAsString(),
                StartEventResource.class);
        assertNotNull("UI Start Trigger Resource is null", startEventResource);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void getStartEventTrigger_should_return_200() throws Exception {
        String caseId = "1504259907353529";
        String triggerId = "HAS_PRE_STATES_EVENT";
        final String URL =  "/cases/" + caseId + "/event-triggers/" + triggerId;

        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, "Bearer user1");
        headers.add(V2.EXPERIMENTAL_HEADER, "true");

        final MvcResult result = mockMvc.perform(get(URL)
            .contentType(JSON_CONTENT_TYPE)
            .accept(V2.MediaType.START_EVENT)
            .headers(headers))
            .andExpect(status().is(200))
            .andReturn();

        final StartEventResource startEventResource = mapper.readValue(result.getResponse().getContentAsString(),
                StartEventResource.class);
        assertNotNull("UI Start Trigger Resource is null", startEventResource);
    }
}
