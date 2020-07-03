package uk.gov.hmcts.ccd.domain.service.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@Service
@Qualifier("default")
public class DefaultEndpointAuthorisationService implements EndpointAuthorisationService {

    private static final String ROLE_CASE_WORKER_CAA = "caseworker-caa";

    private final UserRepository userRepository;
    private final CaseAccessService caseAccessService;

    @Autowired
    public DefaultEndpointAuthorisationService(final @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                                               final CaseAccessService caseAccessService) {
        this.userRepository = userRepository;
        this.caseAccessService = caseAccessService;
    }

    @Override
    public boolean isAccessAllowed(CaseDetails caseDetails) {

        if (this.userRepository.getUserRoles().contains(ROLE_CASE_WORKER_CAA)) {
            return true;
        }

        if (this.caseAccessService.canOnlyViewGrantedCases()) {
            return this.caseAccessService.isExplicitAccessGranted(caseDetails);
        }

        return this.caseAccessService.isJurisdictionAccessAllowed(caseDetails.getJurisdiction());
    }
}
