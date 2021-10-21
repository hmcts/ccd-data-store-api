package uk.gov.hmcts.ccd.domain.service.casedeletion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.config.JacksonObjectMapperConfig;
import uk.gov.hmcts.ccd.domain.model.casedeletion.TTL;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeToLiveServiceTest {

    private TimeToLiveService timeToLiveService;
    private CaseEventDefinition caseEventDefinition;
    private static final Integer TTL_INCREMENT = 10;
    private static final Integer TTL_GUARD = 365;
    private Map<String, JsonNode> caseData = new HashMap<>();
    private static final ObjectMapper OBJECT_MAPPER = new JacksonObjectMapperConfig().defaultObjectMapper();

    @Mock
    private ApplicationParams applicationParams;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        caseData.put("key", OBJECT_MAPPER.readTree("{\"Value\": \"value\"}"));
        timeToLiveService = new TimeToLiveService(OBJECT_MAPPER, applicationParams);
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
        caseData.put(TTL.TTL_CASE_FIELD_ID, OBJECT_MAPPER.valueToTree(ttl));
        assertEquals(caseData, timeToLiveService.updateCaseDetailsWithTTL(caseData, caseEventDefinition));
    }

    @Test
    void updateCaseDetailsTTLCaseFieldPresentTTLIncrementSet() {
        TTL ttl = TTL.builder().systemTTL(LocalDate.now()).build();
        caseData.put(TTL.TTL_CASE_FIELD_ID, OBJECT_MAPPER.valueToTree(ttl));

        Map<String, JsonNode> expectedCaseData = addDaysToSystemTTL(caseData, TTL_INCREMENT);

        caseEventDefinition.setTtlIncrement(TTL_INCREMENT);

        assertEquals(expectedCaseData, timeToLiveService.updateCaseDetailsWithTTL(caseData, caseEventDefinition));
    }

    @Test
    void verifyTTLContentNotChangedNullCaseData() {
        timeToLiveService.verifyTTLContentNotChanged(null, null);
    }

    @Test
    void verifyTTLContentNotChangedNoTTLInCaseData() {
        Map<String, JsonNode> expectedCaseData = new HashMap<>(caseData);



        final ServiceException exception = assertThrows(ServiceException.class,
            () -> timeToLiveService.verifyTTLContentNotChanged(expectedCaseData, caseData));
        Assert.assertThat(exception.getMessage(),
            startsWith("Unable to read TTL from case data"));
    }

    @Test
    void verifyTTLContentNotChangedTTLValuesUnchanged() {
        TTL ttl = TTL.builder().systemTTL(LocalDate.now()).build();
        caseData.put(TTL.TTL_CASE_FIELD_ID, OBJECT_MAPPER.valueToTree(ttl));

        Map<String, JsonNode> expectedCaseData = new HashMap<>(caseData);
        expectedCaseData.put(TTL.TTL_CASE_FIELD_ID, OBJECT_MAPPER.valueToTree(ttl));

        timeToLiveService.verifyTTLContentNotChanged(expectedCaseData, caseData);
    }

    @Test
    void verifyTTLContentNotChangedTTLValuesChanged() {
        TTL ttl = TTL.builder().systemTTL(LocalDate.now()).build();
        caseData.put(TTL.TTL_CASE_FIELD_ID, OBJECT_MAPPER.valueToTree(ttl));

        Map<String, JsonNode> expectedCaseData = addDaysToSystemTTL(caseData, TTL_INCREMENT);

        Exception exception = assertThrows(BadRequestException.class, () -> {
            timeToLiveService.verifyTTLContentNotChanged(expectedCaseData, caseData);
        });

        assertEquals(TimeToLiveService.TIME_TO_LIVE_MODIFIED_ERROR_MESSAGE, exception.getMessage());
    }


    @Test
    void verifyTTLSuspensionNotChanged() {
        TTL ttl = TTL
            .builder()
            .systemTTL(LocalDate.now())
            .suspended(TTL.NO)
            .overrideTTL(LocalDate.now())
            .build();
        caseData.put(TTL.TTL_CASE_FIELD_ID, OBJECT_MAPPER.valueToTree(ttl));

        TTL updatedTtl = TTL.builder()
            .systemTTL(LocalDate.now())
            .suspended(TTL.NO)
            .overrideTTL(LocalDate.now())
            .build();
        Map<String, JsonNode> updatedCaseData = new HashMap<>();
        updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, OBJECT_MAPPER.valueToTree(updatedTtl));

        assertDoesNotThrow(() -> timeToLiveService.validateSuspensionChange(updatedCaseData, caseData));
    }

    @Test
    void verifyTTLSuspensionChanged() {
        when(applicationParams.getTtlGuard()).thenReturn(TTL_GUARD);

        TTL ttl = TTL
            .builder()
            .systemTTL(LocalDate.now())
            .suspended(TTL.YES)
            .overrideTTL(LocalDate.now())
            .build();
        caseData.put(TTL.TTL_CASE_FIELD_ID, OBJECT_MAPPER.valueToTree(ttl));

        TTL updatedTtl = TTL
            .builder()
            .systemTTL(LocalDate.now())
            .suspended(TTL.NO)
            .overrideTTL(LocalDate.now())
            .build();
        Map<String, JsonNode> updatedCaseData = new HashMap<>();
        updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, OBJECT_MAPPER.valueToTree(updatedTtl));

        final ValidationException exception = assertThrows(ValidationException.class,
            () -> timeToLiveService.validateSuspensionChange(updatedCaseData, caseData));
        Assert.assertThat(exception.getMessage(),
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
        caseData.put(TTL.TTL_CASE_FIELD_ID, OBJECT_MAPPER.valueToTree(ttl));

        TTL updatedTtl = TTL
            .builder()
            .systemTTL(LocalDate.now().plusDays(TTL_GUARD))
            .suspended(TTL.NO)
            .overrideTTL(LocalDate.now())
            .build();
        Map<String, JsonNode> updatedCaseData = new HashMap<>();
        updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, OBJECT_MAPPER.valueToTree(updatedTtl));

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
        caseData.put(TTL.TTL_CASE_FIELD_ID, OBJECT_MAPPER.valueToTree(ttl));

        TTL updatedTtl = TTL
            .builder()
            .systemTTL(LocalDate.now().plusDays(TTL_GUARD - 1))
            .suspended(TTL.NO)
            .overrideTTL(LocalDate.now())
            .build();
        Map<String, JsonNode> updatedCaseData = new HashMap<>();
        updatedCaseData.put(TTL.TTL_CASE_FIELD_ID, OBJECT_MAPPER.valueToTree(updatedTtl));

        final ValidationException exception = assertThrows(ValidationException.class,
            () -> timeToLiveService.validateSuspensionChange(updatedCaseData, caseData));
        Assert.assertThat(exception.getMessage(),
            startsWith("Unsetting a suspension can only be allowed "
                + "if the deletion will occur beyond the guard period."));
    }

    @Test
    void verifyResolvedTtlSetToNullWhenTtlSuspended() {
        TTL ttl = TTL.builder().systemTTL(LocalDate.now()).overrideTTL(LocalDate.now()).suspended(TTL.YES).build();
        caseData.put(TTL.TTL_CASE_FIELD_ID, OBJECT_MAPPER.valueToTree(ttl));

        LocalDate updatedResolvedTTL = timeToLiveService.getUpdatedResolvedTTL(caseData);

        assertNull(updatedResolvedTTL);
    }

    @Test
    void verifyResolvedTtlSetToNullWhenTtlNotSuspended() {
        TTL ttl = TTL.builder().systemTTL(null).overrideTTL(null).suspended(TTL.NO).build();
        caseData.put(TTL.TTL_CASE_FIELD_ID, OBJECT_MAPPER.valueToTree(ttl));

        LocalDate updatedResolvedTTL = timeToLiveService.getUpdatedResolvedTTL(caseData);

        assertNull(updatedResolvedTTL);
    }

    @Test
    void verifyResolvedTTLSetToOverrideTTL() {
        TTL ttl = TTL.builder().systemTTL(LocalDate.now()).overrideTTL(LocalDate.now()).suspended(TTL.NO).build();
        caseData.put(TTL.TTL_CASE_FIELD_ID, OBJECT_MAPPER.valueToTree(ttl));

        LocalDate updatedResolvedTTL = timeToLiveService.getUpdatedResolvedTTL(caseData);

        assertEquals(ttl.getOverrideTTL(), updatedResolvedTTL);
    }

    @Test
    void verifyResolvedTTLSetToSystemTTL() {
        TTL ttl = TTL.builder().systemTTL(LocalDate.now()).overrideTTL(null).suspended(TTL.NO).build();
        caseData.put(TTL.TTL_CASE_FIELD_ID, OBJECT_MAPPER.valueToTree(ttl));

        LocalDate updatedResolvedTTL = timeToLiveService.getUpdatedResolvedTTL(caseData);

        assertEquals(ttl.getSystemTTL(), updatedResolvedTTL);
    }

    private Map<String, JsonNode> addDaysToSystemTTL(Map<String, JsonNode> data, Integer numOfDays) {
        Map<String, JsonNode> clonedData = new HashMap<>(data);
        TTL expectedTtl = TTL.builder()
            .systemTTL(LocalDate.now().plusDays(numOfDays))
            .build();
        clonedData.put(TTL.TTL_CASE_FIELD_ID, OBJECT_MAPPER.valueToTree(expectedTtl));
        return clonedData;
    }
}
