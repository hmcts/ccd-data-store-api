package uk.gov.hmcts.ccd.domain.service.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@Service
@Qualifier("default")
public class DefaultEndpointAuthorisationService implements EndpointAuthorisationService {

    private final UserRepository userRepository;
    private final CaseAccessService caseAccessService;
    private final ApplicationParams applicationParams;

    @Autowired
    public DefaultEndpointAuthorisationService(final @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                                               final CaseAccessService caseAccessService,
                                               final ApplicationParams applicationParams) {
        this.userRepository = userRepository;
        this.caseAccessService = caseAccessService;
        this.applicationParams = applicationParams;
    }

    @Override
    public boolean isAccessAllowed(CaseDetails caseDetails) {

        if (userRepository.getUserRoles().stream().anyMatch(applicationParams.getCcdAccessControlCrossJurisdictionRoles()::contains)) {
            return true;
        }

        if (this.caseAccessService.canOnlyViewExplicitlyGrantedCases()) {
            return this.caseAccessService.isExplicitAccessGranted(caseDetails);
        }

        return this.caseAccessService.isJurisdictionAccessAllowed(caseDetails.getJurisdiction());
    }
}
