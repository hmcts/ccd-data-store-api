package uk.gov.hmcts.ccd.endpoint.std;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.endpoint.CallbackTestData;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.hmcts.ccd.auditlog.AuditInterceptor.REQUEST_ID;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;
import static uk.gov.hmcts.ccd.v2.V2.EXPERIMENTAL_HEADER;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.roleAssignmentResponseJson;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.userRoleAssignmentJson;

/**
 * Integration test for decentralised data persistence feature.
 * Tests the routing of case creation events to external services via ServicePersistenceAPI.
 */
@TestPropertySource(properties = {
    "ccd.decentralised.case-type-service-urls[PCS]=http://localhost:${wiremock.server.port}"
})
public class DecentralisedPersistenceIT extends WireMockBaseTest {

    private static final String DECENTRALISED_CASE_TYPE_ID = "PCS";
    private static final String JURISDICTION_ID = "PROBATE";
    private static final String USER_ID = "123";
    private static final String CREATE_CASE_EVENT_ID = "CREATE-CASE";
    private static final String SERVICE_PERSISTENCE_API_PATH = "/ccd-persistence/cases";
    private static final String CASE_DATA_FIELD = "case_data";

    private static final String DATA_JSON_STRING = """
        {
          "PersonFirstName": "ccd-George",
          "PersonLastName": "Roof",
          "PersonAddress": {
            "Country": "Wales",
            "Postcode": "WB11DDF",
            "AddressLine1": "Flat 9",
            "AddressLine2": "2 Hubble Avenue",
            "AddressLine3": "ButtonVille"
          }
        }
        """;

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;
    private JdbcTemplate jdbcTemplate;
    private JsonNode data;

    @Before
    public void setUp() throws IOException {
        data = mapper.readTree(DATA_JSON_STRING);

        MockUtils.setSecurityAuthorities(authentication,
            MockUtils.ROLE_TEST_PUBLIC,
            MockUtils.ROLE_CASEWORKER_PUBLIC);

        stubUserInfo(USER_ID);


        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        jdbcTemplate = new JdbcTemplate(db);

        // Stub the case type definition API to return our decentralised case type
        stubFor(WireMock.get(urlMatching("/api/data/case-type/" + DECENTRALISED_CASE_TYPE_ID))
            .willReturn(okJson(getTestDefinition(wiremockPort, DECENTRALISED_CASE_TYPE_ID)).withStatus(200)));
    }

    @Test
    public void test1_shouldInvokeServicePersistenceAPIWhenCreatingDecentralisedCase() throws Exception {
        // GIVEN: A decentralised case type with a configured service URL
        final var url = String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases",
            USER_ID, JURISDICTION_ID, DECENTRALISED_CASE_TYPE_ID);

        final var caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().withEventId(CREATE_CASE_EVENT_ID).build());
        caseDetailsToSave.setData(JacksonUtils.convertValue(data));
        caseDetailsToSave.setToken(generateEventTokenNewCase(USER_ID, JURISDICTION_ID,
            DECENTRALISED_CASE_TYPE_ID, CREATE_CASE_EVENT_ID));

        final String responseBodyTemplate = """
            {
                "errors": [],
                "warnings": [],
                "case_details": {
                    "id": {{jsonPath request.body '$.case_details.id'}},
                    "case_type_id": "{{jsonPath request.body '$.case_details.case_type_id'}}",
                    "jurisdiction": "{{jsonPath request.body '$.case_details.jurisdiction'}}",
                    "state": "{{jsonPath request.body '$.case_details.state'}}",
                    "version": 1,
                    "security_classification": "PUBLIC",
                    "case_data": {
                      "PersonFirstName": "ccd-George",
                      "PersonLastName": "Roof",
                      "PersonAddress": {
                        "Country": "Wales",
                        "Postcode": "WB11DDF",
                        "AddressLine1": "Flat 9",
                        "AddressLine2": "2 Hubble Avenue",
                        "AddressLine3": "ButtonVille"
                      }
                    }
                },
                "revision": 1
            }
            """;

        // WHEN: We stub the ServicePersistenceAPI to return a successful response using a dynamic template
        stubFor(WireMock.post(urlEqualTo(SERVICE_PERSISTENCE_API_PATH))
            .withHeader("Idempotency-Key", matching(".*"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(responseBodyTemplate)
                .withTransformers("response-template"))); // Enable response templating

        // AND: We create a case via CCD's API
        final var mvcResult = mockMvc.perform(post(url)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        // THEN: The request should succeed
        assertEquals("Expected successful case creation", 201, mvcResult.getResponse().getStatus());

        // AND: Extract and store the case reference for use in subsequent tests
        final var responseJson = mapper.readTree(mvcResult.getResponse().getContentAsString());

        // Extract the case reference from the response - it's in the 'id' field at the top level
        JsonNode referenceNode = responseJson.get("id");
        assertNotNull("Case reference should be present in response", referenceNode);

        // AND: The response should contain the case data from the decentralised service
        final var actualResponseData = extractCaseDataFromResponse(mvcResult);
        final var expectedResponseDataNode = mapper.readTree(DATA_JSON_STRING);
        final var expectedAddress = JacksonUtils.convertJsonNode(expectedResponseDataNode.get("PersonAddress"));

        assertThat(actualResponseData)
            .as("Response should contain case data from decentralised service (only readable fields)")
            .containsEntry("PersonLastName", expectedResponseDataNode.get("PersonLastName").asText())
            .containsEntry("PersonAddress", expectedAddress);

        // AND: The ServicePersistenceAPI should have been called exactly once
        verify(postRequestedFor(urlEqualTo(SERVICE_PERSISTENCE_API_PATH))
            .withHeader("Idempotency-Key", matching(".*")));

        // AND: A case pointer should be created in the local database
        final var casePointers = jdbcTemplate.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Expected exactly one case pointer in local database", 1, casePointers.size());

        final var casePointer = casePointers.get(0);
        assertEquals("Case pointer should have correct case type", DECENTRALISED_CASE_TYPE_ID,
            casePointer.getCaseTypeId());
        assertEquals("Case pointer should have empty data", Map.of(), casePointer.getData());
        assertEquals("Case pointer should be in empty state", "", casePointer.getState());
        assertNotNull("Case pointer should have a reference", casePointer.getReference());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_case_pointer.sql"})
    public void testGetCase() throws Exception {
        String caseId = "1644062237356399";
        final String URL = "/cases/" + caseId;

        // Configure WireMock stub for ServicePersistenceAPI getCases endpoint
        final String getCasesResponseBody = """
            [
                {
                    "case_details": {
                        "id": 1644062237356399,
                        "case_type_id": "PCS",
                        "jurisdiction": "PROBATE",
                        "state": "CaseCreated",
                        "version": 1,
                        "security_classification": "PUBLIC",
                        "case_data": {
                            "PersonFirstName": "ccd-Existing",
                            "PersonLastName": "Roof",
                            "PersonAddress": {
                                "Country": "Wales",
                                "Postcode": "WB11DDF",
                                "AddressLine1": "Flat 9",
                                "AddressLine2": "2 Hubble Avenue",
                                "AddressLine3": "ButtonVille"
                            }
                        }
                    },
                    "revision": 1
                }
            ]
            """;

        stubFor(WireMock.get(urlMatching("/ccd-persistence/cases.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(getCasesResponseBody)));

        final MvcResult mvcResult = mockMvc.perform(get(URL)
            .header(EXPERIMENTAL_HEADER, "experimental")
            .header(REQUEST_ID, "12335")
            .contentType(JSON_CONTENT_TYPE)
        ).andReturn();

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        String content = mvcResult.getResponse().getContentAsString();
        Assertions.assertNotNull(content, "Content Should not be null");
        Map<?, ?> responseBody = mapper.readValue(content, Map.class);
        Map<?, ?> responseData = (Map<?, ?>) responseBody.get("data");
        assertEquals("Roof", responseData.get("PersonLastName"));

    }

    @Test
    public void shouldRollbackCasePointerWhenServicePersistenceAPIReturnsErrorsResponse() throws Exception {
        // GIVEN: We first create a case pointer directly by setting up a successful response
        // then simulating a failure on the second request
        final var url = String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases",
            USER_ID, JURISDICTION_ID, DECENTRALISED_CASE_TYPE_ID);

        final var caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().withEventId(CREATE_CASE_EVENT_ID).build());
        caseDetailsToSave.setData(JacksonUtils.convertValue(data));
        caseDetailsToSave.setToken(generateEventTokenNewCase(USER_ID, JURISDICTION_ID,
            DECENTRALISED_CASE_TYPE_ID, CREATE_CASE_EVENT_ID));

        // WHEN: We stub the ServicePersistenceAPI to return a response with validation errors
        final String errorResponseBody = """
            {
                "errors": ["Field is required", "Invalid value provided"],
                "warnings": [],
                "case_details": null
            }
            """;

        stubFor(WireMock.post(urlEqualTo(SERVICE_PERSISTENCE_API_PATH))
            .withHeader("Idempotency-Key", matching(".*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(errorResponseBody)));

        // AND: We attempt to create a case via CCD's API
        final var mvcResult = mockMvc.perform(post(url)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        // THEN: The request should fail due to validation errors
        assertEquals("Expected 422 Unprocessable Entity due to validation errors",
            422, mvcResult.getResponse().getStatus());

        // AND: No case pointer should remain in the local database (rolled back)
        final var casePointers = jdbcTemplate.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Expected no case pointers in local database after rollback", 0, casePointers.size());
    }

    @Test
    public void shouldReturn409ConflictWhenServicePersistenceAPIReturnsConflict() throws Exception {
        // GIVEN: A decentralised case type with a configured service URL
        final var url = String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases",
            USER_ID, JURISDICTION_ID, DECENTRALISED_CASE_TYPE_ID);

        final var caseDetailsToSave = newCaseDataContent().build();
        caseDetailsToSave.setEvent(anEvent().withEventId(CREATE_CASE_EVENT_ID).build());
        caseDetailsToSave.setData(JacksonUtils.convertValue(data));
        caseDetailsToSave.setToken(generateEventTokenNewCase(USER_ID, JURISDICTION_ID,
            DECENTRALISED_CASE_TYPE_ID, CREATE_CASE_EVENT_ID));

        // WHEN: We stub the ServicePersistenceAPI to return a 409 Conflict response
        stubFor(WireMock.post(urlEqualTo(SERVICE_PERSISTENCE_API_PATH))
            .withHeader("Idempotency-Key", matching(".*"))
            .willReturn(aResponse()
                .withStatus(409)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\": \"Case already exists\"}")));

        // AND: We attempt to create a case via CCD's API
        final var mvcResult = mockMvc.perform(post(url)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        // THEN: The request should return a 409 Conflict status
        assertEquals("Expected 409 Conflict response", 409, mvcResult.getResponse().getStatus());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_case_pointer.sql"})
    public void shouldSubmitDecentralisedEventAndUpdatePointerMetadata() throws Exception {
        final String caseId = "1644062237356399";
        final Long caseReference = Long.valueOf(caseId);
        final String url = String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases/%s/events",
            USER_ID, JURISDICTION_ID, DECENTRALISED_CASE_TYPE_ID, caseId);

        final Integer initialVersion = jdbcTemplate.queryForObject(
            "SELECT version FROM case_data WHERE reference = ?",
            Integer.class,
            caseReference
        );
        assertNotNull("Case pointer should exist before submitting event", initialVersion);

        final JsonNode existingCaseData = buildLegacyPersonData();

        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/ccd-persistence/cases"))
            .withQueryParam("case-refs", equalTo(caseId))
            .willReturn(okJson(mapper.writeValueAsString(
                List.of(remoteCaseDetails(caseReference, existingCaseData, 1, "CaseCreated"))))));

        final JsonNode updatedCaseData = existingCaseData.deepCopy();

        final int expectedRevision = 2;
        final var submitResponse = mapper.createObjectNode();
        submitResponse.putArray("errors");
        submitResponse.putArray("warnings");
        final var responseCaseDetails = submitResponse.putObject("case_details");
        responseCaseDetails.put("id", caseReference);
        responseCaseDetails.put("case_type_id", DECENTRALISED_CASE_TYPE_ID);
        responseCaseDetails.put("jurisdiction", JURISDICTION_ID);
        responseCaseDetails.put("state", "CaseUpdated");
        responseCaseDetails.put("version", expectedRevision);
        responseCaseDetails.put("security_classification", "PUBLIC");
        responseCaseDetails.set("case_data", updatedCaseData);
        submitResponse.put("revision", expectedRevision);

        wireMockServer.stubFor(WireMock.post(urlEqualTo(SERVICE_PERSISTENCE_API_PATH))
            .withHeader("Idempotency-Key", matching(".*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(mapper.writeValueAsString(submitResponse))));

        stubSuccessfulBeforeStartCallback();

        final String eventToken = startEventToken(caseId, "UPDATE-EVENT");
        final var caseDetailsContent = buildEventContent(eventToken, updatedCaseData, "UPDATE-EVENT");
        final var mvcResult = submitEvent(url, caseDetailsContent);

        assertEquals("Expected successful decentralised event submission", 201, mvcResult.getResponse().getStatus());
        final JsonNode responseJson = mapper.readTree(mvcResult.getResponse().getContentAsString());
        final JsonNode caseDataNode = responseJson.get(CASE_DATA_FIELD);
        assertThat(caseDataNode.get("PersonLastName")).isEqualTo(updatedCaseData.get("PersonLastName"));
        assertThat(caseDataNode.get("PersonAddress")).isEqualTo(updatedCaseData.get("PersonAddress"));

        verify(getRequestedFor(urlPathEqualTo("/ccd-persistence/cases"))
            .withQueryParam("case-refs", equalTo(caseId)));
        verify(postRequestedFor(urlEqualTo(SERVICE_PERSISTENCE_API_PATH))
            .withHeader("Idempotency-Key", matching(".*")));

        final Integer updatedVersion = jdbcTemplate.queryForObject(
            "SELECT version FROM case_data WHERE reference = ?",
            Integer.class,
            caseReference
        );
        assertThat(updatedVersion)
            .as("Pointer version should be updated to the newest decentralised revision")
            .isEqualTo(expectedRevision);


        final String storedData = jdbcTemplate.queryForObject(
            "SELECT data::TEXT FROM case_data WHERE reference = ?",
            String.class,
            caseReference
        );
        assertEquals("Decentralised pointer should not store mutable case data", "{}", storedData);

        wireMockServer.resetAll();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_case_pointer.sql"})
    public void shouldReturnDecentralisedEventsForCaseworker() throws Exception {
        final String caseReference = "1644062237356399";
        final String url = String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases/%s/events",
            USER_ID, JURISDICTION_ID, DECENTRALISED_CASE_TYPE_ID, caseReference);

        stubRoleAssignments(caseReference);

        final Long caseReferenceId = Long.valueOf(caseReference);
        final JsonNode existingCaseData = buildLegacyPersonData();

        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/ccd-persistence/cases"))
            .withQueryParam("case-refs", equalTo(caseReference))
            .willReturn(okJson(mapper.writeValueAsString(
                List.of(remoteCaseDetails(caseReferenceId, existingCaseData, 1, "CaseCreated"))))));

        final ArrayNode historyPayload = mapper.createArrayNode();
        final ObjectNode eventWrapper = historyPayload.addObject();
        eventWrapper.put("id", 99L);
        eventWrapper.put("case_reference", caseReferenceId);
        final ObjectNode eventNode = eventWrapper.putObject("event");
        eventNode.put("event_name", "Create Case");
        eventNode.put("summary", "Decentralised case created");
        eventNode.put("description", "Remote service created the case");
        eventNode.put("state_id", "CaseCreated");
        eventNode.put("state_name", "CaseCreated");
        eventNode.put("case_type_id", DECENTRALISED_CASE_TYPE_ID);
        eventNode.put("created_date", "2024-06-15T10:15:30");
        eventNode.put("security_classification", "PUBLIC");
        eventNode.put("id", "CREATE-CASE");

        wireMockServer.stubFor(WireMock.get(urlEqualTo("/ccd-persistence/cases/" + caseReference + "/history"))
            .willReturn(okJson(mapper.writeValueAsString(historyPayload)).withStatus(200)));

        final MvcResult mvcResult = mockMvc.perform(get(url)
            .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertEquals("Expected decentralised events to be returned", 200, mvcResult.getResponse().getStatus());

        final AuditEvent[] events =
            mapper.readValue(mvcResult.getResponse().getContentAsString(), AuditEvent[].class);

        assertThat(events)
            .as("Decentralised events response should surface the remote audit entry")
            .hasSize(1);

        final AuditEvent event = events[0];
        assertThat(event.getEventName()).isEqualTo("Create Case");
        assertThat(event.getEventId()).isEqualTo("CREATE-CASE");
        assertThat(event.getCaseTypeId()).isEqualTo(DECENTRALISED_CASE_TYPE_ID);
        assertThat(event.getStateId()).isEqualTo("CaseCreated");

        verify(getRequestedFor(urlEqualTo("/ccd-persistence/cases/" + caseReference + "/history")));

        wireMockServer.resetAll();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_case_pointer.sql"})
    public void shouldReturn422WhenDecentralisedServiceReturnsErrors() throws Exception {
        final String caseId = "1644062237356399";
        final Long caseReference = Long.valueOf(caseId);
        final String url = String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases/%s/events",
            USER_ID, JURISDICTION_ID, DECENTRALISED_CASE_TYPE_ID, caseId);

        final Integer initialVersion = jdbcTemplate.queryForObject(
            "SELECT version FROM case_data WHERE reference = ?",
            Integer.class,
            caseReference
        );
        assertNotNull("Case pointer should exist before submitting event", initialVersion);

        final JsonNode existingCaseData = buildLegacyPersonData();

        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/ccd-persistence/cases"))
            .withQueryParam("case-refs", equalTo(caseId))
            .willReturn(okJson(mapper.writeValueAsString(
                List.of(remoteCaseDetails(caseReference, existingCaseData, 1, "CaseCreated"))))));

        wireMockServer.stubFor(WireMock.post(urlEqualTo(SERVICE_PERSISTENCE_API_PATH))
            .withHeader("Idempotency-Key", matching(".*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "errors": ["Field is required"],
                        "warnings": [],
                        "case_details": null
                    }
                    """)));

        stubSuccessfulBeforeStartCallback();

        final String eventToken = startEventToken(caseId, "UPDATE-EVENT");
        final var caseDetailsContent = buildEventContent(eventToken, existingCaseData, "UPDATE-EVENT");
        final var mvcResult = submitEvent(url, caseDetailsContent);

        assertEquals("Expected validation errors from decentralised service", 422, mvcResult.getResponse().getStatus());

        verify(getRequestedFor(urlPathEqualTo("/ccd-persistence/cases"))
            .withQueryParam("case-refs", equalTo(caseId)));
        verify(postRequestedFor(urlEqualTo(SERVICE_PERSISTENCE_API_PATH))
            .withHeader("Idempotency-Key", matching(".*")));

        final Integer updatedVersion = jdbcTemplate.queryForObject(
            "SELECT version FROM case_data WHERE reference = ?",
            Integer.class,
            caseReference
        );
        assertThat(updatedVersion)
            .as("Pointer version should remain unchanged when decentralised submit fails")
            .isEqualTo(initialVersion);

        final String storedData = jdbcTemplate.queryForObject(
            "SELECT data::TEXT FROM case_data WHERE reference = ?",
            String.class,
            caseReference
        );
        assertEquals("Pointer should remain as metadata-only shell after failure", "{}", storedData);

        wireMockServer.resetAll();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_case_pointer.sql"})
    public void shouldTreatRepeatedSubmitWithSameTokenIdempotently() throws Exception {
        final String caseId = "1644062237356399";
        final Long caseReference = Long.valueOf(caseId);
        final String url = String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases/%s/events",
            USER_ID, JURISDICTION_ID, DECENTRALISED_CASE_TYPE_ID, caseId);

        final JsonNode existingCaseData = buildLegacyPersonData();

        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/ccd-persistence/cases"))
            .withQueryParam("case-refs", equalTo(caseId))
            .willReturn(okJson(mapper.writeValueAsString(
                List.of(remoteCaseDetails(caseReference, existingCaseData, 1, "CaseCreated"))))));

        final int expectedRevision = 2;
        final String successResponseBody = mapper.writeValueAsString(submitResponseBody(caseReference,
            expectedRevision,
            existingCaseData));

        wireMockServer.stubFor(WireMock.post(urlEqualTo(SERVICE_PERSISTENCE_API_PATH))
            .inScenario("IdempotentDecentralisedSubmit")
            .whenScenarioStateIs(Scenario.STARTED)
            .withHeader("Idempotency-Key", matching(".*"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(successResponseBody))
            .willSetStateTo("RETRY"));

        wireMockServer.stubFor(WireMock.post(urlEqualTo(SERVICE_PERSISTENCE_API_PATH))
            .inScenario("IdempotentDecentralisedSubmit")
            .whenScenarioStateIs("RETRY")
            .withHeader("Idempotency-Key", matching(".*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(successResponseBody)));

        stubSuccessfulBeforeStartCallback();

        final String eventToken = startEventToken(caseId, "UPDATE-EVENT");
        final var caseDetailsContent = buildEventContent(eventToken, existingCaseData, "UPDATE-EVENT");

        final var firstResult = submitEvent(url, caseDetailsContent);
        assertEquals("First submission should create the event", 201, firstResult.getResponse().getStatus());

        final Integer versionAfterFirstSubmit = jdbcTemplate.queryForObject(
            "SELECT version FROM case_data WHERE reference = ?",
            Integer.class,
            caseReference
        );
        assertThat(versionAfterFirstSubmit).isEqualTo(expectedRevision);

        final var secondResult = submitEvent(url, caseDetailsContent);
        assertThat(secondResult.getResponse().getStatus())
            .as("Replay should not trigger a second create in the decentralised service")
            .isIn(200, 201);
        assertEquals("Idempotent response body should match first submission",
            firstResult.getResponse().getContentAsString(),
            secondResult.getResponse().getContentAsString());

        final Integer versionAfterSecondSubmit = jdbcTemplate.queryForObject(
            "SELECT version FROM case_data WHERE reference = ?",
            Integer.class,
            caseReference
        );
        assertThat(versionAfterSecondSubmit).isEqualTo(expectedRevision);

        wireMockServer.resetAll();
    }

    private void stubRoleAssignments(String caseReference) {
        final String responseJson = roleAssignmentResponseJson(
            userRoleAssignmentJson(USER_ID, MockUtils.ROLE_CASEWORKER_PUBLIC, caseReference,
                DECENTRALISED_CASE_TYPE_ID),
            userRoleAssignmentJson(USER_ID, MockUtils.ROLE_TEST_PUBLIC, caseReference,
                DECENTRALISED_CASE_TYPE_ID));

        stubFor(WireMock.get(urlEqualTo("/am/role-assignments/actors/" + USER_ID))
            .willReturn(okJson(responseJson).withStatus(200)));
    }

    private ObjectNode submitResponseBody(Long caseReference, int revision, JsonNode caseData) {
        final ObjectNode response = mapper.createObjectNode();
        response.putArray("errors");
        response.putArray("warnings");
        final ObjectNode caseDetailsNode = response.putObject("case_details");
        caseDetailsNode.put("id", caseReference);
        caseDetailsNode.put("case_type_id", DECENTRALISED_CASE_TYPE_ID);
        caseDetailsNode.put("jurisdiction", JURISDICTION_ID);
        caseDetailsNode.put("state", "CaseUpdated");
        caseDetailsNode.put("version", revision);
        caseDetailsNode.put("security_classification", "PUBLIC");
        caseDetailsNode.set("case_data", caseData);
        response.put("revision", revision);
        return response;
    }

    private String getTestDefinition(int port, String caseTypeId) {
        return CallbackTestData.getTestDefinition(port).replace("CallbackCase", caseTypeId);
    }

    private Map<String, Object> extractCaseDataFromResponse(MvcResult mvcResult) throws Exception {
        final var responseJson = mapper.readTree(mvcResult.getResponse().getContentAsString());
        JsonNode caseDataNode = responseJson.get(CASE_DATA_FIELD);
        return mapper.convertValue(caseDataNode, Map.class);
    }

    private ObjectNode remoteCaseDetails(Long caseReference,
                                         JsonNode caseData,
                                         int version,
                                         String state) {
        final ObjectNode wrapper = mapper.createObjectNode();
        final ObjectNode remoteCase = wrapper.putObject("case_details");
        remoteCase.put("id", caseReference);
        remoteCase.put("case_type_id", DECENTRALISED_CASE_TYPE_ID);
        remoteCase.put("jurisdiction", JURISDICTION_ID);
        remoteCase.put("state", state);
        remoteCase.put("version", version);
        remoteCase.put("security_classification", "PUBLIC");
        remoteCase.set("case_data", caseData);
        wrapper.put("revision", version);
        return wrapper;
    }

    private String startEventToken(String caseId, String eventId) throws Exception {
        final String startEventUrl = String.format(
            "/caseworkers/%s/jurisdictions/%s/case-types/%s/cases/%s/event-triggers/%s/token",
            USER_ID,
            JURISDICTION_ID,
            DECENTRALISED_CASE_TYPE_ID,
            caseId,
            eventId
        );

        final var startEventResult = mockMvc.perform(get(startEventUrl)
            .contentType(JSON_CONTENT_TYPE)
        ).andReturn();

        assertEquals("Start event should provide a valid token", 200, startEventResult.getResponse().getStatus());
        final JsonNode startEventJson = mapper.readTree(startEventResult.getResponse().getContentAsString());
        return startEventJson.get("token").asText();
    }

    private CaseDataContent buildEventContent(String token, JsonNode caseData, String eventId) {
        final var content = newCaseDataContent().build();
        content.setToken(token);
        content.setEvent(anEvent().withEventId(eventId).build());
        content.setData(JacksonUtils.convertValue(caseData));
        return content;
    }

    private MvcResult submitEvent(String url, CaseDataContent content) throws Exception {
        return mockMvc.perform(post(url)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(content))
        ).andReturn();
    }

    private void stubSuccessfulBeforeStartCallback() {
        wireMockServer.stubFor(WireMock.post(urlMatching("/before-start.*"))
            .willReturn(okJson("""
                {
                  "data": {},
                  "errors": [],
                  "warnings": []
                }
                """).withStatus(200)));
    }

    private JsonNode buildLegacyPersonData() {
        final ObjectNode person = mapper.createObjectNode();
        final ObjectNode address = mapper.createObjectNode();
        address.put("Country", "Wales");
        address.put("Postcode", "WB11DDF");
        address.put("AddressLine1", "Flat 9");
        address.put("AddressLine2", "2 Hubble Avenue");
        address.put("AddressLine3", "ButtonVille");
        person.set("PersonAddress", address);
        person.put("PersonLastName", "Roof");
        person.put("PersonFirstName", "ccd-Existing");
        return person;
    }
}
