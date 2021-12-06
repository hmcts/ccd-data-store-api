package uk.gov.hmcts.ccd.domain.service.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.security.DefaultAuthorisedCaseDefinitionDataService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.JurisdictionBuilder.newJurisdiction;

class DefaultAuthorisedCaseDefinitionDataServiceTest {

    private static final String CASE_TYPE = "caseType";
    private final CaseTypeDefinition caseTypeDefinition = mock(CaseTypeDefinition.class);

    @Mock
    private CaseTypeService caseTypeService;
    @Mock
    private AccessControlService accessControlService;

    @Mock
    private CaseDataAccessControl caseDataAccessControl;

    @InjectMocks
    private DefaultAuthorisedCaseDefinitionDataService authorisedCaseDataService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(caseTypeDefinition.getJurisdictionDefinition()).thenReturn(newJurisdiction().withJurisdictionId("jid")
            .build());
    }

    @Nested
    @DisplayName("Get user authorised case states")
    class GetUserAuthorisedCaseStates {

        private static final String JURISDICTION = "jurisdiction";
        private static final String STATE1 = "state1";
        private static final String STATE2 = "state2";
        private static final String USERID = "userId";
        private final Set<String> userRoles = new HashSet<>();
        Set<AccessProfile> accessProfiles = createAccessProfiles(userRoles);

        @BeforeEach
        void setUp() {
            when(caseTypeDefinition.getId()).thenReturn(CASE_TYPE);
            when(caseTypeService.getCaseTypeForJurisdiction(CASE_TYPE, JURISDICTION)).thenReturn(caseTypeDefinition);
            when(caseTypeService.getCaseType(CASE_TYPE)).thenReturn(caseTypeDefinition);
            when(caseDataAccessControl.generateAccessProfilesByCaseTypeId(anyString()))
                .thenReturn(accessProfiles);
            when(accessControlService.filterCaseStatesByAccess(caseTypeDefinition, accessProfiles, CAN_READ))
                .thenReturn(getCaseStates());
        }

        @Test
        @DisplayName("Should return list of user authorised case states for a jurisdiction and case type")
        void shouldReturnAuthorisedCaseStates() {
            List<CaseStateDefinition> result = authorisedCaseDataService.getUserAuthorisedCaseStates(JURISDICTION,
                CASE_TYPE, CAN_READ);

            verify(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE, JURISDICTION);
            verifyResult(result.stream().map(CaseStateDefinition::getId).collect(Collectors.toList()));
        }

        @Test
        @DisplayName("Should return list of user authorised case state ids for a jurisdiction and case type")
        void shouldReturnAuthorisedCaseStateIds() {
            List<String> result = authorisedCaseDataService.getUserAuthorisedCaseStateIds(JURISDICTION, CASE_TYPE,
                CAN_READ);

            verify(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE, JURISDICTION);
            verifyResult(result);
        }

        @Test
        @DisplayName("Should return list of user authorised case state ids for a case type")
        void shouldReturnAuthorisedCaseStateIdsForCaseType() {
            List<String> result = authorisedCaseDataService.getUserAuthorisedCaseStateIds(CASE_TYPE, CAN_READ);

            verify(caseTypeService).getCaseType(CASE_TYPE);
            verifyResult(result);
        }

        private List<CaseStateDefinition> getCaseStates() {
            CaseStateDefinition caseStateDefinition1 = new CaseStateDefinition();
            caseStateDefinition1.setId(STATE1);
            CaseStateDefinition caseStateDefinition2 = new CaseStateDefinition();
            caseStateDefinition2.setId(STATE2);
            return Arrays.asList(caseStateDefinition1, caseStateDefinition2);
        }

        private void verifyResult(List<String> result) {
            assertAll(
                () -> assertThat(result, containsInAnyOrder(STATE1, STATE2)),
                () -> verify(caseDataAccessControl).generateAccessProfilesByCaseTypeId(anyString()),
                () -> verify(accessControlService).filterCaseStatesByAccess(caseTypeDefinition, accessProfiles,
                    CAN_READ)
            );
        }
    }

    @Nested
    @DisplayName("Get user authorised case type")
    class GetUserAuthorisedCaseTypeDefinition {
        private final Set<String> userRoles = new HashSet<>();
        Set<AccessProfile> accessProfiles = createAccessProfiles(userRoles);

        @BeforeEach
        void setUp() {
            when(caseTypeDefinition.getId()).thenReturn(CASE_TYPE);
            when(caseTypeService.getCaseType(CASE_TYPE)).thenReturn(caseTypeDefinition);
            when(caseDataAccessControl.generateAccessProfilesByCaseTypeId(anyString()))
                .thenReturn(accessProfiles);
            when(caseTypeDefinition.getSecurityClassification()).thenReturn(SecurityClassification.PRIVATE);
        }

        @Test
        @DisplayName("should return case type when user has read access and user classification is higher or euqal to"
            + " case type classification")
        void shouldGetAuthorisedCaseType() {
            when(accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition, accessProfiles, CAN_READ))
                .thenReturn(true);
            when(caseDataAccessControl.getHighestUserClassification(any(CaseTypeDefinition.class), anyBoolean()))
                .thenReturn(SecurityClassification.PRIVATE);

            Optional<CaseTypeDefinition> result = authorisedCaseDataService.getAuthorisedCaseType(CASE_TYPE, CAN_READ);

            assertThat(result.isPresent(), is(true));
            assertThat(result.get(), is(caseTypeDefinition));
            verifyCalls();
        }

        @Test
        @DisplayName("should not return case type when user has no read access to the case type")
        void shouldNotReturnCaseTypeWhenNoAccess() {
            when(accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition, accessProfiles, CAN_READ))
                .thenReturn(false);
            when(caseDataAccessControl.getHighestUserClassification(any(CaseTypeDefinition.class), anyBoolean()))
                .thenReturn(SecurityClassification.PRIVATE);

            Optional<CaseTypeDefinition> result = authorisedCaseDataService.getAuthorisedCaseType(CASE_TYPE, CAN_READ);

            assertThat(result.isPresent(), is(false));
            verifyCalls();
        }

        @Test
        @DisplayName("should not return case type when user classification is lower than case type classification")
        void shouldNotReturnCaseTypeWhenClassificationNotMatched() {
            when(accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition, accessProfiles, CAN_READ))
                .thenReturn(true);
            when(caseDataAccessControl.getHighestUserClassification(any(CaseTypeDefinition.class), anyBoolean()))
                .thenReturn(SecurityClassification.PUBLIC);

            Optional<CaseTypeDefinition> result = authorisedCaseDataService.getAuthorisedCaseType(CASE_TYPE, CAN_READ);

            assertThat(result.isPresent(), is(false));
            verifyCalls();
        }

        @Test
        @DisplayName("should not return case type when user classification is not defined for caseType jurisdiction")
        void shouldNotReturnCaseTypeWhenUserHasNoClassificationForJurisdiction() {
            when(accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition, accessProfiles, CAN_READ))
                .thenReturn(true);
            when(caseDataAccessControl.getUserClassifications(any(CaseTypeDefinition.class), anyBoolean()))
                .thenReturn(Collections.emptySet());
            when(caseDataAccessControl.getHighestUserClassification(any(CaseTypeDefinition.class), anyBoolean()))
                .thenReturn(SecurityClassification.PUBLIC);

            Optional<CaseTypeDefinition> result = authorisedCaseDataService.getAuthorisedCaseType(CASE_TYPE, CAN_READ);

            assertThat(result.isPresent(), is(false));
        }

        void verifyCalls() {
            verify(caseTypeService).getCaseType(CASE_TYPE);
            verify(caseDataAccessControl).generateAccessProfilesByCaseTypeId(anyString());
            verify(accessControlService).canAccessCaseTypeWithCriteria(caseTypeDefinition, accessProfiles, CAN_READ);
        }
    }


    private Set<AccessProfile> createAccessProfiles(Set<String> userRoles) {
        return userRoles.stream()
            .map(userRole -> AccessProfile.builder().readOnly(false)
                .accessProfile(userRole)
                .build())
            .collect(Collectors.toSet());
    }
}
