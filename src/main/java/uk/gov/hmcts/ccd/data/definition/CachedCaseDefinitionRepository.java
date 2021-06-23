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
import uk.gov.hmcts.ccd.endpoint.exceptions.DataProcessingException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Maps.newHashMap;

@Service
@Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
@RequestScope
// TODO: Make this repository return copies of the maps https://tools.hmcts.net/jira/browse/RDM-1459
public class CachedCaseDefinitionRepository implements CaseDefinitionRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedCaseDefinitionRepository.class);

    public static final String QUALIFIER = "cached";
    public static final String BASE_TYPE_DEFAULT_KEY = "baseTypes";
    public static final String JURISDICTIONS_DEFAULT_KEY = "jurisdictions";

    private final CaseDefinitionRepository caseDefinitionRepository;
    private final Map<String, List<CaseTypeDefinition>> caseTypesForJurisdictions = newHashMap();
    private final Map<String, List<JurisdictionDefinition>> jurisdictionsList = newHashMap();
    private final Map<String, CaseTypeDefinitionVersion> versions = newHashMap();
    private final Map<String, JurisdictionDefinition> jurisdictions = newHashMap();
    private final Map<String, CaseTypeDefinition> caseTypes = newHashMap();
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
        return this.getCaseType(latestVersion.getVersion(), caseTypeId);
    }

    @Override
    public CaseTypeDefinition getCaseType(int version, String caseTypeId) {
        try {
            return caseTypes.computeIfAbsent(caseTypeId + version,
                e -> caseDefinitionRepository.getCaseType(version, caseTypeId)).shallowClone();
        } catch (CloneNotSupportedException cloneNotSupportedException) {
            throw new DataProcessingException().withDetails(
                String.format("Unable to clone case type definition for CaseTypeId %s.", caseTypeId));
        }
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
        return jurisdictions.computeIfAbsent(jurisdictionId, caseDefinitionRepository::getJurisdiction);
    }

    @Override
    public List<JurisdictionDefinition> getJurisdictions(List<String> jurisdictionIds) {
        return jurisdictionsList.computeIfAbsent(prepareJurisdictionsKey(jurisdictionIds),
            e -> caseDefinitionRepository.getJurisdictions(jurisdictionIds));
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
        return baseTypes.computeIfAbsent(BASE_TYPE_DEFAULT_KEY, e -> caseDefinitionRepository.getBaseTypes());
    }

    private String prepareJurisdictionsKey(List<String> jurisdictionIds) {
        if (jurisdictionIds == null || jurisdictionIds.isEmpty()) {
            return JURISDICTIONS_DEFAULT_KEY;
        }

        Stream<String> sortedJurisdictions = jurisdictionIds.stream().sorted();
        return sortedJurisdictions.collect(Collectors.joining());
    }
}
