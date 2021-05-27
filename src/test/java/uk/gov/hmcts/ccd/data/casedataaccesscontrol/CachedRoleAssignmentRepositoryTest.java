package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class CachedRoleAssignmentRepositoryTest {
    private final String userId = "USERID1";

    @Mock
    private RoleAssignmentRepository roleAssignmentRepositoryMock;

    @Mock
    private RoleAssignmentResponse roleAssignmentResponse;

    private CachedRoleAssignmentRepository classUnderTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(roleAssignmentResponse).when(roleAssignmentRepositoryMock).getRoleAssignments(userId);
        classUnderTest = new CachedRoleAssignmentRepository(roleAssignmentRepositoryMock);
    }

    @Test
    @DisplayName("should initially retrieve role assignments from decorated repository")
    void shouldGetRoleAssignmentsFromDefaultRepository() {
        RoleAssignmentResponse roleAssignments = classUnderTest.getRoleAssignments(userId);

        assertAll(
            () -> assertThat(roleAssignments, is(roleAssignmentResponse)),
            () -> verify(roleAssignmentRepositoryMock, times(1)).getRoleAssignments(userId)
        );

        RoleAssignmentResponse roleAssignments2 = classUnderTest.getRoleAssignments(userId);

        assertAll(
            () -> assertThat(roleAssignments2, is(roleAssignmentResponse)),
            () -> verifyNoMoreInteractions(roleAssignmentRepositoryMock)
        );
    }
}
