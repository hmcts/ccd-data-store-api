package uk.gov.hmcts.ccd.domain.service.caseaccess;

import com.google.common.collect.Lists;
import org.elasticsearch.common.util.set.Sets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseRoleRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseRoleRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole;
import uk.gov.hmcts.ccd.data.caseaccess.SwitchableCaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.InvalidCaseRoleException;
import uk.gov.hmcts.ccd.v2.external.domain.CaseUser;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;

@Service
public class CaseAccessOperation {

    private final SwitchableCaseUserRepository switchableCaseUserRepository;
    private final CaseDetailsRepository caseDetailsRepository;
    private final CaseRoleRepository caseRoleRepository;

    public CaseAccessOperation(final SwitchableCaseUserRepository switchableCaseUserRepository,
                               @Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
                               @Qualifier(CachedCaseRoleRepository.QUALIFIER) CaseRoleRepository caseRoleRepository) {
        this.switchableCaseUserRepository = switchableCaseUserRepository;
        this.caseDetailsRepository = caseDetailsRepository;
        this.caseRoleRepository = caseRoleRepository;
    }

    @Transactional
    public void grantAccess(final String jurisdictionId, final String caseReference, final String userId) {
        final Optional<CaseDetails> maybeCase = caseDetailsRepository.findByReference(jurisdictionId,
            Long.valueOf(caseReference));

        final CaseDetails caseDetails = maybeCase.orElseThrow(() -> new CaseNotFoundException(caseReference));
        switchableCaseUserRepository.forWriting(caseDetails.getCaseTypeId()).forEach(caseUserRepository ->
            caseUserRepository.grantAccess(Long.valueOf(caseDetails.getId()), userId, CREATOR.getRole())
        );
    }

    @Transactional
    public void revokeAccess(final String jurisdictionId, final String caseReference, final String userId) {
        final Optional<CaseDetails> maybeCase = caseDetailsRepository.findByReference(jurisdictionId,
            Long.valueOf(caseReference));
        final CaseDetails caseDetails = maybeCase.orElseThrow(() -> new CaseNotFoundException(caseReference));
        switchableCaseUserRepository.forWriting(caseDetails.getCaseTypeId()).forEach(caseUserRepository ->
            caseUserRepository.revokeAccess(Long.valueOf(caseDetails.getId()), userId, CREATOR.getRole())
        );
    }

    public List<String> findCasesUserIdHasAccessTo(final String userId) {
        List<String> casesReferences = Lists.newArrayList();
        switchableCaseUserRepository.forReading().forEach(caseUserRepository ->
            casesReferences.addAll(caseUserRepository.findCasesUserIdHasAccessTo(userId)
                .stream()
                .map(caseId -> caseDetailsRepository.findById(caseId))
                .filter(caseDetails -> discardCasesForUknownMode(caseUserRepository, caseDetails))
                .map(caseDetails -> caseDetails.getReference() + "")
                .collect(Collectors.toList()))
        );
        return casesReferences;
    }

    @Transactional
    public void updateUserAccess(CaseDetails caseDetails, CaseUser caseUser) {
        final Set<String> validCaseRoles = caseRoleRepository.getCaseRoles(caseDetails.getCaseTypeId());
        final Set<String> globalCaseRoles = GlobalCaseRole.all();
        final Set<String> targetCaseRoles = caseUser.getCaseRoles();

        validateCaseRoles(Sets.union(globalCaseRoles, validCaseRoles), targetCaseRoles);

        final Long caseId = new Long(caseDetails.getId());
        final String userId = caseUser.getUserId();
        final List<String> currentCaseRoles = switchableCaseUserRepository.forReading(caseDetails.getCaseTypeId()).findCaseRoles(caseId, userId);

        grantAddedCaseRoles(userId, caseId, currentCaseRoles, targetCaseRoles);
        revokeRemovedCaseRoles(userId, caseId, currentCaseRoles, targetCaseRoles);
    }

    private boolean discardCasesForUknownMode(final CaseUserRepository caseUserRepository, final CaseDetails caseDetails) {
        return switchableCaseUserRepository.getReadModeForCaseType(caseDetails.getCaseTypeId()).equals(caseUserRepository.getType());
    }

    private void validateCaseRoles(Set<String> validCaseRoles, Set<String> targetCaseRoles) {
        targetCaseRoles.stream()
            .filter(role -> !validCaseRoles.contains(role))
            .findFirst()
            .ifPresent(role -> {
                throw new InvalidCaseRoleException(role);
            });
    }

    private void grantAddedCaseRoles(String userId,
                                     Long caseId,
                                     List<String> currentCaseRoles,
                                     Set<String> targetCaseRoles) {
        List<CaseUserRepository> caseUserRepositories = switchableCaseUserRepository.forWriting(getCaseTypeId(caseId));
        targetCaseRoles.stream()
            .filter(targetRole -> !currentCaseRoles.contains(targetRole))
            .forEach(targetRole -> caseUserRepositories
                .forEach(caseUserRepository ->
                    caseUserRepository.grantAccess(caseId, userId, targetRole))
            );
    }

    private void revokeRemovedCaseRoles(String userId,
                                        Long caseId,
                                        List<String> currentCaseRoles,
                                        Set<String> targetCaseRoles) {
        List<CaseUserRepository> caseUserRepositories = switchableCaseUserRepository.forWriting(getCaseTypeId(caseId));
        currentCaseRoles.stream()
            .filter(currentRole -> !targetCaseRoles.contains(currentRole))
            .forEach(currentRole -> caseUserRepositories
                .forEach(caseUserRepository ->
                    caseUserRepository.revokeAccess(caseId,
                        userId,
                        currentRole))
            );
    }

    private String getCaseTypeId(final Long caseId) {
        return caseDetailsRepository.findById(caseId).getCaseTypeId();
    }
}
