package uk.gov.hmcts.ccd.domain.service.security;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;

/**
 * Service to return authorised case definition data as per user authority.
 */
@Service
public class DefaultAuthorisedCaseDefinitionDataService implements AuthorisedCaseDefinitionDataService {

    private final CaseTypeService caseTypeService;
    private final AccessControlService accessControlService;
    private final UserRepository userRepository;
    private final CaseUserRepository caseUserRepository;

    @Autowired
    public DefaultAuthorisedCaseDefinitionDataService(CaseTypeService caseTypeService,
                                                      AccessControlService accessControlService,
                                                      @Qualifier(CachedUserRepository.QUALIFIER)
                                                              UserRepository userRepository,
                                                      @Qualifier(CachedCaseUserRepository.QUALIFIER)
                                                      CaseUserRepository caseUserRepository) {
        this.caseTypeService = caseTypeService;
        this.accessControlService = accessControlService;
        this.userRepository = userRepository;
        this.caseUserRepository = caseUserRepository;
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
        return filterCaseStatesAndUserRolesForUser(caseTypeDefinition.getStates(), access);
        //  return filterCaseStatesForUser(caseTypeDefinition.getStates(), access);
    }

    @Override
    public List<String> getUserAuthorisedCaseStateIds(String caseTypeId, Predicate<AccessControlList> access) {
        CaseTypeDefinition caseTypeDefinition = caseTypeService.getCaseType(caseTypeId);
        List<CaseStateDefinition> caseStateDefinitions =
            filterCaseStatesForUser(caseTypeDefinition.getStates(), access);
        return collectCaseStateIds(caseStateDefinitions);
    }

    @Override
    public List<String> getUserAuthorisedCaseStateIds(String jurisdiction, String caseTypeId,
                                                      Predicate<AccessControlList> access) {
        List<CaseStateDefinition> caseStateDefinitions = getUserAuthorisedCaseStates(jurisdiction, caseTypeId, access);
        return collectCaseStateIds(caseStateDefinitions);
    }

    private boolean verifyAclOnCaseType(CaseTypeDefinition caseTypeDefinition, Predicate<AccessControlList> access) {
        return accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition, getUserRoles(), access);
    }

    private boolean verifySecurityClassificationOnCaseType(CaseTypeDefinition caseTypeDefinition) {
        return userRepository.getHighestUserClassification(
            caseTypeDefinition.getJurisdictionDefinition().getId())
            .higherOrEqualTo(caseTypeDefinition.getSecurityClassification());
    }

    private List<CaseStateDefinition> filterCaseStatesForUser(List<CaseStateDefinition> caseStateDefinitions,
                                                              Predicate<AccessControlList> access) {
        return accessControlService.filterCaseStatesByAccess(caseStateDefinitions, getUserRoles(), access);
    }

    private List<String> collectCaseStateIds(List<CaseStateDefinition> caseStateDefinitions) {
        return caseStateDefinitions.stream().map(CaseStateDefinition::getId).collect(toList());
    }

    private Set<String> getUserRoles() {
        return userRepository.getUserRoles();
    }

    private List<CaseStateDefinition> filterCaseStatesAndUserRolesForUser(List<CaseStateDefinition> caseStateDefinitions,
                                                                          Predicate<AccessControlList> access) {
        return accessControlService.filterCaseStatesByAccess(caseStateDefinitions, getCaseAndUserRoles(), access);
    }

    private Set<String> getCaseAndUserRoles() {
        return Sets.union(userRepository.getUserRoles(),
            caseUserRepository.getCaseUserRolesByUserId(userRepository.getUserId()));
    }
}
