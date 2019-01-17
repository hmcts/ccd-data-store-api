package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDraftBuilder.newCaseDraft;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventTriggerBuilder.newCaseEventTrigger;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.DraftResponseBuilder.newDraftResponse;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.StartEventTriggerBuilder.newStartEventTrigger;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventField;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.startevent.StartEventOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

class DefaultGetEventTriggerOperationTest {

    private static final String EVENT_TRIGGER_ID = "testEventTriggerId";
    private static final String EVENT_TRIGGER_NAME = "testEventTriggerName";
    private static final String EVENT_TRIGGER_DESCRIPTION = "testEventTriggerDescription";
    private static final Boolean EVENT_TRIGGER_SHOW_SUMMARY = true;
    private static final Boolean EVENT_TRIGGER_SHOW_EVENT_NOTES = false;
    private static final String CASE_REFERENCE = "1234567891012345";
    private static final String DRAFT_ID = "DRAFT1";
    private static final String CASE_TYPE_ID = "Grant";
    private static final Boolean IGNORE = Boolean.TRUE;
    private static final String TOKEN = "testToken";
    private final DraftResponse draftResponse = newDraftResponse().withDocument(newCaseDraft().withEventTriggerId(EVENT_TRIGGER_ID).build()).build();
    private final CaseDetails caseDetails = newCaseDetails().withCaseTypeId(CASE_TYPE_ID).build();
    private final CaseEvent caseEvent = new CaseEvent();
    private final List<CaseEventField> eventFields = Lists.newArrayList();
    private final StartEventTrigger startEventTrigger = newStartEventTrigger().withEventToken(TOKEN).withCaseDetails(caseDetails).build();
    private final CaseEventTrigger caseEventTrigger = newCaseEventTrigger().build();

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private StartEventOperation startEventOperation;

    @Mock
    private UIDService uidService;

    @Mock
    private DraftGateway draftGateway;

    @Mock
    private CaseEventTriggerBuilder caseEventTriggerBuilder;

    private DefaultGetEventTriggerOperation defaultGetEventTriggerOperation;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        defaultGetEventTriggerOperation = new DefaultGetEventTriggerOperation(
            caseDetailsRepository,
            uidService,
            startEventOperation,
            draftGateway,
            caseEventTriggerBuilder);

        doReturn(true).when(uidService).validateUID(CASE_REFERENCE);
        doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReference(CASE_REFERENCE);

        when(startEventOperation.triggerStartForCaseType(CASE_TYPE_ID,
                                                         EVENT_TRIGGER_ID,
                                                         IGNORE)).thenReturn(startEventTrigger);
        when(startEventOperation.triggerStartForCase(CASE_REFERENCE,
                                                     EVENT_TRIGGER_ID,
                                                     IGNORE)).thenReturn(startEventTrigger);
        when(startEventOperation.triggerStartForDraft(DRAFT_ID,
                                                      IGNORE)).thenReturn(startEventTrigger);
        when(caseEventTriggerBuilder.build(startEventTrigger,
                                           CASE_TYPE_ID,
                                           EVENT_TRIGGER_ID,
                                           null)).thenReturn(caseEventTrigger);
        when(caseEventTriggerBuilder.build(startEventTrigger,
                                           CASE_TYPE_ID,
                                           EVENT_TRIGGER_ID,
                                           CASE_REFERENCE)).thenReturn(caseEventTrigger);
        when(caseEventTriggerBuilder.build(startEventTrigger,
                                           CASE_TYPE_ID,
                                           EVENT_TRIGGER_ID,
                                           DRAFT_ID)).thenReturn(caseEventTrigger);
    }

    @Nested
    @DisplayName("for case type")
    class ForCaseType {

        @Test
        @DisplayName("should get trigger with all data set")
        void shouldGetTriggerWithAllDataSet() {
            CaseEventTrigger result = defaultGetEventTriggerOperation.executeForCaseType(CASE_TYPE_ID,
                                                                                         EVENT_TRIGGER_ID,
                                                                                         IGNORE);
            InOrder inOrder = inOrder(startEventOperation,
                                      caseEventTriggerBuilder);

            assertAll(
                () -> assertThat(result, sameInstance(caseEventTrigger)),
                () -> inOrder.verify(startEventOperation).triggerStartForCaseType(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE),
                () -> inOrder.verify(caseEventTriggerBuilder).build(startEventTrigger, CASE_TYPE_ID, EVENT_TRIGGER_ID, null)
            );
        }
    }

    @Nested
    @DisplayName("for case")
    class ForCase {

        @Test
        @DisplayName("Should fail if case reference invalid")
        void shouldFailIfCaseReferenceInvalid() {

            doReturn(false).when(uidService).validateUID(CASE_REFERENCE);

            final Exception exception = assertThrows(BadRequestException.class, () -> defaultGetEventTriggerOperation.executeForCase(CASE_REFERENCE,
                                                                                                                                     EVENT_TRIGGER_ID,
                                                                                                                                     IGNORE));
            assertThat(exception.getMessage(), startsWith("Case reference is not valid"));
        }

        @Test
        @DisplayName("Should fail if no case")
        void shouldPropagateCaseNotFoundExceptionIfCaseNotFound() {

            doReturn(true).when(uidService).validateUID(CASE_REFERENCE);
            doReturn(Optional.empty()).when(caseDetailsRepository).findByReference(CASE_REFERENCE);

            final Exception exception = assertThrows(CaseNotFoundException.class, () -> defaultGetEventTriggerOperation.executeForCase(CASE_REFERENCE,
                                                                                                                                       EVENT_TRIGGER_ID,
                                                                                                                                       IGNORE));
            assertThat(exception.getMessage(), startsWith("No case found for reference: " + CASE_REFERENCE));
        }

        @Test
        @DisplayName("should get trigger with all data set")
        void shouldGetTriggerWithAllDataSet() {
            caseEvent.setId(EVENT_TRIGGER_ID);
            caseEvent.setName(EVENT_TRIGGER_NAME);
            caseEvent.setDescription(EVENT_TRIGGER_DESCRIPTION);
            caseEvent.setShowSummary(EVENT_TRIGGER_SHOW_SUMMARY);
            caseEvent.setShowEventNotes(EVENT_TRIGGER_SHOW_EVENT_NOTES);

            caseEvent.setCaseFields(eventFields);

            CaseEventTrigger result = defaultGetEventTriggerOperation.executeForCase(CASE_REFERENCE,
                                                                                     EVENT_TRIGGER_ID,
                                                                                     IGNORE);

            InOrder inOrder = inOrder(caseDetailsRepository,
                                      startEventOperation,
                                      uidService,
                                      caseEventTriggerBuilder);
            assertAll(
                () -> assertThat(result, sameInstance(caseEventTrigger)),
                () -> inOrder.verify(uidService).validateUID(CASE_REFERENCE),
                () -> inOrder.verify(caseDetailsRepository).findByReference(CASE_REFERENCE),
                () -> inOrder.verify(startEventOperation).triggerStartForCase(CASE_REFERENCE, EVENT_TRIGGER_ID, IGNORE),
                () -> inOrder.verify(caseEventTriggerBuilder).build(startEventTrigger, CASE_TYPE_ID, EVENT_TRIGGER_ID, CASE_REFERENCE),
                () -> inOrder.verifyNoMoreInteractions()
            );
        }
    }

    @Nested
    @DisplayName("for draft")
    class ForDraft {

        @BeforeEach
        void setUp() {
            when(draftGateway.get(Draft.stripId(DRAFT_ID))).thenReturn(draftResponse);
            when(draftGateway.getCaseDetails(Draft.stripId(DRAFT_ID))).thenReturn(caseDetails);
        }


        @Test
        @DisplayName("should fail if no draft")
        void shouldFailIfNoDraft() {
            when(draftGateway.getCaseDetails(Draft.stripId(DRAFT_ID))).thenThrow(ResourceNotFoundException.class);

            assertThrows(ResourceNotFoundException.class, () -> defaultGetEventTriggerOperation.executeForDraft(DRAFT_ID,
                                                                                                                IGNORE));
        }

        @Test
        @DisplayName("should fail if downstream fails to get trigger for draft")
        void shouldFailIfNoDraftDetails() {
            doThrow(ResourceNotFoundException.class).when(startEventOperation).triggerStartForDraft(DRAFT_ID,
                                                                                                    IGNORE);
            assertThrows(ResourceNotFoundException.class, () -> defaultGetEventTriggerOperation.executeForDraft(DRAFT_ID,
                                                                                                                IGNORE));
        }

        @Test
        @DisplayName("should get trigger")
        void shouldGetTrigger() {
            CaseEventTrigger result = defaultGetEventTriggerOperation.executeForDraft(DRAFT_ID,
                                                                                      IGNORE);

            InOrder inOrder = inOrder(draftGateway,
                                      startEventOperation,
                                      uidService,
                                      caseEventTriggerBuilder);

            assertAll(
                () -> assertThat(result, sameInstance(caseEventTrigger)),
                () -> inOrder.verify(draftGateway).getCaseDetails(Draft.stripId(DRAFT_ID)),
                () -> inOrder.verify(startEventOperation).triggerStartForDraft(DRAFT_ID, IGNORE),
                () -> inOrder.verify(caseEventTriggerBuilder).build(startEventTrigger, CASE_TYPE_ID, EVENT_TRIGGER_ID, DRAFT_ID)
            );
        }
    }
}
