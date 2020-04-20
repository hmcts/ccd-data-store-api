package uk.gov.hmcts.ccd.domain.service.getcase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Service
@Qualifier("authorised")
public class AuthorisedGetCaseOperation implements GetCaseOperation {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<HashMap<String, JsonNode>> STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
    };

    private final GetCaseOperation getCaseOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final AccessControlService accessControlService;
    private final UserRepository userRepository;
    private final CaseUserRepository caseUserRepository;


    public AuthorisedGetCaseOperation(@Qualifier("classified") final GetCaseOperation getCaseOperation,
                                      @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                      final AccessControlService accessControlService,
                                      @Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                      @Qualifier(CachedCaseUserRepository.QUALIFIER)  CaseUserRepository caseUserRepository) {
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

    private CaseTypeDefinition getCaseType(String caseTypeId) {
        return caseDefinitionRepository.getCaseType(caseTypeId);
    }


    private Set<String> getUserRoles(String caseId) {
        return Sets.union(userRepository.getUserRoles(),
            caseUserRepository
                .findCaseRoles(Long.valueOf(caseId), userRepository.getUserId())
                .stream()
                .collect(Collectors.toSet()));
    }

    private Optional<CaseDetails> verifyReadAccess(CaseTypeDefinition caseTypeDefinition, Set<String> userRoles, CaseDetails caseDetails) {

        if (caseTypeDefinition == null || caseDetails == null || CollectionUtils.isEmpty(userRoles)) {
            return Optional.empty();
        }

        if (!accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition, userRoles, CAN_READ) ||
            !accessControlService.canAccessCaseStateWithCriteria(caseDetails.getState(), caseTypeDefinition, userRoles, CAN_READ)) {
            return Optional.empty();
        }

        caseDetails.setData(MAPPER.convertValue(
            accessControlService.filterCaseFieldsByAccess(
                MAPPER.convertValue(caseDetails.getData(), JsonNode.class),
                caseTypeDefinition.getCaseFieldDefinitions(),
                userRoles,
                CAN_READ,
                false),
            STRING_JSON_MAP));
        caseDetails.setDataClassification(MAPPER.convertValue(
            accessControlService.filterCaseFieldsByAccess(
                MAPPER.convertValue(caseDetails.getDataClassification(), JsonNode.class),
                caseTypeDefinition.getCaseFieldDefinitions(),
                userRoles,
                CAN_READ,
                true),
            STRING_JSON_MAP));

        return Optional.of(caseDetails);
    }

}
