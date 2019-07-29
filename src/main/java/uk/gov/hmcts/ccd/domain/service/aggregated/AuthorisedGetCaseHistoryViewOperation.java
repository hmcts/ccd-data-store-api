package uk.gov.hmcts.ccd.domain.service.aggregated;

import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.SwitchableCaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseHistoryView;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier(AuthorisedGetCaseHistoryViewOperation.QUALIFIER)
public class AuthorisedGetCaseHistoryViewOperation extends AbstractAuthorisedCaseViewOperation implements
    GetCaseHistoryViewOperation {

    public static final String QUALIFIER = "authorised-case-history";
    private final GetCaseHistoryViewOperation getCaseHistoryViewOperation;

    public AuthorisedGetCaseHistoryViewOperation(
        @Qualifier(DefaultGetCaseHistoryViewOperation.QUALIFIER) final GetCaseHistoryViewOperation getCaseHistoryViewOperation,
        @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
        final AccessControlService accessControlService,
        @Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
        @Qualifier(SwitchableCaseUserRepository.QUALIFIER) final CaseUserRepository caseUserRepository,
        @Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository) {

        super(caseDefinitionRepository, accessControlService, userRepository, caseUserRepository, caseDetailsRepository);
        this.getCaseHistoryViewOperation = getCaseHistoryViewOperation;
    }

    @Override
    public CaseHistoryView execute(String caseReference, Long eventId) {
        CaseDetails caseDetails = getCase(caseReference);
        CaseType caseType = getCaseType(caseDetails.getCaseTypeId());
        Set<String> userRoles = getUserRoles(caseType.getId(), caseDetails.getId());
        verifyCaseTypeReadAccess(caseType, userRoles);
        CaseHistoryView caseHistoryView = getCaseHistoryViewOperation.execute(caseReference, eventId);
        filterAllowedTabsWithFields(caseHistoryView, userRoles);
        return caseHistoryView;
    }
}
