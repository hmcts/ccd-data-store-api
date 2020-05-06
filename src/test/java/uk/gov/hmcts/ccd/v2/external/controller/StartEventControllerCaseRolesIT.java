package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
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
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.v2.CaseRolesTestData;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.StartEventResource;

import javax.inject.Inject;
import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PUBLIC;

@SuppressWarnings("checkstyle:OperatorWrap") // too many legacy OperatorWrap occurrences on JSON strings so suppress until move to Java12+
public class StartEventControllerCaseRolesIT extends WireMockBaseTest {
    private static final String GET_EVENT_TRIGGER_FOR_CASE_TYPE_EXTERNAL = "/case-types/CaseRolesCase/event-triggers/CREATE-CASE";

    private static JsonNode CALLBACK_DATA = null;
    private static final String CALLBACK_DATA_JSON_STRING =
        "{\n" +
        "  \"PersonFirstName\": \"ccd-First Name\"\n" +
        "}\n";

    private static JsonNode CALLBACK_DATA_CLASSIFICATION = null;
    private static final String CALLBACK_DATA_CLASSIFICATION_JSON_STRING =
        "{\n" +
        "    \"PersonFirstName\": \"PUBLIC\"\n" +
        "}";

    @Inject
    private WebApplicationContext wac;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private MockMvc mockMvc;

    public StartEventControllerCaseRolesIT() throws IOException {
    }

    @Before
    public void setUp() throws IOException {

        CALLBACK_DATA = mapper.readTree(CALLBACK_DATA_JSON_STRING);
        CALLBACK_DATA_CLASSIFICATION = mapper.readTree(CALLBACK_DATA_CLASSIFICATION_JSON_STRING);

        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void externalGetStartCaseTrigger_200_shouldAddFieldsWithCreatorCaseRole() throws Exception {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, "Bearer user1");
        headers.add(V2.EXPERIMENTAL_HEADER, "true");

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(mapper.convertValue(CALLBACK_DATA, STRING_NODE_TYPE));
        callbackResponse.setDataClassification(mapper.convertValue(CALLBACK_DATA_CLASSIFICATION, STRING_NODE_TYPE));
        callbackResponse.setSecurityClassification(PUBLIC);

        stubFor(WireMock.get(urlMatching("/api/data/case-type/CaseRolesCase"))
            .willReturn(okJson(CaseRolesTestData.getTestDefinition(super.wiremockPort)).withStatus(200)));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult result = mockMvc.perform(get(GET_EVENT_TRIGGER_FOR_CASE_TYPE_EXTERNAL)
            .contentType(JSON_CONTENT_TYPE)
            .accept(V2.MediaType.START_CASE_EVENT)
            .headers(headers))
            .andExpect(status().is(200))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());

        final StartEventResource startEventResource = mapper.readValue(result.getResponse().getContentAsString(), StartEventResource.class);
        assertNotNull("UI Start Trigger Resource is null", startEventResource);

        assertEquals("Unexpected CaseDetails.data size", 1, startEventResource.getCaseDetails().getData().size());

        assertTrue(startEventResource.getCaseDetails().getData().containsKey("PersonFirstName"));
    }
}
