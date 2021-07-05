package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentRepository;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResponse;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleCategory;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;

@DisplayName("RoleAssignmentService")
class RoleAssignmentServiceTest {

    private static final String USER_ID = "user1";
    private static final String CASE_ID = "111111";

    private List<String> caseIds = Arrays.asList("111", "222");
    private List<String> userIds = Arrays.asList("111", "222");

    @Mock
    private RoleAssignmentRepository roleAssignmentRepository;
    @Mock
    private RoleAssignmentsMapper roleAssignmentsMapper;
    @Mock
    private RoleAssignments mockedRoleAssignments;
    @Mock
    private RoleAssignmentResponse mockedRoleAssignmentResponse;
    @Mock
    private RoleAssignmentsFilteringService roleAssignmentFilteringService;
    @Mock
    private CaseTypeDefinition caseTypeDefinition;
    @Mock
    private RoleAssignmentCategoryService roleAssignmentCategoryService;

    private RoleAssignmentService roleAssignmentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        BDDMockito.given(roleAssignmentCategoryService.getRoleCategory(USER_ID))
            .willReturn(RoleCategory.PROFESSIONAL);

        roleAssignmentService = new RoleAssignmentService(roleAssignmentRepository,
            roleAssignmentsMapper, roleAssignmentFilteringService, roleAssignmentCategoryService);
    }

    private RoleAssignments getRoleAssignments() {

        final Instant currentTIme = Instant.now();
        final long oneHour = 3600000;

        final RoleAssignmentAttributes roleAssignmentAttributes =
            RoleAssignmentAttributes.builder().caseId(Optional.of(CASE_ID)).build();

        final List<RoleAssignment> roleAssignments = Arrays.asList(

            RoleAssignment.builder().actorId("actorId").roleType(RoleType.CASE.name())
                .attributes(roleAssignmentAttributes)
                .beginTime(currentTIme.minusMillis(oneHour)).endTime(currentTIme.plusMillis(oneHour)).build(),

            RoleAssignment.builder().actorId("actorId1").roleType(RoleType.CASE.name())
                .attributes(roleAssignmentAttributes)
                .beginTime(currentTIme.minusMillis(oneHour)).endTime(currentTIme.plusMillis(oneHour)).build()
        );
        return RoleAssignments.builder().roleAssignments(roleAssignments).build();
    }

    @Nested
    @DisplayName("getRoleAssignments()")
    class GetRoleAssignments {

        @Test
        public void shouldGetRoleAssignments() {
            given(roleAssignmentRepository.getRoleAssignments(USER_ID))
                .willReturn(mockedRoleAssignmentResponse);
            given(roleAssignmentsMapper.toRoleAssignments(mockedRoleAssignmentResponse))
                .willReturn(mockedRoleAssignments);

            RoleAssignments roleAssignments = roleAssignmentService.getRoleAssignments(USER_ID);

            assertThat(roleAssignments, is(mockedRoleAssignments));
        }
    }


    @Nested
    @DisplayName("findRoleAssignmentsByCasesAndUsers()")
    class GetRoleAssignmentsByCasesAndUsers {

        @Test
        public void shouldGetRoleAssignments() {

            given(roleAssignmentRepository.findRoleAssignmentsByCasesAndUsers(caseIds, userIds))
                .willReturn(mockedRoleAssignmentResponse);

            given(roleAssignmentsMapper.toRoleAssignments(mockedRoleAssignmentResponse))
                .willReturn(getRoleAssignments());

            final List<CaseAssignedUserRole> caseAssignedUserRole =
                roleAssignmentService.findRoleAssignmentsByCasesAndUsers(caseIds, userIds);

            assertTrue(caseAssignedUserRole.size() == 2);
            assertThat(caseAssignedUserRole.get(0).getCaseDataId(), is(CASE_ID));
        }


    }

    @Nested
    @DisplayName("getCaseReferencesForAGivenUser(String userId)")
    class GetCaseIdsForAGivenUser {

        @Test
        public void shouldGetRoleAssignments() {

            given(roleAssignmentRepository.getRoleAssignments(USER_ID))
                .willReturn(mockedRoleAssignmentResponse);

            given(roleAssignmentsMapper.toRoleAssignments(mockedRoleAssignmentResponse))
                .willReturn(getRoleAssignments());

            List<String> resultCases =
                roleAssignmentService.getCaseReferencesForAGivenUser(USER_ID);

            assertTrue(resultCases.size() == 2);
        }
    }

    @Nested
    @DisplayName("getCaseReferencesForAGivenUser(String userId, CaseTypeDefinition caseTypeDefinition)")
    class GetCaseIdsForAGivenUserAndCaseTypeDefinition {

        @Test
        public void shouldGetRoleAssignments() {

            given(roleAssignmentRepository.getRoleAssignments(USER_ID))
                .willReturn(mockedRoleAssignmentResponse);

            RoleAssignments roleAssignments = getRoleAssignments();
            given(roleAssignmentsMapper.toRoleAssignments(mockedRoleAssignmentResponse))
                .willReturn(roleAssignments);
            given(roleAssignmentFilteringService.filter(roleAssignments, caseTypeDefinition))
                .willReturn(roleAssignments.getRoleAssignments());

            List<String> resultCases =
                roleAssignmentService.getCaseReferencesForAGivenUser(USER_ID, caseTypeDefinition);

            assertTrue(resultCases.size() == 2);
            roleAssignmentFilteringService.filter(roleAssignments, caseTypeDefinition);
        }
    }
}
