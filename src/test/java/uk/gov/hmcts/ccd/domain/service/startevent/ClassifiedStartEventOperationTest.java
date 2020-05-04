package uk.gov.hmcts.ccd.domain.service.startevent;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;

class ClassifiedStartEventOperationTest {

    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String CASE_REFERENCE = "1234123412341234";
    private static final String DRAFT_REFERENCE = "1";
    private static final String EVENT_TRIGGER_ID = "updateEvent";
    private static final Boolean IGNORE_WARNING = Boolean.TRUE;

    @Mock
    private StartEventOperation startEventOperation;

    @Mock
    private SecurityClassificationService classificationService;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private CaseDataService caseDataService;

    @Mock
    private DraftGateway draftGateway;

    private ClassifiedStartEventOperation classifiedStartEventOperation;

    private CaseDetails caseDetails;
    private CaseDetails classifiedDetails;
    private StartEventResult startEvent;
    private CaseTypeDefinition caseTypeDefinition;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseDetails = newCaseDetails().withCaseTypeId(CASE_TYPE_ID).build();
        startEvent = new StartEventResult();
        startEvent.setCaseDetails(caseDetails);
        caseTypeDefinition = newCaseType().build();

        classifiedDetails = new CaseDetails();
        doReturn(Optional.of(classifiedDetails)).when(classificationService).applyClassification(caseDetails);

        classifiedStartEventOperation = new ClassifiedStartEventOperation(startEventOperation,
                                                                          classificationService,
                                                                          caseDefinitionRepository,
                                                                          caseDataService,
                                                                          draftGateway);
    }

    @Nested
    @DisplayName("for case type - deprecated")
    class ForCaseTypeDeprecated {

        @Test
        @DisplayName("should call decorated start event operation as is")
        void shouldCallDecoratedStartEventOperation() {
            doReturn(startEvent).when(startEventOperation).triggerStartForCaseType(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING);

            final StartEventResult output = classifiedStartEventOperation.triggerStartForCaseType(CASE_TYPE_ID,
                                                                                                   EVENT_TRIGGER_ID,
                                                                                                   IGNORE_WARNING);

            assertAll(
                () -> assertThat(output, sameInstance(startEvent)),
                () -> assertThat(output.getCaseDetails(), sameInstance(caseDetails)),
                () -> verify(startEventOperation).triggerStartForCaseType(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING)
            );
        }
    }

    @Nested
    @DisplayName("for case type")
    class ForCaseType {

        @Test
        @DisplayName("should call decorated start event operation as is")
        void shouldCallDecoratedStartEventOperation() {
            doReturn(startEvent).when(startEventOperation).triggerStartForCaseType(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING);

            final StartEventResult output = classifiedStartEventOperation.triggerStartForCaseType(CASE_TYPE_ID,
                                                                                                   EVENT_TRIGGER_ID,
                                                                                                   IGNORE_WARNING);

            assertAll(
                () -> assertThat(output, sameInstance(startEvent)),
                () -> assertThat(output.getCaseDetails(), sameInstance(caseDetails)),
                () -> verify(startEventOperation).triggerStartForCaseType(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING)
            );
        }
    }

    @Nested
    @DisplayName("for case")
    class ForCase {

        @BeforeEach
        void setUp() {
            doReturn(startEvent).when(startEventOperation).triggerStartForCase(CASE_REFERENCE,
                                                                               EVENT_TRIGGER_ID,
                                                                               IGNORE_WARNING);
        }

        @Test
        @DisplayName("should call decorated start event operation as is")
        void shouldCallDecoratedStartEventOperation() {

            classifiedStartEventOperation.triggerStartForCase(CASE_REFERENCE,
                                                              EVENT_TRIGGER_ID,
                                                              IGNORE_WARNING);

            verify(startEventOperation).triggerStartForCase(CASE_REFERENCE,
                                                            EVENT_TRIGGER_ID,
                                                            IGNORE_WARNING);
        }

        @Test
        @DisplayName("should return event trigger as is when case details null")
        void shouldReturnEventTriggerWhenCaseDetailsNull() {
            startEvent.setCaseDetails(null);

            final StartEventResult output = classifiedStartEventOperation.triggerStartForCase(CASE_REFERENCE,
                                                                                               EVENT_TRIGGER_ID,
                                                                                               IGNORE_WARNING);

            assertAll(
                () -> assertThat(output, sameInstance(startEvent)),
                () -> assertThat(output.getCaseDetails(), is(nullValue()))
            );
        }

        @Test
        @DisplayName("should return event trigger with classified case details when not null")
        void shouldReturnEventTriggerWithClassifiedCaseDetails() {

            final StartEventResult output = classifiedStartEventOperation.triggerStartForCase(CASE_REFERENCE,
                                                                                               EVENT_TRIGGER_ID,
                                                                                               IGNORE_WARNING);

            assertAll(
                () -> assertThat(output.getCaseDetails(), sameInstance(classifiedDetails)),
                () -> verify(classificationService).applyClassification(caseDetails)
            );
        }
    }

    @Nested
    @DisplayName("for draft")
    class ForDraft {

        @BeforeEach
        void setUp() {
            doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
            doReturn(startEvent).when(startEventOperation).triggerStartForDraft(DRAFT_REFERENCE,
                                                                                IGNORE_WARNING);
            when(draftGateway.getCaseDetails(Draft.stripId(DRAFT_REFERENCE))).thenReturn(caseDetails);
        }

        @Test
        @DisplayName("should call decorated start event operation as is")
        void shouldCallDecoratedStartEventOperation() {

            classifiedStartEventOperation.triggerStartForDraft(DRAFT_REFERENCE, IGNORE_WARNING);

            verify(startEventOperation).triggerStartForDraft(DRAFT_REFERENCE, IGNORE_WARNING);
        }

        @Test
        @DisplayName("should derive default classifications from case type")
        void shouldDeriveDefaultClassificationsFromCaseType() {

            classifiedStartEventOperation.triggerStartForDraft(DRAFT_REFERENCE, IGNORE_WARNING);

            assertAll(
                () -> verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
                () -> verify(caseDataService).getDefaultSecurityClassifications(eq(caseTypeDefinition), eq(caseDetails.getData()), eq(Maps.newHashMap()))
            );
        }

        @Test
        @DisplayName("should return event trigger as is when case details null")
        void shouldReturnEventTriggerWhenCaseDetailsNull() {
            startEvent.setCaseDetails(null);

            final StartEventResult output = classifiedStartEventOperation.triggerStartForDraft(DRAFT_REFERENCE,
                                                                                                IGNORE_WARNING);

            assertAll(
                () -> assertThat(output, sameInstance(startEvent)),
                () -> assertThat(output.getCaseDetails(), is(nullValue()))
            );
        }

        @Test
        @DisplayName("should return event trigger with classified case details when not null")
        void shouldReturnEventTriggerWithClassifiedCaseDetails() {

            final StartEventResult output = classifiedStartEventOperation.triggerStartForDraft(DRAFT_REFERENCE,
                                                                                                IGNORE_WARNING);

            assertAll(
                () -> assertThat(output.getCaseDetails(), sameInstance(classifiedDetails)),
                () -> verify(classificationService).applyClassification(caseDetails)
            );
        }
    }

}
