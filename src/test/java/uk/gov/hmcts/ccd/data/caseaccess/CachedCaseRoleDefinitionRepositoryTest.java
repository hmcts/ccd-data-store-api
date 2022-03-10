package uk.gov.hmcts.ccd.data.caseaccess;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class CachedCaseRoleDefinitionRepositoryTest {

    @Mock
    private CaseRoleRepository caseRoleRepository;

    private final String caseType = "CASETYPE";
    private final Set<String> caseRoles = Sets.newHashSet("cr1", "cr2", "cr3");

    private CachedCaseRoleRepository classUnderTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        doReturn(caseRoles).when(caseRoleRepository).getCaseRoles(caseType);
        classUnderTest = new CachedCaseRoleRepository(caseRoleRepository);
    }

    @Test
    @DisplayName("should initially retrieve case roles from decorated repository")
    void shouldGetCaseRolesByCaseTypeFromDefaultRepository() {
        Set<String> returned = classUnderTest.getCaseRoles(caseType);

        assertAll(
            () -> assertThat(returned, is(caseRoles)),
            () -> verify(caseRoleRepository, times(1)).getCaseRoles(caseType)
        );
    }

    @Test
    @DisplayName("should retrieve case roles from cache")
    void shouldGetCaseRolesByCaseTypeFromCache() {
        Set<String> returned1 = classUnderTest.getCaseRoles(caseType);

        assertAll(
            () -> assertThat(returned1, is(caseRoles)),
            () -> verify(caseRoleRepository, times(1)).getCaseRoles(caseType)
        );

        Set<String> returned2 = classUnderTest.getCaseRoles(caseType);

        assertAll(
            () -> assertThat(returned2, is(caseRoles)),
            () -> verifyNoMoreInteractions(caseRoleRepository)
        );
    }

    @Test
    @DisplayName("should initially retrieve case roles from decorated repository")
    void shouldGetCaseRolesFromDefaultRepository() {
        Set<String> returned = classUnderTest.getCaseRoles(DefaultCaseRoleRepository.DEFAULT_USER_ID,
            DefaultCaseRoleRepository.DEFAULT_JURISDICTION_ID, caseType);

        assertAll(
            () -> assertThat(returned, is(caseRoles)),
            () -> verify(caseRoleRepository, times(1)).getCaseRoles(caseType)
        );
    }

    @Test
    @DisplayName("should retrieve case roles from cache")
    void shouldGetCaseRolesFromCache() {
        Set<String> returned1 = classUnderTest.getCaseRoles(DefaultCaseRoleRepository.DEFAULT_USER_ID,
            DefaultCaseRoleRepository.DEFAULT_JURISDICTION_ID, caseType);

        assertAll(
            () -> assertThat(returned1, is(caseRoles)),
            () -> verify(caseRoleRepository, times(1)).getCaseRoles(caseType)
        );

        Set<String> returned2 = classUnderTest.getCaseRoles(DefaultCaseRoleRepository.DEFAULT_USER_ID,
            DefaultCaseRoleRepository.DEFAULT_JURISDICTION_ID, caseType);

        assertAll(
            () -> assertThat(returned2, is(caseRoles)),
            () -> verifyNoMoreInteractions(caseRoleRepository)
        );
    }

    @Test
    @DisplayName("should initially retrieve case roles from decorated repository with access control set to true.")
    void shouldGetCaseRolesFromDefaultRepositoryWithTrueAccessControl() {
        doReturn(caseRoles).when(caseRoleRepository).getRoles(caseType);
        Set<String> returned = classUnderTest.getRoles(caseType);

        assertAll(
            () -> assertThat(returned, is(caseRoles)),
            () -> verify(caseRoleRepository, times(1)).getRoles(caseType)
        );
    }

    @Test
    @DisplayName("should initially retrieve case roles from decorated repository with access control set to true.")
    void shouldGetCaseRolesFromCacheWithTrueAccessControl() {
        doReturn(caseRoles).when(caseRoleRepository).getRoles(caseType);
        Set<String> returned = classUnderTest.getRoles(caseType);


        assertAll(
            () -> assertThat(returned, is(caseRoles)),
            () -> verify(caseRoleRepository, times(1)).getRoles(caseType)
        );
        Set<String> returned2 = classUnderTest.getRoles(caseType);

        assertAll(
            () -> assertThat(returned2, is(caseRoles)),
            () -> verifyNoMoreInteractions(caseRoleRepository)
        );
    }

}
