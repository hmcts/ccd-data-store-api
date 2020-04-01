package uk.gov.hmcts.ccd.domain.service.getcase;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Service
@Qualifier("authorised")
public class AuthorisedGetCaseOperation implements GetCaseOperation {
    private final GetCaseOperation getCaseOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final AccessControlService accessControlService;
    private final UserRepository userRepository;
    private final CaseUserRepository caseUserRepository;


    public AuthorisedGetCaseOperation(@Qualifier("classified") final GetCaseOperation getCaseOperation,
                                      @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                      final AccessControlService accessControlService,
                                      @Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                      @Qualifier(CachedCaseUserRepository.QUALIFIER) CaseUserRepository caseUserRepository) {
        this.getCaseOperation = getCaseOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.accessControlService = accessControlService;
        this.userRepository = userRepository;
        this.caseUserRepository = caseUserRepository;
    }

    @Override
    public Optional<CaseDetails> execute(String jurisdictionId, String caseTypeId, String caseReference) {

        return this.execute(caseReference);
    }

    @Override
    public Optional<CaseDetails> execute(String caseReference) {
        return getCaseOperation.execute(caseReference)
            .flatMap(caseDetails ->
                verifyReadAccess(getCaseType(caseDetails.getCaseTypeId()),
                    getUserRoles(caseDetails.getId()),
                    caseDetails));
    }

    private CaseType getCaseType(String caseTypeId) {
        return caseDefinitionRepository.getCaseType(caseTypeId);
    }


    private Set<String> getUserRoles(String caseId) {
        return Sets.union(userRepository.getUserRoles(),
            caseUserRepository
                .findCaseRoles(Long.valueOf(caseId), userRepository.getUserId())
                .stream()
                .collect(Collectors.toSet()));
    }

    private Optional<CaseDetails> verifyReadAccess(CaseType caseType, Set<String> userRoles, CaseDetails caseDetails) {

        if (caseType == null || caseDetails == null || CollectionUtils.isEmpty(userRoles)) {
            return Optional.empty();
        }

        if (!accessControlService.canAccessCaseTypeWithCriteria(caseType, userRoles, CAN_READ) ||
            !accessControlService.canAccessCaseStateWithCriteria(caseDetails.getState(), caseType, userRoles, CAN_READ)) {
            return Optional.empty();
        }

        caseDetails.setData(JacksonUtils.convertValue(
            accessControlService.filterCaseFieldsByAccess(
                JacksonUtils.convertValueJsonNode(caseDetails.getData()),
                caseType.getCaseFields(),
                userRoles,
                CAN_READ,
                false)));
        caseDetails.setDataClassification(JacksonUtils.convertValue(
            accessControlService.filterCaseFieldsByAccess(
                JacksonUtils.convertValueJsonNode(caseDetails.getDataClassification()),
                caseType.getCaseFields(),
                userRoles,
                CAN_READ,
                true)));

        return Optional.of(caseDetails);
    }

}
