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

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

class CachedCaseDefinitionRepositoryTest {

    private static final String JURISDICTION_ID = "DIVORCE";
    private static final String CASE_TYPE_ID = "APPLICATIONC100";
    private static final String USER_ROLE = "caseworker-divorce-loa1";
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
        void shouldRetrieveCaseTypesFromDecorated () {
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
        void shouldCacheCaseTypesForSubsequentCalls () {
            final List<CaseType> expectedCaseTypes = Lists.newArrayList(new CaseType(), new CaseType());
            doReturn(expectedCaseTypes).when(caseDefinitionRepository).getCaseTypesForJurisdiction(JURISDICTION_ID);

            cachedCaseDefinitionRepository.getCaseTypesForJurisdiction(JURISDICTION_ID);

            verify(caseDefinitionRepository, times(1)).getCaseTypesForJurisdiction(JURISDICTION_ID);

            doReturn(Lists.newArrayList(new CaseType(), new CaseType())).when(caseDefinitionRepository).getCaseTypesForJurisdiction(JURISDICTION_ID);

            final List<CaseType> caseTypes = cachedCaseDefinitionRepository.getCaseTypesForJurisdiction(JURISDICTION_ID);

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
        void shouldRetrieveBaseTypesFromDecorated () {
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
        void shouldCacheBaseTypesForSubsequentCalls () {
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
    class getUserRoleClassifications {

        @Test
        @DisplayName("should initially retrieve user role from decorated repository")
        void shouldRetrieveUserRoleFromDecorated () {
            final UserRole expectedUserRole = new UserRole();
            doReturn(expectedUserRole).when(caseDefinitionRepository).getUserRoleClassifications(USER_ROLE);

            final UserRole userRole = cachedCaseDefinitionRepository.getUserRoleClassifications(USER_ROLE);

            assertAll(
                () -> assertThat(userRole, is(expectedUserRole)),
                () -> verify(caseDefinitionRepository, times(1)).getUserRoleClassifications(USER_ROLE)
            );
        }

        @Test
        @DisplayName("should cache user role for subsequent calls")
        void shouldCacheUserRoleForSubsequentCalls () {
            final UserRole expectedUserRole = new UserRole();
            doReturn(expectedUserRole).when(caseDefinitionRepository).getUserRoleClassifications(USER_ROLE);

            cachedCaseDefinitionRepository.getUserRoleClassifications(USER_ROLE);

            verify(caseDefinitionRepository, times(1)).getUserRoleClassifications(USER_ROLE);

            doReturn(new UserRole()).when(caseDefinitionRepository).getUserRoleClassifications(USER_ROLE);

            final UserRole userRole = cachedCaseDefinitionRepository.getUserRoleClassifications(USER_ROLE);

            assertAll(
                () -> assertThat(userRole, is(expectedUserRole)),
                () -> verifyNoMoreInteractions(caseDefinitionRepository)
            );
        }
    }

}
