package uk.gov.hmcts.ccd.data.definition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
// TODO: Make this repository return copies of the maps https://tools.hmcts.net/jira/browse/RDM-1459
public class CachedCaseDefinitionRepository implements CaseDefinitionRepository {

    public static final String QUALIFIER = "cached";
    private static final Logger LOGGER = LoggerFactory.getLogger(CachedCaseDefinitionRepository.class);

    private final CaseDefinitionRepository caseDefinitionRepository;

    @Autowired
    @SuppressWarnings("checkstyle:MemberName")
    private CachedCaseDefinitionRepository _this;

    @Autowired
    public CachedCaseDefinitionRepository(@Qualifier(DefaultCaseDefinitionRepository.QUALIFIER)
                                              final CaseDefinitionRepository caseDefinitionRepository) {
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    @Override
    @Cacheable("caseTypesForJurisdictionCache")
    public List<CaseTypeDefinition> getCaseTypesForJurisdiction(final String jurisdictionId) {
        return caseDefinitionRepository.getCaseTypesForJurisdiction(jurisdictionId);
    }

    @Override
    public CaseTypeDefinition getCaseType(final String caseTypeId) {
        CaseTypeDefinitionVersion latestVersion = this.getLatestVersion(caseTypeId);
        return _this.getCaseType(latestVersion.getVersion(), caseTypeId);
    }

    @Override
    @Cacheable("caseTypeDefinitionsCache")
    public CaseTypeDefinition getCaseType(int version, String caseTypeId) {
        return caseDefinitionRepository.getCaseType(version, caseTypeId);
    }

    @Override
    @Cacheable("userRoleClassificationsCache")
    public UserRole getUserRoleClassifications(String userRole) {
        return caseDefinitionRepository.getUserRoleClassifications(userRole);
    }

    @Override
    public List<UserRole> getClassificationsForUserRoleList(List<String> userRoles) {
        return userRoles.stream()
            .map(_this::getUserRoleClassifications)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public CaseTypeDefinitionVersion getLatestVersion(String caseTypeId) {
        return caseDefinitionRepository.getLatestVersion(caseTypeId);
    }

    @Override
    @Cacheable("jurisdictionCache")
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
    @Cacheable("baseTypesCache")
    public List<FieldTypeDefinition> getBaseTypes() {
        return caseDefinitionRepository.getBaseTypes();
    }

    public CaseDefinitionRepository getCaseDefinitionRepository() {
        return caseDefinitionRepository;
    }

}
