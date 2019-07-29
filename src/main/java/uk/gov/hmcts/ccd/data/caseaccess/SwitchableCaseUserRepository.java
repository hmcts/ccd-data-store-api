package uk.gov.hmcts.ccd.data.caseaccess;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Named
@Singleton
@Qualifier(SwitchableCaseUserRepository.QUALIFIER)
public class SwitchableCaseUserRepository implements CaseUserRepository {

    public static final String QUALIFIER = "switchable";
    private final CaseDetailsRepository caseDetailsRepository;
    private final CaseUserRepository ccdCaseUserRepository;
    private final CaseUserRepository amCaseUserRepository;
    private final AMSwitch amSwitch;

    public SwitchableCaseUserRepository(@Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
                                        @Qualifier(CCDCaseUserRepository.QUALIFIER) final CaseUserRepository ccdCaseUserRepository,
                                        @Qualifier(AMCaseUserRepository.QUALIFIER) final CaseUserRepository amCaseUserRepository,
                                        final AMSwitch amSwitch) {
        this.caseDetailsRepository = caseDetailsRepository;
        this.ccdCaseUserRepository = ccdCaseUserRepository;
        this.amCaseUserRepository = amCaseUserRepository;
        this.amSwitch = amSwitch;
    }

    @Override
    public void grantAccess(final String jurisdictionId, final String caseReference, final Long caseId, final String userId, final String caseRole) {
        final Optional<CaseDetails> maybeCase = caseDetailsRepository.findByReference(jurisdictionId, Long.valueOf(caseReference));
        final CaseDetails caseDetails = maybeCase.orElseThrow(() -> new CaseNotFoundException(caseReference));
        if (amSwitch.isWriteAccessManagementWithCCD(caseDetails.getCaseTypeId())) {
            ccdCaseUserRepository.grantAccess(jurisdictionId, caseReference, Long.valueOf(caseDetails.getId()), userId, caseRole);
        }
        if (amSwitch.isWriteAccessManagementWithAM(caseDetails.getCaseTypeId())) {
            amCaseUserRepository.grantAccess(jurisdictionId, caseReference, Long.valueOf(caseDetails.getId()), userId, caseRole);
        }
    }

    @Override
    public void revokeAccess(final String jurisdictionId, final String caseReference, final Long caseId, final String userId, final String caseRole) {
        final Optional<CaseDetails> maybeCase = caseDetailsRepository.findByReference(jurisdictionId,
            Long.valueOf(caseReference));
        final CaseDetails caseDetails = maybeCase.orElseThrow(() -> new CaseNotFoundException(caseReference));
        if (amSwitch.isWriteAccessManagementWithCCD(caseDetails.getCaseTypeId())) {
            ccdCaseUserRepository.revokeAccess(jurisdictionId, caseReference, Long.valueOf(caseDetails.getId()), userId, caseRole);
        }
        if (amSwitch.isWriteAccessManagementWithAM(caseDetails.getCaseTypeId())) {
            amCaseUserRepository.revokeAccess(jurisdictionId, caseReference, Long.valueOf(caseDetails.getId()), userId, caseRole);
        }
    }

    @Override
    public List<Long> findCasesUserIdHasAccessTo(final String userId) {
        List<Long> casesReferences = Lists.newArrayList();
        Lists.newArrayList(ccdCaseUserRepository, amCaseUserRepository).forEach(caseUserRepository ->
            casesReferences.addAll(caseUserRepository.findCasesUserIdHasAccessTo(userId)
                .stream()
                .map(caseId -> caseDetailsRepository.findById(caseId))
                .filter(caseDetails -> discardCasesForUknownMode(caseUserRepository, caseDetails))
                .map(caseDetails -> Long.valueOf(caseDetails.getId()))
                .collect(Collectors.toList()))
        );
        return casesReferences;
    }

    @Override
    public List<String> findCaseRoles(final String caseTypeId, final Long caseId, final String userId) {
        return amSwitch.isReadAccessManagementWithCCD(caseTypeId) ? ccdCaseUserRepository.findCaseRoles(caseTypeId, caseId, userId) : amCaseUserRepository.findCaseRoles(caseTypeId, caseId, userId);
    }

    private boolean discardCasesForUknownMode(final CaseUserRepository caseUserRepository, final CaseDetails caseDetails) {
        return amSwitch.isReadAccessManagementWithCCD(caseDetails.getCaseTypeId()) && caseUserRepository instanceof CCDCaseUserRepository ||
            amSwitch.isReadAccessManagementWithAM(caseDetails.getCaseTypeId()) && caseUserRepository instanceof AMCaseUserRepository;
    }
}
