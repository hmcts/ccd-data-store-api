package uk.gov.hmcts.ccd.endpoint.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.assertj.core.util.Lists;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;
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
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PUBLIC;

public class CallbackTest extends WireMockBaseTest {
    private final JsonNode CALLBACK_DATA = mapper.readTree(
        "{\n" +
            "  \"PersonFirstName\": \"ccd-First Name\",\n" +
            "  \"PersonLastName\": \"Last Name\",\n" +
            "  \"PersonAddress\": {\n" +
            "    \"AddressLine1\": \"Address Line 11\",\n" +
            "    \"AddressLine2\": \"Address Line 12\"\n" +
            "  }\n" +
            "}\n"
    );
    private final JsonNode CALLBACK_DATA_CLASSIFICATION = mapper.readTree(
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
            "  }"
    );

    private final JsonNode INVALID_CALLBACK_DATA = mapper.readTree(
        "{\n" +
            "  \"PersonFirstName\": \"First Name\",\n" +
            "  \"PersonLastName\": \"Last Name\",\n" +
            "  \"PersonAddress\": {\n" +
            "    \"AddressLine1\": \"Address Line 11\",\n" +
            "    \"AddressLine2\": \"Address Line 12\"\n" +
            "  }\n" +
            "}\n"
    );

    @Inject
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

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

    @Before
    public void setUp() throws JsonProcessingException {
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);
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
    public void shouldReturn200WhenGetEventTriggerForCaseTypeWithValidCallbackData() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/event-triggers/%s", USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CREATE_CASE_EVENT_TRIGGER_ID);

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

        assertEquals(mvcResult.getResponse().getContentAsString(), 200, mvcResult.getResponse().getStatus());

        final CaseEventTrigger caseEventTrigger = mapper.readValue(mvcResult.getResponse().getContentAsString(), CaseEventTrigger.class);
        assertThat(caseEventTrigger.getCaseFields(), hasIds(new String[]{"PersonFirstName", "PersonLastName", "PersonAddress"}));
        assertThat(caseEventTrigger.getCaseFields(), hasSize(3));
        assertTrue("No token", !caseEventTrigger.getEventToken().isEmpty());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenGetEventTriggerForCaseTypeWithCallbackDataWithErrors() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/event-triggers/%s", USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setErrors(Lists.newArrayList("Error"));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertEquals("Callback errors should have caused UNPROCESSABLE_ENTITY response", 422, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenGetEventTriggerForCaseTypeWithCallbackDataWithWarningsAndIgnoreWarningFalse() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/event-triggers/%s?ignore-warning=false", USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setWarnings(Lists.newArrayList("Warning"));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertEquals("Callback warnings should have caused UNPROCESSABLE_ENTITY response", 422, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn200WhenGetEventTriggerForCaseTypeWithCallbackDataWithWarningsAndIgnoreWarningTrue() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/event-triggers/%s?ignore-warning=true", USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setWarnings(Lists.newArrayList("Warning"));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 200, mvcResult.getResponse().getStatus());

        final CaseEventTrigger caseEventTrigger = mapper.readValue(mvcResult.getResponse().getContentAsString(), CaseEventTrigger.class);
        assertThat(caseEventTrigger.getCaseFields(), hasIds(new String[]{"PersonFirstName", "PersonLastName", "PersonAddress"}));
        assertThat(caseEventTrigger.getCaseFields(), hasSize(3));
        assertTrue("No token", !caseEventTrigger.getEventToken().isEmpty());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenGetEventTriggerForCaseTypeWithCallbackDataWithValidationErrors() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/event-triggers/%s?ignore-warning=false", USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(INVALID_CALLBACK_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertEquals("Invalid callback data should have caused UNPROCESSABLE_ENTITY response", 422, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenGetEventTriggerForCaseTypeWithInvalidEventTriggerHavingNonEmptyPreStates() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/event-triggers/%s", USER_ID, JURISDICTION_ID, CASE_TYPE_ID, UPDATE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(CALLBACK_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertEquals("Non empty pre states should have caused UNPROCESSABLE_ENTITY response", 422, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn404WhenGetEventTriggerForCaseTypeWithInvalidCaseTypeId() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/event-triggers/%s", USER_ID, JURISDICTION_ID, INVALID_CASE_TYPE_ID, CREATE_CASE_EVENT_TRIGGER_ID);

        stubFor(WireMock.get(urlMatching("/api/data/case-type/" + INVALID_CASE_TYPE_ID))
            .willReturn(notFound()));

        mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andExpect(status().is(404));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn404WhenGetEventTriggerForCaseTypeWithInvalidEventTriggerId() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/event-triggers/%s", USER_ID, JURISDICTION_ID, CASE_TYPE_ID, INVALID_CREATE_CASE_EVENT_TRIGGER_ID);

        mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andExpect(status().is(404));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenGetEventTriggerForCaseTypeWithInvalidEventTriggerHavingNullPreStates() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/event-triggers/%s", USER_ID, JURISDICTION_ID, CASE_TYPE_ID, NULL_PRE_STATES_CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(CALLBACK_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertEquals("Null pre states should have caused UNPROCESSABLE_ENTITY response", 422, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn200WhenGetEventTriggerForCaseWithValidCallbackData() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/event-triggers/%s", USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, UPDATE_EVENT_TRIGGER_ID);

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

        assertEquals(mvcResult.getResponse().getContentAsString(), 200, mvcResult.getResponse().getStatus());

        final CaseEventTrigger caseEventTrigger = mapper.readValue(mvcResult.getResponse().getContentAsString(), CaseEventTrigger.class);
        assertThat(caseEventTrigger.getCaseFields(), hasIds(new String[]{"PersonFirstName", "PersonLastName", "PersonAddress"}));
        assertThat(caseEventTrigger.getCaseFields(), hasSize(3));
        assertTrue("No token", !caseEventTrigger.getEventToken().isEmpty());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenGetEventTriggerForCaseWithCallbackDataWithErrors() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/event-triggers/%s", USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, UPDATE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setErrors(Lists.newArrayList("Error"));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertEquals("Callback errors should have caused UNPROCESSABLE_ENTITY response", 422, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenGetEventTriggerForCaseWithCallbackDataWithWarningsAndIgnoreWarningFalse() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/event-triggers/%s?ignore-warning=false", USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, UPDATE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setWarnings(Lists.newArrayList("Warning"));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertEquals("Callback warnings should have caused UNPROCESSABLE_ENTITY response", 422, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn200WhenGetEventTriggerForCaseWithCallbackDataWithWarningsAndIgnoreWarningTrue() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/event-triggers/%s?ignore-warning=true", USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, UPDATE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setWarnings(Lists.newArrayList("Warning"));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 200, mvcResult.getResponse().getStatus());

        final CaseEventTrigger caseEventTrigger = mapper.readValue(mvcResult.getResponse().getContentAsString(), CaseEventTrigger.class);
        assertThat(caseEventTrigger.getCaseFields(), hasIds(new String[]{"PersonFirstName", "PersonLastName", "PersonAddress"}));
        assertThat(caseEventTrigger.getCaseFields(), hasSize(3));
        assertTrue("No token", !caseEventTrigger.getEventToken().isEmpty());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenGetEventTriggerForCaseWithCallbackDataWithValidationErrors() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/event-triggers/%s?ignore-warning=false", USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, UPDATE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(INVALID_CALLBACK_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertEquals("Invalid callback data should have caused UNPROCESSABLE_ENTITY response", 422, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenGetEventTriggerForCaseWithInvalidEventTriggerHavingNoPreStates() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/event-triggers/%s", USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(CALLBACK_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertEquals("Empty pre states should have caused UNPROCESSABLE_ENTITY response", 422, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenGetEventTriggerForCaseWithInvalidEventTriggerHavingNonMatchingPreStates() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/event-triggers/%s", USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, NON_MATCHING_PRE_STATES_UPDATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(CALLBACK_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertEquals("Non matching pre states should have caused UNPROCESSABLE_ENTITY response", 422, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenGetEventTriggerForCaseWithInvalidEventTriggerHavingNullPreStates() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/event-triggers/%s", USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, NULL_PRE_STATES_CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(CALLBACK_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertEquals("Null pre states should have caused UNPROCESSABLE_ENTITY response", 422, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenGetEventTriggerForCaseWithInvalidEventTriggerHavingEmptyPreStates() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/event-triggers/%s", USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(CALLBACK_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertEquals("Empty pre states should have caused UNPROCESSABLE_ENTITY response", 422, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn404WhenGetEventTriggerForCaseWithNonExistentCaseId() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/event-triggers/%s", USER_ID, JURISDICTION_ID, CASE_TYPE_ID, NON_EXISTENT_CASE_REFERENCE, UPDATE_EVENT_TRIGGER_ID);

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 404, mvcResult.getResponse().getStatus());

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn404WhenGetEventTriggerForCaseWithInvalidCaseId() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/cases/%s/event-triggers/%s", USER_ID, JURISDICTION_ID, CASE_TYPE_ID, INVALID_CASE_REFERENCE, UPDATE_EVENT_TRIGGER_ID);

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 404, mvcResult.getResponse().getStatus());

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn404WhenGetEventTriggerForCaseWithInvalidEventTriggerId() throws Exception {
        final String URL = String.format("/aggregated/caseworkers/%d/jurisdictions/%s/case-types/%s/event-triggers/%s", USER_ID, JURISDICTION_ID, CASE_TYPE_ID, INVALID_CREATE_CASE_EVENT_TRIGGER_ID);

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
