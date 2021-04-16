package uk.gov.hmcts.ccd.data.caseaccess;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import uk.gov.hmcts.ccd.WireMockBaseTest;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class CaseUserRepositoryIT extends WireMockBaseTest {

    @SpyBean
    private DefaultCaseUserRepository caseUserRepository;

    @Autowired
    private CachedCaseUserRepository cachedCaseUserRepository;

    private final Long caseId = 12345L;
    private final String userId = "USERID1";
    private final List<String> caseUserRoles = Lists.newArrayList("[CREATOR]", "[LASOLICITOR]");
    private final List<Long> caseIds = Lists.newArrayList(12345L, 12346L);

    @Before
    public void setUp() {
        doReturn(caseUserRoles).when(caseUserRepository).findCaseRoles(caseId, userId);
        doReturn(caseIds).when(caseUserRepository).findCasesUserIdHasAccessTo(userId);
    }

    @Test
    public void shouldGetUserCaseRolesFromDefaultRepository() {
        List<String> returned = cachedCaseUserRepository.findCaseRoles(caseId, userId);

        assertAll(
            () -> assertThat(returned, is(caseUserRoles)),
            () -> verify(caseUserRepository, times(1)).findCaseRoles(caseId, userId)
        );

        List<String> returned2 = cachedCaseUserRepository.findCaseRoles(caseId, userId);

        assertAll(
            () -> assertThat(returned2, is(caseUserRoles)),
            () -> verifyNoMoreInteractions(caseUserRepository)
        );
    }

    @Test
    public void shouldGetCaseIdsUserHasAccess() {
        List<Long> returned = cachedCaseUserRepository.findCasesUserIdHasAccessTo(userId);

        assertAll(
            () -> assertThat(returned, is(caseIds)),
            () -> verify(caseUserRepository, times(1)).findCasesUserIdHasAccessTo(userId)
        );

        List<Long> returned2 = cachedCaseUserRepository.findCasesUserIdHasAccessTo(userId);

        assertAll(
            () -> assertThat(returned2, is(caseIds)),
            () -> verifyNoMoreInteractions(caseUserRepository)
        );
    }
}
