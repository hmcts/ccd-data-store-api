package uk.gov.hmcts.ccd.v2.external.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.caseaccess.CaseAccessOperation;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ForbiddenException;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;
import uk.gov.hmcts.ccd.v2.external.domain.CaseUser;

import java.util.Arrays;
import java.util.Optional;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CaseUserController")
class CaseUserControllerTest {
    private static final String CASE_REFERENCE = "1234123412341238";
    private static final String NOT_CASE_REFERENCE = "1234123412341234";
    private static final String USER_ID = "123qwe";
    private static final String JURISDICTION_ID = "test_jurisdiction";
    private static final String CASE_ROLE_1 = "[DEFENDANT]";

    @Mock
    private UIDService caseReferenceService;

    @Mock
    private UserAuthorisation userAuthorisation;

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private CaseAccessOperation caseAccessOperation;

    @InjectMocks
    private CaseUserController caseUserController;

    private CaseUser defaultCaseUser;
    private CaseDetails caseDetails;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(true);
        when(caseReferenceService.validateUID(NOT_CASE_REFERENCE)).thenReturn(false);

        when(userAuthorisation.hasJurisdictionRole(JURISDICTION_ID)).thenReturn(true);

        caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);

        when(caseDetailsRepository.findByReference(CASE_REFERENCE)).thenReturn(Optional.of(caseDetails));

        defaultCaseUser = caseUser(CASE_ROLE_1);
    }

    @Nested
    @DisplayName("putUser")
    class PutUser {

        @Test
        @DisplayName("should return 400 when case reference not valid")
        void should400WhenReferenceNotValid() {
            assertThrows(
                BadRequestException.class,
                () -> caseUserController.putUser(NOT_CASE_REFERENCE, USER_ID, null)
            );
        }

        @Test
        @DisplayName("should return 400 when case user missing")
        void should400WhenCaseUserMissing() {
            assertThrows(
                BadRequestException.class,
                () -> caseUserController.putUser(CASE_REFERENCE, USER_ID, null)
            );
        }

        @Test
        @DisplayName("should return 404 when case not found")
        void should404WhenCaseNotFound() {
            when(caseDetailsRepository.findByReference(CASE_REFERENCE)).thenReturn(Optional.empty());

            assertThrows(
                CaseNotFoundException.class,
                () -> caseUserController.putUser(CASE_REFERENCE, USER_ID, defaultCaseUser)
            );
        }

        @Test
        @DisplayName("should update when user has jurisdiction role")
        void shouldUpdateWhenUserHasLimitedAccess() {
            final ArgumentCaptor<CaseUser> caseUserCaptor = ArgumentCaptor.forClass(CaseUser.class);

            caseUserController.putUser(CASE_REFERENCE, USER_ID, defaultCaseUser);

            verify(caseAccessOperation).updateUserAccess(eq(caseDetails), caseUserCaptor.capture());

            final CaseUser caseUser = caseUserCaptor.getValue();
            assertAll(
                () -> assertThat(caseUser.getUserId(), equalTo(USER_ID)),
                () -> assertThat(caseUser.getCaseRoles(), hasSize(1)),
                () -> assertThat(caseUser.getCaseRoles(), contains(CASE_ROLE_1))
            );
        }

        @Test
        @DisplayName("should return 403 when user doesn't have access to case jurisdiction")
        void should403WhenUserNoJurisdictionAccess() {
            when(userAuthorisation.hasJurisdictionRole(JURISDICTION_ID)).thenReturn(false);

            assertThrows(
                ForbiddenException.class,
                () -> caseUserController.putUser(CASE_REFERENCE, USER_ID, defaultCaseUser)
            );
        }

        @Test
        @DisplayName("should revoke access when case roles empty")
        void shouldRevokeAccessWhenRolesEmpty() {
            final ArgumentCaptor<CaseUser> caseUserCaptor = ArgumentCaptor.forClass(CaseUser.class);

            caseUserController.putUser(CASE_REFERENCE, USER_ID, caseUser());

            verify(caseAccessOperation).updateUserAccess(eq(caseDetails), caseUserCaptor.capture());

            final CaseUser caseUser = caseUserCaptor.getValue();
            assertAll(
                () -> assertThat(caseUser.getUserId(), equalTo(USER_ID)),
                () -> assertThat(caseUser.getCaseRoles(), hasSize(0))
            );
        }

        @Test
        @DisplayName("should grant access to case")
        void shouldGrantCaseRoleAccess() {
            final ArgumentCaptor<CaseUser> caseUserCaptor = ArgumentCaptor.forClass(CaseUser.class);

            caseUserController.putUser(CASE_REFERENCE, USER_ID, defaultCaseUser);

            verify(caseAccessOperation).updateUserAccess(eq(caseDetails), caseUserCaptor.capture());

            final CaseUser caseUser = caseUserCaptor.getValue();
            assertAll(
                () -> assertThat(caseUser.getUserId(), equalTo(USER_ID)),
                () -> assertThat(caseUser.getCaseRoles(), hasSize(1)),
                () -> assertThat(caseUser.getCaseRoles(), contains(CASE_ROLE_1))
            );
        }
    }

    private CaseUser caseUser(String... caseRoles) {
        final CaseUser caseUser = new CaseUser();
        caseUser.getCaseRoles()
                .addAll(Arrays.asList(caseRoles));
        return caseUser;
    }

}
