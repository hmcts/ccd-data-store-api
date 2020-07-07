package uk.gov.hmcts.ccd.domain.service.getcase;

import java.util.Optional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

class CreatorGetCaseOperationTest {

    @Mock
    private GetCaseOperation getCaseOperation;

    @Mock
    private CaseAccessService caseAccessService;

    private CreatorGetCaseOperation classUnderTest;

    private static final String JURISDICTION_ID = "JURISDICTION_ID";
    private static final String CASE_TYPE_ID = "CASE_TYPE_ID";
    private static final String CASE_REFERENCE = "CASE_REFERENCE";

    private CaseDetails caseDetails = new CaseDetails();

    @BeforeEach
    public void setupMocks() {
        MockitoAnnotations.initMocks(this);

        classUnderTest = new CreatorGetCaseOperation(getCaseOperation, caseAccessService);

        when(getCaseOperation.execute(any(), any(), any())).thenReturn(Optional.of(caseDetails));
        when(getCaseOperation.execute(any())).thenReturn(Optional.of(caseDetails));
    }

    @Nested
    @DisplayName("execute(String jurisdictionId, String caseTypeId, String caseReference)")
    class DeprecatedMethod {

        @Test
        @DisplayName("Should return an Optional containing the case if the case is visible")
        void searchOperationReturnsCaseDetails_solicitorVisibilityServiceCalledForCaseDetailsAndReturnsTrue_caseDetailsReturned() {
            when(caseAccessService.canUserAccess(any())).thenReturn(true);
            assertCaseDetailsPresent(true, true,JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);
            verify(caseAccessService).canUserAccess(same(caseDetails));
        }

        @Test
        @DisplayName("Should return an empty optional if case is not visible")
        void searchOperationReturnsCaseDetails_solicitorVisibilityServiceCalledForCaseDetailsAndReturnsFalse_caseDetailsReturned() {
            when(caseAccessService.canUserAccess(any())).thenReturn(false);
            assertCaseDetailsPresent(false, true,JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);
            verify(caseAccessService).canUserAccess(same(caseDetails));
        }

        @Test
        @DisplayName("Should return an empty optional when GetCaseOperation returns an empty optional")
        void searchOperationReturnsCaseDetails_GetCaseOperationReturnsEmptyOptional_caseDetailsReturned() {
            when(getCaseOperation.execute(any(),any(),any())).thenReturn(Optional.empty());
            assertCaseDetailsPresent(false, false, JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);
        }

    }

    @Nested
    @DisplayName("execute(String caseReference)")
    class ContemporaryMethod {

        @Test
        @DisplayName("Should return an Optional containing the case if the case is visible")
        void searchOperationReturnsCaseDetails_solicitorVisibilityServiceCalledForCaseDetailsAndReturnsTrue_caseDetailsReturned() {
            when(caseAccessService.canUserAccess(any())).thenReturn(true);
            assertCaseDetailsPresent(true, true, JURISDICTION_ID);
        }

        @Test
        @DisplayName("Should return an empty optional if case is not visible")
        void searchOperationReturnsCaseDetails_solicitorVisibilityServiceCalledForCaseDetailsAndReturnsFalse_caseDetailsReturned() {
            when(caseAccessService.canUserAccess(any())).thenReturn(false);
            assertCaseDetailsPresent(false, true, JURISDICTION_ID);
        }

        @Test
        @DisplayName("Should return an empty optional when GetCaseOperation returns an empty optional")
        void searchOperationReturnsCaseDetails_GetCaseOperationReturnsEmptyOptional_caseDetailsReturned() {
            when(getCaseOperation.execute(any())).thenReturn(Optional.empty());
            assertCaseDetailsPresent(false,false, JURISDICTION_ID);
        }

    }

    private void assertCaseDetailsPresent(boolean isPresent, boolean solicitorVisibilityServiceCalled, String... args)  {

        Optional<CaseDetails> result = (args.length == 1)
            ? classUnderTest.execute(args[0])
                : classUnderTest.execute(args[0], args[1], args[2]);

        assertEquals(isPresent, result.isPresent());

        if (isPresent) {
            assertTrue(result.get() == caseDetails);
        }

        if (args.length == 1) {
            verify(getCaseOperation).execute(same(JURISDICTION_ID));
        } else {
            verify(getCaseOperation).execute(same(JURISDICTION_ID), same(CASE_TYPE_ID), same(CASE_REFERENCE));
        }

        if (solicitorVisibilityServiceCalled) {
            verify(caseAccessService).canUserAccess(same(caseDetails));
        }

    }

}
