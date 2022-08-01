package uk.gov.hmcts.ccd.domain.service.casedeletion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.config.JacksonObjectMapperConfig;
import uk.gov.hmcts.ccd.domain.model.casedeletion.TTL;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.casedeletion.TTL.NO;
import static uk.gov.hmcts.ccd.domain.model.casedeletion.TTL.TTL_CASE_FIELD_ID;
import static uk.gov.hmcts.ccd.domain.model.casedeletion.TTLTest.TTL_SUSPENDED_VALUES_FOR_IS_SUSPENDED_FALSE;
import static uk.gov.hmcts.ccd.domain.model.casedeletion.TTLTest.TTL_SUSPENDED_VALUES_FOR_IS_SUSPENDED_TRUE;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.TEXT;
import static uk.gov.hmcts.ccd.domain.service.casedeletion.TimeToLiveService.TIME_TO_LIVE_GUARD_ERROR_MESSAGE;

@SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
@ExtendWith(MockitoExtension.class)
class TimeToLiveServiceTest {

    @InjectMocks
    private TimeToLiveService timeToLiveService;
    private static final Integer TTL_INCREMENT = 10;
    private static final Integer TTL_GUARD = 365;
    private static final LocalDate FAR_FUTURE_DATE = LocalDate.now().plusDays(TTL_GUARD * 2);
    private final Map<String, JsonNode> caseData = new HashMap<>();

    @Spy
    private ObjectMapper objectMapper = new JacksonObjectMapperConfig().defaultObjectMapper();

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private CaseDataService caseDataService;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        caseData.put("key", objectMapper.readTree("{\"Value\": \"value\"}"));
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static final String TTL_MISSING_OR_NULL
        = "uk.gov.hmcts.ccd.domain.service.casedeletion.TimeToLiveServiceTest#getTestCaseDataWithNoTTLAsArguments";

    @SuppressWarnings("unused")
    private static List<Arguments> getTestCaseDataWithNoTTLAsArguments() {

        var mapper = new JacksonObjectMapperConfig().defaultObjectMapper();

        final Map<String, JsonNode> caseDataTTLMissing = new HashMap<>();
        final Map<String, JsonNode> caseDataTTLNull = new HashMap<>();
        caseDataTTLNull.put("TTL", null);
        final Map<String, JsonNode> caseDataTTLPropertiesNull = new HashMap<>();
        caseDataTTLPropertiesNull.put("TTL", mapper.valueToTree(new TTL()));

        List<Arguments> arguments = new ArrayList<>();
        arguments.add(Arguments.of((Map<String, JsonNode>)null));
        arguments.add(Arguments.of(caseDataTTLMissing));
        arguments.add(Arguments.of(caseDataTTLNull));
        arguments.add(Arguments.of(caseDataTTLPropertiesNull));

        return arguments;
    }

    @Nested
    @DisplayName("isCaseTypeUsingTTL")
    class IsCaseTypeUsingTTL {

        @ParameterizedTest(name = "isCaseTypeUsingTTL false when list of CaseFieldDefinitions is null or empty: {0}")
        @NullAndEmptySource
        void isCaseTypeUsingTTL_falseWhenCaseFieldDefinitionsNullOrEmpty(List<CaseFieldDefinition> caseFields) {

            // GIVEN
            var caseTypeDefinition = new CaseTypeDefinition();
            caseTypeDefinition.setCaseFieldDefinitions(caseFields);

            // WHEN
            var output = timeToLiveService.isCaseTypeUsingTTL(caseTypeDefinition);

            // THEN
            assertFalse(output);
        }

        @Test
        void isCaseTypeUsingTTL_falseWhenNoTtlFieldDefinition() {

            // GIVEN
            var caseTypeDefinition = createCaseTypeDefinitionWithoutTTL();

            // WHEN
            var output = timeToLiveService.isCaseTypeUsingTTL(caseTypeDefinition);

            // THEN
            assertFalse(output);
        }

        @Test
        void isCaseTypeUsingTTL_trueWhenTtlFieldDefinition() {

            // GIVEN
            var caseTypeDefinition = createCaseTypeDefinitionWithTTL();

            // WHEN
            var output = timeToLiveService.isCaseTypeUsingTTL(caseTypeDefinition);

            // THEN
            assertTrue(output);
        }

        @SuppressWarnings("ConstantConditions") // NB: testing @NonNull annotation is present and active
        @Test
        void isCaseTypeUsingTTL_throwsNullPointerWhenDefinitionIsNull() {

            // WHEN / THEN
            assertThrows(NullPointerException.class, () ->
                timeToLiveService.isCaseTypeUsingTTL(null)
            );
        }

    }


    @Nested
    @DisplayName("updateCaseDataClassificationWithTTL")
    class UpdateCaseDataClassificationWithTTL {

        @Captor
        private ArgumentCaptor<Map<String, JsonNode>> caseDataCaptor;

        private final Map<String, JsonNode> caseDataClassification = new HashMap<>();

        private final JsonNode ttlDataClassification = objectMapper.valueToTree("{\"Ttl_Field\": \"PUBLIC\"}");

        @BeforeEach
        void setUp() throws JsonProcessingException {
            caseDataClassification.put("key", objectMapper.readTree("{\"Value\": \"PUBLIC\"}"));
        }

        @Test
        void updateCaseDataClassificationWithTTL_unchangedWhenCaseTypeNotConfigureToUseTtlField() {

            // GIVEN
            var caseTypeDefinition = createCaseTypeDefinitionWithoutTTL();
            var caseEventDefinition = new CaseEventDefinition();
            caseEventDefinition.setTtlIncrement(TTL_INCREMENT);

            // WHEN
            var output = timeToLiveService.updateCaseDataClassificationWithTTL(
                addDaysToSystemTTL(caseData, 1), caseDataClassification, caseEventDefinition, caseTypeDefinition
            );

            // THEN
            assertEquals(caseDataClassification, output);

        }

        @Test
        void updateCaseDataClassificationWithTTL_unchangedWhenNoTTLIncrementSet() {

            // GIVEN
            var caseTypeDefinition = createCaseTypeDefinitionWithTTL();
            var caseEventDefinition = new CaseEventDefinition();
            caseEventDefinition.setTtlIncrement(null);

            // WHEN
            var output = timeToLiveService.updateCaseDataClassificationWithTTL(
                addDaysToSystemTTL(caseData, 1), caseDataClassification, caseEventDefinition, caseTypeDefinition
            );

            // THEN
            assertEquals(caseDataClassification, output);

        }

        @ParameterizedTest(
            name = "updateCaseDataClassificationWithTTL returns unchanged value when case data is null or empty: {0}"
        )
        @NullAndEmptySource
        void updateCaseDataClassificationWithTTL_unchangedWhenCaseDataIsNullOrEmpty(Map<String, JsonNode> data) {

            // GIVEN
            var caseTypeDefinition = createCaseTypeDefinitionWithTTL();
            var caseEventDefinition = new CaseEventDefinition();
            caseEventDefinition.setTtlIncrement(TTL_INCREMENT);

            // WHEN
            var output = timeToLiveService.updateCaseDataClassificationWithTTL(
                data, caseDataClassification, caseEventDefinition, caseTypeDefinition
            );

            // THEN
            assertEquals(caseDataClassification, output);
        }

        @Test
        void updateCaseDataClassificationWithTTL_unchangedWhenCaseDataMissingTtlField() {

            // GIVEN
            var caseTypeDefinition = createCaseTypeDefinitionWithTTL();
            var caseEventDefinition = new CaseEventDefinition();
            caseEventDefinition.setTtlIncrement(TTL_INCREMENT);

            // WHEN
            var output = timeToLiveService.updateCaseDataClassificationWithTTL(
                caseData, caseDataClassification, caseEventDefinition, caseTypeDefinition
            );

            // THEN
            assertEquals(caseDataClassification, output);
        }

        @ParameterizedTest(
            name = "updateCaseDataClassificationWithTTL returns data classification when supplied Null OR Empty: {0}"
        )
        @NullAndEmptySource
        void updateCaseDataClassificationWithTTL_returnsDataClassificationWhenNullOrEmpty(
            Map<String, JsonNode> dataClassification) {

            // GIVEN
            var caseTypeDefinition = createCaseTypeDefinitionWithTTL();
            var caseEventDefinition = new CaseEventDefinition();
            caseEventDefinition.setTtlIncrement(TTL_INCREMENT);

            mockGetDefaultSecurityClassificationsResponse();

            var data = addDaysToSystemTTL(caseData, 1);

            // WHEN
            var output = timeToLiveService.updateCaseDataClassificationWithTTL(
                data, dataClassification, caseEventDefinition, caseTypeDefinition
            );

            // THEN
            verifyGetDefaultSecurityClassificationsCall(data, caseTypeDefinition);
            verifyResponseContainsTtl(output);
        }

        @Test
        void updateCaseDataClassificationWithTTL_returnsDataClassificationPopulated() {

            // GIVEN
            var caseTypeDefinition = createCaseTypeDefinitionWithTTL();
            var caseEventDefinition = new CaseEventDefinition();
            caseEventDefinition.setTtlIncrement(TTL_INCREMENT);

            mockGetDefaultSecurityClassificationsResponse();

            var data = addDaysToSystemTTL(caseData, 1);

            // WHEN
            var output = timeToLiveService.updateCaseDataClassificationWithTTL(
                data, caseDataClassification, caseEventDefinition, caseTypeDefinition
            );

            // THEN
            verifyGetDefaultSecurityClassificationsCall(data, caseTypeDefinition);
            verifyResponseContainsTtl(output);

            // verify other classification values preserved
            var outputWithoutTtl = new HashMap<>(output);
            outputWithoutTtl.remove(TTL_CASE_FIELD_ID);
            assertEquals(caseDataClassification, outputWithoutTtl);
        }

        private void mockGetDefaultSecurityClassificationsResponse() {
            Map<String, JsonNode> dataClassification = new HashMap<>();
            dataClassification.put(TTL_CASE_FIELD_ID, ttlDataClassification);

            doReturn(dataClassification).when(caseDataService).getDefaultSecurityClassifications(
                any(), any(), any()
            );
        }

        private void verifyResponseContainsTtl(Map<String, JsonNode> response) {
            assertTrue(response.containsKey(TTL_CASE_FIELD_ID));
            assertEquals(ttlDataClassification, response.get(TTL_CASE_FIELD_ID));
        }

        private void verifyGetDefaultSecurityClassificationsCall(Map<String, JsonNode> data,
                                                                 CaseTypeDefinition caseTypeDefinition) {

            verify(caseDataService, times(1)).getDefaultSecurityClassifications(
                eq(caseTypeDefinition),
                caseDataCaptor.capture(),
                any()
            );
            // must have TTL value
            assertEquals(data.get(TTL_CASE_FIELD_ID), caseDataCaptor.getValue().get(TTL_CASE_FIELD_ID));
        }
    }

    @Nested
    @DisplayName("updateCaseDetailsWithTTL")
    class UpdateCaseDetailsWithTTL {

        @Test
        void updateCaseDetailsWithTTL_CaseTypeNotConfigureToUseTtlField_unchanged() {

            // GIVEN
            var caseTypeDefinition = createCaseTypeDefinitionWithoutTTL();
            var caseEventDefinition = new CaseEventDefinition();

            // NB: the following should be ignored as CaseType check takes preference
            caseEventDefinition.setTtlIncrement(TTL_INCREMENT);
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree("some random value to  preserve"));

            // WHEN
            var output = timeToLiveService.updateCaseDetailsWithTTL(caseData, caseEventDefinition, caseTypeDefinition);

            // THEN
            assertEquals(caseData, output);
        }

        @Test
        void updateCaseDetailsWithTTL_NullCaseDataNoTTLIncrementSet_unchanged() {

            // GIVEN
            var caseTypeDefinition = createCaseTypeDefinitionWithTTL();
            var caseEventDefinition = new CaseEventDefinition();
            caseEventDefinition.setTtlIncrement(null);

            // WHEN
            var output = timeToLiveService.updateCaseDetailsWithTTL(null, caseEventDefinition, caseTypeDefinition);

            // THEN
            assertNull(output);
        }

        @Test
        void updateCaseDetailsWithTTL_NullCaseDataTTLIncrementSet_systemTtlSet() {

            // GIVEN
            var caseTypeDefinition = createCaseTypeDefinitionWithTTL();
            var caseEventDefinition = new CaseEventDefinition();
            caseEventDefinition.setTtlIncrement(TTL_INCREMENT);

            Map<String, JsonNode> expectedCaseData = addDaysToSystemTTL(new HashMap<>(), TTL_INCREMENT);

            // WHEN
            var output = timeToLiveService.updateCaseDetailsWithTTL(null, caseEventDefinition, caseTypeDefinition);

            // THEN
            assertEquals(expectedCaseData, output);
        }

        @Test
        void updateCaseDetailsWithTTL_NoTTLCaseFieldPresentNoTTLIncrementSet_unchanged() {

            // GIVEN
            var caseTypeDefinition = createCaseTypeDefinitionWithTTL();
            var caseEventDefinition = new CaseEventDefinition();
            caseEventDefinition.setTtlIncrement(null);

            // WHEN
            var output = timeToLiveService.updateCaseDetailsWithTTL(caseData, caseEventDefinition, caseTypeDefinition);

            // THEN
            assertEquals(caseData, output);
        }

        @Test
        void updateCaseDetailsWithTTL_NoTTLCaseFieldPresentTTLIncrementSet_systemTtlSet() {

            // GIVEN
            var caseTypeDefinition = createCaseTypeDefinitionWithTTL();
            var caseEventDefinition = new CaseEventDefinition();
            caseEventDefinition.setTtlIncrement(TTL_INCREMENT);

            Map<String, JsonNode> expectedCaseData = addDaysToSystemTTL(caseData, TTL_INCREMENT);

            // WHEN
            var output = timeToLiveService.updateCaseDetailsWithTTL(caseData, caseEventDefinition, caseTypeDefinition);

            // THEN
            assertEquals(expectedCaseData, output);
        }

        @Test
        void updateCaseDetailsWithTTL_TTLCaseFieldPresentNoTTLIncrementSet_unchanged() {

            // GIVEN
            var caseTypeDefinition = createCaseTypeDefinitionWithTTL();
            var caseEventDefinition = new CaseEventDefinition();
            caseEventDefinition.setTtlIncrement(null);

            TTL ttl = TTL.builder().systemTTL(LocalDate.now()).build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            // WHEN
            var output = timeToLiveService.updateCaseDetailsWithTTL(caseData, caseEventDefinition, caseTypeDefinition);

            // THEN
            assertEquals(caseData, output);
        }

        @Test
        void updateCaseDetailsWithTTL_TTLCaseFieldPresentTTLIncrementSet_systemTtlSet() {

            // GIVEN
            var caseTypeDefinition = createCaseTypeDefinitionWithTTL();
            var caseEventDefinition = new CaseEventDefinition();
            caseEventDefinition.setTtlIncrement(TTL_INCREMENT);

            TTL ttl = TTL.builder().systemTTL(LocalDate.now()).build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            Map<String, JsonNode> expectedCaseData = addDaysToSystemTTL(caseData, TTL_INCREMENT);

            // WHEN
            var output = timeToLiveService.updateCaseDetailsWithTTL(caseData, caseEventDefinition, caseTypeDefinition);

            // THEN
            assertEquals(expectedCaseData, output);
        }

        @Test
        void updateCaseDetailsWithTTL_ThrowsValidationExceptionWhenJsonParsingFails() throws IOException {

            // GIVEN
            var caseTypeDefinition = createCaseTypeDefinitionWithTTL();
            var caseEventDefinition = new CaseEventDefinition();
            caseEventDefinition.setTtlIncrement(TTL_INCREMENT);

            TTL ttl = TTL.builder().systemTTL(LocalDate.now()).build();
            String ttlNodeAsString = objectMapper.valueToTree(ttl).toString();

            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));
            caseEventDefinition.setTtlIncrement(TTL_INCREMENT);

            doThrow(JsonProcessingException.class).when(objectMapper).readValue(ttlNodeAsString, TTL.class);

            // WHEN / THEN
            Exception exception = assertThrows(ValidationException.class, () ->
                timeToLiveService.updateCaseDetailsWithTTL(caseData, caseEventDefinition, caseTypeDefinition)
            );

            assertEquals(TimeToLiveService.FAILED_TO_READ_TTL_FROM_CASE_DATA, exception.getMessage());
        }
    }


    @Nested
    @DisplayName("verifyTTLContentNotChangedByCallback")
    class VerifyTTLContentNotChangedByCallback {

        @Test
        void verifyTTLContentNotChangedByCallback_WhenCallbackCaseDataIsNull() {
            TTL ttl = TTL.builder().systemTTL(LocalDate.now()).build();

            Map<String, JsonNode> beforeCaseData = new HashMap<>(caseData);
            beforeCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            assertDoesNotThrow(() -> timeToLiveService.verifyTTLContentNotChangedByCallback(beforeCaseData, null));
        }

        @Test
        void verifyTTLContentNotChangedByCallback_WhenBeforeCaseDataIsNull() {
            TTL ttl = TTL.builder().systemTTL(LocalDate.now()).build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            assertDoesNotThrow(() -> timeToLiveService.verifyTTLContentNotChangedByCallback(null, caseData));
        }

        @Test
        void verifyTTLContentNotChangedByCallback_WhenTTLMissingFromCallbackResponseCaseData_CheckRepopulate() {
            TTL ttl = TTL.builder().systemTTL(LocalDate.now()).build();

            Map<String, JsonNode> beforeCaseData = new HashMap<>(caseData);
            // NB: only added to beforeCaseData
            beforeCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            assertDoesNotThrow(() -> timeToLiveService.verifyTTLContentNotChangedByCallback(beforeCaseData, caseData));
            // verify missing callback TTL repopulated from beforeCaseData
            assertEquals(beforeCaseData.get(TTL_CASE_FIELD_ID), caseData.get(TTL_CASE_FIELD_ID));
        }

        @Test
        void verifyTTLContentNotChangedByCallback_ThrowsExceptionWhenTTLSetToNullInCallbackResponseCaseData() {
            TTL ttl = TTL.builder().systemTTL(LocalDate.now()).build();

            Map<String, JsonNode> beforeCaseData = new HashMap<>(caseData);
            beforeCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));
            caseData.put(TTL.TTL_CASE_FIELD_ID, null);

            Exception exception = assertThrows(BadRequestException.class, () ->
                timeToLiveService.verifyTTLContentNotChangedByCallback(beforeCaseData, caseData)
            );
            assertEquals(TimeToLiveService.TIME_TO_LIVE_MODIFIED_ERROR_MESSAGE, exception.getMessage());
        }

        @Test
        void verifyTTLContentNotChangedByCallback_WhenTTLSetToNullInBothCaseData() {
            Map<String, JsonNode> beforeCaseData = new HashMap<>(caseData);
            beforeCaseData.put(TTL.TTL_CASE_FIELD_ID, null);
            caseData.put(TTL.TTL_CASE_FIELD_ID, null);

            assertDoesNotThrow(() -> timeToLiveService.verifyTTLContentNotChangedByCallback(beforeCaseData, caseData));
        }

        @Test
        void verifyTTLContentNotChangedByCallback_ThrowsExceptionWhenTTLAddedInCallbackResponseCaseData() {
            TTL ttl = TTL.builder().systemTTL(LocalDate.now()).build();

            Map<String, JsonNode> beforeCaseData = new HashMap<>(caseData);
            // NB: only added to callbackResponseCaseData
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            Exception exception = assertThrows(BadRequestException.class, () ->
                timeToLiveService.verifyTTLContentNotChangedByCallback(beforeCaseData, caseData)
            );
            assertEquals(TimeToLiveService.TIME_TO_LIVE_MODIFIED_ERROR_MESSAGE, exception.getMessage());
        }

        @Test
        void verifyTTLContentNotChangedByCallback_NoTTLInCaseData() {
            Map<String, JsonNode> beforeCaseData = new HashMap<>(caseData);

            assertDoesNotThrow(() -> timeToLiveService.verifyTTLContentNotChangedByCallback(beforeCaseData, caseData));
        }

        @Test
        void verifyTTLContentNotChangedByCallback_TTLValuesUnchanged() {
            TTL ttl = TTL.builder().systemTTL(LocalDate.now()).build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            Map<String, JsonNode> beforeCaseData = new HashMap<>(caseData);
            beforeCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            assertDoesNotThrow(() -> timeToLiveService.verifyTTLContentNotChangedByCallback(beforeCaseData, caseData));
        }

        @Test
        void verifyTTLContentNotChangedByCallback_ThrowsExceptionWhenTTLValuesChanged() {
            TTL ttl = TTL.builder().systemTTL(LocalDate.now()).build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            Map<String, JsonNode> beforeCaseData = addDaysToSystemTTL(caseData, TTL_INCREMENT);

            Exception exception = assertThrows(BadRequestException.class, () ->
                timeToLiveService.verifyTTLContentNotChangedByCallback(beforeCaseData, caseData)
            );
            assertEquals(TimeToLiveService.TIME_TO_LIVE_MODIFIED_ERROR_MESSAGE, exception.getMessage());
        }

        @Test
        void verifyTTLContentNotChangedByCallback_ThrowsExceptionWhenJsonParsingFails() throws IOException {
            TTL ttl = TTL.builder().systemTTL(LocalDate.now()).build();
            String ttlNodeAsString = objectMapper.valueToTree(ttl).toString();

            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            doThrow(JsonProcessingException.class).when(objectMapper).readValue(ttlNodeAsString, TTL.class);

            Exception exception = assertThrows(ValidationException.class, () ->
                timeToLiveService.verifyTTLContentNotChangedByCallback(caseData, caseData)
            );
            assertEquals(TimeToLiveService.FAILED_TO_READ_TTL_FROM_CASE_DATA, exception.getMessage());
        }
    }


    @Nested
    @DisplayName("validateTTLChangeAgainstTTLGuard")
    class ValidateTTLChangeAgainstTTLGuard {

        @ParameterizedTest(
            name = "validateTTLChangeAgainstTTLGuard generates no error if supplied case data is null or empty: {0}"
        )
        @NullAndEmptySource
        void verifyTTLSuspension_NoError_whenCaseDataNullOrEmpty(Map<String, JsonNode> data) {
            TTL updatedTtl = TTL.builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.YES)
                .overrideTTL(LocalDate.now())
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            assertDoesNotThrow(() -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, data));
        }

        @ParameterizedTest(
            name = "validateTTLChangeAgainstTTLGuard generates no error if supplied updated case data is null or empty:"
                + " {0}"
        )
        @NullAndEmptySource
        void verifyTTLSuspension_NoError_whenUpdatedCaseDataNullOrEmpty(Map<String, JsonNode> updatedData) {
            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.YES)
                .overrideTTL(LocalDate.now())
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            assertDoesNotThrow(() -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedData, caseData));
        }

        @Test
        void verifyTTLSuspension_NoError_whenCaseDataTtlIsNull() {
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(null));

            TTL updatedTtl = TTL.builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.YES)
                .overrideTTL(LocalDate.now())
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            assertDoesNotThrow(() -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, caseData));
        }

        @Test
        void verifyTTLSuspension_NoError_whenUpdatedCaseDataTtlIsNull() {
            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.YES)
                .overrideTTL(LocalDate.now())
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(null));

            assertDoesNotThrow(() -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, caseData));
        }

        @Test
        void verifyTTLSuspensionNotChanged_Yes() {
            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.YES)
                .overrideTTL(LocalDate.now())
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            TTL updatedTtl = TTL.builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.YES)
                .overrideTTL(LocalDate.now())
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            assertDoesNotThrow(() -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, caseData));
        }

        @Test
        void verifyTTLSuspensionNotChanged_No() {
            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.NO)
                .overrideTTL(LocalDate.now())
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            TTL updatedTtl = TTL.builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.NO)
                .overrideTTL(LocalDate.now())
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            assertDoesNotThrow(() -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, caseData));
        }

        @ParameterizedTest(
            name = "validateTTLChangeAgainstTTLGuard generates no error if TTL.suspended changed: {0} -> Yes"
        )
        @MethodSource(TTL_SUSPENDED_VALUES_FOR_IS_SUSPENDED_FALSE)
        void verifyTTLSuspensionChanged_NoToYes_alternativeNos(String ttlSuspended) {
            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(ttlSuspended)
                .overrideTTL(LocalDate.now())
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            TTL updatedTtl = TTL.builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.YES) // i.e. false value is changing to true value
                .overrideTTL(LocalDate.now())
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            assertDoesNotThrow(() -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, caseData));
        }

        @ParameterizedTest(
            name = "validateTTLChangeAgainstTTLGuard generates no error if TTL.suspended changed: No -> {0}"
        )
        @MethodSource(TTL_SUSPENDED_VALUES_FOR_IS_SUSPENDED_TRUE)
        void verifyTTLSuspensionChanged_NoToYes_alternativeYeses(String updatedTtlSuspended) {
            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.NO) // i.e. false value will be changed to true value
                .overrideTTL(LocalDate.now())
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            TTL updatedTtl = TTL.builder()
                .systemTTL(LocalDate.now())
                .suspended(updatedTtlSuspended)
                .overrideTTL(LocalDate.now())
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            assertDoesNotThrow(() -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, caseData));
        }

        @ParameterizedTest(
            name = "validateTTLChangeAgainstTTLGuard generates error if TTL.suspended changed: Yes -> {0}"
        )
        @MethodSource(TTL_SUSPENDED_VALUES_FOR_IS_SUSPENDED_FALSE)
        void verifyTTLSuspensionChanged_YesToNo_alternativeNos(String updatedTtlSuspended) {
            when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);

            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.YES) // i.e. true value will be changed to false value
                .overrideTTL(LocalDate.now())
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            TTL updatedTtl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(updatedTtlSuspended)
                .overrideTTL(LocalDate.now())
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            final ValidationException exception = assertThrows(ValidationException.class,
                () -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, caseData));
            assertThat(exception.getMessage(),
                startsWith(TIME_TO_LIVE_GUARD_ERROR_MESSAGE));
        }

        @ParameterizedTest(
            name = "validateTTLChangeAgainstTTLGuard generates error if TTL.suspended changed: {0} -> No"
        )
        @MethodSource(TTL_SUSPENDED_VALUES_FOR_IS_SUSPENDED_TRUE)
        void verifyTTLSuspensionChanged_YesToNo_alternativeYeses(String ttlSuspended) {
            when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);

            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(ttlSuspended)
                .overrideTTL(LocalDate.now())
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            TTL updatedTtl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.NO)  // i.e. true value is changing to false value
                .overrideTTL(LocalDate.now())
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            final ValidationException exception = assertThrows(ValidationException.class,
                () -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, caseData));
            assertThat(exception.getMessage(),
                startsWith(TIME_TO_LIVE_GUARD_ERROR_MESSAGE));
        }

        @Test
        void verifyTTLOverrideChangedWithValidValue() {
            when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);
            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.NO)
                .overrideTTL(LocalDate.now())
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            TTL updatedTtl = TTL.builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.NO)
                .overrideTTL(FAR_FUTURE_DATE)
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            assertDoesNotThrow(() -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, caseData));
        }

        @Test
        void verifyTTLOverrideChangedWithInValidValue() {
            when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);
            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.NO)
                .overrideTTL(LocalDate.now())
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            TTL updatedTtl = TTL.builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.NO)
                .overrideTTL(LocalDate.now().plusDays(TTL_GUARD - 1))
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            final ValidationException exception = assertThrows(ValidationException.class,
                () -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, caseData));
            assertThat(exception.getMessage(),
                startsWith(TIME_TO_LIVE_GUARD_ERROR_MESSAGE));
        }

        @Test
        void verifyTTLOverrideRemovedWithAndSystemTTLValue() {
            when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);
            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.NO)
                .overrideTTL(LocalDate.now())
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            TTL updatedTtl = TTL.builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.YES)
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            assertDoesNotThrow(() -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, caseData));
        }

        @Test
        void verifyTTLOverrideRemovedWithInValidSystemTTLValue() {
            when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);

            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.NO)
                .overrideTTL(LocalDate.now())
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            TTL updatedTtl = TTL.builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.NO)
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            final ValidationException exception = assertThrows(ValidationException.class,
                () -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, caseData));
            assertThat(exception.getMessage(),
                startsWith(TIME_TO_LIVE_GUARD_ERROR_MESSAGE));
        }

        @Test
        void verifyTTLOverrideAddedWithValidValue() {
            when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);
            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.NO)
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            TTL updatedTtl = TTL.builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.NO)
                .overrideTTL(FAR_FUTURE_DATE)
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            assertDoesNotThrow(() -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, caseData));
        }

        @Test
        void verifyTTLOverrideAddedWithInValidValue() {
            when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);
            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.NO)
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            TTL updatedTtl = TTL.builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.NO)
                .overrideTTL(LocalDate.now().minusDays(2L))
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            final ValidationException exception = assertThrows(ValidationException.class,
                () -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, caseData));
            assertThat(exception.getMessage(),
                startsWith(TIME_TO_LIVE_GUARD_ERROR_MESSAGE));
        }

        @Test
        void verifyTTLOverrideAddedWithInValidValueAndSuspended() {
            when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);
            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.NO)
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            TTL updatedTtl = TTL.builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.YES)
                .overrideTTL(LocalDate.now().minusDays(2L))
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            assertDoesNotThrow(() -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, caseData));
        }

        @Test
        void verifyTTLGuardSystemTtlNotBeforeTtlGuard() {
            when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);

            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.YES)
                .overrideTTL(null)
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            TTL updatedTtl = TTL
                .builder()
                .systemTTL(LocalDate.now().plusDays(TTL_GUARD))
                .suspended(TTL.NO)
                .overrideTTL(null)
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            assertDoesNotThrow(() -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, caseData));
        }

        @Test
        void verifyTTLGuardSystemTtlIsBeforeTtlGuard() {

            when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);

            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.YES)
                .overrideTTL(null)
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            TTL updatedTtl = TTL
                .builder()
                .systemTTL(LocalDate.now().plusDays(TTL_GUARD - 1))
                .suspended(TTL.NO)
                .overrideTTL(null)
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            final ValidationException exception = assertThrows(ValidationException.class,
                () -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, caseData));
            assertThat(exception.getMessage(),
                startsWith(TIME_TO_LIVE_GUARD_ERROR_MESSAGE));
        }

        @Test
        void verifyTTLGuardOverrideTtlNotBeforeTtlGuard() {
            when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);

            TTL ttl = TTL
                .builder()
                .systemTTL(FAR_FUTURE_DATE)
                .suspended(TTL.YES)
                .overrideTTL(LocalDate.now())
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            TTL updatedTtl = TTL
                .builder()
                .systemTTL(FAR_FUTURE_DATE)
                .suspended(TTL.NO)
                .overrideTTL(LocalDate.now().plusDays(TTL_GUARD))
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            assertDoesNotThrow(() -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, caseData));
        }

        @Test
        void verifyTTLGuardOverrideTtlIsBeforeTtlGuard() {

            when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);

            TTL ttl = TTL
                .builder()
                .systemTTL(FAR_FUTURE_DATE)
                .suspended(TTL.YES)
                .overrideTTL(LocalDate.now())
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            TTL updatedTtl = TTL
                .builder()
                .systemTTL(FAR_FUTURE_DATE)
                .suspended(TTL.NO)
                .overrideTTL(LocalDate.now().plusDays(TTL_GUARD - 1))
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            final ValidationException exception = assertThrows(ValidationException.class,
                () -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, caseData));
            assertThat(exception.getMessage(),
                startsWith(TIME_TO_LIVE_GUARD_ERROR_MESSAGE));
        }

        @ParameterizedTest(
            name = "validateTTLChangeAgainstTTLGuard generates no error if TTL added with valid override: "
                + "current case data {0}"
        )
        @MethodSource(TTL_MISSING_OR_NULL)
        void verifyTTLAdded_OverrideWithValidValue(Map<String, JsonNode> currentCaseData) {
            when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);

            TTL updatedTtl = TTL.builder()
                .systemTTL(LocalDate.now())
                .suspended(null)
                .overrideTTL(FAR_FUTURE_DATE)
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            assertDoesNotThrow(
                () -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, currentCaseData)
            );
        }

        @ParameterizedTest(
            name = "validateTTLChangeAgainstTTLGuard generates errors if TTL added with invalid override: "
                + "current case data {0}"
        )
        @MethodSource(TTL_MISSING_OR_NULL)
        void verifyTTLAdded_OverrideAddedWithInvalidValue(Map<String, JsonNode> currentCaseData) {
            when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);
            caseData.put(TTL.TTL_CASE_FIELD_ID, null);

            TTL updatedTtl = TTL.builder()
                .systemTTL(FAR_FUTURE_DATE)
                .suspended(TTL.NO)
                .overrideTTL(LocalDate.now().minusDays(2L))
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            final ValidationException exception = assertThrows(ValidationException.class,
                () -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, currentCaseData));
            assertThat(exception.getMessage(),
                startsWith(TIME_TO_LIVE_GUARD_ERROR_MESSAGE));
        }

        @ParameterizedTest(
            name = "validateTTLChangeAgainstTTLGuard generates no error if TTL added with invalid override but"
                + " suspended Yes: current case data {0}"
        )
        @MethodSource(TTL_MISSING_OR_NULL)
        void verifyTTLAdded_OverrideAddedWithInvalidValueButSuspendedYes(Map<String, JsonNode> currentCaseData) {
            when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);

            TTL updatedTtl = TTL.builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.YES)
                .overrideTTL(LocalDate.now().minusDays(2L))
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            assertDoesNotThrow(
                () -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, currentCaseData)
            );
        }

        @ParameterizedTest(
            name = "validateTTLChangeAgainstTTLGuard generates no error if TTL added with null override and suspended: "
                + "current case data {0}"
        )
        @MethodSource(TTL_MISSING_OR_NULL)
        void verifyTTLAdded_NoOverrideOrSuspended(Map<String, JsonNode> currentCaseData) {
            TTL updatedTtl = TTL.builder()
                .systemTTL(LocalDate.now())
                .suspended(null)
                .overrideTTL(null)
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            assertDoesNotThrow(
                () -> timeToLiveService.validateTTLChangeAgainstTTLGuard(updatedCaseData, currentCaseData)
            );
        }

    }


    @Nested
    @DisplayName("getUpdatedResolvedTTL")
    class GetUpdatedResolvedTTL {

        @Test
        void verifyResolvedTtlSetToNullWhenTtlSuspended() {
            TTL ttl = TTL.builder().systemTTL(LocalDate.now()).overrideTTL(LocalDate.now()).suspended(TTL.YES).build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            LocalDate updatedResolvedTTL = timeToLiveService.getUpdatedResolvedTTL(caseData);

            assertNull(updatedResolvedTTL);
        }

        @Test
        void verifyResolvedTtlSetToNullWhenTtlNotSuspended() {
            TTL ttl = TTL.builder().systemTTL(null).overrideTTL(null).suspended(TTL.NO).build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            LocalDate updatedResolvedTTL = timeToLiveService.getUpdatedResolvedTTL(caseData);

            assertNull(updatedResolvedTTL);
        }

        @Test
        void verifyResolvedTTLSetToOverrideTTL() {
            TTL ttl = TTL.builder().systemTTL(LocalDate.now()).overrideTTL(LocalDate.now()).suspended(TTL.NO).build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            LocalDate updatedResolvedTTL = timeToLiveService.getUpdatedResolvedTTL(caseData);

            assertEquals(ttl.getOverrideTTL(), updatedResolvedTTL);
        }

        @Test
        void verifyResolvedTTLSetToSystemTTL() {
            TTL ttl = TTL.builder().systemTTL(LocalDate.now()).overrideTTL(null).suspended(TTL.NO).build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            LocalDate updatedResolvedTTL = timeToLiveService.getUpdatedResolvedTTL(caseData);

            assertEquals(ttl.getSystemTTL(), updatedResolvedTTL);
        }

        @ParameterizedTest(
            name = "Verify ResolvedTtl set to null when supplied data is null or empty: {0}"
        )
        @NullAndEmptySource
        void verifyResolvedTtlSetToNullWhenCaseDataNullOrEmpty(Map<String, JsonNode> data) {

            LocalDate updatedResolvedTTL = timeToLiveService.getUpdatedResolvedTTL(data);

            assertNull(updatedResolvedTTL);
        }

    }


    private Map<String, JsonNode> addDaysToSystemTTL(Map<String, JsonNode> data, Integer numOfDays) {
        Map<String, JsonNode> clonedData = new HashMap<>(data);
        LocalDate systemTtl = LocalDate.now().plusDays(numOfDays);

        if (data.containsKey(TTL_CASE_FIELD_ID)) {
            var expectedTtlJson = data.get(TTL_CASE_FIELD_ID).deepCopy();
            ((ObjectNode)expectedTtlJson).put("SystemTTL", String.valueOf(systemTtl));
            clonedData.put(TTL.TTL_CASE_FIELD_ID, expectedTtlJson);
        } else {
            TTL expectedTtl = TTL.builder()
                .systemTTL(systemTtl)
                .suspended(NO) // default
                .build();
            clonedData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(expectedTtl));
        }

        return clonedData;
    }

    private CaseTypeDefinition createCaseTypeDefinitionWithoutTTL() {

        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        caseFieldDefinition.setId("myTextField");
        caseFieldDefinition.setFieldTypeDefinition(createFieldTypeDefinition(TEXT));

        List<CaseFieldDefinition> caseFields = new ArrayList<>();
        caseFields.add(caseFieldDefinition);

        var newCaseTypeDefinition = new CaseTypeDefinition();
        newCaseTypeDefinition.setCaseFieldDefinitions(caseFields);

        return newCaseTypeDefinition;
    }

    private CaseTypeDefinition createCaseTypeDefinitionWithTTL() {
        var newCaseTypeDefinition = createCaseTypeDefinitionWithoutTTL();

        CaseFieldDefinition ttlFieldDefinition = new CaseFieldDefinition();
        ttlFieldDefinition.setId(TTL_CASE_FIELD_ID);
        ttlFieldDefinition.setFieldTypeDefinition(createFieldTypeDefinition(TTL_CASE_FIELD_ID));

        newCaseTypeDefinition.getCaseFieldDefinitions().add(ttlFieldDefinition);
        return newCaseTypeDefinition;
    }

    private FieldTypeDefinition createFieldTypeDefinition(String type) {
        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        fieldTypeDefinition.setId(type);
        fieldTypeDefinition.setType(type);
        return fieldTypeDefinition;
    }

}
