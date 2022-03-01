package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.GlobalSearchTestFixture;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.TestFixtures;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.AuditRepository;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.globalsearch.SearchPartyValue;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.v2.external.resource.CaseResource;
import uk.gov.hmcts.ccd.v2.external.resource.SupplementaryDataResource;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;
import static uk.gov.hmcts.ccd.v2.V2.EXPERIMENTAL_HEADER;
import static uk.gov.hmcts.ccd.v2.V2.Error.CASE_ID_INVALID;
import static uk.gov.hmcts.ccd.v2.V2.Error.CASE_NOT_FOUND;
import static uk.gov.hmcts.ccd.v2.V2.Error.NOT_AUTHORISED_UPDATE_SUPPLEMENTARY_DATA;

class CaseControllerTestIT extends WireMockBaseTest {

    private static final String CASE_TYPE = "TestAddressBookCase";
    private static final String CASE_TYPE_WITH_SEARCH_PARTY = "TestAddressBookCase2";
    private static final String CASE_TYPE_WITH_MULTIPLE_SEARCH_CRITERIA_AND_SEARCH_PARTY
        = "MultipleSearchCriteriaAndSearchParties";
    private static final String JURISDICTION = "PROBATE";
    private static final String TEST_EVENT_ID = "TEST_EVENT";
    private static final String UID = "123";
    private static final String CASE_TYPE_CREATOR_ROLE = "TestAddressBookCreatorCase";
    private static final String CASE_TYPE_CREATOR_ROLE_NO_CREATE_ACCESS = "TestAddressBookCreatorNoCreateAccessCase";
    private static final String REQUEST_ID = "request-id";
    private static final String REQUEST_ID_VALUE = "1234567898765432";
    public static final String ROLE_PROBATE_SOLICITOR = "caseworker-probate-solicitor";
    private static final String UID_WITH_EVENT_ACCESS = "123";

    @Inject
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @SpyBean
    private AuditRepository auditRepository;

    @BeforeEach
    public void setUp() {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldLogAuditInfoForGetCaseById() throws Exception {
        String caseId = "1504259907353529";
        final String URL = "/cases/" + caseId;

        final MvcResult mvcResult = mockMvc.perform(get(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .header(REQUEST_ID, REQUEST_ID_VALUE)
            .contentType(JSON_CONTENT_TYPE)
        ).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        assertNotNull(content, "Content Should not be null");
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);

        assertThat(savedCaseResource.getReference(), is(caseId));

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.CASE_ACCESSED.getLabel()));
        assertThat(captor.getValue().getCaseId(), is(savedCaseResource.getReference()));
        assertThat(captor.getValue().getIdamId(), is(UID));
        assertThat(captor.getValue().getInvokingService(), is(MockUtils.CCD_GW));
        assertThat(captor.getValue().getHttpStatus(), is(200));
        assertThat(captor.getValue().getCaseType(), is(CASE_TYPE));
        assertThat(captor.getValue().getJurisdiction(), is(JURISDICTION));
        assertThat(captor.getValue().getRequestId(), is(REQUEST_ID_VALUE));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldLogAuditInfoForGetCaseByIdWithNullRequestId() throws Exception {
        String caseId = "1504259907353529";
        final String URL = "/cases/" + caseId;

        final MvcResult mvcResult = mockMvc.perform(get(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .contentType(JSON_CONTENT_TYPE)
        ).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        assertNotNull(content, "Content Should not be null");
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);

        assertThat(savedCaseResource.getReference(), is(caseId));

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.CASE_ACCESSED.getLabel()));
        assertThat(captor.getValue().getCaseId(), is(savedCaseResource.getReference()));
        assertThat(captor.getValue().getIdamId(), is(UID));
        assertThat(captor.getValue().getInvokingService(), is(MockUtils.CCD_GW));
        assertThat(captor.getValue().getHttpStatus(), is(200));
        assertThat(captor.getValue().getCaseType(), is(CASE_TYPE));
        assertThat(captor.getValue().getJurisdiction(), is(JURISDICTION));
        assertNull(captor.getValue().getRequestId());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldLogAuditInfoForCreateEventByCaseId() throws Exception {
        String caseId = "1504259907353529";
        final String URL = "/cases/" + caseId + "/events";

        final CaseDataContent caseDetailsToSave = newCaseDataContent()
            .withCaseReference(caseId)
            .withEvent(anEvent()
                .withEventId("HAS_PRE_STATES_EVENT")
                .withSummary("Short comment")
                .build())
            .withToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, "HAS_PRE_STATES_EVENT"))
            .build();

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .header(REQUEST_ID, REQUEST_ID_VALUE)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andReturn();

        assertEquals(201, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull(savedCaseResource, "Saved Case Details should not be null");

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.UPDATE_CASE.getLabel()));
        assertThat(captor.getValue().getCaseId(), is(savedCaseResource.getReference()));
        assertThat(captor.getValue().getIdamId(), is(UID));
        assertThat(captor.getValue().getInvokingService(), is(MockUtils.CCD_GW));
        assertThat(captor.getValue().getHttpStatus(), is(201));
        assertThat(captor.getValue().getCaseType(), is(CASE_TYPE));
        assertThat(captor.getValue().getJurisdiction(), is(JURISDICTION));
        assertThat(captor.getValue().getEventSelected(), is("HAS_PRE_STATES_EVENT"));
        assertThat(captor.getValue().getRequestId(), is(REQUEST_ID_VALUE));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldSaveOnBehalfOfUserAndProxiedByUser() throws Exception {
        String caseId = "1504259907353529";
        final String URL = "/cases/" + caseId + "/events";

        UserInfo userInfo = UserInfo.builder()
            .uid("TestUserId")
            .givenName("firstname")
            .familyName("familyname")
            .build();
        stubFor(WireMock.get(urlMatching("/o/userinfo"))
            .withHeader(HttpHeaders.AUTHORIZATION, containing("Test_Token"))
            .willReturn(okJson(mapper.writeValueAsString(userInfo)).withStatus(200)));

        final CaseDataContent caseDetailsToSave = newCaseDataContent()
            .withCaseReference(caseId)
            .withOnBehalfOfUserToken("Test_Token")
            .withEvent(anEvent()
                .withEventId("HAS_PRE_STATES_EVENT")
                .withSummary("Short comment")
                .build())
            .withToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, "HAS_PRE_STATES_EVENT"))
            .build();

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .header(REQUEST_ID, REQUEST_ID_VALUE)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andReturn();

        assertEquals(201, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull(savedCaseResource, "Saved Case Details should not be null");
    }

    @Test
    public void shouldReturn201WhenPostCreateCase() throws Exception {
        final String URL = "/case-types/" + CASE_TYPE + "/cases";
        final String description = "A very long comment.......";
        final String summary = "Short comment";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(description);
        triggeringEvent.setSummary(summary);
        caseDetailsToSave.setEvent(triggeringEvent);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .header(REQUEST_ID, REQUEST_ID_VALUE)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andReturn();

        assertEquals(201, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        assertNotNull(content, "Content Should not be null");
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull(savedCaseResource, "Saved Case Details should not be null");

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.CREATE_CASE.getLabel()));
        assertThat(captor.getValue().getCaseId(), is(savedCaseResource.getReference()));
        assertThat(captor.getValue().getIdamId(), is(UID));
        assertThat(captor.getValue().getInvokingService(), is(MockUtils.CCD_GW));
        assertThat(captor.getValue().getHttpStatus(), is(201));
        assertThat(captor.getValue().getCaseType(), is(CASE_TYPE));
        assertThat(captor.getValue().getJurisdiction(), is(JURISDICTION));
        assertThat(captor.getValue().getEventSelected(), is(TEST_EVENT_ID));
        assertThat(captor.getValue().getRequestId(), is(REQUEST_ID_VALUE));
    }

    @Test
    public void shouldPopulateSearchCriteriaPostCreateCase() throws Exception {
        final String URL =  "/case-types/" + CASE_TYPE_WITH_SEARCH_PARTY + "/cases";
        final String description = "A very long comment.......";
        final String summary = "Short comment";

        final String testFieldValue = "2012-04-21";
        final String firstNameValue = "MyFirstName";
        final String lastNameValue = "MyLastName";
        final String addressLine1 = "My Street Address";
        final String postCode = "SW1H 9AJ";

        Map<String, JsonNode> caseData = new HashMap<>();
        caseData.put("TextField1", JacksonUtils.MAPPER.readTree("\"" + testFieldValue + "\""));
        caseData.put("PersonFirstName", JacksonUtils.MAPPER.readTree("\"" + firstNameValue + "\""));
        caseData.put("PersonLastName", JacksonUtils.MAPPER.readTree("\"" + lastNameValue + "\""));
        caseData.put("PersonAddress", JacksonUtils.MAPPER.readTree("{\n"
            + "      \"PostCode\": \"" + postCode + "\",\n"
            + "      \"AddressLine1\": \"" + addressLine1 + "\"\n"
            + "}"));

        final CaseDataContent caseDetailsToSave = newCaseDataContent().withData(caseData).build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(description);
        triggeringEvent.setSummary(summary);
        caseDetailsToSave.setEvent(triggeringEvent);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE_WITH_SEARCH_PARTY, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .header(REQUEST_ID, REQUEST_ID_VALUE)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andReturn();

        assertEquals(201, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        assertNotNull(content, "Content Should not be null");
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull(savedCaseResource, "Saved Case Details should not be null");

        JsonNode searchCriteriaJsonNode = savedCaseResource.getData().get("SearchCriteria");
        assertEquals(searchCriteriaJsonNode.get("OtherCaseReferences").findValue("value").asText(),
            testFieldValue,
            "Saved case data should contain SearchCriteria with OtherCaseReferences");

        SearchPartyValue searchPartyValue =  mapper.treeToValue(
            searchCriteriaJsonNode.get("SearchParties").findValue("value"),
            SearchPartyValue.class);
        assertAll("Saved case data should contain SearchCriteria with SearchParty populated",
            () -> assertEquals(firstNameValue + " " + lastNameValue, searchPartyValue.getName(), "name populated"),
            () -> assertEquals(testFieldValue, searchPartyValue.getDateOfBirth(), "dateOfBirth populated"),
            () -> assertEquals(addressLine1, searchPartyValue.getAddressLine1(), "Address Line 1 populated"),
            () -> assertEquals(postCode, searchPartyValue.getPostCode(), "PostCode populated")
        );

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.CREATE_CASE.getLabel()));
        assertThat(captor.getValue().getCaseId(), is(savedCaseResource.getReference()));
        assertThat(captor.getValue().getIdamId(), is(UID));
        assertThat(captor.getValue().getInvokingService(), is(MockUtils.CCD_GW));
        assertThat(captor.getValue().getHttpStatus(), is(201));
        assertThat(captor.getValue().getCaseType(), is(CASE_TYPE_WITH_SEARCH_PARTY));
        assertThat(captor.getValue().getJurisdiction(), is(JURISDICTION));
        assertThat(captor.getValue().getEventSelected(), is(TEST_EVENT_ID));
        assertThat(captor.getValue().getRequestId(), is(REQUEST_ID_VALUE));
    }

    @Test
    public void shouldPopulateMultipleSearchCriteriaAndSearchPartiesPostCreateCase() throws Exception {
        final String URL =  "/case-types/" + CASE_TYPE_WITH_MULTIPLE_SEARCH_CRITERIA_AND_SEARCH_PARTY + "/cases";
        final String description = "A very long comment.......";
        final String summary = "Short comment";

        final CaseDataContent caseDetailsToSave = newCaseDataContent()
            .withData(GlobalSearchTestFixture.createCaseData())
            .build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(description);
        triggeringEvent.setSummary(summary);
        caseDetailsToSave.setEvent(triggeringEvent);
        final String token = generateEventTokenNewCase(UID, JURISDICTION,
            CASE_TYPE_WITH_MULTIPLE_SEARCH_CRITERIA_AND_SEARCH_PARTY, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .header(REQUEST_ID, REQUEST_ID_VALUE)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andReturn();

        assertEquals(201, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        assertNotNull(content, "Content Should not be null");
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull(savedCaseResource, "Saved Case Details should not be null");

        GlobalSearchTestFixture.assertGlobalSearchData(savedCaseResource.getData());
        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.CREATE_CASE.getLabel()));
        assertThat(captor.getValue().getCaseId(), is(savedCaseResource.getReference()));
        assertThat(captor.getValue().getIdamId(), is(UID));
        assertThat(captor.getValue().getInvokingService(), is(MockUtils.CCD_GW));
        assertThat(captor.getValue().getHttpStatus(), is(201));
        assertThat(captor.getValue().getCaseType(), is(CASE_TYPE_WITH_MULTIPLE_SEARCH_CRITERIA_AND_SEARCH_PARTY));
        assertThat(captor.getValue().getJurisdiction(), is(JURISDICTION));
        assertThat(captor.getValue().getEventSelected(), is(TEST_EVENT_ID));
        assertThat(captor.getValue().getRequestId(), is(REQUEST_ID_VALUE));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases_global_search.sql"})
    public void shouldPopulateMultipleSearchCriteriaAndSearchPartiesPostCreateEvent() throws Exception {
        String caseId = "1504259907353529";
        final String URL =  "/cases/" + caseId + "/events";

        UserInfo userInfo = UserInfo.builder()
            .uid("TestUserId")
            .givenName("firstname")
            .familyName("familyname")
            .build();
        stubFor(WireMock.get(urlMatching("/o/userinfo"))
            .withHeader(HttpHeaders.AUTHORIZATION, containing("Test_Token"))
            .willReturn(okJson(mapper.writeValueAsString(userInfo)).withStatus(200)));

        final CaseDataContent caseDetailsToSave = newCaseDataContent()
            .withCaseReference(caseId)
            .withOnBehalfOfUserToken("Test_Token")
            .withEvent(anEvent()
                .withEventId("HAS_PRE_STATES_EVENT")
                .withSummary("Short comment")
                .build())
            .withToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, "HAS_PRE_STATES_EVENT"))
            .withData(GlobalSearchTestFixture.createCaseData())
            .build();

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .header(REQUEST_ID, REQUEST_ID_VALUE)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andReturn();

        assertEquals(201, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull(savedCaseResource, "Saved Case Details should not be null");

        GlobalSearchTestFixture.assertGlobalSearchData(savedCaseResource.getData());
    }

    @Test
    @DisplayName("Submit case creation event without any documents but "
        + "upload a document with 'document_hash' field via about_to_submit callback")
    public void shouldReturn201WhenPostCreateCaseAndAboutToSubmitCallbackWithDocument() throws Exception {
        final String callbackEventId = "TEST_SUBMIT_CALLBACK_EVENT";
        final String URL = "/case-types/" + CASE_TYPE + "/cases";
        final String description = "A very long comment.......";
        final String summary = "Short comment";
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, callbackEventId);

        final Event triggeringEvent = anEvent()
            .withEventId(callbackEventId)
            .withDescription(description)
            .withSummary(summary)
            .build();
        final CaseDataContent caseDetailsToSave = newCaseDataContent()
            .withEvent(triggeringEvent)
            .withToken(token)
            .build();

        final String jsonString = TestFixtures.fromFileAsString("__files/test-addressbook-case.json")
            .replace("${CALLBACK_URL}", hostUrl + "/callback/document");

        stubFor(WireMock.get(urlMatching("/api/data/case-type/" + CASE_TYPE))
            .willReturn(okJson(jsonString).withStatus(200)));

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .header(REQUEST_ID, REQUEST_ID_VALUE)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andReturn();

        assertEquals(201, mvcResult.getResponse().getStatus());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final String content = mvcResult.getResponse().getContentAsString();
        assertNotNull(content, "Content Should not be null");
        final CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull(savedCaseResource, "Saved Case Details should not be null");

        final ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertAll(() -> {
            assertThat(captor.getValue().getOperationType(), is(AuditOperationType.CREATE_CASE.getLabel()));
            assertThat(captor.getValue().getOperationType(), is(AuditOperationType.CREATE_CASE.getLabel()));
            assertThat(captor.getValue().getCaseId(), is(savedCaseResource.getReference()));
            assertThat(captor.getValue().getIdamId(), is(UID));
            assertThat(captor.getValue().getInvokingService(), is(MockUtils.CCD_GW));
            assertThat(captor.getValue().getHttpStatus(), is(201));
            assertThat(captor.getValue().getCaseType(), is(CASE_TYPE));
            assertThat(captor.getValue().getJurisdiction(), is(JURISDICTION));
            assertThat(captor.getValue().getEventSelected(), is(callbackEventId));
            assertThat(captor.getValue().getRequestId(), is(REQUEST_ID_VALUE));
        });
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseV3() throws Exception {
        final String url = "/case-types/" + CASE_TYPE + "/cases";
        final String description = "A very long comment.......";
        final String summary = "Short comment";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(description);
        triggeringEvent.setSummary(summary);
        caseDetailsToSave.setEvent(triggeringEvent);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);

        final Map<String, String> parameterMap = new HashMap<>(4);
        parameterMap.put("charset", "UTF-8");
        MediaType jsonContentV3CreateCase = new MediaType(
            "application",
            "vnd.uk.gov.hmcts.ccd-data-store-api.create-case.v3+json",
            parameterMap);

        final MvcResult mvcResult = mockMvc.perform(post(url)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .contentType(jsonContentV3CreateCase)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andReturn();

        assertEquals(201, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        assertNotNull(content, "Content Should not be null");
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull(savedCaseResource, "Saved Case Details should not be null");
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseWithCreatorRoleWithNoDataForCaseworker() throws Exception {
        final String description = "A very long comment.......";
        final String summary = "Short comment";

        final String URL = "/case-types/" + CASE_TYPE_CREATOR_ROLE + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(description);
        triggeringEvent.setSummary(summary);
        caseDetailsToSave.setEvent(triggeringEvent);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE_CREATOR_ROLE, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);


        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andReturn();

        assertEquals(201, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        assertNotNull(content, "Content Should not be null");
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull(savedCaseResource, "Saved Case Details should not be null");
    }

    @Test
    public void shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccessOnCreatorRole() throws Exception {
        final String URL = "/case-types/" + CASE_TYPE_CREATOR_ROLE_NO_CREATE_ACCESS + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();

        mockMvc.perform(post(URL)
                .header(EXPERIMENTAL_HEADER, "experimental")
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsString(caseDetailsToSave))
            ).andExpect(status().is(404))
            .andReturn();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldSetSupplementaryData() throws Exception {
        String caseId = "1504259907353529";
        final String URL = "/cases/" + caseId + "/supplementary-data";
        SupplementaryDataUpdateRequest supplementaryDataUpdateRequest = createSupplementaryDataSetRequestOrgB();

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(supplementaryDataUpdateRequest))
        ).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        SupplementaryDataResource supplementaryDataResource =
            mapper.readValue(content, SupplementaryDataResource.class);
        assertNotNull(supplementaryDataResource);
        assertNotNull(supplementaryDataResource.getResponse());
        Map<String, Object> response = supplementaryDataResource.getResponse();
        assertEquals(1, response.size());
        assertTrue(response.containsKey("orgs_assigned_users.organisationB"));
        assertEquals(23, response.get("orgs_assigned_users.organisationB"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldSetSupplementaryDataMultipleUpdate() throws Exception {
        String caseId = "1504259907353529";
        final String URL = "/cases/" + caseId + "/supplementary-data";
        SupplementaryDataUpdateRequest supplementaryDataUpdateRequest = createSupplementaryDataSetRequestMultiple();

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(supplementaryDataUpdateRequest))
        ).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        SupplementaryDataResource supplementaryDataResource =
            mapper.readValue(content, SupplementaryDataResource.class);
        assertNotNull(supplementaryDataResource);
        assertNotNull(supplementaryDataResource.getResponse());
        assertNotNull(supplementaryDataResource.getResponse());
        Map<String, Object> response = supplementaryDataResource.getResponse();
        assertEquals(2, response.size());
        assertTrue(response.containsKey("orgs_assigned_users.organisationB"));
        assertEquals(23, response.get("orgs_assigned_users.organisationB"));
        assertTrue(response.containsKey("orgs_assigned_users.organisationA"));
        assertEquals(25, response.get("orgs_assigned_users.organisationA"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldIncrementSupplementaryData() throws Exception {
        String caseId = "1504259907353529";
        final String URL = "/cases/" + caseId + "/supplementary-data";
        SupplementaryDataUpdateRequest supplementaryDataUpdateRequest = createSupplementaryDataIncrementRequest();

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(supplementaryDataUpdateRequest))
        ).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        SupplementaryDataResource supplementaryDataResource =
            mapper.readValue(content, SupplementaryDataResource.class);
        assertNotNull(supplementaryDataResource, "updated supplementary data resource");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldCreateSupplementaryDataWhenNotExists() throws Exception {
        String caseId = "1504259907353545";
        final String URL = "/cases/" + caseId + "/supplementary-data";
        SupplementaryDataUpdateRequest supplementaryDataUpdateRequest = createSupplementaryDataSetRequest();

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(supplementaryDataUpdateRequest))
        ).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        SupplementaryDataResource supplementaryDataResource =
            mapper.readValue(content, SupplementaryDataResource.class);
        assertNotNull(supplementaryDataResource);
        assertNotNull(supplementaryDataResource.getResponse());
        assertEquals(1, supplementaryDataResource.getResponse().size());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldThrowExceptionWhenCaseNotFound() throws Exception {
        String caseId = "1504259907353586";
        final String URL = "/cases/" + caseId + "/supplementary-data";
        SupplementaryDataUpdateRequest supplementaryDataUpdateRequest = createSupplementaryDataSetRequest();

        final String message = mockMvc.perform(post(URL)
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsString(supplementaryDataUpdateRequest))
            ).andExpect(status().is(404))
            .andReturn().getResolvedException().getMessage();

        assertTrue(StringUtils.contains(message, CASE_NOT_FOUND));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldThrowExceptionWhenCaseIsNotValid() throws Exception {
        String caseId = "12233";
        final String URL = "/cases/" + caseId + "/supplementary-data";
        SupplementaryDataUpdateRequest supplementaryDataUpdateRequest = createSupplementaryDataSetRequest();

        final String message = mockMvc.perform(post(URL)
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsString(supplementaryDataUpdateRequest))
            ).andExpect(status().is(400))
            .andReturn().getResolvedException().getMessage();

        assertTrue(StringUtils.contains(message, CASE_ID_INVALID));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void testShouldGetCategoriesAndDocuments() throws Exception {
        final String caseId = "1504259907353529";
        final String URL = "/categoriesAndDocuments/" + caseId;
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final MvcResult mvcResult = mockMvc.perform(get(URL)
            .header(REQUEST_ID, REQUEST_ID_VALUE)
            .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        Assertions.assertThat(mvcResult.getResponse())
            .isNotNull()
            .satisfies(response -> Assertions.assertThat(response.getStatus()).isEqualTo(204));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void testGetCategoriesAndDocumentsShouldReturn404WhenCaseDoesNotExist() throws Exception {
        final String caseId = "4259907353529155";
        final String URL = "/categoriesAndDocuments/" + caseId;
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final MvcResult mvcResult = mockMvc.perform(get(URL)
            .header(REQUEST_ID, REQUEST_ID_VALUE)
            .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        Assertions.assertThat(mvcResult.getResponse())
            .isNotNull()
            .satisfies(response -> Assertions.assertThat(response.getStatus()).isEqualTo(404));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void testGetCategoriesAndDocumentsShouldReturn404WhenUserIsNotAllowedAccessToCase() throws Exception {
        final String caseId = "1504259907353529";
        final String URL = "/categoriesAndDocuments/" + caseId;
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CITIZEN);

        final MvcResult mvcResult = mockMvc.perform(get(URL)
            .header(REQUEST_ID, REQUEST_ID_VALUE)
            .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        Assertions.assertThat(mvcResult.getResponse())
            .isNotNull()
            .satisfies(response -> Assertions.assertThat(response.getStatus()).isEqualTo(404));
    }

    @Test
    void testGetCategoriesAndDocumentsShouldReturnBadRequestWhenCaseRefHasWrongFormat() throws Exception {
        final String badCaseId = "1504259907353529000";
        final String URL = "/categoriesAndDocuments/" + badCaseId;
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final MvcResult mvcResult = mockMvc.perform(get(URL)
            .header(REQUEST_ID, REQUEST_ID_VALUE)
            .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        Assertions.assertThat(mvcResult.getResponse())
            .isNotNull()
            .satisfies(response -> Assertions.assertThat(response.getStatus()).isEqualTo(400));
    }

    @Nested
    @DisplayName("GET /cases/{caseId}/supplementary-data")
    class UserRoleValidation {

        @BeforeEach
        void setup() {
            String userJson = "{\n"
                + "          \"sub\": \"Cloud.Strife@test.com\",\n"
                + "          \"uid\": \"1234\",\n"
                + "          \"roles\": [\n"
                + "            \"caseworker\",\n"
                + "            \"caseworker-test\"\n"
                + "          ],\n"
                + "          \"name\": \"Cloud Strife\"\n"
                + "        }";
            stubFor(WireMock.get(urlMatching("/o/userinfo"))
                .willReturn(okJson(userJson).withStatus(200)));
        }

        @Test
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
        void shouldThrowExceptionWhenSolicitorRoleWithNoCasesAssignedToTheUser() throws Exception {

            MockUtils.setSecurityAuthorities(authentication, ROLE_PROBATE_SOLICITOR);
            String caseId = "1504259907353545";
            final String URL = "/cases/" + caseId + "/supplementary-data";
            SupplementaryDataUpdateRequest supplementaryDataUpdateRequest = createSupplementaryDataSetRequest();

            final String message = mockMvc.perform(post(URL)
                    .contentType(JSON_CONTENT_TYPE)
                    .content(mapper.writeValueAsString(supplementaryDataUpdateRequest))
                ).andExpect(status().is(403))
                .andReturn().getResolvedException().getMessage();

            assertTrue(StringUtils.contains(message, NOT_AUTHORISED_UPDATE_SUPPLEMENTARY_DATA));
        }
    }

    private SupplementaryDataUpdateRequest createSupplementaryDataSetRequestMultiple() throws JsonProcessingException {
        String jsonRequest = "{\n"
            + "\t\"$set\": {\n"
            + "\t\t\"orgs_assigned_users.organisationA\": 25,\n"
            + "\t\t\"orgs_assigned_users.organisationB\": 23\n"
            + "\t}\n"
            + "}";

        Map<String, Map<String, Object>> requestData = mapper.readValue(jsonRequest, Map.class);
        return new SupplementaryDataUpdateRequest(requestData);
    }

    private SupplementaryDataUpdateRequest createSupplementaryDataSetRequestOrgB() throws JsonProcessingException {
        String jsonRequest = "{\n"
            + "\t\"$set\": {\n"
            + "\t\t\"orgs_assigned_users.organisationB\": 23\n"
            + "\t}\n"
            + "}";

        Map<String, Map<String, Object>> requestData = mapper.readValue(jsonRequest, Map.class);
        return new SupplementaryDataUpdateRequest(requestData);
    }

    private SupplementaryDataUpdateRequest createSupplementaryDataSetRequest() throws JsonProcessingException {
        String jsonRequest = "{\n"
            + "\t\"$set\": {\n"
            + "\t\t\"orgs_assigned_users.organisationA\": 22\n"
            + "\t}\n"
            + "}";

        Map<String, Map<String, Object>> requestData = mapper.readValue(jsonRequest, Map.class);
        return new SupplementaryDataUpdateRequest(requestData);
    }

    private SupplementaryDataUpdateRequest createSupplementaryDataIncrementRequest() throws JsonProcessingException {
        String jsonRequest = "{\n"
            + "\t\"$inc\": {\n"
            + "\t\t\"orgs_assigned_users.organisationA\": 3\n"
            + "\t}\n"
            + "}";

        Map<String, Map<String, Object>> requestData = mapper.readValue(jsonRequest, Map.class);
        return new SupplementaryDataUpdateRequest(requestData);
    }

}
