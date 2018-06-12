package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseHistoryView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Set;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_TYPE_FOUND_DETAILS;

@Service
@Qualifier(AuthorisedGetCaseTypesOperation.QUALIFIER)
public class AuthorisedGetCaseViewOperation implements GetCaseViewOperation {

    public static final String QUALIFIER = "authorised";
    private final GetCaseViewOperation getCaseViewOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final AccessControlService accessControlService;
    private final UserRepository userRepository;

    public AuthorisedGetCaseViewOperation(final @Qualifier(DefaultGetCaseViewOperation.QUALIFIER) GetCaseViewOperation getCaseViewOperation,
                                          @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                          final AccessControlService accessControlService,
                                          final @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository) {
        this.getCaseViewOperation = getCaseViewOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.accessControlService = accessControlService;
        this.userRepository = userRepository;
    }

    @Override
    public CaseView execute(String jurisdictionId, String caseTypeId, String caseReference) {

        CaseType caseType = getCaseType(caseTypeId);

        Set<String> userRoles = getUserRoles();

        verifyReadAccess(caseType, userRoles);

        CaseView caseView = getCaseViewOperation.execute(jurisdictionId, caseTypeId, caseReference);

        return filterUpsertAccess(caseType, userRoles, caseView);
    }

    @Override
    public CaseHistoryView execute(String jurisdictionId, String caseTypeId, String caseReference, Long eventId) {
        CaseType caseType = getCaseType(caseTypeId);

        Set<String> userRoles = getUserRoles();

        verifyReadAccess(caseType, userRoles);

        return getCaseViewOperation.execute(jurisdictionId, caseTypeId, caseReference, eventId);
    }

    private void verifyReadAccess(CaseType caseType, Set<String> userRoles) {
        if (!accessControlService.canAccessCaseTypeWithCriteria(
            caseType,
            userRoles,
            CAN_READ)) {
            ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(AccessControlService.NO_CASE_TYPE_FOUND);
            resourceNotFoundException.withDetails(NO_CASE_TYPE_FOUND_DETAILS);
            throw resourceNotFoundException;
        }
    }

    private CaseView filterUpsertAccess(CaseType caseType, Set<String> userRoles, CaseView caseView) {
        CaseViewTrigger[] authorisedTriggers;
        if (!accessControlService.canAccessCaseTypeWithCriteria(caseType,
                                                                userRoles,
                                                                AccessControlService.CAN_UPDATE)||
            !accessControlService.canAccessCaseStateWithCriteria(caseView.getState().getId(),
                                                                 caseType,
                                                                 userRoles,
                                                                 AccessControlService.CAN_UPDATE)) {
            authorisedTriggers = new CaseViewTrigger[] {};
        } else {
            authorisedTriggers = accessControlService.filterCaseViewTriggersByCreateAccess(caseView.getTriggers(),
                                                                               caseType.getEvents(),
                                                                               userRoles);
        }

        caseView.setTriggers(authorisedTriggers);

        return caseView;
    }

    private CaseType getCaseType(String caseTypeId) {
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseType == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }
        return caseType;
    }

    private Set<String> getUserRoles() {
        return userRepository.getUserRoles();
    }

}
