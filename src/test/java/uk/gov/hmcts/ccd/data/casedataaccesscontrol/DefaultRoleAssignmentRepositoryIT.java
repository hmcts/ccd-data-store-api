package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.ActorIdType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.Classification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleType;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.ETAG;
import static org.springframework.http.HttpHeaders.IF_NONE_MATCH;

@Disabled("Temporary for pipeline issues")
@DisplayName("DefaultRoleAssignmentRepository")
class DefaultRoleAssignmentRepositoryIT extends WireMockBaseTest {

    private static final String ID = "4d96923f-891a-4cb1-863e-9bec44d1689d";
    private static final String ID1 = "4d96923f-891a-4cb1-863e-9bec44d1612d";
    private static final String ACTOR_ID_TYPE = ActorIdType.IDAM.name();
    private static final String ACTOR_ID = "567567";
    private static final String ROLE_TYPE = RoleType.ORGANISATION.name();
    private static final String ROLE_NAME = "judge";
    private static final String CLASSIFICATION = Classification.PUBLIC.name();
    private static final String GRANT_TYPE = GrantType.STANDARD.name();
    private static final String ROLE_CATEGORY = RoleCategory.JUDICIAL.name();
    private static final Boolean READ_ONLY = Boolean.FALSE;
    private static final String BEGIN_TIME = "2021-01-01T00:00:00.000Z";
    private static final String END_TIME = "2223-01-01T00:00:00.000Z";
    private static final String CREATED = "2020-12-23T06:37:58.000196065Z";
    private static final Instant EXPECTED_BEGIN_TIME = Instant.parse(BEGIN_TIME);
    private static final Instant EXPECTED_END_TIME = Instant.parse(END_TIME);
    private static final Instant EXPECTED_CREATED = Instant.parse(CREATED);
    private static final String ATTRIBUTES_CONTRACT_TYPE = "SALARIED";
    private static final String ATTRIBUTES_JURISDICTION = "divorce";
    private static final String ATTRIBUTES_CASE_ID = "1504259907353529";
    private static final String ATTRIBUTES_REGION = "south-east";
    private static final String ATTRIBUTES_LOCATION = "south-east-cornwall";
    private static final String AUTHORISATIONS_AUTH_1 = "auth1";
    private static final String AUTHORISATIONS_AUTH_2 = "auth2";
    private static final String POST_CODE = "EC12 3LN";
    @SuppressWarnings("checkstyle:LineLength") // don't want to break error messages and add unwanted +
    private static final String HTTP_400_ERROR_MESSAGE = "Client error when getting Role Assignments from Role Assignment Service because of ";
    @SuppressWarnings("checkstyle:LineLength") // don't want to break error messages and add unwanted +
    private static final String HTTP_500_ERROR_MESSAGE = "Problem getting Role Assignments from Role Assignment Service because of ";

    @Nested
    @DisplayName("createRoleAssignment()")
    class CreateRoleAssignment {

        private StubMapping badRasStub;
        private static final String CREATE_URL = "/am/role-assignments";

        @BeforeEach
        void setUp() {
            badRasStub = null;
        }

        @AfterEach
        void tearDown() {
            if (badRasStub != null) {
                WireMock.removeStub(badRasStub);
            }
        }

        @DisplayName("should return roleAssignments after successful Create call")
        @Test
        void shouldReturnRoleAssignmentsAfterCreateCall() {

            // GIVEN
            RoleAssignmentRequestResource assignmentRequest = createAssignmentRequest(Set.of("[ROLE1]", "[ROLE2]"));

            // WHEN
            RoleAssignmentRequestResponse response = roleAssignmentRepository.createRoleAssignment(assignmentRequest);

            // THEN
            assertNotNull(response);
            // verify response can deserialize at least the items supplied (and a select few others)
            assertNotNull(response.getRoleAssignmentResponse());
            // :: verify header
            assertNotNull(response.getRoleAssignmentResponse().getRoleRequest());
            RoleRequestResource roleRequestHeader = response.getRoleAssignmentResponse().getRoleRequest();
            RoleRequestResource suppliedRoleRequestHeader = assignmentRequest.getRoleRequest();
            assertAll(
                () -> assertThat(roleRequestHeader.getAssignerId(), is(suppliedRoleRequestHeader.getAssignerId())),
                () -> assertThat(roleRequestHeader.getProcess(), is(suppliedRoleRequestHeader.getProcess())),
                () -> assertThat(roleRequestHeader.getReference(), is(suppliedRoleRequestHeader.getReference())),
                // additional fields populated from RAS
                () -> assertNotNull(roleRequestHeader.getRequestType()),
                () -> assertNotNull(roleRequestHeader.getStatus()),
                () -> assertNotNull(roleRequestHeader.getCreated())
            );
            // :: verify roles
            assertThat(
                response.getRoleAssignmentResponse().getRequestedRoles().size(),
                is(assignmentRequest.getRequestedRoles().size())
            );

            Map<String, RoleAssignmentResource> roleMap = response.getRoleAssignmentResponse().getRequestedRoles()
                .stream().collect(Collectors.toMap(RoleAssignmentResource::getRoleName, role -> role));

            assignmentRequest.getRequestedRoles().forEach(suppliedRequestedRole -> {
                String roleName = suppliedRequestedRole.getRoleName();
                assertAll(
                    () -> assertTrue(roleMap.containsKey(roleName)),
                    () -> validateRoleAssignment(roleMap.get(roleName), suppliedRequestedRole)
                );
            });
        }

        @DisplayName("should throw BadRequestException when POST roleAssignments returns a client error")
        @Test
        void shouldErrorBadRequestWhenCreateRoleAssignmentReturnsClientError() {

            // GIVEN
            RoleAssignmentRequestResource assignmentRequest = createAssignmentRequest(Set.of("[ROLE1]"));
            badRasStub = WireMock.stubFor(WireMock.post(urlMatching(CREATE_URL)).willReturn(badRequest()));

            // WHEN / THEN
            final BadRequestException exception = assertThrows(BadRequestException.class,
                () -> roleAssignmentRepository.createRoleAssignment(assignmentRequest));

            assertThat(exception.getMessage(),
                startsWith(
                    String.format(DefaultRoleAssignmentRepository.ROLE_ASSIGNMENTS_CLIENT_ERROR, "creating", "")
                )
            );
        }

        @DisplayName("should throw ServiceException when POST roleAssignments returns a server error")
        @Test
        void shouldErrorServiceExceptionWhenCreateRoleAssignmentReturnsServerError() {

            // GIVEN
            RoleAssignmentRequestResource assignmentRequest = createAssignmentRequest(Set.of("[ROLE1]"));
            badRasStub = WireMock.stubFor(WireMock.post(urlMatching(CREATE_URL)).willReturn(serverError()));

            // WHEN / THEN
            final ServiceException exception = assertThrows(ServiceException.class,
                () -> roleAssignmentRepository.createRoleAssignment(assignmentRequest));

            assertThat(exception.getMessage(),
                startsWith(
                    String.format(DefaultRoleAssignmentRepository.ROLE_ASSIGNMENT_SERVICE_ERROR, "creating", "")
                )
            );
        }

        private RoleAssignmentRequestResource createAssignmentRequest(final Set<String> roles) {

            RoleRequestResource roleRequest = RoleRequestResource.builder()
                .assignerId(ACTOR_ID)
                .process(RoleAssignmentRepository.DEFAULT_PROCESS)
                .reference(ATTRIBUTES_CASE_ID + "-" + ACTOR_ID)
                .replaceExisting(false)
                .build();

            List<RoleAssignmentResource> requestedRoles = roles.stream()
                .map(roleName -> RoleAssignmentResource.builder()
                    .actorIdType(ActorIdType.IDAM.name())
                    .actorId(ACTOR_ID)
                    .roleType(RoleType.CASE.name())
                    .roleName(roleName)
                    .classification(Classification.RESTRICTED.name())
                    .grantType(GrantType.SPECIFIC.name())
                    .roleCategory(RoleCategory.PROFESSIONAL.name())
                    .readOnly(false)
                    .beginTime(Instant.now())
                    .attributes(RoleAssignmentAttributesResource.builder()
                        .jurisdiction(Optional.of(ATTRIBUTES_JURISDICTION))
                        .caseType(Optional.of(ATTRIBUTES_CASE_ID))
                        .caseId(Optional.of(ATTRIBUTES_CASE_ID))
                        .build())
                    .build())
                .collect(Collectors.toList());

            return RoleAssignmentRequestResource.builder()
                .roleRequest(roleRequest)
                .requestedRoles(requestedRoles)
                .build();
        }

        private void validateRoleAssignment(RoleAssignmentResource actualRole,
                                            RoleAssignmentResource submittedRole) {
            assertAll(
                () -> assertThat(actualRole.getActorIdType(), is(submittedRole.getActorIdType())),
                () -> assertThat(actualRole.getActorId(), is(submittedRole.getActorId())),
                () -> assertThat(actualRole.getRoleType(), is(submittedRole.getRoleType())),
                () -> assertThat(actualRole.getRoleName(), is(submittedRole.getRoleName())),
                () -> assertThat(actualRole.getClassification(), is(submittedRole.getClassification())),
                () -> assertThat(actualRole.getGrantType(), is(submittedRole.getGrantType())),
                () -> assertThat(actualRole.getRoleCategory(), is(submittedRole.getRoleCategory())),
                () -> assertThat(actualRole.getBeginTime(), is(submittedRole.getBeginTime())),
                // attributes
                () -> assertThat(actualRole.getAttributes().getCaseId(), is(submittedRole.getAttributes().getCaseId())),
                () -> assertThat(
                    actualRole.getAttributes().getJurisdiction(), is(submittedRole.getAttributes().getJurisdiction())
                ),
                () -> assertThat(
                    actualRole.getAttributes().getCaseType(), is(submittedRole.getAttributes().getCaseType())
                ),
                // additional fields populated from RAS
                () -> assertNotNull(actualRole.getId()),
                () -> assertNotNull(actualRole.getCreated())
            );
        }

    }


    @Nested
    @DisplayName("deleteRoleAssignmentsByQuery()")
    class DeleteRoleAssignmentsByQuery {

        private static final String DELETE_URL = "/am/role-assignments/query/delete";

        private static final String CASE_ID_1 = "11111";
        private static final String CASE_ID_2 = "22222";

        private static final String USER_ID_1 = "12345";
        private static final String USER_ID_2 = "23456";

        private static final String ROLE_1 = "[ROLE1]";
        private static final String ROLE_2 = "[ROLE2]";

        @BeforeEach
        void setUp() {
            WireMock.resetAllRequests();
        }

        @DisplayName("should make a call to the delete role assignment by query end point for single case role")
        @Test
        void shouldMakeCallToDeleteByQueryApiForSingleCaseRole() {
            WireMock.stubFor(WireMock.post(urlMatching(DELETE_URL)).willReturn(ok()));

            // GIVEN
            List<RoleAssignmentQuery> queryRequests = List.of(
                new RoleAssignmentQuery(CASE_ID_1, USER_ID_1, List.of(ROLE_1))
            );

            // WHEN
            roleAssignmentRepository.deleteRoleAssignmentsByQuery(queryRequests);

            // THEN
            verify(1, postRequestedFor(urlMatching(DELETE_URL))
                .withRequestBody(matchingJsonPath("$.queryRequests[0].attributes.caseId[0]", equalTo(CASE_ID_1)))
                .withRequestBody(matchingJsonPath("$.queryRequests[0].actorId[0]", equalTo(USER_ID_1)))
                .withRequestBody(matchingJsonPath("$.queryRequests[0].roleType[0]", equalTo(RoleType.CASE.name())))
                .withRequestBody(matchingJsonPath("$.queryRequests[0].roleName[0]", equalTo(ROLE_1))));
        }

        @DisplayName("should make a call to the delete role assignment by query end point for multiple case roles")
        @Test
        void shouldMakeCallToDeleteByQueryApiForMultipleCaseRoles() {
            WireMock.stubFor(WireMock.post(urlMatching(DELETE_URL)).willReturn(ok()));

            // GIVEN
            List<RoleAssignmentQuery> queryRequests = List.of(
                new RoleAssignmentQuery(CASE_ID_1, USER_ID_1, List.of(ROLE_1)),
                new RoleAssignmentQuery(CASE_ID_2, USER_ID_2, List.of(ROLE_1, ROLE_2))
            );

            // WHEN
            roleAssignmentRepository.deleteRoleAssignmentsByQuery(queryRequests);

            // THEN
            verify(1, postRequestedFor(urlMatching(DELETE_URL))
                .withRequestBody(matchingJsonPath("$.queryRequests[0].attributes.caseId[0]", equalTo(CASE_ID_1)))
                .withRequestBody(matchingJsonPath("$.queryRequests[0].actorId[0]", equalTo(USER_ID_1)))
                .withRequestBody(matchingJsonPath("$.queryRequests[0].roleType[0]", equalTo(RoleType.CASE.name())))
                .withRequestBody(matchingJsonPath("$.queryRequests[0].roleName[0]", equalTo(ROLE_1)))

                .withRequestBody(matchingJsonPath("$.queryRequests[1].attributes.caseId[0]", equalTo(CASE_ID_2)))
                .withRequestBody(matchingJsonPath("$.queryRequests[1].actorId[0]", equalTo(USER_ID_2)))
                .withRequestBody(matchingJsonPath("$.queryRequests[1].roleType[0]", equalTo(RoleType.CASE.name())))
                .withRequestBody(matchingJsonPath("$.queryRequests[1].roleName[0]", equalTo(ROLE_1)))
                .withRequestBody(matchingJsonPath("$.queryRequests[1].roleName[1]", equalTo(ROLE_2))));
        }

        @DisplayName("should throw BadRequestException when POST RoleAssignments Query Delete returns a client error")
        @Test
        void shouldErrorBadRequestWhenDeleteRoleAssignmentsByQueryReturnsClientError() {

            // GIVEN
            List<RoleAssignmentQuery> queryRequests = List.of(
                new RoleAssignmentQuery(CASE_ID_1, USER_ID_1, List.of(ROLE_1))
            );

            WireMock.stubFor(WireMock.post(urlMatching(DELETE_URL)).willReturn(badRequest()));

            // WHEN / THEN
            final BadRequestException exception = assertThrows(BadRequestException.class,
                () -> roleAssignmentRepository.deleteRoleAssignmentsByQuery(queryRequests));

            assertThat(exception.getMessage(),
                startsWith(
                    String.format(DefaultRoleAssignmentRepository.ROLE_ASSIGNMENTS_CLIENT_ERROR, "deleting", "")
                )
            );
        }

        @DisplayName("should throw ServiceException when POST RoleAssignments Query Delete returns a server error")
        @Test
        void shouldErrorServiceExceptionWhenDeleteRoleAssignmentsByQueryReturnsServerError() {

            // GIVEN
            List<RoleAssignmentQuery> queryRequests = List.of(
                new RoleAssignmentQuery(CASE_ID_2, USER_ID_2, List.of(ROLE_2))
            );

            WireMock.stubFor(WireMock.post(urlMatching(DELETE_URL)).willReturn(serverError()));

            // WHEN / THEN
            final ServiceException exception = assertThrows(ServiceException.class,
                () -> roleAssignmentRepository.deleteRoleAssignmentsByQuery(queryRequests));

            assertThat(exception.getMessage(),
                startsWith(
                    String.format(DefaultRoleAssignmentRepository.ROLE_ASSIGNMENT_SERVICE_ERROR, "deleting", "")
                )
            );
        }
    }


    @Nested
    @DisplayName("getRoleAssignments()")
    class GetRoleAssignments {

        @DisplayName("should return roleAssignments")
        @Test
        void shouldReturnRoleAssignments() {
            stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID))
                .willReturn(okJson(jsonBody(ID))));

            validateRoleAssignments(ID);
        }

        @DisplayName("should error on 404 when GET roleAssignments")
        @Test
        void shouldErrorOn404WhenGetRoleAssignments() {
            stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID)).willReturn(notFound()));

            final ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class, () -> roleAssignmentRepository.getRoleAssignments(ACTOR_ID));

            assertThat(exception.getMessage(),
                startsWith("No Role Assignments found for userId="
                    + ACTOR_ID + " when getting from Role Assignment Service because of"));
        }

        @DisplayName("should GET roleAssignments from cache when ETag found")
        @Test
        void shouldUseETagToGetRoleAssignmentsFromCache() {
            // store the response and ETag in the cache
            stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID)).inScenario("ETag")
                .whenScenarioStateIs(STARTED)
                .willReturn(okJson(jsonBody(ID))
                    .withHeader(ETAG, "\"W/123456789\"")
                )
                .willSetStateTo("Cache populated with RoleAssignments"));

            stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID)).inScenario("ETag")
                .whenScenarioStateIs("Cache populated with RoleAssignments")
                .withHeader(IF_NONE_MATCH, equalTo("\"W/123456789\""))
                .willReturn(aResponse()
                    .withStatus(304)
                    .withHeader(ETAG, "\"W/123456789\"")));

            validateRoleAssignments(ID);
            validateRoleAssignments(ID);
        }

        @DisplayName("should update the cache when ETag differs from the one from the response")
        @Test
        void shouldUpdateCacheWhenETagDiffersFromTheOneFromTheResponse() {
            // store the response and ETag in the cache
            stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID)).inScenario("ETag1")
                .whenScenarioStateIs(STARTED)
                .willReturn(okJson(jsonBody(ID))
                    .withHeader(ETAG, "\"W/553456789\"")
                )
                .willSetStateTo("Cache populated with RoleAssignments"));

            // data has changed on the server and the response contains a new ETag and body
            stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID)).inScenario("ETag1")
                .whenScenarioStateIs("Cache populated with RoleAssignments")
                .withHeader(IF_NONE_MATCH, equalTo("\"W/553456789\""))
                .willReturn(okJson(jsonBody(ID1))
                    .withHeader(ETAG, "\"W/663456789\"")
                )
                .willSetStateTo("Cache updated with RoleAssignments"));

            stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID)).inScenario("ETag1")
                .whenScenarioStateIs("Cache updated with RoleAssignments")
                .withHeader(IF_NONE_MATCH, equalTo("\"W/663456789\""))
                .willReturn(aResponse().withStatus(304)));

            validateRoleAssignments(ID);
            validateRoleAssignments(ID1);
            validateRoleAssignments(ID1);
        }

        @DisplayName("should not populate cache when we receive empty roleAssignments")
        @Test
        void shouldNotPopulateCacheWhenRoleAssignmentsArrayIsEmpty() {
            // empty array of RoleAssignments should not be stored in the cache
            stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID)).inScenario("ETag2")
                .whenScenarioStateIs(STARTED)
                .willReturn(okJson(jsonBodyWithNoRoleAssignments())
                    .withHeader(ETAG, "\"W/123456789\"")
                )
                .willSetStateTo("Cache not populated with RoleAssignments"));

            stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID)).inScenario("ETag2")
                .whenScenarioStateIs("Cache not populated with RoleAssignments")
                .willReturn(okJson(jsonBodyWithNoRoleAssignments())
                    .withHeader(ETAG, "\"W/123456789\"")
                ));

            RoleAssignmentResponse roleAssignments = roleAssignmentRepository.getRoleAssignments(ACTOR_ID);
            assertThat(roleAssignments.getRoleAssignments().size(), is(0));

            RoleAssignmentResponse roleAssignments1 = roleAssignmentRepository.getRoleAssignments(ACTOR_ID);
            assertThat(roleAssignments1.getRoleAssignments().size(), is(0));
        }

        @DisplayName("should error on 400 when GET roleAssignments")
        @Test
        void shouldErrorOn400WhenGetRoleAssignments() {
            stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID)).willReturn(badRequest()));

            final BadRequestException exception = assertThrows(BadRequestException.class,
                () -> roleAssignmentRepository.getRoleAssignments(ACTOR_ID));

            assertThat(exception.getMessage(),
                startsWith("Client error when getting Role Assignments from Role Assignment Service because of "));
        }

        @DisplayName("should error on 500 when GET roleAssignments")
        @Test
        void shouldErrorOn500WhenGetRoleAssignments() {
            stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID)).willReturn(serverError()));

            final ServiceException exception = assertThrows(ServiceException.class,
                () -> roleAssignmentRepository.getRoleAssignments(ACTOR_ID));

            assertThat(exception.getMessage(),
                startsWith("Problem getting Role Assignments from Role Assignment Service because of "));
        }

        @DisplayName("should return roleAssignments")
        @Test
        void shouldReturnRoleAssignmentsWhenUnknownFieldsOnRequest() {
            stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + ACTOR_ID))
                .willReturn(okJson(jsonBodyUnknownFields(ID))));

            validateRoleAssignments(ID);
        }

        private void validateRoleAssignments(String id) {
            RoleAssignmentResponse roleAssignments = roleAssignmentRepository.getRoleAssignments(ACTOR_ID);

            assertThat(roleAssignments.getRoleAssignments().size(), is(1));
            RoleAssignmentResource roleAssignmentResource = roleAssignments.getRoleAssignments().get(0);
            assertThat(roleAssignmentResource.getId(), is(id));
            assertThat(roleAssignmentResource.getActorIdType(), is(ACTOR_ID_TYPE));
            assertThat(roleAssignmentResource.getActorId(), is(ACTOR_ID));
            assertThat(roleAssignmentResource.getRoleType(), is(ROLE_TYPE));
            assertThat(roleAssignmentResource.getRoleName(), is(ROLE_NAME));
            assertThat(roleAssignmentResource.getClassification(), is(CLASSIFICATION));
            assertThat(roleAssignmentResource.getGrantType(), is(GRANT_TYPE));
            assertThat(roleAssignmentResource.getRoleCategory(), is(ROLE_CATEGORY));
            assertThat(roleAssignmentResource.getReadOnly(), is(READ_ONLY));
            assertThat(roleAssignmentResource.getBeginTime(), is(EXPECTED_BEGIN_TIME));
            assertThat(roleAssignmentResource.getEndTime(), is(EXPECTED_END_TIME));
            assertThat(roleAssignmentResource.getCreated(), is(EXPECTED_CREATED));

            assertThat(roleAssignmentResource.getAttributes().getContractType().get(), is(ATTRIBUTES_CONTRACT_TYPE));
            assertThat(roleAssignmentResource.getAttributes().getJurisdiction().get(), is(ATTRIBUTES_JURISDICTION));
            assertThat(roleAssignmentResource.getAttributes().getCaseId().get(), is(ATTRIBUTES_CASE_ID));
            assertThat(roleAssignmentResource.getAttributes().getLocation().get(), is(ATTRIBUTES_LOCATION));
            assertThat(roleAssignmentResource.getAttributes().getRegion().get(), is(ATTRIBUTES_REGION));

            assertThat(roleAssignmentResource.getAuthorisations().size(), is(2));
            assertThat(roleAssignmentResource.getAuthorisations().get(0), is(AUTHORISATIONS_AUTH_1));
            assertThat(roleAssignmentResource.getAuthorisations().get(1), is(AUTHORISATIONS_AUTH_2));
        }

    }


    @Nested
    @DisplayName("findRoleAssignmentsByCasesAndUsers()")
    class FindRoleAssignmentsByCasesAndUsers {

        private final List<String> caseIds = Arrays.asList("111", "222");
        private final List<String> userIds = Arrays.asList("111", "222");

        @DisplayName("should return roleAssignments by user and roles")
        @Test
        void shouldReturnRoleAssignmentsByUserAndRoles() {
            stubFor(WireMock.post(urlMatching("/am/role-assignments/query")).willReturn(okJson(jsonBody(ID))));
            validateRAForFindRoleAssignmentsByCasesAndUsers();
        }

        @DisplayName("should error on 404 when post FindRoleAssignmentsByCasesAndUsers")
        @Test
        void shouldErrorOn404WhenPostFindRoleAssignmentsByCasesAndUsers() {
            final String errorMessage = "No Role Assignments found for userIds=" + userIds + " and casesIds=" + userIds;
            stubFor(WireMock.post(urlMatching("/am/role-assignments/query")).willReturn(notFound()));

            final ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                roleAssignmentRepository.findRoleAssignmentsByCasesAndUsers(caseIds, userIds)
            );

            assertThat(exception.getMessage(), startsWith(errorMessage));
        }

        @DisplayName("should error on 500 when post FindRoleAssignmentsByCasesAndUsers")
        @Test
        void shouldErrorOn500WhenPostFindRoleAssignmentsByCasesAndUsers() {

            stubFor(WireMock.post(urlMatching("/am/role-assignments/query")).willReturn(serverError()));

            final ServiceException exception = assertThrows(ServiceException.class, () ->
                roleAssignmentRepository.findRoleAssignmentsByCasesAndUsers(caseIds, userIds)
            );

            assertThat(exception.getMessage(),
                startsWith(HTTP_500_ERROR_MESSAGE));
        }

        @DisplayName("should error on 400 when post FindRoleAssignmentsByCasesAndUsers")
        @Test
        void shouldErrorOn400WhenPostFindRoleAssignmentsByCasesAndUsers() {
            stubFor(WireMock.post(urlMatching("/am/role-assignments/query")).willReturn(badRequest()));

            final BadRequestException exception = assertThrows(BadRequestException.class,
                () -> roleAssignmentRepository.findRoleAssignmentsByCasesAndUsers(caseIds, userIds)
            );
            assertThat(exception.getMessage(), startsWith(HTTP_400_ERROR_MESSAGE));
        }


        private void validateRAForFindRoleAssignmentsByCasesAndUsers() {
            final RoleAssignmentResponse roleAssignments =
                roleAssignmentRepository.findRoleAssignmentsByCasesAndUsers(caseIds, userIds);

            assertThat(roleAssignments.getRoleAssignments().size(), is(1));
            RoleAssignmentResource roleAssignmentResource = roleAssignments.getRoleAssignments().get(0);
            assertThat(roleAssignmentResource.getId(), is(ID));
            assertThat(roleAssignmentResource.getActorIdType(), is(ACTOR_ID_TYPE));
            assertThat(roleAssignmentResource.getActorId(), is(ACTOR_ID));
            assertThat(roleAssignmentResource.getRoleType(), is(ROLE_TYPE));
            assertThat(roleAssignmentResource.getRoleName(), is(ROLE_NAME));
            assertThat(roleAssignmentResource.getClassification(), is(CLASSIFICATION));
            assertThat(roleAssignmentResource.getGrantType(), is(GRANT_TYPE));
            assertThat(roleAssignmentResource.getRoleCategory(), is(ROLE_CATEGORY));
            assertThat(roleAssignmentResource.getReadOnly(), is(READ_ONLY));
            assertThat(roleAssignmentResource.getBeginTime(), is(EXPECTED_BEGIN_TIME));
            assertThat(roleAssignmentResource.getEndTime(), is(EXPECTED_END_TIME));
            assertThat(roleAssignmentResource.getCreated(), is(EXPECTED_CREATED));

            assertThat(roleAssignmentResource.getAttributes().getContractType().get(), is(ATTRIBUTES_CONTRACT_TYPE));
            assertThat(roleAssignmentResource.getAttributes().getJurisdiction().get(), is(ATTRIBUTES_JURISDICTION));
            assertThat(roleAssignmentResource.getAttributes().getCaseId().get(), is(ATTRIBUTES_CASE_ID));
            assertThat(roleAssignmentResource.getAttributes().getLocation().get(), is(ATTRIBUTES_LOCATION));
            assertThat(roleAssignmentResource.getAttributes().getRegion().get(), is(ATTRIBUTES_REGION));

            assertThat(roleAssignmentResource.getAuthorisations().size(), is(2));
            assertThat(roleAssignmentResource.getAuthorisations().get(0), is(AUTHORISATIONS_AUTH_1));
            assertThat(roleAssignmentResource.getAuthorisations().get(1), is(AUTHORISATIONS_AUTH_2));
        }
    }


    private static String jsonBody(String id) {
        return "{\n"
            + "  \"roleAssignmentResponse\": [\n"
            + "    {\n"
            + "      \"id\": \"" + id + "\",\n"
            + "      \"actorIdType\": \"" + ACTOR_ID_TYPE + "\",\n"
            + "      \"actorId\": \"" + ACTOR_ID + "\",\n"
            + "      \"roleType\": \"" + ROLE_TYPE + "\",\n"
            + "      \"roleName\": \"" + ROLE_NAME + "\",\n"
            + "      \"classification\": \"" + CLASSIFICATION + "\",\n"
            + "      \"grantType\": \"" + GRANT_TYPE + "\",\n"
            + "      \"roleCategory\": \"" + ROLE_CATEGORY + "\",\n"
            + "      \"readOnly\": " + READ_ONLY + ",\n"
            + "      \"beginTime\": \"" + BEGIN_TIME + "\",\n"
            + "      \"endTime\": \"" + END_TIME + "\",\n"
            + "      \"created\": \"" + CREATED + "\",\n"
            + "      \"attributes\": {\n"
            + "        \"contractType\": \"" + ATTRIBUTES_CONTRACT_TYPE + "\",\n"
            + "        \"jurisdiction\": \"" + ATTRIBUTES_JURISDICTION + "\",\n"
            + "        \"caseId\": \"" + ATTRIBUTES_CASE_ID + "\",\n"
            + "        \"location\": \"" + ATTRIBUTES_LOCATION + "\",\n"
            + "        \"region\": \"" + ATTRIBUTES_REGION + "\"\n"
            + "      },\n"
            + "      \"authorisations\": [\"" + AUTHORISATIONS_AUTH_1 + "\", \"" + AUTHORISATIONS_AUTH_2 + "\"]\n"
            + "    }\n"
            + "  ]\n"
            + "}";
    }

    private static String jsonBodyWithNoRoleAssignments() {
        return "{\n"
            + "  \"roleAssignmentResponse\": []\n"
            + "}";
    }

    private static String jsonBodyUnknownFields(String id) {
        return "{\n"
            + "  \"roleAssignmentResponse\": [\n"
            + "    {\n"
            + "      \"id\": \"" + id + "\",\n"
            + "      \"actorIdType\": \"" + ACTOR_ID_TYPE + "\",\n"
            + "      \"actorId\": \"" + ACTOR_ID + "\",\n"
            + "      \"roleType\": \"" + ROLE_TYPE + "\",\n"
            + "      \"roleName\": \"" + ROLE_NAME + "\",\n"
            + "      \"classification\": \"" + CLASSIFICATION + "\",\n"
            + "      \"grantType\": \"" + GRANT_TYPE + "\",\n"
            + "      \"roleCategory\": \"" + ROLE_CATEGORY + "\",\n"
            + "      \"readOnly\": " + READ_ONLY + ",\n"
            + "      \"beginTime\": \"" + BEGIN_TIME + "\",\n"
            + "      \"endTime\": \"" + END_TIME + "\",\n"
            + "      \"created\": \"" + CREATED + "\",\n"
            + "      \"fieldA\": \"" + CREATED + "\",\n"
            + "      \"attributes\": {\n"
            + "        \"contractType\": \"" + ATTRIBUTES_CONTRACT_TYPE + "\",\n"
            + "        \"jurisdiction\": \"" + ATTRIBUTES_JURISDICTION + "\",\n"
            + "        \"caseId\": \"" + ATTRIBUTES_CASE_ID + "\",\n"
            + "        \"location\": \"" + ATTRIBUTES_LOCATION + "\",\n"
            + "        \"region\": \"" + ATTRIBUTES_REGION + "\",\n"
            + "        \"postCode\": \"" + POST_CODE + "\"\n"
            + "      },\n"
            + "      \"authorisations\": [\"" + AUTHORISATIONS_AUTH_1 + "\", \"" + AUTHORISATIONS_AUTH_2 + "\"]\n"
            + "    }\n"
            + "  ]\n"
            + "}";
    }

}

