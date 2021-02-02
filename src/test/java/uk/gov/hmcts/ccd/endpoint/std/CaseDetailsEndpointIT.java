package uk.gov.hmcts.ccd.endpoint.std;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.AuditRepository;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageCollection;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.model.std.MessageQueueCandidate;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.collection.IsIn.isIn;
import static org.hamcrest.core.Every.everyItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PRIVATE;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseViewFieldBuilder.aViewField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.WizardPageBuilder.newWizardPage;

// too many legacy OperatorWrap occurrences on JSON strings so suppress until move to Java12+
@SuppressWarnings("checkstyle:OperatorWrap")
public class CaseDetailsEndpointIT extends WireMockBaseTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String CASE_TYPE = "TestAddressBookCase";
    private static final String CASE_TYPE_VALIDATE = "TestAddressBookCaseValidate";
    private static final String CASE_TYPE_VALIDATE_MULTI_PAGE = "TestAddressBookCaseValidateMultiPage";
    private static final String CASE_TYPE_NO_CREATE_CASE_ACCESS = "TestAddressBookCaseNoCreateCaseAccess";
    private static final String CASE_TYPE_NO_UPDATE_CASE_ACCESS = "TestAddressBookCaseNoUpdateCaseAccess";
    private static final String CASE_TYPE_NO_CREATE_EVENT_ACCESS = "TestAddressBookCaseNoCreateEventAccess";
    private static final String CASE_TYPE_NO_CREATE_FIELD_ACCESS = "TestAddressBookCaseNoCreateFieldAccess";
    private static final String CASE_TYPE_NO_UPDATE_FIELD_ACCESS = "TestAddressBookCaseNoUpdateFieldAccess";
    private static final String CASE_TYPE_NO_READ_FIELD_ACCESS = "TestAddressBookCaseNoReadFieldAccess";
    private static final String CASE_TYPE_NO_READ_CASE_TYPE_ACCESS = "TestAddressBookCaseNoReadCaseTypeAccess";
    private static final String JURISDICTION = "PROBATE";
    private static final String TEST_EVENT_ID = "TEST_EVENT";
    private static final String CREATE_EVENT_ID = "Create2";
    private static final String PRE_STATES_EVENT_ID = "HAS_PRE_STATES_EVENT";
    private static final String FAKE_EVENT_ID = "FAKE_EVENT";
    private static final String GET_CASES_AS_CASEWORKER =
        "/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/cases";
    private static final String GET_PAGINATED_SEARCH_METADATA =
        "/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/cases/pagination_metadata";
    private static final String GET_PAGINATED_SEARCH_METADATA_CITIZENS =
        "/citizens/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/cases/pagination_metadata";
    private static final String TEST_CASE_TYPE = "TestAddressBookCase";
    private static final String TEST_JURISDICTION = "PROBATE";
    private static final String TEST_STATE = "CaseCreated";
    private static final String UID = "123";
    private static final String DRAFT_ID = "5";
    private static final String CASE_TYPE_CREATOR_ROLE = "TestAddressBookCreatorCase";
    private static final String CASE_TYPE_CREATOR_ROLE_NO_CREATE_ACCESS = "TestAddressBookCreatorNoCreateAccessCase";
    private static final String DESCRIPTION = "A very long comment.......";
    private static final String SUMMARY = "Short comment";

    private static final String LONG_COMMENT = "A very long comment.......";
    private static final String SHORT_COMMENT = "Short comment";

    private static final String MID_EVENT_CALL_BACK = "/event-callback/mid-event";
    private static final String MID_EVENT_CALL_BACK_MULTI_PAGE = "/event-callback/multi-page-mid-event";

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;
    private JdbcTemplate template;

    @SpyBean
    private AuditRepository auditRepository;

    private static final String REFERENCE_2 = "1504259907353545";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        template = new JdbcTemplate(db);
    }

    @Test
    public void shouldReturn409WhenPostCreateCaseAndNonUniqueReferenceOccursTwiceForCaseworker() throws Exception {
        when(uidService.generateUID()).thenReturn(REFERENCE).thenReturn(REFERENCE).thenReturn(REFERENCE);
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";
        final JsonNode DATA = mapper.readTree("{}\n");
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID));
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));
        caseDetailsToSave.setToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID));

        // initial one returns 201
        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        // this should give 409
        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();
        assertEquals(mvcResult.getResponse().getContentAsString(), 409, mvcResult.getResponse().getStatus());

        verify(uidService, times(3)).generateUID();

        // we should still have one case in DB
        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());
    }

    @Test
    public void shouldReturn409WhenPostCreateCaseAndNonUniqueReferenceOccursTwiceForCitizen() throws Exception {
        when(uidService.generateUID()).thenReturn(REFERENCE).thenReturn(REFERENCE).thenReturn(REFERENCE);
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";
        final JsonNode DATA = mapper.readTree("{}\n");
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID));
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));
        caseDetailsToSave.setToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID));

        // initial one returns 201
        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        // this should give 409
        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();
        assertEquals(mvcResult.getResponse().getContentAsString(), 409, mvcResult.getResponse().getStatus());

        verify(uidService, times(3)).generateUID();

        // we should still have one case in DB
        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseAndSameReferenceFirstTimeButRetryIsUniqueForCaseworker()
                                                                                                    throws Exception {
        when(uidService.generateUID()).thenReturn(REFERENCE).thenReturn(REFERENCE).thenReturn(REFERENCE_2);
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";
        final JsonNode DATA = mapper.readTree("{}\n");
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID));
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));
        caseDetailsToSave.setToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID));

        // initial one returns 201
        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        // this should give 201 as the retry should have REFERENCE_2
        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();
        assertEquals(mvcResult.getResponse().getContentAsString(), 201, mvcResult.getResponse().getStatus());

        verify(uidService, times(3)).generateUID();

        // we should still have one case in DB
        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 2, caseDetailsList.size());
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseAndSameReferenceFirstTimeButRetryIsUniqueForCitizen()
                                                                                                    throws Exception {
        when(uidService.generateUID()).thenReturn(REFERENCE).thenReturn(REFERENCE).thenReturn(REFERENCE_2);
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";
        final JsonNode DATA = mapper.readTree("{}\n");
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID));
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));
        caseDetailsToSave.setToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID));

        // initial one returns 201
        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        // this should give 201 as the retry should have REFERENCE_2
        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();
        assertEquals(mvcResult.getResponse().getContentAsString(), 201, mvcResult.getResponse().getStatus());

        verify(uidService, times(3)).generateUID();

        // we should still have one case in DB
        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 2, caseDetailsList.size());
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseWithEmptyDataClassificationForCaseworker() throws Exception {
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";
        final JsonNode DATA = mapper.readTree(
            "{\n" +
                "  \"PersonFirstName\": \"First Name\",\n" +
                "  \"PersonLastName\": \"Last Name\",\n" +
                "  \"PersonAddress\": {\n" +
                "    \"AddressLine1\": \"Address Line 1\",\n" +
                "    \"AddressLine2\": \"Address Line 2\"\n" +
                "  },\n" +
                "  \"Aliases\": [{\"value\": \"x1\", \"id\": \"1\"}, {\"value\": \"x2\", \"id\": \"2\"}]," +
                "  \"D8Document\":{" +
                "    \"document_url\": \"http://localhost:" + getPort()
                + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\"}" +
                "}\n"
        );
        final JsonNode sanitizedData = mapper.readTree(
            "{\n" +
                "  \"PersonFirstName\": \"First Name\",\n" +
                "  \"PersonLastName\": \"Last Name\",\n" +
                "  \"PersonAddress\": {\n" +
                "    \"AddressLine1\": \"Address Line 1\",\n" +
                "    \"AddressLine2\": \"Address Line 2\"\n" +
                "  },\n" +
                "  \"Aliases\": [{\"value\": \"x1\", \"id\": \"1\"}, {\"value\": \"x2\", \"id\": \"2\"}]," +
                "  \"D8Document\":{\n" +
                "    \"document_url\": \"http://localhost:" + getPort()
                + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\",\n" +
                "    \"document_binary_url\": \"http://localhost:[port]/documents/"
                + "05e7cd7e-7041-4d8a-826a-7bb49dfd83d0/binary\",\n" +
                "    \"document_filename\": \"Seagulls_Square.jpg\"" +
                "}\n" +
                "}\n"
        );
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(TEST_EVENT_ID);
        Map data = JacksonUtils.convertValue(DATA);
        caseDetailsToSave.setData(data);
        caseDetailsToSave.setToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID));
        caseDetailsToSave.setDraftId(DRAFT_ID);


        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();
        assertEquals("Incorrect Response Status Code", 201, mvcResult.getResponse().getStatus());
        Map expectedSanitizedData = mapper.readValue(sanitizedData.toString(), Map.class);
        Map actualData = mapper.readValue(mapper.readTree(mvcResult.getResponse().getContentAsString())
            .get("case_data").toString(), Map.class);
        assertThat("Incorrect Response Content", actualData.entrySet(), everyItem(isIn(expectedSanitizedData
            .entrySet())));

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertTrue("Incorrect Case Reference", uidService.validateUID(String.valueOf(savedCaseDetails
            .getReference())));
        assertEquals("Incorrect Case Type", CASE_TYPE, savedCaseDetails.getCaseTypeId());
        Map sanitizedDataMap = JacksonUtils.convertValue(sanitizedData);
        assertThat("Incorrect Data content", savedCaseDetails.getData().entrySet(), everyItem(isIn(
            sanitizedDataMap.entrySet())));
        assertEquals("Incorrect security classification size", 5, savedCaseDetails.getDataClassification().size());
        JsonNode expectedClassification = mapper.readTree(
            "{" +
                "  \"PersonAddress\":{" +
                "    \"classification\": \"PUBLIC\"," +
                "    \"value\": {" +
                "      \"AddressLine1\":\"PUBLIC\"," +
                "      \"AddressLine2\":\"PUBLIC\"" +
                "    }" +
                "  }," +
                "  \"PersonLastName\":\"PUBLIC\"," +
                "  \"PersonFirstName\":\"PUBLIC\"," +
                "  \"Aliases\": {" +
                "    \"classification\": \"PUBLIC\"," +
                "    \"value\": [" +
                "      { \"id\": \"1\", \"classification\": \"PUBLIC\" },"
                + "      { \"id\": \"2\", \"classification\": \"PUBLIC\" }"
                + "    ]" +
                "  }," +
                "  \"D8Document\":\"PUBLIC\"" +
                "}"
        );
        JsonNode actualClassification = JacksonUtils.convertValueJsonNode(savedCaseDetails.getDataClassification());
        assertEquals("Incorrect security classifications", expectedClassification, actualClassification);
        assertEquals("state3", savedCaseDetails.getState());

        final List<AuditEvent> caseAuditEventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("Incorrect number of case events", 1, caseAuditEventList.size());

        final List<MessageQueueCandidate> messageQueueList =
            template.query("SELECT * FROM message_queue_candidates", this::mapMessageCandidate);
        assertEquals("Incorrect number of rows in messageQueue", 1, messageQueueList.size());

        // Assertion belows are for creation event
        final AuditEvent caseAuditEvent = caseAuditEventList.get(0);
        assertEquals("123", caseAuditEvent.getUserId());
        assertEquals("Strife", caseAuditEvent.getUserLastName());
        assertEquals("Cloud", caseAuditEvent.getUserFirstName());
        assertEquals("TEST EVENT NAME", caseAuditEvent.getEventName());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals(savedCaseDetails.getCreatedDate(), caseAuditEvent.getCreatedDate());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertEquals(savedCaseDetails.getDataClassification(), caseAuditEvent.getDataClassification());
        assertThat(caseAuditEvent.getSecurityClassification(), equalTo(PRIVATE));

        assertThat(messageQueueList.get(0).getMessageInformation().findPath("case_id").toString(),
            containsString(savedCaseDetails.getId()));
        assertThat(messageQueueList.get(0).getMessageInformation().findPath("user_id").toString(),
            containsString(caseAuditEvent.getUserId()));
        assertThat(messageQueueList.get(0).getMessageInformation().findPath("event_id").toString(),
            containsString(caseAuditEvent.getEventId()));
        assertThat(messageQueueList.get(0).getMessageInformation().findPath("case_type_id").toString(),
            containsString(savedCaseDetails.getCaseTypeId()));
        assertThat(messageQueueList.get(0).getMessageInformation().findPath("new_state_id").toString(),
            containsString(savedCaseDetails.getState()));
        assertThat(messageQueueList.get(0).getMessageInformation().findPath("event_instance_id").toString(),
            containsString(caseAuditEvent.getId().toString()));
        assertEquals("null", messageQueueList.get(0).getMessageInformation().findPath("previous_state_id").toString());
        assertThat(messageQueueList.get(0).getId(), equalTo(1L));
        assertEquals("CASE_EVENT", messageQueueList.get(0).getMessageType());
    }

    @Test
    public void shouldGenerateCaseEventMessagingDefinition() throws Exception {
        String caseType = "MessagePublishing";
        String eventId = "CREATE";
        String url = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + caseType + "/cases";
        final JsonNode DATA = mapper.readTree("{\n"
            + "  \"MoneyGBPField\": \"1000\",\n"
            + "  \"FixedListField\": \"VALUE3\",\n"
            + "  \"AddressUKField\": {\n"
            + "    \"AddressLine1\": null,\n"
            + "    \"AddressLine2\": null,\n"
            + "    \"AddressLine3\": null,\n"
            + "    \"PostTown\": null,\n"
            + "    \"County\": null,\n"
            + "    \"PostCode\": null,\n"
            + "    \"Country\": null\n"
            + "  },\n"
            + "  \"ComplexField\": {\n"
            + "  \"ComplexTextField\": \"text field\",\n"
            + "   \"ComplexFixedListField\": null,\n"
            + "    \"ComplexNestedField\": {\n"
            + "      \"NestedNumberField\": null,\n"
            + "      \"NestedCollectionTextField\": []\n"
            + "    }\n"
            + "  },\n"
            + "  \"DateTimeField\": \"2000-01-01T11:11:11.000\",\n"
            + "  \"PhoneUKField\": \"09876528531\",\n"
            + "  \"NumberField\": 90,\n"
            + "  \"MultiSelectListField\": [\n"
            + "    \"OPTION4\",\n"
            + "    \"OPTION2\"\n"
            + "  ],\n"
            + "  \"YesOrNoField\": \"No\",\n"
            + "  \"EmailField\": \"test@test.com\",\n"
            + "  \"TextField\": \"text\",\n"
            + "  \"DateField\": \"2000-01-01\",\n"
            + "  \"TextAreaField\": \"text areas\"\n"
            + "}");


        Map data = JacksonUtils.convertValue(DATA);
        CaseDataContent caseDetailsToSave = newCaseDataContent()
            .withEvent(anEvent().withEventId(eventId).build())
            .withData(data)
            .withToken(generateEventTokenNewCase(UID, JURISDICTION, caseType, eventId))
            .build();

        MvcResult mvcResult = mockMvc.perform(post(url)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();
        assertEquals("Incorrect Response Status Code", 201, mvcResult.getResponse().getStatus());

        List<MessageQueueCandidate> messageQueueList =
            template.query("SELECT * FROM message_queue_candidates", this::mapMessageCandidate);
        assertEquals("Incorrect number of rows in messageQueue", 1, messageQueueList.size());

        assertEquals(messageQueueList.get(0).getMessageInformation().get("additional_data").get("Definition"),
            mapper.readTree("{\n"
                + "    \"OtherAlias\": {\n"
                + "        \"type\": \"SimpleNumber\",\n"
                + "        \"subtype\": \"Number\",\n"
                + "        \"typeDef\": null,\n"
                + "        \"originalId\": \"ComplexNestedField.NestedNumberField\"\n"
                + "    },\n"
                + "    \"NumberField\": {\n"
                + "        \"type\": \"SimpleNumber\",\n"
                + "        \"subtype\": \"Number\",\n"
                + "        \"typeDef\": null,\n"
                + "        \"originalId\": \"NumberField\"\n"
                + "    },\n"
                + "    \"ComplexField\": {\n"
                + "        \"type\": \"Complex\",\n"
                + "        \"subtype\": \"ComplexType\",\n"
                + "        \"typeDef\": {\n"
                + "            \"ComplexTextField\": {\n"
                + "                \"type\": \"SimpleText\",\n"
                + "                \"subtype\": \"Text\",\n"
                + "                \"typeDef\": null,\n"
                + "                \"originalId\": \"ComplexTextField\"\n"
                + "            },\n"
                + "            \"ComplexNestedField\": {\n"
                + "                \"type\": \"Complex\",\n"
                + "                \"subtype\": \"NestedComplexType\",\n"
                + "                \"typeDef\": {\n"
                + "                    \"NestedNumberField\": {\n"
                + "                        \"type\": \"SimpleNumber\",\n"
                + "                        \"subtype\": \"Number\",\n"
                + "                        \"typeDef\": null,\n"
                + "                        \"originalId\": \"NestedNumberField\"\n"
                + "                    }\n"
                + "                },\n"
                + "                \"originalId\": \"ComplexNestedField\"\n"
                + "            }\n"
                + "        },\n"
                + "        \"originalId\": \"ComplexField\"\n"
                + "    },\n"
                + "    \"YesOrNoField\": {\n"
                + "        \"type\": \"SimpleBoolean\",\n"
                + "        \"subtype\": \"YesOrNo\",\n"
                + "        \"typeDef\": null,\n"
                + "        \"originalId\": \"YesOrNoField\"\n"
                + "    },\n"
                + "    \"DateTimeField\": {\n"
                + "        \"type\": \"SimpleDateTime\",\n"
                + "        \"subtype\": \"DateTime\",\n"
                + "        \"typeDef\": null,\n"
                + "        \"originalId\": \"DateTimeField\"\n"
                + "    },\n"
                + "    \"DocumentField\": {\n"
                + "        \"type\": \"Complex\",\n"
                + "        \"subtype\": \"Document\",\n"
                + "        \"typeDef\": {\n"
                + "            \"document_url\": {\n"
                + "                \"type\": \"SimpleText\",\n"
                + "                \"subtype\": \"Text\",\n"
                + "                \"typeDef\": null,\n"
                + "                \"originalId\": \"document_url\"\n"
                + "            },\n"
                + "            \"document_filename\": {\n"
                + "                \"type\": \"SimpleText\",\n"
                + "                \"subtype\": \"Text\",\n"
                + "                \"typeDef\": null,\n"
                + "                \"originalId\": \"document_filename\"\n"
                + "            },\n"
                + "            \"document_binary_url\": {\n"
                + "                \"type\": \"SimpleText\",\n"
                + "                \"subtype\": \"Text\",\n"
                + "                \"typeDef\": null,\n"
                + "                \"originalId\": \"document_binary_url\"\n"
                + "            }\n"
                + "        },\n"
                + "        \"originalId\": \"DocumentField\"\n"
                + "    },\n"
                + "    \"AddressUKField\": {\n"
                + "        \"type\": \"Complex\",\n"
                + "        \"subtype\": \"AddressUK\",\n"
                + "        \"typeDef\": {\n"
                + "            \"County\": {\n"
                + "                \"type\": \"SimpleText\",\n"
                + "                \"subtype\": \"Text\",\n"
                + "                \"typeDef\": null,\n"
                + "                \"originalId\": \"County\"\n"
                + "            },\n"
                + "            \"Country\": {\n"
                + "                \"type\": \"SimpleText\",\n"
                + "                \"subtype\": \"Text\",\n"
                + "                \"typeDef\": null,\n"
                + "                \"originalId\": \"Country\"\n"
                + "            },\n"
                + "            \"PostCode\": {\n"
                + "                \"type\": \"SimpleText\",\n"
                + "                \"subtype\": \"Text\",\n"
                + "                \"typeDef\": null,\n"
                + "                \"originalId\": \"PostCode\"\n"
                + "            },\n"
                + "            \"PostTown\": {\n"
                + "                \"type\": \"SimpleText\",\n"
                + "                \"subtype\": \"Text\",\n"
                + "                \"typeDef\": null,\n"
                + "                \"originalId\": \"PostTown\"\n"
                + "            },\n"
                + "            \"AddressLine1\": {\n"
                + "                \"type\": \"SimpleText\",\n"
                + "                \"subtype\": \"Text\",\n"
                + "                \"typeDef\": null,\n"
                + "                \"originalId\": \"AddressLine1\"\n"
                + "            },\n"
                + "            \"AddressLine2\": {\n"
                + "                \"type\": \"SimpleText\",\n"
                + "                \"subtype\": \"Text\",\n"
                + "                \"typeDef\": null,\n"
                + "                \"originalId\": \"AddressLine2\"\n"
                + "            },\n"
                + "            \"AddressLine3\": {\n"
                + "                \"type\": \"SimpleText\",\n"
                + "                \"subtype\": \"Text\",\n"
                + "                \"typeDef\": null,\n"
                + "                \"originalId\": \"AddressLine3\"\n"
                + "            }\n"
                + "        },\n"
                + "        \"originalId\": \"AddressUKField\"\n"
                + "    },\n"
                + "    \"CollectionField\": {\n"
                + "        \"type\": \"Collection\",\n"
                + "        \"subtype\": \"Text\",\n"
                + "        \"typeDef\": null,\n"
                + "        \"originalId\": \"CollectionField\"\n"
                + "    },\n"
                + "    \"TopLevelPublish\": {\n"
                + "        \"type\": \"SimpleText\",\n"
                + "        \"subtype\": \"Text\",\n"
                + "        \"typeDef\": null,\n"
                + "        \"originalId\": \"ComplexTextField\"\n"
                + "    },\n"
                + "    \"AliasForTextField\": {\n"
                + "        \"type\": \"SimpleText\",\n"
                + "        \"subtype\": \"Text\",\n"
                + "        \"typeDef\": null,\n"
                + "        \"originalId\": \"TextField\"\n"
                + "    },\n"
                + "    \"ComplexCollectionField\": {\n"
                + "        \"type\": \"Collection\",\n"
                + "        \"subtype\": \"ComplexType\",\n"
                + "        \"typeDef\": {\n"
                + "            \"ComplexTextField\": {\n"
                + "                \"type\": \"SimpleText\",\n"
                + "                \"subtype\": \"Text\",\n"
                + "                \"typeDef\": null,\n"
                + "                \"originalId\": \"ComplexTextField\"\n"
                + "            },\n"
                + "            \"ComplexNestedField\": {\n"
                + "                \"type\": \"Complex\",\n"
                + "                \"subtype\": \"NestedComplexType\",\n"
                + "                \"typeDef\": {\n"
                + "                    \"NestedNumberField\": {\n"
                + "                        \"type\": \"SimpleNumber\",\n"
                + "                        \"subtype\": \"Number\",\n"
                + "                        \"typeDef\": null,\n"
                + "                        \"originalId\": \"NestedNumberField\"\n"
                + "                    },\n"
                + "                    \"NestedCollectionTextField\": {\n"
                + "                        \"type\": \"Collection\",\n"
                + "                        \"subtype\": \"Text\",\n"
                + "                        \"typeDef\": null,\n"
                + "                        \"originalId\": \"NestedCollectionTextField\"\n"
                + "                    }\n"
                + "                },\n"
                + "                \"originalId\": \"ComplexNestedField\"\n"
                + "            },\n"
                + "            \"ComplexFixedListField\": {\n"
                + "                \"type\": \"FixedList\",\n"
                + "                \"subtype\": \"FixedList\",\n"
                + "                \"typeDef\": null,\n"
                + "                \"originalId\": \"ComplexFixedListField\"\n"
                + "            }\n"
                + "        },\n"
                + "        \"originalId\": \"ComplexCollectionField\"\n"
                + "    }\n"
                + "}"));
    }

    @Test
    public void shouldGenerateCaseEventDataMessagingDefinition() throws Exception {
        String caseType = "MessagePublishing";
        String eventId = "CREATE";
        String url = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + caseType + "/cases";

        final JsonNode DATA = mapper.readTree("{\n"
            + "  \"MoneyGBPField\": \"1000\",\n"
            + "  \"FixedListField\": \"VALUE3\",\n"
            + "  \"AddressUKField\": {\n"
            + "    \"AddressLine1\": \"123 street name\",\n"
            + "    \"AddressLine2\": \"\",\n"
            + "    \"AddressLine3\": \"\",\n"
            + "    \"PostTown\": \"town\",\n"
            + "    \"County\": \"county\",\n"
            + "    \"PostCode\": \"postcode\",\n"
            + "    \"Country\": \"\"\n"
            + "  },\n"
            + "  \"ComplexField\": {\n"
            + "    \"ComplexTextField\": \"text in complex\",\n"
            + "    \"ComplexFixedListField\": \"VALUE3\",\n"
            + "    \"ComplexNestedField\": {\n"
            + "      \"NestedNumberField\": \"1\",\n"
            + "      \"NestedCollectionTextField\": [\n"
            + "        {\n"
            + "          \"value\": \"collection of text in nested complex 1\",\n"
            + "          \"id\": \"62c18dd8-d6d2-4378-b940-8614ee1ab25a\"\n"
            + "        },\n"
            + "        {\n"
            + "          \"value\": \"collection of text  in nested complex 2\",\n"
            + "          \"id\": \"4acd46b4-f292-4e5d-a436-16dcca6b2cfe\"\n"
            + "        }\n"
            + "      ]\n"
            + "    }\n"
            + "  },\n"
            + "  \"DateTimeField\": \"2000-12-12T11:11:11.000\",\n"
            + "  \"PhoneUKField\": \"07986542987\",\n"
            + "  \"NumberField\": \"2\",\n"
            + "  \"MultiSelectListField\": [\n"
            + "    \"OPTION4\",\n"
            + "    \"OPTION3\"\n"
            + "  ],\n"
            + "  \"YesOrNoField\": \"Yes\",\n"
            + "  \"EmailField\": \"test@test.com\",\n"
            + "  \"TextField\": \"text field\",\n"
            + "  \"DateField\": \"2000-12-12\",\n"
            + "  \"TextAreaField\": \"text area\",\n"
            + "  \"CollectionField\": [\n"
            + "    {\n"
            + "      \"value\": \"collection field\",\n"
            + "      \"id\": \"9af355b6-19ef-4a19-b5db-ad873772b478\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"value\": \"collection field 2\",\n"
            + "      \"id\": \"7bce938e-7400-424f-86c9-c896ecbabc1f\"\n"
            + "    }\n"
            + "  ]\n"
            + "}");


        Map data = JacksonUtils.convertValue(DATA);
        CaseDataContent caseDetailsToSave = newCaseDataContent()
            .withEvent(anEvent().withEventId(eventId).build())
            .withData(data)
            .withToken(generateEventTokenNewCase(UID, JURISDICTION, caseType, eventId))
            .build();

        MvcResult mvcResult = mockMvc.perform(post(url)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();
        assertEquals("Incorrect Response Status Code", 201, mvcResult.getResponse().getStatus());

        List<MessageQueueCandidate> messageQueueList =
            template.query("SELECT * FROM message_queue_candidates", this::mapMessageCandidate);
        assertEquals("Incorrect number of rows in messageQueue", 1, messageQueueList.size());

        assertEquals(mapper.readTree("{\n"
                + "  \"OtherAlias\": 1,\n"
                + "  \"NumberField\": 2,\n"
                + "  \"ComplexField\": {\n"
                + "    \"ComplexTextField\": \"text in complex\",\n"
                + "    \"ComplexNestedField\": {\n"
                + "      \"NestedNumberField\": 1\n"
                + "    }\n"
                + "  },\n"
                + "  \"YesOrNoField\": true,\n"
                + "  \"DateTimeField\": \"2000-12-12T11:11:11.000\",\n"
                + "  \"DocumentField\": null,\n"
                + "  \"AddressUKField\": {\n"
                + "    \"County\": \"county\",\n"
                + "    \"Country\": \"\",\n"
                + "    \"PostCode\": \"postcode\",\n"
                + "    \"PostTown\": \"town\",\n"
                + "    \"AddressLine1\": \"123 street name\",\n"
                + "    \"AddressLine2\": \"\",\n"
                + "    \"AddressLine3\": \"\"\n"
                + "  },\n"
                + "  \"CollectionField\": [\n"
                + "    {\n"
                + "      \"id\": \"9af355b6-19ef-4a19-b5db-ad873772b478\",\n"
                + "      \"value\": \"collection field\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"id\": \"7bce938e-7400-424f-86c9-c896ecbabc1f\",\n"
                + "      \"value\": \"collection field 2\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"TopLevelPublish\": \"text in complex\",\n"
                + "  \"AliasForTextField\": \"text field\",\n"
                + "  \"ComplexCollectionField\": null\n"
                + "}"),
            messageQueueList.get(0).getMessageInformation().get("additional_data").get("Data"));
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseWithEmptyDataClassificationForCitizen() throws Exception {

        final String URL =
            "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases?ignore-warning=true";
        final JsonNode DATA = mapper.readTree(
            "{\n"
                + "  \"PersonFirstName\": \"First Name\",\n"
                + "  \"PersonLastName\": \"Last Name\",\n"
                + "  \"PersonAddress\": {\n"
                + "    \"AddressLine1\": \"Address Line 1\",\n"
                + "    \"AddressLine2\": \"Address Line 2\"\n"
                + "  },\n"
                + "\"D8Document\":{"
                + "\"document_url\": \"http://localhost:" + getPort()
                + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\"}"
                + "}\n"
        );
        final JsonNode sanitizedData = mapper.readTree(
            "{\n"
                + "  \"PersonFirstName\": \"First Name\",\n"
                + "  \"PersonLastName\": \"Last Name\",\n"
                + "  \"PersonAddress\": {\n"
                + "    \"AddressLine1\": \"Address Line 1\",\n"
                + "    \"AddressLine2\": \"Address Line 2\"\n"
                + "  },\n"
                + "  \"D8Document\":{\n"
                + "    \"document_url\": \"http://localhost:" + getPort()
                + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\",\n"
                + "    \"document_binary_url\": \"http://localhost:[port]/documents/"
                + "05e7cd7e-7041-4d8a-826a-7bb49dfd83d0/binary\",\n"
                + "    \"document_filename\": \"Seagulls_Square.jpg\""
                + "}\n"
                + "}\n"
        );
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(LONG_COMMENT);
        triggeringEvent.setSummary(SHORT_COMMENT);
        caseDetailsToSave.setEvent(triggeringEvent);
        Map data = JacksonUtils.convertValue(DATA);
        caseDetailsToSave.setData(data);
        caseDetailsToSave.setToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID));

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();
        assertEquals("Incorrect Response Status Code", 201, mvcResult.getResponse().getStatus());
        Map expectedSanitizedData = mapper.readValue(sanitizedData.toString(), Map.class);
        Map actualData = mapper.readValue(mapper.readTree(mvcResult.getResponse().getContentAsString())
            .get("case_data").toString(), Map.class);
        assertThat("Incorrect Response Content", actualData.entrySet(), everyItem(isIn(expectedSanitizedData
            .entrySet())));

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertTrue("Incorrect Case Reference", uidService.validateUID(String.valueOf(savedCaseDetails
            .getReference())));
        assertEquals("Incorrect Case Type", CASE_TYPE, savedCaseDetails.getCaseTypeId());
        Map sanitizedDataMap = JacksonUtils.convertValue(sanitizedData);
        assertThat("Incorrect Data content", savedCaseDetails.getData().entrySet(), everyItem(isIn(
            sanitizedDataMap.entrySet())));
        assertEquals("Incorrect security classification size", 4, savedCaseDetails.getDataClassification().size());
        JsonNode expectedClassification = mapper.readTree("{" +
            "  \"PersonAddress\":{" +
            "    \"classification\": \"PUBLIC\"," +
            "    \"value\":{" +
            "      \"AddressLine1\":\"PUBLIC\"," +
            "      \"AddressLine2\":\"PUBLIC\"" +
            "    }" +
            "  }," +
            "  \"PersonLastName\":\"PUBLIC\"," +
            "  \"PersonFirstName\":\"PUBLIC\"," +
            "  \"D8Document\":\"PUBLIC\"" +
            "}");
        JsonNode actualClassification = JacksonUtils.convertValueJsonNode(savedCaseDetails.getDataClassification());
        assertEquals("Incorrect security classifications", expectedClassification, actualClassification);

        final List<AuditEvent> caseAuditEventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("Incorrect number of case events", 1, caseAuditEventList.size());

        final AuditEvent caseAuditEvent = caseAuditEventList.get(0);
        assertEquals("123", caseAuditEvent.getUserId());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals("Case in state 3", caseAuditEvent.getStateName());
        assertEquals(savedCaseDetails.getCreatedDate(), caseAuditEvent.getCreatedDate());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertEquals("Event ID", TEST_EVENT_ID, caseAuditEvent.getEventId());
        assertEquals("Long Comment", LONG_COMMENT, caseAuditEvent.getDescription());
        assertEquals("Short Comment", SHORT_COMMENT, caseAuditEvent.getSummary());
        assertEquals(savedCaseDetails.getDataClassification(), caseAuditEvent.getDataClassification());
        assertThat(caseAuditEvent.getSecurityClassification(), equalTo(PRIVATE));
    }

    private int getPort() {
        return super.wiremockPort;
    }

    @Test
    public void shouldReturn422WhenPostCreateCaseWithMissingDocumentBinaryLinkForCaseworker() throws Exception {
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";
        final JsonNode DATA = mapper.readTree(
            "{\n"
                + "  \"PersonFirstName\": \"First Name\",\n"
                + "  \"PersonLastName\": \"Last Name\",\n"
                + "  \"PersonAddress\": {\n"
                + "    \"AddressLine1\": \"Address Line 1\",\n"
                + "    \"AddressLine2\": \"Address Line 2\"\n"
                + "  },\n"
                + "\"D8Document\":{"
                + "\"document_url\": \"http://localhost:" + getPort()
                + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1\"}"
                + "}\n"
        );

        Map data = JacksonUtils.convertValue(DATA);
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID));
        caseDetailsToSave.setData(data);
        caseDetailsToSave.setToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID));


        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();
        assertEquals("Incorrect Response Status Code", 422, mvcResult.getResponse().getStatus());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", 0, caseDetailsList.size());

    }

    @Test
    public void shouldReturn422WhenPostCreateCaseWithMissingDocumentBinaryLinkForCitizen() throws Exception {

        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
            + "/cases?ignore-warning=true";
        final JsonNode DATA = mapper.readTree(
            "{\n"
                + "  \"PersonFirstName\": \"First Name\",\n"
                + "  \"PersonLastName\": \"Last Name\",\n"
                + "  \"PersonAddress\": {\n"
                + "    \"AddressLine1\": \"Address Line 1\",\n"
                + "    \"AddressLine2\": \"Address Line 2\"\n"
                + "  },\n"
                + "\"D8Document\":{"
                + "\"document_url\": \"http://localhost:" + getPort()
                + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1\"}"
                + "}\n"
        );
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(LONG_COMMENT);
        triggeringEvent.setSummary(SHORT_COMMENT);
        caseDetailsToSave.setEvent(triggeringEvent);
        Map data = JacksonUtils.convertValue(DATA);
        caseDetailsToSave.setData(data);
        caseDetailsToSave.setToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID));

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();
        assertEquals("Incorrect Response Status Code", 422, mvcResult.getResponse().getStatus());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", 0, caseDetailsList.size());
    }


    @Test
    public void shouldReturn422WhenPostCreateCaseWithInvalidDocumentUrlDomainForCaseworker() throws Exception {
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";
        final JsonNode DATA = mapper.readTree(
            "{\n" +
                "  \"PersonFirstName\": \"First Name\",\n" +
                "  \"PersonLastName\": \"Last Name\",\n" +
                "  \"PersonAddress\": {\n" +
                "    \"AddressLine1\": \"Address Line 1\",\n" +
                "    \"AddressLine2\": \"Address Line 2\"\n" +
                "  },\n" +
                "\"D8Document\":{" +
                "\"document_url\": \"http://incorrect_domain:incorrect_port/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1\"}" +
                "}\n"
        );

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(TEST_EVENT_ID);
        Map data = JacksonUtils.convertValue(DATA);
        caseDetailsToSave.setData(data);
        caseDetailsToSave.setToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID));


        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();
        assertEquals("Incorrect Response Status Code", 422, mvcResult.getResponse().getStatus());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", 0, caseDetailsList.size());

    }

    @Test
    public void shouldReturn422WhenPostCreateCaseWithInvalidDocumentUrlDomainForCitizen() throws Exception {

        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
            + "/cases?ignore-warning=true";
        final JsonNode DATA = mapper.readTree(
            "{\n" +
                "  \"PersonFirstName\": \"First Name\",\n" +
                "  \"PersonLastName\": \"Last Name\",\n" +
                "  \"PersonAddress\": {\n" +
                "    \"AddressLine1\": \"Address Line 1\",\n" +
                "    \"AddressLine2\": \"Address Line 2\"\n" +
                "  },\n" +
                "\"D8Document\":{" +
                "\"document_url\": \"http://incorrect_domain:incorrect_port/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1\"}" +
                "}\n"
        );
        Map data = JacksonUtils.convertValue(DATA);
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setData(data);
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID,SHORT_COMMENT, LONG_COMMENT));
        caseDetailsToSave.setToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID));

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();
        assertEquals("Incorrect Response Status Code", 422, mvcResult.getResponse().getStatus());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", 0, caseDetailsList.size());
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseWithNoDataForCaseworker() throws Exception {
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID, SUMMARY, DESCRIPTION));
        caseDetailsToSave.setToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID));

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Expected empty case data", "", mapper.readTree(mvcResult.getResponse().getContentAsString())
            .get("case_data").asText());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertTrue("Incorrect Case Reference", uidService.validateUID(String.valueOf(savedCaseDetails
            .getReference())));
        assertEquals("Incorrect Case Type", CASE_TYPE, savedCaseDetails.getCaseTypeId());
        assertEquals("Incorrect Data content", "{}", savedCaseDetails.getData().toString());
        assertEquals("state3", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getLastStateModifiedDate());

        final List<AuditEvent> caseAuditEventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("Incorrect number of case events", 1, caseAuditEventList.size());

        final AuditEvent caseAuditEvent = caseAuditEventList.get(0);
        assertEquals("123", caseAuditEvent.getUserId());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals("Case in state 3", caseAuditEvent.getStateName());
        assertEquals(savedCaseDetails.getCreatedDate(), caseAuditEvent.getCreatedDate());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertEquals("Event ID", TEST_EVENT_ID, caseAuditEvent.getEventId());
        assertEquals("Description", LONG_COMMENT, caseAuditEvent.getDescription());
        assertEquals("Summary", SHORT_COMMENT, caseAuditEvent.getSummary());
        assertTrue(caseAuditEvent.getDataClassification().isEmpty());
        assertThat(caseAuditEvent.getSecurityClassification(), equalTo(PRIVATE));
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseWithNoDataForCitizen() throws Exception {
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID, SUMMARY, DESCRIPTION));
        caseDetailsToSave.setToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID));

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Expected empty case data", "", mapper.readTree(mvcResult.getResponse().getContentAsString())
            .get("case_data").asText());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertTrue("Incorrect Case Reference", uidService.validateUID(String.valueOf(savedCaseDetails
            .getReference())));
        assertEquals("Incorrect Case Type", CASE_TYPE, savedCaseDetails.getCaseTypeId());
        assertEquals("Incorrect Data content", "{}", savedCaseDetails.getData().toString());
        assertEquals("state3", savedCaseDetails.getState());

        final List<AuditEvent> caseAuditEventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("Incorrect number of case events", 1, caseAuditEventList.size());

        final AuditEvent caseAuditEvent = caseAuditEventList.get(0);
        assertEquals("123", caseAuditEvent.getUserId());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals("Case in state 3", caseAuditEvent.getStateName());
        assertEquals(savedCaseDetails.getCreatedDate(), caseAuditEvent.getCreatedDate());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertEquals("Event ID", TEST_EVENT_ID, caseAuditEvent.getEventId());
        assertEquals("Description", LONG_COMMENT, caseAuditEvent.getDescription());
        assertEquals("Summary", SHORT_COMMENT, caseAuditEvent.getSummary());
        assertTrue(caseAuditEvent.getDataClassification().isEmpty());
        assertThat(caseAuditEvent.getSecurityClassification(), equalTo(PRIVATE));
    }

    @Test
    public void saveCaseDetailsForCaseWorkerShouldLogAudit() throws Exception {
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID, SUMMARY, DESCRIPTION));
        caseDetailsToSave.setToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID));

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertTrue("Incorrect Case Reference", uidService.validateUID(String.valueOf(savedCaseDetails
            .getReference())));

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.CREATE_CASE.getLabel()));
        assertTrue("Incorrect Case Reference", uidService.validateUID(captor.getValue().getCaseId()));
        assertThat(captor.getValue().getIdamId(), is(UID));
        assertThat(captor.getValue().getInvokingService(), is(MockUtils.CCD_GW));
        assertThat(captor.getValue().getHttpStatus(), is(201));
        assertThat(captor.getValue().getCaseType(), is(CASE_TYPE));
        assertThat(captor.getValue().getJurisdiction(), is(JURISDICTION));
        assertThat(captor.getValue().getEventSelected(), is(TEST_EVENT_ID));
    }

    @Test
    public void saveCaseDetailsForCitizenShouldLogAudit() throws Exception {
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID, SUMMARY, DESCRIPTION));
        caseDetailsToSave.setToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID));

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.CREATE_CASE.getLabel()));
        assertTrue("Incorrect Case Reference", uidService.validateUID(captor.getValue().getCaseId()));
        assertThat(captor.getValue().getIdamId(), is(UID));
        assertThat(captor.getValue().getInvokingService(), is(MockUtils.CCD_GW));
        assertThat(captor.getValue().getHttpStatus(), is(201));
        assertThat(captor.getValue().getCaseType(), is(CASE_TYPE));
        assertThat(captor.getValue().getJurisdiction(), is(JURISDICTION));
        assertThat(captor.getValue().getEventSelected(), is(TEST_EVENT_ID));
    }

    @Test
    public void shouldReturn201WithEmptyBodyWhenPostCreateCaseWithNoReadAccessOnCaseTypeForCaseworker()
                                                                                                    throws Exception {
        shouldReturn201WithEmptyBodyWhenPostCreateCaseWithNoReadAccessOnCaseType("caseworkers");
    }

    @Test
    public void shouldReturn201WithEmptyBodyWhenPostCreateCaseWithNoReadAccessOnCaseTypeForCitizen() throws Exception {
        shouldReturn201WithEmptyBodyWhenPostCreateCaseWithNoReadAccessOnCaseType("citizens");
    }

    @Test
    public void shouldReturn201WithFieldRemovedWhenPostCreateCaseWithNoFieldReadAccessForCaseworker()
                                                                                                    throws Exception {
        shouldReturn201WithFieldRemovedWhenPostCreateCaseWithNoFieldReadAccess("caseworkers");
    }

    @Test
    public void shouldReturn201WithFieldRemovedWhenPostCreateCaseWithNoFieldReadAccessForCitizen() throws Exception {
        shouldReturn201WithFieldRemovedWhenPostCreateCaseWithNoFieldReadAccess("citizens");
    }

    @Test
    public void shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccessForCaseworker() throws Exception {
        shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccess("caseworkers");
    }

    @Test
    public void shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccessForCitizen() throws Exception {
        shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccess("citizens");
    }

    @Test
    public void shouldReturn404WhenPostCreateCaseWithNoCreateEventAccessForCaseworker() throws Exception {
        shouldReturn404WhenPostCreateCaseWithNoCreateEventAccess("caseworkers");
    }

    @Test
    public void shouldReturn404WhenPostCreateCaseWithNoCreateEventAccessForCitizen() throws Exception {
        shouldReturn404WhenPostCreateCaseWithNoCreateEventAccess("citizens");
    }

    @Test
    public void shouldReturn404WhenPostCreateCaseWithNoCreateFieldAccessForCaseworker() throws Exception {
        shouldReturn404WhenPostCreateCaseWithNoCreateFieldAccess("caseworkers");
    }

    @Test
    public void shouldReturn404WhenPostCreateCaseWithNoCreateFieldAccessForCitizen() throws Exception {
        shouldReturn404WhenPostCreateCaseWithNoCreateFieldAccess("citizens");
    }

    @Test
    public void shouldReturn404WhenPostCreateCaseWithNoEventForCaseworker() throws Exception {
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave)))
            .andExpect(status().is(404));

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_data", resultSet -> {
            assertEquals(0, resultSet.getInt(1));
        });
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(0, resultSet.getInt(1));
        });
    }

    @Test
    public void shouldReturn404WhenPostCreateCaseWithNoEventForCitizen() throws Exception {
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave)))
            .andExpect(status().is(404));

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_data", resultSet -> {
            assertEquals(0, resultSet.getInt(1));
        });
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(0, resultSet.getInt(1));
        });
    }

    @Test
    public void shouldReturn404WhenPostCreateCaseWithNoEventIdForCaseworker() throws Exception {
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";
        caseDetailsToSave.setEvent(anEvent().build());

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave)))
            .andExpect(status().is(404));

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_data", resultSet -> {
            assertEquals(0, resultSet.getInt(1));
        });
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(0, resultSet.getInt(1));
        });
    }

    @Test
    public void shouldReturn404WhenPostCreateCaseWithNoEventIdForCitizen() throws Exception {
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";
        caseDetailsToSave.setEvent(anEvent().build());

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave)))
            .andExpect(status().is(404));

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_data", resultSet -> {
            assertEquals(0, resultSet.getInt(1));
        });
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(0, resultSet.getInt(1));
        });
    }

    @Test
    public void shouldReturn422WhenPostCreateCaseWithInvalidPreStatesForCaseworker() throws Exception {
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(PRE_STATES_EVENT_ID);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(422));

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_data", resultSet -> {
            assertEquals(0, resultSet.getInt(1));
        });
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(0, resultSet.getInt(1));
        });
    }

    @Test
    public void shouldReturn422WhenPostCreateCaseWithInvalidPreStatesForCitizen() throws Exception {
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(PRE_STATES_EVENT_ID);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(422));

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_data", resultSet -> {
            assertEquals(0, resultSet.getInt(1));
        });
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(0, resultSet.getInt(1));
        });
    }

    @Test
    public void shouldReturn404WhenPostCreateCaseWithUnknownEventForCaseworker() throws Exception {
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(FAKE_EVENT_ID);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404));

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_data", resultSet -> {
            assertEquals(0, resultSet.getInt(1));
        });
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(0, resultSet.getInt(1));
        });
    }

    @Test
    public void shouldReturn404WhenPostCreateCaseWithUnknownEventForCitizen() throws Exception {
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(FAKE_EVENT_ID);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404));

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_data", resultSet -> {
            assertEquals(0, resultSet.getInt(1));
        });
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(0, resultSet.getInt(1));
        });
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenGetCaseWithNoCaseTypeReadAccessForCaseworker() throws Exception {
        shouldReturn404WhenGetCaseWithNoCaseTypeReadAccess("caseworkers");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenGetCaseWithNoCaseTypeReadAccessForCitizen() throws Exception {
        shouldReturn404WhenGetCaseWithNoCaseTypeReadAccess("citizens");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200WhenGetValidCaseForCaseworker() throws Exception {

        // Check that we have the expected test data set size, this is to ensure that state filtering is correct
        assertCaseDataResultSetSize();
        {
            final MvcResult result = mockMvc
                .perform(get("/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
                    + "/cases/1504259907353529")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andReturn();

            final CaseDetails caseDetails =
                mapper.readValue(result.getResponse().getContentAsString(), CaseDetails.class);

            assertEquals(1504259907353529L, caseDetails.getReference().longValue());

            assertEquals("TestAddressBookCase", caseDetails.getCaseTypeId());
            assertEquals("PROBATE", caseDetails.getJurisdiction());
            assertEquals("CaseCreated", caseDetails.getState());

            assertEquals("Janet", caseDetails.getData().get("PersonFirstName").asText());
            assertEquals("Parker", caseDetails.getData().get("PersonLastName").asText());
            assertEquals("123", caseDetails.getData().get("PersonAddress").get("AddressLine1").asText());
            assertEquals("Fake Street", caseDetails.getData().get("PersonAddress").get("AddressLine2").asText());
            assertEquals("Hexton", caseDetails.getData().get("PersonAddress").get("AddressLine3").asText());
            assertEquals("England", caseDetails.getData().get("PersonAddress").get("Country").asText());
            assertEquals("HX08 5TG", caseDetails.getData().get("PersonAddress").get("Postcode").asText());
            assertEquals("http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1",
                caseDetails.getData().get("D8Document").get("document_url").asText());
            assertEquals("http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1/binary",
                caseDetails.getData().get("D8Document").get("document_binary_url").asText());
            assertEquals("Seagulls_Square.jpg",
                caseDetails.getData().get("D8Document").get("document_filename").asText());
            assertEquals(4, caseDetails.getDataClassification().size());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonFirstName").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonLastName").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("classification")
                .asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value")
                .get("AddressLine1").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value")
                .get("AddressLine2").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value")
                .get("AddressLine3").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value")
                .get("Country").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value")
                .get("Postcode").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("D8Document").asText());
        }

        {
            final MvcResult result = mockMvc
                .perform(get("/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
                    + "/cases/1504259907353537")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andReturn();

            final CaseDetails caseDetails =
                mapper.readValue(result.getResponse().getContentAsString(), CaseDetails.class);

            assertEquals(1504259907353537L, caseDetails.getReference().longValue());

            assertEquals("TestAddressBookCase", caseDetails.getCaseTypeId());
            assertEquals("PROBATE", caseDetails.getJurisdiction());
            assertEquals("CaseCreated", caseDetails.getState());

            assertEquals("Peter", caseDetails.getData().get("PersonFirstName").asText());
            assertEquals("Pullen", caseDetails.getData().get("PersonLastName").asText());
            assertEquals("Governer House", caseDetails.getData().get("PersonAddress").get("AddressLine1").asText());
            assertEquals("1 Puddle Lane", caseDetails.getData().get("PersonAddress").get("AddressLine2").asText());
            assertEquals("London", caseDetails.getData().get("PersonAddress").get("AddressLine3").asText());
            assertEquals("England", caseDetails.getData().get("PersonAddress").get("Country").asText());
            assertEquals("SE1 4EE", caseDetails.getData().get("PersonAddress").get("Postcode").asText());
            assertEquals(3, caseDetails.getDataClassification().size());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonFirstName").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonLastName").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("classification")
                .asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value")
                .get("AddressLine1").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value")
                .get("AddressLine2").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value")
                .get("AddressLine3").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value")
                .get("Country").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value")
                .get("Postcode").asText());
        }
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void findCaseDetailsForCaseworkerShouldLogAudit() throws Exception {
        final MvcResult result = mockMvc
            .perform(get("/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
                + "/cases/1504259907353529")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is(200))
            .andReturn();

        final CaseDetails caseDetails = mapper.readValue(result.getResponse().getContentAsString(), CaseDetails.class);

        assertEquals(1504259907353529L, caseDetails.getReference().longValue());

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.CASE_ACCESSED.getLabel()));
        assertThat(captor.getValue().getCaseId(), is("1504259907353529"));
        assertThat(captor.getValue().getIdamId(), is(UID));
        assertThat(captor.getValue().getInvokingService(), is(MockUtils.CCD_GW));
        assertThat(captor.getValue().getHttpStatus(), is(200));
        assertThat(captor.getValue().getCaseType(), is(CASE_TYPE));
        assertThat(captor.getValue().getJurisdiction(), is(JURISDICTION));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_private_cases.sql"})
    public void shouldReturn404WhenGetCaseClassificationTooHighForCaseworker() throws Exception {

        mockMvc
            .perform(
                get("/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
                    + "/cases/1504259907353545")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().is(404))
            .andReturn();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200WhenGetValidCaseForCitizen() throws Exception {
        // Check that we have the expected test data set size, this is to ensure that state filtering is correct
        assertCaseDataResultSetSize();

        {
            final MvcResult result = mockMvc
                .perform(get("/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
                    + "/cases/1504259907353529")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andReturn();

            final CaseDetails caseDetails =
                mapper.readValue(result.getResponse().getContentAsString(), CaseDetails.class);

            assertEquals(1504259907353529L, caseDetails.getReference().longValue());

            assertEquals("TestAddressBookCase", caseDetails.getCaseTypeId());
            assertEquals("PROBATE", caseDetails.getJurisdiction());
            assertEquals("CaseCreated", caseDetails.getState());

            assertEquals("Janet", caseDetails.getData().get("PersonFirstName").asText());
            assertEquals("Parker", caseDetails.getData().get("PersonLastName").asText());
            assertEquals("123", caseDetails.getData().get("PersonAddress").get("AddressLine1").asText());
            assertEquals("Fake Street", caseDetails.getData().get("PersonAddress").get("AddressLine2").asText());
            assertEquals("Hexton", caseDetails.getData().get("PersonAddress").get("AddressLine3").asText());
            assertEquals("England", caseDetails.getData().get("PersonAddress").get("Country").asText());
            assertEquals("HX08 5TG", caseDetails.getData().get("PersonAddress").get("Postcode").asText());
            assertEquals("http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1",
                caseDetails.getData().get("D8Document").get("document_url").asText());
            assertEquals("http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1/binary",
                caseDetails.getData().get("D8Document").get("document_binary_url").asText());
            assertEquals("Seagulls_Square.jpg",
                caseDetails.getData().get("D8Document").get("document_filename").asText());
            assertEquals(4, caseDetails.getDataClassification().size());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonFirstName").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonLastName").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("classification")
                .asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value")
                .get("AddressLine1").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress")
                .get("classification").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("D8Document").asText());
        }

        {
            final MvcResult result = mockMvc
                .perform(get("/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
                    + "/cases/1504259907353537")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andReturn();

            final CaseDetails caseDetails = mapper.readValue(result.getResponse().getContentAsString(),
                CaseDetails.class);

            assertEquals(1504259907353537L, caseDetails.getReference().longValue());

            assertEquals("TestAddressBookCase", caseDetails.getCaseTypeId());
            assertEquals("PROBATE", caseDetails.getJurisdiction());
            assertEquals("CaseCreated", caseDetails.getState());

            assertEquals("Peter", caseDetails.getData().get("PersonFirstName").asText());
            assertEquals("Pullen", caseDetails.getData().get("PersonLastName").asText());
            assertEquals("Governer House", caseDetails.getData().get("PersonAddress").get("AddressLine1").asText());
            assertEquals("1 Puddle Lane", caseDetails.getData().get("PersonAddress").get("AddressLine2").asText());
            assertEquals("London", caseDetails.getData().get("PersonAddress").get("AddressLine3").asText());
            assertEquals("England", caseDetails.getData().get("PersonAddress").get("Country").asText());
            assertEquals("SE1 4EE", caseDetails.getData().get("PersonAddress").get("Postcode").asText());
            assertEquals(3, caseDetails.getDataClassification().size());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonFirstName").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonLastName").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value")
                .get("AddressLine1").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value")
                .get("AddressLine2").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value")
                .get("AddressLine3").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value")
                .get("Country").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value")
                .get("Postcode").asText());
        }
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void findCaseDetailsForCitizenShouldLogAudit() throws Exception {

        final MvcResult result = mockMvc
            .perform(get("/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
                + "/cases/1504259907353529")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is(200))
            .andReturn();

        final CaseDetails caseDetails = mapper.readValue(result.getResponse().getContentAsString(), CaseDetails.class);

        assertEquals(1504259907353529L, caseDetails.getReference().longValue());

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.CASE_ACCESSED.getLabel()));
        assertThat(captor.getValue().getCaseId(), is("1504259907353529"));
        assertThat(captor.getValue().getIdamId(), is(UID));
        assertThat(captor.getValue().getInvokingService(), is(MockUtils.CCD_GW));
        assertThat(captor.getValue().getHttpStatus(), is(200));
        assertThat(captor.getValue().getCaseType(), is(CASE_TYPE));
        assertThat(captor.getValue().getJurisdiction(), is(JURISDICTION));

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200WithFieldRemovedWhenGetValidCaseWithNoFieldReadAccessForCaseworker() throws Exception {
        shouldReturn200WithFieldRemovedWhenGetValidCaseWithNoFieldReadAccess("caseworkers");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200WithFieldRemovedWhenGetValidCaseWithNoFieldReadAccessForCitizen() throws Exception {
        shouldReturn200WithFieldRemovedWhenGetValidCaseWithNoFieldReadAccess("citizens");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn400WhenGetValidCaseWithInvalidCaseReferenceForCaseworker() throws Exception {
        // Check that we have the expected test data set size, this is to ensure that state filtering is correct
        assertCaseDataResultSetSize();

        {
            mockMvc
                .perform(get("/caseworkers/0/jurisdictions/" + TEST_JURISDICTION + "/case-types/"
                    + TEST_CASE_TYPE + "/cases/1504259907353528")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(400))
                .andReturn();
        }

        assertCaseDataResultSetSize();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn400WhenGetValidCaseWithInvalidCaseReferenceForCitizen() throws Exception {
        // Check that we have the expected test data set size, this is to ensure that state filtering is correct
        assertCaseDataResultSetSize();

        {
            mockMvc
                .perform(get("/citizens/0/jurisdictions/" + TEST_JURISDICTION + "/case-types/"
                    + TEST_CASE_TYPE + "/cases/1504259907353528")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(400))
                .andReturn();
        }

        assertCaseDataResultSetSize();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenGetCaseWithNonExistentCaseReferenceForCaseworker() throws Exception {
        // Check that we have the expected test data set size, this is to ensure that state filtering is correct
        assertCaseDataResultSetSize();

        mockMvc
            .perform(get("/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
                + "/cases/9999999999999995")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is(404));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenGetCaseWithNonExistentCaseReferenceForCitizen() throws Exception {
        // Check that we have the expected test data set size, this is to ensure that state filtering is correct
        assertCaseDataResultSetSize();

        mockMvc
            .perform(get("/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
                + "/cases/9999999999999995")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is(404));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithNoDataForCaseworker() throws Exception {
        final String caseReference = "1504259907353545";
        final String URL = "/caseworkers/" + UID + "/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(PRE_STATES_EVENT_ID, SUMMARY, DESCRIPTION));

        final JsonNode DATA = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"Wales\"," +
            "\"Postcode\":\"W11 5DF\"," +
            "\"AddressLine1\":\"Flat 9\"," +
            "\"AddressLine2\":\"2 Hubble Avenue\"," +
            "\"AddressLine3\":\"ButtonVillie\"}," +
            "\"PersonLastName\":\"Roof\"," +
            "\"PersonFirstName\":\"George\"}");
        final String expectedClassificationString = "{" +
            "  \"PersonAddress\":{" +
            "    \"classification\": \"PUBLIC\"," +
            "    \"value\": {" +
            "      \"Country\":\"PUBLIC\"," +
            "      \"Postcode\":\"PUBLIC\"," +
            "      \"AddressLine1\":\"PUBLIC\"," +
            "      \"AddressLine2\":\"PUBLIC\"," +
            "      \"AddressLine3\":\"PUBLIC\"" +
            "    }" +
            "  }," +
            "  \"PersonLastName\":\"PUBLIC\"," +
            "  \"PersonFirstName\":\"PUBLIC\"," +
            "  \"D8Document\": \"PUBLIC\"" +
            "}";
        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, caseReference, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);
        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Incorrect Response Content",
            DATA.toString(),
            mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data").toString());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("Incorrect Case Type", CASE_TYPE, savedCaseDetails.getCaseTypeId());
        assertEquals("Incorrect Data content: Data should not have changed", JacksonUtils.convertValue(DATA),
            savedCaseDetails.getData());
        assertEquals("State should have been updated", "state4", savedCaseDetails.getState());
        JSONAssert.assertEquals(expectedClassificationString,
            JacksonUtils.convertValueJsonNode(savedCaseDetails.getDataClassification()).toString(),
            JSONCompareMode.LENIENT);

        final List<AuditEvent> caseAuditEventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("A new event should have been created", 5, caseAuditEventList.size());

        // Assertion belows are for creation event
        final AuditEvent caseAuditEvent = caseAuditEventList.get(4);
        assertEquals("123", caseAuditEvent.getUserId());
        assertEquals("Strife", caseAuditEvent.getUserLastName());
        assertEquals("Cloud", caseAuditEvent.getUserFirstName());
        assertEquals("HAS PRE STATES EVENT", caseAuditEvent.getEventName());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals("Case in state 4", caseAuditEvent.getStateName());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertEquals(SUMMARY, caseAuditEvent.getSummary());
        assertEquals(DESCRIPTION, caseAuditEvent.getDescription());
        JSONAssert.assertEquals(expectedClassificationString,
            JacksonUtils.convertValueJsonNode(caseAuditEvent.getDataClassification()).toString(),
            JSONCompareMode.LENIENT);
    }


    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithNoDataForCitizen() throws Exception {
        final String caseReference = "1504259907353545";
        final String URL = "/citizens/" + UID + "/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(PRE_STATES_EVENT_ID, SUMMARY, DESCRIPTION));
        final JsonNode DATA = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"Wales\"," +
            "\"Postcode\":\"W11 5DF\"," +
            "\"AddressLine1\":\"Flat 9\"," +
            "\"AddressLine2\":\"2 Hubble Avenue\"," +
            "\"AddressLine3\":\"ButtonVillie\"}," +
            "\"PersonLastName\":\"Roof\"," +
            "\"PersonFirstName\":\"George\"}");
        final String expectedClassificationString = "{" +
            "  \"PersonAddress\":{" +
            "    \"classification\": \"PUBLIC\"," +
            "    \"value\":{" +
            "      \"Country\":\"PUBLIC\"," +
            "      \"Postcode\":\"PUBLIC\"," +
            "      \"AddressLine1\":\"PUBLIC\"," +
            "      \"AddressLine2\":\"PUBLIC\"," +
            "      \"AddressLine3\":\"PUBLIC\"" +
            "    }" +
            "  }," +
            "    \"PersonLastName\":\"PUBLIC\"," +
            "    \"PersonFirstName\":\"PUBLIC\"," +
            "    \"D8Document\":\"PUBLIC\"" +
            "}";
        final String token = generateEventToken(template,
                                                UID, JURISDICTION, CASE_TYPE, caseReference, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);
        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Incorrect Response Content",
            DATA.toString(),
            mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data").toString());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("Incorrect Case Type", CASE_TYPE, savedCaseDetails.getCaseTypeId());
        assertEquals("Incorrect Data content: Data should not have changed",
            JacksonUtils.convertValue(DATA), savedCaseDetails.getData());
        assertEquals("State should have been updated", "state4", savedCaseDetails.getState());
        JSONAssert.assertEquals(expectedClassificationString,
            JacksonUtils.convertValueJsonNode(savedCaseDetails.getDataClassification()).toString(),
            JSONCompareMode.LENIENT);

        final List<AuditEvent> caseAuditEventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("A new event should have been created", 5, caseAuditEventList.size());

        // Assertion belows are for creation event
        final AuditEvent caseAuditEvent = caseAuditEventList.get(4);
        assertEquals("123", caseAuditEvent.getUserId());
        assertEquals("Strife", caseAuditEvent.getUserLastName());
        assertEquals("Cloud", caseAuditEvent.getUserFirstName());
        assertEquals("HAS PRE STATES EVENT", caseAuditEvent.getEventName());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals("Case in state 4", caseAuditEvent.getStateName());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertEquals(SUMMARY, caseAuditEvent.getSummary());
        assertEquals(DESCRIPTION, caseAuditEvent.getDescription());
        JSONAssert.assertEquals(expectedClassificationString,
            JacksonUtils.convertValueJsonNode(caseAuditEvent.getDataClassification()).toString(),
            JSONCompareMode.LENIENT);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldUpdateLastStateModifiedTimeWhenAnEventTriggeredStateTransition() throws Exception {
        final String caseReference = "1504259907353545";
        final String URL = "/citizens/" + UID + "/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(PRE_STATES_EVENT_ID, SUMMARY, DESCRIPTION));

        final CaseDetails initialCaseDetails = template.queryForObject("SELECT * FROM case_data where reference = "
            + caseReference, this::mapCaseData);
        assertEquals("CaseCreated", initialCaseDetails.getState());
        assertNotNull(initialCaseDetails.getLastStateModifiedDate());

        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, caseReference, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);
        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        final CaseDetails updatedCaseDetails = template.queryForObject("SELECT * FROM case_data where reference = "
            + caseReference, this::mapCaseData);
        assertNotNull(updatedCaseDetails);
        assertEquals("state4", updatedCaseDetails.getState());
        assertNotEquals(initialCaseDetails.getLastStateModifiedDate(), updatedCaseDetails.getLastStateModifiedDate());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldNotUpdateLastStateModifiedTimeWhenAnEventNotTriggeredStateTransition() throws Exception {

        final String caseTypeUrlPortion = "bookcase-default-post-state";
        final String caseReference = "1557845948403939";
        final String url = "/caseworkers/" + UID + "/jurisdictions/" + JURISDICTION + "/case-types/"
                + caseTypeUrlPortion + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final String token = generateEventToken(template, UID, JURISDICTION, caseTypeUrlPortion, caseReference,
            TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID, SUMMARY, DESCRIPTION));

        final CaseDetails initialCaseDetails = template.queryForObject("SELECT * FROM case_data where reference = "
            + caseReference, this::mapCaseData);
        assertEquals("CaseCreated", initialCaseDetails.getState());
        assertNotNull(initialCaseDetails.getLastStateModifiedDate());

        final MvcResult mvcResult = mockMvc.perform(post(url)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        final CaseDetails updatedCaseDetails = template.queryForObject("SELECT * FROM case_data where reference = "
            + caseReference, this::mapCaseData);
        assertNotNull(updatedCaseDetails);
        assertEquals("CaseCreated", updatedCaseDetails.getState());
        assertEquals(initialCaseDetails.getLastStateModifiedDate(), updatedCaseDetails.getLastStateModifiedDate());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithExistingDocumentBinaryForCaseworker() throws Exception {
        shouldReturn201WhenPostCreateCaseEventWithExistingDocumentBinary("caseworkers");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithExistingDocumentBinaryForCitizen() throws Exception {
        shouldReturn201WhenPostCreateCaseEventWithExistingDocumentBinary("citizens");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithValidDataForCaseworker() throws Exception {
        shouldReturn201WhenPostCreateCaseEventWithValidData("caseworkers");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithValidDataForCitizen() throws Exception {
        shouldReturn201WhenPostCreateCaseEventWithValidData("citizens");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithNoPreStateCheckForCaseworker() throws Exception {
        final String urlPortionForCaseType = "bookcase-default-pre-state-test";
        final String caseReference = "1557850043804031";
        final String url = "/caseworkers/" + UID + "/jurisdictions/" + JURISDICTION + "/case-types/"
            + urlPortionForCaseType + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID, SUMMARY, DESCRIPTION));
        final String token = generateEventToken(template, UID, JURISDICTION, urlPortionForCaseType, caseReference,
            TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);
        final JsonNode DATA = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"_ WB11DDF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"}");
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));

        final MvcResult mvcResult = mockMvc.perform(post(url)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Incorrect Response Content",
            DATA.toString(),
            mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data").toString());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("Incorrect Case Type", urlPortionForCaseType, savedCaseDetails.getCaseTypeId());
        assertEquals(
            "Incorrect Data content: Data should have changed",
            caseDetailsToSave.getData(),
            savedCaseDetails.getData());
        assertEquals("State should have been updated", "state3", savedCaseDetails.getState());

        final List<AuditEvent> caseAuditEventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("A new event should have been created", 5, caseAuditEventList.size());

        final List<MessageQueueCandidate> messageQueueList =
            template.query("SELECT * FROM message_queue_candidates", this::mapMessageCandidate);
        assertEquals("Incorrect number of rows in messageQueue", 0, messageQueueList.size());
        // Assertion belows are for creation event
        final AuditEvent caseAuditEvent = caseAuditEventList.get(4);
        assertEquals("123", caseAuditEvent.getUserId());
        assertEquals("Strife", caseAuditEvent.getUserLastName());
        assertEquals("Cloud", caseAuditEvent.getUserFirstName());
        assertEquals("TEST EVENT NAME", caseAuditEvent.getEventName());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals("Case in state 3", caseAuditEvent.getStateName());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertEquals(SUMMARY, caseAuditEvent.getSummary());
        assertEquals(DESCRIPTION, caseAuditEvent.getDescription());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithNoPreStateCheckForCitizen() throws Exception {
        final String urlPortionForCaseType = "bookcase-default-pre-state-test";
        final String caseReference = "1557850043804031";
        final String url = "/citizens/" + UID + "/jurisdictions/" + JURISDICTION + "/case-types/"
            + urlPortionForCaseType + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID, SUMMARY, DESCRIPTION));
        final String token = generateEventToken(template, UID, JURISDICTION, urlPortionForCaseType, caseReference,
            TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);
        final JsonNode DATA = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"_ WB11DDF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"}");
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));

        final MvcResult mvcResult = mockMvc.perform(post(url)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Incorrect Response Content",
            DATA.toString(),
            mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data").toString());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("Incorrect Case Type", urlPortionForCaseType, savedCaseDetails.getCaseTypeId());
        assertEquals(
            "Incorrect Data content: Data should have changed",
            caseDetailsToSave.getData(),
            savedCaseDetails.getData());
        assertEquals("State should have been updated", "state3", savedCaseDetails.getState());

        final List<AuditEvent> caseAuditEventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("A new event should have been created", 5, caseAuditEventList.size());

        // Assertion belows are for creation event
        final AuditEvent caseAuditEvent = caseAuditEventList.get(4);
        assertEquals("123", caseAuditEvent.getUserId());
        assertEquals("Strife", caseAuditEvent.getUserLastName());
        assertEquals("Cloud", caseAuditEvent.getUserFirstName());
        assertEquals("TEST EVENT NAME", caseAuditEvent.getEventName());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals("Case in state 3", caseAuditEvent.getStateName());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertEquals(SUMMARY, caseAuditEvent.getSummary());
        assertEquals(DESCRIPTION, caseAuditEvent.getDescription());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithNoChangesToPostStateForCaseworker() throws Exception {
        final String caseTypeUrlPortion = "bookcase-default-post-state";
        final String caseReference = "1557845948403939";
        final String url = "/caseworkers/" + UID + "/jurisdictions/" + JURISDICTION + "/case-types/"
            + caseTypeUrlPortion + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final String token = generateEventToken(template, UID, JURISDICTION, caseTypeUrlPortion, caseReference,
            TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID, SUMMARY, DESCRIPTION));
        final JsonNode data = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"_ WB11DDF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"}");
        caseDetailsToSave.setData(JacksonUtils.convertValue(data));

        final MvcResult mvcResult = mockMvc.perform(post(url)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Incorrect Response Content",
            data.toString(),
            mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data").toString());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("Incorrect Case Type", caseTypeUrlPortion, savedCaseDetails.getCaseTypeId());
        assertEquals(
            "Incorrect Data content: Data should have changed",
            caseDetailsToSave.getData(),
            savedCaseDetails.getData());
        assertEquals("State should have been updated", "CaseCreated", savedCaseDetails.getState());

        final List<AuditEvent> caseAuditEventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("A new event should have been created", 5, caseAuditEventList.size());

        // Assertion belows are for creation event
        final AuditEvent caseAuditEvent = caseAuditEventList.get(4);
        assertEquals("123", caseAuditEvent.getUserId());
        assertEquals("Strife", caseAuditEvent.getUserLastName());
        assertEquals("Cloud", caseAuditEvent.getUserFirstName());
        assertEquals("TEST EVENT NAME", caseAuditEvent.getEventName());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals("Case Created", caseAuditEvent.getStateName());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertEquals(SUMMARY, caseAuditEvent.getSummary());
        assertEquals(DESCRIPTION, caseAuditEvent.getDescription());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithNoChangesToPostStateForCitizen() throws Exception {
        final String caseTypeUrlPortion = "bookcase-default-post-state";
        final String caseReference = "1557845948403939";
        final String url = "/citizens/" + UID + "/jurisdictions/" + JURISDICTION + "/case-types/" + caseTypeUrlPortion
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();

        final String token = generateEventToken(template, UID, JURISDICTION, caseTypeUrlPortion, caseReference,
            TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID, SUMMARY, DESCRIPTION));
        final JsonNode data = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"_ WB11DDF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"}");
        caseDetailsToSave.setData(JacksonUtils.convertValue(data));

        final MvcResult mvcResult = mockMvc.perform(post(url)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Incorrect Response Content",
            data.toString(),
            mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data").toString());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("Incorrect Case Type", caseTypeUrlPortion, savedCaseDetails.getCaseTypeId());
        assertEquals(
            "Incorrect Data content: Data should have changed",
            caseDetailsToSave.getData(),
            savedCaseDetails.getData());
        assertEquals("State should have been updated", "CaseCreated", savedCaseDetails.getState());

        final List<AuditEvent> caseAuditEventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("A new event should have been created", 5, caseAuditEventList.size());

        // Assertion belows are for creation event
        final AuditEvent caseAuditEvent = caseAuditEventList.get(4);
        assertEquals("123", caseAuditEvent.getUserId());
        assertEquals("Strife", caseAuditEvent.getUserLastName());
        assertEquals("Cloud", caseAuditEvent.getUserFirstName());
        assertEquals("TEST EVENT NAME", caseAuditEvent.getEventName());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals("Case Created", caseAuditEvent.getStateName());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertEquals(SUMMARY, caseAuditEvent.getSummary());
        assertEquals(DESCRIPTION, caseAuditEvent.getDescription());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WithEmptyBodyWhenPostCreateCaseEventWithNoCaseTypeReadAccessForCaseworker()
                                                                                                    throws Exception {
        shouldReturn201WithEmptyBodyWhenPostCreateCaseEventWithNoCaseTypeReadAccess("caseworkers");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WithEmptyBodyWhenPostCreateCaseEventWithNoCaseTypeReadAccessForCitizen()
                                                                                                    throws Exception {
        shouldReturn201WithEmptyBodyWhenPostCreateCaseEventWithNoCaseTypeReadAccess("citizens");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WithFieldRemovedWhenPostCreateCaseEventWithNoFieldReadAccessForCaseworker()
                                                                                                    throws Exception {
        shouldReturn201WithFieldRemovedWhenPostCreateCaseEventWithNoFieldReadAccess("caseworkers");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WithFieldRemovedWhenPostCreateCaseEventWithNoFieldReadAccessForCitizen()
                                                                                                    throws Exception {
        shouldReturn201WithFieldRemovedWhenPostCreateCaseEventWithNoFieldReadAccess("citizens");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenPostCreateCaseEventWithNoUpdateCaseAccessForCaseworker() throws Exception {
        shouldReturn404WhenPostCreateCaseEventWithNoUpdateCaseAccess("caseworkers");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenPostCreateCaseEventWithNoUpdateCaseAccessForCitizen() throws Exception {
        shouldReturn404WhenPostCreateCaseEventWithNoUpdateCaseAccess("citizens");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenPostCreateCaseEventWithNoCreateEventAccessForCaseworker() throws Exception {
        shouldReturn404WhenPostCreateCaseEventWithNoCreateEventAccess("caseworkers");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenPostCreateCaseEventWithNoCreateEventAccessForCitizen() throws Exception {
        shouldReturn404WhenPostCreateCaseEventWithNoCreateEventAccess("citizens");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenPostCreateCaseEventWithNoCreateFieldAccessForCaseworker() throws Exception {
        shouldReturn404WhenPostCreateCaseEventWithNoCreateFieldAccess("caseworkers");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenPostCreateCaseEventWithNoCreateFieldAccessForCitizen() throws Exception {
        shouldReturn404WhenPostCreateCaseEventWithNoCreateFieldAccess("citizens");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenPostCreateCaseEventWithNoUpdateFieldAccessForCaseworker() throws Exception {
        shouldReturn404WhenPostCreateCaseEventWithNoUpdateFieldAccess("caseworkers");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenPostCreateCaseEventWithNoUpdateFieldAccessForCitizen() throws Exception {
        shouldReturn404WhenPostCreateCaseEventWithNoUpdateFieldAccess("citizens");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithNoSummaryForCaseWorker() throws Exception {
        final String caseReference = "1504259907353545";
        final String URL = "/caseworkers/" + UID + "/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(PRE_STATES_EVENT_ID, null, DESCRIPTION));

        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, caseReference, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(5, resultSet.getInt(1));
        });

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should have been updated", "state4", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithNoSummaryForCitizen() throws Exception {
        final String caseReference = "1504259907353545";
        final String URL = "/citizens/" + UID + "/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(PRE_STATES_EVENT_ID, null, DESCRIPTION));

        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, caseReference, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(5, resultSet.getInt(1));
        });

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should have been updated", "state4", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithBlankSummaryForCaseWorker() throws Exception {
        final String caseReference = "1504259907353545";
        final String summary = "        ";
        final String URL = "/caseworkers/" + UID + "/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(PRE_STATES_EVENT_ID, summary, DESCRIPTION));

        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, caseReference, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);
        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(5, resultSet.getInt(1));
        });

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should have been updated", "state4", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.UPDATE_CASE.getLabel()));
        assertThat(captor.getValue().getCaseId(), is(caseReference));
        assertThat(captor.getValue().getIdamId(), is(UID));
        assertThat(captor.getValue().getInvokingService(), is(MockUtils.CCD_GW));
        assertThat(captor.getValue().getHttpStatus(), is(201));
        assertThat(captor.getValue().getCaseType(), is(CASE_TYPE));
        assertThat(captor.getValue().getJurisdiction(), is(JURISDICTION));
        assertThat(captor.getValue().getEventSelected(), is(PRE_STATES_EVENT_ID));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithBlankSummaryForCitizen() throws Exception {
        final String caseReference = "1504259907353545";
        final String summary = "        ";
        final String URL = "/citizens/" + UID + "/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(PRE_STATES_EVENT_ID, summary, DESCRIPTION));
        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, caseReference, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);
        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(5, resultSet.getInt(1));
        });

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should have been updated", "state4", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.UPDATE_CASE.getLabel()));
        assertThat(captor.getValue().getCaseId(), is(caseReference));
        assertThat(captor.getValue().getIdamId(), is(UID));
        assertThat(captor.getValue().getInvokingService(), is(MockUtils.CCD_GW));
        assertThat(captor.getValue().getHttpStatus(), is(201));
        assertThat(captor.getValue().getCaseType(), is(CASE_TYPE));
        assertThat(captor.getValue().getJurisdiction(), is(JURISDICTION));
        assertThat(captor.getValue().getEventSelected(), is(PRE_STATES_EVENT_ID));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn422WhenPostCreateCaseEventWithSummaryTooLongForCaseWorker() throws Exception {
        final String caseReference = "1504259907353545";
        final String summary = new String(new char[1025]).replace("\0", "-");
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(PRE_STATES_EVENT_ID, summary, DESCRIPTION));

        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, caseReference, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(422))
            .andReturn();

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(4, resultSet.getInt(1));
        });

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should NOT have been updated", "CaseCreated", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn422WhenPostCreateCaseEventWithSummaryTooLongForCitizen() throws Exception {
        final String caseReference = "1504259907353545";
        final String summary = new String(new char[1025]).replace("\0", "-");
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/"
            + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(PRE_STATES_EVENT_ID, summary, DESCRIPTION));

        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, caseReference, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(422))
            .andReturn();

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(4, resultSet.getInt(1));
        });

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should NOT have been updated", "CaseCreated", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn422WhenPostCreateCaseEventWithDescriptionTooLongForCaseWorker() throws Exception {
        final String caseReference = "1504259907353545";
        final String description = new String(new char[65666]).replace("\0", "-");
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/"
            + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(PRE_STATES_EVENT_ID, SUMMARY, description));

        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, caseReference, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(422))
            .andReturn();

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(4, resultSet.getInt(1));
        });

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should NOT have been updated", "CaseCreated", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn422WhenPostCreateCaseEventWithDescriptionTooLongForCitizen() throws Exception {
        final String caseReference = "1504259907353545";
        final String description = new String(new char[65666]).replace("\0", "-");
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/"
            + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(PRE_STATES_EVENT_ID, SUMMARY, description));

        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, caseReference, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(422))
            .andReturn();

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(4, resultSet.getInt(1));
        });

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should NOT have been updated", "CaseCreated", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenPostCreateCaseEventWithNoEventIdForCaseworker() throws Exception {
        final String caseReference = "1504259907353545";
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/"
            + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(null, SUMMARY, DESCRIPTION));

        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, caseReference, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(4, resultSet.getInt(1));
        });

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should NOT have been updated", "CaseCreated", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenPostCreateCaseEventWithNoEventIdForCitizen() throws Exception {
        final String caseReference = "1504259907353545";
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/"
            + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(null, SUMMARY, DESCRIPTION));

        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, caseReference, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(4, resultSet.getInt(1));
        });

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should NOT have been updated", "CaseCreated", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenPostCreateCaseEventWithNoEventForCaseWorker() throws Exception {
        final String caseReference = "1504259907353545";
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/"
            + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, caseReference, "ANY_EVENT");
        caseDetailsToSave.setToken(token);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(4, resultSet.getInt(1));
        });

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should NOT have been updated", "CaseCreated", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenPostCreateCaseEventWithNoEventForCitizen() throws Exception {
        final String caseReference = "1504259907353545";
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/"
            + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, caseReference, "ANY_EVENT");
        caseDetailsToSave.setToken(token);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(4, resultSet.getInt(1));
        });

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should NOT have been updated", "CaseCreated", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenPostCreateCaseEventWithNonExistentCaseIdForCaseWorker() throws Exception {
        final String caseReference = "9999999999999995";
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/"
            + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(PRE_STATES_EVENT_ID, SUMMARY, DESCRIPTION));

        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, 1504259907353537L, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(4, resultSet.getInt(1));
        });
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenPostCreateCaseEventWithNonExistentCaseIdForCitizen() throws Exception {
        final String caseReference = "9999999999999995";
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/"
            + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(PRE_STATES_EVENT_ID, SUMMARY, DESCRIPTION));

        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, 1504259907353537L, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(4, resultSet.getInt(1));
        });
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn400WhenPostCreateCaseEventWithInvalidCaseIdForCaseWorker() throws Exception {
        final String caseReference = "invalidReference";
        final String hasPreStatesEvent = "HAS_PRE_STATES_EVENT";
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/"
            + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(hasPreStatesEvent, SUMMARY, DESCRIPTION));

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, 1504259907353537L,
            hasPreStatesEvent);
        caseDetailsToSave.setToken(token);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(400))
            .andReturn();

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(4, resultSet.getInt(1));
        });
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn400WhenPostCreateCaseEventWithInvalidCaseIdForCitizen() throws Exception {
        final String caseReference = "invalidReference";
        final String hasPreStatesEvent = "HAS_PRE_STATES_EVENT";
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/"
            + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(hasPreStatesEvent, SUMMARY, DESCRIPTION));

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, 1504259907353537L,
            hasPreStatesEvent);
        caseDetailsToSave.setToken(token);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(400))
            .andReturn();

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(4, resultSet.getInt(1));
        });
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenPostCreateCaseEventWithUnknownFieldsForCaseWorker() throws Exception {
        final String caseReference = "1504259907353545";
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/"
            + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(PRE_STATES_EVENT_ID, SUMMARY, DESCRIPTION));
        final JsonNode invalidData = mapper.readTree("{ \"XXX\": \"YYY\" }");
        final JsonNode initialData = mapper.readTree("{" +
                                                     "\"PersonAddress\":{" +
                                                     "\"Country\":\"Wales\"," +
                                                     "\"Postcode\":\"W11 5DF\"," +
                                                     "\"AddressLine1\":\"Flat 9\"," +
                                                     "\"AddressLine2\":\"2 Hubble Avenue\"," +
                                                     "\"AddressLine3\":\"ButtonVillie\"}," +
                                                     "\"PersonLastName\":\"Roof\"," +
                                                     "\"PersonFirstName\":\"George\"}");
        final Map<String, JsonNode> expectedData = JacksonUtils.convertValue(initialData);

        caseDetailsToSave.setData(JacksonUtils.convertValue(invalidData));

        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, caseReference, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))).andExpect(status().is(404))
            .andReturn();

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("Incorrect Case Type", CASE_TYPE, savedCaseDetails.getCaseTypeId());
        assertEquals(
            "Incorrect Data content: Data should NOT have changed",
            expectedData,
            savedCaseDetails.getData());
        assertEquals("State should NOT have been updated", "CaseCreated", savedCaseDetails.getState());

        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(4, resultSet.getInt(1));
        });
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenPostCreateCaseEventWithUnknownFieldsForCitizen() throws Exception {
        final String caseReference = "1504259907353545";
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/"
            + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(PRE_STATES_EVENT_ID, SUMMARY, DESCRIPTION));
        final JsonNode invalidData = mapper.readTree("{ \"XXX\": \"YYY\" }");
        final JsonNode initialData = mapper.readTree("{" +
                                                     "\"PersonAddress\":{" +
                                                     "\"Country\":\"Wales\"," +
                                                     "\"Postcode\":\"W11 5DF\"," +
                                                     "\"AddressLine1\":\"Flat 9\"," +
                                                     "\"AddressLine2\":\"2 Hubble Avenue\"," +
                                                     "\"AddressLine3\":\"ButtonVillie\"}," +
                                                     "\"PersonLastName\":\"Roof\"," +
                                                     "\"PersonFirstName\":\"George\"}");
        final Map<String, JsonNode> expectedData = JacksonUtils.convertValue(initialData);

        caseDetailsToSave.setData(JacksonUtils.convertValue(invalidData));

        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, caseReference, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))).andExpect(status().is(404))
            .andReturn();

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("Incorrect Case Type", CASE_TYPE, savedCaseDetails.getCaseTypeId());
        assertEquals(
            "Incorrect Data content: Data should NOT have changed",
            expectedData,
            savedCaseDetails.getData());
        assertEquals("State should NOT have been updated", "CaseCreated", savedCaseDetails.getState());

        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(4, resultSet.getInt(1));
        });
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn409WhenPostCreateCaseEventWithCaseVersionConflictForCaseWorker() throws Exception {
        final String caseReference = "1504259907353545";
        final String URL = "/caseworkers/" + UID + "/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(PRE_STATES_EVENT_ID, SUMMARY, DESCRIPTION));

        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, caseReference, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        // Simulate case alteration by other actor to fail event token version check
        template.update("UPDATE case_data SET data = '{}', version = 2 WHERE reference = ?", caseReference);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(409))
            .andReturn();

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(4, resultSet.getInt(1));
        });
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn409WhenPostCreateCaseEventWithCaseVersionConflictForCitizen() throws Exception {
        final String caseReference = "1504259907353545";
        final String URL = "/citizens/" + UID + "/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(PRE_STATES_EVENT_ID, SUMMARY, DESCRIPTION));

        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, caseReference, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        // Simulate case alteration by other actor to fail event token version check
        template.update("UPDATE case_data SET data = '{}', version = 2 WHERE reference = ?", caseReference);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(409))
            .andReturn();

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(4, resultSet.getInt(1));
        });
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn422WhenPostCreateCaseEventWithCaseStateConflictForCaseWorker() throws Exception {
        final String caseReference = "1504259907353545";
        final String URL = "/caseworkers/" + UID + "/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(PRE_STATES_EVENT_ID, SUMMARY, DESCRIPTION));

        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, caseReference, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        // Simulate case state alteration by other actor to fail event token version check - no effect since it's
        // checked at save step
        template.update("UPDATE case_data SET state = 'CaseStopped', version = 2 WHERE reference = ?", caseReference);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(422))
            .andReturn();

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(4, resultSet.getInt(1));
        });
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn422WhenPostCreateCaseEventWithCaseStateConflictForCitizen() throws Exception {
        final String caseReference = "1504259907353545";
        final String URL = "/citizens/" + UID + "/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(PRE_STATES_EVENT_ID, SUMMARY, DESCRIPTION));

        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, caseReference, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        // Simulate case state alteration by other actor to fail event token version check - no effect
        // since it's checked at save step
        template.update("UPDATE case_data SET state = 'CaseStopped', version = 2 WHERE reference = ?", caseReference);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(422))
            .andReturn();

        // No database entry created
        template.query("SELECT COUNT(*) FROM case_event", resultSet -> {
            assertEquals(4, resultSet.getInt(1));
        });
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200WithNoCaseDataWhenGetTokenForStartCaseWithNoCaseTypeReadAccessForCaseworker()
        throws Exception {
        shouldReturn200WithNoCaseDataWhenGetTokenForStartCaseWithNoCaseTypeReadAccess("caseworkers");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200WithNoCaseDataWhenGetTokenForStartCaseWithNoCaseTypeReadAccessForCitizen()
        throws Exception {
        shouldReturn200WithNoCaseDataWhenGetTokenForStartCaseWithNoCaseTypeReadAccess("citizens");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200WithNoCaseDataWhenGetTokenForStartEventWithNoCaseTypeReadAccessForCaseworker()
        throws Exception {
        shouldReturn200WithNoCaseDataWhenGetTokenForStartEventWithNoCaseTypeReadAccess("caseworkers");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200WithNoCaseDataWhenGetTokenForStartEventWithNoCaseTypeReadAccessForCitizen()
        throws Exception {
        shouldReturn200WithNoCaseDataWhenGetTokenForStartEventWithNoCaseTypeReadAccess("citizens");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200WithFieldRemovedWhenGetTokenForStartEventWithNoCaseTypeReadAccessForCaseworker()
        throws Exception {
        shouldReturn200WithFieldRemovedWhenGetTokenForStartEventWithNoCaseTypeReadAccess("caseworkers");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200WithFieldRemovedWhenGetTokenForStartEventWithNoCaseTypeReadAccessForCitizen()
        throws Exception {
        shouldReturn200WithFieldRemovedWhenGetTokenForStartEventWithNoCaseTypeReadAccess("citizens");
    }

    private void shouldReturn200WithNoCaseDataWhenGetTokenForStartCaseWithNoCaseTypeReadAccess(String userRole)
        throws Exception {
        final String url = "/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/" +
            CASE_TYPE_NO_READ_CASE_TYPE_ACCESS + "/event-triggers/" + CREATE_EVENT_ID + "/token";

        final MvcResult mvcResult = mockMvc.perform(get(url).contentType(JSON_CONTENT_TYPE))
            .andExpect(status().is(200))
            .andReturn();

        String expected = "{  \n" +
            "   \"case_details\":{  \n" +
            "      \"id\":null,\n" +
            "      \"jurisdiction\":\"PROBATE\",\n" +
            "      \"state\":null,\n" +
            "      \"case_type_id\":\"TestAddressBookCaseNoReadCaseTypeAccess\",\n" +
            "      \"created_date\":null,\n" +
            "      \"last_modified\":null,\n" +
            "      \"security_classification\":null,\n" +
            "      \"case_data\":{  \n" +
            "\n" +
            "      },\n" +
            "      \"data_classification\":{  \n" +
            "\n" +
            "      },\n" +
            "      \"after_submit_callback_response\":null,\n" +
            "      \"callback_response_status_code\":null,\n" +
            "      \"callback_response_status\":null,\n" +
            "      \"security_classifications\":{  \n" +
            "\n" +
            "      }\n" +
            "   },\n" +
            "   \"event_id\":\"Create2\"\n" +
            "}";
        String actual = mvcResult.getResponse().getContentAsString();
        assertAll(
            () -> JSONAssert.assertEquals(expected, actual, JSONCompareMode.LENIENT),
            () -> assertThat(MAPPER.readTree(actual).has("token"), is(true))
        );
    }

    private void shouldReturn404WhenPostCreateCaseWithNoCreateFieldAccess(String userRole) throws Exception {
        final String URL = "/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/"
            + CASE_TYPE_NO_CREATE_FIELD_ACCESS + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID, SUMMARY, DESCRIPTION));

        final JsonNode DATA = mapper.readTree(
            "{\n" +
                "  \"PersonFirstName\": \"First Name\",\n" +
                "  \"PersonLastName\": \"Last Name\",\n" +
                "  \"PersonAddress\": {\n" +
                "    \"AddressLine1\": \"Address Line 1\",\n" +
                "    \"AddressLine2\": \"Address Line 2\"\n" +
                "  }\n" +
                "}\n"
        );
        Map data = JacksonUtils.convertValue(DATA);
        caseDetailsToSave.setData(data);


        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();
    }

    private void shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccess(String role) throws Exception {
        shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccess(role, CASE_TYPE_NO_CREATE_CASE_ACCESS);
    }

    private void shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccess(String role, String caseType)
                                                                                                    throws Exception {
        final String URL = "/" + role + "/0/jurisdictions/" + JURISDICTION + "/case-types/" + caseType + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();


        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();
    }

    private void shouldReturn201WithFieldRemovedWhenPostCreateCaseWithNoFieldReadAccess(String userRole)
                                                                                                    throws Exception {
        final String URL = "/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/"
            + CASE_TYPE_NO_READ_FIELD_ACCESS + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(CREATE_EVENT_ID, SUMMARY, DESCRIPTION));

        final JsonNode DATA = mapper.readTree(
            "{\n" +
                "  \"PersonFirstName\": \"First Name\",\n" +
                "  \"PersonLastName\": \"Last Name\",\n" +
                "  \"PersonAddress\": {\n" +
                "    \"AddressLine1\": \"Address Line 1\",\n" +
                "    \"AddressLine2\": \"Address Line 2\"\n" +
                "  }\n" +
                "}\n"
        );
        Map data = JacksonUtils.convertValue(DATA);
        caseDetailsToSave.setData(data);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE_NO_READ_FIELD_ACCESS,
            CREATE_EVENT_ID);
        caseDetailsToSave.setToken(token);


        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        final JsonNode sanitizedData = mapper.readTree(
            "{\n" +
                "  \"PersonLastName\": \"Last Name\",\n" +
                "  \"PersonAddress\": {\n" +
                "    \"AddressLine1\": \"Address Line 1\",\n" +
                "    \"AddressLine2\": \"Address Line 2\"\n" +
                "  }\n" +
                "}\n"
        );
        Map expectedSanitizedData = mapper.readValue(sanitizedData.toString(), Map.class);
        JsonNode caseData = mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data");
        JsonNode dataClassification = mapper.readTree(mvcResult.getResponse().getContentAsString())
            .get("data_classification");
        Map actualData = mapper.readValue(caseData.toString(), Map.class);
        assertAll(() -> assertThat("Incorrect Response Content",
            actualData.entrySet(),
            everyItem(isIn(expectedSanitizedData.entrySet()))),
            () -> assertThat("Response contains filtered out data", caseData.has("PersonFirstName"),
                is(false)),
            () -> assertThat(dataClassification.has("PersonFirstName"), CoreMatchers.is(false)),
            () -> assertThat(dataClassification.has("PersonLastName"), CoreMatchers.is(true)),
            () -> assertThat(dataClassification.has("PersonAddress"), CoreMatchers.is(true))
        );
    }


    private void shouldReturn404WhenPostCreateCaseWithNoCreateEventAccess(String role) throws Exception {
        final String URL = "/" + role + "/0/jurisdictions/" + JURISDICTION + "/case-types/"
            + CASE_TYPE_NO_CREATE_EVENT_ACCESS + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID, SUMMARY, DESCRIPTION));

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();
    }

    private void shouldReturn201WithEmptyBodyWhenPostCreateCaseWithNoReadAccessOnCaseType(String role)
                                                                                                    throws Exception {
        final String URL = "/" + role + "/0/jurisdictions/" + JURISDICTION + "/case-types/"
            + CASE_TYPE_NO_READ_CASE_TYPE_ACCESS + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(CREATE_EVENT_ID, SUMMARY, DESCRIPTION));

        final JsonNode DATA = mapper.readTree(
            "{\n" +
                "  \"PersonFirstName\": \"First Name\",\n" +
                "  \"PersonLastName\": \"Last Name\",\n" +
                "  \"PersonAddress\": {\n" +
                "    \"AddressLine1\": \"Address Line 1\",\n" +
                "    \"AddressLine2\": \"Address Line 2\"\n" +
                "  }\n" +
                "}\n"
        );
        Map data = JacksonUtils.convertValue(DATA);
        caseDetailsToSave.setData(data);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, CREATE_EVENT_ID);
        caseDetailsToSave.setToken(token);


        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(), CoreMatchers.is(isEmptyString()));
    }

    private void shouldReturn201WithEmptyBodyWhenPostCreateCaseEventWithNoCaseTypeReadAccess(String userRole)
                                                                                                    throws Exception {
        final String caseReference = "1504259907353610";
        final String url = "/" + userRole + "/" + UID + "/jurisdictions/" + JURISDICTION + "/case-types/"
            + CASE_TYPE_NO_READ_CASE_TYPE_ACCESS
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, caseReference, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID, SUMMARY, DESCRIPTION));
        final JsonNode data = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"_ WB11DDF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"}");
        caseDetailsToSave.setData(JacksonUtils.convertValue(data));

        final MvcResult mvcResult = mockMvc.perform(post(url)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        assertThat(response, CoreMatchers.is(isEmptyString()));
    }


    private void shouldReturn201WithFieldRemovedWhenPostCreateCaseEventWithNoFieldReadAccess(String userRole)
                                                                                                    throws Exception {
        final String caseReference = "1504259907353628";
        final String url = "/" + userRole + "/" + UID + "/jurisdictions/" + JURISDICTION + "/case-types/"
            + CASE_TYPE_NO_READ_FIELD_ACCESS
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, caseReference, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID, SUMMARY, DESCRIPTION));
        final JsonNode data = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"_ WB11DDF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"}");
        caseDetailsToSave.setData(JacksonUtils.convertValue(data));

        final MvcResult mvcResult = mockMvc.perform(post(url)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        JsonNode nodeData = mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data");

        JsonNode nodeClassification = mapper.readTree(mvcResult.getResponse().getContentAsString())
            .get("data_classification");
        assertAll(
            () -> assertThat(nodeData.has("PersonFirstName"), CoreMatchers.is(false)),
            () -> assertThat(nodeData.get("PersonLastName"), CoreMatchers.is(getTextNode("_ Roof"))),
            () -> assertThat(nodeData.has("PersonAddress"), CoreMatchers.is(true)),
            () -> assertThat(nodeData.get("PersonAddress").get("Country"),
                             CoreMatchers.is(getTextNode("_ Wales"))),
            () -> assertThat(nodeData.get("PersonAddress").get("Postcode"),
                             CoreMatchers.is(getTextNode("_ WB11DDF"))),
            () -> assertThat(nodeData.get("PersonAddress").get("AddressLine1"),
                             CoreMatchers.is(getTextNode("_ Flat 9"))),
            () -> assertThat(nodeData.get("PersonAddress").get("AddressLine2"),
                             CoreMatchers.is(getTextNode("_ 2 Hubble Avenue"))),
            () -> assertThat(nodeData.get("PersonAddress").get("AddressLine3"),
                             CoreMatchers.is(getTextNode("_ ButtonVillie"))),
            () -> assertThat(nodeClassification.has("PersonFirstName"), CoreMatchers.is(false)),
            () -> assertThat(nodeClassification.has("PersonLastName"), CoreMatchers.is(true)),
            () -> assertThat(nodeClassification.has("PersonAddress"), CoreMatchers.is(true)),
            () -> assertThat(nodeClassification.get("PersonAddress").get("value").has("Country"),
                CoreMatchers.is(true)),
            () -> assertThat(nodeClassification.get("PersonAddress").get("value").has("Postcode"),
                CoreMatchers.is(true)),
            () -> assertThat(nodeClassification.get("PersonAddress").get("value").has("AddressLine1"),
                CoreMatchers.is(true)),
            () -> assertThat(nodeClassification.get("PersonAddress").get("value").has("AddressLine2"),
                CoreMatchers.is(true)),
            () -> assertThat(nodeClassification.get("PersonAddress").get("value").has("AddressLine3"),
                CoreMatchers.is(true))
        );
    }

    private void shouldReturn404WhenPostCreateCaseEventWithNoUpdateCaseAccess(String userRole) throws Exception {
        final String caseReference = "1504259907353578";
        final String URL = "/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/"
            + CASE_TYPE_NO_UPDATE_CASE_ACCESS  + "/cases/" + caseReference + "/events";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, caseReference, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID, SUMMARY, DESCRIPTION));
        final JsonNode data = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"_ WB11DDF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"}");
        caseDetailsToSave.setData(JacksonUtils.convertValue(data));


        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();
    }

    private void shouldReturn404WhenPostCreateCaseEventWithNoCreateEventAccess(String userRole) throws Exception {
        final String caseReference = "1504259907353586";
        final String URL = "/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/"
            + CASE_TYPE_NO_CREATE_EVENT_ACCESS  + "/cases/" + caseReference + "/events";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, caseReference, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID, SUMMARY, DESCRIPTION));
        final JsonNode data = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"_ WB11DDF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"}");
        caseDetailsToSave.setData(JacksonUtils.convertValue(data));


        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();
    }

    private void shouldReturn200WithNoCaseDataWhenGetTokenForStartEventWithNoCaseTypeReadAccess(String userRole)
        throws Exception {
        final String reference = "1504259907353610";
        final String URL = "/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/" +
            CASE_TYPE_NO_READ_CASE_TYPE_ACCESS + "/cases/" + reference + "/event-triggers/" + TEST_EVENT_ID + "/token";

        final MvcResult mvcResult = mockMvc.perform(get(URL).contentType(JSON_CONTENT_TYPE))
            .andExpect(status().is(200))
            .andReturn();

        String expected = "{  \n" +
            "   \"case_details\":{  \n" +
            "      \"id\":1504259907353610,\n" +
            "      \"jurisdiction\":\"PROBATE\",\n" +
            "      \"state\":\"CaseCreated\",\n" +
            "      \"case_type_id\":\"TestAddressBookCaseNoReadCaseTypeAccess\",\n" +
            "      \"last_modified\":null,\n" +
            "      \"security_classification\":\"PUBLIC\",\n" +
            "      \"case_data\":{  \n" +
            "\n" +
            "      },\n" +
            "      \"data_classification\":{  \n" +
            "\n" +
            "      },\n" +
            "      \"after_submit_callback_response\":null,\n" +
            "      \"callback_response_status_code\":null,\n" +
            "      \"callback_response_status\":null,\n" +
            "      \"security_classifications\":{  \n" +
            "\n" +
            "      }\n" +
            "   },\n" +
            "   \"event_id\":\"TEST_EVENT\"\n" +
            "}";
        String actual = mvcResult.getResponse().getContentAsString();
        assertAll(
            () -> JSONAssert.assertEquals(expected, actual, JSONCompareMode.LENIENT),
            () -> assertThat("Token is not present", MAPPER.readTree(actual).has("token"), is(true)),
            () -> assertThat("Created_date is not present", MAPPER.readTree(actual).get("case_details")
                .has("created_date"), is(true))
        );
    }

    private void shouldReturn200WithFieldRemovedWhenGetTokenForStartEventWithNoCaseTypeReadAccess(String userRole)
        throws Exception {
        final String reference = "1504259907353628";
        final String URL = "/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/" +
            CASE_TYPE_NO_READ_FIELD_ACCESS + "/cases/" + reference + "/event-triggers/" + TEST_EVENT_ID + "/token";

        final MvcResult mvcResult = mockMvc.perform(get(URL).contentType(JSON_CONTENT_TYPE))
            .andExpect(status().is(200))
            .andReturn();

        JsonNode rootNode = mapper.readTree(mvcResult.getResponse().getContentAsString());
        assertAll(() -> assertThat(rootNode.has("case_details"), CoreMatchers.is(true)),
            () -> assertThat(rootNode.get("case_details").get("case_data").has("PersonFirstName"),
                             CoreMatchers.is(false)),
            () -> assertThat(rootNode.get("case_details").get("case_data").has("PersonLastName"),
                             CoreMatchers.is(true)),
            () -> assertThat(rootNode.get("case_details").get("case_data").has("PersonAddress"),
                             CoreMatchers.is(true)),
            () -> assertThat(rootNode.get("case_details").get("case_data").has("D8Document"),
                             CoreMatchers.is(true)),
            () -> assertThat(rootNode.get("case_details").get("data_classification").has("PersonFirstName"),
                             CoreMatchers.is(false)),
            () -> assertThat(rootNode.get("case_details").get("data_classification").has("PersonLastName"),
                             CoreMatchers.is(true)),
            () -> assertThat(rootNode.get("case_details").get("data_classification").has("PersonAddress"),
                             CoreMatchers.is(true)),
            () -> assertThat(rootNode.get("case_details").get("data_classification").has("D8Document"),
                             CoreMatchers.is(true))
        );
    }

    private void shouldReturn404WhenPostCreateCaseEventWithNoCreateFieldAccess(String userRole) throws Exception {
        final String caseReference = "1504259907353594";
        final String URL = "/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/"
            + CASE_TYPE_NO_CREATE_FIELD_ACCESS  + "/cases/" + caseReference + "/events";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, caseReference, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID, SUMMARY, DESCRIPTION));
        final JsonNode data = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"_ WB11DDF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"}");
        caseDetailsToSave.setData(JacksonUtils.convertValue(data));


        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();
    }

    private void shouldReturn404WhenPostCreateCaseEventWithNoUpdateFieldAccess(String userRole) throws Exception {
        final String caseReference = "1504259907353602";
        final String URL = "/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/"
            + CASE_TYPE_NO_UPDATE_FIELD_ACCESS  + "/cases/" + caseReference + "/events";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, caseReference, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID, SUMMARY, DESCRIPTION));
        final JsonNode data = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"_ WB11DDF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"}");
        caseDetailsToSave.setData(JacksonUtils.convertValue(data));


        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();
    }

    private void shouldReturn404WhenGetCaseWithNoCaseTypeReadAccess(String userRole) throws Exception {
        final String caseReference = "1504259907353610";
        final String url = "/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/"
            + CASE_TYPE_NO_READ_CASE_TYPE_ACCESS + "/cases/" + caseReference;

        mockMvc.perform(get(url)
            .contentType(JSON_CONTENT_TYPE)
        ).andExpect(status().is(404))
            .andReturn();
    }

    private void shouldReturn200WithFieldRemovedWhenGetValidCaseWithNoFieldReadAccess(String userRole)
                                                                                                    throws Exception {
        // Check that we have the expected test data set size, this is to ensure that state filtering is correct
        assertCaseDataResultSetSize();

        {
            final MvcResult result = mockMvc
                .perform(get("/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/"
                    + CASE_TYPE_NO_READ_FIELD_ACCESS + "/cases/1504259907353628")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andReturn();

            final CaseDetails caseDetails =
                mapper.readValue(result.getResponse().getContentAsString(), CaseDetails.class);

            assertEquals(1504259907353628L, caseDetails.getReference().longValue());

            JsonNode nodeData = JacksonUtils.convertValueJsonNode(caseDetails.getData());
            assertAll(
                () -> assertThat(nodeData.has("PersonFirstName"), CoreMatchers.is(false)),
                () -> assertThat(nodeData.get("PersonLastName"), CoreMatchers.is(getTextNode("Parker"))),
                () -> assertThat(nodeData.has("PersonAddress"), CoreMatchers.is(true)),
                () -> assertThat(nodeData.get("PersonAddress").get("Country"), CoreMatchers
                    .is(getTextNode("England"))),
                () -> assertThat(nodeData.get("PersonAddress").get("Postcode"), CoreMatchers
                    .is(getTextNode("HX08 5TG"))),
                () -> assertThat(nodeData.get("PersonAddress").get("AddressLine1"), CoreMatchers
                    .is(getTextNode("123"))),
                () -> assertThat(nodeData.get("PersonAddress").get("AddressLine2"), CoreMatchers
                    .is(getTextNode("Fake Street"))),
                () -> assertThat(nodeData.get("PersonAddress").get("AddressLine3"), CoreMatchers
                    .is(getTextNode("Hexton"))),
                () -> assertThat(nodeData.has("D8Document"), CoreMatchers.is(true)),
                () -> assertThat(nodeData.get("D8Document").get("document_url"), CoreMatchers
                    .is(getTextNode("http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1"))),
                () -> assertThat(nodeData.get("D8Document").get("document_binary_url"), CoreMatchers
                    .is(getTextNode("http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1"
                        + "/binary"))),
                () -> assertThat(nodeData.get("D8Document").get("document_filename"), CoreMatchers
                    .is(getTextNode("Seagulls_Square.jpg")))
            );
            JsonNode nodeClassification = JacksonUtils.convertValueJsonNode(caseDetails.getDataClassification());
            assertAll(
                () -> assertThat(nodeClassification.has("PersonFirstName"), CoreMatchers.is(false)),
                () -> assertThat(nodeClassification.get("PersonLastName"), CoreMatchers
                    .is(getTextNode("PUBLIC"))),
                () -> assertThat(nodeClassification.has("PersonAddress"), CoreMatchers.is(true)),
                () -> assertThat(nodeClassification.get("PersonAddress").get("classification"), CoreMatchers
                    .is(getTextNode("PUBLIC"))),
                () -> assertThat(nodeClassification.get("PersonAddress").get("value").get("Country"), CoreMatchers
                    .is(getTextNode("PUBLIC"))),
                () -> assertThat(nodeClassification.get("PersonAddress").get("value").get("Postcode"), CoreMatchers
                    .is(getTextNode("PUBLIC"))),
                () -> assertThat(nodeClassification.get("PersonAddress").get("value").get("AddressLine1"), CoreMatchers
                    .is(getTextNode("PUBLIC"))),
                () -> assertThat(nodeClassification.get("PersonAddress").get("value").get("AddressLine2"), CoreMatchers
                    .is(getTextNode("PUBLIC"))),
                () -> assertThat(nodeClassification.get("PersonAddress").get("value").get("AddressLine3"), CoreMatchers
                    .is(getTextNode("PUBLIC"))),
                () -> assertThat(nodeClassification.get("D8Document"), CoreMatchers.is(getTextNode("PUBLIC")))
            );
        }
    }

    private void shouldReturn201WhenPostCreateCaseEventWithValidData(String userRole) throws Exception {
        final String caseReference = "1504259907353545";
        final String URL = "/" + userRole + "/" + UID + "/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(PRE_STATES_EVENT_ID, SUMMARY, DESCRIPTION));

        final JsonNode DATA = mapper.readTree(exampleData());
        final JsonNode sanitizedData = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"W11 5DF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"," +
            "\"D8Document\":{" +
            "    \"document_url\": \"http://localhost:" + getPort()
            + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\"," +
            "    \"document_binary_url\": \"http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0"
            + "/binary\"," +
            "    \"document_filename\": \"Seagulls_Square.jpg\"" +
            "}" +
            "}");
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));
        final String expectedClassificationString = "{" +
            "  \"PersonAddress\":{" +
            "    \"classification\": \"PUBLIC\"," +
            "    \"value\": {" +
            "      \"Country\":\"PUBLIC\"," +
            "      \"Postcode\":\"PUBLIC\"," +
            "      \"AddressLine1\":\"PUBLIC\"," +
            "      \"AddressLine2\":\"PUBLIC\"," +
            "      \"AddressLine3\":\"PUBLIC\"" +
            "    }" +
            "  }," +
            "  \"PersonLastName\":\"PUBLIC\"," +
            "  \"PersonFirstName\":\"PUBLIC\"," +
            "  \"D8Document\": \"PUBLIC\"" +
            "}";
        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, caseReference, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Incorrect Response Content",
            sanitizedData.toString(),
            mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data").toString());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("Incorrect Case Type", CASE_TYPE, savedCaseDetails.getCaseTypeId());
        Map sanitizedDataMap = JacksonUtils.convertValue(sanitizedData);
        assertThat(
            "Incorrect Data content: Data should have changed",
            savedCaseDetails.getData().entrySet(), everyItem(isIn(sanitizedDataMap.entrySet())));
        assertEquals("State should have been updated", "state4", savedCaseDetails.getState());
        JSONAssert.assertEquals(expectedClassificationString,
            mapper.convertValue(savedCaseDetails.getDataClassification(), JsonNode.class).toString(), JSONCompareMode
                .LENIENT);

        final List<AuditEvent> caseAuditEventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("A new event should have been created", 5, caseAuditEventList.size());

        // Assertion belows are for creation event
        final AuditEvent caseAuditEvent = caseAuditEventList.get(4);
        assertEquals("123", caseAuditEvent.getUserId());
        assertEquals("Strife", caseAuditEvent.getUserLastName());
        assertEquals("Cloud", caseAuditEvent.getUserFirstName());
        assertEquals("HAS PRE STATES EVENT", caseAuditEvent.getEventName());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals("Case in state 4", caseAuditEvent.getStateName());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertEquals(SUMMARY, caseAuditEvent.getSummary());
        assertEquals(DESCRIPTION, caseAuditEvent.getDescription());
        JSONAssert.assertEquals(expectedClassificationString,
            JacksonUtils.convertValue(caseAuditEvent.getDataClassification()).toString(), JSONCompareMode.LENIENT);
    }

    private void shouldReturn201WhenPostCreateCaseEventWithExistingDocumentBinary(String userRole) throws Exception {
        final String caseReference = "1504259907353529";
        final String URL = "/" + userRole + "/" + UID + "/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(PRE_STATES_EVENT_ID, SUMMARY, DESCRIPTION));

        final JsonNode DATA = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"W11 5DF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"" +
            "}");
        final JsonNode sanitizedData = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"W11 5DF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"," +
            "\"D8Document\":{" +
            "    \"document_url\": \"http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1\"," +
            "    \"document_filename\": \"Seagulls_Square.jpg\"," +
            "    \"document_binary_url\": \"http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1"
            + "/binary\"" +
            "}" +
            "}");
        caseDetailsToSave.setData(JacksonUtils.convertValue(DATA));
        final String token = generateEventToken(template,
            UID, JURISDICTION, CASE_TYPE, caseReference, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Incorrect Response Content",
            sanitizedData.toString(),
            mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data").toString());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", NUMBER_OF_CASES, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("Incorrect Case Type", CASE_TYPE, savedCaseDetails.getCaseTypeId());
        Map sanitizedDataMap = JacksonUtils.convertValue(sanitizedData);
        assertThat(
            "Incorrect Data content: Data should have changed",
            savedCaseDetails.getData().entrySet(), everyItem(isIn(sanitizedDataMap.entrySet())));
    }

    @Test
    public void shouldReturn200WhenPostValidateCaseDetailsWithValidDataForCaseworker() throws Exception {
        final JsonNode DATA = mapper.readTree(exampleData());

        final CaseDataContent caseDetailsToValidate = newCaseDataContent()
            .withEvent(anEvent()
                .withEventId(TEST_EVENT_ID)
                .withSummary(SHORT_COMMENT)
                .withDescription(LONG_COMMENT)
                .build())
            .withToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID))
            .withData(JacksonUtils.convertValue(DATA))
            .withIgnoreWarning(Boolean.FALSE)
            .build();

        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/validate";
        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToValidate))
        ).andExpect(status().is(200)).andReturn();

        final JsonNode expectedResponse = MAPPER.readTree("{\"data\": " + exampleData() + "}");
        final String expectedResponseString = mapper.writeValueAsString(expectedResponse);
        assertEquals("Incorrect Response Content",
            expectedResponseString,
            mapper.readTree(mvcResult.getResponse().getContentAsString()).toString());
    }

    @Test
    public void shouldReturn422WhenPostValidateCaseDetailsWithInvalidDataForCaseworker() throws Exception {
        final JsonNode DATA = mapper.readTree(exampleDataWithInvalidPostcode());
        final CaseDataContent caseDetailsToValidate = newCaseDataContent()
            .withEvent(anEvent()
                .withEventId(TEST_EVENT_ID)
                .withSummary(SHORT_COMMENT)
                .withDescription(LONG_COMMENT)
                .build())
            .withToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID))
            .withData(JacksonUtils.convertValue(DATA))
            .withIgnoreWarning(Boolean.FALSE)
            .build();

        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/validate";
        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToValidate))
        ).andExpect(status().is(422))
            .andReturn();

        assertEquals("Expected field validation error is not there",
            "\"PersonAddress.Postcode\"",
            mapper.readTree(mvcResult.getResponse().getContentAsString()).get("details").get("field_errors").get(0)
                .get("id").toString());
        assertEquals("Incorrect Response Content",
            "\"Case data validation failed\"",
            mapper.readTree(mvcResult.getResponse().getContentAsString()).get("message").toString());
    }

    @Test
    public void shouldReturn200WhenPostValidateCaseDetailsWithValidDataForCitizen() throws Exception {
        final JsonNode DATA = mapper.readTree(exampleData());

        final CaseDataContent caseDetailsToValidate = newCaseDataContent()
            .withEvent(anEvent()
                .withEventId(TEST_EVENT_ID)
                .withSummary(SHORT_COMMENT)
                .withDescription(LONG_COMMENT)
                .build())
            .withToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID))
            .withData(JacksonUtils.convertValue(DATA))
            .withIgnoreWarning(Boolean.FALSE)
            .build();

        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/validate";
        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToValidate))
        ).andExpect(status().is(200)).andReturn();

        final JsonNode expectedResponse = MAPPER.readTree("{\"data\": " + exampleData() + "}");
        final String expectedResponseString = mapper.writeValueAsString(expectedResponse);
        assertEquals("Incorrect Response Content",
            expectedResponseString,
            mapper.readTree(mvcResult.getResponse().getContentAsString()).toString());
    }

    @Test
    public void shouldReturn422WhenPostValidateCaseDetailsWithInvalidDataForCitizen() throws Exception {
        final JsonNode DATA = mapper.readTree(exampleDataWithInvalidPostcode());

        final CaseDataContent caseDetailsToValidate = newCaseDataContent()
            .withEvent(anEvent()
                .withEventId(TEST_EVENT_ID)
                .withSummary(SHORT_COMMENT)
                .withDescription(LONG_COMMENT)
                .build())
            .withToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID))
            .withData(JacksonUtils.convertValue(DATA))
            .withIgnoreWarning(Boolean.FALSE)
            .build();

        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/validate";
        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToValidate))
        ).andExpect(status().is(422))
            .andReturn();

        assertEquals("Expected field validation error is not there",
            "\"PersonAddress.Postcode\"",
            mapper.readTree(mvcResult.getResponse().getContentAsString()).get("details").get("field_errors").get(0)
                .get("id").toString());
        assertEquals("Incorrect Response Content",
            "\"Case data validation failed\"",
            mapper.readTree(mvcResult.getResponse().getContentAsString()).get("message").toString());

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200_whenSearchAsSolicitor() throws Exception {

        assertCaseDataResultSetSize();

        final MvcResult result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("state", TEST_STATE)
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();


        String responseAsString = result.getResponse().getContentAsString();
        List<CaseDetails> caseDetails = Arrays.asList(mapper.readValue(responseAsString, CaseDetails[].class));

        assertThat(caseDetails, hasSize(2));
        assertThat(caseDetails, (everyItem(hasProperty("jurisdiction", is(JURISDICTION)))));
        assertThat(caseDetails, (everyItem(hasProperty("caseTypeId", is(TEST_CASE_TYPE)))));
        assertThat(caseDetails, (everyItem(hasProperty("state", is(TEST_STATE)))));

        assertThat(responseAsString, containsString("Janet"));
        assertThat(responseAsString, containsString("Parker"));
        assertThat(responseAsString, containsString("Fake Street"));
        assertThat(responseAsString, containsString("\"PersonLastName\":\"PUBLIC\""));
        assertThat(responseAsString,
            containsString("http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1"));

        assertThat(responseAsString, containsString("George"));
        assertThat(responseAsString, containsString("Roof"));
        assertThat(responseAsString, containsString("2 Hubble Avenue"));
        assertThat(responseAsString, containsString("\"AddressLine1\":\"PUBLIC\""));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200_whenSearchWithParamsAsCaseWorker() throws Exception {

        assertCaseDataResultSetSize();

        final MvcResult result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("case.PersonFirstName", "Janet ")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        List<CaseDetails> caseDetails = Arrays.asList(mapper.readValue(responseAsString, CaseDetails[].class));

        assertThat(caseDetails, hasSize(2));
        assertThat(result.getResponse().getContentAsString(), containsString(JURISDICTION));
        assertThat(result.getResponse().getContentAsString(), containsString(TEST_CASE_TYPE));
        assertThat(result.getResponse().getContentAsString(), containsString(TEST_STATE));
        assertThat(result.getResponse().getContentAsString(), containsString("Janet"));
        assertThat(result.getResponse().getContentAsString(), containsString("Parker"));
        assertThat(result.getResponse().getContentAsString(), containsString("Fake Street"));
        assertThat(result.getResponse().getContentAsString(), containsString("Hexton"));
        assertThat(result.getResponse().getContentAsString(), containsString("HX08 5TG"));
        assertThat(result.getResponse().getContentAsString(), containsString("http://localhost:[port]/"
            + "documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldSearchCaseDetailsByReference() throws Exception {

        assertCaseDataResultSetSize();

        MvcResult result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("case_reference", "1504259907353545")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        List<CaseDetails> caseDetails = Arrays.asList(mapper.readValue(responseAsString, CaseDetails[].class));
        assertThat(caseDetails, hasSize(1));
        assertThat(result.getResponse().getContentAsString(), containsString(JURISDICTION));
        assertThat(result.getResponse().getContentAsString(), containsString(TEST_CASE_TYPE));
        assertThat(result.getResponse().getContentAsString(), containsString("George"));

        result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("case_reference", "1504259907353545")
            .param("case.PersonFirstName", "George ")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        responseAsString = result.getResponse().getContentAsString();
        caseDetails = Arrays.asList(mapper.readValue(responseAsString, CaseDetails[].class));
        assertThat(caseDetails, hasSize(1));
        assertThat(result.getResponse().getContentAsString(), containsString("George"));

        result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("case_reference", "1504259907353545")
            .param("case.PersonFirstName", "notExisting")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        responseAsString = result.getResponse().getContentAsString();
        caseDetails = Arrays.asList(mapper.readValue(responseAsString, CaseDetails[].class));
        assertThat(caseDetails, hasSize(0));

        result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("case_reference", "1504259907353545")
            .param("state", "notExistingState")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        caseDetails = Arrays.asList(mapper.readValue(result.getResponse().getContentAsString(), CaseDetails[].class));
        assertThat(caseDetails, hasSize(0));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldSearchCaseDetailsBySecurityClassification() throws Exception {

        assertCaseDataResultSetSize();

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PRIVATE);

        MvcResult result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("security_classification", "PRIVATE")
            .param("state", "CaseCreated")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        List<CaseDetails> caseDetails = Arrays.asList(mapper.readValue(responseAsString, CaseDetails[].class));
        assertThat(caseDetails, hasSize(1));
        assertThat(responseAsString, containsString("1504259907353598"));

        result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("security_classification", "PUBLIC")
            .param("state", "CaseCreated")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        responseAsString = result.getResponse().getContentAsString();
        caseDetails = Arrays.asList(mapper.readValue(responseAsString, CaseDetails[].class));
        assertThat(caseDetails, hasSize(2));
        assertThat(caseDetails, (everyItem(hasProperty("securityClassification", is(SecurityClassification
            .valueOf("PUBLIC"))))));


        result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("security_classification", "prIVATE")
            .param("state", "CaseCreated")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        responseAsString = result.getResponse().getContentAsString();
        caseDetails = Arrays.asList(mapper.readValue(responseAsString, CaseDetails[].class));
        assertThat(caseDetails, hasSize(1));
        assertThat(responseAsString, containsString("1504259907353598"));
        assertThat(responseAsString, containsString("Angel"));

        result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("security_classification", "PRIVATE")
            .param("state", "CaseCreated")
            .param("case.PersonFirstName", "Angel")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        responseAsString = result.getResponse().getContentAsString();
        caseDetails = Arrays.asList(mapper.readValue(responseAsString, CaseDetails[].class));
        assertThat(caseDetails, hasSize(1));
        assertThat(responseAsString, containsString("1504259907353598"));
        assertThat(responseAsString, containsString("Angel"));

        result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("security_classification", "PRIVATE")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        caseDetails = Arrays.asList(mapper.readValue(result.getResponse().getContentAsString(), CaseDetails[].class));
        assertThat(caseDetails, hasSize(2));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldSearchCaseDetailsByCreationDate() throws Exception {

        assertCaseDataResultSetSize();

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PRIVATE);

        MvcResult result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("created_date", "2016-08-22")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        List<CaseDetails> caseDetails = Arrays.asList(mapper.readValue(responseAsString, CaseDetails[].class));

        assertThat(caseDetails, hasSize(2));
        assertThat(responseAsString, containsString("1504259907353545"));
        assertThat(responseAsString, containsString("1504259907353537"));

        result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("created_date", "2016-08-22")
            .param("case.PersonFirstName", "Angel")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        responseAsString = result.getResponse().getContentAsString();
        caseDetails = Arrays.asList(mapper.readValue(responseAsString, CaseDetails[].class));

        assertThat(caseDetails, hasSize(1));
        assertThat(responseAsString, containsString("1504259907353598"));
        assertThat(responseAsString, containsString("Angel"));

        result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("created_date", "2016-08-22")
            .param("case.PersonFirstName", "Angel")
            .param("case.PersonLastName", "notExisting")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        responseAsString = result.getResponse().getContentAsString();
        caseDetails = Arrays.asList(mapper.readValue(responseAsString, CaseDetails[].class));

        assertThat(caseDetails, hasSize(0));

        result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("created_date", "2016-08-22")
            .param("security_classification", "PRIVATE")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        caseDetails = Arrays.asList(mapper.readValue(result.getResponse().getContentAsString(), CaseDetails[].class));
        assertThat(caseDetails, hasSize(1));
        assertThat(result.getResponse().getContentAsString(), containsString("1504259907353598"));


        result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("created_date", "2016-08-22")
            .param("security_classification", "PRIVATE")
            .param("state", "notExistingState")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        caseDetails = Arrays.asList(mapper.readValue(result.getResponse().getContentAsString(), CaseDetails[].class));
        assertThat(caseDetails, hasSize(0));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldSearchCaseDetailsByLastModified() throws Exception {

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PRIVATE);

        MvcResult result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("last_modified_date", "2016-08-24")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        List<CaseDetails> caseDetails = Arrays.asList(mapper.readValue(responseAsString, CaseDetails[].class));

        assertThat(caseDetails, hasSize(2));
        assertThat(responseAsString, containsString("1504259907353545"));
        assertThat(responseAsString, containsString("1504259907353537"));

        result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("last_modified_date", "2016-08-24")
            .param("case.PersonLastName", "Morten")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        responseAsString = result.getResponse().getContentAsString();
        caseDetails = Arrays.asList(mapper.readValue(responseAsString, CaseDetails[].class));

        assertThat(caseDetails, hasSize(1));
        assertThat(responseAsString, containsString("1504259907353598"));

        result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("created_date", "2016-08-22")
            .param("last_modified_date", "2016-08-24")
            .param("security_classification", "puBLIc")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        caseDetails = Arrays.asList(mapper.readValue(result.getResponse().getContentAsString(), CaseDetails[].class));
        assertThat(caseDetails, hasSize(2));
        assertThat(result.getResponse().getContentAsString(), containsString("1504259907353545"));

        result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("last_modified_date", "2016-08-27")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        caseDetails = Arrays.asList(mapper.readValue(result.getResponse().getContentAsString(), CaseDetails[].class));
        assertThat(caseDetails, hasSize(0));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = { "classpath:sql/insert_cases.sql" })
    public void shouldAuditLogCaseWorkerSearch() throws Exception {

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PRIVATE);

        MvcResult result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("last_modified_date", "2016-08-24")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        List<CaseDetails> caseDetails = Arrays.asList(mapper.readValue(responseAsString, CaseDetails[].class));

        assertThat(caseDetails, hasSize(2));
        assertThat(responseAsString, containsString("1504259907353545"));
        assertThat(responseAsString, containsString("1504259907353537"));

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.SEARCH_CASE.getLabel()));
        assertThat(captor.getValue().getJurisdiction(), is(JURISDICTION));
        assertThat(captor.getValue().getCaseType(), is(CASE_TYPE));
        assertThat(captor.getValue().getCaseId(), is("1504259907353545,1504259907353537"));

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = { "classpath:sql/insert_cases.sql" })
    public void shouldAuditLogCitizenSearch() throws Exception {

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PRIVATE);

        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";

        MvcResult result = mockMvc.perform(get(URL)
            .contentType(JSON_CONTENT_TYPE)
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        List<CaseDetails> caseDetails = Arrays.asList(mapper.readValue(responseAsString, CaseDetails[].class));

        assertThat(caseDetails, hasSize(2));
        assertThat(responseAsString, containsString("1504259907353545"));
        assertThat(responseAsString, containsString("1504259907353529"));

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.SEARCH_CASE.getLabel()));
        assertThat(captor.getValue().getJurisdiction(), is(JURISDICTION));
        assertThat(captor.getValue().getCaseType(), is(CASE_TYPE));
        assertThat(captor.getValue().getCaseId(), is("1504259907353529,1504259907353545"));

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturnEmptyResult_whenSearchWithNonMatchingCriteriaAsCaseWorker() throws Exception {

        assertCaseDataResultSetSize();

        final MvcResult result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("case.PersonFirstName", "JanetX")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        List<CaseDetails> caseDetails = Arrays.asList(mapper.readValue(result.getResponse().getContentAsString(),
            CaseDetails[].class));

        assertThat(caseDetails, empty());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturnPaginatedResults() throws Exception {

        assertCaseDataResultSetSize();
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PRIVATE);

        MvcResult result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("page", "1")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        List<CaseDetails> allPages = newArrayList();

        List<CaseDetails> caseDetailsPage1 = Arrays.asList(mapper.readValue(responseAsString, CaseDetails[].class));
        assertThat(caseDetailsPage1, hasSize(2));
        allPages.addAll(caseDetailsPage1);

        result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("page", "2")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();


        responseAsString = result.getResponse().getContentAsString();
        List<CaseDetails> caseDetailsPage2 = Arrays.asList(mapper.readValue(responseAsString, CaseDetails[].class));
        assertThat(caseDetailsPage2, hasSize(2));
        allPages.addAll(caseDetailsPage2);

        result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("page", "3")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();


        responseAsString = result.getResponse().getContentAsString();
        List<CaseDetails> caseDetailsPage3 = Arrays.asList(mapper.readValue(responseAsString, CaseDetails[].class));
        //TODO RDM-1455 due to filtering being applied after pagination, to be fixed after EL implementation
        assertThat(caseDetailsPage3, hasSize(2));
        allPages.addAll(caseDetailsPage3);

        result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("page", "4")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();


        responseAsString = result.getResponse().getContentAsString();
        List<CaseDetails> caseDetailsPage4 = Arrays.asList(mapper.readValue(responseAsString, CaseDetails[].class));
        assertThat(caseDetailsPage4, hasSize(0));

        Set<Long> references = allPages.stream().map(cd -> cd.getReference()).collect(Collectors.toSet());
        //TODO RDM-1455 due to filtering being applied after pagination, to be fixed after EL implementation
        assertThat(references, hasSize(6));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturnPaginatedSearchMetadata() throws Exception {

        assertCaseDataResultSetSize();
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PRIVATE);

        MvcResult result = mockMvc.perform(get(GET_PAGINATED_SEARCH_METADATA)
            .contentType(JSON_CONTENT_TYPE)
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();


        String responseAsString = result.getResponse().getContentAsString();
        PaginatedSearchMetadata metadata = mapper.readValue(responseAsString, PaginatedSearchMetadata.class);

        assertThat(metadata.getTotalPagesCount(), is(3));
        assertThat(metadata.getTotalResultsCount(), is(6));

        result = mockMvc.perform(get(GET_PAGINATED_SEARCH_METADATA)
            .contentType(JSON_CONTENT_TYPE)
            .param("security_classification", "private")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();


        responseAsString = result.getResponse().getContentAsString();
        metadata = mapper.readValue(responseAsString, PaginatedSearchMetadata.class);

        assertThat(metadata.getTotalPagesCount(), is(1));
        assertThat(metadata.getTotalResultsCount(), is(2));

        result = mockMvc.perform(get(GET_PAGINATED_SEARCH_METADATA)
            .contentType(JSON_CONTENT_TYPE)
            .param("security_classification", "private")
            .param("case.PersonLastName", "Morten")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();


        responseAsString = result.getResponse().getContentAsString();
        metadata = mapper.readValue(responseAsString, PaginatedSearchMetadata.class);

        assertThat(metadata.getTotalPagesCount(), is(1));
        assertThat(metadata.getTotalResultsCount(), is(1));

        result = mockMvc.perform(get(GET_PAGINATED_SEARCH_METADATA)
            .contentType(JSON_CONTENT_TYPE)
            .param("security_classification", "public")
            .param("case.PersonLastName", "notExisting")
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();


        responseAsString = result.getResponse().getContentAsString();
        metadata = mapper.readValue(responseAsString, PaginatedSearchMetadata.class);

        assertThat(metadata.getTotalPagesCount(), is(0));
        assertThat(metadata.getTotalResultsCount(), is(0));
    }


    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturnPaginatedSearchMetadataForCitizen() throws Exception {

        assertCaseDataResultSetSize();
        MockUtils.setSecurityAuthorities(authentication, "role-citizen");

        MvcResult result = mockMvc.perform(get(GET_PAGINATED_SEARCH_METADATA_CITIZENS)
            .contentType(JSON_CONTENT_TYPE)
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();


        String responseAsString = result.getResponse().getContentAsString();
        PaginatedSearchMetadata metadata = mapper.readValue(responseAsString, PaginatedSearchMetadata.class);

        assertThat(metadata.getTotalPagesCount(), is(4));
        assertThat(metadata.getTotalResultsCount(), is(7));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn400_whenSearchWithBadRequestParamAsCaseWorker() throws Exception {

        assertCaseDataResultSetSize();

        mockMvc.perform(get(GET_CASES_AS_CASEWORKER).contentType(JSON_CONTENT_TYPE)
            .param("case.PersonFirstName$", "JanetX")  // bad search param here
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(400))
            .andReturn();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200_whenSearchParamsAreSanitizedAsCaseWorker() throws Exception {

        assertCaseDataResultSetSize();

        final MvcResult result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
            .contentType(JSON_CONTENT_TYPE)
            .param("case.PersonAddress.Country", "EnglanD")  // expects this
            // to be sanitized
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        List<CaseDetails> caseDetails = Arrays.asList(mapper.readValue(responseAsString, CaseDetails[].class));

        assertThat(caseDetails, hasSize(2));
        assertThat(responseAsString, containsString("Janet"));
        assertThat(responseAsString, containsString("Peter"));
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseWithCreatorRoleWithNoDataForCaseworker() throws Exception {
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE_CREATOR_ROLE
            + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID, SUMMARY, DESCRIPTION));

        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE_CREATOR_ROLE, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);


        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Expected empty case data", "", mapper.readTree(mvcResult.getResponse().getContentAsString())
            .get("case_data").asText());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertTrue("Incorrect Case Reference", uidService.validateUID(String.valueOf(savedCaseDetails
            .getReference())));
        assertEquals("Incorrect Case Type", CASE_TYPE_CREATOR_ROLE, savedCaseDetails.getCaseTypeId());
        assertEquals("Incorrect Data content", "{}", savedCaseDetails.getData().toString());
        assertEquals("state3", savedCaseDetails.getState());

        final List<AuditEvent> caseAuditEventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("Incorrect number of case events", 1, caseAuditEventList.size());

        final AuditEvent caseAuditEvent = caseAuditEventList.get(0);
        assertEquals("123", caseAuditEvent.getUserId());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals("Case in state 3", caseAuditEvent.getStateName());
        assertEquals(savedCaseDetails.getCreatedDate(), caseAuditEvent.getCreatedDate());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertEquals("Event ID", TEST_EVENT_ID, caseAuditEvent.getEventId());
        assertEquals("Description", LONG_COMMENT, caseAuditEvent.getDescription());
        assertEquals("Summary", SHORT_COMMENT, caseAuditEvent.getSummary());
        assertTrue(caseAuditEvent.getDataClassification().isEmpty());
        assertThat(caseAuditEvent.getSecurityClassification(), equalTo(PRIVATE));
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseWithCreatorRoleWithNoDataForCitizen() throws Exception {
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE_CREATOR_ROLE
            + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(createEvent(TEST_EVENT_ID, SUMMARY, DESCRIPTION));

        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE_CREATOR_ROLE, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);


        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Expected empty case data", "", mapper.readTree(mvcResult.getResponse().getContentAsString())
            .get("case_data").asText());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertTrue("Incorrect Case Reference", uidService.validateUID(String.valueOf(savedCaseDetails
            .getReference())));
        assertEquals("Incorrect Case Type", CASE_TYPE_CREATOR_ROLE, savedCaseDetails.getCaseTypeId());
        assertEquals("Incorrect Data content", "{}", savedCaseDetails.getData().toString());
        assertEquals("state3", savedCaseDetails.getState());

        final List<AuditEvent> caseAuditEventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("Incorrect number of case events", 1, caseAuditEventList.size());

        final AuditEvent caseAuditEvent = caseAuditEventList.get(0);
        assertEquals("123", caseAuditEvent.getUserId());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals("Case in state 3", caseAuditEvent.getStateName());
        assertEquals(savedCaseDetails.getCreatedDate(), caseAuditEvent.getCreatedDate());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertEquals("Event ID", TEST_EVENT_ID, caseAuditEvent.getEventId());
        assertEquals("Description", LONG_COMMENT, caseAuditEvent.getDescription());
        assertEquals("Summary", SHORT_COMMENT, caseAuditEvent.getSummary());
        assertTrue(caseAuditEvent.getDataClassification().isEmpty());
        assertThat(caseAuditEvent.getSecurityClassification(), equalTo(PRIVATE));
    }

    @Test
    public void shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccessOnCreatorRoleForCaseworker() throws Exception {
        shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccess("caseworkers",
            CASE_TYPE_CREATOR_ROLE_NO_CREATE_ACCESS);
    }

    @Test
    public void shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccessOnCreatorRoleForCitizen() throws Exception {
        shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccess("citizens",
            CASE_TYPE_CREATOR_ROLE_NO_CREATE_ACCESS);
    }

    /**
     * Checks that we have the expected test data set size, this is to ensure
     * that state filtering is correct.
     */
    private void assertCaseDataResultSetSize() {
        final int count = template.queryForObject("SELECT count(1) as n FROM case_data", Integer.class);
        assertEquals("Incorrect case data size", NUMBER_OF_CASES, count);
    }

    private JsonNode getTextNode(String value) {
        return JSON_NODE_FACTORY.textNode(value);
    }

    private String exampleDataWithInvalidPostcode() {
        return "{"
            + "\"PersonAddress\":{"
            + "\"Country\":\"_ Wales\","
            + "\"Postcode\":\"W11225DF\","
            + "\"AddressLine1\":\"_ Flat 9\","
            + "\"AddressLine2\":\"_ 2 Hubble Avenue\","
            + "\"AddressLine3\":\"_ ButtonVillie\"},"
            + "\"PersonLastName\":\"_ Roof\","
            + "\"PersonFirstName\":\"_ George\","
            + "\"D8Document\":{"
            + "\"document_url\": \"http://localhost:" + getPort() + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\""
            + "}"
            + "}";
    }

    private String exampleData() {
        return "{"
            + "\"PersonAddress\":{"
            + "\"Country\":\"_ Wales\","
            + "\"Postcode\":\"W11 5DF\","
            + "\"AddressLine1\":\"_ Flat 9\","
            + "\"AddressLine2\":\"_ 2 Hubble Avenue\","
            + "\"AddressLine3\":\"_ ButtonVillie\"},"
            + "\"PersonLastName\":\"_ Roof\","
            + "\"PersonFirstName\":\"_ George\","
            + "\"D8Document\":{"
            + "\"document_url\": \"http://localhost:" + getPort() + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\""
            + "}"
            + "}";
    }

    @Test
    public void shouldFilterCaseDataWhoseOrderGreaterThanPassedPageId() throws Exception {
        final JsonNode data = mapper.readTree(exampleCaseData());
        final JsonNode eventData = mapper.readTree(exampleEventData());
        WizardPageCollection wizardPageCollection = createWizardPageCollection(MID_EVENT_CALL_BACK);
        stubFor(WireMock.get(urlMatching("/api/display/wizard-page-structure.*"))
            .willReturn(okJson(mapper.writeValueAsString(wizardPageCollection)).withStatus(200)));

        final String description = "A very long comment.......";
        final String summary = "Short comment";
        final CaseDataContent caseDetailsToValidate = newCaseDataContent()
            .withEvent(anEvent()
                .withEventId(TEST_EVENT_ID)
                .withSummary(summary)
                .withDescription(description)
                .build())
            .withToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE_VALIDATE, TEST_EVENT_ID))
            .withData(mapper.convertValue(data, new TypeReference<HashMap<String, JsonNode>>() {}))
            .withEventData(mapper.convertValue(eventData, new TypeReference<HashMap<String, JsonNode>>() {}))
            .withIgnoreWarning(Boolean.FALSE)
            .build();

        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE_VALIDATE
            + "/validate?pageId=createCaseInfoPage";
        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToValidate))
        ).andExpect(status().is(200)).andReturn();

        verifyWireMock(1, postRequestedFor(urlMatching(MID_EVENT_CALL_BACK)));
        verifyWireMock(1, postRequestedFor(urlMatching(MID_EVENT_CALL_BACK))
                                              .withRequestBody(equalToJson(requestBodyJson())));

        final JsonNode expectedResponse = MAPPER.readTree("{\"data\": " + expectedCaseData() + "}");
        final String exptextedResponseValue = mapper.writeValueAsString(expectedResponse);
        assertEquals("Incorrect Response Content",
            exptextedResponseValue,
            mapper.readTree(mvcResult.getResponse().getContentAsString()).toString());
    }

    @Test
    public void shouldFilterCaseDataWhoseOrderGreaterThanPassedPageIdMultiplePreviousPages() throws Exception {
        final JsonNode data = mapper.readTree(secondPageData());
        final JsonNode eventData = mapper.readTree(exampleEventDataMultiPages());
        WizardPageCollection wizardPageCollection = createWizardPageCollection(MID_EVENT_CALL_BACK_MULTI_PAGE);

        wizardPageCollection.getWizardPages()
            .add(createWizardPage("createCaseThirdPage",
                "CaseField31",
                "CaseField32", 3, MID_EVENT_CALL_BACK_MULTI_PAGE));
        wizardPageCollection.getWizardPages()
            .add(createWizardPage("createCaseFourthPage",
                "CaseField41",
                "CaseField42", 4, MID_EVENT_CALL_BACK_MULTI_PAGE));

        stubFor(WireMock.get(urlMatching("/api/display/wizard-page-structure.*"))
            .willReturn(okJson(mapper.writeValueAsString(wizardPageCollection)).withStatus(200)));

        final String description = "A very long comment.......";
        final String summary = "Short comment";
        final CaseDataContent caseDetailsToValidate = newCaseDataContent()
            .withEvent(anEvent()
                .withEventId(TEST_EVENT_ID)
                .withSummary(summary)
                .withDescription(description)
                .build())
            .withToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE_VALIDATE_MULTI_PAGE, TEST_EVENT_ID))
            .withData(mapper.convertValue(data, new TypeReference<HashMap<String, JsonNode>>() {}))
            .withEventData(mapper.convertValue(eventData, new TypeReference<HashMap<String, JsonNode>>() {}))
            .withIgnoreWarning(Boolean.FALSE)
            .build();

        final String URL = "/caseworkers/0/jurisdictions/"
            + JURISDICTION + "/case-types/"
            + CASE_TYPE_VALIDATE_MULTI_PAGE
            + "/validate?pageId=createCaseNextPage";
        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToValidate))
        ).andExpect(status().is(200)).andReturn();

        verifyWireMock(1, postRequestedFor(urlMatching(MID_EVENT_CALL_BACK_MULTI_PAGE)));
        verifyWireMock(1, postRequestedFor(urlMatching(MID_EVENT_CALL_BACK_MULTI_PAGE))
            .withRequestBody(equalToJson(requestBodyJsonMultiPage())));

        final JsonNode expectedResponse = MAPPER.readTree("{\"data\": " + expectedCaseDataMultiPage() + "}");
        final String expectedResponseValue = mapper.writeValueAsString(expectedResponse);
        assertEquals("Incorrect Response Content",
            expectedResponseValue,
            mapper.readTree(mvcResult.getResponse().getContentAsString()).toString());
    }

    private String requestBodyJsonMultiPage() {
        return "{\n"
            + "  \"case_details\" : {\n"
            + "    \"id\" : null,\n"
            + "    \"jurisdiction\" : \"PROBATE\",\n"
            + "    \"state\" : null,\n"
            + "    \"version\" : null,\n"
            + "    \"case_type_id\" : \"TestAddressBookCaseValidateMultiPage\",\n"
            + "    \"created_date\" : null,\n"
            + "    \"last_modified\" : null,\n"
            + "    \"last_state_modified_date\" : null,\n"
            + "    \"security_classification\" : null,\n"
            + "    \"case_data\" : {\n"
            + "      \"PersonLastName\" : \"_ Roof\",\n"
            + "      \"CaseNumber\" : \"_ 1234567\",\n"
            + "      \"PersonFirstName\" : \"_ George\",\n"
            + "      \"TelephoneNumber\" : \"_ 07865645667\",\n"
            + "      \"D8Document\" : {\n"
            + "        \"document_url\" : \"http://localhost:" + this.getPort()
            + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\"\n"
            + "      }\n"
            + "    },\n"
            + "    \"data_classification\" : null,\n"
            + "    \"supplementary_data\" : null,\n"
            + "    \"after_submit_callback_response\" : null,\n"
            + "    \"callback_response_status_code\" : null,\n"
            + "    \"callback_response_status\" : null,\n"
            + "    \"delete_draft_response_status_code\" : null,\n"
            + "    \"delete_draft_response_status\" : null,\n"
            + "    \"security_classifications\" : null\n"
            + "  },\n"
            + "  \"case_details_before\" : null,\n"
            + "  \"event_id\" : \"TEST_EVENT\",\n"
            + "  \"ignore_warning\" : false\n"
            + "}";
    }

    private String requestBodyJson() {
        return "{\"case_details\":{\"id\":null,\"jurisdiction\":\"PROBATE\",\"state\":null,\"version\":null,"
                + "\"case_type_id\":\"TestAddressBookCaseValidate\",\"created_date\":null,\"last_modified\":null,"
                + "\"last_state_modified_date\":null,\"security_classification\":null,\"case_data\":{\"PersonLastName"
                + "\":\"_ Roof\",\"PersonFirstName\":\"_ George\","
                + "\"D8Document\":{\"document_url\":\"http://localhost:" + getPort() + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\"}},"
                + "\"data_classification\":null,\"supplementary_data\":null,\"after_submit_callback_response\":null,"
                + "\"callback_response_status_code\":null,\"callback_response_status\":null,"
                + "\"delete_draft_response_status_code\":null,\"delete_draft_response_status\":null,"
                + "\"security_classifications\":null},\"case_details_before\":null,"
                + "\"event_id\":\"TEST_EVENT\",\"ignore_warning\":false}";
    }

    private WizardPageCollection createWizardPageCollection(String eventCallBackURI) {
        WizardPageCollection wizardPageCollection = new WizardPageCollection();
        wizardPageCollection.getWizardPages()
            .add(createWizardPage("createCaseInfoPage",
                "PersonFirstName",
                "PersonLastName", 1, eventCallBackURI));
        wizardPageCollection.getWizardPages()
            .add(createWizardPage("createCaseNextPage",
                "CaseNumber",
                "TelephoneNumber", 2, eventCallBackURI));
        return wizardPageCollection;
    }

    private WizardPage createWizardPage(String pageName, String caseFieldName1, String caseFieldName2,
                                        Integer pageOrder, String eventCallBackURI) {
        final CaseViewField caseViewField1 = aViewField()
            .withId(caseFieldName1)
            .withOrder(1)
            .build();
        final CaseViewField caseViewField2 = aViewField()
            .withId(caseFieldName2)
            .withOrder(2)
            .build();

        return newWizardPage()
            .withId(pageName)
            .withOrder(pageOrder)
            .withField(caseViewField1)
            .withField(caseViewField2)
            .withCallBackURLMidEvent("http://localhost:" + getPort() + eventCallBackURI)
            .build();
    }

    private String expectedCaseData() {
        return "{"
            + "\"PersonLastName\":\"Roof\","
            + "\"PersonFirstName\":\"George\""
            + "}";
    }

    private String expectedCaseDataMultiPage() {
        return "{\n"
            + "\t\"PersonLastName\": \"Roof\",\n"
            + "\t\"CaseNumber\": \"1234567\",\n"
            + "\t\"PersonFirstName\": \"George\",\n"
            + "\t\"TelephoneNumber\": \"07865645667\"\n"
            + "}";
    }

    private String secondPageData() {
        return "{"
            + "\"CaseNumber\":\"_ 1234567\","
            + "\"TelephoneNumber\":\"_ 07865645667\","
            + "\"D8Document\":{"
            + "\"document_url\": \"http://localhost:" + getPort() + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\""
            + "}"
            + "}";
    }

    private String exampleEventDataMultiPages() {
        return "{"
            + "\"PersonLastName\":\"_ Roof\","
            + "\"PersonFirstName\":\"_ George\","
            + "\"CaseNumber\":\"_ 1234567\","
            + "\"TelephoneNumber\":\"_ 07865645667\","
            + "\"CaseField31\":\"_ Test123\","
            + "\"CaseField32\":\"_ Test765\","
            + "\"CaseField41\":\"_ Test987\","
            + "\"CaseField42\":\"_ Test567\","
            + "\"D8Document\":{"
            + "\"document_url\": \"http://localhost:" + getPort() + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\""
            + "}"
            + "}";
    }

    private String exampleCaseData() {
        return "{"
            + "\"PersonLastName\":\"Roof\","
            + "\"PersonFirstName\":\"George\","
            + "\"D8Document\":{"
            + "\"document_url\": \"http://localhost:" + getPort() + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\""
            + "}"
            + "}";
    }

    private String exampleEventData() {
        return "{"
            + "\"PersonLastName\":\"_ Roof\","
            + "\"PersonFirstName\":\"_ George\","
            + "\"CaseNumber\":\"_ 1234567\","
            + "\"TelephoneNumber\":\"_ 07865645667\","
            + "\"D8Document\":{"
            + "\"document_url\": \"http://localhost:" + getPort() + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\""
            + "}"
            + "}";
    }

    private static Event createEvent(String eventId, String summary, String description) {
        return anEvent()
            .withEventId(eventId)
            .withSummary(summary)
            .withDescription(description)
            .build();
    }

    private static Event createEvent(String eventId) {
        return anEvent()
            .withEventId(eventId)
            .build();
    }
}
