package uk.gov.hmcts.ccd.domain.service.supplementarydata.rolevalidator;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultUserRoleValidatorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CaseAccessService caseAccessService;

    private DefaultUserRoleValidator userRoleValidator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        userRoleValidator = new DefaultUserRoleValidator(userRepository,
            caseAccessService);
    }

    @Test
    void shouldReturnTrueWhenLoggedInUserHasCAARole() {
        CaseDetails caseDetails = mock(CaseDetails.class);
        when(this.userRepository.getUserRoles()).thenReturn(Sets.newHashSet("caseworker-caa"));
        boolean canAccess = this.userRoleValidator.canUpdateSupplementaryData(caseDetails);
        assertTrue(canAccess);
    }

    @Test
    void shouldReturnTrueWhenUserHasSolicitorRoleAndUserHasAccessToCase() {
        CaseDetails caseDetails = mock(CaseDetails.class);
        when(this.userRepository.getUserRoles()).thenReturn(Sets.newHashSet("caseworker-probate-solicitor"));
        when(this.caseAccessService.canUserAccess(caseDetails)).thenReturn(true);
        boolean canAccess = this.userRoleValidator.canUpdateSupplementaryData(caseDetails);
        assertTrue(canAccess);
    }

    @Test
    void shouldReturnTrueWhenUserHasAccessToJurisdictions() {
        CaseDetails caseDetails = mock(CaseDetails.class);
        when(caseDetails.getJurisdiction()).thenReturn("PROBATE");
        when(this.userRepository.getUserRoles()).thenReturn(Sets.newHashSet("caseworker-probate-solicitor"));
        when(this.caseAccessService.canUserAccess(caseDetails)).thenReturn(false);
        when(this.caseAccessService.isJurisdictionAccessAllowed(anyString())).thenReturn(true);
        boolean canAccess = this.userRoleValidator.canUpdateSupplementaryData(caseDetails);
        assertTrue(canAccess);
    }

    @Test
    void shouldReturnFalseWhenUserHasAccessNoAccess() {
        CaseDetails caseDetails = mock(CaseDetails.class);
        when(caseDetails.getJurisdiction()).thenReturn("PROBATE");
        when(this.userRepository.getUserRoles()).thenReturn(Sets.newHashSet("caseworker-probate-solicitor"));
        when(this.caseAccessService.canUserAccess(caseDetails)).thenReturn(false);
        when(this.caseAccessService.isJurisdictionAccessAllowed(anyString())).thenReturn(false);
        boolean canAccess = this.userRoleValidator.canUpdateSupplementaryData(caseDetails);
        assertFalse(canAccess);
    }
}
