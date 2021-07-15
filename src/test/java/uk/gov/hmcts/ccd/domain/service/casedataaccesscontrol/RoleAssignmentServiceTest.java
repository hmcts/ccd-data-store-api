package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentRepository;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentRequestResource;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentRequestResponse;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResource;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResponse;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleRequestResource;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.ActorIdType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.Classification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;

@DisplayName("RoleAssignmentService")
class RoleAssignmentServiceTest {

    private static final String USER_ID = "user1";
    private static final String CASE_ID = "111111";

    private final List<String> caseIds = Arrays.asList("111", "222");
    private final List<String> userIds = Arrays.asList("111", "222");

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
    private FilteredRoleAssignments filteredRoleAssignments;
    @Mock
    private RoleAssignmentCategoryService roleAssignmentCategoryService;

    @InjectMocks
    private RoleAssignmentService roleAssignmentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        given(roleAssignmentCategoryService.getRoleCategory(USER_ID)).willReturn(RoleCategory.PROFESSIONAL);
    }

    @Nested
    @DisplayName("createCaseRoleAssignments()")
    class CreateCaseRoleAssignments {

        private ArgumentCaptor<RoleAssignmentRequestResource> roleAssignmentRequestResourceCaptor;

        @BeforeEach
        void setUp() {

            RoleAssignmentRequestResponse roleAssignmentRequestResponse
                = Mockito.mock(RoleAssignmentRequestResponse.class);

            given(roleAssignmentRepository.createRoleAssignment(any(RoleAssignmentRequestResource.class)))
                .willReturn(roleAssignmentRequestResponse);
            given(roleAssignmentsMapper.toRoleAssignments(roleAssignmentRequestResponse))
                .willReturn(mockedRoleAssignments);

            roleAssignmentRequestResourceCaptor = ArgumentCaptor.forClass(RoleAssignmentRequestResource.class);
        }

        @Test
        void shouldCreateSingleCaseRoleAssignments() {

            // GIVEN
            CaseDetails caseDetails = createCaseDetails();
            Set<String> roles = Set.of("[ROLE1]");
            boolean replaceExisting = false;

            // WHEN
            RoleAssignments roleAssignments = roleAssignmentService.createCaseRoleAssignments(caseDetails,
                                                                                              USER_ID,
                                                                                              roles,
                                                                                              replaceExisting);

            // THEN
            assertThat(roleAssignments, is(mockedRoleAssignments));
            // verify data passed to repository has correct values
            verify(roleAssignmentRepository).createRoleAssignment(roleAssignmentRequestResourceCaptor.capture());
            RoleAssignmentRequestResource assignmentRequest = roleAssignmentRequestResourceCaptor.getValue();
            assertAll(
                () -> assertCorrectlyPopulatedRoleRequest(caseDetails, replaceExisting, assignmentRequest.getRequest()),

                () -> assertCorrectlyPopulatedRequestedRoles(caseDetails, roles, assignmentRequest.getRequestedRoles())
            );
        }

        @Test
        void shouldCreateMultipleCaseRoleAssignments() {

            // GIVEN
            CaseDetails caseDetails = createCaseDetails();
            Set<String> roles = Set.of("[ROLE1]", "[ROLE2]");
            boolean replaceExisting = true;

            // WHEN
            RoleAssignments roleAssignments = roleAssignmentService.createCaseRoleAssignments(caseDetails,
                                                                                              USER_ID,
                                                                                              roles,
                                                                                              replaceExisting);

            // THEN
            assertThat(roleAssignments, is(mockedRoleAssignments));
            // verify data passed to repository has correct values
            verify(roleAssignmentRepository).createRoleAssignment(roleAssignmentRequestResourceCaptor.capture());
            RoleAssignmentRequestResource assignmentRequest = roleAssignmentRequestResourceCaptor.getValue();
            assertAll(
                () -> assertCorrectlyPopulatedRoleRequest(caseDetails, replaceExisting, assignmentRequest.getRequest()),

                () -> assertCorrectlyPopulatedRequestedRoles(caseDetails, roles, assignmentRequest.getRequestedRoles())
            );
        }

        private void assertCorrectlyPopulatedRoleRequest(final CaseDetails expectedCaseDetails,
                                                         final boolean expectedReplaceExisting,
                                                         final RoleRequestResource actualRoleRequest) {

            assertNotNull(actualRoleRequest);
            assertAll(
                () -> assertEquals(USER_ID, actualRoleRequest.getAssignerId()),
                () -> assertEquals(RoleAssignmentRepository.DEFAULT_PROCESS, actualRoleRequest.getProcess()),
                () -> assertEquals(expectedCaseDetails.getReference() + "-" + USER_ID,
                        actualRoleRequest.getReference()),
                () -> assertEquals(expectedReplaceExisting, actualRoleRequest.isReplaceExisting())
            );
        }

        private void assertCorrectlyPopulatedRequestedRoles(final CaseDetails expectedCaseDetails,
                                                             final Set<String> expectedRoles,
                                                             final List<RoleAssignmentResource> actualRequestedRoles) {
            assertNotNull(actualRequestedRoles);
            assertEquals(expectedRoles.size(), actualRequestedRoles.size());

            Map<String, RoleAssignmentResource> roleMap = actualRequestedRoles.stream()
                .collect(Collectors.toMap(RoleAssignmentResource::getRoleName, role -> role));

            expectedRoles.forEach(roleName -> assertAll(
                () -> assertTrue(roleMap.containsKey(roleName)),
                () -> assertCorrectlyPopulatedRoleAssignment(expectedCaseDetails, roleName, roleMap.get(roleName))
            ));
        }

        private void assertCorrectlyPopulatedRoleAssignment(final CaseDetails expectedCaseDetails,
                                                            final String expectedRoleName,
                                                            final RoleAssignmentResource actualRoleAssignment) {

            assertNotNull(actualRoleAssignment);
            assertAll(
                () -> assertEquals(USER_ID, actualRoleAssignment.getActorId()),
                () -> assertEquals(expectedRoleName, actualRoleAssignment.getRoleName()),

                // defaults
                () -> assertEquals(ActorIdType.IDAM.name(), actualRoleAssignment.getActorIdType()),
                () -> assertEquals(RoleType.CASE.name(), actualRoleAssignment.getRoleType()),
                () -> assertEquals(Classification.RESTRICTED.name(), actualRoleAssignment.getClassification()),
                () -> assertEquals(GrantType.SPECIFIC.name(), actualRoleAssignment.getGrantType()),
                () -> assertEquals(RoleCategory.PROFESSIONAL.name(), actualRoleAssignment.getRoleCategory()),
                () -> assertFalse(actualRoleAssignment.getReadOnly()),
                () -> assertNotNull(actualRoleAssignment.getBeginTime()),

                // attributes match case
                () -> assertEquals(
                    Optional.of(expectedCaseDetails.getReferenceAsString()),
                    actualRoleAssignment.getAttributes().getCaseId()
                ),
                () -> assertEquals(
                    Optional.of(expectedCaseDetails.getJurisdiction()),
                    actualRoleAssignment.getAttributes().getJurisdiction()
                ),
                () -> assertEquals(
                    Optional.of(expectedCaseDetails.getCaseTypeId()),
                    actualRoleAssignment.getAttributes().getCaseType()
                )
            );
        }

        private CaseDetails createCaseDetails() {
            CaseDetails caseDetails = new CaseDetails();
            caseDetails.setReference(123456L);
            caseDetails.setJurisdiction("test-jurisdiction");
            caseDetails.setCaseTypeId("case-type-id");
            return caseDetails;
        }

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
        void shouldGetRoleAssignments() {
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
        void shouldGetRoleAssignments() {

            given(roleAssignmentRepository.findRoleAssignmentsByCasesAndUsers(caseIds, userIds))
                .willReturn(mockedRoleAssignmentResponse);

            given(roleAssignmentsMapper.toRoleAssignments(mockedRoleAssignmentResponse))
                .willReturn(getRoleAssignments());

            final List<CaseAssignedUserRole> caseAssignedUserRole =
                roleAssignmentService.findRoleAssignmentsByCasesAndUsers(caseIds, userIds);

            assertEquals(2, caseAssignedUserRole.size());
            assertThat(caseAssignedUserRole.get(0).getCaseDataId(), is(CASE_ID));
        }

    }

    @Nested
    @DisplayName("getCaseReferencesForAGivenUser(String userId)")
    class GetCaseIdsForAGivenUser {

        @Test
        void shouldGetRoleAssignments() {

            given(roleAssignmentRepository.getRoleAssignments(USER_ID))
                .willReturn(mockedRoleAssignmentResponse);

            given(roleAssignmentsMapper.toRoleAssignments(mockedRoleAssignmentResponse))
                .willReturn(getRoleAssignments());

            List<String> resultCases =
                roleAssignmentService.getCaseReferencesForAGivenUser(USER_ID);

            assertEquals(2, resultCases.size());
        }
    }

    @Nested
    @DisplayName("getCaseReferencesForAGivenUser(String userId, CaseTypeDefinition caseTypeDefinition)")
    class GetCaseIdsForAGivenUserAndCaseTypeDefinition {

        @Test
        void shouldGetRoleAssignments() {

            given(roleAssignmentRepository.getRoleAssignments(USER_ID))
                .willReturn(mockedRoleAssignmentResponse);

            RoleAssignments roleAssignments = getRoleAssignments();
            given(roleAssignmentsMapper.toRoleAssignments(mockedRoleAssignmentResponse))
                .willReturn(roleAssignments);

            given(filteredRoleAssignments.getFilteredMatchingRoleAssignments())
                .willReturn(roleAssignments.getRoleAssignments());
            given(roleAssignmentFilteringService.filter(roleAssignments, caseTypeDefinition))
                .willReturn(filteredRoleAssignments);

            List<String> resultCases =
                roleAssignmentService.getCaseReferencesForAGivenUser(USER_ID, caseTypeDefinition);

            assertEquals(2, resultCases.size());
            roleAssignmentFilteringService.filter(roleAssignments, caseTypeDefinition);
        }
    }
}
