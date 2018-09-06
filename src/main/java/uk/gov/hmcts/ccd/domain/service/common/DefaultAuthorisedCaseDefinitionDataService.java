package uk.gov.hmcts.ccd.domain.service.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

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
    public List<CaseState> getUserAuthorisedCaseStates(String jurisdiction, String caseTypeId, Predicate<AccessControlList> access) {
        CaseType caseType = caseTypeService.getCaseTypeForJurisdiction(caseTypeId, jurisdiction);
        Set<String> userRoles = userRepository.getUserRoles();

        return accessControlService.filterCaseStatesByAccess(caseType.getStates(), userRoles, access);
    }

    @Override
    public List<String> getUserAuthorisedCaseStateIds(String jurisdiction, String caseTypeId, Predicate<AccessControlList> access) {
        return getUserAuthorisedCaseStates(jurisdiction, caseTypeId, access).stream().map(CaseState::getId).collect(toList());
    }
}
