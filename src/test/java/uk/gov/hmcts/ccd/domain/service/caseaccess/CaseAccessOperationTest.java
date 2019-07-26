package uk.gov.hmcts.ccd.domain.service.caseaccess;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.caseaccess.CaseRoleRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.SwitchableCaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.InvalidCaseRoleException;
import uk.gov.hmcts.ccd.v2.external.domain.CaseUser;

import java.util.Arrays;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;

class CaseAccessOperationTest {

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
    private static final Long AM_CASE_REFERENCE_3 = 1234123412341232L;
    private static final Long BOTH_CASE_REFERENCE = 1234123412341238L;
    private static final Long BOTH_CASE_REFERENCE_2 = 1234123412341211L;
    private static final Long BOTH_CASE_REFERENCE_3 = 1234123412341212L;
    private static final String USER_ID = "123";
    private static final Long CCD_CASE_ID = 456L;
    private static final Long CCD_CASE_ID_2 = 346L;
    private static final Long CCD_CASE_ID_3 = 246L;
    private static final Long AM_CASE_ID = 789L;
    private static final Long AM_CASE_ID_2 = 781L;
    private static final Long AM_CASE_ID_3 = 782L;
    private static final Long BOTH_CASE_ID = 123L;
    private static final Long BOTH_CASE_ID_2 = 124L;
    private static final Long BOTH_CASE_ID_3 = 125L;
    private static final Long CASE_NOT_FOUND = 9999999999999999L;
    private static final String NOT_CASE_ROLE = "NotACaseRole";
    private static final String CASE_ROLE = "[DEFENDANT]";
    private static final String CASE_ROLE_OTHER = "[OTHER]";
    private static final String CASE_ROLE_GRANTED = "[ALREADY_GRANTED]";
    private static final String CCD_TYPE = "ccd";
    private static final String AM_TYPE = "am";

    private List<CaseUserRepository> ccdCaseUserRepositories;
    private List<CaseUserRepository> amCaseUserRepositories;
    private List<CaseUserRepository> bothCaseUserRepositories;

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private CaseUserRepository ccdCaseUserRepository;

    @Mock
    private CaseUserRepository amCaseUserRepository;

    @Mock
    private SwitchableCaseUserRepository switchableCaseUserRepository;

    @Mock
    private CaseRoleRepository caseRoleRepository;

    @InjectMocks
    private CaseAccessOperation caseAccessOperation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        ccdCaseUserRepositories = Lists.newArrayList();
        amCaseUserRepositories = Lists.newArrayList();
        bothCaseUserRepositories = Lists.newArrayList();

        configureSwitchableCaseUserRepository();
        configureCaseUserRepository();
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
                configureCaseRoleRepository(CCD_CASE_TYPE_ID);
            }

            @Test
            @DisplayName("should throw not found exception when reference not found")
            void shouldThrowNotFound() {
                assertAll(
                    () -> assertThrows(CaseNotFoundException.class, () -> {
                        caseAccessOperation.grantAccess(JURISDICTION, CASE_NOT_FOUND.toString(), USER_ID);
                    }),
                    () -> verify(caseDetailsRepository).findByReference(JURISDICTION, CASE_NOT_FOUND),
                    () -> verifyZeroInteractions(switchableCaseUserRepository),
                    () -> verifyZeroInteractions(ccdCaseUserRepository),
                    () -> verifyZeroInteractions(amCaseUserRepository)
                );
            }

            @Test
            @DisplayName("should throw not found exception when reference in different jurisdiction")
            void shouldHandleWrongJurisdiction() {
                assertAll(
                    () -> assertThrows(CaseNotFoundException.class, () -> {
                        caseAccessOperation.grantAccess(WRONG_JURISDICTION, CCD_CASE_REFERENCE.toString(), USER_ID);
                    }),
                    () -> verify(caseDetailsRepository).findByReference(WRONG_JURISDICTION, CCD_CASE_REFERENCE),
                    () -> verifyZeroInteractions(switchableCaseUserRepository),
                    () -> verifyZeroInteractions(ccdCaseUserRepository),
                    () -> verifyZeroInteractions(amCaseUserRepository)
                );
            }

            @Test
            @DisplayName("should invoke services in order for granting access to user")
            void shouldInvokeServicesInOrder() {
                caseAccessOperation.grantAccess(JURISDICTION, CCD_CASE_REFERENCE.toString(), USER_ID);

                InOrder inOrder = inOrder(caseDetailsRepository, switchableCaseUserRepository, ccdCaseUserRepository, amCaseUserRepository);
                assertAll(
                    () -> inOrder.verify(caseDetailsRepository).findByReference(JURISDICTION, CCD_CASE_REFERENCE),
                    () -> inOrder.verify(switchableCaseUserRepository).forWriting(CCD_CASE_TYPE_ID),
                    () -> inOrder.verify(ccdCaseUserRepository).grantAccess(CCD_CASE_ID, USER_ID, CREATOR.getRole()),
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
                configureCaseRoleRepository(AM_CASE_TYPE_ID);
            }

            @Test
            @DisplayName("should invoke services in order for granting access to user")
            void shouldInvokeServicesInOrder() {
                caseAccessOperation.grantAccess(JURISDICTION, AM_CASE_REFERENCE.toString(), USER_ID);

                InOrder inOrder = inOrder(caseDetailsRepository, switchableCaseUserRepository, ccdCaseUserRepository, amCaseUserRepository);
                assertAll(
                    () -> inOrder.verify(caseDetailsRepository).findByReference(JURISDICTION, AM_CASE_REFERENCE),
                    () -> inOrder.verify(switchableCaseUserRepository).forWriting(AM_CASE_TYPE_ID),
                    () -> inOrder.verify(amCaseUserRepository).grantAccess(AM_CASE_ID, USER_ID, CREATOR.getRole()),
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
                configureCaseRoleRepository(BOTH_CASE_TYPE_ID);
            }

            @Test
            @DisplayName("should invoke services in order for granting access to user")
            void shouldInvokeServicesInOrder() {
                caseAccessOperation.grantAccess(JURISDICTION, BOTH_CASE_REFERENCE.toString(), USER_ID);

                InOrder inOrder = inOrder(caseDetailsRepository, switchableCaseUserRepository, ccdCaseUserRepository, amCaseUserRepository);
                assertAll(
                    () -> inOrder.verify(caseDetailsRepository).findByReference(JURISDICTION, BOTH_CASE_REFERENCE),
                    () -> inOrder.verify(switchableCaseUserRepository).forWriting(BOTH_CASE_TYPE_ID),
                    () -> inOrder.verify(ccdCaseUserRepository).grantAccess(BOTH_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> inOrder.verify(amCaseUserRepository).grantAccess(BOTH_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> inOrder.verifyNoMoreInteractions(),
                    () -> verifyNoMoreInteractions(amCaseUserRepository),
                    () -> verifyNoMoreInteractions(ccdCaseUserRepository)
                );
            }
        }
    }

    @Nested()
    @DisplayName("updateUserAccess(reference, caseUser)")
    class GrantAccessCaseUser {
        private CaseDetails caseDetails;

        @Nested
        @DisplayName("AM to CCD")
        class AMToCCD {

            @BeforeEach
            void setUp() {
                caseDetails = configureCaseRepository(CCD_CASE_REFERENCE, CCD_CASE_ID, CCD_CASE_TYPE_ID);
                configureCaseRoleRepository(CCD_CASE_TYPE_ID);
            }

            @Test
            @DisplayName("should reject update when it contains an unknown case role")
            void shouldRejectWhenUnknownCaseRoles() {
                final Executable execAccessUpdate = () -> caseAccessOperation.updateUserAccess(caseDetails,
                    caseUser(NOT_CASE_ROLE));
                assertThrows(InvalidCaseRoleException.class, execAccessUpdate);
                verifyZeroInteractions(switchableCaseUserRepository);
                verifyZeroInteractions(ccdCaseUserRepository);
                verifyZeroInteractions(amCaseUserRepository);
            }

            @Test
            @DisplayName("should grant access when added case role valid")
            void shouldGrantAccessForCaseRole() {
                caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE));

                verify(ccdCaseUserRepository).grantAccess(CCD_CASE_ID, USER_ID, CASE_ROLE);
                verifyZeroInteractions(amCaseUserRepository);
            }

            @Test
            @DisplayName("should grant access when added case roles contains global [CREATOR]")
            void shouldGrantAccessForCaseRoleCreator() {
                caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE, CREATOR.getRole()));

                assertAll(
                    () -> verify(ccdCaseUserRepository).grantAccess(CCD_CASE_ID, USER_ID, CASE_ROLE),
                    () -> verify(ccdCaseUserRepository).grantAccess(CCD_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> verifyZeroInteractions(amCaseUserRepository)
                );
            }

            @Test
            @DisplayName("should revoke access for removed case roles")
            void shouldRevokeRemovedCaseRoles() {
                caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE));

                verify(ccdCaseUserRepository).revokeAccess(CCD_CASE_ID, USER_ID, CASE_ROLE_GRANTED);
                verifyZeroInteractions(amCaseUserRepository);
            }

            @Test
            @DisplayName("should ignore case roles already granted")
            void shouldIgnoreGrantedCaseRoles() {
                caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE_GRANTED));

                verify(ccdCaseUserRepository, never()).grantAccess(CCD_CASE_ID, USER_ID, CASE_ROLE_GRANTED);
                verifyZeroInteractions(amCaseUserRepository);
            }

            @Test
            @DisplayName("should invoke services in order for two new roles granting and one revoking")
            void shouldInvokeServicesInOrder() {
                caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE, CREATOR.getRole()));

                InOrder inOrder = inOrder(caseRoleRepository, switchableCaseUserRepository, ccdCaseUserRepository, amCaseUserRepository);
                assertAll(
                    () -> inOrder.verify(caseRoleRepository).getCaseRoles(CCD_CASE_TYPE_ID),
                    () -> inOrder.verify(switchableCaseUserRepository).forReading(CCD_CASE_TYPE_ID),
                    () -> inOrder.verify(ccdCaseUserRepository).findCaseRoles(CCD_CASE_ID, USER_ID),
                    () -> inOrder.verify(switchableCaseUserRepository).forWriting(CCD_CASE_TYPE_ID),
                    () -> inOrder.verify(ccdCaseUserRepository).grantAccess(CCD_CASE_ID, USER_ID, CASE_ROLE),
                    () -> inOrder.verify(ccdCaseUserRepository).grantAccess(CCD_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> inOrder.verify(switchableCaseUserRepository).forWriting(CCD_CASE_TYPE_ID),
                    () -> inOrder.verify(ccdCaseUserRepository).revokeAccess(CCD_CASE_ID, USER_ID, CASE_ROLE_GRANTED),
                    inOrder::verifyNoMoreInteractions,
                    () -> verifyNoMoreInteractions(caseRoleRepository),
                    () -> verifyNoMoreInteractions(switchableCaseUserRepository),
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
                configureCaseRoleRepository(AM_CASE_TYPE_ID);
            }

            @Test
            @DisplayName("should grant access when added case role valid")
            void shouldGrantAccessForCaseRole() {
                caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE));

                verify(amCaseUserRepository).grantAccess(AM_CASE_ID, USER_ID, CASE_ROLE);
                verifyZeroInteractions(ccdCaseUserRepository);
            }

            @Test
            @DisplayName("should grant access when added case roles contains global [CREATOR]")
            void shouldGrantAccessForCaseRoleCreator() {
                caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE, CREATOR.getRole()));

                assertAll(
                    () -> verify(amCaseUserRepository).grantAccess(AM_CASE_ID, USER_ID, CASE_ROLE),
                    () -> verify(amCaseUserRepository).grantAccess(AM_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> verifyZeroInteractions(ccdCaseUserRepository)
                );
            }

            @Test
            @DisplayName("should revoke access for removed case roles")
            void shouldRevokeRemovedCaseRoles() {
                caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE));

                verify(amCaseUserRepository).revokeAccess(AM_CASE_ID, USER_ID, CASE_ROLE_GRANTED);
                verifyZeroInteractions(ccdCaseUserRepository);
            }

            @Test
            @DisplayName("should ignore case roles already granted")
            void shouldIgnoreGrantedCaseRoles() {
                caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE_GRANTED));

                verify(amCaseUserRepository, never()).grantAccess(AM_CASE_ID, USER_ID, CASE_ROLE_GRANTED);
                verifyZeroInteractions(ccdCaseUserRepository);
            }

            @Test
            @DisplayName("should invoke services in order for two new roles granting and one revoking")
            void shouldInvokeServicesInOrder() {
                caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE, CREATOR.getRole()));

                InOrder inOrder = inOrder(caseRoleRepository, switchableCaseUserRepository, ccdCaseUserRepository, amCaseUserRepository);
                assertAll(
                    () -> inOrder.verify(caseRoleRepository).getCaseRoles(AM_CASE_TYPE_ID),
                    () -> inOrder.verify(switchableCaseUserRepository).forReading(AM_CASE_TYPE_ID),
                    () -> inOrder.verify(amCaseUserRepository).findCaseRoles(AM_CASE_ID, USER_ID),
                    () -> inOrder.verify(switchableCaseUserRepository).forWriting(AM_CASE_TYPE_ID),
                    () -> inOrder.verify(amCaseUserRepository).grantAccess(AM_CASE_ID, USER_ID, CASE_ROLE),
                    () -> inOrder.verify(amCaseUserRepository).grantAccess(AM_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> inOrder.verify(switchableCaseUserRepository).forWriting(AM_CASE_TYPE_ID),
                    () -> inOrder.verify(amCaseUserRepository).revokeAccess(AM_CASE_ID, USER_ID, CASE_ROLE_GRANTED),
                    inOrder::verifyNoMoreInteractions,
                    () -> verifyNoMoreInteractions(caseRoleRepository),
                    () -> verifyNoMoreInteractions(switchableCaseUserRepository),
                    () -> verifyNoMoreInteractions(ccdCaseUserRepository),
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
                configureCaseRoleRepository(BOTH_CASE_TYPE_ID);
            }

            @Test
            @DisplayName("should grant access when added case role valid")
            void shouldGrantAccessForCaseRole() {
                caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE));

                verify(amCaseUserRepository).grantAccess(BOTH_CASE_ID, USER_ID, CASE_ROLE);
                verify(ccdCaseUserRepository).grantAccess(BOTH_CASE_ID, USER_ID, CASE_ROLE);
            }

            @Test
            @DisplayName("should grant access when added case roles contains global [CREATOR]")
            void shouldGrantAccessForCaseRoleCreator() {
                caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE, CREATOR.getRole()));

                assertAll(
                    () -> verify(ccdCaseUserRepository).grantAccess(BOTH_CASE_ID, USER_ID, CASE_ROLE),
                    () -> verify(ccdCaseUserRepository).grantAccess(BOTH_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> verify(amCaseUserRepository).grantAccess(BOTH_CASE_ID, USER_ID, CASE_ROLE),
                    () -> verify(amCaseUserRepository).grantAccess(BOTH_CASE_ID, USER_ID, CREATOR.getRole())
                );
            }

            @Test
            @DisplayName("should revoke access for removed case roles")
            void shouldRevokeRemovedCaseRoles() {
                caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE));

                verify(ccdCaseUserRepository).revokeAccess(BOTH_CASE_ID, USER_ID, CASE_ROLE_GRANTED);
                verify(amCaseUserRepository).revokeAccess(BOTH_CASE_ID, USER_ID, CASE_ROLE_GRANTED);
            }

            @Test
            @DisplayName("should ignore case roles already granted")
            void shouldIgnoreGrantedCaseRoles() {
                caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE_GRANTED));

                verify(amCaseUserRepository, never()).grantAccess(BOTH_CASE_ID, USER_ID, CASE_ROLE_GRANTED);
                verify(ccdCaseUserRepository, never()).grantAccess(BOTH_CASE_ID, USER_ID, CASE_ROLE_GRANTED);
            }

            @Test
            @DisplayName("should invoke services in order for two new roles granting and one revoking")
            void shouldInvokeServicesInOrder() {
                caseAccessOperation.updateUserAccess(caseDetails, caseUser(CASE_ROLE, CREATOR.getRole()));

                InOrder inOrder = inOrder(caseRoleRepository, switchableCaseUserRepository, ccdCaseUserRepository, amCaseUserRepository);
                assertAll(
                    () -> inOrder.verify(caseRoleRepository).getCaseRoles(BOTH_CASE_TYPE_ID),
                    () -> inOrder.verify(switchableCaseUserRepository).forReading(BOTH_CASE_TYPE_ID),
                    () -> inOrder.verify(ccdCaseUserRepository).findCaseRoles(BOTH_CASE_ID, USER_ID),
                    () -> inOrder.verify(switchableCaseUserRepository).forWriting(BOTH_CASE_TYPE_ID),
                    () -> inOrder.verify(ccdCaseUserRepository).grantAccess(BOTH_CASE_ID, USER_ID, CASE_ROLE),
                    () -> inOrder.verify(amCaseUserRepository).grantAccess(BOTH_CASE_ID, USER_ID, CASE_ROLE),
                    () -> inOrder.verify(ccdCaseUserRepository).grantAccess(BOTH_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> inOrder.verify(amCaseUserRepository).grantAccess(BOTH_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> inOrder.verify(switchableCaseUserRepository).forWriting(BOTH_CASE_TYPE_ID),
                    () -> inOrder.verify(ccdCaseUserRepository).revokeAccess(BOTH_CASE_ID, USER_ID, CASE_ROLE_GRANTED),
                    () -> inOrder.verify(amCaseUserRepository).revokeAccess(BOTH_CASE_ID, USER_ID, CASE_ROLE_GRANTED),
                    inOrder::verifyNoMoreInteractions,
                    () -> verifyNoMoreInteractions(caseRoleRepository),
                    () -> verifyNoMoreInteractions(switchableCaseUserRepository),
                    () -> verifyNoMoreInteractions(ccdCaseUserRepository),
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
                configureCaseRoleRepository(CCD_CASE_TYPE_ID);
            }

            @Test
            @DisplayName("should revoke access to user")
            void shouldRevokeAccess() {
                caseAccessOperation.revokeAccess(JURISDICTION, CCD_CASE_REFERENCE.toString(), USER_ID);

                assertAll(
                    () -> verify(caseDetailsRepository).findByReference(JURISDICTION, CCD_CASE_REFERENCE),
                    () -> verify(ccdCaseUserRepository).revokeAccess(CCD_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> verifyZeroInteractions(amCaseUserRepository)
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
                    () -> verify(ccdCaseUserRepository, never()).revokeAccess(CCD_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> verifyZeroInteractions(amCaseUserRepository)
                );
            }

            @Test
            @DisplayName("should throw not found exception when reference in different jurisdiction")
            void shouldHandleWrongJurisdiction() {
                assertAll(
                    () -> assertThrows(CaseNotFoundException.class, () -> {
                        caseAccessOperation.revokeAccess(WRONG_JURISDICTION, CCD_CASE_REFERENCE.toString(), USER_ID);
                    }),
                    () -> verify(caseDetailsRepository).findByReference(WRONG_JURISDICTION, CCD_CASE_REFERENCE),
                    () -> verify(ccdCaseUserRepository, never()).revokeAccess(CCD_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> verifyZeroInteractions(amCaseUserRepository)
                );
            }

            @Test
            @DisplayName("should invoke services in order for revoking access to user")
            void shouldInvokeServicesInOrder() {
                caseAccessOperation.revokeAccess(JURISDICTION, CCD_CASE_REFERENCE.toString(), USER_ID);

                InOrder inOrder = inOrder(caseDetailsRepository, switchableCaseUserRepository, ccdCaseUserRepository, amCaseUserRepository);
                assertAll(
                    () -> inOrder.verify(caseDetailsRepository).findByReference(JURISDICTION, CCD_CASE_REFERENCE),
                    () -> inOrder.verify(switchableCaseUserRepository).forWriting(CCD_CASE_TYPE_ID),
                    () -> inOrder.verify(ccdCaseUserRepository).revokeAccess(CCD_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> inOrder.verifyNoMoreInteractions(),
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
                configureCaseRoleRepository(AM_CASE_TYPE_ID);
            }

            @Test
            @DisplayName("should revoke access to user")
            void shouldRevokeAccess() {
                caseAccessOperation.revokeAccess(JURISDICTION, AM_CASE_REFERENCE.toString(), USER_ID);

                assertAll(
                    () -> verify(caseDetailsRepository).findByReference(JURISDICTION, AM_CASE_REFERENCE),
                    () -> verify(amCaseUserRepository).revokeAccess(AM_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> verifyZeroInteractions(ccdCaseUserRepository)
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
                    () -> verify(amCaseUserRepository, never()).revokeAccess(AM_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> verifyZeroInteractions(ccdCaseUserRepository)
                );
            }

            @Test
            @DisplayName("should throw not found exception when reference in different jurisdiction")
            void shouldHandleWrongJurisdiction() {
                assertAll(
                    () -> assertThrows(CaseNotFoundException.class, () -> {
                        caseAccessOperation.revokeAccess(WRONG_JURISDICTION, AM_CASE_REFERENCE.toString(), USER_ID);
                    }),
                    () -> verify(caseDetailsRepository).findByReference(WRONG_JURISDICTION, AM_CASE_REFERENCE),
                    () -> verify(amCaseUserRepository, never()).revokeAccess(AM_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> verifyZeroInteractions(ccdCaseUserRepository)
                );
            }

            @Test
            @DisplayName("should invoke services in order for revoking access to user")
            void shouldInvokeServicesInOrder() {
                caseAccessOperation.revokeAccess(JURISDICTION, AM_CASE_REFERENCE.toString(), USER_ID);

                InOrder inOrder = inOrder(caseDetailsRepository, switchableCaseUserRepository, ccdCaseUserRepository, amCaseUserRepository);
                assertAll(
                    () -> inOrder.verify(caseDetailsRepository).findByReference(JURISDICTION, AM_CASE_REFERENCE),
                    () -> inOrder.verify(switchableCaseUserRepository).forWriting(AM_CASE_TYPE_ID),
                    () -> inOrder.verify(amCaseUserRepository).revokeAccess(AM_CASE_ID, USER_ID, CREATOR.getRole()),
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
                configureCaseRoleRepository(BOTH_CASE_TYPE_ID);
            }

            @Test
            @DisplayName("should revoke access to user")
            void shouldRevokeAccess() {
                caseAccessOperation.revokeAccess(JURISDICTION, BOTH_CASE_REFERENCE.toString(), USER_ID);

                assertAll(
                    () -> verify(caseDetailsRepository).findByReference(JURISDICTION, BOTH_CASE_REFERENCE),
                    () -> verify(ccdCaseUserRepository).revokeAccess(BOTH_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> verify(amCaseUserRepository).revokeAccess(BOTH_CASE_ID, USER_ID, CREATOR.getRole())
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
                    () -> verify(ccdCaseUserRepository, never()).revokeAccess(BOTH_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> verify(amCaseUserRepository, never()).revokeAccess(BOTH_CASE_ID, USER_ID, CREATOR.getRole())
                );
            }

            @Test
            @DisplayName("should throw not found exception when reference in different jurisdiction")
            void shouldHandleWrongJurisdiction() {
                assertAll(
                    () -> assertThrows(CaseNotFoundException.class, () -> {
                        caseAccessOperation.revokeAccess(WRONG_JURISDICTION, BOTH_CASE_REFERENCE.toString(), USER_ID);
                    }),
                    () -> verify(caseDetailsRepository).findByReference(WRONG_JURISDICTION, BOTH_CASE_REFERENCE),
                    () -> verify(ccdCaseUserRepository, never()).revokeAccess(BOTH_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> verify(amCaseUserRepository, never()).revokeAccess(BOTH_CASE_ID, USER_ID, CREATOR.getRole())
                );
            }

            @Test
            @DisplayName("should invoke services in order for revoking access to user")
            void shouldInvokeServicesInOrder() {
                caseAccessOperation.revokeAccess(JURISDICTION, BOTH_CASE_REFERENCE.toString(), USER_ID);

                InOrder inOrder = inOrder(caseDetailsRepository, switchableCaseUserRepository, ccdCaseUserRepository, amCaseUserRepository);
                assertAll(
                    () -> inOrder.verify(caseDetailsRepository).findByReference(JURISDICTION, BOTH_CASE_REFERENCE),
                    () -> inOrder.verify(switchableCaseUserRepository).forWriting(BOTH_CASE_TYPE_ID),
                    () -> inOrder.verify(ccdCaseUserRepository).revokeAccess(BOTH_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> inOrder.verify(amCaseUserRepository).revokeAccess(BOTH_CASE_ID, USER_ID, CREATOR.getRole()),
                    () -> inOrder.verifyNoMoreInteractions(),
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

                configureCaseRoleRepository(CCD_CASE_TYPE_ID);
            }

            @Test
            @DisplayName("should find cases user has access to")
            void shouldFindCasesUserHasAccessTo() {
                List<String> casesUserIdHasAccessTo = caseAccessOperation.findCasesUserIdHasAccessTo(USER_ID);

                Assertions.assertAll(
                    () -> verify(ccdCaseUserRepository).findCasesUserIdHasAccessTo(USER_ID),
                    () -> verify(amCaseUserRepository).findCasesUserIdHasAccessTo(USER_ID),
                    () -> assertThat(casesUserIdHasAccessTo, hasItems(CCD_CASE_REFERENCE.toString(),
                        CCD_CASE_REFERENCE_3.toString(),
                        AM_CASE_REFERENCE.toString()))
                );
            }

            @Test
            @DisplayName("should propagate exception to caller if thrown")
            void shouldPropagateExceptionToCallerIfThrown() {
                doThrow(ApiException.class).when(amCaseUserRepository).findCasesUserIdHasAccessTo(USER_ID);
                assertThrows(ApiException.class, () -> caseAccessOperation.findCasesUserIdHasAccessTo(USER_ID),
                    "Cases lookup should have failed");
            }

            @Test
            @DisplayName("should invoke services in order to find cases user id has access to")
            void shouldInvokeServicesInOrder() {
                when(amCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID)).thenReturn(Lists.newArrayList(AM_CASE_ID, CCD_CASE_ID_2, BOTH_CASE_ID_3));

                caseAccessOperation.findCasesUserIdHasAccessTo(USER_ID);

                InOrder inOrder = inOrder(switchableCaseUserRepository, ccdCaseUserRepository, amCaseUserRepository, caseDetailsRepository, caseRoleRepository);
                assertAll(
                    () -> inOrder.verify(switchableCaseUserRepository).forReading(),

                    () -> inOrder.verify(ccdCaseUserRepository).findCasesUserIdHasAccessTo(USER_ID),
                    () -> inOrder.verify(caseDetailsRepository).findById(CCD_CASE_ID),
                    () -> inOrder.verify(switchableCaseUserRepository).getReadModeForCaseType(CCD_CASE_TYPE_ID),
                    () -> inOrder.verify(ccdCaseUserRepository).getType(),
                    () -> inOrder.verify(caseDetailsRepository).findById(AM_CASE_ID_2),
                    () -> inOrder.verify(switchableCaseUserRepository).getReadModeForCaseType(AM_CASE_TYPE_ID),
                    () -> inOrder.verify(ccdCaseUserRepository).getType(),
                    () -> inOrder.verify(caseDetailsRepository).findById(CCD_CASE_ID_3),
                    () -> inOrder.verify(switchableCaseUserRepository).getReadModeForCaseType(CCD_CASE_TYPE_ID),
                    () -> inOrder.verify(ccdCaseUserRepository).getType(),

                    () -> inOrder.verify(amCaseUserRepository).findCasesUserIdHasAccessTo(USER_ID),
                    () -> inOrder.verify(caseDetailsRepository).findById(AM_CASE_ID),
                    () -> inOrder.verify(switchableCaseUserRepository).getReadModeForCaseType(AM_CASE_TYPE_ID),
                    () -> inOrder.verify(amCaseUserRepository).getType(),
                    () -> inOrder.verify(caseDetailsRepository).findById(CCD_CASE_ID_2),
                    () -> inOrder.verify(switchableCaseUserRepository).getReadModeForCaseType(CCD_CASE_TYPE_ID),
                    () -> inOrder.verify(amCaseUserRepository).getType(),
                    () -> inOrder.verify(caseDetailsRepository).findById(BOTH_CASE_ID_3),
                    () -> inOrder.verify(switchableCaseUserRepository).getReadModeForCaseType(BOTH_CASE_TYPE_ID),
                    () -> inOrder.verify(amCaseUserRepository).getType(),

                    inOrder::verifyNoMoreInteractions,
                    () -> verifyNoMoreInteractions(switchableCaseUserRepository),
                    () -> verifyNoMoreInteractions(ccdCaseUserRepository),
                    () -> verifyNoMoreInteractions(amCaseUserRepository),
                    () -> verifyNoMoreInteractions(caseDetailsRepository),
                    () -> verifyZeroInteractions(caseRoleRepository)
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

    private void configureCaseRoleRepository(String caseTypeId) {
        when(caseRoleRepository.getCaseRoles(caseTypeId)).thenReturn(Sets.newHashSet(CASE_ROLE,
            CASE_ROLE_OTHER,
            CASE_ROLE_GRANTED));
    }

    private void configureSwitchableCaseUserRepository() {
        ccdCaseUserRepositories.add(ccdCaseUserRepository);
        amCaseUserRepositories.add(amCaseUserRepository);
        bothCaseUserRepositories.add(ccdCaseUserRepository);
        bothCaseUserRepositories.add(amCaseUserRepository);
        when(switchableCaseUserRepository.forReading()).thenReturn(bothCaseUserRepositories);
        when(switchableCaseUserRepository.forReading(CCD_CASE_TYPE_ID)).thenReturn(ccdCaseUserRepository);
        when(switchableCaseUserRepository.forReading(AM_CASE_TYPE_ID)).thenReturn(amCaseUserRepository);
        when(switchableCaseUserRepository.forReading(BOTH_CASE_TYPE_ID)).thenReturn(ccdCaseUserRepository);
        when(switchableCaseUserRepository.forWriting(CCD_CASE_TYPE_ID)).thenReturn(ccdCaseUserRepositories);
        when(switchableCaseUserRepository.forWriting(AM_CASE_TYPE_ID)).thenReturn(amCaseUserRepositories);
        when(switchableCaseUserRepository.forWriting(BOTH_CASE_TYPE_ID)).thenReturn(bothCaseUserRepositories);
        when(switchableCaseUserRepository.getReadModeForCaseType(CCD_CASE_TYPE_ID)).thenReturn(CCD_TYPE);
        when(switchableCaseUserRepository.getReadModeForCaseType(AM_CASE_TYPE_ID)).thenReturn(AM_TYPE);
        when(switchableCaseUserRepository.getReadModeForCaseType(BOTH_CASE_TYPE_ID)).thenReturn(CCD_TYPE);
    }

    private void configureCaseUserRepository() {
        when(ccdCaseUserRepository.getType()).thenReturn(CCD_TYPE);
        when(amCaseUserRepository.getType()).thenReturn(AM_TYPE);
        when(ccdCaseUserRepository.findCaseRoles(CCD_CASE_ID,
            USER_ID)).thenReturn(Collections.singletonList(CASE_ROLE_GRANTED));
        when(ccdCaseUserRepository.findCaseRoles(BOTH_CASE_ID,
            USER_ID)).thenReturn(Collections.singletonList(CASE_ROLE_GRANTED));
        when(amCaseUserRepository.findCaseRoles(AM_CASE_ID,
            USER_ID)).thenReturn(Collections.singletonList(CASE_ROLE_GRANTED));
        when(amCaseUserRepository.findCaseRoles(BOTH_CASE_ID,
            USER_ID)).thenReturn(Collections.singletonList(CASE_ROLE_GRANTED));
        when(ccdCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID)).thenReturn(Lists.newArrayList(CCD_CASE_ID, AM_CASE_ID_2, CCD_CASE_ID_3));
        when(amCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID)).thenReturn(Lists.newArrayList(AM_CASE_ID, CCD_CASE_ID_2, BOTH_CASE_ID_3));
    }


    private CaseUser caseUser(String... caseRoles) {
        final CaseUser caseUser = new CaseUser();
        caseUser.setUserId(USER_ID);
        caseUser.getCaseRoles().addAll(Arrays.asList(caseRoles));
        return caseUser;
    }

}
