package uk.gov.hmcts.ccd.domain.service.callbacks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import jakarta.inject.Inject;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CallbackResponseBuilder.aCallbackResponse;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@TestPropertySource(properties =
    {
    "http.client.read.timeout=500"
    })
public class CallbackInvokerWireMockTest extends WireMockBaseTest {

    private static final ObjectMapper mapper = JacksonUtils.MAPPER;

    @Inject
    private CallbackInvoker callbackInvoker;

    private CallbackResponse callbackResponse;
    private CaseDetails caseDetails = new CaseDetails();
    private final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
    private final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();

    @BeforeEach
    public void setUp() throws Exception {
        // IDAM
        callbackResponse = aCallbackResponse().build();
        caseDetails = newCaseDetails().build();

        String testUrl = hostUrl + "/test-callbackGrrrr";
        caseEventDefinition.setCallBackURLAboutToStartEvent(testUrl);
        caseEventDefinition.setName("Test");
        wireMockServer.resetAll();
    }

    @Test
    public void shouldRetryOnErrorWithIgnoreWarningFalseAndDefaultRetryContext() throws Exception {

        stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .inScenario("CallbackRetry")
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500).withFixedDelay(501))
            .willSetStateTo("FirstFailedAttempt"));
        stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .inScenario("CallbackRetry")
            .whenScenarioStateIs("FirstFailedAttempt")
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500).withFixedDelay(501))
            .willSetStateTo("SecondFailedAttempt"));
        stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .inScenario("CallbackRetry")
            .whenScenarioStateIs("SecondFailedAttempt")
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200))
            .willSetStateTo("SuccessfulAttempt"));

        callbackInvoker.invokeAboutToStartCallback(caseEventDefinition, caseTypeDefinition, caseDetails, false);

        verify(exactly(3), postRequestedFor(urlMatching("/test-callbackGrrrr.*")));
    }

    @Test
    public void shouldNotRetryWhenCallbackRetriesDisabled() throws Exception {

        stubFor(post(urlMatching("/test-callbackGrrrr.*"))
            .inScenario("CallbackRetry")
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(500).withFixedDelay(501))
            .willSetStateTo("FirstFailedAttempt"));

        List<Integer> disabledRetries = Lists.newArrayList(0);
        caseEventDefinition.setRetriesTimeoutAboutToStartEvent(disabledRetries);

        CallbackException callbackException = assertThrows(CallbackException.class, () ->
            callbackInvoker.invokeAboutToStartCallback(caseEventDefinition, caseTypeDefinition, caseDetails, false));
        MatcherAssert.assertThat(callbackException.getMessage(),
            CoreMatchers.containsString("Callback to service has been unsuccessful for event Test url"));
        verify(exactly(1), postRequestedFor(urlMatching("/test-callbackGrrrr.*")));
    }

    @Nested
    @DisplayName("setSupplementaryData()")
    class SetSupplementaryData {

        @Test
        @DisplayName("should set supplementary data when present in about-to-start callback response")
        void shouldSetSupplementaryDataWhenPresentInAboutToStartCallback() throws Exception {


            String testUrl = hostUrl + "/test-about-to-start";
            caseEventDefinition.setCallBackURLAboutToStartEvent(testUrl);

            // Create callback response with supplementary_data (empty data map to avoid field validation)
            Map<String, JsonNode> callbackData = new HashMap<>();

            ObjectNode orgsNode = JsonNodeFactory.instance.objectNode();
            orgsNode.set("organisationA", IntNode.valueOf(54));
            orgsNode.set("organisationB", IntNode.valueOf(32));
            callbackData.put("orgs_assigned_users", orgsNode);

            CallbackResponse responseWithSupplementaryData = aCallbackResponse()
                .withSupplementaryData(callbackData)
                .build();

            stubFor(post(urlMatching("/test-about-to-start.*"))
                .willReturn(okJson(mapper.writeValueAsString(responseWithSupplementaryData)).withStatus(200)));

            callbackInvoker.invokeAboutToStartCallback(caseEventDefinition, caseTypeDefinition, caseDetails, false);

            assertNotNull(caseDetails.getSupplementaryData());
            assertNotNull(caseDetails.getSupplementaryData().get("orgs_assigned_users"));
            assertThat(caseDetails.getSupplementaryData().get("orgs_assigned_users")
                .get("organisationA").asInt(), is(54));
            assertThat(caseDetails.getSupplementaryData().get("orgs_assigned_users")
                .get("organisationB").asInt(), is(32));
            verify(exactly(1), postRequestedFor(urlMatching("/test-about-to-start.*")));
        }

        @Test
        @DisplayName("should not set supplementary data when not present in about-to-start callback response")
        void shouldNotSetSupplementaryDataWhenNotPresentInAboutToStartCallback() throws Exception {
            String testUrl = hostUrl + "/test-about-to-start";
            caseEventDefinition.setCallBackURLAboutToStartEvent(testUrl);

            CallbackResponse responseWithoutSupplementaryData = aCallbackResponse()
                .build();

            stubFor(post(urlMatching("/test-about-to-start.*"))
                .willReturn(okJson(mapper.writeValueAsString(responseWithoutSupplementaryData)).withStatus(200)));

            callbackInvoker.invokeAboutToStartCallback(caseEventDefinition, caseTypeDefinition, caseDetails, false);

            assertNull(caseDetails.getSupplementaryData());
            verify(exactly(1), postRequestedFor(urlMatching("/test-about-to-start.*")));
        }

        @Test
        @DisplayName("should set supplementary data when present in about-to-submit callback response")
        void shouldSetSupplementaryDataWhenPresentInAboutToSubmitCallback() throws Exception {
            String testUrl = hostUrl + "/test-about-to-submit";
            caseEventDefinition.setCallBackURLAboutToSubmitEvent(testUrl);

            CaseDetails caseDetailsBefore = newCaseDetails().build();

            // Create callback response with supplementary_data (empty data map to avoid field validation)
            Map<String, JsonNode> callbackData = new HashMap<>();

            ObjectNode orgsNode = JsonNodeFactory.instance.objectNode();
            orgsNode.set("organisationA", IntNode.valueOf(54));
            orgsNode.set("organisationB", IntNode.valueOf(32));
            callbackData.put("orgs_assigned_users", orgsNode);

            CallbackResponse responseWithSupplementaryData = aCallbackResponse()
                .withSupplementaryData(callbackData)
                .withData(new HashMap<>())
                .build();

            stubFor(post(urlMatching("/test-about-to-submit.*"))
                .willReturn(okJson(mapper.writeValueAsString(responseWithSupplementaryData)).withStatus(200)));

            callbackInvoker.invokeAboutToSubmitCallback(caseEventDefinition, caseDetailsBefore, caseDetails,
                caseTypeDefinition, false);

            assertNotNull(caseDetails.getSupplementaryData());
            assertNotNull(caseDetails.getSupplementaryData().get("orgs_assigned_users"));
            assertThat(caseDetails.getSupplementaryData().get("orgs_assigned_users")
                .get("organisationA").asInt(), is(54));
            assertThat(caseDetails.getSupplementaryData().get("orgs_assigned_users")
                .get("organisationB").asInt(), is(32));
            verify(exactly(1), postRequestedFor(urlMatching("/test-about-to-submit.*")));
        }

        @Test
        @DisplayName("should not set supplementary data when not present in about-to-submit callback response")
        void shouldNotSetSupplementaryDataWhenNotPresentInAboutToSubmitCallback() throws Exception {
            String testUrl = hostUrl + "/test-about-to-submit";
            caseEventDefinition.setCallBackURLAboutToSubmitEvent(testUrl);

            CaseDetails caseDetailsBefore = newCaseDetails().build();

            CallbackResponse responseWithoutSupplementaryData = aCallbackResponse().build();

            stubFor(post(urlMatching("/test-about-to-submit.*"))
                .willReturn(okJson(mapper.writeValueAsString(responseWithoutSupplementaryData)).withStatus(200)));

            callbackInvoker.invokeAboutToSubmitCallback(caseEventDefinition, caseDetailsBefore, caseDetails,
                caseTypeDefinition, false);

            assertNull(caseDetails.getSupplementaryData());
            verify(exactly(1), postRequestedFor(urlMatching("/test-about-to-submit.*")));
        }

        @Test
        @DisplayName("should set supplementary data when present in mid-event callback response")
        void shouldSetSupplementaryDataWhenPresentInMidEventCallback() throws Exception {
            String testUrl = hostUrl + "/test-mid-event";
            WizardPage wizardPage = new WizardPage();
            wizardPage.setCallBackURLMidEvent(testUrl);

            CaseDetails caseDetailsBefore = newCaseDetails().build();

            // Create callback response with supplementary_data (empty data map to avoid field validation)
            Map<String, JsonNode> callbackData = new HashMap<>();

            ObjectNode orgsNode = JsonNodeFactory.instance.objectNode();
            orgsNode.set("organisationA", IntNode.valueOf(54));
            orgsNode.set("organisationB", IntNode.valueOf(32));
            callbackData.put("orgs_assigned_users", orgsNode);

            CallbackResponse responseWithSupplementaryData = aCallbackResponse()
                .withSupplementaryData(callbackData)
                .build();

            stubFor(post(urlMatching("/test-mid-event.*"))
                .willReturn(okJson(mapper.writeValueAsString(responseWithSupplementaryData)).withStatus(200)));

            callbackInvoker.invokeMidEventCallback(wizardPage, caseTypeDefinition, caseEventDefinition,
                caseDetailsBefore, caseDetails, false);

            assertNotNull(caseDetails.getSupplementaryData());
            assertNotNull(caseDetails.getSupplementaryData().get("orgs_assigned_users"));
            assertThat(caseDetails.getSupplementaryData().get("orgs_assigned_users")
                .get("organisationA").asInt(), is(54));
            assertThat(caseDetails.getSupplementaryData().get("orgs_assigned_users")
                .get("organisationB").asInt(), is(32));
            verify(exactly(1), postRequestedFor(urlMatching("/test-mid-event.*")));
        }

        @Test
        @DisplayName("should not set supplementary data when not present in mid-event callback response")
        void shouldNotSetSupplementaryDataWhenNotPresentInMidEventCallback() throws Exception {
            String testUrl = hostUrl + "/test-mid-event";
            WizardPage wizardPage = new WizardPage();
            wizardPage.setCallBackURLMidEvent(testUrl);

            CaseDetails caseDetailsBefore = newCaseDetails().build();

            CallbackResponse responseWithoutSupplementaryData = aCallbackResponse().build();

            stubFor(post(urlMatching("/test-mid-event.*"))
                .willReturn(okJson(mapper.writeValueAsString(responseWithoutSupplementaryData)).withStatus(200)));

            callbackInvoker.invokeMidEventCallback(wizardPage, caseTypeDefinition, caseEventDefinition,
                caseDetailsBefore, caseDetails, false);

            assertNull(caseDetails.getSupplementaryData());
            verify(exactly(1), postRequestedFor(urlMatching("/test-mid-event.*")));
        }
    }

}
