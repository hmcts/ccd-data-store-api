package uk.gov.hmcts.ccd.data.definition;

import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;

import java.util.List;

public interface CaseDefinitionRepository {
    List<CaseTypeDefinition> getCaseTypesForJurisdiction(String jurisdictionId);

    /**
     * Retrieves a case type definition for the specified case type ID and returns a cloned copy.
     * This method first checks the in-memory cache for the case type definition. If the definition is not cached,
     * it retrieves it from the case definition repository, caches it, and returns a cloned copy.
     * Unlike the scoped cache approach, this method ensures that a cloned copy is returned every time, even within
     * the same request, to guarantee immutability and thread-safety.
     * Use this method when immutability is crucial and the object may be used in various contexts.
     *
     * @param caseTypeId The ID of the case type to retrieve.
     * @return A cloned copy of the cached case type definition.
     */
    CaseTypeDefinition getCaseType(String caseTypeId);

    CaseTypeDefinition getCaseType(int version, String caseTypeId);

    List<FieldTypeDefinition> getBaseTypes();

    UserRole getUserRoleClassifications(String userRole);

    List<UserRole> getClassificationsForUserRoleList(List<String> userRoles);

    CaseTypeDefinitionVersion getLatestVersion(String caseTypeId);

    JurisdictionDefinition getJurisdiction(String jurisdictionId);

    List<String> getCaseTypesIDsByJurisdictions(List<String> jurisdictionIds);

    List<JurisdictionDefinition> getAllJurisdictionsFromDefinitionStore();

    List<String> getAllCaseTypesIDs();

    /**
     * Retrieves a cached case type definition for the specified case type ID using a two-level caching mechanism and
     * returns a cloned copy.
     * The first level of caching is an in-memory cache that checks if the case type definition is already available
     * before attempting to retrieve it from an external source (case definition repository). If the definition is not
     * cached, it fetches the definition from the repository, stores it in the in-memory cache, and returns a cloned
     * copy.
     * The second level of caching is a scope-specific cache implemented using a HashMap, defined as a Spring component
     * with RequestScope. This ensures that within the same request, the method can return the same object without
     * cloning, improving performance by avoiding redundant cloning operations.
     * Use this method when the returned object is circulated in a read-only transaction to enhance performance.
     *
     * @param caseTypeId The ID of the case type to retrieve.
     * @return A cloned copy of the cached case type definition.
     */
    CaseTypeDefinition getScopedCachedCaseType(String caseTypeId);
}
