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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.assertj.core.api.Assertions.assertThat;
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
            assertThat(roleAssignments).isEqualTo(mockedRoleAssignments);
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
            assertThat(roleAssignments).isEqualTo(mockedRoleAssignments);
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

            assertThat(actualRoleRequest).isNotNull();
            assertAll(
                () -> assertThat(actualRoleRequest.getAssignerId()).isEqualTo(expectedUserId),
                () -> assertThat(actualRoleRequest.getProcess()).isEqualTo(RoleAssignmentRepository.DEFAULT_PROCESS),
                () -> assertThat(actualRoleRequest.getReference())
                    .isEqualTo(expectedCaseDetails.getReference() + "-" + expectedUserId),
                () -> assertThat(actualRoleRequest.isReplaceExisting()).isEqualTo(expectedReplaceExisting)
            );
        }

        private void assertCorrectlyPopulatedRequestedRoles(final CaseDetails expectedCaseDetails,
                                                            final String expectedUserId,
                                                            final Set<String> expectedRoles,
                                                            final RoleCategory expectedRoleCategory,
                                                            final List<RoleAssignmentResource> actualRequestedRoles) {
            assertThat(actualRequestedRoles).isNotNull();
            assertThat(actualRequestedRoles).hasSize(expectedRoles.size());

            Map<String, RoleAssignmentResource> roleMap = actualRequestedRoles.stream()
                .collect(Collectors.toMap(RoleAssignmentResource::getRoleName, role -> role));

            expectedRoles.forEach(roleName -> assertAll(
                () -> assertThat(roleMap).containsKey(roleName),
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

            assertThat(actualRoleAssignment).isNotNull();
            assertAll(
                () -> assertThat(actualRoleAssignment.getActorId()).isEqualTo(expectedUserId),
                () -> assertThat(actualRoleAssignment.getRoleName()).isEqualTo(expectedRoleName),

                // defaults
                () -> assertThat(actualRoleAssignment.getActorIdType()).isEqualTo(ActorIdType.IDAM.name()),
                () -> assertThat(actualRoleAssignment.getRoleType()).isEqualTo(RoleType.CASE.name()),
                () -> assertThat(actualRoleAssignment.getClassification()).isEqualTo(Classification.RESTRICTED.name()),
                () -> assertThat(actualRoleAssignment.getGrantType()).isEqualTo(GrantType.SPECIFIC.name()),
                () -> assertThat(actualRoleAssignment.getRoleCategory()).isEqualTo(expectedRoleCategory.name()),
                () -> assertThat(actualRoleAssignment.getReadOnly()).isFalse(),
                () -> assertThat(actualRoleAssignment.getBeginTime()).isNotNull(),

                // attributes match case
                () -> assertThat(actualRoleAssignment.getAttributes().getCaseId())
                    .isEqualTo(Optional.of(expectedCaseDetails.getReferenceAsString())
                    ),
                () -> assertThat(actualRoleAssignment.getAttributes().getJurisdiction())
                    .isEqualTo(Optional.of(expectedCaseDetails.getJurisdiction())
                    ),
                () -> assertThat(actualRoleAssignment.getAttributes().getCaseType())
                    .isEqualTo(Optional.of(expectedCaseDetails.getCaseTypeId())
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
                () -> assertThat(queryRequests).hasSize(deleteRequests.size()),
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
                () -> assertThat(queryRequests).hasSize(deleteRequests.size()),
                () -> assertCorrectlyPopulatedRoleAssignmentQueries(deleteRequests, queryRequests)
            );
        }

        private void assertCorrectlyPopulatedRoleAssignmentQueries(
            final List<RoleAssignmentsDeleteRequest> expectedDeleteRequests,
            final List<RoleAssignmentQuery> actualRoleAssignmentQueries
        ) {
            assertThat(actualRoleAssignmentQueries).isNotNull();
            assertThat(actualRoleAssignmentQueries).hasSize(expectedDeleteRequests.size());

            // create map by userID (NB: this relies on the test data using a unique user_id for each query)
            Map<String, RoleAssignmentQuery> queryMapByUser = actualRoleAssignmentQueries.stream()
                .collect(Collectors.toMap(query -> query.getActorId().get(0), query -> query));

            expectedDeleteRequests.forEach(expectedDeleteRequest -> assertAll(
                () -> assertThat(queryMapByUser).containsKey(expectedDeleteRequest.getUserId()),
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
            assertThat(actualRoleAssignmentQuery).isNotNull();
            assertAll(
                // verify format
                () -> assertThat(actualRoleAssignmentQuery.getAttributes().getCaseId()).hasSize(1),
                () -> assertThat(actualRoleAssignmentQuery.getActorId()).hasSize(1),
                () -> assertThat(actualRoleAssignmentQuery.getRoleType()).hasSize(1),
                () -> assertThat(actualRoleAssignmentQuery.getRoleName())
                    .hasSize(expectedDeleteRequest.getRoleNames().size()),

                // verify data
                () -> assertThat(actualRoleAssignmentQuery.getAttributes().getCaseId().get(0))
                    .isEqualTo(expectedDeleteRequest.getCaseId()),
                () -> assertThat(actualRoleAssignmentQuery.getActorId().get(0))
                    .isEqualTo(expectedDeleteRequest.getUserId()),
                () -> assertThat(actualRoleAssignmentQuery.getRoleType().get(0)).isEqualTo(RoleType.CASE.name()),
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
            assertThat(roleAssignments).isEqualTo(mockedRoleAssignments);
        }

    }

    @Nested
    @DisplayName("getRoleAssignmentsForCreate()")
    class GetRoleAssignmentsForCreate {

        @Test
        void shouldGetRoleAssignmentsForCreate() {

            // GIVEN
            final var expectedResult = new RoleAssignments();
            expectedResult.setRoleAssignments(new ArrayList<>());
            given(roleAssignmentRepository.getRoleAssignments(USER_ID))
                .willReturn(mockedRoleAssignmentResponse);
            given(roleAssignmentsMapper.toRoleAssignments(mockedRoleAssignmentResponse))
                .willReturn(expectedResult);

            // WHEN
            final var roleAssignments = roleAssignmentService.getRoleAssignmentsForCreate(USER_ID);

            // THEN
            assertThat(roleAssignments).isEqualTo(expectedResult);
        }

        @Test
        void shouldReturnActiveOrganisationAssignmentsWithoutCaseIdForCreate() {

            // GIVEN
            RoleAssignment expectedRoleAssignment = createOrganisationRoleAssignmentWithoutCaseId();
            RoleAssignment expectedRoleAssignmentWithEmptyCaseId = createOrganisationRoleAssignmentWithEmptyCaseId();
            givenRoleAssignmentsForCreate(expectedRoleAssignment, expectedRoleAssignmentWithEmptyCaseId);

            // WHEN
            final var roleAssignments = roleAssignmentService.getRoleAssignmentsForCreate(USER_ID);

            // THEN
            assertThat(roleAssignments.getRoleAssignments())
                .isEqualTo(List.of(expectedRoleAssignment, expectedRoleAssignmentWithEmptyCaseId));
        }

        @Test
        void shouldExcludeCaseRoleAssignmentsForCreate() {

            // GIVEN
            givenRoleAssignmentsForCreate(
                createCaseRoleAssignmentWithCaseId(),
                createCaseRoleAssignmentWithoutCaseId()
            );

            // WHEN
            final var roleAssignments = roleAssignmentService.getRoleAssignmentsForCreate(USER_ID);

            // THEN
            assertThat(roleAssignments.getRoleAssignments()).isEmpty();
        }

        @Test
        void shouldExcludeOrganisationAssignmentsWithCaseIdForCreate() {

            // GIVEN
            givenRoleAssignmentsForCreate(createOrganisationRoleAssignmentWithCaseId());

            // WHEN
            final var roleAssignments = roleAssignmentService.getRoleAssignmentsForCreate(USER_ID);

            // THEN
            assertThat(roleAssignments.getRoleAssignments()).isEmpty();
        }

        @Test
        void shouldExcludeExpiredOrganisationAssignmentsForCreate() {

            // GIVEN
            givenRoleAssignmentsForCreate(createExpiredOrganisationRoleAssignmentWithoutCaseId());

            // WHEN
            final var roleAssignments = roleAssignmentService.getRoleAssignmentsForCreate(USER_ID);

            // THEN
            assertThat(roleAssignments.getRoleAssignments()).isEmpty();
        }

        @Test
        void shouldExcludeExpiredCaseRoleAssignmentsWithoutCaseIdForCreate() {

            // GIVEN
            givenRoleAssignmentsForCreate(createExpiredCaseRoleAssignmentWithoutCaseId());

            // WHEN
            final var roleAssignments = roleAssignmentService.getRoleAssignmentsForCreate(USER_ID);

            // THEN
            assertThat(roleAssignments.getRoleAssignments()).isEmpty();
        }

        @Test
        void shouldExcludeExpiredCaseRoleAssignmentsWithCaseIdForCreate() {

            // GIVEN
            givenRoleAssignmentsForCreate(createExpiredCaseRoleAssignmentWithCaseId());

            // WHEN
            final var roleAssignments = roleAssignmentService.getRoleAssignmentsForCreate(USER_ID);

            // THEN
            assertThat(roleAssignments.getRoleAssignments()).isEmpty();
        }

        @Test
        void shouldExcludeExpiredOrganisationAssignmentsWithCaseIdForCreate() {

            // GIVEN
            givenRoleAssignmentsForCreate(createExpiredOrganisationRoleAssignmentWithCaseId());

            // WHEN
            final var roleAssignments = roleAssignmentService.getRoleAssignmentsForCreate(USER_ID);

            // THEN
            assertThat(roleAssignments.getRoleAssignments()).isEmpty();
        }

        @Test
        void shouldOnlyReturnActiveOrganisationAssignmentsWithoutCaseIdForCreate() {

            // GIVEN
            RoleAssignment expectedRoleAssignment = createOrganisationRoleAssignmentWithoutCaseId();
            givenRoleAssignmentsForCreate(
                expectedRoleAssignment,
                createCaseRoleAssignmentWithCaseId(),
                createCaseRoleAssignmentWithoutCaseId(),
                createOrganisationRoleAssignmentWithCaseId(),
                createExpiredOrganisationRoleAssignmentWithoutCaseId()
            );

            // WHEN
            final var roleAssignments = roleAssignmentService.getRoleAssignmentsForCreate(USER_ID);

            // THEN
            assertThat(roleAssignments.getRoleAssignments()).isEqualTo(List.of(expectedRoleAssignment));
        }

        private void givenRoleAssignmentsForCreate(RoleAssignment... roleAssignments) {
            RoleAssignments roleAssignmentsResponse = RoleAssignments.builder()
                .roleAssignments(Arrays.asList(roleAssignments))
                .build();
            given(roleAssignmentRepository.getRoleAssignments(USER_ID))
                .willReturn(mockedRoleAssignmentResponse);
            given(roleAssignmentsMapper.toRoleAssignments(mockedRoleAssignmentResponse))
                .willReturn(roleAssignmentsResponse);
        }

        private RoleAssignment createOrganisationRoleAssignmentWithoutCaseId() {
            return createRoleAssignment(RoleType.ORGANISATION, RoleAssignmentAttributes.builder().build(), false);
        }

        private RoleAssignment createOrganisationRoleAssignmentWithEmptyCaseId() {
            return createRoleAssignment(
                RoleType.ORGANISATION,
                RoleAssignmentAttributes.builder().caseId(Optional.empty()).build(),
                false
            );
        }

        private RoleAssignment createOrganisationRoleAssignmentWithCaseId() {
            return createRoleAssignment(
                RoleType.ORGANISATION,
                RoleAssignmentAttributes.builder().caseId(Optional.of(CASE_ID)).build(),
                false
            );
        }

        private RoleAssignment createExpiredOrganisationRoleAssignmentWithoutCaseId() {
            return createRoleAssignment(RoleType.ORGANISATION, RoleAssignmentAttributes.builder().build(), true);
        }

        private RoleAssignment createExpiredOrganisationRoleAssignmentWithCaseId() {
            return createRoleAssignment(
                RoleType.ORGANISATION,
                RoleAssignmentAttributes.builder().caseId(Optional.of(CASE_ID)).build(),
                true
            );
        }

        private RoleAssignment createCaseRoleAssignmentWithCaseId() {
            return createRoleAssignment(
                RoleType.CASE,
                RoleAssignmentAttributes.builder().caseId(Optional.of(CASE_ID)).build(),
                false
            );
        }

        private RoleAssignment createCaseRoleAssignmentWithoutCaseId() {
            return createRoleAssignment(RoleType.CASE, RoleAssignmentAttributes.builder().build(), false);
        }

        private RoleAssignment createExpiredCaseRoleAssignmentWithCaseId() {
            return createRoleAssignment(
                RoleType.CASE,
                RoleAssignmentAttributes.builder().caseId(Optional.of(CASE_ID)).build(),
                true
            );
        }

        private RoleAssignment createExpiredCaseRoleAssignmentWithoutCaseId() {
            return createRoleAssignment(RoleType.CASE, RoleAssignmentAttributes.builder().build(), true);
        }

        private RoleAssignment createRoleAssignment(RoleType roleType,
                                                    RoleAssignmentAttributes attributes,
                                                    boolean expired) {
            final Instant currentTime = Instant.now();
            final long oneHour = 3600000;
            final Instant beginTime = currentTime.minusMillis(expired ? oneHour * 2 : oneHour);
            final Instant endTime = expired ? currentTime.minusMillis(oneHour) : currentTime.plusMillis(oneHour);
            return RoleAssignment.builder()
                .actorId(USER_ID)
                .roleType(roleType.name())
                .roleName("[CREATE_ACCESS_PROFILE]")
                .attributes(attributes)
                .beginTime(beginTime)
                .endTime(endTime)
                .build();
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
            assertThat(caseAssignedUserRole).hasSize(2);
            assertThat(caseAssignedUserRole.get(0).getCaseDataId()).isEqualTo(CASE_ID);
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
            assertThat(resultCases).hasSize(1);
            assertThat(resultCases.get(0)).isEqualTo(CASE_ID);
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
            assertThat(resultCases)
                .hasSize(2)
                .containsAll(caseIds);
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
            assertThat(resultCases).hasSize(1); // single case
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
            assertThat(resultCases).hasSize(2); // multiple cases
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

            assertThat(resultCases).hasSize(2);
            roleAssignmentFilteringService.filter(roleAssignments, caseTypeDefinition);
        }

    }

    /**
     * Create test role assignments: single case with two case-roles.
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
     * Create multiple test role assignments: two cases each with two case-roles.
     */
    private RoleAssignments createTestRoleAssignmentsMultipleCases() {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        roleAssignments.addAll(createTestRoleAssignments(CASE_ID).getRoleAssignments());
        roleAssignments.addAll(createTestRoleAssignments(CASE_ID_2).getRoleAssignments());

        return RoleAssignments.builder().roleAssignments(roleAssignments).build();
    }

}
