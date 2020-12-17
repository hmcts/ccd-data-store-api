package uk.gov.hmcts.ccd.domain.service.common;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.enablingcondition.EnablingConditionParser;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CaseEventEnablingServiceTest {


    private CaseEventEnablingService caseEventEnablingService;

    @Mock
    private EnablingConditionParser enablingConditionParser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        this.caseEventEnablingService = new CaseEventEnablingService(this.enablingConditionParser);
    }

    @Test
    void shouldReturnTrueWhenEnablingConditionIsNull() {
        Boolean value = this.caseEventEnablingService.evaluate(
            null, new HashMap<>());

        assertTrue(value);
    }

    @Test
    void shouldReturnTrueWhenEnablingConditionIsEmpty() {
        Boolean value = this.caseEventEnablingService.evaluate(
            "", new HashMap<>());

        assertTrue(value);
    }

    @Test
    void shouldInvokeParserWhenEnablingConditionIsNotEmpty() {
        Boolean value = this.caseEventEnablingService.evaluate(
            "FieldA='Test' AND FieldB='Test1'", new HashMap<>());

        verify(this.enablingConditionParser,
            times(1)).evaluate(any(String.class), any(Map.class));
    }
}
