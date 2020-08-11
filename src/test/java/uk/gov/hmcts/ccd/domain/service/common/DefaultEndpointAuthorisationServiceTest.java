package uk.gov.hmcts.ccd.domain.service.common;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultEndpointAuthorisationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CaseAccessService caseAccessService;

    @Mock
    private ApplicationParams applicationParams;

    private DefaultEndpointAuthorisationService userRoleValidator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        userRoleValidator = new DefaultEndpointAuthorisationService(userRepository,
            caseAccessService, applicationParams);
    }

    @Test
    void shouldReturnTrueWhenLoggedInUserHasCAARole() {
        CaseDetails caseDetails = mock(CaseDetails.class);
        when(applicationParams.getCcdAccessControlCrossJurisdictionRoles()).thenReturn(Arrays.asList("caseworker-caa"));
        when(this.userRepository.anyRoleEqualsAnyOf(applicationParams.getCcdAccessControlCrossJurisdictionRoles())).thenReturn(true);
        boolean canAccess = this.userRoleValidator.isAccessAllowed(caseDetails);
        assertTrue(canAccess);
    }

    @Test
    void shouldReturnTrueWhenUserHasSolicitorRoleAndUserHasAccessToCase() {
        CaseDetails caseDetails = mock(CaseDetails.class);
        when(this.userRepository.getUserRoles()).thenReturn(Sets.newHashSet("caseworker-probate-solicitor"));
        when(this.caseAccessService.canOnlyViewExplicitlyGrantedCases()).thenReturn(true);
        when(this.caseAccessService.isExplicitAccessGranted(caseDetails)).thenReturn(true);
        boolean canAccess = this.userRoleValidator.isAccessAllowed(caseDetails);
        assertTrue(canAccess);
    }

    @Test
    void shouldReturnTrueWhenUserHasAccessToJurisdictions() {
        CaseDetails caseDetails = mock(CaseDetails.class);
        when(caseDetails.getJurisdiction()).thenReturn("PROBATE");
        when(this.userRepository.getUserRoles()).thenReturn(Sets.newHashSet("caseworker-probate-solicitor"));
        when(this.caseAccessService.canOnlyViewExplicitlyGrantedCases()).thenReturn(false);
        when(this.caseAccessService.isJurisdictionAccessAllowed(anyString())).thenReturn(true);
        boolean canAccess = this.userRoleValidator.isAccessAllowed(caseDetails);
        assertTrue(canAccess);
    }

    @Test
    void shouldReturnFalseWhenUserHasAccessNoAccess() {
        CaseDetails caseDetails = mock(CaseDetails.class);
        when(caseDetails.getJurisdiction()).thenReturn("PROBATE");
        when(this.userRepository.getUserRoles()).thenReturn(Sets.newHashSet("caseworker-probate-solicitor"));
        when(this.caseAccessService.canOnlyViewExplicitlyGrantedCases()).thenReturn(false);
        when(this.caseAccessService.isJurisdictionAccessAllowed(anyString())).thenReturn(false);
        boolean canAccess = this.userRoleValidator.isAccessAllowed(caseDetails);
        assertFalse(canAccess);
    }
}
