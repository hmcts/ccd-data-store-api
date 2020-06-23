package uk.gov.hmcts.ccd.domain.service.supplementarydata.rolevalidator;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.IdamJurisdictionsResolver;
import uk.gov.hmcts.ccd.data.user.JurisdictionsResolver;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

@Service
@Qualifier("default")
public class DefaultSupplementaryDataUserRoleValidator implements SupplementaryDataUserRoleValidator {

    private final String roleCaseWorkerCaa = "caseworker-caa";

    private final UserRepository userRepository;
    private final CaseDetailsRepository caseDetailsRepository;
    private final JurisdictionsResolver jurisdictionsResolver;
    private final CaseAccessService caseAccessService;

    @Autowired
    public DefaultSupplementaryDataUserRoleValidator(final @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                                                     final @Qualifier(CachedCaseDetailsRepository.QUALIFIER) CaseDetailsRepository caseDetailsRepository,
                                                     final @Qualifier(IdamJurisdictionsResolver.QUALIFIER) JurisdictionsResolver jurisdictionsResolver,
                                                     final CaseAccessService caseAccessService) {
        this.userRepository = userRepository;
        this.caseDetailsRepository = caseDetailsRepository;
        this.jurisdictionsResolver = jurisdictionsResolver;
        this.caseAccessService = caseAccessService;
    }

    @Override
    public boolean canUpdateSupplementaryData(String caseReference) {
        Optional<CaseDetails> caseDetails = this.caseDetailsRepository.findByReference(caseReference);
        boolean canAccess = false;
        if (caseDetails.isPresent()) {
            canAccess = this.caseAccessService.canUserAccess(caseDetails.get());

            canAccess = canAccess || this.jurisdictionsResolver
                .getJurisdictions()
                .stream()
                .anyMatch(caseDetails.get().getJurisdiction()::equalsIgnoreCase);
        }
        return canAccess || this.userRepository.getUserRoles().contains(roleCaseWorkerCaa);
    }
}
