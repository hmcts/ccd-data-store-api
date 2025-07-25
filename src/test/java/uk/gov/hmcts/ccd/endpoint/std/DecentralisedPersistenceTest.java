package uk.gov.hmcts.ccd.endpoint.std;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
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
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PUBLIC;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

/**
 * Integration test for decentralised data persistence feature.
 * Tests the routing of case creation events to external services via ServicePersistenceAPI.
 */
@TestPropertySource(properties = {
    "ccd.decentralised.case-type-service-urls[PCS]=http://localhost:${wiremock.server.port}"
})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DecentralisedPersistenceTest extends WireMockBaseTest {

    private static final String DECENTRALISED_CASE_TYPE_ID = "PCS"; // Matches test property
    private static final String JURISDICTION_ID = "TEST";
    private static final String USER_ID = "123";
    private static final String CREATE_CASE_EVENT_ID = "CREATE-CASE";
    private static final String SERVICE_PERSISTENCE_API_PATH = "/ccd/cases";
    private static final String CASE_DETAILS_FIELD = "case_details";
    private static final String CASE_DATA_FIELD = "case_data";
    private static final Long TEST_CASE_REFERENCE = 1234567890123456L;

    private static final String DATA_JSON_STRING = """
        {
          "PersonFirstName": "ccd-First Name",
          "PersonLastName": "Last Name",
          "PersonAddress": {
            "AddressLine1": "Address Line 1",
            "AddressLine2": "Address Line 2"
          }
        }
        """;

    // Expected response data should only include fields with read permissions
    private static final String EXPECTED_RESPONSE_DATA_JSON_STRING = """
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
    private static Long createdCaseReference;

    @Before
    public void setUp() throws IOException {
        data = mapper.readTree(DATA_JSON_STRING);

        MockitoAnnotations.initMocks(this);
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_TEST_PUBLIC);

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

        final var expectedCaseDetails = createExpectedCaseDetails();
        final var decentralisedResponseJson = """
            {"%s": %s}
            """.formatted(CASE_DETAILS_FIELD, mapper.writeValueAsString(expectedCaseDetails));

        // WHEN: We stub the ServicePersistenceAPI to return a successful response
        stubFor(WireMock.post(urlEqualTo(SERVICE_PERSISTENCE_API_PATH))
            .withHeader("Idempotency-Key", matching(".*"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(decentralisedResponseJson)));

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
        if (referenceNode == null) {
            // Fallback: try to find it in case_details.reference if the response structure is different
            JsonNode caseDetailsNode = responseJson.get(CASE_DETAILS_FIELD);
            if (caseDetailsNode != null) {
                referenceNode = caseDetailsNode.get("reference");
            }
        }
        assertNotNull("Case reference should be present in response", referenceNode);
        createdCaseReference = referenceNode.asLong();
        assertNotNull("Case reference should be present in response", createdCaseReference);
        System.out.println("DEBUG: Set case reference in test1: " + createdCaseReference);

        // AND: The response should contain the case data from the decentralised service
        final var actualResponseData = extractCaseDataFromResponse(mvcResult);
        final var expectedResponseDataNode = mapper.readTree(EXPECTED_RESPONSE_DATA_JSON_STRING);
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
        assertEquals("Case pointer should have correct case type", DECENTRALISED_CASE_TYPE_ID, casePointer.getCaseTypeId());
        assertEquals("Case pointer should have empty data", Map.of(), casePointer.getData());
        assertEquals("Case pointer should be in empty state", "", casePointer.getState());
        assertNotNull("Case pointer should have a reference", casePointer.getReference());
    }

    private CaseDetails createExpectedCaseDetails() {
        final var caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(DECENTRALISED_CASE_TYPE_ID);
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setReference(TEST_CASE_REFERENCE);
        caseDetails.setState("CaseCreated");
        caseDetails.setData(JacksonUtils.convertValue(data));
        caseDetails.setSecurityClassification(PUBLIC);
        return caseDetails;
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
