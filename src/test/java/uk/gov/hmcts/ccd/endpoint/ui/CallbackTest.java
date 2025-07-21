package uk.gov.hmcts.ccd.endpoint.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.assertj.core.util.Lists;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.endpoint.CallbackTestData;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PUBLIC;

// too many legacy OperatorWrap occurrences on JSON strings so suppress until move to Java12+
@SuppressWarnings("checkstyle:OperatorWrap")
public class CallbackTest extends WireMockBaseTest {

    private static JsonNode CALLBACK_DATA = null;
    private static final String CALLBACK_DATA_JSON_STRING =
        "{\n" +
        "  \"PersonFirstName\": \"ccd-First Name\",\n" +
        "  \"PersonLastName\": \"Last Name\",\n" +
        "  \"PersonAddress\": {\n" +
        "    \"AddressLine1\": \"Address Line 11\",\n" +
        "    \"AddressLine2\": \"Address Line 12\"\n" +
        "  }\n" +
        "}\n";

    private static JsonNode CALLBACK_DATA_CLASSIFICATION = null;
    private static final String CALLBACK_DATA_CLASSIFICATION_JSON_STRING =
        "{\n" +
        "    \"PersonFirstName\": \"PUBLIC\",\n" +
        "    \"PersonLastName\": \"PUBLIC\",\n" +
        "    \"PersonAddress\": {\n" +
        "      \"classification\" : \"PUBLIC\",\n" +
        "      \"value\" : {\n" +
        "        \"AddressLine1\": \"PUBLIC\",\n" +
        "        \"AddressLine2\": \"PUBLIC\",\n" +
        "        \"AddressLine3\": \"PUBLIC\",\n" +
        "        \"Country\": \"PUBLIC\",\n" +
        "        \"Postcode\": \"PUBLIC\"\n" +
        "      }\n" +
        "    }\n" +
        "  }";

    private static JsonNode INVALID_CALLBACK_DATA = null;
    private static final String INVALID_CALLBACK_DATA_JSON_STRING =
        "{\n" +
        "  \"PersonFirstName\": \"First Name\",\n" +
        "  \"PersonLastName\": \"Last Name\",\n" +
        "  \"PersonAddress\": {\n" +
        "    \"AddressLine1\": \"Address Line 11\",\n" +
        "    \"AddressLine2\": \"Address Line 12\"\n" +
        "  }\n" +
        "}\n";

    @Inject
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    private static final String CREATE_CASE_EVENT_ID = "CREATE-CASE";
    private static final String UPDATE_EVENT_ID = "UPDATE-EVENT";
    private static final String UPDATE_EVENT_TRIGGER_ID = UPDATE_EVENT_ID;
    private static final String CREATE_CASE_EVENT_TRIGGER_ID = CREATE_CASE_EVENT_ID;
    private static final String NULL_PRE_STATES_CREATE_CASE_EVENT_TRIGGER_ID = "PRE-STATES-NULL";
    private static final String NON_MATCHING_PRE_STATES_UPDATE_CASE_EVENT_TRIGGER_ID = "UPDATE-EVENT-NON-MATCHING";
    private static final String INVALID_CREATE_CASE_EVENT_TRIGGER_ID = "INVALID_TRIGGER_ID";
    private static final String CASE_TYPE_ID = "CallbackCase";
    private static final String INVALID_CASE_TYPE_ID = "InvalidCallbackCase";
    private static final String JURISDICTION_ID = "TEST";
    private static final Integer USER_ID = 123;
    private static final Long CASE_REFERENCE = 1504259907353545L;
    private static final Long NON_EXISTENT_CASE_REFERENCE = 9999999999999995L;
    private static final Long INVALID_CASE_REFERENCE = 1504259907L;

    private static final String REFERENCE = "1504259907353529";

    public CallbackTest() throws IOException {
    }

    @BeforeEach
    void setUp() throws JsonProcessingException {

        CALLBACK_DATA = mapper.readTree(CALLBACK_DATA_JSON_STRING);
        CALLBACK_DATA_CLASSIFICATION = mapper.readTree(CALLBACK_DATA_CLASSIFICATION_JSON_STRING);
        INVALID_CALLBACK_DATA = mapper.readTree(INVALID_CALLBACK_DATA_JSON_STRING);

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_TEST_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        System.out.println(CallbackTestData.getTestDefinition(super.wiremockPort));
        stubFor(get(urlMatching("/api/data/case-type/CallbackCase"))
            .willReturn(okJson(CallbackTestData.getTestDefinition(super.wiremockPort)).withStatus(200)));
        stubFor(WireMock.get(urlMatching("/api/display/wizard-page-structure.*"))
            .willReturn(okJson(mapper.writeValueAsString(wizardStructureResponse)).withStatus(200)));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    void shouldReturn200WhenGetEventTriggerForCaseTypeWithValidCallbackData() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/event-triggers/%s",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(CALLBACK_DATA));
        callbackResponse.setDataClassification(JacksonUtils.convertValue(CALLBACK_DATA_CLASSIFICATION));
        callbackResponse.setSecurityClassification(PUBLIC);

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);

        final CaseUpdateViewEvent caseUpdateViewEvent = mapper.readValue(mvcResult.getResponse().getContentAsString(),
            CaseUpdateViewEvent.class);
        assertThat(caseUpdateViewEvent.getCaseFields())
            .extracting(CaseViewField::getId)
            .containsExactlyInAnyOrder("PersonFirstName", "PersonLastName", "PersonAddress");
        assertThat(caseUpdateViewEvent.getCaseFields()).hasSize(3);
        assertThat(caseUpdateViewEvent.getEventToken().isEmpty()).isFalse();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    void shouldReturn422WhenGetEventTriggerForCaseTypeWithCallbackDataWithErrors() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/event-triggers/%s",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setErrors(Lists.newArrayList("Error"));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertThat(422).isEqualTo(mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    void shouldReturn422WhenGetEventTriggerForCaseTypeWithCallbackDataWithWarningsAndIgnoreWarningFalse()
                                                                                                    throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/event-triggers/%s"
                + "?ignore-warning=false",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setWarnings(Lists.newArrayList("Warning"));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    void shouldReturn200WhenGetEventTriggerForCaseTypeWithCallbackDataWithWarningsAndIgnoreWarningTrue()
                                                                                                    throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/event-triggers/%s?"
                + "ignore-warning=true",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setWarnings(Lists.newArrayList("Warning"));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);

        final CaseUpdateViewEvent caseUpdateViewEvent = mapper.readValue(mvcResult.getResponse().getContentAsString(),
            CaseUpdateViewEvent.class);
        assertThat(caseUpdateViewEvent.getCaseFields())
            .extracting(CaseViewField::getId)
            .containsExactlyInAnyOrder("PersonFirstName", "PersonLastName", "PersonAddress");
        assertThat(caseUpdateViewEvent.getCaseFields()).hasSize(3);
        assertThat(caseUpdateViewEvent.getEventToken()).isNotEmpty();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    void shouldReturn422WhenGetEventTriggerForCaseTypeWithCallbackDataWithValidationErrors() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/event-triggers/%s?"
                + "ignore-warning=false",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(INVALID_CALLBACK_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    void shouldReturn422WhenGetEventTriggerForCaseTypeWithInvalidEventTriggerHavingNonEmptyPreStates()
                                                                                                    throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/event-triggers/%s",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, UPDATE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(CALLBACK_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    void shouldReturn404WhenGetEventTriggerForCaseTypeWithInvalidCaseTypeId() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/event-triggers/%s",
            USER_ID, JURISDICTION_ID, INVALID_CASE_TYPE_ID, CREATE_CASE_EVENT_TRIGGER_ID);

        stubFor(WireMock.get(urlMatching("/api/data/case-type/" + INVALID_CASE_TYPE_ID))
            .willReturn(notFound()));

        mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andExpect(status().is(404));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    void shouldReturn404WhenGetEventTriggerForCaseTypeWithInvalidEventTriggerId() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/event-triggers/%s",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, INVALID_CREATE_CASE_EVENT_TRIGGER_ID);

        mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andExpect(status().is(404));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    void shouldReturn422WhenGetEventTriggerForCaseTypeWithInvalidEventTriggerHavingNullPreStates()
                                                                                                    throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/event-triggers/%s",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, NULL_PRE_STATES_CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(CALLBACK_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    void shouldReturn200WhenGetEventTriggerForCaseWithValidCallbackData() throws Exception {
        final String URL =
                String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/event-triggers/%s",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, UPDATE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(CALLBACK_DATA));
        callbackResponse.setDataClassification(JacksonUtils.convertValue(CALLBACK_DATA_CLASSIFICATION));
        callbackResponse.setSecurityClassification(PUBLIC);

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);

        final CaseUpdateViewEvent caseUpdateViewEvent = mapper.readValue(mvcResult.getResponse().getContentAsString(),
            CaseUpdateViewEvent.class);
        assertThat(caseUpdateViewEvent.getCaseFields())
            .extracting(CaseViewField::getId)
            .containsExactlyInAnyOrder("PersonFirstName", "PersonLastName", "PersonAddress");
        assertThat(caseUpdateViewEvent.getCaseFields()).hasSize(3);
        assertThat(caseUpdateViewEvent.getEventToken()).isNotEmpty();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    void shouldReturn422WhenGetEventTriggerForCaseWithCallbackDataWithErrors() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/"
                + "event-triggers/%s",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, UPDATE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setErrors(Lists.newArrayList("Error"));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    void shouldReturn422WhenGetEventTriggerForCaseWithCallbackDataWithWarningsAndIgnoreWarningFalse()
                                                                                                    throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/"
                + "event-triggers/%s?ignore-warning=false",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, UPDATE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setWarnings(Lists.newArrayList("Warning"));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    void shouldReturn200WhenGetEventTriggerForCaseWithCallbackDataWithWarningsAndIgnoreWarningTrue()
                                                                                                    throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/"
                + "event-triggers/%s?ignore-warning=true",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, UPDATE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setWarnings(Lists.newArrayList("Warning"));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);

        final CaseUpdateViewEvent caseUpdateViewEvent = mapper.readValue(mvcResult.getResponse().getContentAsString(),
            CaseUpdateViewEvent.class);
        assertThat(caseUpdateViewEvent.getCaseFields())
            .extracting(CaseViewField::getId)
            .containsExactlyInAnyOrder("PersonFirstName", "PersonLastName", "PersonAddress");
        assertThat(caseUpdateViewEvent.getCaseFields()).hasSize(3);
        assertThat(caseUpdateViewEvent.getEventToken()).isNotEmpty();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    void shouldReturn422WhenGetEventTriggerForCaseWithCallbackDataWithValidationErrors() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/"
               + "event-triggers/%s?ignore-warning=false",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, UPDATE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(INVALID_CALLBACK_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    void shouldReturn422WhenGetEventTriggerForCaseWithInvalidEventTriggerHavingNoPreStates() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/"
                + "event-triggers/%s",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(CALLBACK_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    void shouldReturn422WhenGetEventTriggerForCaseWithInvalidEventTriggerHavingNonMatchingPreStates()
                                                                                                    throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/"
                + "event-triggers/%s",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE,
            NON_MATCHING_PRE_STATES_UPDATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(CALLBACK_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    void shouldReturn422WhenGetEventTriggerForCaseWithInvalidEventTriggerHavingNullPreStates()
                                                                                                    throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/"
                + "event-triggers/%s",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, NULL_PRE_STATES_CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(CALLBACK_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    void shouldReturn422WhenGetEventTriggerForCaseWithInvalidEventTriggerHavingEmptyPreStates()
                                                                                                    throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/"
                + "event-triggers/%s",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(CALLBACK_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    void shouldReturn404WhenGetEventTriggerForCaseWithNonExistentCaseId() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/"
                + "event-triggers/%s",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, NON_EXISTENT_CASE_REFERENCE, UPDATE_EVENT_TRIGGER_ID);

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    void shouldReturn404WhenGetEventTriggerForCaseWithInvalidCaseId() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/"
                + "event-triggers/%s",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, INVALID_CASE_REFERENCE, UPDATE_EVENT_TRIGGER_ID);

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    void shouldReturn404WhenGetEventTriggerForCaseWithInvalidEventTriggerId() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/event-triggers/%s",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, INVALID_CREATE_CASE_EVENT_TRIGGER_ID);

        mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andExpect(status().is(404));
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
