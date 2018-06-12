package uk.gov.hmcts.ccd.domain.service.listevents;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ClassifiedListEventsOperationTest {

    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "CaseTypeId";
    private final static String CASE_REFERENCE = "999999";
    private final static Long EVENT_ID = 100L;

    @Mock
    private ListEventsOperation listEventsOperation;

    @Mock
    private SecurityClassificationService classificationService;

    private ClassifiedListEventsOperation classifiedOperation;
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

        doReturn(events).when(listEventsOperation).execute(caseDetails);
        doReturn(events).when(listEventsOperation).execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);

        classifiedEvents = Lists.newArrayList(event);

        doReturn(classifiedEvents).when(classificationService).applyClassification(JURISDICTION_ID, events);

        classifiedOperation = new ClassifiedListEventsOperation(listEventsOperation, classificationService);
    }

    @Test
    @DisplayName("should call decorated implementation")
    void shouldCallDecoratedImplementation() {

        classifiedOperation.execute(caseDetails);

        verify(listEventsOperation).execute(caseDetails);
    }

    @Test
    @DisplayName("should return empty list when decorated implementation returns null")
    void shouldReturnEmptyListInsteadOfNull() {
        doReturn(null).when(listEventsOperation).execute(caseDetails);

        final List<AuditEvent> outputs = classifiedOperation.execute(caseDetails);

        assertAll(
            () -> assertThat(outputs, is(notNullValue())),
            () -> assertThat(outputs, hasSize(0)),
            () -> verify(classificationService, never()).applyClassification(JURISDICTION_ID, null)
        );
    }

    @Test
    @DisplayName("should apply security classifications for case details")
    void shouldApplySecurityClassificationsForCaseDetails() {
        final List<AuditEvent> outputs = classifiedOperation.execute(caseDetails);

        assertAll(
            () -> verify(classificationService).applyClassification(JURISDICTION_ID, events),
            () -> assertThat(outputs, sameInstance(classifiedEvents))
        );
    }

    @Test
    @DisplayName("should apply security classifications when jurisdiction, case type id and case reference is received")
    void shouldApplySecurityClassificationsForJurisdictionCaseTypeIdAndCaseReference() {
        final List<AuditEvent> outputs = classifiedOperation.execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);

        assertAll(
            () -> verify(classificationService).applyClassification(JURISDICTION_ID, events),
            () -> assertThat(outputs, sameInstance(classifiedEvents))
        );
    }

    @Test
    @DisplayName("should apply security classifications when jurisdiction, case type id and case event id is received")
    void shouldApplySecurityClassificationsForJurisdictionCaseTypeIdAndEventId() {
        doReturn(event).when(listEventsOperation).execute(JURISDICTION_ID, CASE_TYPE_ID, EVENT_ID);
        doReturn(classifiedEvents).when(classificationService).applyClassification(eq(JURISDICTION_ID), anyListOf(AuditEvent.class));

        AuditEvent output = classifiedOperation.execute(JURISDICTION_ID, CASE_TYPE_ID, EVENT_ID);

        assertAll(
            () -> verify(classificationService).applyClassification(eq(JURISDICTION_ID), anyListOf(AuditEvent.class)),
            () -> assertThat(output, sameInstance(event))
        );
    }

    @Test
    @DisplayName("should apply security classifications when jurisdiction, case type id and case event id is received")
    void shouldThrowExceptionWhenNoEventReturnAfterSecurityClassifications() {
        doReturn(event).when(listEventsOperation).execute(JURISDICTION_ID, CASE_TYPE_ID, EVENT_ID);
        doReturn(emptyList()).when(classificationService).applyClassification(eq(JURISDICTION_ID), anyListOf(AuditEvent.class));

        assertThrows(ResourceNotFoundException.class, () -> classifiedOperation.execute(JURISDICTION_ID, CASE_TYPE_ID, EVENT_ID));
    }
}
