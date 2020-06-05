package uk.gov.hmcts.ccd.data.caseaccess;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyList;
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
    void shouldGetUserCaseRolesFromDefaultRepository() {
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

    @Test
    @DisplayName("should initially retrieve case ids user has access")
    void shouldGetCaseUserRolesFromDefaultRepository() {
        List<Long> caseIds = new ArrayList<>();
        caseIds.add(1234L);
        caseIds.add(1235L);

        List<String> userIds = new ArrayList<>();
        userIds.add("123456");
        userIds.add("123457");
        List<CaseUserEntity> caseUserEntities = new ArrayList<>();
        caseUserEntities.add(new CaseUserEntity(1234L, "123456", "[CREATOR]"));
        caseUserEntities.add(new CaseUserEntity(1235L, "123457", "[SOLICITOR]"));
        doReturn(caseUserEntities).when(caseUserRepository).findCaseUserRoles(anyList(), anyList());

        List<CaseUserEntity> returned = classUnderTest.findCaseUserRoles(caseIds, userIds);

        assertAll(
            () -> assertThat(returned, is(caseUserEntities)),
            () -> verify(caseUserRepository, times(1)).findCaseUserRoles(caseIds, userIds)
        );
        List<CaseUserEntity> returned2 = classUnderTest.findCaseUserRoles(caseIds, userIds);

        assertAll(
            () -> assertThat(returned2, is(caseUserEntities)),
            () -> verify(caseUserRepository, times(2)).findCaseUserRoles(caseIds, userIds)
        );
    }
}
