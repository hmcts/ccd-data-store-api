package uk.gov.hmcts.ccd.domain.service.aggregated;

import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Set;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_TYPE_FOUND_DETAILS;

public abstract class AbstractAuthorisedCaseViewOperation {

    private final CaseDefinitionRepository caseDefinitionRepository;
    private final AccessControlService accessControlService;
    private final UserRepository userRepository;

    AbstractAuthorisedCaseViewOperation(CaseDefinitionRepository caseDefinitionRepository,
                                        AccessControlService accessControlService,
                                        UserRepository userRepository) {
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.accessControlService = accessControlService;
        this.userRepository = userRepository;
    }

    void verifyReadAccess(CaseType caseType, Set<String> userRoles) {
        if (!accessControlService.canAccessCaseTypeWithCriteria(caseType, userRoles, CAN_READ)) {
            ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(AccessControlService
                                                                                                   .NO_CASE_TYPE_FOUND);
            resourceNotFoundException.withDetails(NO_CASE_TYPE_FOUND_DETAILS);
            throw resourceNotFoundException;
        }
    }

    CaseType getCaseType(String caseTypeId) {
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseType == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }
        return caseType;
    }

    Set<String> getUserRoles() {
        return userRepository.getUserRoles();
    }

    AccessControlService getAccessControlService() {
        return accessControlService;
    }
}
