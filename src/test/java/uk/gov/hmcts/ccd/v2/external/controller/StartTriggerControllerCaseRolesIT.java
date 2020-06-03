package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
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
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.v2.CaseRolesTestData;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.StartTriggerResource;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PUBLIC;

public class StartTriggerControllerCaseRolesIT extends WireMockBaseTest {
    private static final String GET_EVENT_TRIGGER_FOR_CASE_TYPE_EXTERNAL = "/case-types/CaseRolesCase/event-triggers" +
        "/CREATE-CASE";

    private final JsonNode CALLBACK_DATA = mapper.readTree(
        "{\n" +
            "  \"PersonFirstName\": \"ccd-First Name\"\n" +
            "}\n"
    );
    private final JsonNode CALLBACK_DATA_CLASSIFICATION = mapper.readTree(
        "{\n" +
            "    \"PersonFirstName\": \"PUBLIC\"\n" +
            "  }"
    );
    @Inject
    private WebApplicationContext wac;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private MockMvc mockMvc;

    public StartTriggerControllerCaseRolesIT() throws IOException {
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void externalGetStartCaseTrigger_200_shouldAddFieldsWithCREATORCaseRole() throws Exception {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, "Bearer user1");
        headers.add(V2.EXPERIMENTAL_HEADER, "true");

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(CALLBACK_DATA));
        callbackResponse.setDataClassification(JacksonUtils.convertValue(CALLBACK_DATA_CLASSIFICATION));
        callbackResponse.setSecurityClassification(PUBLIC);

        stubFor(WireMock.get(urlMatching("/api/data/case-type/CaseRolesCase"))
            .willReturn(okJson(CaseRolesTestData.getTestDefinition(super.wiremockPort)).withStatus(200)));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult result = mockMvc.perform(get(GET_EVENT_TRIGGER_FOR_CASE_TYPE_EXTERNAL)
            .contentType(JSON_CONTENT_TYPE)
            .accept(V2.MediaType.START_CASE_TRIGGER)
            .headers(headers))
            .andExpect(status().is(200))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());

        final StartTriggerResource startTriggerResource = mapper.readValue(result.getResponse().getContentAsString(), StartTriggerResource.class);
        assertNotNull("UI Start Trigger Resource is null", startTriggerResource);

        assertEquals("Unexpected CaseDetails.data size", 1, startTriggerResource.getCaseDetails().getData().size());

        assertTrue(startTriggerResource.getCaseDetails().getData().containsKey("PersonFirstName"));
    }

    private Matcher<Iterable<? extends CaseViewField>> hasIds(String[] expectedIds) {
        return containsInAnyOrder(Arrays.stream(expectedIds).map(this::id).collect(Collectors.toList()));
    }

    private Matcher<CaseViewField> id(String id) {
        return new FeatureMatcher<CaseViewField, String>(equalTo(id), "id", "id") {
            @Override
            protected String featureValueOf(CaseViewField actual) {
                return actual.getId();
            }
        };
    }
}
