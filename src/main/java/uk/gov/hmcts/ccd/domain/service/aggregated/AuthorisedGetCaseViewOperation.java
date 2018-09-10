package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import java.util.Set;

@Service
@Qualifier(AuthorisedGetCaseTypesOperation.QUALIFIER)
public class AuthorisedGetCaseViewOperation extends AbstractAuthorisedCaseViewOperation implements
    GetCaseViewOperation {

    public static final String QUALIFIER = "authorised";
    private final GetCaseViewOperation getCaseViewOperation;

    public AuthorisedGetCaseViewOperation(
        final @Qualifier(DefaultGetCaseViewOperation.QUALIFIER) GetCaseViewOperation getCaseViewOperation,
        @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
        final AccessControlService accessControlService,
        final @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository) {
        super(caseDefinitionRepository, accessControlService, userRepository);
        this.getCaseViewOperation = getCaseViewOperation;
    }

    @Override
    public CaseView execute(String caseReference) {
        CaseView caseView = getCaseViewOperation.execute(caseReference);

        CaseType caseType = getCaseType(caseView.getCaseType().getId());
        Set<String> userRoles = getUserRoles();
        verifyReadAccess(caseType, userRoles);

        return filterUpsertAccess(caseType, userRoles, caseView);
    }

    @Override
    @Deprecated
    public CaseView execute(String jurisdictionId, String caseTypeId, String caseReference) {
        return execute(caseReference);
    }

    private CaseView filterUpsertAccess(CaseType caseType, Set<String> userRoles, CaseView caseView) {
        CaseViewTrigger[] authorisedTriggers;
        if (!getAccessControlService().canAccessCaseTypeWithCriteria(caseType,
                                                                     userRoles,
                                                                     AccessControlService.CAN_UPDATE) ||
            !getAccessControlService().canAccessCaseStateWithCriteria(caseView.getState().getId(),
                                                                      caseType,
                                                                      userRoles,
                                                                      AccessControlService.CAN_UPDATE)) {
            authorisedTriggers = new CaseViewTrigger[]{};
        } else {
            authorisedTriggers = getAccessControlService().filterCaseViewTriggersByCreateAccess(caseView.getTriggers(),
                                                                                                caseType.getEvents(),
                                                                                                userRoles);
        }

        caseView.setTriggers(authorisedTriggers);

        return caseView;
    }
}
