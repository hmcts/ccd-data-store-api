package uk.gov.hmcts.ccd.domain.service.casedeletion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.config.JacksonObjectMapperConfig;
import uk.gov.hmcts.ccd.domain.model.casedeletion.TTL;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.casedeletion.TTLTest.TTL_SUSPENSION_NO_PARAMETERS;
import static uk.gov.hmcts.ccd.domain.model.casedeletion.TTLTest.TTL_SUSPENSION_YES_PARAMETERS;

@ExtendWith(MockitoExtension.class)
class TimeToLiveServiceTest {

    @InjectMocks
    private TimeToLiveService timeToLiveService;
    private CaseEventDefinition caseEventDefinition;
    private static final Integer TTL_INCREMENT = 10;
    private static final Integer TTL_GUARD = 365;
    private final Map<String, JsonNode> caseData = new HashMap<>();

    @Spy
    private ObjectMapper objectMapper = new JacksonObjectMapperConfig().defaultObjectMapper();

    @Mock
    private ApplicationParams applicationParams;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        caseData.put("key", objectMapper.readTree("{\"Value\": \"value\"}"));
        caseEventDefinition = new CaseEventDefinition();
    }

    @Test
    void updateCaseDetailsWithNullCaseData() {
        assertNull(timeToLiveService.updateCaseDetailsWithTTL(null, caseEventDefinition));
    }

    @Test
    void updateCaseDetailsWithTTLNoTtlIncrementSet() {
        assertEquals(caseData, timeToLiveService.updateCaseDetailsWithTTL(caseData, caseEventDefinition));
    }

    @Test
    void updateCaseDetailsTTLCaseFieldPresentNoTTLIncrementSet() {
        TTL ttl = TTL.builder().systemTTL(LocalDate.now()).build();
        caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));
        assertEquals(caseData, timeToLiveService.updateCaseDetailsWithTTL(caseData, caseEventDefinition));
    }

    @Test
    void updateCaseDetailsTTLCaseFieldPresentTTLIncrementSet() {
        TTL ttl = TTL.builder().systemTTL(LocalDate.now()).build();
        caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

        Map<String, JsonNode> expectedCaseData = addDaysToSystemTTL(caseData, TTL_INCREMENT);

        caseEventDefinition.setTtlIncrement(TTL_INCREMENT);

        assertEquals(expectedCaseData, timeToLiveService.updateCaseDetailsWithTTL(caseData, caseEventDefinition));
    }

    @Test
    void updateCaseDetailsThrowsValidationExceptionWhenJsonParsingFails() throws IOException {
        TTL ttl = TTL.builder().systemTTL(LocalDate.now()).build();
        String ttlNodeAsString = objectMapper.valueToTree(ttl).toString();

        caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));
        caseEventDefinition.setTtlIncrement(TTL_INCREMENT);

        doThrow(JsonProcessingException.class).when(objectMapper).readValue(ttlNodeAsString, TTL.class);

        Exception exception = assertThrows(ValidationException.class, () ->
            timeToLiveService.updateCaseDetailsWithTTL(caseData, caseEventDefinition)
        );

        assertEquals(TimeToLiveService.FAILED_TO_READ_TTL_FROM_CASE_DATA, exception.getMessage());
    }

    @Test
    void verifyTTLContentNotChangedNullCaseData() {
        assertDoesNotThrow(() -> timeToLiveService.verifyTTLContentNotChanged(null, null));
    }

    @Test
    void verifyTTLContentNotChangedNoTTLInCaseData() {
        Map<String, JsonNode> expectedCaseData = new HashMap<>(caseData);

        assertDoesNotThrow(() -> timeToLiveService.verifyTTLContentNotChanged(expectedCaseData, caseData));
    }

    @Test
    void verifyTTLContentNotChangedTTLValuesUnchanged() {
        TTL ttl = TTL.builder().systemTTL(LocalDate.now()).build();
        caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

        Map<String, JsonNode> expectedCaseData = new HashMap<>(caseData);
        expectedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

        assertDoesNotThrow(() -> timeToLiveService.verifyTTLContentNotChanged(expectedCaseData, caseData));
    }

    @Test
    void verifyTTLContentNotChangedTTLValuesChanged() {
        TTL ttl = TTL.builder().systemTTL(LocalDate.now()).build();
        caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

        Map<String, JsonNode> expectedCaseData = addDaysToSystemTTL(caseData, TTL_INCREMENT);

        Exception exception = assertThrows(BadRequestException.class, () ->
            timeToLiveService.verifyTTLContentNotChanged(expectedCaseData, caseData)
        );

        assertEquals(TimeToLiveService.TIME_TO_LIVE_MODIFIED_ERROR_MESSAGE, exception.getMessage());
    }

    @Nested
    @DisplayName("validateSuspensionChange")
    class ValidateSuspensionChange {

        @ParameterizedTest(
            name = "validateSuspensionChange generates no error if supplied case data is null or empty: {0}"
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

            assertDoesNotThrow(() -> timeToLiveService.validateSuspensionChange(updatedCaseData, data));
        }

        @ParameterizedTest(
            name = "validateSuspensionChange generates no error if supplied updated case data is null or empty: {0}"
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

            assertDoesNotThrow(() -> timeToLiveService.validateSuspensionChange(updatedData, caseData));
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

            assertDoesNotThrow(() -> timeToLiveService.validateSuspensionChange(updatedCaseData, caseData));
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

            assertDoesNotThrow(() -> timeToLiveService.validateSuspensionChange(updatedCaseData, caseData));
        }

        @ParameterizedTest(
            name = "validateSuspensionChange generates no error if TTL.suspended changed: {0} -> Yes"
        )
        @MethodSource(TTL_SUSPENSION_NO_PARAMETERS)
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
                .suspended(TTL.YES)
                .overrideTTL(LocalDate.now())
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            assertDoesNotThrow(() -> timeToLiveService.validateSuspensionChange(updatedCaseData, caseData));
        }

        @ParameterizedTest(
            name = "validateSuspensionChange generates no error if TTL.suspended changed: No -> {0}"
        )
        @MethodSource(TTL_SUSPENSION_YES_PARAMETERS)
        void verifyTTLSuspensionChanged_NoToYes_alternativeYeses(String updatedTtlSuspended) {
            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.NO)
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

            assertDoesNotThrow(() -> timeToLiveService.validateSuspensionChange(updatedCaseData, caseData));
        }

        @ParameterizedTest(
            name = "validateSuspensionChange generates error if TTL.suspended changed: Yes -> {0}"
        )
        @MethodSource(TTL_SUSPENSION_NO_PARAMETERS)
        void verifyTTLSuspensionChanged_YesToNo_alternativeNos(String updatedTtlSuspended) {
            when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);

            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.YES)
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
                () -> timeToLiveService.validateSuspensionChange(updatedCaseData, caseData));
            assertThat(exception.getMessage(),
                startsWith("Unsetting a suspension can only be allowed "
                    + "if the deletion will occur beyond the guard period."));
        }

        @ParameterizedTest(
            name = "validateSuspensionChange generates error if TTL.suspended changed: {0} -> No"
        )
        @MethodSource(TTL_SUSPENSION_YES_PARAMETERS)
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
                .suspended(TTL.NO)
                .overrideTTL(LocalDate.now())
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            final ValidationException exception = assertThrows(ValidationException.class,
                () -> timeToLiveService.validateSuspensionChange(updatedCaseData, caseData));
            assertThat(exception.getMessage(),
                startsWith("Unsetting a suspension can only be allowed "
                    + "if the deletion will occur beyond the guard period."));
        }

        @Test
        void verifyTTLGuardSystemTtlNotBeforeTtlGuard() {
            when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);

            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.YES)
                .overrideTTL(LocalDate.now())
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            TTL updatedTtl = TTL
                .builder()
                .systemTTL(LocalDate.now().plusDays(TTL_GUARD))
                .suspended(TTL.NO)
                .overrideTTL(LocalDate.now())
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            assertDoesNotThrow(() -> timeToLiveService.validateSuspensionChange(updatedCaseData, caseData));
        }

        @Test
        void verifyTTLGuardSystemTtlIsBeforeTtlGuard() {

            when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);

            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.YES)
                .overrideTTL(LocalDate.now())
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            TTL updatedTtl = TTL
                .builder()
                .systemTTL(LocalDate.now().plusDays(TTL_GUARD - 1))
                .suspended(TTL.NO)
                .overrideTTL(LocalDate.now())
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            final ValidationException exception = assertThrows(ValidationException.class,
                () -> timeToLiveService.validateSuspensionChange(updatedCaseData, caseData));
            assertThat(exception.getMessage(),
                startsWith("Unsetting a suspension can only be allowed "
                    + "if the deletion will occur beyond the guard period."));
        }

        @Test
        void verifyTTLGuardOverrideTtlNotBeforeTtlGuard() {
            when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);

            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.YES)
                .overrideTTL(LocalDate.now())
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            TTL updatedTtl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.NO)
                .overrideTTL(LocalDate.now().plusDays(TTL_GUARD))
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            assertDoesNotThrow(() -> timeToLiveService.validateSuspensionChange(updatedCaseData, caseData));
        }

        @Test
        void verifyTTLGuardOverrideTtlIsBeforeTtlGuard() {

            when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);

            TTL ttl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.YES)
                .overrideTTL(LocalDate.now())
                .build();
            caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));

            TTL updatedTtl = TTL
                .builder()
                .systemTTL(LocalDate.now())
                .suspended(TTL.NO)
                .overrideTTL(LocalDate.now().plusDays(TTL_GUARD - 1))
                .build();
            Map<String, JsonNode> updatedCaseData = new HashMap<>();
            updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(updatedTtl));

            final ValidationException exception = assertThrows(ValidationException.class,
                () -> timeToLiveService.validateSuspensionChange(updatedCaseData, caseData));
            assertThat(exception.getMessage(),
                startsWith("Unsetting a suspension can only be allowed "
                    + "if the deletion will occur beyond the guard period."));
        }

    }


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

    @Test
    void verifyTTLContentNotChangedThrowsValidationExceptionWhenJsonParsingFails() throws IOException {
        TTL ttl = TTL.builder().systemTTL(LocalDate.now()).build();
        String ttlNodeAsString = objectMapper.valueToTree(ttl).toString();

        caseData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(ttl));
        caseEventDefinition.setTtlIncrement(TTL_INCREMENT);

        doThrow(JsonProcessingException.class).when(objectMapper).readValue(ttlNodeAsString, TTL.class);

        Exception exception = assertThrows(ValidationException.class, () ->
            timeToLiveService.verifyTTLContentNotChanged(caseData, caseData)
        );

        assertEquals(TimeToLiveService.FAILED_TO_READ_TTL_FROM_CASE_DATA, exception.getMessage());
    }

    @SuppressWarnings("SameParameterValue")
    private Map<String, JsonNode> addDaysToSystemTTL(Map<String, JsonNode> data, Integer numOfDays) {
        Map<String, JsonNode> clonedData = new HashMap<>(data);
        TTL expectedTtl = TTL.builder()
            .systemTTL(LocalDate.now().plusDays(numOfDays))
            .build();
        clonedData.put(TTL.TTL_CASE_FIELD_ID, objectMapper.valueToTree(expectedTtl));
        return clonedData;
    }
}
