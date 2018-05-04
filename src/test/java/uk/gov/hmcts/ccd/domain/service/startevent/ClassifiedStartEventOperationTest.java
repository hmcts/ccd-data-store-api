package uk.gov.hmcts.ccd.domain.service.startevent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

class ClassifiedStartEventOperationTest {

    private static final Integer UID = 23;
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String CASE_REFERENCE = "1234123412341234";
    private static final String EVENT_TRIGGER_ID = "updateEvent";
    private static final Boolean IGNORE_WARNING = Boolean.TRUE;

    @Mock
    private StartEventOperation startEventOperation;

    @Mock
    private SecurityClassificationService classificationService;

    private ClassifiedStartEventOperation classifiedStartEventOperation;

    private CaseDetails caseDetails;
    private CaseDetails classifiedDetails;
    private StartEventTrigger startEvent;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseDetails = new CaseDetails();
        startEvent = new StartEventTrigger();
        startEvent.setCaseDetails(caseDetails);

        classifiedDetails = new CaseDetails();
        doReturn(Optional.of(classifiedDetails)).when(classificationService).applyClassification(caseDetails);

        classifiedStartEventOperation = new ClassifiedStartEventOperation(startEventOperation, classificationService);
    }

    @Nested
    @DisplayName("for case type")
    class ForCaseType {

        @Test
        @DisplayName("should call decorated start event operation as is")
        void shouldCallDecoratedStartEventOperation() {
            doReturn(startEvent).when(startEventOperation).triggerStartForCaseType(UID, JURISDICTION_ID, CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING);

            final StartEventTrigger output = classifiedStartEventOperation.triggerStartForCaseType(UID, JURISDICTION_ID, CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING);

            assertAll(
                () -> assertThat(output, sameInstance(startEvent)),
                () -> assertThat(output.getCaseDetails(), sameInstance(caseDetails)),
                () -> verify(startEventOperation).triggerStartForCaseType(UID, JURISDICTION_ID, CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING)
            );
        }
    }

    @Nested
    @DisplayName("for case")
    class ForCase {

        @BeforeEach
        void setUp() {
            doReturn(startEvent).when(startEventOperation).triggerStartForCase(UID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, EVENT_TRIGGER_ID, IGNORE_WARNING);
        }

        @Test
        @DisplayName("should call decorated start event operation as is")
        void shouldCallDecoratedStartEventOperation() {

            classifiedStartEventOperation.triggerStartForCase(UID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, EVENT_TRIGGER_ID, IGNORE_WARNING);

            verify(startEventOperation).triggerStartForCase(UID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, EVENT_TRIGGER_ID, IGNORE_WARNING);
        }

        @Test
        @DisplayName("should return event trigger as is when case details null")
        void shouldReturnEventTriggerWhenCaseDetailsNull() {
            startEvent.setCaseDetails(null);

            final StartEventTrigger output = classifiedStartEventOperation.triggerStartForCase(UID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, EVENT_TRIGGER_ID, IGNORE_WARNING);

            assertAll(
                () -> assertThat(output, sameInstance(startEvent)),
                () -> assertThat(output.getCaseDetails(), is(nullValue()))
            );
        }

        @Test
        @DisplayName("should return event trigger with classified case details when not null")
        void shouldReturnEventTriggerWithClassifiedCaseDetails() {

            final StartEventTrigger output = classifiedStartEventOperation.triggerStartForCase(UID, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, EVENT_TRIGGER_ID, IGNORE_WARNING);

            assertAll(
                () -> assertThat(output.getCaseDetails(), sameInstance(classifiedDetails)),
                () -> verify(classificationService).applyClassification(caseDetails)
            );
        }
    }

}
