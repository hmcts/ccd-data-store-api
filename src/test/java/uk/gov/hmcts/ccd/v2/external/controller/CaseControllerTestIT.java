package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.Map;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;
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
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.v2.external.resource.CaseResource;
import uk.gov.hmcts.ccd.v2.external.resource.SupplementaryDataResource;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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

public class CaseControllerTestIT extends WireMockBaseTest {

    private static final String CASE_TYPE = "TestAddressBookCase";
    private static final String JURISDICTION = "PROBATE";
    private static final String TEST_EVENT_ID = "TEST_EVENT";
    private static final String UID = "123";
    private static final String CASE_TYPE_CREATOR_ROLE = "TestAddressBookCreatorCase";
    private static final String CASE_TYPE_CREATOR_ROLE_NO_CREATE_ACCESS = "TestAddressBookCreatorNoCreateAccessCase";
    private static final String REQUEST_ID = "request-id";
    private static final String REQUEST_ID_VALUE = "1234567898765432";
    public static final String ROLE_PROBATE_SOLICITOR = "caseworker-probate-solicitor";

    @Inject
    private WebApplicationContext wac;

    private MockMvc mockMvc;
    @SpyBean
    private AuditRepository auditRepository;

    @Before
    public void setUp() {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldLogAuditInfoForGetCaseById() throws Exception {
        String caseId = "1504259907353529";
        final String URL =  "/cases/" + caseId;

        final MvcResult mvcResult = mockMvc.perform(get(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .header(REQUEST_ID, REQUEST_ID_VALUE)
            .contentType(JSON_CONTENT_TYPE)
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 200, mvcResult.getResponse().getStatus());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
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
    public void shouldLogAuditInfoForCreateEventByCaseId() throws Exception {
        String caseId = "1504259907353529";
        final String URL =  "/cases/" + caseId + "/events";

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

        assertEquals(mvcResult.getResponse().getContentAsString(), 201, mvcResult.getResponse().getStatus());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull("Saved Case Details should not be null", savedCaseResource);

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
    public void shouldReturn201WhenPostCreateCase() throws Exception {
        final String URL =  "/case-types/" + CASE_TYPE + "/cases";
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

        assertEquals(mvcResult.getResponse().getContentAsString(), 201, mvcResult.getResponse().getStatus());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull("Saved Case Details should not be null", savedCaseResource);

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
    public void shouldReturn201WhenPostCreateCaseWithCreatorRoleWithNoDataForCaseworker() throws Exception {
        final String description = "A very long comment.......";
        final String summary = "Short comment";

        final String URL =  "/case-types/" + CASE_TYPE_CREATOR_ROLE + "/cases";

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

        assertEquals(mvcResult.getResponse().getContentAsString(), 201, mvcResult.getResponse().getStatus());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseResource savedCaseResource = mapper.readValue(content, CaseResource.class);
        assertNotNull("Saved Case Details should not be null", savedCaseResource);
    }

    @Test
    public void shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccessOnCreatorRole() throws Exception {
        final String URL =  "/case-types/" + CASE_TYPE_CREATOR_ROLE_NO_CREATE_ACCESS + "/cases";

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
        final String URL =  "/cases/" + caseId + "/supplementary-data";
        SupplementaryDataUpdateRequest supplementaryDataUpdateRequest = createSupplementaryDataSetRequestOrgB();

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(supplementaryDataUpdateRequest))
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 200, mvcResult.getResponse().getStatus());
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
        final String URL =  "/cases/" + caseId + "/supplementary-data";
        SupplementaryDataUpdateRequest supplementaryDataUpdateRequest = createSupplementaryDataSetRequestMultiple();

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(supplementaryDataUpdateRequest))
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 200, mvcResult.getResponse().getStatus());
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
        final String URL =  "/cases/" + caseId + "/supplementary-data";
        SupplementaryDataUpdateRequest supplementaryDataUpdateRequest = createSupplementaryDataIncrementRequest();

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(supplementaryDataUpdateRequest))
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 200, mvcResult.getResponse().getStatus());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = mvcResult.getResponse().getContentAsString();
        SupplementaryDataResource supplementaryDataResource =
            mapper.readValue(content, SupplementaryDataResource.class);
        assertNotNull("updated supplementary data resource", supplementaryDataResource);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldCreateSupplementaryDataWhenNotExists() throws Exception {
        String caseId = "1504259907353545";
        final String URL =  "/cases/" + caseId + "/supplementary-data";
        SupplementaryDataUpdateRequest supplementaryDataUpdateRequest = createSupplementaryDataSetRequest();

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(supplementaryDataUpdateRequest))
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 200, mvcResult.getResponse().getStatus());
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
        final String URL =  "/cases/" + caseId + "/supplementary-data";
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
        final String URL =  "/cases/" + caseId + "/supplementary-data";
        SupplementaryDataUpdateRequest supplementaryDataUpdateRequest = createSupplementaryDataSetRequest();

        final String message = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(supplementaryDataUpdateRequest))
        ).andExpect(status().is(400))
            .andReturn().getResolvedException().getMessage();

        assertTrue(StringUtils.contains(message, CASE_ID_INVALID));
    }

    @Nested
    @DisplayName("GET /cases/{caseId}/supplementary-data")
    private class UserRoleValidation {

        @BeforeEach
        public void setup() {
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
        public void shouldThrowExceptionWhenSolicitorRoleWithNoCasesAssignedToTheUser() throws Exception {

            MockUtils.setSecurityAuthorities(authentication, ROLE_PROBATE_SOLICITOR);
            String caseId = "1504259907353545";
            final String URL =  "/cases/" + caseId + "/supplementary-data";
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
