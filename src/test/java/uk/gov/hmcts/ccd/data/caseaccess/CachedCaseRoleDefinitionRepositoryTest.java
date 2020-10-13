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

    private final String caseType1 = "CASETYPE1";
    private final Set<String> caseRoles = Sets.newHashSet("cr1", "cr2", "cr3");

    private CachedCaseRoleRepository classUnderTest;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(caseRoles).when(caseRoleRepository).getCaseRoles(caseType1);
        classUnderTest = new CachedCaseRoleRepository(caseRoleRepository);
    }

    @Test
    @DisplayName("should initially retrieve case roles from decorated repository")
    void shoudlGetCaseRolesFromDefaultRepository() {
        Set<String> returned = classUnderTest.getCaseRoles(caseType1);

        assertAll(
            () -> assertThat(returned, is(caseRoles)),
            () -> verify(caseRoleRepository, times(1)).getCaseRoles(caseType1)
        );
    }

    @Test
    @DisplayName("should initially retrieve case roles from decorated repository")
    void shouldGetCaseRolesFromCache() {
        Set<String> returned = classUnderTest.getCaseRoles(caseType1);

        assertAll(
            () -> assertThat(returned, is(caseRoles)),
            () -> verify(caseRoleRepository, times(1)).getCaseRoles(caseType1)
        );
        Set<String> returned2 = classUnderTest.getCaseRoles(caseType1);

        assertAll(
            () -> assertThat(returned2, is(caseRoles)),
            () -> verifyNoMoreInteractions(caseRoleRepository)
        );
    }
}
