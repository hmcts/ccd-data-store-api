package uk.gov.hmcts.ccd.endpoint.std;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.MockitoAnnotations;
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

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.hmcts.ccd.auditlog.AuditInterceptor.REQUEST_ID;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;
import static uk.gov.hmcts.ccd.v2.V2.EXPERIMENTAL_HEADER;

/**
 * Integration test for decentralised data persistence feature.
 * Tests the routing of case creation events to external services via ServicePersistenceAPI.
 */
@TestPropertySource(properties = {
    "ccd.decentralised.case-type-service-urls[PCS]=http://localhost:${wiremock.server.port}"
})
public class DecentralisedPersistenceTest extends WireMockBaseTest {

    private static final String DECENTRALISED_CASE_TYPE_ID = "PCS"; // Matches test property
    private static final String JURISDICTION_ID = "PROBATE";
    private static final String USER_ID = "123";
    private static final String CREATE_CASE_EVENT_ID = "CREATE-CASE";
    private static final String SERVICE_PERSISTENCE_API_PATH = "/ccd-persistence/cases";
    private static final String CASE_DATA_FIELD = "case_data";

    private static final String DATA_JSON_STRING = """
        {
          "PersonLastName": "Last Name",
          "PersonAddress": {
            "AddressLine1": "Address Line 1",
            "AddressLine2": "Address Line 2"
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

        MockitoAnnotations.initMocks(this);
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);


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
                      "PersonLastName": "Last Name",
                      "PersonAddress": {
                        "AddressLine1": "Address Line 1",
                        "AddressLine2": "Address Line 2"
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
        final var expectedResponseData = JacksonUtils.convertJsonNode(expectedResponseDataNode);

        assertThat("Response should contain case data from decentralised service (only readable fields)",
            actualResponseData.entrySet(), equalTo(expectedResponseData.entrySet()));

        // AND: The ServicePersistenceAPI should have been called exactly once
        verify(1, postRequestedFor(urlEqualTo(SERVICE_PERSISTENCE_API_PATH))
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
                            "PersonLastName": "Last Name",
                            "PersonAddress": {
                                "AddressLine1": "Address Line 1", 
                                "AddressLine2": "Address Line 2"
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
        Map m = mapper.readValue(content, Map.class);
        var data = (Map) m.get("data");
        assertEquals(data.get("PersonLastName"), "Last Name");

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


    private String getTestDefinition(int port, String caseTypeId) {
        return CallbackTestData.getTestDefinition(port).replace("CallbackCase", caseTypeId);
    }

    private Map<String, Object> extractCaseDataFromResponse(MvcResult mvcResult) throws Exception {
        final var responseJson = mapper.readTree(mvcResult.getResponse().getContentAsString());
        JsonNode caseDataNode = responseJson.get(CASE_DATA_FIELD);
        return mapper.convertValue(caseDataNode, Map.class);
    }
}
