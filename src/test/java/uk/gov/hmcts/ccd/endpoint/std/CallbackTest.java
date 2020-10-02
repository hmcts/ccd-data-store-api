package uk.gov.hmcts.ccd.endpoint.std;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItem;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItemType;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.endpoint.CallbackTestData;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PUBLIC;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

// too many legacy OperatorWrap occurrences on JSON strings so suppress until move to Java12+
@SuppressWarnings("checkstyle:OperatorWrap")
public class CallbackTest extends WireMockBaseTest {

    private static final String URL_BEFORE_COMMIT = "/before-commit.*";

    private static JsonNode DATA = null;
    private static final String DATA_JSON_STRING =
        "{\n" +
        "  \"PersonFirstName\": \"ccd-First Name\",\n" +
        "  \"PersonLastName\": \"Last Name\",\n" +
        "  \"PersonAddress\": {\n" +
        "    \"AddressLine1\": \"Address Line 1\",\n" +
        "    \"AddressLine2\": \"Address Line 2\"\n" +
        "  }\n" +
        "}\n";

    private static JsonNode DATA_CLASSIFICATION = null;
    private static final String DATA_CLASSIFICATION_JSON_STRING =
        "{\n" +
        "    \"PersonFirstName\": \"PUBLIC\",\n" +
        "    \"PersonLastName\": \"PUBLIC\",\n" +
        "    \"PersonAddress\": {\n" +
        "      \"classification\" : \"PUBLIC\",\n" +
        "      \"value\" : {\n" +
        "        \"AddressLine1\": \"PUBLIC\",\n" +
        "        \"AddressLine2\": \"PUBLIC\"\n" +
        "      }\n" +
        "    },\n" +
        "    \"D8Document\": \"PUBLIC\"" +
        "  }";

    private static JsonNode CALLBACK_DATA_CLASSIFICATION = null;
    private static final String CALLBACK_DATA_CLASSIFICATION_JSON_STRING =
        "{\n" +
        "    \"PersonFirstName\": \"PRIVATE\",\n" +
        "    \"PersonLastName\": \"PRIVATE\",\n" +
        "    \"PersonAddress\": {\n" +
        "      \"classification\" : \"PRIVATE\",\n" +
        "      \"value\" : {\n" +
        "        \"AddressLine1\": \"PRIVATE\",\n" +
        "        \"AddressLine2\": \"PRIVATE\"\n" +
        "      }\n" +
        "    },\n" +
        "    \"D8Document\": \"PRIVATE\"" +
        "  }";

    private static JsonNode CALLBACK_DATA_WITH_MISSING_CLASSIFICATION = null;
    private static final String CALLBACK_DATA_WITH_MISSING_CLASSIFICATION_JSON_STRING =
        "{\n" +
        "    \"PersonFirstName\": \"PRIVATE\",\n" +
        "    \"PersonLastName\": \"PRIVATE\",\n" +
        "    \"PersonAddress\": {\n" +
        "      \"classification\" : \"PRIVATE\",\n" +
        "      \"value\" : {\n" +
        "        \"AddressLine1\": \"PRIVATE\",\n" +
        "        \"AddressLine2\": \"PRIVATE\"\n" +
        "      }\n" +
        "    }\n" +
        "  }";

    private static final String EXPECTED_CALLBACK_DATA_CLASSIFICATION_STRING =
        "{\n" +
        "    \"PersonLastName\": \"PRIVATE\",\n" +
        "    \"PersonAddress\": {\n" +
        "      \"classification\" : \"PRIVATE\",\n" +
        "      \"value\" : {\n" +
        "        \"AddressLine1\": \"PRIVATE\",\n" +
        "        \"AddressLine2\": \"PRIVATE\"\n" +
        "      }\n" +
        "    },\n" +
        "    \"D8Document\": \"PRIVATE\"" +
        "  }";

    private String modifiedDataString;

    private static JsonNode MODIFIED_DATA = null;

    private String expectedModifiedDataAfterAuthString;

    private static JsonNode EXPECTED_SAVED_DATA = null;

    private String expectedSavedDataString;

    private static JsonNode EXPECTED_MODIFIED_DATA = null;

    private String sanitizedModifiedDataWithMissingBinaryLinkString;

    private static JsonNode SANITIZED_MODIFIED_DATA_WITH_MISSING_BINARY_LINK = null;

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

    private static JsonNode MODIFIED_CORRUPTED_DATA = null;
    private static final String MODIFIED_CORRUPTED_DATA_JSON_STRING =
        "{\n" +
        "  \"adsdsassdasad\": \"First Name\",\n" +
        "  \"PersonLastName\": \"Last Name\",\n" +
        "  \"PersonAddress\": {\n" +
        "    \"AddressLine1\": \"Address Line 11\",\n" +
        "    \"AddressLine2\": \"Address Line 12\"\n" +
        "  }\n" +
        "}\n";

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;
    private JdbcTemplate jdbcTemplate;

    private static final String CREATE_CASE_EVENT_ID = "CREATE-CASE";
    private static final String UPDATE_EVENT_ID = "UPDATE-EVENT";
    private static final String UPDATE_EVENT_TRIGGER_ID = UPDATE_EVENT_ID;
    private static final String CREATE_CASE_EVENT_TRIGGER_ID = CREATE_CASE_EVENT_ID;
    private static final String CASE_TYPE_ID = "CallbackCase";
    private static final String JURISDICTION_ID = "TEST";
    private static final String USER_ID = "123";
    private static final String OTHER_USER_ID = "456";
    private static final Long CASE_REFERENCE = 1504259907353545L;
    private static final Long INVALID_REFERENCE = 1504259907L;

    private static final String SUMMARY = "Case event summary";
    private static final String DESCRIPTION = "Case event description";

    public CallbackTest() throws IOException {
    }

    @Before
    public void setUp() throws IOException {

        DATA = mapper.readTree(DATA_JSON_STRING);
        DATA_CLASSIFICATION = mapper.readTree(DATA_CLASSIFICATION_JSON_STRING);
        CALLBACK_DATA_CLASSIFICATION = mapper.readTree(CALLBACK_DATA_CLASSIFICATION_JSON_STRING);
        CALLBACK_DATA_WITH_MISSING_CLASSIFICATION =
            mapper.readTree(CALLBACK_DATA_WITH_MISSING_CLASSIFICATION_JSON_STRING);
        INVALID_CALLBACK_DATA = mapper.readTree(INVALID_CALLBACK_DATA_JSON_STRING);
        MODIFIED_CORRUPTED_DATA = mapper.readTree(MODIFIED_CORRUPTED_DATA_JSON_STRING);

        modifiedDataString = "{\n"
            + "  \"PersonFirstName\": \"ccd-First Name\",\n"
            + "  \"PersonLastName\": \"Last Name\",\n"
            + "  \"PersonAddress\": {\n"
            + "    \"AddressLine1\": \"Address Line 11\",\n"
            + "    \"AddressLine2\": \"Address Line 12\"\n"
            + "  },\n"
            + "  \"D8Document\":{"
            + "    \"document_url\": \"http://localhost:" + getPort()
            + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\""
            + "  }\n"
            + "}\n";

        expectedModifiedDataAfterAuthString = "{\n"
            + "  \"PersonLastName\": \"Last Name\",\n"
            + "  \"PersonAddress\": {\n"
            + "    \"AddressLine1\": \"Address Line 11\",\n"
            + "    \"AddressLine2\": \"Address Line 12\"\n"
            + "  },\n"
            + "  \"D8Document\":{"
            + "    \"document_url\": \"http://localhost:" + getPort()
            + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\",\n"
            + "    \"document_binary_url\": \"http://localhost:[port]/documents/"
            + "05e7cd7e-7041-4d8a-826a-7bb49dfd83d0/binary\",\n"
            + "    \"document_filename\": \"Seagulls_Square.jpg\""
            + "  }\n"
            + "}\n";

        expectedSavedDataString = "{\n"
            + "  \"PersonFirstName\": \"ccd-First Name\",\n"
            + "  \"PersonLastName\": \"Last Name\",\n"
            + "  \"PersonAddress\": {\n"
            + "    \"AddressLine1\": \"Address Line 11\",\n"
            + "    \"AddressLine2\": \"Address Line 12\"\n"
            + "  },\n"
            + "  \"D8Document\":{"
            + "    \"document_url\": \"http://localhost:" + getPort()
            + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\",\n"
            + "    \"document_binary_url\": \"http://localhost:[port]/documents/"
            + "05e7cd7e-7041-4d8a-826a-7bb49dfd83d0/binary\",\n"
            + "    \"document_filename\": \"Seagulls_Square.jpg\""
            + "  }\n"
            + "}\n";

        sanitizedModifiedDataWithMissingBinaryLinkString = "{\n"
            + "  \"PersonLastName\": \"Last Name\",\n"
            + "  \"PersonAddress\": {\n"
            + "    \"AddressLine1\": \"Address Line 11\",\n"
            + "    \"AddressLine2\": \"Address Line 12\"\n"
            + "  },\n"
            + "  \"D8Document\":{"
            + "    \"document_url\": \"http://localhost:" + getPort()
            + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1\"\n"
            + "  }\n"
            + "}\n";

        MockitoAnnotations.initMocks(this);
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_TEST_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        jdbcTemplate = new JdbcTemplate(db);
        stubFor(get(urlMatching("/api/data/case-type/CallbackCase"))
            .willReturn(okJson(CallbackTestData.getTestDefinition(getPort())).withStatus(200)));
        MODIFIED_DATA = mapper.readTree(modifiedDataString);
        EXPECTED_MODIFIED_DATA = mapper.readTree(expectedModifiedDataAfterAuthString);
        EXPECTED_SAVED_DATA = mapper.readTree(expectedSavedDataString);
        SANITIZED_MODIFIED_DATA_WITH_MISSING_BINARY_LINK =
            mapper.readTree(sanitizedModifiedDataWithMissingBinaryLinkString);
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseWithModifiedDataForCaseworker() throws Exception {
        final String URL = String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases", USER_ID,
            JURISDICTION_ID, CASE_TYPE_ID);
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(CREATE_CASE_EVENT_ID);
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));
        caseDetailsToSave.setToken(generateEventTokenNewCase(USER_ID, JURISDICTION_ID, CASE_TYPE_ID,
            CREATE_CASE_EVENT_ID));

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(MODIFIED_DATA));
        callbackResponse.setDataClassification(JacksonUtils.convertValue(DATA_CLASSIFICATION));
        callbackResponse.setSecurityClassification(PUBLIC);

        stubFor(WireMock.post(urlMatching("/before-commit.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        stubFor(WireMock.post(urlMatching("/after-commit.*"))
            .willReturn(ok()));

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 201, mvcResult.getResponse().getStatus());
        Map expectedSanitizedData = mapper.readValue(EXPECTED_MODIFIED_DATA.toString(), Map.class);
        Map actualData = mapper.readValue(mapper.readTree(mvcResult.getResponse().getContentAsString())
            .get("case_data").toString(), Map.class);
        assertThat("Incorrect Response Content", actualData.entrySet(), equalTo(expectedSanitizedData
            .entrySet()));

        final List<CaseDetails> caseDetailsList = jdbcTemplate.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertEquals("Incorrect Case Type", CASE_TYPE_ID, savedCaseDetails.getCaseTypeId());
        Map sanitizedData = JacksonUtils.convertValue(EXPECTED_SAVED_DATA);
        assertThat("Incorrect Data content", savedCaseDetails.getData().entrySet(),
            equalTo(sanitizedData.entrySet()));
        assertEquals("CaseCreated", savedCaseDetails.getState());
        assertThat(savedCaseDetails.getSecurityClassification(), Matchers.equalTo(PUBLIC));

        final List<AuditEvent> caseAuditEventList =
            jdbcTemplate.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("Incorrect number of case events", 1, caseAuditEventList.size());

        // Assertion belows are for creation event
        final AuditEvent caseAuditEvent = caseAuditEventList.get(0);
        assertEquals(USER_ID, caseAuditEvent.getUserId());
        assertEquals("Strife", caseAuditEvent.getUserLastName());
        assertEquals("Cloud", caseAuditEvent.getUserFirstName());
        assertEquals(CREATE_CASE_EVENT_ID, caseAuditEvent.getEventId());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals(savedCaseDetails.getCreatedDate(), caseAuditEvent.getCreatedDate());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertThat(caseAuditEvent.getSecurityClassification(), Matchers.equalTo(PUBLIC));
        final List<SignificantItem> significantItemList =
            jdbcTemplate.query("SELECT * FROM case_event_significant_items where case_event_id = "
                + caseAuditEvent.getId(), this::mapSignificantItem);
        assertEquals(0, significantItemList.size());
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseWithModifiedDataForCaseworkerAndSignificantDocument()
                                                                                                    throws Exception {
        final String URL = String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases", USER_ID,
            JURISDICTION_ID, CASE_TYPE_ID);
        final CaseDataContent caseDetailsToSave = new CaseDataContent();
        caseDetailsToSave.setEvent(new Event());
        caseDetailsToSave.getEvent().setEventId(CREATE_CASE_EVENT_ID);
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));
        caseDetailsToSave.setToken(generateEventTokenNewCase(USER_ID, JURISDICTION_ID, CASE_TYPE_ID,
            CREATE_CASE_EVENT_ID));

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(MODIFIED_DATA));
        callbackResponse.setDataClassification(JacksonUtils.convertValue(DATA_CLASSIFICATION));
        callbackResponse.setSecurityClassification(PUBLIC);
        final SignificantItem significantItem = new SignificantItem();
        significantItem.setUrl("https://www.npmjs.com/package/supertest");
        significantItem.setType(SignificantItemType.DOCUMENT.name());
        significantItem.setDescription("Some description");
        callbackResponse.setSignificantItem(significantItem);

        stubFor(WireMock.post(urlMatching("/before-commit.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        stubFor(WireMock.post(urlMatching("/after-commit.*"))
            .willReturn(ok()));

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 201, mvcResult.getResponse().getStatus());
        Map expectedSanitizedData = mapper.readValue(EXPECTED_MODIFIED_DATA.toString(), Map.class);
        Map actualData =
            mapper.readValue(mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data")
                .toString(), Map.class);
        assertThat("Incorrect Response Content", actualData.entrySet(), equalTo(expectedSanitizedData
            .entrySet()));

        final List<CaseDetails> caseDetailsList = jdbcTemplate.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertEquals("Incorrect Case Type", CASE_TYPE_ID, savedCaseDetails.getCaseTypeId());
        Map sanitizedData = JacksonUtils.convertValue(EXPECTED_SAVED_DATA);
        assertThat("Incorrect Data content", savedCaseDetails.getData().entrySet(), equalTo(sanitizedData
            .entrySet()));
        assertEquals("CaseCreated", savedCaseDetails.getState());
        assertThat(savedCaseDetails.getSecurityClassification(), Matchers.equalTo(PUBLIC));

        final List<AuditEvent> caseAuditEventList =
            jdbcTemplate.query("SELECT * FROM case_event ", this::mapAuditEvent);
        assertEquals("Incorrect number of case events", 1, caseAuditEventList.size());

        // Assertion belows are for creation event
        final AuditEvent caseAuditEvent = caseAuditEventList.get(0);
        assertEquals(USER_ID, caseAuditEvent.getUserId());
        assertEquals("Strife", caseAuditEvent.getUserLastName());
        assertEquals("Cloud", caseAuditEvent.getUserFirstName());
        assertEquals(CREATE_CASE_EVENT_ID, caseAuditEvent.getEventId());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals(savedCaseDetails.getCreatedDate(), caseAuditEvent.getCreatedDate());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertThat(caseAuditEvent.getSecurityClassification(), Matchers.equalTo(PUBLIC));
        final List<SignificantItem> significantItemList =
            jdbcTemplate.query("SELECT * FROM case_event_significant_items where case_event_id = "
                + caseAuditEvent.getId(), this::mapSignificantItem);
        assertEquals("https://www.npmjs.com/package/supertest", significantItemList.get(0).getUrl());
        assertEquals("Some description", significantItemList.get(0).getDescription());
        assertEquals(SignificantItemType.DOCUMENT.name(), significantItemList.get(0).getType());
    }

    @Test
    public void shouldReturn400WhenPostCreateCaseWithInvalidSignificantDocument() throws Exception {
        final String URL = String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases", USER_ID,
            JURISDICTION_ID, CASE_TYPE_ID);
        final CaseDataContent caseDetailsToSave = new CaseDataContent();
        caseDetailsToSave.setEvent(new Event());
        caseDetailsToSave.getEvent().setEventId(CREATE_CASE_EVENT_ID);
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));
        caseDetailsToSave.setToken(generateEventTokenNewCase(USER_ID, JURISDICTION_ID, CASE_TYPE_ID,
            CREATE_CASE_EVENT_ID));

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(MODIFIED_DATA));
        callbackResponse.setDataClassification(JacksonUtils.convertValue(DATA_CLASSIFICATION));
        callbackResponse.setSecurityClassification(PUBLIC);
        final SignificantItem significantItem = new SignificantItem();
        significantItem.setUrl("https://www.npmjs.com/package/supertest");
        significantItem.setType(SignificantItemType.DOCUMENT.name());
        callbackResponse.setSignificantItem(significantItem);

        stubFor(WireMock.post(urlMatching("/before-commit.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        stubFor(WireMock.post(urlMatching("/after-commit.*"))
            .willReturn(ok()));

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 422, mvcResult.getResponse().getStatus());
        assertTrue(mvcResult.getResponse().getContentAsString().contains("Description should not be empty but also not"
            + " more than 64 characters"));

    }

    @Test
    public void shouldReturn201WhenPostCreateCaseWithModifiedDataForCitizen() throws Exception {
        final String URL = String.format("/citizens/%s/jurisdictions/%s/case-types/%s/cases", USER_ID, JURISDICTION_ID,
            CASE_TYPE_ID);
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(CREATE_CASE_EVENT_ID);
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));
        caseDetailsToSave.setToken(generateEventTokenNewCase(USER_ID, JURISDICTION_ID, CASE_TYPE_ID,
            CREATE_CASE_EVENT_ID));

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(MODIFIED_DATA));
        callbackResponse.setDataClassification(JacksonUtils.convertValue(DATA_CLASSIFICATION));
        callbackResponse.setSecurityClassification(PUBLIC);

        stubFor(WireMock.post(urlMatching("/before-commit.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        stubFor(WireMock.post(urlMatching("/after-commit.*"))
            .willReturn(ok()));

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 201, mvcResult.getResponse().getStatus());
        Map expectedSanitizedData = mapper.readValue(EXPECTED_MODIFIED_DATA.toString(), Map.class);
        Map actualData =
            mapper.readValue(mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data")
                .toString(), Map.class);
        assertThat("Incorrect Response Content", actualData.entrySet(), equalTo(expectedSanitizedData
            .entrySet()));

        final List<CaseDetails> caseDetailsList = jdbcTemplate.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertEquals("Incorrect Case Type", CASE_TYPE_ID, savedCaseDetails.getCaseTypeId());
        Map sanitizedData = JacksonUtils.convertValue(EXPECTED_SAVED_DATA);
        assertThat("Incorrect Data content", savedCaseDetails.getData().entrySet(), equalTo(sanitizedData
            .entrySet()));

        assertEquals("CaseCreated", savedCaseDetails.getState());

        final List<AuditEvent> caseAuditEventList =
            jdbcTemplate.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("Incorrect number of case events", 1, caseAuditEventList.size());

        // Assertion belows are for creation event
        final AuditEvent caseAuditEvent = caseAuditEventList.get(0);
        assertEquals(USER_ID, caseAuditEvent.getUserId());
        assertEquals("Strife", caseAuditEvent.getUserLastName());
        assertEquals("Cloud", caseAuditEvent.getUserFirstName());
        assertEquals(CREATE_CASE_EVENT_ID, caseAuditEvent.getEventId());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals(savedCaseDetails.getCreatedDate(), caseAuditEvent.getCreatedDate());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertThat(caseAuditEvent.getSecurityClassification(), Matchers.equalTo(PUBLIC));
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseWithCallbackOverridingDataClassificationForCaseworker()
                                                                                                    throws Exception {
        final String URL = String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases", USER_ID,
            JURISDICTION_ID, CASE_TYPE_ID);
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(CREATE_CASE_EVENT_ID);
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));
        caseDetailsToSave.setToken(generateEventTokenNewCase(USER_ID, JURISDICTION_ID, CASE_TYPE_ID,
            CREATE_CASE_EVENT_ID));

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(MODIFIED_DATA));
        callbackResponse.setDataClassification(JacksonUtils.convertValue(CALLBACK_DATA_CLASSIFICATION));
        callbackResponse.setSecurityClassification(PUBLIC);

        stubFor(WireMock.post(urlMatching("/before-commit.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        stubFor(WireMock.post(urlMatching("/after-commit.*"))
            .willReturn(ok()));

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 201, mvcResult.getResponse().getStatus());
        Map actualData =
            mapper.readValue(mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data")
            .toString(), Map.class);
        assertThat("Incorrect Response Data Content", actualData.entrySet().size(), equalTo(0));
        String actualDataClassification = mapper.readTree(mvcResult.getResponse().getContentAsString())
            .get("data_classification").toString();
        JSONAssert.assertEquals(EXPECTED_CALLBACK_DATA_CLASSIFICATION_STRING, actualDataClassification,
            JSONCompareMode.LENIENT);

        final List<CaseDetails> caseDetailsList = jdbcTemplate.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertEquals("Incorrect Case Type", CASE_TYPE_ID, savedCaseDetails.getCaseTypeId());
        Map sanitizedData = JacksonUtils.convertValue(EXPECTED_SAVED_DATA);
        assertThat("Incorrect Data content", savedCaseDetails.getData().entrySet(), equalTo(sanitizedData
            .entrySet()));
        assertEquals("CaseCreated", savedCaseDetails.getState());
        assertThat(savedCaseDetails.getSecurityClassification(), Matchers.equalTo(PUBLIC));

        final List<AuditEvent> caseAuditEventList =
            jdbcTemplate.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("Incorrect number of case events", 1, caseAuditEventList.size());

        // Assertion belows are for creation event
        final AuditEvent caseAuditEvent = caseAuditEventList.get(0);
        assertEquals(USER_ID, caseAuditEvent.getUserId());
        assertEquals("Strife", caseAuditEvent.getUserLastName());
        assertEquals("Cloud", caseAuditEvent.getUserFirstName());
        assertEquals(CREATE_CASE_EVENT_ID, caseAuditEvent.getEventId());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals(savedCaseDetails.getCreatedDate(), caseAuditEvent.getCreatedDate());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertThat(caseAuditEvent.getSecurityClassification(), Matchers.equalTo(PUBLIC));
    }

    @Test
    public void shouldReturn422WhenPostCreateCaseWithCallbackOverridingDataWithMissingClassificationForCaseworker()
                                                                                                    throws Exception {
        final String URL = String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases", USER_ID,
            JURISDICTION_ID, CASE_TYPE_ID);
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(CREATE_CASE_EVENT_ID);
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));
        caseDetailsToSave.setToken(generateEventTokenNewCase(USER_ID, JURISDICTION_ID, CASE_TYPE_ID,
            CREATE_CASE_EVENT_ID));

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(MODIFIED_DATA));
        callbackResponse.setDataClassification(JacksonUtils.convertValue(CALLBACK_DATA_WITH_MISSING_CLASSIFICATION));
        callbackResponse.setSecurityClassification(PUBLIC);

        stubFor(WireMock.post(urlMatching("/before-commit.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        stubFor(WireMock.post(urlMatching("/after-commit.*"))
            .willReturn(ok()));

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals("Incorrect Response Status Code", 422, mvcResult.getResponse().getStatus());
        assertEquals("Incorrect Error Message", "\"The event cannot be complete due to a callback returned data "
                + "validation error (c)\"",
            mapper.readTree(mvcResult.getResponse().getContentAsString()).get("message").toString());
    }

    @Test
    public void shouldReturn422WhenPostCreateCaseWithInvalidModifiedDataFromBeforeCommitForCaseworker()
                                                                                                     throws Exception {
        final String URL = String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases", USER_ID,
            JURISDICTION_ID, CASE_TYPE_ID);
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(CREATE_CASE_EVENT_ID);
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));
        caseDetailsToSave.setToken(generateEventTokenNewCase(USER_ID, JURISDICTION_ID, CASE_TYPE_ID,
            CREATE_CASE_EVENT_ID));

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(MODIFIED_CORRUPTED_DATA));

        stubFor(WireMock.post(urlMatching("/before-commit.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        stubFor(WireMock.post(urlMatching("/after-commit.*"))
            .willReturn(ok()));

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals("Invalid modified content were not caught by validators", 422,
            mvcResult.getResponse().getStatus());
    }

    @Test
    public void shouldReturn422WhenPostCreateCaseWithInvalidModifiedDataFromBeforeCommitForCitizen() throws Exception {
        final String URL = String.format("/citizens/%s/jurisdictions/%s/case-types/%s/cases", USER_ID, JURISDICTION_ID,
            CASE_TYPE_ID);
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(CREATE_CASE_EVENT_ID);
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));
        caseDetailsToSave.setToken(generateEventTokenNewCase(USER_ID, JURISDICTION_ID, CASE_TYPE_ID,
            CREATE_CASE_EVENT_ID));

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(MODIFIED_CORRUPTED_DATA));

        stubFor(WireMock.post(urlMatching("/before-commit.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));


        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals("Invalid modified content were not caught by validators", 422,
            mvcResult.getResponse().getStatus());
    }

    @Test
    public void shouldReturn422WhenPostCreateCaseWithInvalidModifiedMissingDocumentDataFromBeforeCommitForCaseworker()
                                                                                                    throws Exception {
        final String URL = String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases", USER_ID,
            JURISDICTION_ID, CASE_TYPE_ID);
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(CREATE_CASE_EVENT_ID);
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));
        caseDetailsToSave.setToken(generateEventTokenNewCase(USER_ID, JURISDICTION_ID, CASE_TYPE_ID,
            CREATE_CASE_EVENT_ID));

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(SANITIZED_MODIFIED_DATA_WITH_MISSING_BINARY_LINK));

        stubFor(WireMock.post(urlMatching("/before-commit.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        stubFor(WireMock.post(urlMatching("/after-commit.*"))
            .willReturn(ok()));

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals("Invalid modified content were not caught by validators", 422,
            mvcResult.getResponse().getStatus());
    }

    @Test
    public void shouldReturn422WhenPostCreateCaseWithInvalidModifiedMissingDocumentDataFromBeforeCommitForCitizen()
                                                                                                     throws Exception {
        final String URL = String.format("/citizens/%s/jurisdictions/%s/case-types/%s/cases", USER_ID, JURISDICTION_ID,
            CASE_TYPE_ID);
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(CREATE_CASE_EVENT_ID);
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));
        caseDetailsToSave.setToken(generateEventTokenNewCase(USER_ID, JURISDICTION_ID, CASE_TYPE_ID,
            CREATE_CASE_EVENT_ID));

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(SANITIZED_MODIFIED_DATA_WITH_MISSING_BINARY_LINK));

        stubFor(WireMock.post(urlMatching("/before-commit.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));


        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals("Invalid modified content were not caught by validators", 422,
            mvcResult.getResponse().getStatus());
    }

    @Test
    public void shouldReturn422WhenPostCreateCaseWithErrorsFromBeforeCommitForCaseworker() throws Exception {
        final String URL = String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases", USER_ID,
            JURISDICTION_ID, CASE_TYPE_ID);
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(CREATE_CASE_EVENT_ID);
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));
        caseDetailsToSave.setToken(generateEventTokenNewCase(USER_ID, JURISDICTION_ID, CASE_TYPE_ID,
            CREATE_CASE_EVENT_ID));

        stubForErrorCallbackResponse(URL_BEFORE_COMMIT);

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals("Callback did not catch block", 422, mvcResult.getResponse().getStatus());
    }

    @Test
    public void shouldReturn422WhenPostCreateCaseWithErrorsFromBeforeCommitForCitizen() throws Exception {
        final String URL = String.format("/citizens/%s/jurisdictions/%s/case-types/%s/cases", USER_ID, JURISDICTION_ID,
            CASE_TYPE_ID);
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(CREATE_CASE_EVENT_ID);
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));
        caseDetailsToSave.setToken(generateEventTokenNewCase(USER_ID, JURISDICTION_ID, CASE_TYPE_ID,
            CREATE_CASE_EVENT_ID));

        stubForErrorCallbackResponse("/before-commit.*");

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals("Callback did not catch block", 422, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenGetEventTokenForCaseWithCallbackErrorsForCaseworker() throws Exception {
        final String URL =
            String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases/%d/event-triggers/%s/token",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, UPDATE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setErrors(Collections.singletonList("Just a test"));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL).contentType(MediaType.APPLICATION_JSON))
            .andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 422, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenGetEventTokenForCaseWithCallbackErrorsForCitizen() throws Exception {
        final String URL = String.format("/citizens/%s/jurisdictions/%s/case-types/%s/cases/%d/event-triggers/%s/token",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, UPDATE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setErrors(Collections.singletonList("Just a test"));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));


        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL).contentType(MediaType.APPLICATION_JSON))
            .andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 422, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn200WhenGetEventTokenForCaseWithValidCallbackDataForCaseworker() throws Exception {
        final String URL =
            String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases/%d/event-triggers/%s/token",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, UPDATE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(MODIFIED_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL).contentType(MediaType.APPLICATION_JSON))
            .andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 200, mvcResult.getResponse().getStatus());

        final StartEventResult startEventTrigger =
            mapper.readValue(mvcResult.getResponse().getContentAsString(), StartEventResult.class);
        assertEquals("Incorrect Data content", JacksonUtils.convertValue(EXPECTED_MODIFIED_DATA), startEventTrigger
            .getCaseDetails().getData());
        assertTrue("No token", !startEventTrigger.getToken().isEmpty());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenGetEventTokenForCaseWithCallbackDataWithValidationErrorsForCaseworker()
                                                                                                    throws Exception {
        final String URL =
            String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases/%d/event-triggers/%s/token",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(INVALID_CALLBACK_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL).contentType(MediaType.APPLICATION_JSON))
            .andReturn();

        assertEquals("Invalid callback data should have caused UNPROCESSABLE_ENTITY response", 422,
            mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenGetEventTokenForCaseWithCallbackDataWithValidationErrorsForCitizen()
                                                                                                    throws Exception {
        final String URL =
            String.format("/citizens/%s/jurisdictions/%s/case-types/%s/cases/%d/event-triggers/%s/token",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(INVALID_CALLBACK_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL).contentType(MediaType.APPLICATION_JSON))
            .andReturn();

        assertEquals("Invalid callback data should have caused UNPROCESSABLE_ENTITY response", 422,
            mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn400WhenGetEventTokenForCaseWithInvalidCaseReferenceForCaseworker() throws Exception {
        final String URL =
            String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases/%d/event-triggers/%s/token",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, INVALID_REFERENCE, CREATE_CASE_EVENT_TRIGGER_ID);

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL).contentType(MediaType.APPLICATION_JSON))
            .andReturn();

        assertEquals("Invalid case reference data should have caused BAD_REQUEST response", 400,
            mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn400WhenGetEventTokenForCaseWithInvalidCaseReferenceForCitizen() throws Exception {
        final String URL =
            String.format("/citizens/%s/jurisdictions/%s/case-types/%s/cases/%d/event-triggers/%s/token",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, INVALID_REFERENCE, CREATE_CASE_EVENT_TRIGGER_ID);

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL).contentType(MediaType.APPLICATION_JSON))
            .andReturn();

        assertEquals("Invalid case reference data should have caused BAD_REQUEST response", 400,
            mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn200WhenGetEventTokenForCaseWithValidCallbackDataForCitizen() throws Exception {
        final String URL =
            String.format("/citizens/%s/jurisdictions/%s/case-types/%s/cases/%d/event-triggers/%s/token",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, UPDATE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(MODIFIED_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));


        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL).contentType(MediaType.APPLICATION_JSON))
            .andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 200, mvcResult.getResponse().getStatus());

        final StartEventResult startEventTrigger = mapper.readValue(mvcResult.getResponse().getContentAsString(),
            StartEventResult.class);
        assertEquals("Incorrect Data content", JacksonUtils.convertValue(EXPECTED_MODIFIED_DATA), startEventTrigger
            .getCaseDetails().getData());
        assertTrue("No token", !startEventTrigger.getToken().isEmpty());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenGetEventTokenForCaseTypeWithCallbackErrorsForCaseworker() throws Exception {
        final String URL = String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/event-triggers/%s/token",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, UPDATE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setErrors(Collections.singletonList("Just a test"));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL).contentType(MediaType.APPLICATION_JSON))
            .andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 422, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenGetEventTokenForCaseTypeWithCallbackErrorsForCitizen() throws Exception {
        final String URL = String.format("/citizens/%s/jurisdictions/%s/case-types/%s/event-triggers/%s/token",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, UPDATE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setErrors(Collections.singletonList("Just a test"));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL).contentType(MediaType.APPLICATION_JSON))
            .andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 422, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn200WhenGetEventTokenForCaseTypeWithValidModifiedDataForCaseworker() throws Exception {
        final String URL = String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/event-triggers/%s/token",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(MODIFIED_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL).contentType(MediaType.APPLICATION_JSON))
            .andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 200, mvcResult.getResponse().getStatus());

        final StartEventResult startEventTrigger = mapper.readValue(mvcResult.getResponse().getContentAsString(),
            StartEventResult.class);
        assertEquals("Incorrect Data content", JacksonUtils.convertValue(EXPECTED_MODIFIED_DATA), startEventTrigger
            .getCaseDetails().getData());
        assertTrue("No token", !startEventTrigger.getToken().isEmpty());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn200WhenGetEventTokenForCaseTypeWithValidModifiedDataForCitizen() throws Exception {
        final String URL = String.format("/citizens/%s/jurisdictions/%s/case-types/%s/event-triggers/%s/token",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(MODIFIED_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL).contentType(MediaType.APPLICATION_JSON))
            .andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 200, mvcResult.getResponse().getStatus());

        final StartEventResult startEventTrigger = mapper.readValue(mvcResult.getResponse().getContentAsString(),
            StartEventResult.class);
        assertEquals("Incorrect Data content", JacksonUtils.convertValue(EXPECTED_MODIFIED_DATA), startEventTrigger
            .getCaseDetails().getData());
        assertTrue("No token", !startEventTrigger.getToken().isEmpty());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenGetEventTokenForCaseTypeWithCallbackDataWithValidationErrorsForCaseworker()
                                                                                                    throws Exception {
        final String URL = String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/event-triggers/%s/token",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(INVALID_CALLBACK_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL).contentType(MediaType.APPLICATION_JSON))
            .andReturn();

        assertEquals("Invalid callback data should have caused UNPROCESSABLE_ENTITY response", 422,
            mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenGetEventTokenForCaseTypeWithCallbackDataWithValidationErrorsForCitizen()
                                                                                                    throws Exception {
        final String URL = String.format("/citizens/%s/jurisdictions/%s/case-types/%s/event-triggers/%s/token",
            USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CREATE_CASE_EVENT_TRIGGER_ID);

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(INVALID_CALLBACK_DATA));

        stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc
            .perform(MockMvcRequestBuilders.get(URL).contentType(MediaType.APPLICATION_JSON))
            .andReturn();

        assertEquals("Invalid callback data should have caused UNPROCESSABLE_ENTITY response", 422,
            mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn201WhenPostCreateEventWithValidDataForCaseworker() throws Exception {
        final String URL =
            String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases/%d/events", USER_ID, JURISDICTION_ID,
                CASE_TYPE_ID, CASE_REFERENCE);

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(UPDATE_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);

        final String token = generateEventToken(jdbcTemplate, USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE,
            UPDATE_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(event);
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(MODIFIED_DATA));
        callbackResponse.setSecurityClassification(PUBLIC);
        callbackResponse.setDataClassification(JacksonUtils.convertValue(DATA_CLASSIFICATION));

        stubFor(WireMock.post(urlMatching("/before-commit.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        stubFor(WireMock.post(urlMatching("/after-commit.*"))
            .willReturn(ok()));

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 201, mvcResult.getResponse().getStatus());
        Map expectedSanitizedData = mapper.readValue(EXPECTED_MODIFIED_DATA.toString(), Map.class);
        Map actualData =
            mapper.readValue(mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data")
            .toString(), Map.class);
        assertThat("Incorrect Response Content", actualData.entrySet(), equalTo(expectedSanitizedData
            .entrySet()));

        final List<CaseDetails> caseDetailsList = jdbcTemplate.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertEquals("Incorrect Case Type", CASE_TYPE_ID, savedCaseDetails.getCaseTypeId());
        Map sanitizedData = JacksonUtils.convertValue(EXPECTED_SAVED_DATA);
        assertThat("Incorrect Data content", savedCaseDetails.getData().entrySet(), equalTo(sanitizedData
            .entrySet()));
        assertEquals("CaseUpdated", savedCaseDetails.getState());

        final List<AuditEvent> caseAuditEventList =
            jdbcTemplate.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("Incorrect number of case events", 1, caseAuditEventList.size());

        // Assertion belows are for creation event
        final AuditEvent caseAuditEvent = caseAuditEventList.get(0);
        assertEquals(USER_ID, caseAuditEvent.getUserId());
        assertEquals("Strife", caseAuditEvent.getUserLastName());
        assertEquals("Cloud", caseAuditEvent.getUserFirstName());
        assertEquals(UPDATE_EVENT_ID, caseAuditEvent.getEventId());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn201WhenPostCreateEventWithValidDataForCitizen() throws Exception {
        final String URL = String.format("/citizens/%s/jurisdictions/%s/case-types/%s/cases/%d/events", USER_ID,
            JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(UPDATE_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);

        final String token = generateEventToken(jdbcTemplate, USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE,
            UPDATE_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(event);
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(MODIFIED_DATA));
        callbackResponse.setDataClassification(JacksonUtils.convertValue(DATA_CLASSIFICATION));
        callbackResponse.setSecurityClassification(PUBLIC);

        stubFor(WireMock.post(urlMatching("/before-commit.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        stubFor(WireMock.post(urlMatching("/after-commit.*"))
            .willReturn(ok()));

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 201, mvcResult.getResponse().getStatus());
        Map expectedSanitizedData = mapper.readValue(EXPECTED_MODIFIED_DATA.toString(), Map.class);
        Map actualData =
            mapper.readValue(mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data")
            .toString(), Map.class);
        assertThat("Incorrect Response Content", actualData.entrySet(), equalTo(expectedSanitizedData
            .entrySet()));

        final List<CaseDetails> caseDetailsList = jdbcTemplate.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertEquals("Incorrect Case Type", CASE_TYPE_ID, savedCaseDetails.getCaseTypeId());
        Map sanitizedData = JacksonUtils.convertValue(EXPECTED_SAVED_DATA);
        assertThat("Incorrect Data content", savedCaseDetails.getData().entrySet(), equalTo(sanitizedData
            .entrySet()));
        assertEquals("CaseUpdated", savedCaseDetails.getState());

        final List<AuditEvent> caseAuditEventList =
            jdbcTemplate.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("Incorrect number of case events", 1, caseAuditEventList.size());

        // Assertion belows are for creation event
        final AuditEvent caseAuditEvent = caseAuditEventList.get(0);
        assertEquals(USER_ID, caseAuditEvent.getUserId());
        assertEquals("Strife", caseAuditEvent.getUserLastName());
        assertEquals("Cloud", caseAuditEvent.getUserFirstName());
        assertEquals(UPDATE_EVENT_ID, caseAuditEvent.getEventId());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenPostCreateEventWithCallbackErrorForCaseworker() throws Exception {
        final String URL =
            String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases/%d/events", USER_ID, JURISDICTION_ID,
                CASE_TYPE_ID, CASE_REFERENCE);

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(UPDATE_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);

        final String token = generateEventToken(jdbcTemplate, USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE,
            UPDATE_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(event);
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));

        stubForErrorCallbackResponse("/before-commit.*");

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 422, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenPostCreateEventWithCallbackErrorForCitizen() throws Exception {
        final String URL = String.format("/citizens/%s/jurisdictions/%s/case-types/%s/cases/%d/events", USER_ID,
            JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(UPDATE_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);

        final String token = generateEventToken(jdbcTemplate, USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE,
            UPDATE_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(event);
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));

        stubForErrorCallbackResponse("/before-commit.*");

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 422, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenPostCreateEventWithInvalidCallbackDataForCaseworker() throws Exception {
        final CaseTypeDefinition caseType = new CaseTypeDefinition();
        caseType.setId(CASE_TYPE_ID);
        final JurisdictionDefinition jurisdiction = new JurisdictionDefinition();
        jurisdiction.setId(JURISDICTION_ID);
        final String URL =
            String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases/%d/events", USER_ID, JURISDICTION_ID,
                CASE_TYPE_ID, CASE_REFERENCE);

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(UPDATE_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);


        final String token = generateEventToken(jdbcTemplate, USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE,
            UPDATE_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(event);
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(MODIFIED_CORRUPTED_DATA));

        stubFor(WireMock.post(urlMatching("/before-commit.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));
        stubFor(WireMock.post(urlMatching("/before-commit.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 422, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn422WhenPostCreateEventWithInvalidCallbackDataForCitizen() throws Exception {
        final CaseTypeDefinition caseType = new CaseTypeDefinition();
        caseType.setId(CASE_TYPE_ID);
        final JurisdictionDefinition jurisdiction = new JurisdictionDefinition();
        jurisdiction.setId(JURISDICTION_ID);
        final String URL = String.format("/citizens/%s/jurisdictions/%s/case-types/%s/cases/%d/events", USER_ID,
            JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(UPDATE_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);


        final String token = generateEventToken(jdbcTemplate, USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE,
            UPDATE_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(event);
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));

        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(MODIFIED_CORRUPTED_DATA));

        stubFor(WireMock.post(urlMatching("/before-commit.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));
        stubFor(WireMock.post(urlMatching("/before-commit.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 422, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn404WhenPostCreateEventWithInvalidEventTokenForCaseworker() throws Exception {
        final String URL =
            String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases/%d/events", USER_ID, JURISDICTION_ID,
                CASE_TYPE_ID, CASE_REFERENCE);

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(UPDATE_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);

        final String token = generateEventToken(jdbcTemplate, OTHER_USER_ID, JURISDICTION_ID, CASE_TYPE_ID,
            CASE_REFERENCE, UPDATE_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(event);
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals("Did not catch invalid token", 404, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    public void shouldReturn404WhenPostCreateEventWithInvalidEventTokenForCitizen() throws Exception {
        final String URL = String.format("/citizens/%s/jurisdictions/%s/case-types/%s/cases/%d/events", USER_ID,
            JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(UPDATE_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);

        final String token = generateEventToken(jdbcTemplate, OTHER_USER_ID, JURISDICTION_ID, CASE_TYPE_ID,
            CASE_REFERENCE, UPDATE_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(event);
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals("Did not catch invalid token", 404, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_callback_cases.sql"})
    @Transactional()
    @Rollback(false)
    public void shouldCommitUpdatedCaseDetailsImmediatelyBeforeSendToPostCallback() throws Exception {
        final String URL =
            String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases/%d/events",USER_ID, JURISDICTION_ID,
                CASE_TYPE_ID, CASE_REFERENCE);

        Event event = anEvent().build();
        event.setEventId(UPDATE_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);

        CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        String token = generateEventToken(jdbcTemplate, USER_ID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE,
            UPDATE_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(event);
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));

        CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setData(JacksonUtils.convertValue(MODIFIED_DATA));
        callbackResponse.setSecurityClassification(PUBLIC);
        callbackResponse.setDataClassification(JacksonUtils.convertValue(DATA_CLASSIFICATION));

        stubFor(WireMock.post(urlMatching("/before-commit.*"))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));

        stubFor(WireMock.post(urlMatching("/after-commit.*"))
            .willReturn(ok()));

        // when
        MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        // then
        assertEquals(mvcResult.getResponse().getContentAsString(), 201, mvcResult.getResponse().getStatus());

        // read from db using another thread / transaction
        ExecutorService es = Executors.newSingleThreadExecutor();
        Future<?> future = es.submit(() -> {
            List<CaseDetails> caseDetailsList = jdbcTemplate.query("SELECT * FROM case_data", this::mapCaseData);
            CaseDetails savedCaseDetails = caseDetailsList.get(0);
            assertEquals("CaseUpdated", savedCaseDetails.getState());
            return null;
        });
        future.get(); // This will rethrow Exceptions and Errors as ExecutionException
    }

    private void stubForErrorCallbackResponse(final String url) throws JsonProcessingException {
        final CallbackResponse callbackResponse = new CallbackResponse();
        callbackResponse.setErrors(Collections.singletonList("Just a test"));

        stubFor(WireMock.post(urlMatching(url))
            .willReturn(okJson(mapper.writeValueAsString(callbackResponse)).withStatus(200)));
    }

    private int getPort() {
        return super.wiremockPort;
    }
}
