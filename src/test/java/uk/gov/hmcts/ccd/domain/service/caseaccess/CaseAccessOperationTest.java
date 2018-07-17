package uk.gov.hmcts.ccd.domain.service.caseaccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CaseAccessOperationTest {

    private static final String JURISDICTION = "CMC";
    private static final String WRONG_JURISDICTION = "DIVORCE";
    private static final Long CASE_REFERENCE = 1234123412341236L;
    private static final String USER_ID = "123";
    private static final Long CASE_ID = 456L;
    private static final Long CASE_NOT_FOUND = 9999999999999999L;

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private CaseUserRepository caseUserRepository;

    @InjectMocks
    private CaseAccessOperation caseAccessOperation;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        configureCaseRepository();
    }

    @Nested
    @DisplayName("grantAccess()")
    class GrantAccess {

        @Test
        @DisplayName("should grant access to user")
        void shouldGrantAccess() {
            caseAccessOperation.grantAccess(JURISDICTION, CASE_REFERENCE.toString(), USER_ID);

            assertAll(
                () -> verify(caseDetailsRepository).findByReference(JURISDICTION, CASE_REFERENCE),
                () -> verify(caseUserRepository).grantAccess(CASE_ID, USER_ID)
            );
        }

        @Test
        @DisplayName("should throw not found exception when reference not found")
        void shouldThrowNotFound() {
            assertAll(
                () -> assertThrows(CaseNotFoundException.class, () -> {
                    caseAccessOperation.grantAccess(JURISDICTION, CASE_NOT_FOUND.toString(), USER_ID);
                }),
                () -> verify(caseDetailsRepository).findByReference(JURISDICTION, CASE_NOT_FOUND),
                () -> verify(caseUserRepository, never()).grantAccess(CASE_ID, USER_ID)
            );
        }

        @Test
        @DisplayName("should throw not found exception when reference in different jurisdiction")
        void shouldHandleWrongJurisdiction() {
            assertAll(
                () -> assertThrows(CaseNotFoundException.class, () -> {
                    caseAccessOperation.grantAccess(WRONG_JURISDICTION, CASE_REFERENCE.toString(), USER_ID);
                }),
                () -> verify(caseDetailsRepository).findByReference(WRONG_JURISDICTION, CASE_REFERENCE),
                () -> verify(caseUserRepository, never()).grantAccess(CASE_ID, USER_ID)
            );
        }
    }

    @Nested
    @DisplayName("revokeAccess()")
    class RevokeAccess {

        @Test
        @DisplayName("should revoke access to user")
        void shouldRevokeAccess() {
            caseAccessOperation.revokeAccess(JURISDICTION, CASE_REFERENCE.toString(), USER_ID);

            assertAll(
                () -> verify(caseDetailsRepository).findByReference(JURISDICTION, CASE_REFERENCE),
                () -> verify(caseUserRepository).revokeAccess(CASE_ID, USER_ID)
            );
        }

        @Test
        @DisplayName("should throw not found exception when reference not found")
        void shouldThrowNotFound() {
            assertAll(
                () -> assertThrows(CaseNotFoundException.class, () -> {
                    caseAccessOperation.revokeAccess(JURISDICTION, CASE_NOT_FOUND.toString(), USER_ID);
                }),
                () -> verify(caseDetailsRepository).findByReference(JURISDICTION, CASE_NOT_FOUND),
                () -> verify(caseUserRepository, never()).revokeAccess(CASE_ID, USER_ID)
            );
        }

        @Test
        @DisplayName("should throw not found exception when reference in different jurisdiction")
        void shouldHandleWrongJurisdiction() {
            assertAll(
                () -> assertThrows(CaseNotFoundException.class, () -> {
                    caseAccessOperation.revokeAccess(WRONG_JURISDICTION, CASE_REFERENCE.toString(), USER_ID);
                }),
                () -> verify(caseDetailsRepository).findByReference(WRONG_JURISDICTION, CASE_REFERENCE),
                () -> verify(caseUserRepository, never()).revokeAccess(CASE_ID, USER_ID)
            );
        }
    }

    private void configureCaseRepository() {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(String.valueOf(CASE_ID));
        caseDetails.setReference(CASE_REFERENCE);

        doReturn(Optional.of(caseDetails)).when(caseDetailsRepository)
                                          .findByReference(JURISDICTION, CASE_REFERENCE);
        doReturn(Optional.empty()).when(caseDetailsRepository)
                                  .findByReference(JURISDICTION, CASE_NOT_FOUND);
        doReturn(Optional.empty()).when(caseDetailsRepository)
                                  .findByReference(WRONG_JURISDICTION, CASE_REFERENCE);
    }

}
