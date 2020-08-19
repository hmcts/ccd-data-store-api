package uk.gov.hmcts.ccd.v2.external.controller;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.service.cauroles.CaseAssignedUserRolesOperation;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseRoleAccessException;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.domain.AddCaseAssignedUserRolesResponse;
import uk.gov.hmcts.ccd.v2.external.resource.CaseAssignedUserRolesResource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static uk.gov.hmcts.ccd.v2.external.controller.CaseAssignedUserRolesController.ADD_SUCCESS_MESSAGE;

class CaseAssignedUserRolesControllerTest {

    private static final String CASE_ID_GOOD = "4444333322221111";
    private static final String CASE_ID_BAD = "1234";

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private UIDService caseReferenceService;

    @Mock
    private CaseAssignedUserRolesOperation caseAssignedUserRolesOperation;

    @Mock
    private SecurityUtils securityUtils;

    private CaseAssignedUserRolesController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(caseReferenceService.validateUID(CASE_ID_GOOD)).thenReturn(true);
        when(caseReferenceService.validateUID(CASE_ID_BAD)).thenReturn(false);

        controller = new CaseAssignedUserRolesController(
            applicationParams,
            caseReferenceService,
            caseAssignedUserRolesOperation,
            securityUtils
        );
    }

    @Nested
    @DisplayName("POST /case-users")
    class AddCaseUserRoles {

        private static final String ADD_SERVICE_GOOD = "ADD_SERVICE_GOOD";
        private static final String ADD_SERVICE_BAD = "ADD_SERVICE_BAD";

        private static final String CLIENT_S2S_TOKEN_GOOD = "good_s2s_token";
        private static final String CLIENT_S2S_TOKEN_BAD = "bad_s2s_token";

        private static final String CASE_ROLE_GOOD = "[CASE_ROLE_GOOD]";
        private static final String CASE_ROLE_BAD = "CASE_ROLE_BAD";
        private static final String ORGANISATION_ID_GOOD = "ORGANISATION_ID_GOOD";
        private static final String ORGANISATION_ID_BAD = "";
        private static final String USER_ID_1 = "123";
        private static final String USER_ID_2 = "321";

        @BeforeEach
        void setUp() {
            // setup happy authorised s2s service path
            when(applicationParams.getAuthorisedServicesForAddUserCaseRoles()).thenReturn(
                Lists.newArrayList(ADD_SERVICE_GOOD)
            );
            doReturn(ADD_SERVICE_GOOD).when(securityUtils).getServiceNameFromS2SToken(CLIENT_S2S_TOKEN_GOOD);
        }

        @Test
        void addCaseUserRoles_shouldCallAddWhenValidSingleGoodCaseUserRoleSupplied() {
            // ARRANGE
            List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
            CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_GOOD, USER_ID_1, CASE_ROLE_GOOD);
            caseUserRoles.add(caseUserRole1);

            CaseAssignedUserRolesResource caseAssignedUserRoles = new CaseAssignedUserRolesResource(caseUserRoles);

            // ACT
            ResponseEntity<AddCaseAssignedUserRolesResponse> response =
                controller.addCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, caseAssignedUserRoles);

            // ASSERT
            assertNotNull(response);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ADD_SUCCESS_MESSAGE, response.getBody().getStatus());
            verify(caseAssignedUserRolesOperation, times(1)).addCaseUserRoles(caseUserRoles);
        }

        @Test
        void addCaseUserRoles_shouldCallAddWhenValidSingleGoodCaseUserRoleSupplied_withOrganisation() {
            // ARRANGE
            List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
            CaseAssignedUserRole caseUserRole1 =
                new CaseAssignedUserRole(CASE_ID_GOOD, USER_ID_1, CASE_ROLE_GOOD, ORGANISATION_ID_GOOD);
            caseUserRoles.add(caseUserRole1);

            CaseAssignedUserRolesResource caseAssignedUserRoles = new CaseAssignedUserRolesResource(caseUserRoles);

            // ACT
            ResponseEntity<AddCaseAssignedUserRolesResponse> response =
                controller.addCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, caseAssignedUserRoles);

            // ASSERT
            assertNotNull(response);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ADD_SUCCESS_MESSAGE, response.getBody().getStatus());
            verify(caseAssignedUserRolesOperation, times(1)).addCaseUserRoles(caseUserRoles);
        }

        @Test
        void addCaseUserRoles_shouldCallAddWhenValidMultipleGoodCaseUserRolesSupplied() {
            // ARRANGE
            List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
            CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_GOOD, USER_ID_1, CASE_ROLE_GOOD);
            CaseAssignedUserRole caseUserRole2 = new CaseAssignedUserRole(CASE_ID_GOOD, USER_ID_2, CASE_ROLE_GOOD);
            CaseAssignedUserRole caseUserRole3 =
                new CaseAssignedUserRole(CASE_ID_GOOD, USER_ID_2, CASE_ROLE_GOOD, ORGANISATION_ID_GOOD);
            caseUserRoles.add(caseUserRole1);
            caseUserRoles.add(caseUserRole2);
            caseUserRoles.add(caseUserRole3);

            CaseAssignedUserRolesResource caseAssignedUserRoles = new CaseAssignedUserRolesResource(caseUserRoles);

            // ACT
            ResponseEntity<AddCaseAssignedUserRolesResponse> response =
                controller.addCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, caseAssignedUserRoles);

            // ASSERT
            assertNotNull(response);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ADD_SUCCESS_MESSAGE, response.getBody().getStatus());
            verify(caseAssignedUserRolesOperation, times(1)).addCaseUserRoles(caseUserRoles);
        }

        @Test
        void addCaseUserRoles_throwsExceptionWhenClientServiceNotAuthorised() {
            // ARRANGE
            doReturn(ADD_SERVICE_BAD).when(securityUtils).getServiceNameFromS2SToken(CLIENT_S2S_TOKEN_BAD);

            // ACT / ASSERT
            CaseRoleAccessException exception = assertThrows(CaseRoleAccessException.class,
                () -> controller.addCaseUserRoles(CLIENT_S2S_TOKEN_BAD, null));

            assertAll(
                () -> assertThat(exception.getMessage(),
                    containsString(V2.Error.CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION))
            );
        }

        @Test
        void addCaseUserRoles_throwsExceptionWhenNullPassed() {
            // ARRANGE

            // ACT / ASSERT
            BadRequestException exception = assertThrows(BadRequestException.class,
                () -> controller.addCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, null));

            assertAll(
                () -> assertThat(exception.getMessage(),
                    containsString(V2.Error.EMPTY_CASE_USER_ROLE_LIST))
            );
        }

        @Test
        void addCaseUserRoles_throwsExceptionWhenNullCaseUserRolesListPassed() {
            // ARRANGE
            CaseAssignedUserRolesResource caseAssignedUserRoles = new CaseAssignedUserRolesResource(null);

            // ACT / ASSERT
            BadRequestException exception = assertThrows(BadRequestException.class,
                () -> controller.addCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, caseAssignedUserRoles));

            assertAll(
                () -> assertThat(exception.getMessage(),
                    containsString(V2.Error.EMPTY_CASE_USER_ROLE_LIST))
            );
        }

        @Test
        void addCaseUserRoles_throwsExceptionWhenEmptyCaseUserRolesListPassed() {
            // ARRANGE
            List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();

            CaseAssignedUserRolesResource caseAssignedUserRoles = new CaseAssignedUserRolesResource(caseUserRoles);

            // ACT / ASSERT
            BadRequestException exception = assertThrows(BadRequestException.class,
                () -> controller.addCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, caseAssignedUserRoles));

            assertAll(
                () -> assertThat(exception.getMessage(),
                    containsString(V2.Error.EMPTY_CASE_USER_ROLE_LIST))
            );
        }

        @Test
        void addCaseUserRoles_throwsExceptionWhenInvalidCaseIdPassed() {
            // ARRANGE
            List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
            // case_id: has to be a valid 16-digit Luhn number)
            CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_BAD, USER_ID_1, CASE_ROLE_GOOD);
            caseUserRoles.add(caseUserRole1);

            CaseAssignedUserRolesResource caseAssignedUserRoles = new CaseAssignedUserRolesResource(caseUserRoles);

            // ACT / ASSERT
            BadRequestException exception = assertThrows(BadRequestException.class,
                () -> controller.addCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, caseAssignedUserRoles));

            assertAll(
                () -> assertThat(exception.getMessage(),
                    containsString(V2.Error.CASE_ID_INVALID))
            );
        }

        @Test
        void addCaseUserRoles_throwsExceptionWhenInvalidUserIdPassed() {
            // ARRANGE
            List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
            // user_id: has to be a string of length > 0
            CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_GOOD, "", CASE_ROLE_GOOD);
            caseUserRoles.add(caseUserRole1);

            CaseAssignedUserRolesResource caseAssignedUserRoles = new CaseAssignedUserRolesResource(caseUserRoles);

            // ACT / ASSERT
            BadRequestException exception = assertThrows(BadRequestException.class,
                () -> controller.addCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, caseAssignedUserRoles));

            assertAll(
                () -> assertThat(exception.getMessage(),
                    containsString(V2.Error.USER_ID_INVALID))
            );
        }

        @Test
        void addCaseUserRoles_throwsExceptionWhenInvalidCaseRolePassed() {
            // ARRANGE
            List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
            // case_role: has to be a none-empty string in square brackets
            CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_GOOD, "", CASE_ROLE_BAD);
            caseUserRoles.add(caseUserRole1);

            CaseAssignedUserRolesResource caseAssignedUserRoles = new CaseAssignedUserRolesResource(caseUserRoles);

            // ACT / ASSERT
            BadRequestException exception = assertThrows(BadRequestException.class,
                () -> controller.addCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, caseAssignedUserRoles));

            assertAll(
                () -> assertThat(exception.getMessage(),
                    containsString(V2.Error.CASE_ROLE_FORMAT_INVALID))
            );
        }

        @Test
        void addCaseUserRoles_throwsExceptionWhenInvalidOrganisationIdPassed() {
            // ARRANGE
            List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
            // organisation_id: has to be a non-empty string, when present
            CaseAssignedUserRole caseUserRole1 =
                new CaseAssignedUserRole(CASE_ID_GOOD, USER_ID_1, CASE_ROLE_GOOD, ORGANISATION_ID_BAD);
            caseUserRoles.add(caseUserRole1);

            CaseAssignedUserRolesResource caseAssignedUserRoles = new CaseAssignedUserRolesResource(caseUserRoles);

            // ACT / ASSERT
            BadRequestException exception = assertThrows(BadRequestException.class,
                () -> controller.addCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, caseAssignedUserRoles));

            assertAll(
                () -> assertThat(exception.getMessage(),
                    containsString(V2.Error.ORGANISATION_ID_INVALID))
            );
        }

        @Test
        void addCaseUserRoles_throwsExceptionWhenMultipleErrorsPassed() {
            // ARRANGE
            List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
            // case_id: has to be a valid 16-digit Luhn number)
            // case_role: has to be a none-empty string in square brackets
            CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_BAD, USER_ID_1, CASE_ROLE_BAD);
            // user_id: has to be a string of length > 0
            CaseAssignedUserRole caseUserRole2 = new CaseAssignedUserRole(CASE_ID_GOOD, "", CASE_ROLE_GOOD);
            // organisation_id: has to be a non-empty string, when present
            CaseAssignedUserRole caseUserRole3 =
                new CaseAssignedUserRole(CASE_ID_GOOD, USER_ID_1, CASE_ROLE_GOOD, ORGANISATION_ID_BAD);
            caseUserRoles.add(caseUserRole1);
            caseUserRoles.add(caseUserRole2);
            caseUserRoles.add(caseUserRole3);

            CaseAssignedUserRolesResource caseAssignedUserRoles = new CaseAssignedUserRolesResource(caseUserRoles);

            // ACT / ASSERT
            BadRequestException exception = assertThrows(BadRequestException.class,
                () -> controller.addCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, caseAssignedUserRoles));

            assertAll(
                () -> assertThat(exception.getMessage(),
                    containsString(V2.Error.CASE_ID_INVALID)),
                () -> assertThat(exception.getMessage(),
                    containsString(V2.Error.USER_ID_INVALID)),
                () -> assertThat(exception.getMessage(),
                    containsString(V2.Error.CASE_ROLE_FORMAT_INVALID)),
                () -> assertThat(exception.getMessage(),
                    containsString(V2.Error.ORGANISATION_ID_INVALID))
            );
        }

    }

    @Nested
    @DisplayName("GET /case-users")
    class GetCaseUserRoles {

        @BeforeEach
        void setUp() {
            when(caseAssignedUserRolesOperation.findCaseUserRoles(anyList(), anyList())).thenReturn(createCaseAssignedUserRoles());
        }

        private List<CaseAssignedUserRole> createCaseAssignedUserRoles() {
            List<CaseAssignedUserRole> userRoles = Lists.newArrayList();
            userRoles.add(new CaseAssignedUserRole());
            userRoles.add(new CaseAssignedUserRole());
            return userRoles;
        }

        @Test
        void getCaseUserRoles_throwsExceptionWhenNullCaseIdListPassed() {
            Optional<List<String>> optionalUserIds = Optional.of(Lists.newArrayList());

            BadRequestException exception = assertThrows(BadRequestException.class,
                () -> controller.getCaseUserRoles(null, optionalUserIds));

            assertAll(
                () -> assertThat(exception.getMessage(),
                    containsString(V2.Error.EMPTY_CASE_ID_LIST))
            );
        }

        @Test
        void getCaseUserRoles_throwsExceptionWhenEmptyCaseIdListPassed() {
            List<String> caseIds = Lists.newArrayList();
            Optional<List<String>> optionalUserIds = Optional.of(Lists.newArrayList());

            BadRequestException exception = assertThrows(BadRequestException.class,
                () -> controller.getCaseUserRoles(caseIds, optionalUserIds));

            assertAll(
                () -> assertThat(exception.getMessage(),
                    containsString(V2.Error.EMPTY_CASE_ID_LIST))
            );
        }

        @Test
        void getCaseUserRoles_throwsExceptionWhenEmptyCaseIdListContainsInvalidCaseId() {
            List<String> caseIds = Lists.newArrayList(CASE_ID_BAD);
            Optional<List<String>> optionalUserIds = Optional.of(Lists.newArrayList());

            BadRequestException exception = assertThrows(BadRequestException.class,
                () -> controller.getCaseUserRoles(caseIds, optionalUserIds));

            assertAll(
                () -> assertThat(exception.getMessage(),
                    containsString(V2.Error.CASE_ID_INVALID))
            );
        }

        @Test
        void getCaseUserRoles_throwsExceptionWhenInvalidUserIdListPassed() {
            List<String> caseIds = Lists.newArrayList(CASE_ID_GOOD);
            Optional<List<String>> optionalUserIds = Optional.of(Lists.newArrayList("8900", "", "89002"));

            BadRequestException exception = assertThrows(BadRequestException.class,
                () -> controller.getCaseUserRoles(caseIds, optionalUserIds));

            assertAll(
                () -> assertThat(exception.getMessage(),
                    containsString(V2.Error.USER_ID_INVALID))
            );
        }

        @Test
        void getCaseUserRoles_shouldGetResponseWhenCaseIdsAndUserIdsPassed() {
            when(caseReferenceService.validateUID(anyString())).thenReturn(true);
            ResponseEntity<CaseAssignedUserRolesResource> response = controller.getCaseUserRoles(
                Lists.newArrayList(CASE_ID_GOOD),
                Optional.of(Lists.newArrayList("8900", "89002")));
            assertNotNull(response);
            assertNotNull(response.getBody());
            assertEquals(2, response.getBody().getCaseAssignedUserRoles().size());
        }

        @Test
        void getCaseUserRoles_shouldGetResponseWhenCaseIdsPassed() {
            when(caseReferenceService.validateUID(anyString())).thenReturn(true);
            ResponseEntity<CaseAssignedUserRolesResource> response = controller.getCaseUserRoles(
                Lists.newArrayList(CASE_ID_GOOD),
                Optional.empty());
            assertNotNull(response);
            assertNotNull(response.getBody());
            assertEquals(2, response.getBody().getCaseAssignedUserRoles().size());
        }

    }

    @Nested
    @DisplayName("Build ID lists for LogAudit")
    class BuildIdListsForLogAudit {

        @Test
        void buildIdLists_shouldReturnEmptyStringWhenNullPassed() {
            // ACT
            String resultBuildCaseIds = CaseAssignedUserRolesController.buildCaseIds(null);
            String resultBuildUserIds = CaseAssignedUserRolesController.buildUserIds(null);
            String resultBuildCaseRoles = CaseAssignedUserRolesController.buildCaseRoles(null);

            // ASSERT
            assertEquals("", resultBuildCaseIds);
            assertEquals("", resultBuildUserIds);
            assertEquals("", resultBuildCaseRoles);
        }

        @Test
        void buildIdLists_shouldReturnEmptyStringWhenNullListPassed() {
            // ACT
            String resultBuildCaseIds =
                CaseAssignedUserRolesController.buildCaseIds(new CaseAssignedUserRolesResource(null));
            String resultBuildUserIds =
                CaseAssignedUserRolesController.buildUserIds(new CaseAssignedUserRolesResource(null));
            String resultBuildCaseRoles =
                CaseAssignedUserRolesController.buildCaseRoles(new CaseAssignedUserRolesResource(null));

            // ASSERT
            assertEquals("", resultBuildCaseIds);
            assertEquals("", resultBuildUserIds);
            assertEquals("", resultBuildCaseRoles);
        }

        @Test
        void buildIdLists_shouldReturnEmptyStringWhenEmptyListPassed() {
            // ACT
            String resultBuildCaseIds =
                CaseAssignedUserRolesController.buildCaseIds(createCaseUserRolesList(0));
            String resultBuildUserIds =
                CaseAssignedUserRolesController.buildUserIds(createCaseUserRolesList(0));
            String resultBuildCaseRoles =
                CaseAssignedUserRolesController.buildCaseRoles(createCaseUserRolesList(0));

            // ASSERT
            assertEquals("", resultBuildCaseIds);
            assertEquals("", resultBuildUserIds);
            assertEquals("", resultBuildCaseRoles);
        }

        @Test
        void buildIdLists_shouldReturnSimpleStringWhenSingleListItemPassed() {
            // ACT
            String resultBuildCaseIds =
                CaseAssignedUserRolesController.buildCaseIds(createCaseUserRolesList(1));
            String resultBuildUserIds =
                CaseAssignedUserRolesController.buildUserIds(createCaseUserRolesList(1));
            String resultBuildCaseRoles =
                CaseAssignedUserRolesController.buildCaseRoles(createCaseUserRolesList(1));

            // ASSERT
            assertEquals("1", resultBuildCaseIds); // test data is: count up
            assertEquals("1", resultBuildUserIds); // test data is: square
            assertEquals("1", resultBuildCaseRoles); // test data is: count down
        }

        @Test
        void buildIdLists_shouldReturnCsvStringWhenManyListItemsPassed() {
            // ACT
            String resultBuildCaseIds =
                CaseAssignedUserRolesController.buildCaseIds(createCaseUserRolesList(3));
            String resultBuildCaseRoles =
                CaseAssignedUserRolesController.buildCaseRoles(createCaseUserRolesList(3));
            String resultBuildUserIds =
                CaseAssignedUserRolesController.buildUserIds(createCaseUserRolesList(3));

            // ASSERT
            assertEquals("1,2,3", resultBuildCaseIds); // test data is: count up
            assertEquals("1,4,9", resultBuildUserIds); // test data is: square
            assertEquals("3,2,1", resultBuildCaseRoles); // test data is: count down
        }

        @Test
        void buildIdLists_shouldReturnMaxCsvListWhenTooManyListItemsPassed() {
            // ACT
            // NB: max list size is 10 (u.g.h.c.a.a.AuditContext.MAX_CASE_IDS_LIST)
            String resultBuildCaseIds =
                CaseAssignedUserRolesController.buildCaseIds(createCaseUserRolesList(11));
            String resultBuildCaseRoles =
                CaseAssignedUserRolesController.buildCaseRoles(createCaseUserRolesList(11));
            String resultBuildUserIds =
                CaseAssignedUserRolesController.buildUserIds(createCaseUserRolesList(11));

            // ASSERT
            assertEquals("1,2,3,4,5,6,7,8,9,10", resultBuildCaseIds); // test data is: count up
            assertEquals("1,4,9,16,25,36,49,64,81,100", resultBuildUserIds); // test data is: square
            assertEquals("11,10,9,8,7,6,5,4,3,2", resultBuildCaseRoles); // test data is: count down
        }

        @Test
        void buildOptionalIds_shouldReturnEmptyStringWhenEmptyPassed() {
            // ACT
            String resultBuildOptionalIds = CaseAssignedUserRolesController.buildOptionalIds(Optional.empty());

            // ASSERT
            assertEquals("", resultBuildOptionalIds);
        }

        @Test
        void buildOptionalIds_shouldReturnEmptyStringWhenEmptyListPassed() {
            // ACT
            String resultBuildOptionalIds =
                CaseAssignedUserRolesController.buildOptionalIds(Optional.of(new ArrayList<>()));

            // ASSERT
            assertEquals("", resultBuildOptionalIds);
        }

        @Test
        void buildOptionalIds_shouldReturnSimpleStringWhenSingleListItemPassed() {
            // ACT
            String resultBuildOptionalIds =
                CaseAssignedUserRolesController.buildOptionalIds(Optional.of(Lists.newArrayList("1")));

            // ASSERT
            assertEquals("1", resultBuildOptionalIds);
        }

        @Test
        void buildOptionalIds_shouldReturnCsvStringWhenManyListItemsPassed() {
            // ACT
            String resultBuildOptionalIds =
                CaseAssignedUserRolesController.buildOptionalIds(Optional.of(
                    Lists.newArrayList("1", "2", "3")));

            // ASSERT
            assertEquals("1,2,3", resultBuildOptionalIds);
        }

        @Test
        void buildOptionalIds_shouldReturnMaxCsvListWhenTooManyListItemsPassed() {
            // ACT
            // NB: max list size is 10 (u.g.h.c.a.a.AuditContext.MAX_CASE_IDS_LIST)
            String resultBuildOptionalIds =
                CaseAssignedUserRolesController.buildOptionalIds(Optional.of(
                    Lists.newArrayList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11")));

            // ASSERT
            assertEquals("1,2,3,4,5,6,7,8,9,10", resultBuildOptionalIds);
        }

        private CaseAssignedUserRolesResource createCaseUserRolesList(int numberRequired) {
            List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();

            for (int i = 1; i <= numberRequired; i++) {
                caseUserRoles.add(new CaseAssignedUserRole(
                    // count up, square, count down
                    Integer.toString(i),  // test data is: count up
                    Integer.toString(i * i), // test data is: square
                    Integer.toString(numberRequired - i + 1) // test data is: count down
                ));
            }

            return new CaseAssignedUserRolesResource(caseUserRoles);
        }

    }

}
