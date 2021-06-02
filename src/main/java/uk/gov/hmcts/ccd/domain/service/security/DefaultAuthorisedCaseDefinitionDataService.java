package uk.gov.hmcts.ccd.domain.service.security;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;

import static java.util.stream.Collectors.toList;

/**
 * Service to return authorised case definition data as per user authority.
 */
@Service
public class DefaultAuthorisedCaseDefinitionDataService implements AuthorisedCaseDefinitionDataService {

    private final CaseTypeService caseTypeService;
    private final AccessControlService accessControlService;
    private final UserRepository userRepository;
    private final CaseDataAccessControl caseDataAccessControl;

    @Autowired
    public DefaultAuthorisedCaseDefinitionDataService(CaseTypeService caseTypeService,
                                                      AccessControlService accessControlService,
                                                      @Qualifier(CachedUserRepository.QUALIFIER)
                                                              UserRepository userRepository,
                                                      CaseDataAccessControl caseDataAccessControl) {
        this.caseTypeService = caseTypeService;
        this.accessControlService = accessControlService;
        this.userRepository = userRepository;
        this.caseDataAccessControl = caseDataAccessControl;
    }

    @Override
    public Optional<CaseTypeDefinition> getAuthorisedCaseType(String caseTypeId, Predicate<AccessControlList> access) {
        CaseTypeDefinition caseTypeDefinition = caseTypeService.getCaseType(caseTypeId);
        if (verifyAclOnCaseType(caseTypeDefinition, access)
            && verifySecurityClassificationOnCaseType(caseTypeDefinition)) {
            return Optional.of(caseTypeDefinition);
        }
        return Optional.empty();
    }

    @Override
    public List<CaseStateDefinition> getUserAuthorisedCaseStates(String jurisdiction, String caseTypeId,
                                                                 Predicate<AccessControlList> access) {
        CaseTypeDefinition caseTypeDefinition = caseTypeService.getCaseTypeForJurisdiction(caseTypeId, jurisdiction);
        return filterCaseStatesForUser(caseTypeDefinition, access);
    }

    @Override
    public List<String> getUserAuthorisedCaseStateIds(String caseTypeId, Predicate<AccessControlList> access) {
        CaseTypeDefinition caseTypeDefinition = caseTypeService.getCaseType(caseTypeId);
        List<CaseStateDefinition> caseStateDefinitions =
            filterCaseStatesForUser(caseTypeDefinition, access);
        return collectCaseStateIds(caseStateDefinitions);
    }

    @Override
    public List<String> getUserAuthorisedCaseStateIds(String jurisdiction, String caseTypeId,
                                                      Predicate<AccessControlList> access) {
        List<CaseStateDefinition> caseStateDefinitions = getUserAuthorisedCaseStates(jurisdiction, caseTypeId, access);
        return collectCaseStateIds(caseStateDefinitions);
    }

    private boolean verifyAclOnCaseType(CaseTypeDefinition caseTypeDefinition, Predicate<AccessControlList> access) {
        return accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition,
            getAccessProfiles(caseTypeDefinition.getId()), access);
    }

    private boolean verifySecurityClassificationOnCaseType(CaseTypeDefinition caseTypeDefinition) {
        return userRepository.getHighestUserClassification(
            caseTypeDefinition.getJurisdictionDefinition().getId())
            .higherOrEqualTo(caseTypeDefinition.getSecurityClassification());
    }

    private List<CaseStateDefinition> filterCaseStatesForUser(CaseTypeDefinition caseTypeDefinition,
                                                              Predicate<AccessControlList> access) {
        return accessControlService.filterCaseStatesByAccess(caseTypeDefinition,
            getAccessProfiles(caseTypeDefinition.getId()),
            access);
    }

    private List<String> collectCaseStateIds(List<CaseStateDefinition> caseStateDefinitions) {
        return caseStateDefinitions.stream().map(CaseStateDefinition::getId).collect(toList());
    }

    private Set<AccessProfile> getAccessProfiles(String caseTypeId) {
        return caseDataAccessControl.generateAccessProfilesByCaseTypeId(caseTypeId);
    }
}
