package uk.gov.hmcts.ccd.domain.service.getevents;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.anyListOf;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ClassifiedGetEventsOperationTest {

    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "CaseTypeId";
    private static final String CASE_REFERENCE = "999999";
    private static final String CASE_REFERENCE_INVALID = "9999991";
    private static final String CASE_REFERENCE_INVALID_NULL = "9999992";
    private static final Long EVENT_ID = 100L;

    @Mock
    private GetEventsOperation getEventsOperation;

    @Mock
    private SecurityClassificationService classificationService;

    @Mock
    private GetCaseOperation getCaseOperation;

    private ClassifiedGetEventsOperation classifiedOperation;
    private CaseDetails caseDetails;
    private List<AuditEvent> events;
    private AuditEvent event;
    private List<AuditEvent> classifiedEvents;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);
        events = Arrays.asList(new AuditEvent(), new AuditEvent());
        event = new AuditEvent();

        doReturn(events).when(getEventsOperation).getEvents(caseDetails);
        doReturn(events).when(getEventsOperation).getEvents(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);
        doReturn(events).when(getEventsOperation).getEvents(CASE_REFERENCE);
        doReturn(Lists.newArrayList()).when(getEventsOperation).getEvents(CASE_REFERENCE_INVALID);
        doReturn(null).when(getEventsOperation).getEvents(CASE_REFERENCE_INVALID_NULL);
        doReturn(Optional.of(caseDetails)).when(getCaseOperation).execute(CASE_REFERENCE);


        classifiedEvents = Lists.newArrayList(event);

        doReturn(classifiedEvents).when(classificationService).applyClassification(JURISDICTION_ID, events);

        classifiedOperation = new ClassifiedGetEventsOperation(getEventsOperation, classificationService, getCaseOperation);
    }

    @Test
    @DisplayName("should call decorated implementation")
    void shouldCallDecoratedImplementation() {

        classifiedOperation.getEvents(caseDetails);

        verify(getEventsOperation).getEvents(caseDetails);
    }

    @Test
    @DisplayName("should return empty list when decorated implementation returns null")
    void shouldReturnEmptyListInsteadOfNull() {
        doReturn(null).when(getEventsOperation).getEvents(caseDetails);

        final List<AuditEvent> outputs = classifiedOperation.getEvents(caseDetails);

        assertAll(
            () -> assertThat(outputs, is(notNullValue())),
            () -> assertThat(outputs, hasSize(0)),
            () -> verify(classificationService, never()).applyClassification(JURISDICTION_ID, null)
        );
    }

    @Test
    @DisplayName("should apply security classifications for case details")
    void shouldApplySecurityClassificationsForCaseDetails() {
        final List<AuditEvent> outputs = classifiedOperation.getEvents(caseDetails);

        assertAll(
            () -> verify(classificationService).applyClassification(JURISDICTION_ID, events),
            () -> assertThat(outputs, sameInstance(classifiedEvents))
        );
    }

    @Test
    @DisplayName("should apply security classifications when jurisdiction, case type id and case reference is received")
    void shouldApplySecurityClassificationsForJurisdictionCaseTypeIdAndCaseReference() {
        final List<AuditEvent> outputs = classifiedOperation.getEvents(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);

        assertAll(
            () -> verify(classificationService).applyClassification(JURISDICTION_ID, events),
            () -> assertThat(outputs, sameInstance(classifiedEvents))
        );
    }

    @Test
    @DisplayName("should apply security classifications when case reference is received")
    void shouldApplySecurityClassificationsForCaseReference() {
        final List<AuditEvent> outputs = classifiedOperation.getEvents(CASE_REFERENCE);

        InOrder inOrder = inOrder(getEventsOperation, classificationService, getCaseOperation);

        assertAll(() -> inOrder.verify(getEventsOperation).getEvents(CASE_REFERENCE),
            () -> inOrder.verify(getCaseOperation).execute(CASE_REFERENCE),
            () -> inOrder.verify(classificationService).applyClassification(JURISDICTION_ID, events)
        );
    }

    @Test
    @DisplayName("should return empty collection when no data found")
    void shouldReturnEmptyListWhenNoDataFoundForCaseReference() {
        final List<AuditEvent> outputs = classifiedOperation.getEvents(CASE_REFERENCE_INVALID);

        assertAll(
            () -> assertThat(outputs, is(notNullValue())),
            () -> assertThat(outputs.size(), is(0))
        );
    }

    @Test
    @DisplayName("should return empty collection when get events returns null")
    void shouldReturnEmptyListWhenNullDataFoundForCaseReference() {
        final List<AuditEvent> outputs = classifiedOperation.getEvents(CASE_REFERENCE_INVALID_NULL);

        assertAll(
            () -> assertThat(outputs, is(notNullValue())),
            () -> assertThat(outputs.size(), is(0))
        );
    }


    @Test
    @DisplayName("should apply security classifications when jurisdiction, case type id and case event id is received")
    void shouldApplySecurityClassificationsForJurisdictionCaseTypeIdAndEventId() {
        doReturn(Optional.of(event)).when(getEventsOperation).getEvent(JURISDICTION_ID, CASE_TYPE_ID, EVENT_ID);
        doReturn(classifiedEvents).when(classificationService)
            .applyClassification(eq(JURISDICTION_ID), anyListOf(AuditEvent.class));

        Optional<AuditEvent> optionalAuditEvent = classifiedOperation.getEvent(JURISDICTION_ID, CASE_TYPE_ID, EVENT_ID);

        assertThat(optionalAuditEvent.isPresent(), is(true));
        AuditEvent output = optionalAuditEvent.get();
        assertAll(
            () -> verify(classificationService).applyClassification(eq(JURISDICTION_ID), anyListOf(AuditEvent.class)),
            () -> assertThat(output, sameInstance(event))
        );
    }

}
