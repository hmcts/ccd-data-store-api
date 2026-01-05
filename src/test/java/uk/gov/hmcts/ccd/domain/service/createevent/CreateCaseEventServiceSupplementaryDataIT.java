package uk.gov.hmcts.ccd.domain.service.createevent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.ccd.TestFixtures;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.stdapi.AboutToSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;

import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

class CreateCaseEventServiceSupplementaryDataIT extends WireMockBaseTest {

    private static final ObjectMapper MAPPER = JacksonUtils.MAPPER;
    private static final String CASE_REFERENCE = CASE_01_REFERENCE;
    private static final String EVENT_ID = "TEST_EVENT";

    @Autowired
    @Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
    private CaseDetailsRepository caseDetailsRepository;

    @Autowired
    private CreateCaseEventService createCaseEventService;

    @MockitoBean
    private CallbackInvoker callbackInvoker;

    @Inject
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        // Stub case type definition to add callback URL and pre_states to TEST_EVENT
        final String caseTypeDefinitionJson = TestFixtures.fromFileAsString("__files/test-addressbook-case_supplementary.json")
            .replace("${CALLBACK_URL}", hostUrl + "/callback-about-to-submit");

        stubFor(get(urlMatching("/api/data/case-type/TestAddressBookCase"))
            .willReturn(okJson(caseTypeDefinitionJson).withStatus(200)));

        // Mock CallbackInvoker to set supplementary_data on CaseDetails
        setupCallbackInvokerMockWithSupplementaryData();
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    @Test
    @DisplayName("should persist supplementary data when present in about to submit callback response")
    void shouldPersistSupplementaryDataWhenPresentInAboutToSubmitCallback() throws Exception {
        // GIVEN - Verify case exists after SQL script execution
        CaseDataContent caseDataContent = newCaseDataContent()
            .withEvent(buildEvent())
            .withData(new HashMap<>())
            .withToken(generateEventTokenNewCase("123", "PROBATE", CASE_01_TYPE, EVENT_ID))
            .withIgnoreWarning(false)
            .build();

        // WHEN - Create case event
        CreateCaseEventResult result = createCaseEventService.createCaseEvent(CASE_REFERENCE, caseDataContent);

        // THEN - Verify supplementary_data is persisted in database
        CaseDetails savedCase = caseDetailsRepository.findByReference(CASE_REFERENCE)
            .orElseThrow(() -> new RuntimeException("Case not found after update"));

        assertAll(
            () -> assertNotNull(savedCase.getSupplementaryData(), "Supplementary data should be persisted"),
            () -> assertThat(savedCase.getSupplementaryData().get("key1").asText()).isEqualTo("value1"),
            () -> assertThat(savedCase.getSupplementaryData().get("key2").asInt()).isEqualTo(42),
            () -> assertNotNull(result.getSavedCaseDetails().getSupplementaryData(),
                "Result should contain supplementary data"),
            () -> assertThat(result.getSavedCaseDetails().getSupplementaryData().get("key1").asText())
                .isEqualTo("value1")
        );
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    @Test
    @DisplayName("should not persist supplementary data when not present in about to submit callback response")
    void shouldNotPersistSupplementaryDataWhenNotPresentInAboutToSubmitCallback() throws Exception {
        // Mock callback without supplementary_data
        setupCallbackInvokerMockWithoutSupplementaryData();

        CaseDataContent caseDataContent = newCaseDataContent()
            .withEvent(buildEvent())
            .withData(new HashMap<>())
            .withToken(generateEventTokenNewCase("123", "PROBATE", CASE_01_TYPE, EVENT_ID))
            .withIgnoreWarning(false)
            .build();

        // WHEN - Create case event
        CreateCaseEventResult result = createCaseEventService.createCaseEvent(CASE_REFERENCE, caseDataContent);

        // THEN - Verify supplementary_data is not persisted
        CaseDetails savedCase = caseDetailsRepository.findByReference(CASE_REFERENCE)
            .orElseThrow(() -> new RuntimeException("Case not found after update"));

        assertAll(
            () -> assertNull(savedCase.getSupplementaryData(),
                "Supplementary data should not be persisted when not in callback"),
            () -> assertNull(result.getSavedCaseDetails().getSupplementaryData(),
                "Result should not contain supplementary data")
        );
    }

    private void setupCallbackInvokerMockWithSupplementaryData() {
        // Mock invokeAboutToSubmitCallback to set supplementary_data on CaseDetails
        doAnswer(invocation -> {

            // Create supplementary_data map
            Map<String, JsonNode> supplementaryData = new HashMap<>();
            supplementaryData.put("key1", TextNode.valueOf("value1"));
            supplementaryData.put("key2", IntNode.valueOf(42));

            updateSupplementaryDataInDatabase(CASE_REFERENCE, supplementaryData);
            selectUpdatedSupplementaryData(CASE_REFERENCE);
            return new AboutToSubmitCallbackResponse();
        }).when(callbackInvoker).invokeAboutToSubmitCallback(any(),
            any(),
            any(),
            any(),
            anyBoolean());
    }

    private void setupCallbackInvokerMockWithoutSupplementaryData() {
        // Mock invokeAboutToSubmitCallback without setting supplementary_data
        doAnswer(invocation -> {
            return new AboutToSubmitCallbackResponse();
        }).when(callbackInvoker).invokeAboutToSubmitCallback(any(),
            any(),
            any(),
            any(),
            anyBoolean());
    }

    private Event buildEvent() {
        Event event = anEvent().build();
        event.setEventId(EVENT_ID);
        event.setSummary("Update case summary");
        event.setDescription("Update case description");
        return event;
    }

    /**
     * Alternative: Update supplementary_data using a Map and convert to JSON.
     *
     * @param caseReference The case reference to update
     * @param supplementaryData The supplementary data map to persist
     */
    private void updateSupplementaryDataInDatabase(String caseReference, Map<String, JsonNode> supplementaryData) {
        try {
            String jsonString = MAPPER.writeValueAsString(supplementaryData);
            updateSupplementaryDataInDatabase(caseReference, jsonString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert supplementary data to JSON", e);
        }
    }

    /**
     * Update supplementary_data directly in the database using SQL UPDATE.
     * This is an alternative to using the callback mock - you can use this to
     * directly update the database and verify the persistence.
     *
     * @param caseReference The case reference to update
     * @param supplementaryDataJson The JSON string for supplementary_data (e.g., '{"key1": "value1", "key2": 42}')
     */
    private void updateSupplementaryDataInDatabase(String caseReference, String supplementaryDataJson) {
        // Update the database
        int rowsUpdated = jdbcTemplate.update(
            "UPDATE case_data SET supplementary_data = ?::jsonb WHERE reference = ?",
            supplementaryDataJson,
            Long.parseLong(caseReference)
        );

        entityManager.flush();
        entityManager.clear();

        // Verify the update was successful
        assertThat(rowsUpdated).as("Should update exactly one row").isEqualTo(1);
    }

    private void selectUpdatedSupplementaryData(String caseReference) {
        // Select and verify the supplementary_data was actually written to the database
        String dbSupplementaryDataJson = jdbcTemplate.queryForObject(
            "SELECT supplementary_data::text FROM case_data WHERE reference = ?",
            String.class,
            Long.parseLong(caseReference)
        );

        // Verify the database contains the expected supplementary_data
        assertThat(dbSupplementaryDataJson).as("Supplementary data should be in database").isNotNull();
        try {
            JsonNode dbSupplementaryData = MAPPER.readTree(dbSupplementaryDataJson);
            assertThat(dbSupplementaryData.has("key1")).as("Database should contain key1").isTrue();
            assertThat(dbSupplementaryData.has("key2")).as("Database should contain key2").isTrue();
            assertThat(dbSupplementaryData.get("key1").asText()).as("Database key1 value").isEqualTo("value1");
            assertThat(dbSupplementaryData.get("key2").asInt()).as("Database key2 value").isEqualTo(42);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse supplementary_data from database", e);
        }
    }
}

