package uk.gov.hmcts.ccd.data.definition;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CachedCaseDefinitionRepositoryTest {

    private static final String JURISDICTION_ID = "DIVORCE";
    private static final String USER_ROLE_1 = "caseworker-divorce-loa1";

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
    class GetCaseTypesForJurisdiction {

        @Test
        @DisplayName("should initially retrieve case types from decorated repository")
        void shouldRetrieveCaseTypesFromDecorated() {
            final List<CaseTypeDefinition> expectedCaseTypes =
                Lists.newArrayList(new CaseTypeDefinition(), new CaseTypeDefinition());
            doReturn(expectedCaseTypes).when(caseDefinitionRepository).getCaseTypesForJurisdiction(JURISDICTION_ID);

            final List<CaseTypeDefinition> caseTypes =
                cachedCaseDefinitionRepository.getCaseTypesForJurisdiction(JURISDICTION_ID);

            assertAll(
                () -> assertThat(caseTypes, is(expectedCaseTypes)),
                () -> verify(caseDefinitionRepository, times(1)).getCaseTypesForJurisdiction(
                    JURISDICTION_ID)
            );
        }
    }

    @Nested
    @DisplayName("getBaseTypes()")
    class GetBaseTypes {

        @Test
        @DisplayName("should initially retrieve base types from decorated repository")
        void shouldRetrieveBaseTypesFromDecorated() {
            final List<FieldTypeDefinition> expectedBaseTypes =
                newArrayList(new FieldTypeDefinition(), new FieldTypeDefinition());
            doReturn(expectedBaseTypes).when(caseDefinitionRepository).getBaseTypes();

            final List<FieldTypeDefinition> baseTypes = cachedCaseDefinitionRepository.getBaseTypes();

            assertAll(
                () -> assertThat(baseTypes, is(expectedBaseTypes)),
                () -> verify(caseDefinitionRepository, times(1)).getBaseTypes()
            );
        }
    }

    @Nested
    @DisplayName("getAllCaseTypesIDs()")
    class GetAllCaseTypesIDs {

        @Test
        @DisplayName("should retrieve all Case types IDs from decorated repository")
        void shouldRetrieveAllCaseTypesIDs() {
            final List<String> expectedBaseTypes = newArrayList("caseTypeId1", "caseTypeId2");
            doReturn(expectedBaseTypes).when(caseDefinitionRepository).getAllCaseTypesIDs();

            final List<String> caseTypesIDs = cachedCaseDefinitionRepository.getAllCaseTypesIDs();

            assertAll(
                () -> assertThat(caseTypesIDs, is(expectedBaseTypes))
            );
        }
    }

    @Nested
    @DisplayName("getCaseTypesIDsByJurisdictions()")
    class GetCaseTypesIDsByJurisdictions {

        @Test
        @DisplayName("should retrieve all Case types IDs by Jurisdictions.")
        void shouldRetrieveCaseTypesIDsByJurisdictions() {
            final List<String> expectedBaseTypes = newArrayList("caseTypeId1", "caseTypeId2");
            final List<String> jurisdictions = newArrayList("jurisdiction1", "jurisdiction2");
            doReturn(expectedBaseTypes).when(caseDefinitionRepository).getCaseTypesIDsByJurisdictions(jurisdictions);

            final List<String> caseTypesIDs =
                cachedCaseDefinitionRepository.getCaseTypesIDsByJurisdictions(jurisdictions);

            assertAll(
                () -> assertThat(caseTypesIDs, is(expectedBaseTypes))
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
                () -> verify(caseDefinitionRepository, times(1)).getUserRoleClassifications(
                    USER_ROLE_1)
            );
        }
    }
}
