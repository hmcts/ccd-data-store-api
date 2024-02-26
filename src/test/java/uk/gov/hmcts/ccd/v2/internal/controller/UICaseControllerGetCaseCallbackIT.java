package uk.gov.hmcts.ccd.v2.internal.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.TestFixtures;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewActionableEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseViewResource;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// https://tools.hmcts.net/jira/browse/RDM-12853
public class UICaseControllerGetCaseCallbackIT extends WireMockBaseTest {
    private static final String GET_CASE = "/internal/cases/1504259907353529";
    private static final String GET_CASE_CALLBACK = "/callback/getcase";
    private static final String UID_WITH_EVENT_ACCESS = "123";
    private static final String TEST_CASE_TYPE = "TestAddressBookCase";

    @Inject
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    HttpHeaders headers = new HttpHeaders();

    @Before
    public void setUp() throws Exception {
        MockUtils.setSecurityAuthorities(RandomStringUtils.randomAlphanumeric(10), authentication,
            MockUtils.ROLE_CASEWORKER_PUBLIC);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        WireMock.resetAllRequests();

        headers.add(AUTHORIZATION, "Bearer " + UID_WITH_EVENT_ACCESS);
        headers.add(V2.EXPERIMENTAL_HEADER, "true");

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final String jsonString = TestFixtures
            .fromFileAsString("__files/test-addressbook-get-case-callback.json")
            .replace("${GET_CASE_CALLBACK_URL}", hostUrl + GET_CASE_CALLBACK);

        stubFor(WireMock.get(urlMatching("/api/data/case-type/" + TEST_CASE_TYPE))
            .willReturn(okJson(jsonString).withStatus(200)));
    }

    // AC-1: Valid CallbackGetCaseUrl configured in CaseType tab and callback invoked successfully
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = { "classpath:sql/insert_cases.sql" })
    public void shouldReturn200MetadataFieldsContainingGetCaseCallbackResponse() throws Exception {

        stubFor(post(urlMatching(GET_CASE_CALLBACK + ".*"))
            .willReturn(okJson(getCaseCallbackJsonResponse("callbackMetadataFieldA"))));

        final MvcResult result = mockMvc.perform(get(GET_CASE)
            .contentType(JSON_CONTENT_TYPE)
            .headers(headers))
            .andExpect(status().is(200))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());

        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseViewResource savedCaseResource = mapper.readValue(content, CaseViewResource.class);
        assertNotNull("Saved Case Details should not be null", savedCaseResource);
        assertEquals("Should contain events with case role access", 5, savedCaseResource.getMetadataFields().size());
        List<String> metadataIds = savedCaseResource.getMetadataFields().stream()
            .map(CaseViewField::getId)
            .collect(Collectors.toList());
        assertTrue(metadataIds.contains("[CREATED_DATE]"));
        assertTrue(metadataIds.contains("[JURISDICTION]"));
        assertTrue(metadataIds.contains("[ACCESS_GRANTED]"));
        assertTrue(metadataIds.contains("[ACCESS_PROCESS]"));
        assertTrue(metadataIds.contains("callbackMetadataFieldA"));
    }

    // AC-2: Invalid CallbackGetCaseUrl configured in CaseType tab and callback invoked but failed after retries.
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = { "classpath:sql/insert_cases.sql" })
    public void shouldReturn504GatewayTimeoutWhenFailedAfterGetCaseCallbackRetries() throws Exception {

        stubFor(post(urlMatching(GET_CASE_CALLBACK + ".*"))
            .inScenario("CallbackRetry")
            .willReturn(aResponse().withStatus(500).withFixedDelay(101))
            .willSetStateTo("FirstFailedAttempt"));
        stubFor(post(urlMatching(GET_CASE_CALLBACK + ".*"))
            .inScenario("CallbackRetry")
            .whenScenarioStateIs("FirstFailedAttempt")
            .willReturn(aResponse().withStatus(500).withFixedDelay(192))
            .willSetStateTo("SecondFailedAttempt"));
        stubFor(post(urlMatching(GET_CASE_CALLBACK + ".*"))
            .inScenario("CallbackRetry")
            .whenScenarioStateIs("SecondFailedAttempt")
            .willReturn(aResponse().withStatus(500).withFixedDelay(490)));

        mockMvc.perform(get(GET_CASE)
                .contentType(JSON_CONTENT_TYPE)
                .headers(headers))
            .andExpect(status().isBadGateway())
            .andReturn();
    }

    // AC-3: Valid CallbackGetCaseUrl configured in CaseType tab and callback invoked but received error response.
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = { "classpath:sql/insert_cases.sql" })
    public void shouldReturnBadGatewayWhenReceivedErrorResponseFromCallback() throws Exception {

        stubFor(post(urlMatching(GET_CASE_CALLBACK + ".*"))
            .willReturn(aResponse().withStatus(500)));

        mockMvc.perform(get(GET_CASE)
                .contentType(JSON_CONTENT_TYPE)
                .headers(headers))
            .andExpect(status().isBadGateway())
            .andReturn();
    }

    // AC-4: Valid CallbackGetCaseUrl configured in CaseType tab and callback invoked but received invalid response
    //       (other than the expected metadata items).
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = { "classpath:sql/insert_cases.sql" })
    public void shouldReturnBadGatewayWhenGetCaseCallbackResponseReturns200ButInvalidResponseBody() throws Exception {

        stubFor(post(urlMatching(GET_CASE_CALLBACK + ".*"))
            .willReturn(okJson(getCaseCallbackJsonInvalidResponse())));

        mockMvc.perform(get(GET_CASE)
                .contentType(JSON_CONTENT_TYPE)
                .headers(headers))
            .andExpect(status().isBadGateway())
            .andReturn();
    }

    // AC-5: Valid CallbackGetCaseUrl configured in CaseType tab and callback invoked but received meta-data item
    //       with ID matching an existing meta-data item from the case_details
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = { "classpath:sql/insert_cases.sql" })
    public void shouldReturnBadGatewayWhenGetCaseCallbackResponseContainsMetadataFieldWithAnExistingId()
        throws Exception {

        stubFor(post(urlMatching(GET_CASE_CALLBACK + ".*"))
            .willReturn(okJson(getCaseCallbackJsonResponse("[CREATED_DATE]"))));

        mockMvc.perform(get(GET_CASE)
                .contentType(JSON_CONTENT_TYPE)
                .headers(headers))
            .andExpect(status().isBadGateway())
            .andReturn();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = { "classpath:sql/insert_cases.sql" })
    public void shouldReturn200WithTriggerWhenInjectedDataMetadataFieldsMatchGetCaseCallbackResponse()
        throws Exception {

        WireMock.resetAllRequests();

        final String jsonString = TestFixtures
            .fromFileAsString("__files/test-addressbook-get-case-callback_injected_data.json")
            .replace("${GET_CASE_CALLBACK_URL}", hostUrl + GET_CASE_CALLBACK);

        stubFor(WireMock.get(urlMatching("/api/data/case-type/" + TEST_CASE_TYPE))
            .willReturn(okJson(jsonString).withStatus(200)));

        stubFor(post(urlMatching(GET_CASE_CALLBACK + ".*"))
            .willReturn(okJson(getCaseCallbackJsonResponse("[INJECTED_DATA.myVar]"))));

        final MvcResult result = mockMvc.perform(get(GET_CASE)
            .contentType(JSON_CONTENT_TYPE)
            .headers(headers))
            .andExpect(status().is(200))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());

        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseViewResource savedCaseResource = mapper.readValue(content, CaseViewResource.class);
        assertNotNull("Saved Case Details should not be null", savedCaseResource);
        assertEquals("Should contain events with case role access", 5, savedCaseResource.getMetadataFields().size());
        List<String> metadataIds = savedCaseResource.getMetadataFields().stream()
            .map(CaseViewField::getId)
            .collect(Collectors.toList());
        assertTrue(metadataIds.contains("[CREATED_DATE]"));
        assertTrue(metadataIds.contains("[JURISDICTION]"));
        assertTrue(metadataIds.contains("[ACCESS_GRANTED]"));
        assertTrue(metadataIds.contains("[ACCESS_PROCESS]"));
        assertTrue(metadataIds.contains("[INJECTED_DATA.myVar]"));

        final List<CaseViewActionableEvent> caseViewActionableEvents =
            List.of(savedCaseResource.getCaseViewActionableEvents());
        assertEquals(2, caseViewActionableEvents.size());
        assertTrue(caseViewActionableEvents.stream()
            .anyMatch(caseViewActionableEvent -> caseViewActionableEvent.getId().equals("START_PROGRESS")));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = { "classpath:sql/insert_cases.sql" })
    public void shouldReturn200WithoutTriggerWhenInjectedDataMetadataFieldsMatchGetCaseCallbackResponse()
        throws Exception {

        WireMock.resetAllRequests();

        final String jsonString = TestFixtures
            .fromFileAsString("__files/test-addressbook-get-case-callback_injected_data.json")
            .replace("${GET_CASE_CALLBACK_URL}", hostUrl + GET_CASE_CALLBACK);

        stubFor(WireMock.get(urlMatching("/api/data/case-type/" + TEST_CASE_TYPE))
            .willReturn(okJson(jsonString).withStatus(200)));

        stubFor(post(urlMatching(GET_CASE_CALLBACK + ".*"))
            .willReturn(okJson(getCaseCallbackJsonResponseWithValue("[INJECTED_DATA.myVar]", "noMatchValue"))));

        final MvcResult result = mockMvc.perform(get(GET_CASE)
            .contentType(JSON_CONTENT_TYPE)
            .headers(headers))
            .andExpect(status().is(200))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());

        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseViewResource savedCaseResource = mapper.readValue(content, CaseViewResource.class);
        assertNotNull("Saved Case Details should not be null", savedCaseResource);
        assertEquals("Should contain events with case role access", 5, savedCaseResource.getMetadataFields().size());
        List<String> metadataIds = savedCaseResource.getMetadataFields().stream()
            .map(CaseViewField::getId)
            .collect(Collectors.toList());
        assertTrue(metadataIds.contains("[CREATED_DATE]"));
        assertTrue(metadataIds.contains("[JURISDICTION]"));
        assertTrue(metadataIds.contains("[ACCESS_GRANTED]"));
        assertTrue(metadataIds.contains("[ACCESS_PROCESS]"));
        assertTrue(metadataIds.contains("[INJECTED_DATA.myVar]"));

        final List<CaseViewActionableEvent> caseViewActionableEvents =
            List.of(savedCaseResource.getCaseViewActionableEvents());
        assertEquals(1, caseViewActionableEvents.size());
        assertFalse(caseViewActionableEvents.stream()
            .anyMatch(caseViewActionableEvent -> caseViewActionableEvent.getId().equals("START_PROGRESS")));
    }

    private String getCaseCallbackJsonResponse(String metadataFieldId) {
        return getCaseCallbackJsonResponseWithValue(metadataFieldId, "TODO");
    }

    private String getCaseCallbackJsonResponseWithValue(String metadataFieldId, String metadataFieldValue) {
        return "{\n"
            + "      \"metadataFields\": [\n"
            + "        {\n"
            + "          \"id\": \"" + metadataFieldId + "\",\n"
            + "          \"label\": \"fieldA\",\n"
            + "          \"hidden\": false,\n"
            + "          \"value\": \"" + metadataFieldValue + "\",\n"
            + "          \"metadata\": true,\n"
            + "          \"hint_text\": null,\n"
            + "          \"field_type\" : {\n"
            + "            \"id\" : \"Text\",\n"
            + "            \"type\" : \"Text\",\n"
            + "            \"min\" : null,\n"
            + "            \"max\" : null,\n"
            + "            \"regular_expression\" : null,\n"
            + "            \"fixed_list_items\" : [ ],\n"
            + "            \"complex_fields\" : [ ],\n"
            + "            \"collection_field_type\" : null\n"
            + "          },\n"
            + "          \"validation_expr\": null,\n"
            + "          \"security_label\": \"PUBLIC\",\n"
            + "          \"order\": null,\n"
            + "          \"display_context\": null,\n"
            + "          \"display_context_parameter\": null,\n"
            + "          \"show_condition\": null,\n"
            + "          \"show_summary_change_option\": null,\n"
            + "          \"show_summary_content_option\": null,\n"
            + "          \"acls\": []\n"
            + "        }\n"
            + "      ]\n"
            + "    }\n"
            + "  }";
    }

    private String getCaseCallbackJsonInvalidResponse() {
        return "{\n"
            + "      \"metadataFields\": [\n"
            + "        {\n"
            + "          \"id\": \"FieldX\",\n"
            + "          \"invalidNode\": \"invalid\",\n"
            + "        }\n"
            + "      ]\n"
            + "    }\n"
            + "  }";
    }
}
