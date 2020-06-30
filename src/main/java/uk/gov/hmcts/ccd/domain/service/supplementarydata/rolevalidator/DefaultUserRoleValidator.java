package uk.gov.hmcts.ccd.domain.service.supplementarydata.rolevalidator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

@Service
@Qualifier("default")
public class DefaultUserRoleValidator implements UserRoleValidator {

    private static final String ROLE_CASE_WORKER_CAA = "caseworker-caa";

    private final UserRepository userRepository;
    private final CaseAccessService caseAccessService;

    @Autowired
    public DefaultUserRoleValidator(final @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                                    final CaseAccessService caseAccessService) {
        this.userRepository = userRepository;
        this.caseAccessService = caseAccessService;
    }

    @Override
    public boolean canUpdateSupplementaryData(CaseDetails caseDetails) {
        boolean canAccess = this.userRepository.getUserRoles().contains(ROLE_CASE_WORKER_CAA);

        if (!canAccess) {
            canAccess = this.caseAccessService.canUserAccess(caseDetails);
        }

        if (!canAccess && !this.caseAccessService.canOnlyViewGrantedCases()) {
            canAccess = this.caseAccessService.isJurisdictionAccessAllowed(caseDetails.getJurisdiction());
        }

        return canAccess;
    }
}
