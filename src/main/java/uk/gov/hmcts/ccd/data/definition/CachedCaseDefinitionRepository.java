package uk.gov.hmcts.ccd.data.definition;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;

@Service
@Slf4j
@Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
@RequestScope
public class CachedCaseDefinitionRepository implements CaseDefinitionRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedCaseDefinitionRepository.class);

    public static final String QUALIFIER = "cached";
    private static final String CASE_TYPE_KEY_FORMAT = "%s___%d";
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final Map<String, List<CaseTypeDefinition>> caseTypesForJurisdictions = newHashMap();
    private final Map<String, CaseTypeDefinitionVersion> versions = newHashMap();
    private final Map<String, UserRole> userRoleClassifications = newHashMap();
    private final Map<String, List<FieldTypeDefinition>> baseTypes = newHashMap();
    private final Map<String, CaseTypeDefinition> caseTypes = newHashMap();
    private final Map<String, JurisdictionDefinition> jurisdictions = newHashMap();

    @Autowired
    public CachedCaseDefinitionRepository(@Qualifier(DefaultCaseDefinitionRepository.QUALIFIER)
                                                  CaseDefinitionRepository caseDefinitionRepository) {
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    @Override
    public List<CaseTypeDefinition> getCaseTypesForJurisdiction(final String jurisdictionId) {
        return caseTypesForJurisdictions.computeIfAbsent(jurisdictionId,
                                                         caseDefinitionRepository::getCaseTypesForJurisdiction);
    }

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
    @Override
    public CaseTypeDefinition getCaseType(final String caseTypeId) {
        CaseTypeDefinitionVersion latestVersion = this.getLatestVersion(caseTypeId);
        return this.getCaseType(latestVersion.getVersion(), caseTypeId);
    }

    @Override
    public CaseTypeDefinition getCaseType(final int version, final String caseTypeId) {
        return getClonedCaseType(version, caseTypeId);
    }

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
    @Override
    public CaseTypeDefinition getScopedCachedCaseType(final String caseTypeId) {
        CaseTypeDefinitionVersion latestVersion = this.getLatestVersion(caseTypeId);
        return getScopedCachedCaseType(caseTypeId, latestVersion);
    }

    private CaseTypeDefinition getScopedCachedCaseType(String caseTypeId, CaseTypeDefinitionVersion latestVersion) {
        return caseTypes.computeIfAbsent(format(CASE_TYPE_KEY_FORMAT, caseTypeId, latestVersion.getVersion()),
            e -> getClonedCaseType(latestVersion.getVersion(), caseTypeId));
    }

    private CaseTypeDefinition getClonedCaseType(int version, String caseTypeId) {
        CaseTypeDefinition clonedCaseType = caseDefinitionRepository.getCaseType(version, caseTypeId).createCopy();
        log.debug("Cloned case type: {}", clonedCaseType);
        return clonedCaseType;
    }

    @Override
    public UserRole getUserRoleClassifications(String userRole) {
        return userRoleClassifications.computeIfAbsent(userRole, caseDefinitionRepository::getUserRoleClassifications);
    }

    @Override
    public List<UserRole> getClassificationsForUserRoleList(List<String> userRoles) {
        List<String> missingRoles = userRoles
            .stream()
            .filter(role -> !userRoleClassifications.containsKey(role))
            .collect(Collectors.toList());

        List<UserRole> missingClassifications = missingRoles
            .stream()
            .map(caseDefinitionRepository::getUserRoleClassifications)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        missingClassifications
            .forEach(userClassification ->
                userRoleClassifications.putIfAbsent(userClassification.getRole(), userClassification));

        return userRoles.stream().map(userRoleClassifications::get)
            .filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public CaseTypeDefinitionVersion getLatestVersion(String caseTypeId) {
        return versions.computeIfAbsent(caseTypeId, caseDefinitionRepository::getLatestVersion);
    }

    @Override
    public JurisdictionDefinition getJurisdiction(String jurisdictionId) {
        LOGGER.debug("Will get jurisdiction '{}' from repository.", jurisdictionId);
        return jurisdictions.computeIfAbsent(jurisdictionId, caseDefinitionRepository::getJurisdiction);
    }

    @Override
    public List<String> getCaseTypesIDsByJurisdictions(List<String> jurisdictionIds) {
        return caseDefinitionRepository.getCaseTypesIDsByJurisdictions(jurisdictionIds);
    }

    @Override
    public List<JurisdictionDefinition> getAllJurisdictionsFromDefinitionStore() {
        return caseDefinitionRepository.getAllJurisdictionsFromDefinitionStore();
    }

    @Override
    public List<String> getAllCaseTypesIDs() {
        return caseDefinitionRepository.getAllCaseTypesIDs();
    }

    @Override
    public List<FieldTypeDefinition> getBaseTypes() {
        return baseTypes.computeIfAbsent("baseTypes", e -> caseDefinitionRepository.getBaseTypes());
    }

    public CaseDefinitionRepository getCaseDefinitionRepository() {
        return caseDefinitionRepository;
    }
}
