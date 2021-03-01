package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

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
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.FakeRoleAssignmentsGenerator.CLASSIFICATION_RESTRICTED;
import static uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.FakeRoleAssignmentsGenerator.GRANT_TYPE_SPECIFIC;
import static uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.FakeRoleAssignmentsGenerator.GRANT_TYPE_STANDARD;
import static uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.FakeRoleAssignmentsGenerator.IDAM_PREFIX;

@DisplayName("FakeRoleAssignmentsGenerator")
class FakeRoleAssignmentsGeneratorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    private FakeRoleAssignmentsGenerator fakeRoleAssignmentsGenerator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        fakeRoleAssignmentsGenerator = new FakeRoleAssignmentsGenerator(userRepository, caseDefinitionRepository);
    }

    @Nested
    @DisplayName("addFakeRoleAssignments()")
    class AddFakeRoleAssignments {

        public static final String JURISDICTION = "DIVORCE";
        public static final String ROLE_SOLICITOR = "caseworker-divorce-solicitor";
        public static final String EXPECTED_ROLE_SOLICITOR = IDAM_PREFIX + "caseworker-divorce-solicitor";
        public static final String ROLE_LOCAL_AUTHORITY = "caseworker-divorce-localAuthority";
        public static final String EXPECTED_ROLE_LOCAL_AUTHORITY = IDAM_PREFIX + "caseworker-divorce-localAuthority";
        public static final String ROLE_PROVIDED = "provided";
        public static final String ROLE_CASEWORKER_1 = "caseworker";
        public static final String EXPECTED_ROLE_CASEWORKER_1 = IDAM_PREFIX + "caseworker";
        public static final String CASEWORKER_1_CLASSIFICATION = "PUBLIC";
        public static final String ROLE_CASEWORKER_2 = "caseworker-divorce";
        public static final String EXPECTED_ROLE_CASEWORKER_2 = IDAM_PREFIX + "caseworker-divorce";
        public static final String CASEWORKER_2_CLASSIFICATION = "PRIVATE";

        @Test
        @DisplayName("should add fake RoleAssignments for granted user roles")
        public void shouldAddFakeRoleAssignmentsForGrantedUserRoles() {
            given(userRepository.getUserRoles()).willReturn(new HashSet<>(asList(ROLE_CASEWORKER_1,
                                                                                 ROLE_CASEWORKER_2,
                                                                                 ROLE_SOLICITOR,
                                                                                 ROLE_LOCAL_AUTHORITY)));

            List<RoleAssignment> roleAssignments = singletonList(caseRoleAssignment());

            List<RoleAssignment> augmentedRoleAssignments = fakeRoleAssignmentsGenerator
                .addFakeRoleAssignments(roleAssignments);

            Optional<RoleAssignment> providedRoleAssignment = augmentedRoleAssignments
                .stream().filter(ra -> ROLE_PROVIDED.equals(ra.getRoleName())).findFirst();
            Optional<RoleAssignment> solicitorRoleAssignment = augmentedRoleAssignments
                .stream().filter(ra -> EXPECTED_ROLE_SOLICITOR.equals(ra.getRoleName())).findFirst();
            Optional<RoleAssignment> localAuthorityRoleAssignment = augmentedRoleAssignments
                .stream().filter(ra -> EXPECTED_ROLE_LOCAL_AUTHORITY.equals(ra.getRoleName())).findFirst();

            assertAll(
                () -> assertThat(augmentedRoleAssignments.size(), is(3)),

                () -> assertTrue(providedRoleAssignment.isPresent()),
                () -> assertTrue(solicitorRoleAssignment.isPresent()),
                () -> assertTrue(localAuthorityRoleAssignment.isPresent()),

                () -> assertThat(providedRoleAssignment.get().getRoleName(), is(ROLE_PROVIDED)),
                () -> assertNull(providedRoleAssignment.get().getGrantType()),
                () -> assertNull(providedRoleAssignment.get().getClassification()),

                () -> assertThat(solicitorRoleAssignment.get().getRoleName(), is(EXPECTED_ROLE_SOLICITOR)),
                () -> assertThat(solicitorRoleAssignment.get().getGrantType(), is(GRANT_TYPE_SPECIFIC)),
                () -> assertThat(solicitorRoleAssignment.get().getClassification(), is(CLASSIFICATION_RESTRICTED)),

                () -> assertThat(localAuthorityRoleAssignment.get().getRoleName(), is(EXPECTED_ROLE_LOCAL_AUTHORITY)),
                () -> assertThat(localAuthorityRoleAssignment.get().getGrantType(), is(GRANT_TYPE_SPECIFIC)),
                () -> assertThat(localAuthorityRoleAssignment.get().getClassification(), is(CLASSIFICATION_RESTRICTED)),

                () -> verify(userRepository).getUserRoles()
            );
        }

        @Test
        @DisplayName("should not add fake RoleAssignments for granted roles when no single case RoleAssignment present")
        public void shouldNotAddFakeRoleAssignmentsForGrantedUserRolesWhenNoSingleCaseRoleAssignmentPresent() {
            given(userRepository.getUserRoles()).willReturn(new HashSet<>(asList(ROLE_CASEWORKER_1,
                                                                                 ROLE_CASEWORKER_2,
                                                                                 ROLE_SOLICITOR,
                                                                                 ROLE_LOCAL_AUTHORITY)));

            List<RoleAssignment> roleAssignments = singletonList(organisationRoleAssignment());

            List<RoleAssignment> augmentedRoleAssignments = fakeRoleAssignmentsGenerator
                .addFakeRoleAssignments(roleAssignments);

            assertAll(
                () -> assertThat(augmentedRoleAssignments.size(), is(1)),
                () -> assertThat(augmentedRoleAssignments.get(0).getRoleName(), is(ROLE_PROVIDED))
            );
        }

        @Test
        @DisplayName("should add fake RoleAssignments for non-granted user roles")
        public void shouldAddFakeRoleAssignmentsForNonGrantedUserRoles() {
            List<String> userRoles = asList(ROLE_CASEWORKER_2, ROLE_CASEWORKER_1);
            List<UserRole> userRolesWithClassification =
                asList(createUserRole(ROLE_CASEWORKER_1, CASEWORKER_1_CLASSIFICATION),
                       createUserRole(ROLE_CASEWORKER_2, CASEWORKER_2_CLASSIFICATION));
            given(userRepository.getUserRoles()).willReturn(new HashSet<>(userRoles));
            given(caseDefinitionRepository.getClassificationsForUserRoleList(anyList()))
                .willReturn(userRolesWithClassification);

            List<RoleAssignment> roleAssignments = singletonList(organisationRoleAssignment());

            List<RoleAssignment> augmentedRoleAssignments = fakeRoleAssignmentsGenerator
                .addFakeRoleAssignments(roleAssignments);

            Optional<RoleAssignment> providedRoleAssignment = augmentedRoleAssignments
                .stream().filter(ra -> ROLE_PROVIDED.equals(ra.getRoleName())).findFirst();
            Optional<RoleAssignment> caseworker1RoleAssignment = augmentedRoleAssignments
                .stream().filter(ra -> EXPECTED_ROLE_CASEWORKER_1.equals(ra.getRoleName())).findFirst();
            Optional<RoleAssignment> caseworker2RoleAssignment = augmentedRoleAssignments
                .stream().filter(ra -> EXPECTED_ROLE_CASEWORKER_2.equals(ra.getRoleName())).findFirst();

            assertAll(
                () -> assertThat(augmentedRoleAssignments.size(), is(3)),

                () -> assertTrue(providedRoleAssignment.isPresent()),
                () -> assertTrue(caseworker1RoleAssignment.isPresent()),
                () -> assertTrue(caseworker2RoleAssignment.isPresent()),

                () -> assertThat(providedRoleAssignment.get().getRoleName(), is(ROLE_PROVIDED)),
                () -> assertNull(providedRoleAssignment.get().getGrantType()),
                () -> assertNull(providedRoleAssignment.get().getClassification()),

                () -> assertThat(caseworker1RoleAssignment.get().getRoleName(), is(EXPECTED_ROLE_CASEWORKER_1)),
                () -> assertThat(caseworker1RoleAssignment.get().getGrantType(), is(GRANT_TYPE_STANDARD)),
                () -> assertThat(caseworker1RoleAssignment.get().getClassification(), is(CASEWORKER_1_CLASSIFICATION)),

                () -> assertThat(caseworker2RoleAssignment.get().getRoleName(), is(EXPECTED_ROLE_CASEWORKER_2)),
                () -> assertThat(caseworker2RoleAssignment.get().getGrantType(), is(GRANT_TYPE_STANDARD)),
                () -> assertThat(caseworker2RoleAssignment.get().getClassification(), is(CASEWORKER_2_CLASSIFICATION)),

                () -> verify(userRepository).getUserRoles()
            );
        }

        private RoleAssignment caseRoleAssignment() {
            return roleAssignment(Optional.of("12345"));
        }

        private RoleAssignment organisationRoleAssignment() {
            return roleAssignment(Optional.empty());
        }

        private RoleAssignment roleAssignment(Optional<String> caseId) {
            return RoleAssignment.builder()
                .roleName(ROLE_PROVIDED)
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
