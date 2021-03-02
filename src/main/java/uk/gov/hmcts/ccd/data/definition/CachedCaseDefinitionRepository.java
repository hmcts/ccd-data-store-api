package uk.gov.hmcts.ccd.data.definition;

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

@Service
@Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
@RequestScope
// TODO: Make this repository return copies of the maps https://tools.hmcts.net/jira/browse/RDM-1459
public class CachedCaseDefinitionRepository implements CaseDefinitionRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedCaseDefinitionRepository.class);

    public static final String QUALIFIER = "cached";

    private final CaseDefinitionRepository caseDefinitionRepository;
    private final Map<String, List<CaseTypeDefinition>> caseTypesForJurisdictions = newHashMap();
    private final Map<String, CaseTypeDefinitionVersion> versions = newHashMap();
    private final Map<String, UserRole> userRoleClassifications = newHashMap();
    private final Map<String, List<FieldTypeDefinition>> baseTypes = newHashMap();

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

    @Override
    public CaseTypeDefinition getCaseType(final String caseTypeId) {
        CaseTypeDefinitionVersion latestVersion = this.getLatestVersion(caseTypeId);
        return caseDefinitionRepository.getCaseType(latestVersion.getVersion(), caseTypeId);
    }

    @Override
    public CaseTypeDefinition getCaseType(int version, String caseTypeId) {
        return caseDefinitionRepository.getCaseType(version, caseTypeId);
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

        return userRoles.stream().map(userRoleClassifications::get).collect(Collectors.toList());
    }

    @Override
    public CaseTypeDefinitionVersion getLatestVersion(String caseTypeId) {
        return versions.computeIfAbsent(caseTypeId, caseDefinitionRepository::getLatestVersion);
    }

    @Override
    public JurisdictionDefinition getJurisdiction(String jurisdictionId) {
        LOGGER.debug("Will get jurisdiction '{}' from repository.", jurisdictionId);
        return caseDefinitionRepository.getJurisdiction(jurisdictionId);
    }

    @Override
    public List<String> getCaseTypesIDsByJurisdictions(List<String> jurisdictionIds) {
        return caseDefinitionRepository.getCaseTypesIDsByJurisdictions(jurisdictionIds);
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
