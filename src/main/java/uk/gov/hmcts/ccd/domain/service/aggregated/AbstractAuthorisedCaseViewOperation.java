package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_TYPE_FOUND_DETAILS;

import com.google.common.collect.Sets;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

public abstract class AbstractAuthorisedCaseViewOperation {

    private final CaseDefinitionRepository caseDefinitionRepository;
    private final AccessControlService accessControlService;
    private final UserRepository userRepository;
    private final CaseUserRepository caseUserRepository;
    private final CaseDetailsRepository caseDetailsRepository;

    AbstractAuthorisedCaseViewOperation(CaseDefinitionRepository caseDefinitionRepository,
                                        AccessControlService accessControlService,
                                        UserRepository userRepository,
                                        CaseUserRepository caseUserRepository,
                                        CaseDetailsRepository caseDetailsRepository) {
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.accessControlService = accessControlService;
        this.userRepository = userRepository;
        this.caseUserRepository = caseUserRepository;
        this.caseDetailsRepository = caseDetailsRepository;
    }

    void verifyReadAccess(CaseType caseType, Set<String> userRoles) {
        if (!accessControlService.canAccessCaseTypeWithCriteria(caseType, userRoles, CAN_READ)) {
            ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(AccessControlService
                .NO_CASE_TYPE_FOUND);
            resourceNotFoundException.withDetails(NO_CASE_TYPE_FOUND_DETAILS);
            throw resourceNotFoundException;
        }
    }

    protected CaseType getCaseType(String caseTypeId) {
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseType == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }
        return caseType;
    }

    protected String getCaseId(String caseReference) {
        return getCase(caseReference).getId();
    }

    protected CaseDetails getCase(String caseReference) {
        Optional<CaseDetails> caseDetails = this.caseDetailsRepository.findByReference(caseReference);
        return caseDetails
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }

    protected Set<String> getUserRoles(String caseId) {
        return Sets.union(userRepository.getUserRoles(),
            caseUserRepository
                .findCaseRoles(Long.valueOf(caseId), userRepository.getUserId())
                .stream()
                .collect(Collectors.toSet()));
    }

    AccessControlService getAccessControlService() {
        return accessControlService;
    }
}
