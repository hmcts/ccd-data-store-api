package uk.gov.hmcts.ccd.data.caseaccess;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@Named
@Singleton
@Qualifier(SwitchableCaseUserRepository.QUALIFIER)
public class SwitchableCaseUserRepository implements CaseUserRepository {

    private static final Logger LOG = LoggerFactory.getLogger(SwitchableCaseUserRepository.class);
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
    public void grantAccess(final String caseTypeId, final Long caseId, final String userId, final String caseRole) {
        if (amSwitch.isWriteAccessManagementWithCCD(caseTypeId)) {
            grantAccess(caseTypeId, caseId, userId, caseRole, ccdCaseUserRepository);
        }
        if (amSwitch.isWriteAccessManagementWithAM(caseTypeId)) {
            grantAccess(caseTypeId, caseId, userId, caseRole, amCaseUserRepository);
        }
    }

    @Override
    public void revokeAccess(final String caseTypeId, final Long caseId, final String userId, final String caseRole) {
        if (amSwitch.isWriteAccessManagementWithCCD(caseTypeId)) {
            revokeAccess(caseTypeId, caseId, userId, caseRole, ccdCaseUserRepository);
        }
        if (amSwitch.isWriteAccessManagementWithAM(caseTypeId)) {
            revokeAccess(caseTypeId, caseId, userId, caseRole, amCaseUserRepository);
        }
    }

    @Override
    public List<Long> findCasesUserIdHasAccessTo(final String userId) {
        List<Long> casesReferences = Lists.newArrayList();
        casesReferences.addAll(findCases(userId, ccdCaseUserRepository));
        casesReferences.addAll(findCases(userId, amCaseUserRepository));
        return casesReferences;
    }

    private List<Long> findCases(final String userId, final CaseUserRepository caseUserRepository) {
        return findCasesUserIdHasAccessTo(userId, caseUserRepository)
            .stream()
            .map(caseId -> caseDetailsRepository.findById(caseId))
            .filter(caseDetails -> discardCasesForUknownMode(caseUserRepository, caseDetails))
            .map(caseDetails -> Long.valueOf(caseDetails.getId()))
            .collect(Collectors.toList());
    }

    @Override
    public List<String> findCaseRoles(final String caseTypeId, final Long caseId, final String userId) {
        return amSwitch.isReadAccessManagementWithCCD(caseTypeId) ? getCaseRoles(caseTypeId, caseId, userId, ccdCaseUserRepository) : getCaseRoles(caseTypeId, caseId, userId, amCaseUserRepository);
    }

    private List<String> getCaseRoles(final String caseTypeId, final Long caseId, final String userId, final CaseUserRepository caseUserRepository) {
        String repositoryType = getRepositoryType(caseUserRepository);
        LOG.info("{}. Finding case roles for caseId={} and userId={} (caseTypeId={})", repositoryType, caseId, userId, caseTypeId);
        List<String> caseRoles = caseUserRepository.findCaseRoles(caseTypeId, caseId, userId);
        LOG.info("{}. Found case roles for caseId={} and userId={} (caseTypeId={})", repositoryType, caseId, userId, caseTypeId);
        return caseRoles;
    }

    private void grantAccess(final String caseTypeId, final Long caseId, final String userId, final String caseRole, final CaseUserRepository caseUserRepository) {
        String repositoryType = getRepositoryType(caseUserRepository);
        LOG.info("{}. Granting role={} access to caseId={} (caseTypeId={}) for userId={}",
            repositoryType, caseRole, caseId, caseTypeId, userId);
        caseUserRepository.grantAccess(caseTypeId, caseId, userId, caseRole);
        LOG.info("{}. Granted role={} access to caseId={} (caseTypeId={}) for userId={}",
            repositoryType, caseRole, caseId, caseTypeId, userId);
    }

    private void revokeAccess(final String caseTypeId, final Long caseId, final String userId, final String caseRole, final CaseUserRepository caseUserRepository) {
        String repositoryType = getRepositoryType(caseUserRepository);
        LOG.info("{}. Revoking role={} access to caseId={} (caseTypeId={}) for userId={}",
            repositoryType, caseRole, caseId, caseTypeId, userId);
        caseUserRepository.revokeAccess(caseTypeId, caseId, userId, caseRole);
        LOG.info("{}. Revoked role={} access to caseId={} (caseTypeId={}) for userId={}",
            repositoryType, caseRole, caseId, caseTypeId, userId);
    }

    private List<Long> findCasesUserIdHasAccessTo(final String userId, final CaseUserRepository caseUserRepository) {
        String repositoryType = getRepositoryType(caseUserRepository);
        LOG.info("{}. Finding case ids userId={} has access to", repositoryType, userId);
        List<Long> casesUserIdHasAccessTo = caseUserRepository.findCasesUserIdHasAccessTo(userId);
        LOG.info("{}. Found caseIds={} userId={} has access to", repositoryType, casesUserIdHasAccessTo, userId);
        return casesUserIdHasAccessTo;
    }

    private String getRepositoryType(final CaseUserRepository caseUserRepository) {
        return caseUserRepository instanceof CCDCaseUserRepository ? "CCD" : "AM";
    }

    private boolean discardCasesForUknownMode(final CaseUserRepository caseUserRepository, final CaseDetails caseDetails) {
        return amSwitch.isReadAccessManagementWithCCD(caseDetails.getCaseTypeId()) && caseUserRepository instanceof CCDCaseUserRepository ||
            amSwitch.isReadAccessManagementWithAM(caseDetails.getCaseTypeId()) && caseUserRepository instanceof AMCaseUserRepository;
    }
}
