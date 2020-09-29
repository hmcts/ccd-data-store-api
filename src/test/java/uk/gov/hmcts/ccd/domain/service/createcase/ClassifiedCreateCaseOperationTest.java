package uk.gov.hmcts.ccd.domain.service.createcase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

class ClassifiedCreateCaseOperationTest {

    private static final String UID = "123";
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "Grant";
    private static final CaseDataContent EVENT_DATA = newCaseDataContent().build();
    private static final Boolean IGNORE = Boolean.FALSE;

    @Mock
    private CreateCaseOperation createCaseOperation;

    @Mock
    private SecurityClassificationService classificationService;

    private ClassifiedCreateCaseOperation classifiedCreateCaseOperation;
    private CaseDetails caseDetails;
    private CaseDetails classifiedCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseDetails = new CaseDetails();
        doReturn(caseDetails).when(createCaseOperation).createCaseDetails(CASE_TYPE_ID,
                                                                          EVENT_DATA,
                                                                          IGNORE);

        classifiedCase = new CaseDetails();
        doReturn(Optional.of(classifiedCase)).when(classificationService).applyClassification(caseDetails);

        classifiedCreateCaseOperation = new ClassifiedCreateCaseOperation(createCaseOperation, classificationService);
    }

    @Test
    @DisplayName("should call decorated operation")
    void shouldCallDecoratedOperation() {
        classifiedCreateCaseOperation.createCaseDetails(CASE_TYPE_ID, EVENT_DATA, IGNORE);

        verify(createCaseOperation).createCaseDetails(CASE_TYPE_ID, EVENT_DATA, IGNORE);
    }

    @Test
    @DisplayName("should return null when decorated operation returns null")
    void shouldReturnNullWhenOperationReturnsNull() {
        doReturn(null).when(createCaseOperation).createCaseDetails(CASE_TYPE_ID,
                                                                   EVENT_DATA,
                                                                   IGNORE);

        final CaseDetails output = classifiedCreateCaseOperation.createCaseDetails(CASE_TYPE_ID,
                                                                                   EVENT_DATA,
                                                                                   IGNORE);

        assertThat(output, is(nullValue()));
    }

    @Test
    @DisplayName("should return classified case detail")
    void shouldReturnClassifiedCaseDetails() {

        final CaseDetails output = classifiedCreateCaseOperation.createCaseDetails(CASE_TYPE_ID,
                                                                                   EVENT_DATA,
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

        final CaseDetails output = classifiedCreateCaseOperation.createCaseDetails(CASE_TYPE_ID,
                                                                                   EVENT_DATA,
                                                                                   IGNORE);

        assertThat(output, is(nullValue()));
    }

}
