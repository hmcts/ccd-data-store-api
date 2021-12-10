package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CachedRoleAssignmentRepositoryTest {

    private static final String USER_ID = "user1";
    private static final String CASE_ID = "111111";

    @Mock
    private RoleAssignmentRepository roleAssignmentRepositoryMock;

    @Mock
    private RoleAssignmentResponse roleAssignmentResponse;

    @InjectMocks
    private CachedRoleAssignmentRepository classUnderTest;

    @Test
    @DisplayName("should create role assignments in decorated repository")
    void shouldCreateRoleAssignmentInDefaultRepository() {

        // GIVEN
        RoleAssignmentRequestResource assignmentRequest = RoleAssignmentRequestResource.builder().build();
        RoleAssignmentRequestResponse expectedResponse = RoleAssignmentRequestResponse.builder().build();

        doReturn(expectedResponse)
            .when(roleAssignmentRepositoryMock).createRoleAssignment(assignmentRequest);

        // WHEN
        RoleAssignmentRequestResponse roleAssignmentRequestResponse
            = classUnderTest.createRoleAssignment(assignmentRequest);

        // THEN
        assertAll(
            () -> assertThat(expectedResponse, is(roleAssignmentRequestResponse)),
            () -> verify(roleAssignmentRepositoryMock).createRoleAssignment(assignmentRequest)
        );
    }

    @Test
    @DisplayName("should delete role assignments by query in decorated repository")
    void shouldDeleteRoleAssignmentByQueryInDefaultRepository() {

        // GIVEN
        List<RoleAssignmentQuery> queryRequests = List.of(
            new RoleAssignmentQuery(CASE_ID, USER_ID, List.of(GlobalCaseRole.CREATOR.getRole()))
        );

        // WHEN
        classUnderTest.deleteRoleAssignmentsByQuery(queryRequests);

        // THEN
        verify(roleAssignmentRepositoryMock).deleteRoleAssignmentsByQuery(queryRequests);
    }

    @Test
    @DisplayName("should initially retrieve role assignments from decorated repository")
    void shouldGetRoleAssignmentsFromDefaultRepository() {

        // GIVEN
        doReturn(roleAssignmentResponse).when(roleAssignmentRepositoryMock).getRoleAssignments(USER_ID);

        // WHEN 1
        RoleAssignmentResponse roleAssignments = classUnderTest.getRoleAssignments(USER_ID);

        // THEN 1
        assertAll(
            () -> assertThat(roleAssignments, is(roleAssignmentResponse)),
            () -> verify(roleAssignmentRepositoryMock).getRoleAssignments(USER_ID)
        );

        // WHEN 2
        RoleAssignmentResponse roleAssignments2 = classUnderTest.getRoleAssignments(USER_ID);

        // THEN 2
        assertAll(
            () -> assertThat(roleAssignments2, is(roleAssignmentResponse)),
            () -> verifyNoMoreInteractions(roleAssignmentRepositoryMock)
        );
    }

}
