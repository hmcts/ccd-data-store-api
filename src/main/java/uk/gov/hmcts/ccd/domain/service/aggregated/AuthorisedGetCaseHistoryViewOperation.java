package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseRoleRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseRoleRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseHistoryView;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

@Service
@Qualifier(AuthorisedGetCaseHistoryViewOperation.QUALIFIER)
public class AuthorisedGetCaseHistoryViewOperation extends AbstractAuthorisedCaseViewOperation implements
    GetCaseHistoryViewOperation {

    public static final String QUALIFIER = "authorised-case-history";
    private final GetCaseHistoryViewOperation getCaseHistoryViewOperation;

    public AuthorisedGetCaseHistoryViewOperation(
        @Qualifier(DefaultGetCaseHistoryViewOperation.QUALIFIER) GetCaseHistoryViewOperation getCaseHistoryViewOperation,
        @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) CaseDefinitionRepository caseDefinitionRepository,
        AccessControlService accessControlService,
        @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
        @Qualifier(CachedCaseRoleRepository.QUALIFIER) CaseRoleRepository caseRoleRepository) {

        super(caseDefinitionRepository, accessControlService, userRepository, caseRoleRepository);
        this.getCaseHistoryViewOperation = getCaseHistoryViewOperation;
    }

    @Override
    public CaseHistoryView execute(String jurisdictionId, String caseTypeId, String caseReference, Long eventId) {
        CaseType caseType = getCaseType(caseTypeId);

        Set<String> userRoles = getUserRoles(caseTypeId);

        verifyReadAccess(caseType, userRoles);

        return getCaseHistoryViewOperation.execute(jurisdictionId, caseTypeId, caseReference, eventId);
    }

}
