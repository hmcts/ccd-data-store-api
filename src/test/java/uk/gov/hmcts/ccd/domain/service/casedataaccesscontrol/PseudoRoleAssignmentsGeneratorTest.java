package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PRIVATE;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PUBLIC;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.RESTRICTED;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType.EXCLUDED;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType.SPECIFIC;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType.STANDARD;
import static uk.gov.hmcts.ccd.domain.service.AccessControl.IDAM_PREFIX;

@DisplayName("PseudoRoleAssignmentsGenerator")
class PseudoRoleAssignmentsGeneratorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private CaseAccessService caseAccessService;

    private PseudoRoleAssignmentsGenerator pseudoRoleAssignmentsGenerator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        pseudoRoleAssignmentsGenerator = new PseudoRoleAssignmentsGenerator(userRepository,
                                                                            caseDefinitionRepository,
                                                                            caseAccessService);
    }

    @Nested
    @DisplayName("createPseudoRoleAssignments()")
    class CreatePseudoRoleAssignments {

        private static final String ROLE_PROVIDED = "provided";
        private static final String ROLE_SOLICITOR = "caseworker-divorce-solicitor";
        private static final String EXPECTED_ROLE_SOLICITOR = IDAM_PREFIX + "caseworker-divorce-solicitor";
        private static final String ROLE_LOCAL_AUTHORITY = "caseworker-divorce-localAuthority";
        private static final String EXPECTED_ROLE_LOCAL_AUTHORITY = IDAM_PREFIX + "caseworker-divorce-localAuthority";
        private static final String ROLE_CASEWORKER_1 = "caseworker";
        private static final String EXPECTED_ROLE_CASEWORKER_1 = IDAM_PREFIX + "caseworker";
        private static final String ROLE_CASEWORKER_2 = "caseworker-divorce";
        private static final String EXPECTED_ROLE_CASEWORKER_2 = IDAM_PREFIX + "caseworker-divorce";

        @Test
        @DisplayName("should create pseudo RoleAssignments for granted user roles")
        public void shouldAddFakeRoleAssignmentsForGrantedUserRoles() {
            given(userRepository.getUserRoles()).willReturn(new HashSet<>(asList(ROLE_CASEWORKER_1,
                                                                                 ROLE_CASEWORKER_2,
                                                                                 ROLE_SOLICITOR,
                                                                                 ROLE_LOCAL_AUTHORITY)));
            given(caseAccessService.userCanOnlyAccessExplicitlyGrantedCases()).willReturn(true);

            List<RoleAssignment> roleAssignments = singletonList(caseRoleAssignment(SPECIFIC.name()));

            List<RoleAssignment> augmentedRoleAssignments = pseudoRoleAssignmentsGenerator
                .createPseudoRoleAssignments(roleAssignments, false);

            Optional<RoleAssignment> caseworker1RoleAssignment = augmentedRoleAssignments
                .stream().filter(ra -> EXPECTED_ROLE_CASEWORKER_1.equals(ra.getRoleName())).findFirst();
            Optional<RoleAssignment> caseworker2RoleAssignment = augmentedRoleAssignments
                .stream().filter(ra -> EXPECTED_ROLE_CASEWORKER_2.equals(ra.getRoleName())).findFirst();
            Optional<RoleAssignment> solicitorRoleAssignment = augmentedRoleAssignments
                .stream().filter(ra -> EXPECTED_ROLE_SOLICITOR.equals(ra.getRoleName())).findFirst();
            Optional<RoleAssignment> localAuthorityRoleAssignment = augmentedRoleAssignments
                .stream().filter(ra -> EXPECTED_ROLE_LOCAL_AUTHORITY.equals(ra.getRoleName())).findFirst();

            assertAll(
                () -> assertThat(augmentedRoleAssignments.size(), is(4)),

                () -> assertTrue(solicitorRoleAssignment.isPresent()),
                () -> assertTrue(localAuthorityRoleAssignment.isPresent()),

                () -> assertThat(caseworker1RoleAssignment.get().getRoleName(), is(EXPECTED_ROLE_CASEWORKER_1)),
                () -> assertThat(caseworker1RoleAssignment.get().getGrantType(), is(SPECIFIC.name())),
                () -> assertThat(caseworker1RoleAssignment.get().getClassification(), is(RESTRICTED.name())),

                () -> assertThat(caseworker2RoleAssignment.get().getRoleName(), is(EXPECTED_ROLE_CASEWORKER_2)),
                () -> assertThat(caseworker2RoleAssignment.get().getGrantType(), is(SPECIFIC.name())),
                () -> assertThat(caseworker2RoleAssignment.get().getClassification(), is(RESTRICTED.name())),

                () -> assertThat(solicitorRoleAssignment.get().getRoleName(), is(EXPECTED_ROLE_SOLICITOR)),
                () -> assertThat(solicitorRoleAssignment.get().getGrantType(), is(SPECIFIC.name())),
                () -> assertThat(solicitorRoleAssignment.get().getClassification(), is(RESTRICTED.name())),

                () -> assertThat(localAuthorityRoleAssignment.get().getRoleName(), is(EXPECTED_ROLE_LOCAL_AUTHORITY)),
                () -> assertThat(localAuthorityRoleAssignment.get().getGrantType(), is(SPECIFIC.name())),
                () -> assertThat(localAuthorityRoleAssignment.get().getClassification(), is(RESTRICTED.name())),

                () -> verify(userRepository).getUserRoles()
            );
        }

        @Test
        @DisplayName("should not create pseudo RoleAssignments for granted roles"
            + " when no single non-EXCLUDED case RoleAssignment present")
        public void shouldNotAddFakeRoleAssignmentsForGrantedUserRolesWhenNoNonExcludedCaseRoleAssignmentPresent() {
            given(userRepository.getUserRoles()).willReturn(new HashSet<>(asList(ROLE_CASEWORKER_1,
                                                                                 ROLE_CASEWORKER_2,
                                                                                 ROLE_SOLICITOR,
                                                                                 ROLE_LOCAL_AUTHORITY)));
            given(caseAccessService.userCanOnlyAccessExplicitlyGrantedCases()).willReturn(true);

            List<RoleAssignment> roleAssignments = List.of(
                organisationRoleAssignment(STANDARD.name()),
                caseRoleAssignment(EXCLUDED.name())
            );

            List<RoleAssignment> augmentedRoleAssignments = pseudoRoleAssignmentsGenerator
                .createPseudoRoleAssignments(roleAssignments, false);

            assertAll(
                () -> assertThat(augmentedRoleAssignments.size(), is(0))
            );
        }

        @Test
        @DisplayName("should create pseudo RoleAssignments for non-granted user roles")
        public void shouldAddFakeRoleAssignmentsForNonGrantedUserRoles() {
            List<String> userRoles = asList(ROLE_CASEWORKER_2, ROLE_CASEWORKER_1);
            given(userRepository.getUserRoles()).willReturn(new HashSet<>(userRoles));
            List<UserRole> userRolesWithClassification =
                asList(createUserRole(ROLE_CASEWORKER_1, PUBLIC.name()),
                       createUserRole(ROLE_CASEWORKER_2, PRIVATE.name()));
            given(caseDefinitionRepository.getClassificationsForUserRoleList(anyList()))
                .willReturn(userRolesWithClassification);
            given(caseAccessService.userCanOnlyAccessExplicitlyGrantedCases()).willReturn(false);

            List<RoleAssignment> roleAssignments = singletonList(organisationRoleAssignment(STANDARD.name()));

            List<RoleAssignment> augmentedRoleAssignments = pseudoRoleAssignmentsGenerator
                .createPseudoRoleAssignments(roleAssignments, false);

            Optional<RoleAssignment> caseworker1RoleAssignment = augmentedRoleAssignments
                .stream().filter(ra -> EXPECTED_ROLE_CASEWORKER_1.equals(ra.getRoleName())).findFirst();
            Optional<RoleAssignment> caseworker2RoleAssignment = augmentedRoleAssignments
                .stream().filter(ra -> EXPECTED_ROLE_CASEWORKER_2.equals(ra.getRoleName())).findFirst();

            assertAll(
                () -> assertThat(augmentedRoleAssignments.size(), is(2)),

                () -> assertTrue(caseworker1RoleAssignment.isPresent()),
                () -> assertTrue(caseworker2RoleAssignment.isPresent()),

                () -> assertThat(caseworker1RoleAssignment.get().getRoleName(), is(EXPECTED_ROLE_CASEWORKER_1)),
                () -> assertThat(caseworker1RoleAssignment.get().getGrantType(), is(GrantType.STANDARD.name())),
                () -> assertThat(caseworker1RoleAssignment.get().getClassification(), is(PUBLIC.name())),

                () -> assertThat(caseworker2RoleAssignment.get().getRoleName(), is(EXPECTED_ROLE_CASEWORKER_2)),
                () -> assertThat(caseworker2RoleAssignment.get().getGrantType(), is(GrantType.STANDARD.name())),
                () -> assertThat(caseworker2RoleAssignment.get().getClassification(), is(PRIVATE.name())),

                () -> verify(userRepository).getUserRoles()
            );
        }

        @Test
        @DisplayName("should create pseudo RoleAssignments for granted roles"
            + " with creation profile")
        public void shouldAddFakeRoleAssignmentsForGrantedUserRolesWithCreationProfile() {
            given(userRepository.getUserRoles()).willReturn(new HashSet<>(asList(ROLE_CASEWORKER_1,
                ROLE_CASEWORKER_2,
                ROLE_SOLICITOR,
                ROLE_LOCAL_AUTHORITY)));
            given(caseAccessService.userCanOnlyAccessExplicitlyGrantedCases()).willReturn(true);

            List<UserRole> userRolesWithClassification =
                asList(createUserRole(ROLE_CASEWORKER_1, PUBLIC.name()),
                    createUserRole(ROLE_CASEWORKER_2, PRIVATE.name()),
                    createUserRole(ROLE_SOLICITOR, PRIVATE.name()),
                    createUserRole(ROLE_LOCAL_AUTHORITY, RESTRICTED.name()));
            given(caseDefinitionRepository.getClassificationsForUserRoleList(anyList()))
                .willReturn(userRolesWithClassification);

            List<RoleAssignment> roleAssignments = singletonList(organisationRoleAssignment(STANDARD.name()));

            List<RoleAssignment> augmentedRoleAssignments = pseudoRoleAssignmentsGenerator
                .createPseudoRoleAssignments(roleAssignments, true);

            Optional<RoleAssignment> caseworker1RoleAssignment = augmentedRoleAssignments
                .stream().filter(ra -> EXPECTED_ROLE_CASEWORKER_1.equals(ra.getRoleName())).findFirst();
            Optional<RoleAssignment> caseworker2RoleAssignment = augmentedRoleAssignments
                .stream().filter(ra -> EXPECTED_ROLE_CASEWORKER_2.equals(ra.getRoleName())).findFirst();
            Optional<RoleAssignment> solicitorRoleAssignment = augmentedRoleAssignments
                .stream().filter(ra -> EXPECTED_ROLE_SOLICITOR.equals(ra.getRoleName())).findFirst();
            Optional<RoleAssignment> localAuthorityRoleAssignment = augmentedRoleAssignments
                .stream().filter(ra -> EXPECTED_ROLE_LOCAL_AUTHORITY.equals(ra.getRoleName())).findFirst();

            assertAll(
                () -> assertThat(augmentedRoleAssignments.size(), is(4)),

                () -> assertTrue(solicitorRoleAssignment.isPresent()),
                () -> assertTrue(localAuthorityRoleAssignment.isPresent()),

                () -> assertThat(caseworker1RoleAssignment.get().getRoleName(), is(EXPECTED_ROLE_CASEWORKER_1)),
                () -> assertThat(caseworker1RoleAssignment.get().getGrantType(), is(STANDARD.name())),
                () -> assertThat(caseworker1RoleAssignment.get().getClassification(), is(PUBLIC.name())),

                () -> assertThat(caseworker2RoleAssignment.get().getRoleName(), is(EXPECTED_ROLE_CASEWORKER_2)),
                () -> assertThat(caseworker2RoleAssignment.get().getGrantType(), is(STANDARD.name())),
                () -> assertThat(caseworker2RoleAssignment.get().getClassification(), is(PRIVATE.name())),

                () -> assertThat(solicitorRoleAssignment.get().getRoleName(), is(EXPECTED_ROLE_SOLICITOR)),
                () -> assertThat(solicitorRoleAssignment.get().getGrantType(), is(STANDARD.name())),
                () -> assertThat(solicitorRoleAssignment.get().getClassification(), is(PRIVATE.name())),

                () -> assertThat(localAuthorityRoleAssignment.get().getRoleName(), is(EXPECTED_ROLE_LOCAL_AUTHORITY)),
                () -> assertThat(localAuthorityRoleAssignment.get().getGrantType(), is(STANDARD.name())),
                () -> assertThat(localAuthorityRoleAssignment.get().getClassification(), is(RESTRICTED.name())),

                () -> verify(userRepository).getUserRoles()
            );
        }

        private RoleAssignment caseRoleAssignment(String grantType) {
            return roleAssignment(Optional.of("12345"), grantType);
        }

        private RoleAssignment organisationRoleAssignment(String grantType) {
            return roleAssignment(Optional.empty(), grantType);
        }

        private RoleAssignment roleAssignment(Optional<String> caseId, String grantType) {
            return RoleAssignment.builder()
                .roleName(ROLE_PROVIDED)
                .grantType(grantType)
                .attributes(RoleAssignmentAttributes.builder().caseId(caseId).build())
                .build();
        }

        private UserRole createUserRole(String role, String classification) {
            UserRole userRole = new UserRole();
            userRole.setRole(role);
            userRole.setSecurityClassification(classification);
            return userRole;
        }
    }

}
