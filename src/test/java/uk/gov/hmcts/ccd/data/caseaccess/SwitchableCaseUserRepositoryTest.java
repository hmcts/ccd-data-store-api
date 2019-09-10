package uk.gov.hmcts.ccd.data.caseaccess;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;

class SwitchableCaseUserRepositoryTest {

    private static final String JURISDICTION = "CMC";
    private static final String WRONG_JURISDICTION = "DIVORCE";
    private static final String CCD_CASE_TYPE_ID = "Application";
    private static final String AM_CASE_TYPE_ID = "Grant";
    private static final String BOTH_CASE_TYPE_ID = "Divorce";
    private static final Long CCD_CASE_REFERENCE = 1234123412341236L;
    private static final Long CCD_CASE_REFERENCE_2 = 1234123412341239L;
    private static final Long CCD_CASE_REFERENCE_3 = 1234123412341210L;
    private static final Long AM_CASE_REFERENCE = 1234123412341237L;
    private static final Long AM_CASE_REFERENCE_2 = 1234123412341231L;
    private static final Long BOTH_CASE_REFERENCE = 1234123412341238L;
    private static final Long BOTH_CASE_REFERENCE_3 = 1234123412341212L;
    private static final String USER_ID = "123";
    private static final Long CCD_CASE_ID = 456L;
    private static final Long CCD_CASE_ID_2 = 346L;
    private static final Long CCD_CASE_ID_3 = 246L;
    private static final Long AM_CASE_ID = 789L;
    private static final Long AM_CASE_ID_2 = 781L;
    private static final Long BOTH_CASE_ID = 123L;
    private static final Long BOTH_CASE_ID_3 = 125L;
    private static final Long CASE_NOT_FOUND = 9999999999999999L;
    private static final String CASE_ROLE_GRANTED = "[ALREADY_GRANTED]";

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private CCDCaseUserRepository ccdCaseUserRepository;

    @Mock
    private AMCaseUserRepository amCaseUserRepository;

    @Mock
    private AMSwitch amSwitch;

    @InjectMocks
    private SwitchableCaseUserRepository caseUserRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        configureCaseUserRepository();
        caseUserRepository = new SwitchableCaseUserRepository(caseDetailsRepository, ccdCaseUserRepository, amCaseUserRepository, amSwitch);
    }

    @Nested
    @DisplayName("grantAccess()")
    class GrantAccess {

        @Nested
        @DisplayName("AM to CCD")
        class AMToCCD {

            @BeforeEach
            void setUp() {
                configureCaseRepository(CCD_CASE_REFERENCE, CCD_CASE_ID, CCD_CASE_TYPE_ID);
                doReturn(true).when(amSwitch).isWriteAccessManagementWithCCD(CCD_CASE_TYPE_ID);
            }

            @Test
            @DisplayName("should invoke services in order for granting access to user")
            void shouldInvokeServicesInOrder() {
                caseUserRepository.grantAccess(JURISDICTION, CCD_CASE_TYPE_ID, CCD_CASE_REFERENCE.toString(), CCD_CASE_ID, USER_ID, CREATOR.getRole());

                InOrder inOrder = inOrder(caseDetailsRepository, amSwitch, ccdCaseUserRepository, amCaseUserRepository);
                assertAll(
                    () -> inOrder.verify(amSwitch).isWriteAccessManagementWithCCD(CCD_CASE_TYPE_ID),
                    () -> inOrder.verify(ccdCaseUserRepository).grantAccess(JURISDICTION, CCD_CASE_TYPE_ID, CCD_CASE_REFERENCE.toString(), CCD_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> inOrder.verify(amSwitch).isWriteAccessManagementWithAM(CCD_CASE_TYPE_ID),
                    () -> verifyNoMoreInteractions(ccdCaseUserRepository),
                    () -> verifyZeroInteractions(amCaseUserRepository)
                );
            }
        }

        @Nested
        @DisplayName("AM to AM")
        class AMToAM {

            @BeforeEach
            void setUp() {
                configureCaseRepository(AM_CASE_REFERENCE, AM_CASE_ID, AM_CASE_TYPE_ID);
                doReturn(true).when(amSwitch).isWriteAccessManagementWithAM(AM_CASE_TYPE_ID);
            }

            @Test
            @DisplayName("should invoke services in order for granting access to user")
            void shouldInvokeServicesInOrder() {
                caseUserRepository.grantAccess(JURISDICTION, AM_CASE_TYPE_ID, AM_CASE_REFERENCE.toString(), AM_CASE_ID, USER_ID, CREATOR.getRole());

                InOrder inOrder = inOrder(caseDetailsRepository, amSwitch, ccdCaseUserRepository, amCaseUserRepository);
                assertAll(
                    () -> inOrder.verify(amSwitch).isWriteAccessManagementWithCCD(AM_CASE_TYPE_ID),
                    () -> inOrder.verify(amSwitch).isWriteAccessManagementWithAM(AM_CASE_TYPE_ID),
                    () -> inOrder.verify(amCaseUserRepository).grantAccess(JURISDICTION, AM_CASE_TYPE_ID, AM_CASE_REFERENCE.toString(), AM_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> verifyNoMoreInteractions(amCaseUserRepository),
                    () -> verifyZeroInteractions(ccdCaseUserRepository)
                );
            }
        }

        @Nested
        @DisplayName("AM to both")
        class AMToBoth {

            @BeforeEach
            void setUp() {
                configureCaseRepository(BOTH_CASE_REFERENCE, BOTH_CASE_ID, BOTH_CASE_TYPE_ID);
                doReturn(true).when(amSwitch).isWriteAccessManagementWithCCD(BOTH_CASE_TYPE_ID);
                doReturn(true).when(amSwitch).isWriteAccessManagementWithAM(BOTH_CASE_TYPE_ID);
            }

            @Test
            @DisplayName("should invoke services in order for granting access to user")
            void shouldInvokeServicesInOrder() {
                caseUserRepository.grantAccess(JURISDICTION, BOTH_CASE_TYPE_ID, BOTH_CASE_REFERENCE.toString(), BOTH_CASE_ID, USER_ID, CREATOR.getRole());

                InOrder inOrder = inOrder(caseDetailsRepository, amSwitch, ccdCaseUserRepository, amCaseUserRepository);
                assertAll(
                    () -> inOrder.verify(amSwitch).isWriteAccessManagementWithCCD(BOTH_CASE_TYPE_ID),
                    () -> inOrder.verify(ccdCaseUserRepository).grantAccess(JURISDICTION, BOTH_CASE_TYPE_ID, BOTH_CASE_REFERENCE.toString(), BOTH_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> inOrder.verify(amSwitch).isWriteAccessManagementWithAM(BOTH_CASE_TYPE_ID),
                    () -> inOrder.verify(amCaseUserRepository).grantAccess(JURISDICTION, BOTH_CASE_TYPE_ID, BOTH_CASE_REFERENCE.toString(), BOTH_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> inOrder.verifyNoMoreInteractions(),
                    () -> verifyNoMoreInteractions(amCaseUserRepository),
                    () -> verifyNoMoreInteractions(ccdCaseUserRepository)
                );
            }
        }
    }

    @Nested
    @DisplayName("revokeAccess()")
    class RevokeAccess {
        private CaseDetails caseDetails;

        @Nested
        @DisplayName("AM to CCD")
        class AMToCCD {

            @BeforeEach
            void setUp() {
                caseDetails = configureCaseRepository(CCD_CASE_REFERENCE, CCD_CASE_ID, CCD_CASE_TYPE_ID);
                doReturn(true).when(amSwitch).isWriteAccessManagementWithCCD(CCD_CASE_TYPE_ID);
            }

            @Test
            @DisplayName("should revoke access to user")
            void shouldRevokeAccess() {
                caseUserRepository.revokeAccess(JURISDICTION, CCD_CASE_TYPE_ID, CCD_CASE_REFERENCE.toString(), CCD_CASE_ID, USER_ID, CREATOR.getRole());

                assertAll(
                    () -> verify(amSwitch).isWriteAccessManagementWithCCD(CCD_CASE_TYPE_ID),
                    () -> verify(ccdCaseUserRepository).revokeAccess(JURISDICTION, CCD_CASE_TYPE_ID, CCD_CASE_REFERENCE.toString(), CCD_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> verifyZeroInteractions(amCaseUserRepository)
                );
            }

            @Test
            @DisplayName("should invoke services in order for revoking access to user")
            void shouldInvokeServicesInOrder() {
                caseUserRepository.revokeAccess(JURISDICTION, CCD_CASE_TYPE_ID, CCD_CASE_REFERENCE.toString(), CCD_CASE_ID, USER_ID, CREATOR.getRole());

                InOrder inOrder = inOrder(caseDetailsRepository, amSwitch, ccdCaseUserRepository, amCaseUserRepository);
                assertAll(
                    () -> inOrder.verify(amSwitch).isWriteAccessManagementWithCCD(CCD_CASE_TYPE_ID),
                    () -> inOrder.verify(ccdCaseUserRepository).revokeAccess(JURISDICTION, CCD_CASE_TYPE_ID, CCD_CASE_REFERENCE.toString(), CCD_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> inOrder.verify(amSwitch).isWriteAccessManagementWithAM(CCD_CASE_TYPE_ID),
                    inOrder::verifyNoMoreInteractions,
                    () -> verifyNoMoreInteractions(ccdCaseUserRepository),
                    () -> verifyZeroInteractions(amCaseUserRepository)
                );
            }
        }

        @Nested
        @DisplayName("AM to AM")
        class AMToAM {

            @BeforeEach
            void setUp() {
                caseDetails = configureCaseRepository(AM_CASE_REFERENCE, AM_CASE_ID, AM_CASE_TYPE_ID);
                doReturn(true).when(amSwitch).isWriteAccessManagementWithAM(AM_CASE_TYPE_ID);
            }

            @Test
            @DisplayName("should invoke services in order for revoking access to user")
            void shouldInvokeServicesInOrder() {
                caseUserRepository.revokeAccess(JURISDICTION, AM_CASE_TYPE_ID, AM_CASE_REFERENCE.toString(), AM_CASE_ID, USER_ID, CREATOR.getRole());

                InOrder inOrder = inOrder(caseDetailsRepository, amSwitch, ccdCaseUserRepository, amCaseUserRepository);
                assertAll(
                    () -> inOrder.verify(amSwitch).isWriteAccessManagementWithCCD(AM_CASE_TYPE_ID),
                    () -> inOrder.verify(amSwitch).isWriteAccessManagementWithAM(AM_CASE_TYPE_ID),
                    () -> inOrder.verify(amCaseUserRepository).revokeAccess(JURISDICTION, AM_CASE_TYPE_ID, AM_CASE_REFERENCE.toString(), AM_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> inOrder.verifyNoMoreInteractions(),
                    () -> verifyNoMoreInteractions(amCaseUserRepository),
                    () -> verifyZeroInteractions(ccdCaseUserRepository)
                );
            }
        }

        @Nested
        @DisplayName("AM to both")
        class AMToBoth {

            @BeforeEach
            void setUp() {
                caseDetails = configureCaseRepository(BOTH_CASE_REFERENCE, BOTH_CASE_ID, BOTH_CASE_TYPE_ID);
                doReturn(true).when(amSwitch).isWriteAccessManagementWithCCD(BOTH_CASE_TYPE_ID);
                doReturn(true).when(amSwitch).isWriteAccessManagementWithAM(BOTH_CASE_TYPE_ID);
            }

            @Test
            @DisplayName("should invoke services in order for revoking access to user")
            void shouldInvokeServicesInOrder() {
                caseUserRepository.revokeAccess(JURISDICTION, BOTH_CASE_TYPE_ID, BOTH_CASE_REFERENCE.toString(), BOTH_CASE_ID, USER_ID, CREATOR.getRole());

                InOrder inOrder = inOrder(caseDetailsRepository, amSwitch, ccdCaseUserRepository, amCaseUserRepository);
                assertAll(
                    () -> inOrder.verify(amSwitch).isWriteAccessManagementWithCCD(BOTH_CASE_TYPE_ID),
                    () -> inOrder.verify(ccdCaseUserRepository).revokeAccess(JURISDICTION, BOTH_CASE_TYPE_ID, BOTH_CASE_REFERENCE.toString(), BOTH_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> inOrder.verify(amSwitch).isWriteAccessManagementWithAM(BOTH_CASE_TYPE_ID),
                    () -> inOrder.verify(amCaseUserRepository).revokeAccess(JURISDICTION, BOTH_CASE_TYPE_ID, BOTH_CASE_REFERENCE.toString(), BOTH_CASE_ID, USER_ID, CREATOR.getRole()),
                    inOrder::verifyNoMoreInteractions,
                    () -> verifyNoMoreInteractions(amCaseUserRepository),
                    () -> verifyNoMoreInteractions(ccdCaseUserRepository)
                );
            }
        }
    }

    @Nested
    @DisplayName("findCasesUserIdHasAccessTo(userId)")
    class FindCasesUserHasAccessTo {

        @Nested
        @DisplayName("AM to both")
        class AMToBoth {

            @BeforeEach
            void setUp() {
                MockitoAnnotations.initMocks(this);

                configureCaseRepository(CCD_CASE_REFERENCE, CCD_CASE_ID, CCD_CASE_TYPE_ID);
                configureCaseRepository(AM_CASE_REFERENCE_2, AM_CASE_ID_2, AM_CASE_TYPE_ID);
                configureCaseRepository(CCD_CASE_REFERENCE_3, CCD_CASE_ID_3, CCD_CASE_TYPE_ID);

                configureCaseRepository(AM_CASE_REFERENCE, AM_CASE_ID, AM_CASE_TYPE_ID);
                configureCaseRepository(CCD_CASE_REFERENCE_2, CCD_CASE_ID_2, CCD_CASE_TYPE_ID);
                configureCaseRepository(BOTH_CASE_REFERENCE_3, BOTH_CASE_ID_3, BOTH_CASE_TYPE_ID);

                doReturn(true).when(amSwitch).isReadAccessManagementWithCCD(CCD_CASE_TYPE_ID);
                doReturn(true).when(amSwitch).isReadAccessManagementWithAM(AM_CASE_TYPE_ID);
            }

            @Test
            @DisplayName("should find cases user has access to")
            void shouldFindCasesUserHasAccessTo() {
                List<Long> casesUserIdHasAccessTo = caseUserRepository.findCasesUserIdHasAccessTo(USER_ID);

                assertAll(
                    () -> verify(ccdCaseUserRepository).findCasesUserIdHasAccessTo(USER_ID),
                    () -> verify(amCaseUserRepository).findCasesUserIdHasAccessTo(USER_ID),
                    () -> assertThat(casesUserIdHasAccessTo, hasItems(CCD_CASE_ID, CCD_CASE_ID_3, AM_CASE_ID))
                );
            }

            @Test
            @DisplayName("should propagate exception to caller if thrown")
            void shouldPropagateExceptionToCallerIfThrown() {
                doThrow(ApiException.class).when(amCaseUserRepository).findCasesUserIdHasAccessTo(USER_ID);
                assertThrows(ApiException.class, () -> caseUserRepository.findCasesUserIdHasAccessTo(USER_ID),
                    "Cases lookup should have failed");
            }

            @Test
            @DisplayName("should invoke services in order to find cases user id has access to")
            void shouldInvokeServicesInOrder() {
                caseUserRepository.findCasesUserIdHasAccessTo(USER_ID);

                InOrder inOrder = inOrder(amSwitch, ccdCaseUserRepository, amCaseUserRepository, caseDetailsRepository);
                assertAll(
                    () -> inOrder.verify(ccdCaseUserRepository).findCasesUserIdHasAccessTo(USER_ID),
                    () -> inOrder.verify(caseDetailsRepository).findById(CCD_CASE_ID),
                    () -> inOrder.verify(amSwitch).isReadAccessManagementWithCCD(CCD_CASE_TYPE_ID),
                    () -> inOrder.verify(caseDetailsRepository).findById(AM_CASE_ID_2),
                    () -> inOrder.verify(amSwitch).isReadAccessManagementWithCCD(AM_CASE_TYPE_ID),
                    () -> inOrder.verify(amSwitch).isReadAccessManagementWithAM(AM_CASE_TYPE_ID),
                    () -> inOrder.verify(caseDetailsRepository).findById(CCD_CASE_ID_3),
                    () -> inOrder.verify(amSwitch).isReadAccessManagementWithCCD(CCD_CASE_TYPE_ID),

                    () -> inOrder.verify(amCaseUserRepository).findCasesUserIdHasAccessTo(USER_ID),
                    () -> inOrder.verify(caseDetailsRepository).findById(AM_CASE_ID),
                    () -> inOrder.verify(amSwitch).isReadAccessManagementWithCCD(AM_CASE_TYPE_ID),
                    () -> inOrder.verify(amSwitch).isReadAccessManagementWithAM(AM_CASE_TYPE_ID),
                    () -> inOrder.verify(caseDetailsRepository).findById(CCD_CASE_ID_2),
                    () -> inOrder.verify(amSwitch).isReadAccessManagementWithCCD(CCD_CASE_TYPE_ID),
                    () -> inOrder.verify(amSwitch).isReadAccessManagementWithAM(CCD_CASE_TYPE_ID),
                    () -> inOrder.verify(caseDetailsRepository).findById(BOTH_CASE_ID_3),
                    () -> inOrder.verify(amSwitch).isReadAccessManagementWithCCD(BOTH_CASE_TYPE_ID),
                    () -> inOrder.verify(amSwitch).isReadAccessManagementWithAM(BOTH_CASE_TYPE_ID),

                    inOrder::verifyNoMoreInteractions,
                    () -> verifyNoMoreInteractions(ccdCaseUserRepository),
                    () -> verifyNoMoreInteractions(amCaseUserRepository),
                    () -> verifyNoMoreInteractions(caseDetailsRepository)
                );
            }
        }
    }

    private CaseDetails configureCaseRepository(Long caseReference, Long caseId, String caseTypeId) {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(String.valueOf(caseId));
        caseDetails.setReference(caseReference);
        caseDetails.setCaseTypeId(caseTypeId);

        doReturn(Optional.of(caseDetails)).when(caseDetailsRepository)
            .findByReference(JURISDICTION, caseReference);
        doReturn(Optional.empty()).when(caseDetailsRepository)
            .findByReference(JURISDICTION, CASE_NOT_FOUND);
        doReturn(Optional.empty()).when(caseDetailsRepository)
            .findByReference(WRONG_JURISDICTION, caseReference);
        doReturn(caseDetails).when(caseDetailsRepository).findById(caseId);
        return caseDetails;
    }

    private void configureCaseUserRepository() {
        when(ccdCaseUserRepository.findCaseRoles(CCD_CASE_TYPE_ID, CCD_CASE_ID,
            USER_ID)).thenReturn(Collections.singletonList(CASE_ROLE_GRANTED));
        when(ccdCaseUserRepository.findCaseRoles(CCD_CASE_TYPE_ID, BOTH_CASE_ID,
            USER_ID)).thenReturn(Collections.singletonList(CASE_ROLE_GRANTED));
        when(amCaseUserRepository.findCaseRoles(AM_CASE_TYPE_ID, AM_CASE_ID,
            USER_ID)).thenReturn(Collections.singletonList(CASE_ROLE_GRANTED));
        when(amCaseUserRepository.findCaseRoles(AM_CASE_TYPE_ID, BOTH_CASE_ID,
            USER_ID)).thenReturn(Collections.singletonList(CASE_ROLE_GRANTED));
        when(ccdCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID)).thenReturn(Lists.newArrayList(CCD_CASE_ID, AM_CASE_ID_2, CCD_CASE_ID_3));
        when(amCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID)).thenReturn(Lists.newArrayList(AM_CASE_ID, CCD_CASE_ID_2, BOTH_CASE_ID_3));
    }
}
