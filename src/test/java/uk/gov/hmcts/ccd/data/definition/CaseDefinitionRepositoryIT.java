package uk.gov.hmcts.ccd.data.definition;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;

import java.util.Arrays;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Matchers.anyString;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.UserRoleBuilder.aUserRole;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWireMock(port = 0)
@TestPropertySource(locations = "classpath:test.properties")
@Ignore
public class CaseDefinitionRepositoryIT {

    @MockBean
    private DefaultCaseDefinitionRepository caseDefinitionRepository;

    @Autowired
    private CachedCaseDefinitionRepository cachedCaseDefinitionRepository;

    private static final String JURISDICTION_ID = "DIVORCE";
    private static final String USER_ROLE_1 = "caseworker-divorce-loa1";
    private static final String USER_ROLE_2 = "caseworker-probate-loa1";
    private static final String USER_ROLE_3 = "caseworker-scss-loa1";

    private final List<CaseTypeDefinition> expectedCaseTypes =
            newArrayList(new CaseTypeDefinition(), new CaseTypeDefinition());
    private final List<FieldTypeDefinition> expectedBaseTypes =
            newArrayList(new FieldTypeDefinition(), new FieldTypeDefinition());
    private final UserRole expectedUserRole1 = aUserRole().withRole(USER_ROLE_1).build();
    private final UserRole expectedUserRole2 = aUserRole().withRole(USER_ROLE_2).build();
    private final UserRole expectedUserRole3 = aUserRole().withRole(USER_ROLE_3).build();

    @Before
    public void setUp() {
        doReturn(expectedCaseTypes).when(caseDefinitionRepository).getCaseTypesForJurisdiction(JURISDICTION_ID);
        doReturn(expectedBaseTypes).when(caseDefinitionRepository).getBaseTypes();
        doReturn(expectedUserRole1).when(caseDefinitionRepository).getUserRoleClassifications(USER_ROLE_1);
        doReturn(expectedUserRole2).when(caseDefinitionRepository).getUserRoleClassifications(USER_ROLE_2);
        doReturn(expectedUserRole3).when(caseDefinitionRepository).getUserRoleClassifications(USER_ROLE_3);
    }

    @Test
    public void shouldCacheCaseTypesForSubsequentCalls() {
        final List<CaseTypeDefinition> caseTypes =
                cachedCaseDefinitionRepository.getCaseTypesForJurisdiction(JURISDICTION_ID);

        assertAll(
            () -> assertThat(caseTypes, is(expectedCaseTypes)),
            () -> verify(caseDefinitionRepository, times(1)).getCaseTypesForJurisdiction(
                JURISDICTION_ID)
        );

        cachedCaseDefinitionRepository.getCaseTypesForJurisdiction(JURISDICTION_ID);
        verifyNoMoreInteractions(caseDefinitionRepository);
    }

    @Test
    public void shouldCacheBaseTypesForSubsequentCalls() {
        final List<FieldTypeDefinition> actualBaseTypes = cachedCaseDefinitionRepository.getBaseTypes();

        assertAll(
            () -> assertThat(actualBaseTypes, is(expectedBaseTypes)),
            () -> verify(caseDefinitionRepository, times(1)).getBaseTypes()
        );

        cachedCaseDefinitionRepository.getBaseTypes();
        verifyNoMoreInteractions(caseDefinitionRepository);
    }

    @Test
    public void shouldCacheUserRoleForSubsequentCalls() {
        final UserRole actualUserRole = cachedCaseDefinitionRepository.getUserRoleClassifications(USER_ROLE_1);

        assertAll(
            () -> assertThat(actualUserRole, is(expectedUserRole1)),
            () -> verify(caseDefinitionRepository, times(1))
                .getUserRoleClassifications(USER_ROLE_1)
        );

        final UserRole userRole = cachedCaseDefinitionRepository.getUserRoleClassifications(USER_ROLE_1);

        assertAll(
            () -> assertThat(userRole.toString(), is(expectedUserRole1.toString())),
            () -> verifyNoMoreInteractions(caseDefinitionRepository)
        );
    }

    @Test
    public void shouldCacheUserRolesForSubsequentCalls() {
        final List<UserRole> userRolesList = cachedCaseDefinitionRepository
            .getClassificationsForUserRoleList(Arrays.asList(USER_ROLE_2, USER_ROLE_1));

        assertAll(
            () -> assertEquals(countUserRoles(userRolesList), 2L),
            () -> verify(caseDefinitionRepository, times(2))
                .getUserRoleClassifications(anyString())
        );

        cachedCaseDefinitionRepository.getClassificationsForUserRoleList(Arrays.asList(USER_ROLE_1, USER_ROLE_2));

        assertAll(
            () -> assertEquals(countUserRoles(userRolesList), 2L),
            () -> verifyNoMoreInteractions(caseDefinitionRepository)
        );

        cachedCaseDefinitionRepository.getClassificationsForUserRoleList(Arrays.asList(USER_ROLE_3, USER_ROLE_1));

        assertAll(
            () -> assertEquals(countUserRoles(userRolesList), 2L),
            () -> verify(caseDefinitionRepository, times(3))
                .getUserRoleClassifications(anyString())
        );
    }

    private long countUserRoles(final List<UserRole> userRolesList) {
        return userRolesList.stream().filter(userRole -> userRole.getRole().equals(USER_ROLE_1)
                || userRole.getRole().equals(USER_ROLE_2) || userRole.getRole().equals(USER_ROLE_3)).count();
    }
}
