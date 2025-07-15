package uk.gov.hmcts.ccd.domain.service.createevent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

class ClassifiedCreateEventOperationTest {

    private static final String UID = "123";
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "Grant";
    private static final String CASE_REFERENCE = "1234123412341234";
    private static final int CASE_VERSION = 0;
    private static final String ATTRIBUTE_PATH = "DocumentField";
    private static final String CATEGORY_ID = "categoryId";
    private static final CaseDataContent CASE_DATA_CONTENT = newCaseDataContent().build();

    @Mock
    private CreateEventOperation createEventOperation;

    @Mock
    private SecurityClassificationServiceImpl classificationService;

    private ClassifiedCreateEventOperation classifiedCreateEventOperation;
    private CaseDetails caseDetails;
    private CaseDetails classifiedCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseDetails = new CaseDetails();
        doReturn(caseDetails).when(createEventOperation).createCaseEvent(CASE_REFERENCE,
                                                                         CASE_DATA_CONTENT);
        doReturn(caseDetails).when(createEventOperation).createCaseSystemEvent(CASE_REFERENCE,
            CASE_VERSION, ATTRIBUTE_PATH, CATEGORY_ID);

        classifiedCase = new CaseDetails();
        doReturn(Optional.of(classifiedCase)).when(classificationService).applyClassification(caseDetails);

        classifiedCreateEventOperation =
                new ClassifiedCreateEventOperation(createEventOperation, classificationService);
    }

    @Test
    @DisplayName("should call decorated operation")
    void shouldCallDecoratedOperation() {
        classifiedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
                                                       CASE_DATA_CONTENT);

        verify(createEventOperation).createCaseEvent(CASE_REFERENCE,
                                                     CASE_DATA_CONTENT);
    }

    @Test
    @DisplayName("should return null when decorated operation returns null")
    void shouldReturnNullWhenOperationReturnsNull() {
        doReturn(null).when(createEventOperation).createCaseEvent(CASE_REFERENCE,
                                                                  CASE_DATA_CONTENT);

        final CaseDetails output = classifiedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
                                                                                  CASE_DATA_CONTENT);

        assertThat(output, is(nullValue()));
    }

    @Test
    @DisplayName("should return classified case detail")
    void shouldReturnClassifiedCaseDetails() {

        final CaseDetails output = classifiedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
                                                                                  CASE_DATA_CONTENT);

        assertAll(
            () -> assertThat(output, sameInstance(classifiedCase)),
            () -> verify(classificationService).applyClassification(caseDetails)
        );
    }

    @Test
    @DisplayName("should return null when case has higher classification")
    void shouldReturnNullCaseDetailsWhenHigherClassification() {

        doReturn(Optional.empty()).when(classificationService).applyClassification(caseDetails);

        final CaseDetails output = classifiedCreateEventOperation.createCaseEvent(CASE_REFERENCE,
                                                                                  CASE_DATA_CONTENT);

        assertThat(output, is(nullValue()));
    }

    @Test
    @DisplayName("should return classified case system event case detail")
    void shouldReturnClassifiedCaseSystemEventDetails() {
        final CaseDetails output = classifiedCreateEventOperation.createCaseSystemEvent(CASE_REFERENCE,
            CASE_VERSION, ATTRIBUTE_PATH, CATEGORY_ID);

        assertAll(
            () -> assertThat(output, sameInstance(classifiedCase)),
            () -> verify(classificationService).applyClassification(caseDetails)
        );
    }

    @Test
    @DisplayName("should return null when decorated operation returns null")
    void shouldReturnNullWhenCaseSystemEventOperationReturnsNull() {
        doReturn(null).when(createEventOperation).createCaseSystemEvent(CASE_REFERENCE,
            CASE_VERSION, ATTRIBUTE_PATH, CATEGORY_ID);

        final CaseDetails output = classifiedCreateEventOperation.createCaseSystemEvent(CASE_REFERENCE,
            CASE_VERSION, ATTRIBUTE_PATH, CATEGORY_ID);

        assertThat(output, is(nullValue()));
    }

}
