package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.Arrays;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTab;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;

@Service
@Qualifier(AuthorisedGetCaseViewOperation.QUALIFIER)
public class AuthorisedGetCaseViewOperation extends AbstractAuthorisedCaseViewOperation implements
    GetCaseViewOperation {

    public static final String QUALIFIER = "authorised";
    private final GetCaseViewOperation getCaseViewOperation;

    public AuthorisedGetCaseViewOperation(
        final @Qualifier(DefaultGetCaseViewOperation.QUALIFIER) GetCaseViewOperation getCaseViewOperation,
        @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
        final AccessControlService accessControlService,
        final @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
        final @Qualifier(CachedCaseUserRepository.QUALIFIER) CaseUserRepository caseUserRepository,
        final @Qualifier(CachedCaseDetailsRepository.QUALIFIER) CaseDetailsRepository caseDetailsRepository) {
        super(caseDefinitionRepository, accessControlService, userRepository, caseUserRepository, caseDetailsRepository);
        this.getCaseViewOperation = getCaseViewOperation;
    }

    @Override
    public CaseView execute(String caseReference) {
        CaseView caseView = getCaseViewOperation.execute(caseReference);

        CaseType caseType = getCaseType(caseView.getCaseType().getId());
        String caseId = getCaseId(caseReference);
        Set<String> userRoles = getUserRoles(caseId);
        verifyCaseTypeReadAccess(caseType, userRoles);
        filterCaseTabFieldsByReadAccess(caseView, userRoles);
        filterAllowedTabsWithFields(caseView, userRoles);
        return filterUpsertAccess(caseType, userRoles, caseView);
    }

    private void filterCaseTabFieldsByReadAccess(CaseView caseView, Set<String> userRoles) {
        caseView.setTabs(Arrays.stream(caseView.getTabs()).map(
            caseViewTab -> {
                caseViewTab.setFields(Arrays.stream(caseViewTab.getFields())
                    .filter(caseViewField -> getAccessControlService().canAccessCaseViewFieldWithCriteria(caseViewField, userRoles, CAN_READ))
                    .toArray(CaseViewField[]::new));
                return caseViewTab;
            }).toArray(CaseViewTab[]::new));
    }

    private CaseView filterUpsertAccess(CaseType caseType, Set<String> userRoles, CaseView caseView) {
        CaseViewTrigger[] authorisedTriggers;
        if (!getAccessControlService().canAccessCaseTypeWithCriteria(caseType,
                                                                     userRoles,
                                                                     CAN_UPDATE)
            || !getAccessControlService().canAccessCaseStateWithCriteria(caseView.getState().getId(),
                                                                      caseType,
                                                                      userRoles,
                                                                      CAN_UPDATE)) {
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
