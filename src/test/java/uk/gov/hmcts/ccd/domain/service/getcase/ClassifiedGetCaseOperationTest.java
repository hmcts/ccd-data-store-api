package uk.gov.hmcts.ccd.domain.service.getcase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

class ClassifiedGetCaseOperationTest {

    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String CASE_REFERENCE = "1234123412341234";

    @Mock
    private GetCaseOperation getCaseOperation;

    @Mock
    private SecurityClassificationService classificationService;

    private ClassifiedGetCaseOperation classifiedGetCaseOperation;
    private Optional<CaseDetails> caseDetails;
    private Optional<CaseDetails> classifiedDetails;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseDetails = Optional.of(new CaseDetails());
        doReturn(caseDetails).when(getCaseOperation).execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);
        doReturn(caseDetails).when(getCaseOperation).execute(CASE_REFERENCE);

        classifiedDetails = Optional.of(new CaseDetails());
        doReturn(classifiedDetails).when(classificationService).applyClassification(caseDetails.get());

        classifiedGetCaseOperation = new ClassifiedGetCaseOperation(getCaseOperation, classificationService);
    }

    @Nested
    @DisplayName("execute(jurisdictionId, caseTypeId, caseReference)")
    class ExecuteJurisdictionCaseTypeReference {
        @Test
        @DisplayName("should call decorated implementation")
        void shouldCallDecoratedImplementation() {
            classifiedGetCaseOperation.execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);

            verify(getCaseOperation).execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);
        }

        @Test
        @DisplayName("should return empty optional when case not found")
        void shouldReturnEmptyWhenCaseNotFound() {
            doReturn(Optional.empty()).when(getCaseOperation).execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);

            final Optional<CaseDetails> output = classifiedGetCaseOperation.execute(JURISDICTION_ID,
                                                                                    CASE_TYPE_ID,
                                                                                    CASE_REFERENCE);

            assertAll(
                () -> assertThat(output.isPresent(), is(false)),
                () -> verify(classificationService, never()).applyClassification(any())
            );
        }

        @Test
        @DisplayName("should apply classification when case found")
        void shouldApplyClassificationWhenCaseFound() {
            final Optional<CaseDetails> result = classifiedGetCaseOperation.execute(JURISDICTION_ID,
                                                                                    CASE_TYPE_ID,
                                                                                    CASE_REFERENCE);

            verify(classificationService).applyClassification(caseDetails.get());
            assertThat(result, sameInstance(classifiedDetails));
        }

        @Test
        @DisplayName("should return empty case details when case has higher classification")
        void shouldReturnEmptyWhenClassifiedCaseIsEmpty() {
            doReturn(Optional.empty()).when(classificationService).applyClassification(caseDetails.get());

            final Optional<CaseDetails> output = classifiedGetCaseOperation.execute(JURISDICTION_ID,
                                                                                    CASE_TYPE_ID,
                                                                                    CASE_REFERENCE);
            assertThat(output.isPresent(), is(false));
        }

    }

    @Nested
    @DisplayName("execute(caseReference)")
    class ExecuteReference {
        @Test
        @DisplayName("should call decorated implementation")
        void shouldCallDecoratedImplementation() {
            classifiedGetCaseOperation.execute(CASE_REFERENCE);

            verify(getCaseOperation).execute(CASE_REFERENCE);
        }

        @Test
        @DisplayName("should return empty optional when case not found")
        void shouldReturnEmptyWhenCaseNotFound() {
            doReturn(Optional.empty()).when(getCaseOperation).execute(CASE_REFERENCE);

            final Optional<CaseDetails> output = classifiedGetCaseOperation.execute(CASE_REFERENCE);

            assertAll(
                () -> assertThat(output.isPresent(), is(false)),
                () -> verify(classificationService, never()).applyClassification(any())
            );
        }

        @Test
        @DisplayName("should apply classification when case found")
        void shouldApplyClassificationWhenCaseFound() {
            final Optional<CaseDetails> result = classifiedGetCaseOperation.execute(CASE_REFERENCE);

            assertAll(
                () -> verify(classificationService).applyClassification(caseDetails.get()),
                () -> assertThat(result, sameInstance(classifiedDetails))
            );
        }

        @Test
        @DisplayName("should return empty optional when case has higher classification")
        void shouldThrowNotFoundWhenClassifiedCaseIsNull() {
            doReturn(Optional.empty()).when(classificationService).applyClassification(caseDetails.get());

            final Optional<CaseDetails> output = classifiedGetCaseOperation.execute(CASE_REFERENCE);

            assertThat(output.isPresent(), is(false));
        }
    }
}
