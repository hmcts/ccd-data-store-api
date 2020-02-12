package uk.gov.hmcts.ccd.data.caseaccess;

import com.google.common.collect.Lists;
import java.util.List;
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

class CachedCaseUserRepositoryTest {

    @Mock
    private CaseUserRepository caseUserRepository;

    private final Long caseId = 12345L;
    private final String userId = "USERID1";
    private final List<String> caseUserRoles = Lists.newArrayList("[CREATOR]", "[LASOLICITOR]");
    private final List<Long> caseIds = Lists.newArrayList(12345L, 12346L);

    private CachedCaseUserRepository classUnderTest;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(caseUserRoles).when(caseUserRepository).findCaseRoles(caseId, userId);
        doReturn(caseIds).when(caseUserRepository).findCasesUserIdHasAccessTo(userId);
        classUnderTest = new CachedCaseUserRepository(caseUserRepository);
    }

    @Test
    @DisplayName("should initially retrieve case user roles from decorated repository")
    void shoudlGetUserCaseRolesFromDefaultRepository() {
        List<String> returned = classUnderTest.findCaseRoles(caseId, userId);

        assertAll(
            () -> assertThat(returned, is(caseUserRoles)),
            () -> verify(caseUserRepository, times(1)).findCaseRoles(caseId, userId)
        );

        List<String> returned2 = classUnderTest.findCaseRoles(caseId, userId);

        assertAll(
            () -> assertThat(returned2, is(caseUserRoles)),
            () -> verifyNoMoreInteractions(caseUserRepository)
        );
    }

    @Test
    @DisplayName("should initially retrieve case ids user has access")
    void shouldGetCaseIdsUserHasAccess() {
        List<Long> returned = classUnderTest.findCasesUserIdHasAccessTo(userId);

        assertAll(
            () -> assertThat(returned, is(caseIds)),
            () -> verify(caseUserRepository, times(1)).findCasesUserIdHasAccessTo(userId)
        );
        List<Long> returned2 = classUnderTest.findCasesUserIdHasAccessTo(userId);

        assertAll(
            () -> assertThat(returned2, is(caseIds)),
            () -> verifyNoMoreInteractions(caseUserRepository)
        );
    }
}
