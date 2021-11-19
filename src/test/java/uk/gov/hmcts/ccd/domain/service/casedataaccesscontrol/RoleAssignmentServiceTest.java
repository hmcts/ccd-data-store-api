package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import com.google.common.collect.Lists;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentQuery;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentRepository;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentRequestResource;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentRequestResponse;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResource;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResponse;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleRequestResource;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentsDeleteRequest;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.ActorIdType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.Classification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.MatcherType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.never;

@DisplayName("RoleAssignmentService")
@ExtendWith(MockitoExtension.class)
class RoleAssignmentServiceTest {

    private static final String USER_ID = "user1";
    private static final String USER_ID_2 = "user2";
    private static final String CASE_ID = "111111";
    private static final String CASE_ID_2 = "222222";

    private static final RoleCategory ROLE_CATEGORY_4_USER_1 = RoleCategory.PROFESSIONAL;
    private static final RoleCategory ROLE_CATEGORY_4_USER_2 = RoleCategory.JUDICIAL;

    private final List<String> caseIds = Arrays.asList(CASE_ID, CASE_ID_2);
    private final List<String> userIds = Arrays.asList(USER_ID, USER_ID_2);

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


    @Nested
    @DisplayName("createCaseRoleAssignments()")
    @SuppressWarnings("ConstantConditions")
    class CreateCaseRoleAssignments {

        private ArgumentCaptor<RoleAssignmentRequestResource> roleAssignmentRequestResourceCaptor;

        @BeforeEach
        void setUp() {

            RoleAssignmentRequestResponse roleAssignmentRequestResponse
                = RoleAssignmentRequestResponse.builder().build();

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

            given(roleAssignmentCategoryService.getRoleCategory(USER_ID)).willReturn(ROLE_CATEGORY_4_USER_1);

            // WHEN
            RoleAssignments roleAssignments = roleAssignmentService.createCaseRoleAssignments(caseDetails,
                                                                                              USER_ID,
                                                                                              roles,
                                                                                              replaceExisting);

            // THEN
            assertThat(roleAssignments, is(mockedRoleAssignments));
            // verify RoleCategory has been loaded from service
            verify(roleAssignmentCategoryService).getRoleCategory(USER_ID);
            // verify data passed to repository has correct values
            verify(roleAssignmentRepository).createRoleAssignment(roleAssignmentRequestResourceCaptor.capture());
            RoleAssignmentRequestResource assignmentRequest = roleAssignmentRequestResourceCaptor.getValue();
            assertAll(
                () -> assertCorrectlyPopulatedRoleRequest(
                    caseDetails,
                    USER_ID,
                    replaceExisting,
                    assignmentRequest.getRoleRequest()
                ),

                () -> assertCorrectlyPopulatedRequestedRoles(
                    caseDetails,
                    USER_ID,
                    roles,
                    ROLE_CATEGORY_4_USER_1,
                    assignmentRequest.getRequestedRoles()
                )
            );
        }

        @Test
        void shouldCreateMultipleCaseRoleAssignments() {

            // GIVEN
            CaseDetails caseDetails = createCaseDetails();
            Set<String> roles = Set.of("[ROLE1]", "[ROLE2]");
            boolean replaceExisting = true;

            given(roleAssignmentCategoryService.getRoleCategory(USER_ID_2)).willReturn(ROLE_CATEGORY_4_USER_2);

            // WHEN
            RoleAssignments roleAssignments = roleAssignmentService.createCaseRoleAssignments(caseDetails,
                                                                                              USER_ID_2,
                                                                                              roles,
                                                                                              replaceExisting);

            // THEN
            assertThat(roleAssignments, is(mockedRoleAssignments));
            // verify RoleCategory has been loaded from service
            verify(roleAssignmentCategoryService).getRoleCategory(USER_ID_2);
            // verify data passed to repository has correct values
            verify(roleAssignmentRepository).createRoleAssignment(roleAssignmentRequestResourceCaptor.capture());
            RoleAssignmentRequestResource assignmentRequest = roleAssignmentRequestResourceCaptor.getValue();
            assertAll(
                () -> assertCorrectlyPopulatedRoleRequest(
                    caseDetails,
                    USER_ID_2, // NB: using different USER ID to verify different RoleCategory
                    replaceExisting,
                    assignmentRequest.getRoleRequest()
                ),

                () -> assertCorrectlyPopulatedRequestedRoles(
                    caseDetails,
                    USER_ID_2, // NB: using different USER ID to verify different RoleCategory
                    roles,
                    ROLE_CATEGORY_4_USER_2, // NB: using different USER ID to verify different RoleCategory
                    assignmentRequest.getRequestedRoles()
                )
            );
        }

        private void assertCorrectlyPopulatedRoleRequest(final CaseDetails expectedCaseDetails,
                                                         final String expectedUserId,
                                                         final boolean expectedReplaceExisting,
                                                         final RoleRequestResource actualRoleRequest) {

            assertNotNull(actualRoleRequest);
            assertAll(
                () -> assertEquals(expectedUserId, actualRoleRequest.getAssignerId()),
                () -> assertEquals(RoleAssignmentRepository.DEFAULT_PROCESS, actualRoleRequest.getProcess()),
                () -> assertEquals(expectedCaseDetails.getReference() + "-" + expectedUserId,
                        actualRoleRequest.getReference()),
                () -> assertEquals(expectedReplaceExisting, actualRoleRequest.isReplaceExisting())
            );
        }

        private void assertCorrectlyPopulatedRequestedRoles(final CaseDetails expectedCaseDetails,
                                                            final String expectedUserId,
                                                            final Set<String> expectedRoles,
                                                            final RoleCategory expectedRoleCategory,
                                                            final List<RoleAssignmentResource> actualRequestedRoles) {
            assertNotNull(actualRequestedRoles);
            assertEquals(expectedRoles.size(), actualRequestedRoles.size());

            Map<String, RoleAssignmentResource> roleMap = actualRequestedRoles.stream()
                .collect(Collectors.toMap(RoleAssignmentResource::getRoleName, role -> role));

            expectedRoles.forEach(roleName -> assertAll(
                () -> assertTrue(roleMap.containsKey(roleName)),
                () -> assertCorrectlyPopulatedRoleAssignment(
                    expectedCaseDetails,
                    expectedUserId,
                    roleName,
                    expectedRoleCategory,
                    roleMap.get(roleName)
                )
            ));
        }

        private void assertCorrectlyPopulatedRoleAssignment(final CaseDetails expectedCaseDetails,
                                                            final String expectedUserId,
                                                            final String expectedRoleName,
                                                            final RoleCategory expectedRoleCategory,
                                                            final RoleAssignmentResource actualRoleAssignment) {

            assertNotNull(actualRoleAssignment);
            assertAll(
                () -> assertEquals(expectedUserId, actualRoleAssignment.getActorId()),
                () -> assertEquals(expectedRoleName, actualRoleAssignment.getRoleName()),

                // defaults
                () -> assertEquals(ActorIdType.IDAM.name(), actualRoleAssignment.getActorIdType()),
                () -> assertEquals(RoleType.CASE.name(), actualRoleAssignment.getRoleType()),
                () -> assertEquals(Classification.RESTRICTED.name(), actualRoleAssignment.getClassification()),
                () -> assertEquals(GrantType.SPECIFIC.name(), actualRoleAssignment.getGrantType()),
                () -> assertEquals(expectedRoleCategory.name(), actualRoleAssignment.getRoleCategory()),
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

    @Nested
    @DisplayName("deleteRoleAssignments()")
    @SuppressWarnings({"ConstantConditions", "FieldCanBeLocal"})
    class DeleteRoleAssignments {

        @Captor
        private ArgumentCaptor<List<RoleAssignmentQuery>> queryRequestsCaptor;

        private final String role1 = "[ROLE1]";
        private final String role2 = "[ROLE2]";

        @Test
        void shouldDoNothingForNullDeleteRequests() {

            // GIVEN
            List<RoleAssignmentsDeleteRequest> deleteRequests = null;

            // WHEN
            roleAssignmentService.deleteRoleAssignments(deleteRequests);

            // THEN
            verify(roleAssignmentRepository, never()).deleteRoleAssignmentsByQuery(any());
        }

        @Test
        void shouldDoNothingForEmptyDeleteRequests() {

            // GIVEN
            List<RoleAssignmentsDeleteRequest> deleteRequests = new ArrayList<>();

            // WHEN
            roleAssignmentService.deleteRoleAssignments(deleteRequests);

            // THEN
            verify(roleAssignmentRepository, never()).deleteRoleAssignmentsByQuery(any());
        }

        @Test
        void shouldDeleteForSingleDeleteRequests() {

            // GIVEN
            List<RoleAssignmentsDeleteRequest> deleteRequests = List.of(
                RoleAssignmentsDeleteRequest.builder()
                    .caseId(CASE_ID)
                    .userId(USER_ID)
                    .roleNames(List.of(role1)).build()
            );

            // WHEN
            roleAssignmentService.deleteRoleAssignments(deleteRequests);

            // THEN
            // verify data passed to repository has correct values
            verify(roleAssignmentRepository).deleteRoleAssignmentsByQuery(queryRequestsCaptor.capture());
            List<RoleAssignmentQuery> queryRequests = queryRequestsCaptor.getValue();

            assertAll(
                () -> assertEquals(deleteRequests.size(), queryRequests.size()),
                () -> assertCorrectlyPopulatedRoleAssignmentQueries(deleteRequests, queryRequests)
            );
        }

        @Test
        void shouldDeleteForMultipleDeleteRequests() {

            // GIVEN
            List<RoleAssignmentsDeleteRequest> deleteRequests = List.of(
                RoleAssignmentsDeleteRequest.builder()
                    .caseId(CASE_ID)
                    .userId(USER_ID)
                    .roleNames(List.of(role1)).build(),

                RoleAssignmentsDeleteRequest.builder()
                    .caseId(CASE_ID)
                    .userId(USER_ID_2) // NB: using different user ID in test data to match assert function's map
                    .roleNames(List.of(role1, role2)).build()
            );

            // WHEN
            roleAssignmentService.deleteRoleAssignments(deleteRequests);

            // THEN
            // verify data passed to repository has correct values
            verify(roleAssignmentRepository).deleteRoleAssignmentsByQuery(queryRequestsCaptor.capture());
            List<RoleAssignmentQuery> queryRequests = queryRequestsCaptor.getValue();

            assertAll(
                () -> assertEquals(deleteRequests.size(), queryRequests.size()),
                () -> assertCorrectlyPopulatedRoleAssignmentQueries(deleteRequests, queryRequests)
            );
        }

        private void assertCorrectlyPopulatedRoleAssignmentQueries(
            final List<RoleAssignmentsDeleteRequest> expectedDeleteRequests,
            final List<RoleAssignmentQuery> actualRoleAssignmentQueries
        ) {
            assertNotNull(actualRoleAssignmentQueries);
            assertEquals(expectedDeleteRequests.size(), actualRoleAssignmentQueries.size());

            // create map by userID (NB: this relies on the test data using a unique user_id for each query)
            Map<String, RoleAssignmentQuery> queryMapByUser = actualRoleAssignmentQueries.stream()
                .collect(Collectors.toMap(query -> query.getActorId().get(0), query -> query));

            expectedDeleteRequests.forEach(expectedDeleteRequest -> assertAll(
                () -> assertTrue(queryMapByUser.containsKey(expectedDeleteRequest.getUserId())),
                () -> assertCorrectlyPopulatedRoleAssignmentQuery(
                    expectedDeleteRequest,
                    queryMapByUser.get(expectedDeleteRequest.getUserId())
                )
            ));
        }

        private void assertCorrectlyPopulatedRoleAssignmentQuery(
            final RoleAssignmentsDeleteRequest expectedDeleteRequest,
            final RoleAssignmentQuery actualRoleAssignmentQuery
        ) {
            assertNotNull(actualRoleAssignmentQuery);
            assertAll(
                // verify format
                () -> assertEquals(1, actualRoleAssignmentQuery.getAttributes().getCaseId().size()),
                () -> assertEquals(1, actualRoleAssignmentQuery.getActorId().size()),
                () -> assertEquals(1, actualRoleAssignmentQuery.getRoleType().size()),
                () -> assertEquals(
                    expectedDeleteRequest.getRoleNames().size(), actualRoleAssignmentQuery.getRoleName().size()
                ),

                // verify data
                () -> assertEquals(
                    expectedDeleteRequest.getCaseId(), actualRoleAssignmentQuery.getAttributes().getCaseId().get(0)
                ),
                () -> assertEquals(expectedDeleteRequest.getUserId(), actualRoleAssignmentQuery.getActorId().get(0)),
                () -> assertEquals(RoleType.CASE.name(), actualRoleAssignmentQuery.getRoleType().get(0)),
                () -> assertArrayEquals(
                    expectedDeleteRequest.getRoleNames().toArray(), actualRoleAssignmentQuery.getRoleName().toArray()
                )
            );
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

            // GIVEN
            given(roleAssignmentRepository.getRoleAssignments(USER_ID))
                .willReturn(mockedRoleAssignmentResponse);
            given(roleAssignmentsMapper.toRoleAssignments(mockedRoleAssignmentResponse))
                .willReturn(mockedRoleAssignments);

            // WHEN
            RoleAssignments roleAssignments = roleAssignmentService.getRoleAssignments(USER_ID);

            // THEN
            assertThat(roleAssignments, is(mockedRoleAssignments));
        }

    }

    @Nested
    @DisplayName("getRoleAssignmentsForCreate()")
    class GetRoleAssignmentsForCreate {

        @Test
        void shouldGetRoleAssignmentsForCreate() {

            // GIVEN
            final var expectedResult =  new RoleAssignments();
            expectedResult.setRoleAssignments(new ArrayList<>());
            given(roleAssignmentRepository.getRoleAssignments(USER_ID))
                .willReturn(mockedRoleAssignmentResponse);
            given(roleAssignmentsMapper.toRoleAssignments(mockedRoleAssignmentResponse))
                .willReturn(mockedRoleAssignments);

            // WHEN
            final var roleAssignments = roleAssignmentService.getRoleAssignmentsForCreate(USER_ID);

            // THEN
            assertThat(roleAssignments, is(expectedResult));
        }
    }


    @Nested
    @DisplayName("findRoleAssignmentsByCasesAndUsers()")
    class FindRoleAssignmentsByCasesAndUsers {

        @Test
        void shouldFindRoleAssignments() {

            // GIVEN
            given(roleAssignmentRepository.findRoleAssignmentsByCasesAndUsers(caseIds, userIds))
                .willReturn(mockedRoleAssignmentResponse);

            given(roleAssignmentsMapper.toRoleAssignments(mockedRoleAssignmentResponse))
                .willReturn(createTestRoleAssignments(CASE_ID));

            // WHEN
            final List<CaseAssignedUserRole> caseAssignedUserRole =
                roleAssignmentService.findRoleAssignmentsByCasesAndUsers(caseIds, userIds);

            // THEN
            assertEquals(2, caseAssignedUserRole.size());
            assertThat(caseAssignedUserRole.get(0).getCaseDataId(), is(CASE_ID));
        }

    }


    @Nested
    @DisplayName("getCaseReferencesForAGivenUser(String userId)")
    class GetCaseReferencesForAGivenUser {

        @Test
        void shouldGetReferencesWithoutDuplicatesSingleCase() {

            // GIVEN
            given(roleAssignmentRepository.getRoleAssignments(USER_ID))
                .willReturn(mockedRoleAssignmentResponse);

            given(roleAssignmentsMapper.toRoleAssignments(mockedRoleAssignmentResponse))
                .willReturn(createTestRoleAssignments(CASE_ID));

            // WHEN
            List<String> resultCases =
                roleAssignmentService.getCaseReferencesForAGivenUser(USER_ID);

            // THEN
            assertEquals(1, resultCases.size());
            assertEquals(CASE_ID, resultCases.get(0));
        }

        @Test
        void shouldGetReferencesWithoutDuplicatesMultipleCases() {

            // GIVEN
            given(roleAssignmentRepository.getRoleAssignments(USER_ID))
                .willReturn(mockedRoleAssignmentResponse);

            given(roleAssignmentsMapper.toRoleAssignments(mockedRoleAssignmentResponse))
                .willReturn(createTestRoleAssignmentsMultipleCases());

            // WHEN
            List<String> resultCases =
                roleAssignmentService.getCaseReferencesForAGivenUser(USER_ID);

            // THEN
            assertEquals(2, resultCases.size());
            assertTrue(resultCases.containsAll(caseIds));
        }

    }


    @Nested
    @DisplayName("getCaseReferencesForAGivenUser(String userId, CaseTypeDefinition caseTypeDefinition)")
    class GetCaseReferencesForAGivenUserAndCaseType {

        @Test
        void shouldGetCaseReferencesWithoutDuplicatesSingleCase() {

            // GIVEN
            given(roleAssignmentRepository.getRoleAssignments(USER_ID))
                .willReturn(mockedRoleAssignmentResponse);

            // test role assignments: single case with two case-roles
            RoleAssignments roleAssignments = createTestRoleAssignments(CASE_ID);
            given(roleAssignmentsMapper.toRoleAssignments(mockedRoleAssignmentResponse))
                .willReturn(roleAssignments);

            given(filteredRoleAssignments.getFilteredMatchingRoleAssignments())
                .willReturn(roleAssignments.getRoleAssignments());
            given(roleAssignmentFilteringService.filter(roleAssignments, caseTypeDefinition))
                .willReturn(filteredRoleAssignments);

            // WHEN
            List<String> resultCases =
                roleAssignmentService.getCaseReferencesForAGivenUser(USER_ID, caseTypeDefinition);

            // THEN
            assertEquals(1, resultCases.size()); // single case
            verify(roleAssignmentFilteringService).filter(roleAssignments, caseTypeDefinition);
        }

        @Test
        void shouldGetCaseReferencesWithoutDuplicatesMultipleCases() {

            // GIVEN
            given(roleAssignmentRepository.getRoleAssignments(USER_ID))
                .willReturn(mockedRoleAssignmentResponse);

            // test role assignments: two cases each with two case-roles
            RoleAssignments roleAssignments = createTestRoleAssignmentsMultipleCases();
            given(roleAssignmentsMapper.toRoleAssignments(mockedRoleAssignmentResponse))
                .willReturn(roleAssignments);

            given(filteredRoleAssignments.getFilteredMatchingRoleAssignments())
                .willReturn(roleAssignments.getRoleAssignments());
            given(roleAssignmentFilteringService.filter(roleAssignments, caseTypeDefinition))
                .willReturn(filteredRoleAssignments);

            // WHEN
            List<String> resultCases =
                roleAssignmentService.getCaseReferencesForAGivenUser(USER_ID, caseTypeDefinition);

            // THEN
            assertEquals(2, resultCases.size()); // multiple cases
            verify(roleAssignmentFilteringService).filter(roleAssignments, caseTypeDefinition);
        }

        @Test
        public void shouldGetRoleAssignmentsBasedOnExcluded() {

            given(roleAssignmentRepository.getRoleAssignments(USER_ID))
                .willReturn(mockedRoleAssignmentResponse);

            RoleAssignments roleAssignments = getRoleAssignments();
            given(roleAssignmentsMapper.toRoleAssignments(mockedRoleAssignmentResponse))
                .willReturn(roleAssignments);

            given(filteredRoleAssignments.getFilteredMatchingRoleAssignments())
                .willReturn(roleAssignments.getRoleAssignments());
            given(roleAssignmentFilteringService.filter(roleAssignments, caseTypeDefinition,
                Lists.newArrayList(MatcherType.GRANTTYPE,
                    MatcherType.SECURITYCLASSIFICATION,
                    MatcherType.AUTHORISATION)))
                .willReturn(filteredRoleAssignments);

            List<RoleAssignment> resultCases =
                roleAssignmentService.getRoleAssignments(USER_ID, caseTypeDefinition);

            assertTrue(resultCases.size() == 2);
            roleAssignmentFilteringService.filter(roleAssignments, caseTypeDefinition);
        }

    }

    /**
     *  Create test role assignments: single case with two case-roles.
     */
    private RoleAssignments createTestRoleAssignments(String caseId) {
        final Instant currentTIme = Instant.now();
        final long oneHour = 3600000;

        final RoleAssignmentAttributes roleAssignmentAttributes =
            RoleAssignmentAttributes.builder().caseId(Optional.of(caseId)).build();

        final List<RoleAssignment> roleAssignments = Arrays.asList(

            RoleAssignment.builder().actorId(USER_ID).roleType(RoleType.CASE.name())
                .attributes(roleAssignmentAttributes)
                .beginTime(currentTIme.minusMillis(oneHour)).endTime(currentTIme.plusMillis(oneHour)).build(),

            RoleAssignment.builder().actorId(USER_ID).roleType(RoleType.CASE.name())
                .attributes(roleAssignmentAttributes)
                .beginTime(currentTIme.minusMillis(oneHour)).endTime(currentTIme.plusMillis(oneHour)).build()
        );
        return RoleAssignments.builder().roleAssignments(roleAssignments).build();
    }

    /**
     *  Create multiple test role assignments: two cases each with two case-roles.
     */
    private RoleAssignments createTestRoleAssignmentsMultipleCases() {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        roleAssignments.addAll(createTestRoleAssignments(CASE_ID).getRoleAssignments());
        roleAssignments.addAll(createTestRoleAssignments(CASE_ID_2).getRoleAssignments());

        return RoleAssignments.builder().roleAssignments(roleAssignments).build();
    }

}
