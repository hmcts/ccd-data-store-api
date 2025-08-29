package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
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
import uk.gov.hmcts.ccd.customheaders.CustomHeadersFilter;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLinkDetails;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLinkInfo;
import uk.gov.hmcts.ccd.domain.model.caselinking.GetLinkedCasesResponse;
import uk.gov.hmcts.ccd.domain.model.caselinking.Reason;
import uk.gov.hmcts.ccd.domain.model.globalsearch.SearchPartyValue;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.util.ClientContextUtil;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.CaseResource;
import uk.gov.hmcts.ccd.v2.external.resource.SupplementaryDataResource;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;
import static uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentUtils.UPLOAD_TIMESTAMP;
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
    private static final String ROLE_PROBATE_SOLICITOR = "caseworker-probate-solicitor";
    private static String CUSTOM_CONTEXT = "";

    @Inject
    private WebApplicationContext wac;
    @Inject
    private CustomHeadersFilter customHeadersFilter;

    private MockMvc mockMvc;

    @SpyBean
    private AuditRepository auditRepository;

    @BeforeEach
    public void setUp() {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).addFilters(customHeadersFilter).build();
        CUSTOM_CONTEXT = applicationParams.getCallbackPassthruHeaderContexts().get(0);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void shouldLogAuditInfoForGetCaseById() throws Exception {
        String caseId = "1504259907353529";
        final String URL = "/cases/" + caseId;

        final MvcResult mvcResult = mockMvc.perform(get(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .header(REQUEST_ID, REQUEST_ID_VALUE)
            .contentType(JSON_CONTENT_TYPE)
        ).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
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
    void shouldLogAuditInfoForGetCaseByIdWithNullRequestId() throws Exception {
        String caseId = "1504259907353529";
        final String URL = "/cases/" + caseId;

        final MvcResult mvcResult = mockMvc.perform(get(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .contentType(JSON_CONTENT_TYPE)
        ).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
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
    void shouldLogAuditInfoForCreateEventByCaseId() throws Exception {
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
    void shouldSaveOnBehalfOfUserAndProxiedByUser() throws Exception {
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
        String content = mvcResult.getResponse().getContentAsString();
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull(savedCaseResource, "Saved Case Details should not be null");
    }

    @Test
    void shouldReturn201WhenPostCreateCase() throws Exception {
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
            .header(CUSTOM_CONTEXT, responseJson1.toString())
            .header(REQUEST_ID, REQUEST_ID_VALUE)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andReturn();

        assertEquals(201, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        String content = mvcResult.getResponse().getContentAsString();
        assertNotNull(content, "Content Should not be null");
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull(savedCaseResource, "Saved Case Details should not be null");

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertTrue(mvcResult.getResponse().getHeaderNames().contains(CUSTOM_CONTEXT));
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
    void shouldReturnCustomHeaderWithAlteredValueFromCallback() throws Exception {
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

        stubFor(WireMock.post(urlMatching("/callback/document"))
            .willReturn(okJson(jsonString).withStatus(200).withHeader(CUSTOM_CONTEXT, responseJson2.toString())));

        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, "Bearer user1");
        headers.add(REQUEST_ID, REQUEST_ID_VALUE);
        headers.add(V2.EXPERIMENTAL_HEADER, "true");
        headers.add(CUSTOM_CONTEXT, responseJson1.toString());

        final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(URL).headers(headers)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andReturn();

        assertEquals(201, mvcResult.getResponse().getStatus());
        assertTrue(mvcResult.getResponse().getHeaderNames().contains(CUSTOM_CONTEXT));
        assertTrue(mvcResult.getResponse().getHeader(CUSTOM_CONTEXT).contains(
            ClientContextUtil.encodeToBase64(responseJson2.toString())));
    }

    @Test
    void shouldPopulateSearchCriteriaPostCreateCase() throws Exception {
        final String URL = "/case-types/" + CASE_TYPE_WITH_SEARCH_PARTY + "/cases";
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
            .header(CUSTOM_CONTEXT, responseJson1.toString())
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andReturn();

        assertTrue(mvcResult.getResponse().getHeaderNames().contains(CUSTOM_CONTEXT));
        assertEquals(201, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        String content = mvcResult.getResponse().getContentAsString();
        assertNotNull(content, "Content Should not be null");
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull(savedCaseResource, "Saved Case Details should not be null");

        JsonNode searchCriteriaJsonNode = savedCaseResource.getData().get("SearchCriteria");
        assertEquals(searchCriteriaJsonNode.get("OtherCaseReferences").findValue("value").asText(),
            testFieldValue,
            "Saved case data should contain SearchCriteria with OtherCaseReferences");

        SearchPartyValue searchPartyValue = mapper.treeToValue(
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
    void shouldPopulateMultipleSearchCriteriaAndSearchPartiesPostCreateCase() throws Exception {
        final String URL = "/case-types/" + CASE_TYPE_WITH_MULTIPLE_SEARCH_CRITERIA_AND_SEARCH_PARTY + "/cases";
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
            .header(CUSTOM_CONTEXT, responseJson1.toString())
            .header(REQUEST_ID, REQUEST_ID_VALUE)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andReturn();

        assertTrue(mvcResult.getResponse().getHeaderNames().contains(CUSTOM_CONTEXT));
        assertEquals(201, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
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
    void shouldPopulateMultipleSearchCriteriaAndSearchPartiesPostCreateEvent() throws Exception {
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
            .withData(GlobalSearchTestFixture.createCaseData())
            .build();

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .header(REQUEST_ID, REQUEST_ID_VALUE)
            .header(CUSTOM_CONTEXT, responseJson1.toString())
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andReturn();

        assertTrue(mvcResult.getResponse().getHeaderNames().contains(CUSTOM_CONTEXT));
        assertEquals(201, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        String content = mvcResult.getResponse().getContentAsString();
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull(savedCaseResource, "Saved Case Details should not be null");

        GlobalSearchTestFixture.assertGlobalSearchData(savedCaseResource.getData());
    }

    @Test
    @DisplayName("Submit case creation event without any documents but "
        + "upload a document with 'document_hash' field via about_to_submit callback")
    void shouldReturn201WhenPostCreateCaseAndAboutToSubmitCallbackWithDocument() throws Exception {
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
            .header(CUSTOM_CONTEXT, responseJson1.toString())
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andReturn();

        assertTrue(mvcResult.getResponse().getHeaderNames().contains(CUSTOM_CONTEXT));
        assertEquals(201, mvcResult.getResponse().getStatus());
        final String content = mvcResult.getResponse().getContentAsString();
        assertNotNull(content, "Content Should not be null");
        final CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull(savedCaseResource, "Saved Case Details should not be null");

        final ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertAll(() -> {
            assertThat(captor.getValue().getOperationType(), is(AuditOperationType.CREATE_CASE.getLabel()));
            assertThat(captor.getValue().getCaseId(), is(savedCaseResource.getReference()));
            assertTrue(savedCaseResource.getData().values().stream().findFirst().get().has(UPLOAD_TIMESTAMP));
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
    void shouldReturn201WhenPostCreateCaseV3() throws Exception {
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
            .header(CUSTOM_CONTEXT, responseJson1.toString())
            .contentType(jsonContentV3CreateCase)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andReturn();

        assertTrue(mvcResult.getResponse().getHeaderNames().contains(CUSTOM_CONTEXT));
        assertEquals(201, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        String content = mvcResult.getResponse().getContentAsString();

        assertNotNull(content, "Content Should not be null");
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull(savedCaseResource, "Saved Case Details should not be null");
    }

    @Test
    void shouldReturn201WhenPostCreateCaseWithCreatorRoleWithNoDataForCaseworker() throws Exception {
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
            .header(CUSTOM_CONTEXT, responseJson1.toString())
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andReturn();

        assertTrue(mvcResult.getResponse().getHeaderNames().contains(CUSTOM_CONTEXT));
        assertEquals(201, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        String content = mvcResult.getResponse().getContentAsString();
        assertNotNull(content, "Content Should not be null");
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull(savedCaseResource, "Saved Case Details should not be null");
    }

    @Test
    void shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccessOnCreatorRole() throws Exception {
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
    void shouldSetSupplementaryData() throws Exception {
        String caseId = "1504259907353529";
        final String URL = "/cases/" + caseId + "/supplementary-data";
        SupplementaryDataUpdateRequest supplementaryDataUpdateRequest = createSupplementaryDataSetRequestOrgB();

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(supplementaryDataUpdateRequest))
        ).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
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
    void shouldSetSupplementaryDataMultipleUpdate() throws Exception {
        String caseId = "1504259907353529";
        final String URL = "/cases/" + caseId + "/supplementary-data";
        SupplementaryDataUpdateRequest supplementaryDataUpdateRequest = createSupplementaryDataSetRequestMultiple();

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(supplementaryDataUpdateRequest))
        ).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
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
    void shouldIncrementSupplementaryData() throws Exception {
        String caseId = "1504259907353529";
        final String URL = "/cases/" + caseId + "/supplementary-data";
        SupplementaryDataUpdateRequest supplementaryDataUpdateRequest = createSupplementaryDataIncrementRequest();

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(supplementaryDataUpdateRequest))
        ).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        String content = mvcResult.getResponse().getContentAsString();
        SupplementaryDataResource supplementaryDataResource =
            mapper.readValue(content, SupplementaryDataResource.class);
        assertNotNull(supplementaryDataResource, "updated supplementary data resource");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    void shouldCreateSupplementaryDataWhenNotExists() throws Exception {
        String caseId = "1504259907353545";
        final String URL = "/cases/" + caseId + "/supplementary-data";
        SupplementaryDataUpdateRequest supplementaryDataUpdateRequest = createSupplementaryDataSetRequest();

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(supplementaryDataUpdateRequest))
        ).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
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
    void shouldThrowExceptionWhenCaseNotFound() throws Exception {
        String caseId = "1504259907353586";
        final String URL = "/cases/" + caseId + "/supplementary-data";
        SupplementaryDataUpdateRequest supplementaryDataUpdateRequest = createSupplementaryDataSetRequest();

        final Exception exception = mockMvc.perform(post(URL)
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsString(supplementaryDataUpdateRequest))
            ).andExpect(status().is(404))
            .andReturn().getResolvedException();

        assertNotNull(exception);
        assertTrue(StringUtils.contains(exception.getMessage(), CASE_NOT_FOUND));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    void shouldThrowExceptionWhenCaseIsNotValid() throws Exception {
        String caseId = "12233";
        final String URL = "/cases/" + caseId + "/supplementary-data";
        SupplementaryDataUpdateRequest supplementaryDataUpdateRequest = createSupplementaryDataSetRequest();

        final Exception exception = mockMvc.perform(post(URL)
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsString(supplementaryDataUpdateRequest))
            ).andExpect(status().is(400))
            .andReturn().getResolvedException();

        assertNotNull(exception);
        assertTrue(StringUtils.contains(exception.getMessage(), CASE_ID_INVALID));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void shouldSaveOnBehalfOfUserAndProxiedByUserId() throws Exception {
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

        String onBehalfOfId = "53b53d81-3026-4e29-863a-ba1c7787f014";

        final CaseDataContent caseDetailsToSave = newCaseDataContent()
            .withCaseReference(caseId)
            .withOnBehalfOfId(onBehalfOfId)
            .withEvent(anEvent()
                .withEventId("HAS_PRE_STATES_EVENT")
                .withSummary("Short comment")
                .build())
            .withToken(generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE, "HAS_PRE_STATES_EVENT"))
            .build();

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .header(CUSTOM_CONTEXT, responseJson1.toString())
            .header(REQUEST_ID, REQUEST_ID_VALUE)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(caseDetailsToSave))
        ).andReturn();

        assertTrue(mvcResult.getResponse().getHeaderNames().contains(CUSTOM_CONTEXT));
        assertEquals(201, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        String content = mvcResult.getResponse().getContentAsString();
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull(savedCaseResource, "Saved Case Details should not be null");
    }

    @Nested
    @DisplayName("GET /cases/{caseId}/supplementary-data")
    class UserRoleValidation {

        @BeforeEach
        void setup() {
            String userJson = """
                {
                    "sub": "Cloud.Strife@test.com",
                    "uid": "1234",
                    "roles": [
                        "caseworker",
                        "caseworker-test"
                      ],
                      "name": "Cloud Strife"
                }
                """;
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

            final Exception exception = mockMvc.perform(post(URL)
                    .contentType(JSON_CONTENT_TYPE)
                    .content(mapper.writeValueAsString(supplementaryDataUpdateRequest))
                ).andExpect(status().is(403))
                .andReturn().getResolvedException();

            assertNotNull(exception);
            assertTrue(StringUtils.contains(exception.getMessage(), NOT_AUTHORISED_UPDATE_SUPPLEMENTARY_DATA));
        }
    }

    private Map<String, Map<String, Object>> readValueToMap(String jsonRequest) throws JsonProcessingException {
        return mapper.readValue(jsonRequest, new TypeReference<HashMap<String, Map<String, Object>>>() {
        });
    }

    private SupplementaryDataUpdateRequest createSupplementaryDataSetRequestMultiple() throws JsonProcessingException {
        String jsonRequest = "{\n"
            + "\t\"$set\": {\n"
            + "\t\t\"orgs_assigned_users.organisationA\": 25,\n"
            + "\t\t\"orgs_assigned_users.organisationB\": 23\n"
            + "\t}\n"
            + "}";

        Map<String, Map<String, Object>> requestData = readValueToMap(jsonRequest);
        return new SupplementaryDataUpdateRequest(requestData);
    }

    private SupplementaryDataUpdateRequest createSupplementaryDataSetRequestOrgB() throws JsonProcessingException {
        String jsonRequest = "{\n"
            + "\t\"$set\": {\n"
            + "\t\t\"orgs_assigned_users.organisationB\": 23\n"
            + "\t}\n"
            + "}";

        Map<String, Map<String, Object>> requestData = readValueToMap(jsonRequest);
        return new SupplementaryDataUpdateRequest(requestData);
    }

    private SupplementaryDataUpdateRequest createSupplementaryDataSetRequest() throws JsonProcessingException {
        String jsonRequest = "{\n"
            + "\t\"$set\": {\n"
            + "\t\t\"orgs_assigned_users.organisationA\": 22\n"
            + "\t}\n"
            + "}";

        Map<String, Map<String, Object>> requestData = readValueToMap(jsonRequest);
        return new SupplementaryDataUpdateRequest(requestData);
    }

    private SupplementaryDataUpdateRequest createSupplementaryDataIncrementRequest() throws JsonProcessingException {
        String jsonRequest = "{\n"
            + "\t\"$inc\": {\n"
            + "\t\t\"orgs_assigned_users.organisationA\": 3\n"
            + "\t}\n"
            + "}";

        Map<String, Map<String, Object>> requestData = readValueToMap(jsonRequest);
        return new SupplementaryDataUpdateRequest(requestData);
    }

    @Nested
    @DisplayName("GET /getLinkedCases/{caseReference}")
    class GetLinkedCases {

        // data values as per: classpath:sql/insert_cases_get_case_links.sql

        // scenario 1: some linked cases
        static final String SCENARIO_01_CASE_REFERENCE = "1504259907353545";
        static final String SCENARIO_01_LINKED_CASE_01_REFERENCE = "1504259907353537";
        static final String SCENARIO_01_LINKED_CASE_02_REFERENCE = "1504259907353552";
        static final String SCENARIO_01_LINKED_CASE_03_REFERENCE_NON_STANDARD = "9233017909132197";

        // scenario 2: many linked cases
        static final String SCENARIO_02_CASE_REFERENCE = "3522116262568758";
        static final String SCENARIO_02_LINKED_CASE_01_REFERENCE = "4504127458172644";
        static final String SCENARIO_02_LINKED_CASE_02_REFERENCE = "6913605797587333";
        static final String SCENARIO_02_LINKED_CASE_03_REFERENCE = "2609130232931622";
        static final String SCENARIO_02_LINKED_CASE_04_REFERENCE = "8256979053075411";
        static final String SCENARIO_02_LINKED_CASE_05_REFERENCE_HIDDEN = "1651653562092458";
        static final String SCENARIO_02_LINKED_CASE_06_REFERENCE = "8855462425591410";

        // scenario 3: no linked cases
        static final String SCENARIO_03_CASE_REFERENCE = "8990926843606105";

        @Test
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_get_case_links.sql"})
        void testShouldGetLinkedCases() throws Exception {
            final String URL = "/getLinkedCases/" + SCENARIO_01_CASE_REFERENCE + "?startRecordNumber=1";

            final MvcResult mvcResult = mockMvc.perform(get(URL)
                    .header(REQUEST_ID, REQUEST_ID_VALUE)
                    .contentType(JSON_CONTENT_TYPE))
                .andReturn();

            assertEquals(200, mvcResult.getResponse().getStatus());

            String content = mvcResult.getResponse().getContentAsString();
            assertNotNull(content, "Content Should not be null");

            GetLinkedCasesResponse getLinkedCasesResponse = mapper.readValue(content, GetLinkedCasesResponse.class);
            assertEquals(2, getLinkedCasesResponse.getLinkedCases().size());

            final Map<String, CaseLinkInfo> linkedCaseMap = getLinkedCasesAsMap(getLinkedCasesResponse);
            assertTrue(linkedCaseMap.keySet().containsAll(List.of(
                SCENARIO_01_LINKED_CASE_01_REFERENCE, SCENARIO_01_LINKED_CASE_02_REFERENCE)));

            // confirm non-standard links are excluded
            assertFalse(linkedCaseMap.containsKey(SCENARIO_01_LINKED_CASE_03_REFERENCE_NON_STANDARD));

            assertCaseLinkInfo(
                SCENARIO_01_LINKED_CASE_01_REFERENCE,
                "Case Name: Scenario 1 linked case 1",
                List.of(
                    Reason.builder()
                        .reasonCode("Reason 1.1")
                        .otherDescription("OtherDescription 1.1")
                        .build()
                ),
                null,
                linkedCaseMap.get(SCENARIO_01_LINKED_CASE_01_REFERENCE)
            );

            assertCaseLinkInfo(
                SCENARIO_01_LINKED_CASE_02_REFERENCE,
                "Case Name: Scenario 1 linked case 2",
                List.of(
                    Reason.builder()
                        .reasonCode("Reason 1.2.1")
                        .otherDescription("OtherDescription 1.2.1")
                        .build(),
                    Reason.builder()
                        .reasonCode("Reason 1.2.2")
                        .otherDescription("OtherDescription 1.2.2")
                        .build()
                ),
                List.of(
                    Reason.builder()
                        .reasonCode("Reason 1.2.3")
                        .otherDescription("OtherDescription 1.2.3")
                        .build()
                ),
                linkedCaseMap.get(SCENARIO_01_LINKED_CASE_02_REFERENCE)
            );

            assertFalse(getLinkedCasesResponse.isHasMoreRecords());
        }

        @Test
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_get_case_links.sql"})
        void testShouldGetLinkedCasesOptionalParameters() throws Exception {
            final String URL = "/getLinkedCases/" + SCENARIO_01_CASE_REFERENCE
                + "?startRecordNumber=2&maxReturnRecordCount=1";

            final MvcResult mvcResult = mockMvc.perform(get(URL)
                    .header(REQUEST_ID, REQUEST_ID_VALUE)
                    .contentType(JSON_CONTENT_TYPE))
                .andReturn();

            String content = mvcResult.getResponse().getContentAsString();
            assertNotNull(content, "Content Should not be null");

            GetLinkedCasesResponse getLinkedCasesResponse = mapper.readValue(content, GetLinkedCasesResponse.class);
            assertEquals(1, getLinkedCasesResponse.getLinkedCases().size());

            final Map<String, CaseLinkInfo> linkedCaseMap = getLinkedCasesAsMap(getLinkedCasesResponse);
            assertTrue(linkedCaseMap.containsKey(SCENARIO_01_LINKED_CASE_02_REFERENCE));

            assertFalse(getLinkedCasesResponse.isHasMoreRecords());
        }

        @Test
        void testGetLinkedCasesInvalidCaseReferenceShouldReturn400() throws Exception {
            final String caseReference = "abc";
            final String URL = "/getLinkedCases/" + caseReference;

            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            final MvcResult mvcResult = mockMvc.perform(get(URL)
                    .header(REQUEST_ID, REQUEST_ID_VALUE)
                    .contentType(JSON_CONTENT_TYPE))
                .andReturn();

            assertEquals(400, mvcResult.getResponse().getStatus());
        }

        @Test
        void testShouldGetLinkedCasesReturn404WhenCaseDoesNotExist() throws Exception {
            final String caseReference = "4444333322221111";
            final String URL = "/getLinkedCases/" + caseReference;

            final MvcResult mvcResult = mockMvc.perform(get(URL)
                    .header(REQUEST_ID, REQUEST_ID_VALUE)
                    .contentType(JSON_CONTENT_TYPE))
                .andReturn();

            assertEquals(404, mvcResult.getResponse().getStatus());
        }

        @Test
        void testShouldGetLinkedCasesStartRecordNumberNotNumericReturn400() throws Exception {
            final String URL = "/getLinkedCases/" + SCENARIO_01_CASE_REFERENCE + "?startRecordNumber=A";

            final MvcResult mvcResult = mockMvc.perform(get(URL)
                    .header(REQUEST_ID, REQUEST_ID_VALUE)
                    .contentType(JSON_CONTENT_TYPE))
                .andReturn();

            assertEquals(400, mvcResult.getResponse().getStatus());
        }

        @Test
        void testShouldGetLinkedCasesMaxRecordCountNotNumericReturn400() throws Exception {
            final String URL = "/getLinkedCases/" + SCENARIO_01_CASE_REFERENCE + "?maxReturnRecordCount=A";

            final MvcResult mvcResult = mockMvc.perform(get(URL)
                    .header(REQUEST_ID, REQUEST_ID_VALUE)
                    .contentType(JSON_CONTENT_TYPE))
                .andReturn();

            assertEquals(400, mvcResult.getResponse().getStatus());
        }

        @Test
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_get_case_links.sql"})
        void shouldLogAuditInfoForGetLinkedCases() throws Exception {
            final String URL = "/getLinkedCases/" + SCENARIO_01_CASE_REFERENCE + "?startRecordNumber=1";

            final MvcResult mvcResult = mockMvc.perform(get(URL)
                    .header(REQUEST_ID, REQUEST_ID_VALUE)
                    .contentType(JSON_CONTENT_TYPE))
                .andReturn();

            assertEquals(200, mvcResult.getResponse().getStatus());
            String content = mvcResult.getResponse().getContentAsString();

            assertNotNull(content, "Content Should not be null");

            ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
            verify(auditRepository).save(captor.capture());

            List<String> caseReferences = Arrays.asList(captor.getValue().getCaseId().split(","));

            assertThat(captor.getValue().getOperationType(), is(AuditOperationType.LINKED_CASES_ACCESSED.getLabel()));
            assertThat(caseReferences.size(), is(3));
            assertThat(caseReferences, containsInAnyOrder(SCENARIO_01_CASE_REFERENCE,
                SCENARIO_01_LINKED_CASE_01_REFERENCE, SCENARIO_01_LINKED_CASE_02_REFERENCE));
            assertThat(captor.getValue().getIdamId(), is(UID));
            assertThat(captor.getValue().getInvokingService(), is(MockUtils.CCD_GW));
            assertThat(captor.getValue().getHttpStatus(), is(200));
            assertThat(captor.getValue().getRequestId(), is(REQUEST_ID_VALUE));
        }

        @Test
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_get_case_links.sql"})
        void getLinkedCasesShouldReturnEmptyResponsePayloadWhenNoLinkedCasesExist() throws Exception {
            final String URL = "/getLinkedCases/" + SCENARIO_03_CASE_REFERENCE + "?startRecordNumber=1";

            final MvcResult mvcResult = mockMvc.perform(get(URL)
                    .header(REQUEST_ID, REQUEST_ID_VALUE)
                    .contentType(JSON_CONTENT_TYPE))
                .andReturn();

            assertEquals(200, mvcResult.getResponse().getStatus());

            final GetLinkedCasesResponse getLinkedCasesResponse =
                mapper.readValue(mvcResult.getResponse().getContentAsString(), GetLinkedCasesResponse.class);

            assertNotNull(getLinkedCasesResponse, "Content should not be null");

            assertTrue(getLinkedCasesResponse.getLinkedCases().isEmpty());

            assertFalse(getLinkedCasesResponse.isHasMoreRecords());
        }

        @Test
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_get_case_links.sql"})
        void getLinkedCasesPaginationPage1() throws Exception {
            final String URL = "/getLinkedCases/" + SCENARIO_02_CASE_REFERENCE
                + "?startRecordNumber=1&maxReturnRecordCount=4";

            final MvcResult mvcResult = mockMvc.perform(get(URL)
                    .header(REQUEST_ID, REQUEST_ID_VALUE)
                    .contentType(JSON_CONTENT_TYPE))
                .andReturn();

            assertEquals(200, mvcResult.getResponse().getStatus());

            String content = mvcResult.getResponse().getContentAsString();
            assertNotNull(content, "Content should not be null");

            GetLinkedCasesResponse getLinkedCasesResponse = mapper.readValue(content, GetLinkedCasesResponse.class);

            assertEquals(4, getLinkedCasesResponse.getLinkedCases().size());

            final Map<String, CaseLinkInfo> linkedCaseMap = getLinkedCasesAsMap(getLinkedCasesResponse);
            assertTrue(linkedCaseMap.keySet().containsAll(List.of(
                SCENARIO_02_LINKED_CASE_01_REFERENCE,
                SCENARIO_02_LINKED_CASE_02_REFERENCE,
                SCENARIO_02_LINKED_CASE_03_REFERENCE,
                SCENARIO_02_LINKED_CASE_04_REFERENCE)));

            assertTrue(getLinkedCasesResponse.isHasMoreRecords());
        }

        @Test
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_get_case_links.sql"})
        void getLinkedCasesPaginationPage2() throws Exception {
            final String URL = "/getLinkedCases/" + SCENARIO_02_CASE_REFERENCE
                + "?startRecordNumber=5&maxReturnRecordCount=4";

            final MvcResult mvcResult = mockMvc.perform(get(URL)
                    .header(REQUEST_ID, REQUEST_ID_VALUE)
                    .contentType(JSON_CONTENT_TYPE))
                .andReturn();

            assertEquals(200, mvcResult.getResponse().getStatus());

            String content = mvcResult.getResponse().getContentAsString();
            assertNotNull(content, "Content Should not be null");

            GetLinkedCasesResponse getLinkedCasesResponse = mapper.readValue(content, GetLinkedCasesResponse.class);

            assertEquals(1, getLinkedCasesResponse.getLinkedCases().size());

            final Map<String, CaseLinkInfo> linkedCaseMap = getLinkedCasesAsMap(getLinkedCasesResponse);
            assertTrue(linkedCaseMap.containsKey(SCENARIO_02_LINKED_CASE_06_REFERENCE));

            // confirm case with standard caseLink field that is hidden from user is excluded from results
            assertFalse(linkedCaseMap.containsKey(SCENARIO_02_LINKED_CASE_05_REFERENCE_HIDDEN));

            assertFalse(getLinkedCasesResponse.isHasMoreRecords());
        }

        private Map<String, CaseLinkInfo> getLinkedCasesAsMap(GetLinkedCasesResponse getLinkedCasesResponse) {
            return getLinkedCasesResponse.getLinkedCases().stream()
                .collect(Collectors.toMap(
                    CaseLinkInfo::getCaseReference,
                    caseLinkInfo -> caseLinkInfo));
        }

        private void assertCaseLinkInfo(String expectedCaseReference,
                                        String expectedCaseNameHmctsInternal,
                                        List<Reason> expectedReasonsFromDetails1,
                                        List<Reason> expectedReasonsFromDetails2,
                                        CaseLinkInfo actualCaseLinkInfo) {
            assertNotNull(actualCaseLinkInfo);

            assertEquals(expectedCaseReference, actualCaseLinkInfo.getCaseReference());
            assertEquals(expectedCaseNameHmctsInternal, actualCaseLinkInfo.getCaseNameHmctsInternal());

            assertNotNull(actualCaseLinkInfo.getState());
            assertNotNull(actualCaseLinkInfo.getCcdCaseType());
            assertNotNull(actualCaseLinkInfo.getCcdJurisdiction());
            assertNotNull(actualCaseLinkInfo.getLinkDetails());

            final CaseLinkDetails caseLinkDetails1 = actualCaseLinkInfo.getLinkDetails().get(0);
            assertNotNull(caseLinkDetails1.getCreatedDateTime());
            assertReasonList(expectedReasonsFromDetails1, caseLinkDetails1.getReasons());

            if (expectedReasonsFromDetails2 == null) {
                assertEquals(1, actualCaseLinkInfo.getLinkDetails().size());
            } else {
                assertEquals(2, actualCaseLinkInfo.getLinkDetails().size());

                final CaseLinkDetails caseLinkDetails2 = actualCaseLinkInfo.getLinkDetails().get(1);
                assertNotNull(caseLinkDetails2.getCreatedDateTime());
                assertReasonList(expectedReasonsFromDetails2, caseLinkDetails2.getReasons());
            }
        }

        private void assertReasonList(List<Reason> expectedReasons,
                                      List<Reason> actualReasons) {
            assertNotNull(actualReasons);
            assertEquals(expectedReasons.size(), actualReasons.size());
        }

    }

}
