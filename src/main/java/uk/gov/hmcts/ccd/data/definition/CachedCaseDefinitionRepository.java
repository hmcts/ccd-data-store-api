package uk.gov.hmcts.ccd.data.definition;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.definition.Jurisdiction;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.collect.Maps.newHashMap;

@Service
@Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
@RequestScope
// TODO: Make this repository return copies of the maps https://tools.hmcts.net/jira/browse/RDM-1459
public class CachedCaseDefinitionRepository implements CaseDefinitionRepository {

    public static final String QUALIFIER = "cached";

    private final CaseDefinitionRepository caseDefinitionRepository;
    private final Map<String, List<CaseType>> caseTypesForJurisdictions = newHashMap();
    private final Map<String , CaseTypeDefinitionVersion> versions = newHashMap();
    private final Map<String, UserRole> userRoleClassifications = newHashMap();
    private final Map<String, List<FieldType>> baseTypes = newHashMap();

    @Autowired
    public CachedCaseDefinitionRepository(@Qualifier(DefaultCaseDefinitionRepository.QUALIFIER) CaseDefinitionRepository caseDefinitionRepository) {
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    public List<CaseType> getCaseTypesForJurisdiction(final String jurisdictionId) {
        return caseTypesForJurisdictions.computeIfAbsent(jurisdictionId, caseDefinitionRepository::getCaseTypesForJurisdiction);
    }

    public CaseType getCaseType(final String caseTypeId) {
        CaseTypeDefinitionVersion latestVersion = this.getLatestVersion(caseTypeId);
        return caseDefinitionRepository.getCaseType(latestVersion.getVersion(), caseTypeId);
    }

    public UserRole getUserRoleClassifications(String userRole) {
        return userRoleClassifications.computeIfAbsent(userRole, caseDefinitionRepository::getUserRoleClassifications);
    }

    @Override
    public List<UserRole> getClassificationsForUserRoleList(List<String> userRoles) {
        final List<String> missingRoles = userRoles.stream()
            .filter(role -> !userRoleClassifications.containsKey(role))
            .collect(Collectors.toList());
        if (!missingRoles.isEmpty()) {
            final List<UserRole> missingClassifications = caseDefinitionRepository.getClassificationsForUserRoleList(missingRoles);
            missingClassifications.forEach(userClassification -> userRoleClassifications.putIfAbsent(userClassification.getRole(), userClassification));
        }
        return userRoles.stream().map(userRoleClassifications::get).collect(Collectors.toList());
    }

    @Override
    public CaseTypeDefinitionVersion getLatestVersion(String caseTypeId) {
        return versions.computeIfAbsent(caseTypeId, caseDefinitionRepository::getLatestVersion);
    }

    @Override
    public List<Jurisdiction> getJurisdictions(List<String> ids) {
        return this.caseDefinitionRepository.getJurisdictions(ids);
    }

    @Override
    public CaseType getCaseType(int version, String caseTypeId) {
        return caseDefinitionRepository.getCaseType(version, caseTypeId);
    }

    public List<FieldType> getBaseTypes() {
        return baseTypes.computeIfAbsent("baseTypes", e -> caseDefinitionRepository.getBaseTypes());
    }

    public CaseDefinitionRepository getCaseDefinitionRepository() {
        return caseDefinitionRepository;
    }
}
