package uk.gov.hmcts.ccd.datastore.befta;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DateCaseClosedCleanupContextTest {

    @AfterEach
    void clearContext() {
        DateCaseClosedCleanupContext.clear();
    }

    @Test
    void recordsUniqueReferencesForScenarioTeardown() {
        DateCaseClosedCleanupContext.beginScenario();

        DateCaseClosedCleanupContext.registerCaseReference("1234567890123456");
        DateCaseClosedCleanupContext.registerCaseReference(1234567890123456L);

        assertEquals(List.of(1234567890123456L),
            DateCaseClosedCleanupContext.currentScenario().getCaseReferences());
    }

    @Test
    void rejectsRegistrationWhenThereIsNoActiveScenario() {
        assertThrows(IllegalStateException.class,
            () -> DateCaseClosedCleanupContext.registerCaseReference(1234567890123456L));
    }

    @Test
    void removesScenarioStateWhenItIsCleared() {
        DateCaseClosedCleanupContext.beginScenario();
        DateCaseClosedCleanupContext.registerCaseReference(1234567890123456L);

        DateCaseClosedCleanupContext.clear();

        assertNull(DateCaseClosedCleanupContext.currentScenario());
    }
}
