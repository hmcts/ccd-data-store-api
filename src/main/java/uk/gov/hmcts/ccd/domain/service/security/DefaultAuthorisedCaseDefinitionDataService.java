package uk.gov.hmcts.ccd.domain.service.security;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
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

    @Autowired
    public DefaultAuthorisedCaseDefinitionDataService(CaseTypeService caseTypeService, AccessControlService accessControlService,
                                                      @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository) {
        this.caseTypeService = caseTypeService;
        this.accessControlService = accessControlService;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<CaseType> getAuthorisedCaseType(String caseTypeId, Predicate<AccessControlList> access) {
        CaseType caseType = caseTypeService.getCaseType(caseTypeId);
        if (verifyAclOnCaseType(caseType, access) && verifySecurityClassificationOnCaseType(caseType)) {
            return Optional.of(caseType);
        }
        return Optional.empty();
    }

    private boolean verifyAclOnCaseType(CaseType caseType, Predicate<AccessControlList> access) {
        return accessControlService.canAccessCaseTypeWithCriteria(caseType, getUserRoles(), access);
    }

    private boolean verifySecurityClassificationOnCaseType(CaseType caseType) {
        return userRepository.getHighestUserClassification().higherOrEqualTo(caseType.getSecurityClassification());
    }

    @Override
    public List<String> getUserAuthorisedCaseStateIds(String jurisdiction, String caseTypeId, Predicate<AccessControlList> access) {
        List<CaseState> caseStates = getUserAuthorisedCaseStates(jurisdiction, caseTypeId, access);
        return collectCaseStateIds(caseStates);
    }

    @Override
    public List<CaseState> getUserAuthorisedCaseStates(String jurisdiction, String caseTypeId, Predicate<AccessControlList> access) {
        CaseType caseType = caseTypeService.getCaseTypeForJurisdiction(caseTypeId, jurisdiction);
        return filterCaseStatesForUser(caseType.getStates(), access);
    }

    @Override
    public List<String> getUserAuthorisedCaseStateIds(String caseTypeId, Predicate<AccessControlList> access) {
        CaseType caseType = caseTypeService.getCaseType(caseTypeId);
        List<CaseState> caseStates = filterCaseStatesForUser(caseType.getStates(), access);
        return collectCaseStateIds(caseStates);
    }

    private List<CaseState> filterCaseStatesForUser(List<CaseState> caseStates, Predicate<AccessControlList> access) {
        return accessControlService.filterCaseStatesByAccess(caseStates, getUserRoles(), access);
    }

    private List<String> collectCaseStateIds(List<CaseState> caseStates) {
        return caseStates.stream().map(CaseState::getId).collect(toList());
    }

    private Set<String> getUserRoles() {
        return userRepository.getUserRoles();
    }
}
