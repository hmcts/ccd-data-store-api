package uk.gov.hmcts.ccd.data.definition;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;

import java.util.Arrays;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.UserRoleBuilder.aUserRole;

class CachedCaseDefinitionRepositoryTest {

    private static final String JURISDICTION_ID = "DIVORCE";
    private static final String USER_ROLE_1 = "caseworker-divorce-loa1";
    private static final String USER_ROLE_2 = "caseworker-probate-loa1";
    private static final String USER_ROLE_3 = "caseworker-some-loa1";
    private static final String USER_ROLE_4 = "caseworker-other-loa1";
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    private CachedCaseDefinitionRepository cachedCaseDefinitionRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        cachedCaseDefinitionRepository = new CachedCaseDefinitionRepository(caseDefinitionRepository);
    }

    @Nested
    @DisplayName("getCaseTypesForJurisdiction()")
    class getCaseTypesForJurisdiction {

        @Test
        @DisplayName("should initially retrieve case types from decorated repository")
        void shouldRetrieveCaseTypesFromDecorated() {
            final List<CaseType> expectedCaseTypes = Lists.newArrayList(new CaseType(), new CaseType());
            doReturn(expectedCaseTypes).when(caseDefinitionRepository).getCaseTypesForJurisdiction(JURISDICTION_ID);

            final List<CaseType> caseTypes = cachedCaseDefinitionRepository.getCaseTypesForJurisdiction(JURISDICTION_ID);

            assertAll(
                () -> assertThat(caseTypes, is(expectedCaseTypes)),
                () -> verify(caseDefinitionRepository, times(1)).getCaseTypesForJurisdiction(JURISDICTION_ID)
            );
        }

        @Test
        @DisplayName("should cache case types for subsequent calls")
        void shouldCacheCaseTypesForSubsequentCalls() {
            final List<CaseType> expectedCaseTypes = Lists.newArrayList(new CaseType(), new CaseType());
            doReturn(expectedCaseTypes).when(caseDefinitionRepository).getCaseTypesForJurisdiction(JURISDICTION_ID);

            cachedCaseDefinitionRepository.getCaseTypesForJurisdiction(JURISDICTION_ID);

            verify(caseDefinitionRepository, times(1)).getCaseTypesForJurisdiction(JURISDICTION_ID);

            doReturn(newArrayList(new CaseType(), new CaseType())).when(caseDefinitionRepository).getCaseTypesForJurisdiction(JURISDICTION_ID);

            final List<CaseType> caseTypes = cachedCaseDefinitionRepository.getCaseTypesForJurisdiction
                (JURISDICTION_ID);

            assertAll(
                () -> assertThat(caseTypes, is(expectedCaseTypes)),
                () -> verifyNoMoreInteractions(caseDefinitionRepository)
            );
        }
    }

    @Nested
    @DisplayName("getBaseTypes()")
    class getBaseTypes {

        @Test
        @DisplayName("should initially retrieve base types from decorated repository")
        void shouldRetrieveBaseTypesFromDecorated() {
            final List<FieldType> expectedBaseTypes = newArrayList(new FieldType(), new FieldType());
            doReturn(expectedBaseTypes).when(caseDefinitionRepository).getBaseTypes();

            final List<FieldType> baseTypes = cachedCaseDefinitionRepository.getBaseTypes();

            assertAll(
                () -> assertThat(baseTypes, is(expectedBaseTypes)),
                () -> verify(caseDefinitionRepository, times(1)).getBaseTypes()
            );
        }

        @Test
        @DisplayName("should cache base types for subsequent calls")
        void shouldCacheBaseTypesForSubsequentCalls() {
            final List<FieldType> expectedBaseTypes = newArrayList(new FieldType(), new FieldType());
            doReturn(expectedBaseTypes).when(caseDefinitionRepository).getBaseTypes();

            cachedCaseDefinitionRepository.getBaseTypes();

            verify(caseDefinitionRepository, times(1)).getBaseTypes();

            doReturn(newArrayList(new FieldType(), new FieldType())).when(caseDefinitionRepository).getBaseTypes();

            final List<FieldType> baseTypes = cachedCaseDefinitionRepository.getBaseTypes();

            assertAll(
                () -> assertThat(baseTypes, is(expectedBaseTypes)),
                () -> verifyNoMoreInteractions(caseDefinitionRepository)
            );
        }
    }

    @Nested
    @DisplayName("getUserRoleClassifications()")
    class GetUserRoleClassifications {

        @Test
        @DisplayName("should initially retrieve user role from decorated repository")
        void shouldRetrieveUserRoleFromDecorated() {
            final UserRole expectedUserRole = new UserRole();
            doReturn(expectedUserRole).when(caseDefinitionRepository).getUserRoleClassifications(USER_ROLE_1);

            final UserRole userRole = cachedCaseDefinitionRepository.getUserRoleClassifications(USER_ROLE_1);

            assertAll(
                () -> assertThat(userRole, is(expectedUserRole)),
                () -> verify(caseDefinitionRepository, times(1)).getUserRoleClassifications(USER_ROLE_1)
            );
        }

        @Test
        @DisplayName("should cache user role for subsequent calls")
        void shouldCacheUserRoleForSubsequentCalls() {
            final UserRole expectedUserRole = aUserRole().withRole(USER_ROLE_1).build();
            doReturn(expectedUserRole).when(caseDefinitionRepository).getUserRoleClassifications(USER_ROLE_1);

            cachedCaseDefinitionRepository.getUserRoleClassifications(USER_ROLE_1);

            verify(caseDefinitionRepository, times(1)).getUserRoleClassifications(USER_ROLE_1);

            doReturn(new UserRole()).when(caseDefinitionRepository).getUserRoleClassifications(USER_ROLE_1);

            final UserRole userRole = cachedCaseDefinitionRepository.getUserRoleClassifications(USER_ROLE_1);

            assertAll(
                () -> assertThat(userRole, is(expectedUserRole)),
                () -> verifyNoMoreInteractions(caseDefinitionRepository)
            );
        }
    }

    @Nested
    @DisplayName("getUserRolesClassifications()")
    class GetUserRolesClassifications {

        @Test
        @DisplayName("should initially retrieve user role from decorated repository")
        void shouldRetrieveUserRoleFromDecorated() {
            final List<UserRole> expectedUserRolesList = Arrays.asList(aUserRole().withRole(USER_ROLE_1).build(),
                aUserRole().withRole(USER_ROLE_2).build());
            final List<String> userRoles = Arrays.asList(USER_ROLE_1, USER_ROLE_2);
            doReturn(expectedUserRolesList).when(caseDefinitionRepository).getClassificationsForUserRoleList(userRoles);

            final List<UserRole> userRolesList = cachedCaseDefinitionRepository.getClassificationsForUserRoleList(userRoles);

            assertAll(
                () -> assertThat(userRolesList, is(expectedUserRolesList)),
                () -> verify(caseDefinitionRepository, times(1)).getClassificationsForUserRoleList(userRoles)
            );
        }

        @Test
        @DisplayName("should cache user role for subsequent calls")
        void shouldCacheUserRoleForSubsequentCalls() {
            final List<UserRole> expectedUserRolesList = Arrays.asList(aUserRole().withRole(USER_ROLE_1).build(),
                aUserRole().withRole(USER_ROLE_2).build());
            final List<String> userRoles = Arrays.asList(USER_ROLE_1, USER_ROLE_2);
            doReturn(expectedUserRolesList).when(caseDefinitionRepository).getClassificationsForUserRoleList(userRoles);

            cachedCaseDefinitionRepository.getClassificationsForUserRoleList(userRoles);

            verify(caseDefinitionRepository, times(1)).getClassificationsForUserRoleList(userRoles);

            final List<UserRole> someOtherUserRolesList = Arrays.asList(aUserRole().withRole(USER_ROLE_3).build(),
                aUserRole().withRole(USER_ROLE_4).build());
            doReturn(someOtherUserRolesList).when(caseDefinitionRepository).getClassificationsForUserRoleList(userRoles);

            final List<UserRole> userRolesList = cachedCaseDefinitionRepository.getClassificationsForUserRoleList
                (userRoles);

            assertAll(
                () -> assertThat(userRolesList, is(expectedUserRolesList)),
                () -> verifyNoMoreInteractions(caseDefinitionRepository)
            );
        }
    }
}
