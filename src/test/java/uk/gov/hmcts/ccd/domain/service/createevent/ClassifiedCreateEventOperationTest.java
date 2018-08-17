package uk.gov.hmcts.ccd.domain.service.createevent;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;

class ClassifiedCreateEventOperationTest {

    private static final String UID = "123";
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "Grant";
    private static final String CASE_REFERENCE = "1234123412341234";
    private static final Event EVENT = anEvent().build();
    private static final Map<String, JsonNode> DATA = new HashMap<>();
    private static final String TOKEN = "JwtToken";
    private static final Boolean IGNORE = Boolean.TRUE;

    @Mock
    private CreateEventOperation createEventOperation;

    @Mock
    private SecurityClassificationService classificationService;

    private ClassifiedCreateEventOperation classifiedCreateEventOperation;
    private CaseDetails caseDetails;
    private CaseDetails classifiedCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseDetails = new CaseDetails();
        doReturn(caseDetails).when(createEventOperation).createCaseEvent(UID,
                                                                         JURISDICTION_ID,
                                                                         CASE_TYPE_ID,
                                                                         CASE_REFERENCE,
                                                                         EVENT,
                                                                         DATA,
                                                                         TOKEN,
                                                                         IGNORE);

        classifiedCase = new CaseDetails();
        doReturn(Optional.of(classifiedCase)).when(classificationService).applyClassification(caseDetails);

        classifiedCreateEventOperation = new ClassifiedCreateEventOperation(createEventOperation, classificationService);
    }

    @Test
    @DisplayName("should call decorated operation")
    void shouldCallDecoratedOperation() {
        classifiedCreateEventOperation.createCaseEvent(UID,
                                                       JURISDICTION_ID,
                                                       CASE_TYPE_ID,
                                                       CASE_REFERENCE,
                                                       EVENT,
                                                       DATA,
                                                       TOKEN,
                                                       IGNORE);

        verify(createEventOperation).createCaseEvent(UID,
                                                     JURISDICTION_ID,
                                                     CASE_TYPE_ID,
                                                     CASE_REFERENCE,
                                                     EVENT,
                                                     DATA,
                                                     TOKEN,
                                                     IGNORE);
    }

    @Test
    @DisplayName("should return null when decorated operation returns null")
    void shouldReturnNullWhenOperationReturnsNull() {
        doReturn(null).when(createEventOperation).createCaseEvent(UID,
                                                                  JURISDICTION_ID,
                                                                  CASE_TYPE_ID,
                                                                  CASE_REFERENCE,
                                                                  EVENT,
                                                                  DATA,
                                                                  TOKEN,
                                                                  IGNORE);

        final CaseDetails output = classifiedCreateEventOperation.createCaseEvent(UID,
                                                                                  JURISDICTION_ID,
                                                                                  CASE_TYPE_ID,
                                                                                  CASE_REFERENCE,
                                                                                  EVENT,
                                                                                  DATA,
                                                                                  TOKEN,
                                                                                  IGNORE);

        assertThat(output, is(nullValue()));
    }

    @Test
    @DisplayName("should return classified case detail")
    void shouldReturnClassifiedCaseDetails() {

        final CaseDetails output = classifiedCreateEventOperation.createCaseEvent(UID,
                                                                                  JURISDICTION_ID,
                                                                                  CASE_TYPE_ID,
                                                                                  CASE_REFERENCE,
                                                                                  EVENT,
                                                                                  DATA,
                                                                                  TOKEN,
                                                                                  IGNORE);

        assertAll(
            () -> assertThat(output, sameInstance(classifiedCase)),
            () -> verify(classificationService).applyClassification(caseDetails)
        );
    }

    @Test
    @DisplayName("should return null when case has higher classification")
    void shouldReturnNullCaseDetailsWhenHigherClassification() {

        doReturn(Optional.empty()).when(classificationService).applyClassification(caseDetails);

        final CaseDetails output = classifiedCreateEventOperation.createCaseEvent(UID,
                                                                                  JURISDICTION_ID,
                                                                                  CASE_TYPE_ID,
                                                                                  CASE_REFERENCE,
                                                                                  EVENT,
                                                                                  DATA,
                                                                                  TOKEN,
                                                                                  IGNORE);

        assertThat(output, is(nullValue()));
    }

}
