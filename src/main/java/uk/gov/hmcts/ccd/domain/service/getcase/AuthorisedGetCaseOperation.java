package uk.gov.hmcts.ccd.domain.service.getcase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Service
@Qualifier("authorised")
public class AuthorisedGetCaseOperation implements GetCaseOperation {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
    };

    private final GetCaseOperation getCaseOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final AccessControlService accessControlService;
    private final UserRepository userRepository;


    public AuthorisedGetCaseOperation(@Qualifier("classified") final GetCaseOperation getCaseOperation,
                                      @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                      final AccessControlService accessControlService,
                                      @Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository) {
        this.getCaseOperation = getCaseOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.accessControlService = accessControlService;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<CaseDetails> execute(String jurisdictionId, String caseTypeId, String caseReference) {

        return getCaseOperation.execute(jurisdictionId, caseTypeId, caseReference)
            .flatMap(caseDetails ->
                         verifyReadAccess(getCaseType(caseTypeId),
                                          getUserRoles(),
                                          caseDetails));
    }

    @Override
    public Optional<CaseDetails> execute(String caseReference) {
        return getCaseOperation.execute(caseReference)
            .flatMap(caseDetails ->
                         verifyReadAccess(getCaseType(caseDetails.getCaseTypeId()),
                                          getUserRoles(),
                                          caseDetails));
    }

    private CaseType getCaseType(String caseTypeId) {
        return caseDefinitionRepository.getCaseType(caseTypeId);
    }

    private Set<String> getUserRoles() {
        return userRepository.getUserRoles();
    }

    private Optional<CaseDetails> verifyReadAccess(CaseType caseType, Set<String> userRoles, CaseDetails caseDetails) {

        if (caseType == null || caseDetails == null || CollectionUtils.isEmpty(userRoles)) {
            return Optional.empty();
        }

        if (!accessControlService.canAccessCaseTypeWithCriteria(caseType, userRoles, CAN_READ) ||
            !accessControlService.canAccessCaseStateWithCriteria(caseDetails.getState(), caseType, userRoles, CAN_READ)) {
            return Optional.empty();
        }

        caseDetails.setData(MAPPER.convertValue(
            accessControlService.filterCaseFieldsByAccess(
                MAPPER.convertValue(caseDetails.getData(), JsonNode.class),
                caseType.getCaseFields(),
                userRoles,
                CAN_READ),
            STRING_JSON_MAP));
        caseDetails.setDataClassification(MAPPER.convertValue(
            accessControlService.filterCaseFieldsByAccess(
                MAPPER.convertValue(caseDetails.getDataClassification(), JsonNode.class),
                caseType.getCaseFields(),
                userRoles,
                CAN_READ),
            STRING_JSON_MAP));

        return Optional.of(caseDetails);
    }

}
