package uk.gov.hmcts.ccd.endpoint.std;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
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
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.collection.IsIn.isIn;
import static org.hamcrest.core.Every.everyItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PRIVATE;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

public class CaseDetailsEndpointIT extends WireMockBaseTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String CASE_TYPE = "TestAddressBookCase";
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
    private static final String GET_CASES_AS_CASEWORKER = "/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/cases";
    private static final String GET_PAGINATED_SEARCH_METADATA = "/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/cases/pagination_metadata";
    private static final String TEST_CASE_TYPE = "TestAddressBookCase";
    private static final String TEST_JURISDICTION = "PROBATE";
    private static final String TEST_STATE = "CaseCreated";
    private static final String UID = "0";
    private static final String DRAFT_ID = "5";

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;
    private JdbcTemplate template;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

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
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(TEST_EVENT_ID);
        caseDetailsToSave.setData(mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        }));
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID
        );
        caseDetailsToSave.setToken(token);

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
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(TEST_EVENT_ID);
        caseDetailsToSave.setData(mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        }));
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID
        );
        caseDetailsToSave.setToken(token);

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

        // we should still have one case in DB
        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseAndSameReferenceFirstTimeButRetryIsUniqueForCaseworker() throws Exception {
        when(uidService.generateUID()).thenReturn(REFERENCE).thenReturn(REFERENCE).thenReturn(REFERENCE_2);
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";
        final JsonNode DATA = mapper.readTree("{}\n");
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(TEST_EVENT_ID);
        caseDetailsToSave.setData(mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        }));
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID
        );
        caseDetailsToSave.setToken(token);

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

        // we should still have one case in DB
        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 2, caseDetailsList.size());
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseAndSameReferenceFirstTimeButRetryIsUniqueForCitizen() throws Exception {
        when(uidService.generateUID()).thenReturn(REFERENCE).thenReturn(REFERENCE).thenReturn(REFERENCE_2);
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";
        final JsonNode DATA = mapper.readTree("{}\n");
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(TEST_EVENT_ID);
        caseDetailsToSave.setData(mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        }));
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID
        );
        caseDetailsToSave.setToken(token);

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
            "    \"document_url\": \"http://localhost:" + getPort() + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\"}" +
            "}\n"
        );
        final JsonNode SANITIZED_DATA = mapper.readTree(
            "{\n" +
            "  \"PersonFirstName\": \"First Name\",\n" +
            "  \"PersonLastName\": \"Last Name\",\n" +
            "  \"PersonAddress\": {\n" +
            "    \"AddressLine1\": \"Address Line 1\",\n" +
            "    \"AddressLine2\": \"Address Line 2\"\n" +
            "  },\n" +
            "  \"Aliases\": [{\"value\": \"x1\", \"id\": \"1\"}, {\"value\": \"x2\", \"id\": \"2\"}]," +
            "  \"D8Document\":{\n" +
            "    \"document_url\": \"http://localhost:" + getPort() + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\",\n" +
            "    \"document_binary_url\": \"http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0/binary\",\n" +
            "    \"document_filename\": \"Seagulls_Square.jpg\"" +
            "}\n" +
            "}\n"
        );
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(TEST_EVENT_ID);
        Map data = mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        caseDetailsToSave.setData(data);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID
        );
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setDraftId(DRAFT_ID);


        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();
        assertEquals("Incorrect Response Status Code", 201, mvcResult.getResponse().getStatus());
        Map expectedSanitizedData = mapper.readValue(SANITIZED_DATA.toString(), Map.class);
        Map actualData = mapper.readValue(mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data").toString(), Map.class);
        assertThat("Incorrect Response Content", actualData.entrySet(), everyItem(isIn(expectedSanitizedData.entrySet())));

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertTrue("Incorrect Case Reference", uidService.validateUID(String.valueOf(savedCaseDetails.getReference())));
        assertEquals("Incorrect Case Type", CASE_TYPE, savedCaseDetails.getCaseTypeId());
        Map sanitizedData = mapper.convertValue(SANITIZED_DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        assertThat("Incorrect Data content", savedCaseDetails.getData().entrySet(), everyItem(isIn(sanitizedData.entrySet())));
        assertEquals("Incorrect security classification size", 5, savedCaseDetails.getDataClassification().size());
        JsonNode expectedClassification = mapper.readTree("{" +
                                                              "  \"PersonAddress\":{" +
                                                              "    \"classification\": \"PUBLIC\"," +
                                                              "    \"value\": {" +
                                                              "      \"AddressLine1\":\"PUBLIC\"," +
                                                              "      \"AddressLine2\":\"PUBLIC\"" +
                                                              "    }" +
                                                              "  }," +
                                                              "  \"PersonLastName\":\"PUBLIC\"," +
                                                              "  \"PersonFirstName\":\"PUBLIC\"," +
                                                              "  \"Aliases\": {\"classification\": \"PUBLIC\", \"value\": [{\"id\": \"1\", \"classification\": \"PUBLIC\"}, {\"id\": \"2\", \"classification\": \"PUBLIC\"}]}," +
                                                              "  \"D8Document\":\"PUBLIC\"" +
                                                              "}");
        JsonNode actualClassification = mapper.convertValue(savedCaseDetails.getDataClassification(), JsonNode.class);
        assertEquals("Incorrect security classifications", expectedClassification, actualClassification);
        assertEquals("state3", savedCaseDetails.getState());

        final List<AuditEvent> caseAuditEventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("Incorrect number of case events", 1, caseAuditEventList.size());

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
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseWithEmptyDataClassificationForCitizen() throws Exception {
        final String LONG_COMMENT = "A very long comment.......";
        final String SHORT_COMMENT = "Short comment";

        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases?ignore-warning=true";
        final JsonNode DATA = mapper.readTree(
            "{\n"
            + "  \"PersonFirstName\": \"First Name\",\n"
            + "  \"PersonLastName\": \"Last Name\",\n"
            + "  \"PersonAddress\": {\n"
            + "    \"AddressLine1\": \"Address Line 1\",\n"
            + "    \"AddressLine2\": \"Address Line 2\"\n"
            + "  },\n"
            + "\"D8Document\":{"
            + "\"document_url\": \"http://localhost:" + getPort() + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\"}"
            + "}\n"
        );
        final JsonNode SANITIZED_DATA = mapper.readTree(
            "{\n"
            + "  \"PersonFirstName\": \"First Name\",\n"
            + "  \"PersonLastName\": \"Last Name\",\n"
            + "  \"PersonAddress\": {\n"
            + "    \"AddressLine1\": \"Address Line 1\",\n"
            + "    \"AddressLine2\": \"Address Line 2\"\n"
            + "  },\n"
            + "  \"D8Document\":{\n"
            + "    \"document_url\": \"http://localhost:" + getPort() + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\",\n"
            + "    \"document_binary_url\": \"http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0/binary\",\n"
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
        Map data = mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        caseDetailsToSave.setData(data);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();
        assertEquals("Incorrect Response Status Code", 201, mvcResult.getResponse().getStatus());
        Map expectedSanitizedData = mapper.readValue(SANITIZED_DATA.toString(), Map.class);
        Map actualData = mapper.readValue(mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data").toString(), Map.class);
        assertThat("Incorrect Response Content", actualData.entrySet(), everyItem(isIn(expectedSanitizedData.entrySet())));

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertTrue("Incorrect Case Reference", uidService.validateUID(String.valueOf(savedCaseDetails.getReference())));
        assertEquals("Incorrect Case Type", CASE_TYPE, savedCaseDetails.getCaseTypeId());
        Map sanitizedData = mapper.convertValue(SANITIZED_DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        assertThat("Incorrect Data content", savedCaseDetails.getData().entrySet(), everyItem(isIn(sanitizedData.entrySet())));
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
        JsonNode actualClassification = mapper.convertValue(savedCaseDetails.getDataClassification(), JsonNode.class);
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
            +     "\"D8Document\":{"
            +     "\"document_url\": \"http://localhost:" + getPort() + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1\"}"
            + "}\n"
        );

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().build());
        caseDetailsToSave.getEvent().setEventId(TEST_EVENT_ID);
        Map data = mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        caseDetailsToSave.setData(data);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID
        );
        caseDetailsToSave.setToken(token);


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
        final String LONG_COMMENT = "A very long comment.......";
        final String SHORT_COMMENT = "Short comment";

        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases?ignore-warning=true";
        final JsonNode DATA = mapper.readTree(
            "{\n"
            + "  \"PersonFirstName\": \"First Name\",\n"
            + "  \"PersonLastName\": \"Last Name\",\n"
            + "  \"PersonAddress\": {\n"
            + "    \"AddressLine1\": \"Address Line 1\",\n"
            + "    \"AddressLine2\": \"Address Line 2\"\n"
            + "  },\n"
            + "\"D8Document\":{"
            + "\"document_url\": \"http://localhost:" + getPort() + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1\"}"
            + "}\n"
        );
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(LONG_COMMENT);
        triggeringEvent.setSummary(SHORT_COMMENT);
        caseDetailsToSave.setEvent(triggeringEvent);
        Map data = mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        caseDetailsToSave.setData(data);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);

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
        Map data = mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        caseDetailsToSave.setData(data);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID
        );
        caseDetailsToSave.setToken(token);


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
        final String LONG_COMMENT = "A very long comment.......";
        final String SHORT_COMMENT = "Short comment";

        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases?ignore-warning=true";
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
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(LONG_COMMENT);
        triggeringEvent.setSummary(SHORT_COMMENT);
        caseDetailsToSave.setEvent(triggeringEvent);
        Map data = mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        caseDetailsToSave.setData(data);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);

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
        final String DESCRIPTION = "A very long comment.......";
        final String SUMMARY = "Short comment";

        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(DESCRIPTION);
        triggeringEvent.setSummary(SUMMARY);
        caseDetailsToSave.setEvent(triggeringEvent);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);


        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Expected empty case data", "", mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data").asText());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertTrue("Incorrect Case Reference", uidService.validateUID(String.valueOf(savedCaseDetails.getReference())));
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
        assertEquals("Description", DESCRIPTION, caseAuditEvent.getDescription());
        assertEquals("Summary", SUMMARY, caseAuditEvent.getSummary());
        assertTrue(caseAuditEvent.getDataClassification().isEmpty());
        assertThat(caseAuditEvent.getSecurityClassification(), equalTo(PRIVATE));
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseWithNoDataForCitizen() throws Exception {
        final String DESCRIPTION = "A very long comment.......";
        final String SUMMARY = "Short comment";

        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(DESCRIPTION);
        triggeringEvent.setSummary(SUMMARY);
        caseDetailsToSave.setEvent(triggeringEvent);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);


        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Expected empty case data", "", mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data").asText());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertTrue("Incorrect Case Reference", uidService.validateUID(String.valueOf(savedCaseDetails.getReference())));
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
        assertEquals("Description", DESCRIPTION, caseAuditEvent.getDescription());
        assertEquals("Summary", SUMMARY, caseAuditEvent.getSummary());
        assertTrue(caseAuditEvent.getDataClassification().isEmpty());
        assertThat(caseAuditEvent.getSecurityClassification(), equalTo(PRIVATE));
    }

    @Test
    public void shouldReturn201WithEmptyBodyWhenPostCreateCaseWithNoReadAccessOnCaseTypeForCaseworker() throws Exception {
        shouldReturn201WithEmptyBodyWhenPostCreateCaseWithNoReadAccessOnCaseType("caseworkers");
    }

    @Test
    public void shouldReturn201WithEmptyBodyWhenPostCreateCaseWithNoReadAccessOnCaseTypeForCitizen() throws Exception {
        shouldReturn201WithEmptyBodyWhenPostCreateCaseWithNoReadAccessOnCaseType("citizens");
    }

    @Test
    public void shouldReturn201WithFieldRemovedWhenPostCreateCaseWithNoFieldReadAccessForCaseworker() throws Exception {
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
                .perform(get("/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/1504259907353529")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andReturn();

            final CaseDetails caseDetails = mapper.readValue(result.getResponse().getContentAsString(), CaseDetails.class);

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
            assertEquals("HX08 UTG", caseDetails.getData().get("PersonAddress").get("Postcode").asText());
            assertEquals("http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1",
                caseDetails.getData().get("D8Document").get("document_url").asText());
            assertEquals("http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1/binary",
                caseDetails.getData().get("D8Document").get("document_binary_url").asText());
            assertEquals("Seagulls_Square.jpg",
                caseDetails.getData().get("D8Document").get("document_filename").asText());
            assertEquals(4, caseDetails.getDataClassification().size());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonFirstName").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonLastName").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("classification").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value").get("AddressLine1").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value").get("AddressLine2").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value").get("AddressLine3").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value").get("Country").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value").get("Postcode").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("D8Document").asText());
        }

        {
            final MvcResult result = mockMvc
                .perform(get("/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/1504259907353537")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andReturn();

            final CaseDetails caseDetails = mapper.readValue(result.getResponse().getContentAsString(), CaseDetails.class);

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
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("classification").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value").get("AddressLine1").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value").get("AddressLine2").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value").get("AddressLine3").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value").get("Country").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value").get("Postcode").asText());
        }
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_private_cases.sql"})
    public void shouldReturn404WhenGetCaseClassificationTooHighForCaseworker() throws Exception {

        mockMvc
            .perform(
                get("/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/1504259907353545")
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
                .perform(get("/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/1504259907353529")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andReturn();

            final CaseDetails caseDetails = mapper.readValue(result.getResponse().getContentAsString(), CaseDetails.class);

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
            assertEquals("HX08 UTG", caseDetails.getData().get("PersonAddress").get("Postcode").asText());
            assertEquals("http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1",
                caseDetails.getData().get("D8Document").get("document_url").asText());
            assertEquals("http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1/binary",
                caseDetails.getData().get("D8Document").get("document_binary_url").asText());
            assertEquals("Seagulls_Square.jpg",
                caseDetails.getData().get("D8Document").get("document_filename").asText());
            assertEquals(4, caseDetails.getDataClassification().size());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonFirstName").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonLastName").asText());
            assertEquals("PUBLIC",  caseDetails.getDataClassification().get("PersonAddress").get("classification").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value").get("AddressLine1").asText());
            assertEquals("PUBLIC",  caseDetails.getDataClassification().get("PersonAddress").get("classification").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("D8Document").asText());
        }

        {
            final MvcResult result = mockMvc
                .perform(get("/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/1504259907353537")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andReturn();

            final CaseDetails caseDetails = mapper.readValue(result.getResponse().getContentAsString(), CaseDetails.class);

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
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value").get("AddressLine1").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value").get("AddressLine2").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value").get("AddressLine3").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value").get("Country").asText());
            assertEquals("PUBLIC", caseDetails.getDataClassification().get("PersonAddress").get("value").get("Postcode").asText());
        }
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

        final String TEST_CASE_TYPE = "TestAddressBookCase";
        final String TEST_JURISDICTION = "TEST";

        {
            mockMvc
                .perform(get("/caseworkers/0/jurisdictions/" + TEST_JURISDICTION + "/case-types/" + TEST_CASE_TYPE + "/cases/1504259907353528")
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

        final String TEST_CASE_TYPE = "TestAddressBookCase";
        final String TEST_JURISDICTION = "TEST";

        {
            mockMvc
                .perform(get("/citizens/0/jurisdictions/" + TEST_JURISDICTION + "/case-types/" + TEST_CASE_TYPE + "/cases/1504259907353528")
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
            .perform(get("/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/9999999999999995")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is(404));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenGetCaseWithNonExistentCaseReferenceForCitizen() throws Exception {
        // Check that we have the expected test data set size, this is to ensure that state filtering is correct
        assertCaseDataResultSetSize();

        mockMvc
            .perform(get("/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/9999999999999995")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is(404));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithNoDataForCaseworker() throws Exception {
        final String CASE_REFERENCE = "1504259907353545";
        final String SUMMARY = "Case event summary";
        final String DESCRIPTION = "Case event description";
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(PRE_STATES_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);
        final JsonNode DATA = mapper.readTree("{" +
                                              "\"PersonAddress\":{" +
                                              "\"Country\":\"Wales\"," +
                                              "\"Postcode\":\"W11 5DF\"," +
                                              "\"AddressLine1\":\"Flat 9\"," +
                                              "\"AddressLine2\":\"2 Hubble Avenue\"," +
                                              "\"AddressLine3\":\"ButtonVillie\"}," +
                                              "\"PersonLastName\":\"Roof\"," +
                                              "\"PersonFirstName\":\"George\"}");
        final String EXPECTED_CLASSIFICATION_STRING = "{" +
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
                                                UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, PRE_STATES_EVENT_ID);
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
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> CASE_REFERENCE.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("Incorrect Case Type", CASE_TYPE, savedCaseDetails.getCaseTypeId());
        assertEquals("Incorrect Data content: Data should not have changed", mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        }), savedCaseDetails.getData());
        assertEquals("State should have been updated", "state4", savedCaseDetails.getState());
        JSONAssert.assertEquals(EXPECTED_CLASSIFICATION_STRING, mapper.convertValue(savedCaseDetails.getDataClassification(), JsonNode.class).toString(), JSONCompareMode.LENIENT);

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
        JSONAssert.assertEquals(EXPECTED_CLASSIFICATION_STRING, mapper.convertValue(caseAuditEvent.getDataClassification(), JsonNode.class).toString(), JSONCompareMode.LENIENT);
    }


    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithNoDataForCitizen() throws Exception {
        final String CASE_REFERENCE = "1504259907353545";
        final String SUMMARY = "Case event summary";
        final String DESCRIPTION = "Case event description";
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(PRE_STATES_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);
        final JsonNode DATA = mapper.readTree("{" +
                                              "\"PersonAddress\":{" +
                                              "\"Country\":\"Wales\"," +
                                              "\"Postcode\":\"W11 5DF\"," +
                                              "\"AddressLine1\":\"Flat 9\"," +
                                              "\"AddressLine2\":\"2 Hubble Avenue\"," +
                                              "\"AddressLine3\":\"ButtonVillie\"}," +
                                              "\"PersonLastName\":\"Roof\"," +
                                              "\"PersonFirstName\":\"George\"}");
        String EXPECTED_CLASSIFICATION_STRING = "{" +
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
                                                UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, PRE_STATES_EVENT_ID);
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
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> CASE_REFERENCE.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("Incorrect Case Type", CASE_TYPE, savedCaseDetails.getCaseTypeId());
        assertEquals("Incorrect Data content: Data should not have changed", mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        }), savedCaseDetails.getData());
        assertEquals("State should have been updated", "state4", savedCaseDetails.getState());
        JSONAssert.assertEquals(EXPECTED_CLASSIFICATION_STRING, mapper.convertValue(savedCaseDetails.getDataClassification(), JsonNode.class).toString(), JSONCompareMode.LENIENT);

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
        JSONAssert.assertEquals(EXPECTED_CLASSIFICATION_STRING, mapper.convertValue(caseAuditEvent.getDataClassification(), JsonNode.class).toString(), JSONCompareMode.LENIENT);
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
        final String caseReference = "1504259907353545";
        final String summary = "Case event summary";
        final String description = "Case event description";
        final String url = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + urlPortionForCaseType
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(TEST_EVENT_ID);
        event.setSummary(summary);
        event.setDescription(description);
        caseDetailsToSave.setEvent(event);
        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, caseReference, TEST_EVENT_ID);
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
        caseDetailsToSave.setData(mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {}));

        final MvcResult mvcResult = mockMvc.perform(post(url)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Incorrect Response Content",
            DATA.toString(),
            mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data").toString());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("Incorrect Case Type", CASE_TYPE, savedCaseDetails.getCaseTypeId());
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
        assertEquals(summary, caseAuditEvent.getSummary());
        assertEquals(description, caseAuditEvent.getDescription());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithNoPreStateCheckForCitizen() throws Exception {
        final String urlPortionForCaseType = "bookcase-default-pre-state-test";
        final String caseReference = "1504259907353545";
        final String summary = "Case event summary";
        final String description = "Case event description";
        final String url = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + urlPortionForCaseType
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(TEST_EVENT_ID);
        event.setSummary(summary);
        event.setDescription(description);
        caseDetailsToSave.setEvent(event);
        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, caseReference, TEST_EVENT_ID);
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
        caseDetailsToSave.setData(mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {}));

        final MvcResult mvcResult = mockMvc.perform(post(url)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Incorrect Response Content",
            DATA.toString(),
            mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data").toString());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("Incorrect Case Type", CASE_TYPE, savedCaseDetails.getCaseTypeId());
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
        assertEquals(summary, caseAuditEvent.getSummary());
        assertEquals(description, caseAuditEvent.getDescription());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithNoChangesToPostStateForCaseworker() throws Exception {
        final String caseTypeUrlPortion = "bookcase-default-post-state";
        final String caseReference = "1504259907353545";
        final String summary = "Case event summary";
        final String description = "Case event description";
        final String url = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + caseTypeUrlPortion
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(TEST_EVENT_ID);
        event.setSummary(summary);
        event.setDescription(description);

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, caseReference, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(event);
        final JsonNode data = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"_ WB11DDF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"}");
        caseDetailsToSave.setData(mapper.convertValue(data, new TypeReference<HashMap<String, JsonNode>>() {}));

        final MvcResult mvcResult = mockMvc.perform(post(url)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Incorrect Response Content",
            data.toString(),
            mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data").toString());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("Incorrect Case Type", CASE_TYPE, savedCaseDetails.getCaseTypeId());
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
        assertEquals(summary, caseAuditEvent.getSummary());
        assertEquals(description, caseAuditEvent.getDescription());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithNoChangesToPostStateForCitizen() throws Exception {
        final String caseTypeUrlPortion = "bookcase-default-post-state";
        final String caseReference = "1504259907353545";
        final String summary = "Case event summary";
        final String description = "Case event description";
        final String url = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + caseTypeUrlPortion
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(TEST_EVENT_ID);
        event.setSummary(summary);
        event.setDescription(description);

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, caseReference, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(event);
        final JsonNode data = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"_ WB11DDF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"}");
        caseDetailsToSave.setData(mapper.convertValue(data, new TypeReference<HashMap<String, JsonNode>>() {}));

        final MvcResult mvcResult = mockMvc.perform(post(url)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Incorrect Response Content",
            data.toString(),
            mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data").toString());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> caseReference.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("Incorrect Case Type", CASE_TYPE, savedCaseDetails.getCaseTypeId());
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
        assertEquals(summary, caseAuditEvent.getSummary());
        assertEquals(description, caseAuditEvent.getDescription());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WithEmptyBodyWhenPostCreateCaseEventWithNoCaseTypeReadAccessForCaseworker() throws Exception {
        shouldReturn201WithEmptyBodyWhenPostCreateCaseEventWithNoCaseTypeReadAccess("caseworkers");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WithEmptyBodyWhenPostCreateCaseEventWithNoCaseTypeReadAccessForCitizen() throws Exception {
        shouldReturn201WithEmptyBodyWhenPostCreateCaseEventWithNoCaseTypeReadAccess("citizens");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WithFieldRemovedWhenPostCreateCaseEventWithNoFieldReadAccessForCaseworker() throws Exception {
        shouldReturn201WithFieldRemovedWhenPostCreateCaseEventWithNoFieldReadAccess("caseworkers");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WithFieldRemovedWhenPostCreateCaseEventWithNoFieldReadAccessForCitizen() throws Exception {
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
        final String CASE_REFERENCE = "1504259907353545";
        final String DESCRIPTION = "Case event description";
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(PRE_STATES_EVENT_ID);
        event.setSummary(null);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);

        final String token = generateEventToken(template,
                                                UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, PRE_STATES_EVENT_ID);
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
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> CASE_REFERENCE.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should have been updated", "state4", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithNoSummaryForCitizen() throws Exception {
        final String CASE_REFERENCE = "1504259907353545";
        final String DESCRIPTION = "Case event description";
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(PRE_STATES_EVENT_ID);
        event.setSummary(null);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);

        final String token = generateEventToken(template,
                                                UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, PRE_STATES_EVENT_ID);
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
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> CASE_REFERENCE.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should have been updated", "state4", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithBlankSummaryForCaseWorker() throws Exception {
        final String CASE_REFERENCE = "1504259907353545";
        final String SUMMARY = "        ";
        final String DESCRIPTION = "Case event summary";
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(PRE_STATES_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);
        final String token = generateEventToken(template,
                                                UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, PRE_STATES_EVENT_ID);
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
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> CASE_REFERENCE.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should have been updated", "state4", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenPostCreateCaseEventWithBlankSummaryForCitizen() throws Exception {
        final String CASE_REFERENCE = "1504259907353545";
        final String SUMMARY = "        ";
        final String DESCRIPTION = "Case event summary";
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(PRE_STATES_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);
        final String token = generateEventToken(template,
                                                UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, PRE_STATES_EVENT_ID);
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
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> CASE_REFERENCE.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should have been updated", "state4", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn422WhenPostCreateCaseEventWithSummaryTooLongForCaseWorker() throws Exception {
        final String CASE_REFERENCE = "1504259907353545";
        final String SUMMARY = new String(new char[1025]).replace("\0", "-");
        final String DESCRIPTION = "Case event description";
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(PRE_STATES_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);

        final String token = generateEventToken(template,
                                                UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, PRE_STATES_EVENT_ID);
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
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> CASE_REFERENCE.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should NOT have been updated", "CaseCreated", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn422WhenPostCreateCaseEventWithSummaryTooLongForCitizen() throws Exception {
        final String CASE_REFERENCE = "1504259907353545";
        final String SUMMARY = new String(new char[1025]).replace("\0", "-");
        final String DESCRIPTION = "Case event description";
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(PRE_STATES_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);

        final String token = generateEventToken(template,
                                                UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, PRE_STATES_EVENT_ID);
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
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> CASE_REFERENCE.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should NOT have been updated", "CaseCreated", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn422WhenPostCreateCaseEventWithDescriptionTooLongForCaseWorker() throws Exception {
        final String CASE_REFERENCE = "1504259907353545";
        final String SUMMARY = "Valid summary";
        final String DESCRIPTION = new String(new char[65666]).replace("\0", "-");
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(PRE_STATES_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);

        final String token = generateEventToken(template,
                                                UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, PRE_STATES_EVENT_ID);
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
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> CASE_REFERENCE.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should NOT have been updated", "CaseCreated", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn422WhenPostCreateCaseEventWithDescriptionTooLongForCitizen() throws Exception {
        final String CASE_REFERENCE = "1504259907353545";
        final String SUMMARY = "Valid summary";
        final String DESCRIPTION = new String(new char[65666]).replace("\0", "-");
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(PRE_STATES_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);

        final String token = generateEventToken(template,
                                                UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, PRE_STATES_EVENT_ID);
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
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> CASE_REFERENCE.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should NOT have been updated", "CaseCreated", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenPostCreateCaseEventWithNoEventIdForCaseworker() throws Exception {
        final String CASE_REFERENCE = "1504259907353545";
        final String SUMMARY = "Case event summary";
        final String DESCRIPTION = "Case event description";
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(null);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);

        final String token = generateEventToken(template,
                                                UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, PRE_STATES_EVENT_ID);
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
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> CASE_REFERENCE.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should NOT have been updated", "CaseCreated", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenPostCreateCaseEventWithNoEventIdForCitizen() throws Exception {
        final String CASE_REFERENCE = "1504259907353545";
        final String SUMMARY = "Case event summary";
        final String DESCRIPTION = "Case event description";
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(null);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);

        final String token = generateEventToken(template,
                                                UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, PRE_STATES_EVENT_ID);
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
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> CASE_REFERENCE.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should NOT have been updated", "CaseCreated", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenPostCreateCaseEventWithNoEventForCaseWorker() throws Exception {
        final String CASE_REFERENCE = "1504259907353545";
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, "ANY_EVENT");
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
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> CASE_REFERENCE.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should NOT have been updated", "CaseCreated", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenPostCreateCaseEventWithNoEventForCitizen() throws Exception {
        final String CASE_REFERENCE = "1504259907353545";
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, "ANY_EVENT");
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
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> CASE_REFERENCE.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("State should NOT have been updated", "CaseCreated", savedCaseDetails.getState());
        assertNotNull(savedCaseDetails.getDataClassification());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenPostCreateCaseEventWithNonExistentCaseIdForCaseWorker() throws Exception {
        final String CASE_REFERENCE = "9999999999999995";
        final String SUMMARY = "Case event summary";
        final String DESCRIPTION = "Case event description";
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(PRE_STATES_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);

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
        final String CASE_REFERENCE = "9999999999999995";
        final String SUMMARY = "Case event summary";
        final String DESCRIPTION = "Case event description";
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(PRE_STATES_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);

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
        final String CASE_REFERENCE = "invalidReference";
        final String EVENT = "HAS_PRE_STATES_EVENT";
        final String SUMMARY = "Case event summary";
        final String DESCRIPTION = "Case event description";
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(EVENT);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, 1504259907353537L, EVENT);
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
        final String CASE_REFERENCE = "invalidReference";
        final String EVENT = "HAS_PRE_STATES_EVENT";
        final String SUMMARY = "Case event summary";
        final String DESCRIPTION = "Case event description";
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(EVENT);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, 1504259907353537L, EVENT);
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
        final String CASE_REFERENCE = "1504259907353545";
        final String SUMMARY = "Case event summary";
        final String DESCRIPTION = "Case event description";
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(PRE_STATES_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);
        final JsonNode INVALID_DATA = mapper.readTree("{ \"XXX\": \"YYY\" }");
        final JsonNode INITIAL_DATA = mapper.readTree("{" +
                                                      "\"PersonAddress\":{" +
                                                      "\"Country\":\"Wales\"," +
                                                      "\"Postcode\":\"W11 5DF\"," +
                                                      "\"AddressLine1\":\"Flat 9\"," +
                                                      "\"AddressLine2\":\"2 Hubble Avenue\"," +
                                                      "\"AddressLine3\":\"ButtonVillie\"}," +
                                                      "\"PersonLastName\":\"Roof\"," +
                                                      "\"PersonFirstName\":\"George\"}");
        final Map<String, JsonNode> expectedData = mapper.convertValue(INITIAL_DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });

        caseDetailsToSave.setData(mapper.convertValue(INVALID_DATA, new TypeReference<HashMap<String, JsonNode>>() {
        }));

        final String token = generateEventToken(template,
                                                UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))).andExpect(status().is(404))
            .andReturn();

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> CASE_REFERENCE.equals(c.getReference().toString()))
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
        final String CASE_REFERENCE = "1504259907353545";
        final String SUMMARY = "Case event summary";
        final String DESCRIPTION = "Case event description";
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(PRE_STATES_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);
        final JsonNode INVALID_DATA = mapper.readTree("{ \"XXX\": \"YYY\" }");
        final JsonNode INITIAL_DATA = mapper.readTree("{" +
                                                      "\"PersonAddress\":{" +
                                                      "\"Country\":\"Wales\"," +
                                                      "\"Postcode\":\"W11 5DF\"," +
                                                      "\"AddressLine1\":\"Flat 9\"," +
                                                      "\"AddressLine2\":\"2 Hubble Avenue\"," +
                                                      "\"AddressLine3\":\"ButtonVillie\"}," +
                                                      "\"PersonLastName\":\"Roof\"," +
                                                      "\"PersonFirstName\":\"George\"}");
        final Map<String, JsonNode> expectedData = mapper.convertValue(INITIAL_DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });

        caseDetailsToSave.setData(mapper.convertValue(INVALID_DATA, new TypeReference<HashMap<String, JsonNode>>() {
        }));

        final String token = generateEventToken(template,
                                                UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))).andExpect(status().is(404))
            .andReturn();

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> CASE_REFERENCE.equals(c.getReference().toString()))
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
        final String CASE_REFERENCE = "1504259907353545";
        final String SUMMARY = "Case event summary";
        final String DESCRIPTION = "Case event description";
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(PRE_STATES_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);
        final String token = generateEventToken(template,
                                                UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        // Simulate case alteration by other actor to fail event token version check
        template.update("UPDATE case_data SET data = '{}' WHERE reference = ?", CASE_REFERENCE);

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
        final String CASE_REFERENCE = "1504259907353545";
        final String SUMMARY = "Case event summary";
        final String DESCRIPTION = "Case event description";
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(PRE_STATES_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);
        final String token = generateEventToken(template,
                                                UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        // Simulate case alteration by other actor to fail event token version check
        template.update("UPDATE case_data SET data = '{}' WHERE reference = ?", CASE_REFERENCE);

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
    public void shouldReturn409WhenPostCreateCaseEventWithCaseStateConflictForCaseWorker() throws Exception {
        final String CASE_REFERENCE = "1504259907353545";
        final String SUMMARY = "Case event summary";
        final String DESCRIPTION = "Case event description";
        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(PRE_STATES_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);
        final String token = generateEventToken(template,
                                                UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        // Simulate case state alteration by other actor to fail event token version check
        template.update("UPDATE case_data SET state = 'CaseStopped' WHERE reference = ?", CASE_REFERENCE);

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
    public void shouldReturn409WhenPostCreateCaseEventWithCaseStateConflictForCitizen() throws Exception {
        final String CASE_REFERENCE = "1504259907353545";
        final String SUMMARY = "Case event summary";
        final String DESCRIPTION = "Case event description";
        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(PRE_STATES_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);
        final String token = generateEventToken(template,
                                                UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        // Simulate case state alteration by other actor to fail event token version check
        template.update("UPDATE case_data SET state = 'CaseStopped' WHERE reference = ?", CASE_REFERENCE);

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
        final String DESCRIPTION = "A very long comment.......";
        final String SUMMARY = "Short comment";

        final String URL = "/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE_NO_CREATE_FIELD_ACCESS + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(DESCRIPTION);
        triggeringEvent.setSummary(SUMMARY);
        caseDetailsToSave.setEvent(triggeringEvent);
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
        Map data = mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        caseDetailsToSave.setData(data);


        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();
    }

    private void shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccess(String role) throws Exception {
        final String URL = "/" + role + "/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE_NO_CREATE_CASE_ACCESS + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();


        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();
    }

    private void shouldReturn201WithFieldRemovedWhenPostCreateCaseWithNoFieldReadAccess(String userRole) throws Exception {
        final String DESCRIPTION = "A very long comment.......";
        final String SUMMARY = "Short comment";

        final String URL = "/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE_NO_READ_FIELD_ACCESS + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(CREATE_EVENT_ID);
        triggeringEvent.setDescription(DESCRIPTION);
        triggeringEvent.setSummary(SUMMARY);
        caseDetailsToSave.setEvent(triggeringEvent);
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
        Map data = mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        caseDetailsToSave.setData(data);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE_NO_READ_FIELD_ACCESS, CREATE_EVENT_ID);
        caseDetailsToSave.setToken(token);


        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        final JsonNode SANITIZED_DATA = mapper.readTree(
            "{\n" +
                "  \"PersonLastName\": \"Last Name\",\n" +
                "  \"PersonAddress\": {\n" +
                "    \"AddressLine1\": \"Address Line 1\",\n" +
                "    \"AddressLine2\": \"Address Line 2\"\n" +
                "  }\n" +
                "}\n"
        );
        Map expectedSanitizedData = mapper.readValue(SANITIZED_DATA.toString(), Map.class);
        JsonNode case_data = mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data");
        JsonNode data_classification = mapper.readTree(mvcResult.getResponse().getContentAsString())
            .get("data_classification");
        Map actualData = mapper.readValue(case_data.toString(), Map.class);
        assertAll(() -> assertThat("Incorrect Response Content",
                                   actualData.entrySet(),
                                   everyItem(isIn(expectedSanitizedData.entrySet()))),
                  () -> assertThat("Response contains filtered out data", case_data.has("PersonFirstName"), is(false)),
                  () -> assertThat(data_classification.has("PersonFirstName"), CoreMatchers.is(false)),
                  () -> assertThat(data_classification.has("PersonLastName"), CoreMatchers.is(true)),
                  () -> assertThat(data_classification.has("PersonAddress"), CoreMatchers.is(true)));

    }


    private void shouldReturn404WhenPostCreateCaseWithNoCreateEventAccess(String role) throws Exception {
        final String DESCRIPTION = "A very long comment.......";
        final String SUMMARY = "Short comment";

        final String URL = "/" + role + "/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE_NO_CREATE_EVENT_ACCESS + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(DESCRIPTION);
        triggeringEvent.setSummary(SUMMARY);
        caseDetailsToSave.setEvent(triggeringEvent);


        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();
    }

    private void shouldReturn201WithEmptyBodyWhenPostCreateCaseWithNoReadAccessOnCaseType(String role) throws Exception {
        final String DESCRIPTION = "A very long comment.......";
        final String SUMMARY = "Short comment";

        final String URL = "/" + role + "/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE_NO_READ_CASE_TYPE_ACCESS + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(CREATE_EVENT_ID);
        triggeringEvent.setDescription(DESCRIPTION);
        triggeringEvent.setSummary(SUMMARY);
        caseDetailsToSave.setEvent(triggeringEvent);
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
        Map data = mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
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

    private void shouldReturn201WithEmptyBodyWhenPostCreateCaseEventWithNoCaseTypeReadAccess(String userRole) throws Exception {
        final String caseReference = "1504259907353610";
        final String summary = "Case event summary";
        final String description = "Case event description";
        final String url = "/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE_NO_READ_CASE_TYPE_ACCESS
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(TEST_EVENT_ID);
        event.setSummary(summary);
        event.setDescription(description);

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, caseReference, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(event);
        final JsonNode data = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"_ WB11DDF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"}");
        caseDetailsToSave.setData(mapper.convertValue(data, new TypeReference<HashMap<String, JsonNode>>() {}));

        final MvcResult mvcResult = mockMvc.perform(post(url)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        assertThat(response, CoreMatchers.is(isEmptyString()));
    }


    private void shouldReturn201WithFieldRemovedWhenPostCreateCaseEventWithNoFieldReadAccess(String userRole) throws Exception {
        final String caseReference = "1504259907353628";
        final String summary = "Case event summary";
        final String description = "Case event description";
        final String url = "/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE_NO_READ_FIELD_ACCESS
            + "/cases/" + caseReference + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(TEST_EVENT_ID);
        event.setSummary(summary);
        event.setDescription(description);

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, caseReference, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(event);
        final JsonNode data = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"_ WB11DDF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"}");
        caseDetailsToSave.setData(mapper.convertValue(data, new TypeReference<HashMap<String, JsonNode>>() {}));

        final MvcResult mvcResult = mockMvc.perform(post(url)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        JsonNode nodeData = mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data");

        JsonNode nodeClassification = mapper.readTree(mvcResult.getResponse().getContentAsString())
            .get("data_classification");
        assertAll(() -> assertThat(nodeData.has("PersonFirstName"), CoreMatchers.is(false)),
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
                  () -> assertThat(nodeClassification.get("PersonAddress").get("value").has("Country"), CoreMatchers.is(true)),
                  () -> assertThat(nodeClassification.get("PersonAddress").get("value").has("Postcode"), CoreMatchers.is(true)),
                  () -> assertThat(nodeClassification.get("PersonAddress").get("value").has("AddressLine1"), CoreMatchers.is(true)),
                  () -> assertThat(nodeClassification.get("PersonAddress").get("value").has("AddressLine2"), CoreMatchers.is(true)),
                  () -> assertThat(nodeClassification.get("PersonAddress").get("value").has("AddressLine3"), CoreMatchers.is(true))
        );
    }

    private void shouldReturn404WhenPostCreateCaseEventWithNoUpdateCaseAccess(String userRole) throws Exception {
        final String caseReference = "1504259907353578";
        final String URL = "/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE_NO_UPDATE_CASE_ACCESS  + "/cases/" + caseReference + "/events";
        final String summary = "Case event summary";
        final String description = "Case event description";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(TEST_EVENT_ID);
        event.setSummary(summary);
        event.setDescription(description);

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, caseReference, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(event);
        final JsonNode data = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"_ WB11DDF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"}");
        caseDetailsToSave.setData(mapper.convertValue(data, new TypeReference<HashMap<String, JsonNode>>() {}));


        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();
    }

    private void shouldReturn404WhenPostCreateCaseEventWithNoCreateEventAccess(String userRole) throws Exception {
        final String caseReference = "1504259907353586";
        final String URL = "/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE_NO_CREATE_EVENT_ACCESS  + "/cases/" + caseReference + "/events";
        final String summary = "Case event summary";
        final String description = "Case event description";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(TEST_EVENT_ID);
        event.setSummary(summary);
        event.setDescription(description);

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, caseReference, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(event);
        final JsonNode data = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"_ WB11DDF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"}");
        caseDetailsToSave.setData(mapper.convertValue(data, new TypeReference<HashMap<String, JsonNode>>() {}));


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
            () -> assertThat("Created_date is not present", MAPPER.readTree(actual).get("case_details").has("created_date"), is(true))
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
                                   CoreMatchers.is(true)));
    }

    private void shouldReturn404WhenPostCreateCaseEventWithNoCreateFieldAccess(String userRole) throws Exception {
        final String caseReference = "1504259907353594";
        final String URL = "/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE_NO_CREATE_FIELD_ACCESS  + "/cases/" + caseReference + "/events";
        final String summary = "Case event summary";
        final String description = "Case event description";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(TEST_EVENT_ID);
        event.setSummary(summary);
        event.setDescription(description);

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, caseReference, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(event);
        final JsonNode data = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"_ WB11DDF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"}");
        caseDetailsToSave.setData(mapper.convertValue(data, new TypeReference<HashMap<String, JsonNode>>() {}));


        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();
    }

    private void shouldReturn404WhenPostCreateCaseEventWithNoUpdateFieldAccess(String userRole) throws Exception {
        final String caseReference = "1504259907353602";
        final String URL = "/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE_NO_UPDATE_FIELD_ACCESS  + "/cases/" + caseReference + "/events";
        final String summary = "Case event summary";
        final String description = "Case event description";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(TEST_EVENT_ID);
        event.setSummary(summary);
        event.setDescription(description);

        final String token = generateEventToken(template, UID, JURISDICTION, CASE_TYPE, caseReference, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);
        caseDetailsToSave.setEvent(event);
        final JsonNode data = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"_ WB11DDF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"}");
        caseDetailsToSave.setData(mapper.convertValue(data, new TypeReference<HashMap<String, JsonNode>>() {}));


        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();
    }

    private void shouldReturn404WhenGetCaseWithNoCaseTypeReadAccess(String userRole) throws Exception {
        final String caseReference = "1504259907353610";
        final String url = "/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE_NO_READ_CASE_TYPE_ACCESS
            + "/cases/" + caseReference;

        mockMvc.perform(get(url)
            .contentType(JSON_CONTENT_TYPE)
        ).andExpect(status().is(404))
            .andReturn();
    }

    private void shouldReturn200WithFieldRemovedWhenGetValidCaseWithNoFieldReadAccess(String userRole) throws Exception {
        // Check that we have the expected test data set size, this is to ensure that state filtering is correct
        assertCaseDataResultSetSize();

        {
            final MvcResult result = mockMvc
                .perform(get("/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE_NO_READ_FIELD_ACCESS + "/cases/1504259907353628")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andReturn();

            final CaseDetails caseDetails = mapper.readValue(result.getResponse().getContentAsString(), CaseDetails.class);

            assertEquals(1504259907353628L, caseDetails.getReference().longValue());

            JsonNode nodeData = mapper.convertValue(caseDetails.getData(), JsonNode.class);
            assertAll(
                () -> assertThat(nodeData.has("PersonFirstName"), CoreMatchers.is(false)),
                () -> assertThat(nodeData.get("PersonLastName"), CoreMatchers.is(getTextNode("Parker"))),
                () -> assertThat(nodeData.has("PersonAddress"), CoreMatchers.is(true)),
                () -> assertThat(nodeData.get("PersonAddress").get("Country"), CoreMatchers.is(getTextNode("England"))),
                () -> assertThat(nodeData.get("PersonAddress").get("Postcode"), CoreMatchers.is(getTextNode("HX08 UTG"))),
                () -> assertThat(nodeData.get("PersonAddress").get("AddressLine1"), CoreMatchers.is(getTextNode("123"))),
                () -> assertThat(nodeData.get("PersonAddress").get("AddressLine2"), CoreMatchers.is(getTextNode("Fake Street"))),
                () -> assertThat(nodeData.get("PersonAddress").get("AddressLine3"), CoreMatchers.is(getTextNode("Hexton"))),
                () -> assertThat(nodeData.has("D8Document"), CoreMatchers.is(true)),
                () -> assertThat(nodeData.get("D8Document").get("document_url"), CoreMatchers.is(getTextNode("http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1"))),
                () -> assertThat(nodeData.get("D8Document").get("document_binary_url"), CoreMatchers.is(getTextNode("http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1/binary"))),
                () -> assertThat(nodeData.get("D8Document").get("document_filename"), CoreMatchers.is(getTextNode("Seagulls_Square.jpg")))
            );
            JsonNode nodeClassification = mapper.convertValue(caseDetails.getDataClassification(), JsonNode.class);
            assertAll(
                () -> assertThat(nodeClassification.has("PersonFirstName"), CoreMatchers.is(false)),
                () -> assertThat(nodeClassification.get("PersonLastName"), CoreMatchers.is(getTextNode("PUBLIC"))),
                () -> assertThat(nodeClassification.has("PersonAddress"), CoreMatchers.is(true)),
                () -> assertThat(nodeClassification.get("PersonAddress").get("classification"), CoreMatchers.is(getTextNode("PUBLIC"))),
                () -> assertThat(nodeClassification.get("PersonAddress").get("value").get("Country"), CoreMatchers.is(getTextNode("PUBLIC"))),
                () -> assertThat(nodeClassification.get("PersonAddress").get("value").get("Postcode"), CoreMatchers.is(getTextNode("PUBLIC"))),
                () -> assertThat(nodeClassification.get("PersonAddress").get("value").get("AddressLine1"), CoreMatchers.is(getTextNode("PUBLIC"))),
                () -> assertThat(nodeClassification.get("PersonAddress").get("value").get("AddressLine2"), CoreMatchers.is(getTextNode("PUBLIC"))),
                () -> assertThat(nodeClassification.get("PersonAddress").get("value").get("AddressLine3"), CoreMatchers.is(getTextNode("PUBLIC"))),
                () -> assertThat(nodeClassification.get("D8Document"), CoreMatchers.is(getTextNode("PUBLIC")))
            );
        }
    }

    private void shouldReturn201WhenPostCreateCaseEventWithValidData(String userRole) throws Exception {
        final String CASE_REFERENCE = "1504259907353545";
        final String SUMMARY = "Case event summary";
        final String DESCRIPTION = "Case event description";
        final String URL = "/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(PRE_STATES_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);
        final JsonNode DATA = mapper.readTree(exampleData());
        final JsonNode SANITIZED_DATA = mapper.readTree("{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"W11 5DF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"," +
            "\"D8Document\":{" +
            "    \"document_url\": \"http://localhost:" + getPort() + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\"," +
            "    \"document_binary_url\": \"http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0/binary\"," +
            "    \"document_filename\": \"Seagulls_Square.jpg\"" +
            "}" +
            "}");
        caseDetailsToSave.setData(mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {}));
        final String EXPECTED_CLASSIFICATION_STRING = "{" +
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
                                                UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Incorrect Response Content",
            SANITIZED_DATA.toString(),
            mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data").toString());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> CASE_REFERENCE.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("Incorrect Case Type", CASE_TYPE, savedCaseDetails.getCaseTypeId());
        Map sanitizedData = mapper.convertValue(SANITIZED_DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        assertThat(
            "Incorrect Data content: Data should have changed",
            savedCaseDetails.getData().entrySet(), everyItem(isIn(sanitizedData.entrySet())));
        assertEquals("State should have been updated", "state4", savedCaseDetails.getState());
        JSONAssert.assertEquals(EXPECTED_CLASSIFICATION_STRING, mapper.convertValue(savedCaseDetails.getDataClassification(), JsonNode.class).toString(), JSONCompareMode.LENIENT);

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
        JSONAssert.assertEquals(EXPECTED_CLASSIFICATION_STRING, mapper.convertValue(caseAuditEvent.getDataClassification(), JsonNode.class).toString(), JSONCompareMode.LENIENT);
    }

    private void shouldReturn201WhenPostCreateCaseEventWithExistingDocumentBinary(String userRole) throws Exception {
        final String CASE_REFERENCE = "1504259907353529";
        final String SUMMARY = "Case event summary";
        final String DESCRIPTION = "Case event description";
        final String URL = "/" + userRole + "/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/" + CASE_REFERENCE + "/events";
        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event event = anEvent().build();
        event.setEventId(PRE_STATES_EVENT_ID);
        event.setSummary(SUMMARY);
        event.setDescription(DESCRIPTION);
        caseDetailsToSave.setEvent(event);
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
        final JsonNode SANITIZED_DATA = mapper.readTree("{" +
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
            "    \"document_binary_url\": \"http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1/binary\"" +
            "}" +
            "}");
        caseDetailsToSave.setData(mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {}));
        final String token = generateEventToken(template,
                                                UID, JURISDICTION, CASE_TYPE, CASE_REFERENCE, PRE_STATES_EVENT_ID);
        caseDetailsToSave.setToken(token);

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Incorrect Response Content",
            SANITIZED_DATA.toString(),
            mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data").toString());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases: No case should be created", 16, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.stream()
            .filter(c -> CASE_REFERENCE.equals(c.getReference().toString()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedCaseDetails);
        assertEquals("Incorrect Case Type", CASE_TYPE, savedCaseDetails.getCaseTypeId());
        Map sanitizedData = mapper.convertValue(SANITIZED_DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        assertThat(
            "Incorrect Data content: Data should have changed",
            savedCaseDetails.getData().entrySet(), everyItem(isIn(sanitizedData.entrySet())));
    }

    @Test
    public void shouldReturn200WhenPostValidateCaseDetailsWithValidDataForCaseworker() throws Exception {
        final JsonNode DATA = mapper.readTree(exampleData());

        final String DESCRIPTION = "A very long comment.......";
        final String SUMMARY = "Short comment";
        final CaseDataContent caseDetailsToValidate = newCaseDataContent()
            .withEvent(anEvent()
                .withEventId(TEST_EVENT_ID)
                .withSummary(SUMMARY)
                .withDescription(DESCRIPTION)
                .build())
            .withToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID))
            .withData(mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {}))
            .withIgnoreWarning(Boolean.FALSE)
            .build();

        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/validate";
        final MvcResult mvcResult = mockMvc.perform(post(URL)
                                                        .contentType(JSON_CONTENT_TYPE)
                                                        .content(mapper.writeValueAsBytes(caseDetailsToValidate))
        ).andExpect(status().is(200)).andReturn();

        final JsonNode expectedResponse = MAPPER.readTree("{\"data\": " + exampleData() + "}");
        final String EXPECTED_RESPONSE = mapper.writeValueAsString(expectedResponse);
        assertEquals("Incorrect Response Content",
            EXPECTED_RESPONSE,
                     mapper.readTree(mvcResult.getResponse().getContentAsString()).toString());
    }

    @Test
    public void shouldReturn422WhenPostValidateCaseDetailsWithInvalidDataForCaseworker() throws Exception {
        final JsonNode DATA = mapper.readTree(exampleDataWithInvalidPostcode());
        final String DESCRIPTION = "A very long comment.......";
        final String SUMMARY = "Short comment";
        final CaseDataContent caseDetailsToValidate = newCaseDataContent()
            .withEvent(anEvent()
                .withEventId(TEST_EVENT_ID)
                .withSummary(SUMMARY)
                .withDescription(DESCRIPTION)
                .build())
            .withToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID))
            .withData(mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {}))
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
                     mapper.readTree(mvcResult.getResponse().getContentAsString()).get("details").get("field_errors").get(0).get("id").toString());
        assertEquals("Incorrect Response Content",
                     "\"Case data validation failed\"",
                     mapper.readTree(mvcResult.getResponse().getContentAsString()).get("message").toString());
    }

    @Test
    public void shouldReturn200WhenPostValidateCaseDetailsWithValidDataForCitizen() throws Exception {
        final JsonNode DATA = mapper.readTree(exampleData());

        final String DESCRIPTION = "A very long comment.......";
        final String SUMMARY = "Short comment";
        final CaseDataContent caseDetailsToValidate = newCaseDataContent()
            .withEvent(anEvent()
                .withEventId(TEST_EVENT_ID)
                .withSummary(SUMMARY)
                .withDescription(DESCRIPTION)
                .build())
            .withToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID))
            .withData(mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {}))
            .withIgnoreWarning(Boolean.FALSE)
            .build();

        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/validate";
        final MvcResult mvcResult = mockMvc.perform(post(URL)
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsBytes(caseDetailsToValidate))
                                                   ).andExpect(status().is(200)).andReturn();

        final JsonNode expectedResponse = MAPPER.readTree("{\"data\": " + exampleData() + "}");
        final String EXPECTED_RESPONSE = mapper.writeValueAsString(expectedResponse);
        assertEquals("Incorrect Response Content",
            EXPECTED_RESPONSE,
            mapper.readTree(mvcResult.getResponse().getContentAsString()).toString());
    }

    @Test
    public void shouldReturn422WhenPostValidateCaseDetailsWithInvalidDataForCitizen() throws Exception {
        final JsonNode DATA = mapper.readTree(exampleDataWithInvalidPostcode());
        final String DESCRIPTION = "A very long comment.......";
        final String SUMMARY = "Short comment";
        final CaseDataContent caseDetailsToValidate = newCaseDataContent()
            .withEvent(anEvent()
                .withEventId(TEST_EVENT_ID)
                .withSummary(SUMMARY)
                .withDescription(DESCRIPTION)
                .build())
            .withToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID))
            .withData(mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {}))
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
                     mapper.readTree(mvcResult.getResponse().getContentAsString()).get("details").get("field_errors").get(0).get("id").toString());
        assertEquals("Incorrect Response Content",
                     "\"Case data validation failed\"",
                     mapper.readTree(mvcResult.getResponse().getContentAsString()).get("message").toString());

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = { "classpath:sql/insert_cases.sql" })
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
        assertThat(caseDetails, (everyItem(hasProperty("jurisdiction", is(TEST_JURISDICTION)))));
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
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = { "classpath:sql/insert_cases.sql" })
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
        assertThat(result.getResponse().getContentAsString(), containsString(TEST_JURISDICTION));
        assertThat(result.getResponse().getContentAsString(), containsString(TEST_CASE_TYPE));
        assertThat(result.getResponse().getContentAsString(), containsString(TEST_STATE));
        assertThat(result.getResponse().getContentAsString(), containsString("Janet"));
        assertThat(result.getResponse().getContentAsString(), containsString("Parker"));
        assertThat(result.getResponse().getContentAsString(), containsString("Fake Street"));
        assertThat(result.getResponse().getContentAsString(), containsString("Hexton"));
        assertThat(result.getResponse().getContentAsString(), containsString("HX08 UTG"));
        assertThat(result.getResponse().getContentAsString(), containsString("http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = { "classpath:sql/insert_cases.sql" })
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
        assertThat(result.getResponse().getContentAsString(), containsString(TEST_JURISDICTION));
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
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = { "classpath:sql/insert_cases.sql" })
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
        assertThat(caseDetails, (everyItem(hasProperty("securityClassification", is(SecurityClassification.valueOf("PUBLIC"))))));


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
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = { "classpath:sql/insert_cases.sql" })
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
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = { "classpath:sql/insert_cases.sql" })
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
    public void shouldReturnEmptyResult_whenSearchWithNonMatchingCriteriaAsCaseWorker() throws Exception {

        assertCaseDataResultSetSize();

        final MvcResult result = mockMvc.perform(get(GET_CASES_AS_CASEWORKER)
                                                     .contentType(JSON_CONTENT_TYPE)
                                                     .param("case.PersonFirstName", "JanetX")
                                                     .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        List<CaseDetails> caseDetails = Arrays.asList(mapper.readValue(result.getResponse().getContentAsString(), CaseDetails[].class));

        assertThat(caseDetails, empty());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = { "classpath:sql/insert_cases.sql" })
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
        assertThat(caseDetailsPage3, hasSize(2)); //TODO RDM-1455 due to filtering being applied after pagination, to be fixed after EL implementation
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
        assertThat(references, hasSize(6)); //TODO RDM-1455 due to filtering being applied after pagination, to be fixed after EL implementation
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = { "classpath:sql/insert_cases.sql" })
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
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = { "classpath:sql/insert_cases.sql" })
    public void shouldReturn400_whenSearchWithBadRequestParamAsCaseWorker() throws Exception {

        assertCaseDataResultSetSize();

        mockMvc.perform(get(GET_CASES_AS_CASEWORKER).contentType(JSON_CONTENT_TYPE)
                            .param("case.PersonFirstName$", "JanetX")  // bad search param here
                            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(400))
            .andReturn();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = { "classpath:sql/insert_cases.sql" })
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

    /**
     * Checks that we have the expected test data set size, this is to ensure
     * that state filtering is correct.
     */
    private void assertCaseDataResultSetSize() {
        final int count = template.queryForObject("SELECT count(1) as n FROM case_data",Integer.class);
        assertEquals("Incorrect case data size", 16, count);
    }

    private JsonNode getTextNode(String value) {
        return JSON_NODE_FACTORY.textNode(value);
    }

    private String exampleDataWithInvalidPostcode() {
        return "{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"W11225DF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"," +
            "\"D8Document\":{" +
            "\"document_url\": \"http://localhost:" + getPort() + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\"" +
            "}" +
            "}";
    }

    private String exampleData() {
        return "{" +
            "\"PersonAddress\":{" +
            "\"Country\":\"_ Wales\"," +
            "\"Postcode\":\"W11 5DF\"," +
            "\"AddressLine1\":\"_ Flat 9\"," +
            "\"AddressLine2\":\"_ 2 Hubble Avenue\"," +
            "\"AddressLine3\":\"_ ButtonVillie\"}," +
            "\"PersonLastName\":\"_ Roof\"," +
            "\"PersonFirstName\":\"_ George\"," +
            "\"D8Document\":{" +
            "\"document_url\": \"http://localhost:" + getPort() + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0\"" +
            "}" +
            "}";
    }
}
